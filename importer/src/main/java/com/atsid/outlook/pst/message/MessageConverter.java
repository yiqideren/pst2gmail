package com.atsid.outlook.pst.message;

import com.atsid.exchange.email.SenderResolver;
import com.atsid.outlook.pst.AttachmentLogger;
import com.google.api.client.util.Base64;
import com.google.api.services.gmail.model.Message;
import com.pff.PSTException;
import com.pff.PSTMessage;
import com.pff.PSTRecipient;
import lombok.extern.log4j.Log4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.mail.Flags;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

@Component
@Log4j
public class MessageConverter {
    @Autowired
    private AttachmentLogger attachmentLogger;
    @Autowired
    private SenderResolver senderResolver;

    /**
     * Converts a <code>PSTMessage</code> to a <code>Message</code> and alters subject and body to indicate
     * that attachments have been removed.
     *
     * @param pstMessage          PSTMessage to convert
     * @param outputPath          Output path where attachments and log files are written
     * @param strippedAttachments List of attachments that ahve been stripped from this email
     * @return Returns a <code>Message</code> object ready for import into GMail
     * @throws MessagingException
     * @throws PSTException
     * @throws IOException
     */
    public Message convertMessage(PSTMessage pstMessage, String outputPath, List<String> strippedAttachments)
            throws MessagingException, PSTException, IOException {
        Properties props = System.getProperties();
        Session session = Session.getInstance(props, null);
        MimeMessage mimeMessage = new MimeMessage(session);
        MimeBodyPart text = new MimeBodyPart();
        MimeBodyPart html = new MimeBodyPart();
        MimeMultipart content = new MimeMultipart("alternative");

        copyContent(pstMessage, strippedAttachments, text, html, content);
        mimeMessage.setContent(content);
        mimeMessage.setHeader("Content-Type", content.getContentType());

        copyRecipients(pstMessage, mimeMessage);
        copyHeaders(pstMessage, outputPath, mimeMessage);

        if (pstMessage.getSenderAddrtype().equals("EX")) {
            setFromExchange(pstMessage, mimeMessage);
        }

        updateSubject(pstMessage, strippedAttachments, mimeMessage);
        mimeMessage.setSentDate(pstMessage.getMessageDeliveryTime());
        mimeMessage.setFlag(Flags.Flag.ANSWERED, pstMessage.hasReplied());
        mimeMessage.setFlag(Flags.Flag.FLAGGED, pstMessage.isFlagged());
        mimeMessage.setFlag(Flags.Flag.SEEN, pstMessage.isRead());

        Message message = createMessageWithEmail(mimeMessage);

        message.set("internalDate", pstMessage.getMessageDeliveryTime().getTime());

        return message;
    }

    /**
     * Updates the subject to indicate that attachments have been removed.  If no attachments are removed, subject
     * is copied as-is from the <code>PSTMessage</code>.
     *
     * @param pstMessage          Email message from PST file
     * @param strippedAttachments List of attachments that have been stripped
     * @param mimeMessage         Mime message used in the conversion process
     * @throws MessagingException
     */
    private void updateSubject(PSTMessage pstMessage, List<String> strippedAttachments, MimeMessage mimeMessage)
            throws MessagingException {
        if (strippedAttachments.isEmpty()) {
            mimeMessage.setSubject(pstMessage.getSubject());
        } else {
            mimeMessage.setSubject(buildSubject(pstMessage.getSubject(), strippedAttachments));
        }
    }

    /**
     * Copies the content from the <code>PSTMessage</code> to a <code>MimeMessage</code>.  If it is an HTML message,
     * both text and HTML content are copied.  If just a text message, no HTML parts are added.
     *
     * @param pstMessage          Email message from PST file
     * @param strippedAttachments List of attachments that have been stripped for this message
     * @param text                Text <code>MimeBodyPart</code> used in all messages
     * @param html                HTML <code>MimeBodyPart</code> used in HTML messages
     * @param content             Message content <code>MimeMultipart</code>
     * @throws MessagingException
     */
    private void copyContent(PSTMessage pstMessage, List<String> strippedAttachments, MimeBodyPart text,
            MimeBodyPart html, MimeMultipart content) throws MessagingException {
        if (strippedAttachments.isEmpty()) {
            text.setText(pstMessage.getBody());
            content.addBodyPart(text);

            if (!pstMessage.getBodyHTML().isEmpty()) {
                html.setContent(pstMessage.getBodyHTML(), "text/html");
                content.addBodyPart(html);
            }
        } else {
            String attachmentMap = buildAttachmentMap(strippedAttachments);

            text.setText(attachmentMap + pstMessage.getBody());
            content.addBodyPart(text);

            if (!pstMessage.getBodyHTML().isEmpty()) {
                html.setContent("<pre>" + attachmentMap + "</pre>" + pstMessage.getBodyHTML(), "text/html");
                content.addBodyPart(html);
            }
        }
    }

    /**
     * Copies headers from <code>PSTMessage</code> over to the <code>MimeMessage</code>.
     *
     * @param pstMessage  Email message from PST file
     * @param outputPath  Output path for attachments and log files
     * @param mimeMessage Mime message to copy headers to
     * @throws MessagingException
     */
    private void copyHeaders(PSTMessage pstMessage, String outputPath, MimeMessage mimeMessage)
            throws MessagingException {
        String[] headers = pstMessage.getTransportMessageHeaders().split("\n");
        String header = headers[0];

        for (int i = 1; i < headers.length; ++i) {
            // if we are a continuation of the previous header, append otherwise add
            String nextHeader = headers[i];

            if (Character.isWhitespace(nextHeader.charAt(0))) {
                // we need to append!
                header = header + "\n" + nextHeader;
            } else {
                if (isFromHeader(header)) {
                    extractFromAddressFromHeader(pstMessage, outputPath, mimeMessage, header);
                } else if (isUsableHeader(header)) {
                    mimeMessage.addHeaderLine(header);
                }

                header = nextHeader;
            }
        }
    }

    /**
     * Extracts the from email address from the headers.
     *
     * @param pstMessage  Email message from PST file
     * @param outputPath  Output path for attachments and log files
     * @param mimeMessage Mime message to set from address on
     * @param header      Header that contains from address
     * @throws MessagingException
     */
    private void extractFromAddressFromHeader(PSTMessage pstMessage, String outputPath, MimeMessage mimeMessage,
            String header) throws MessagingException {
        if (!"SMTP".equals(pstMessage.getSenderAddrtype())) {
            mimeMessage.setFrom(new InternetAddress(header.substring(6)));
        } else {
            try {
                mimeMessage.setFrom(new InternetAddress(pstMessage.getSenderEmailAddress()));
            } catch (AddressException ae) {
                log.error(String.format("Error parsing sender %s while processing email %d",
                        pstMessage.getSenderEmailAddress(), pstMessage.getDescriptorNodeId()));
                attachmentLogger.logSenderError(pstMessage.getDescriptorNodeId(), outputPath,
                        pstMessage.getSenderEmailAddress());
            }
        }
    }

    /**
     * Copies recipients from <code>PSTMessage</code> to <code>MimeMessage</code>.
     *
     * @param pstMessage  Email message from PST file
     * @param mimeMessage Mime message to copy recipients to
     * @throws PSTException
     * @throws IOException
     * @throws MessagingException
     */
    private void copyRecipients(PSTMessage pstMessage, MimeMessage mimeMessage)
            throws PSTException, IOException, MessagingException {
        if (pstMessage.getNumberOfRecipients() > 0) {
            for (int i = 0; i < pstMessage.getNumberOfRecipients(); ++i) {
                PSTRecipient recipient = pstMessage.getRecipient(i);

                switch (recipient.getRecipientType()) {
                case PSTRecipient.MAPI_TO:
                    mimeMessage.addRecipients(javax.mail.Message.RecipientType.TO, recipient.getSmtpAddress());
                    break;
                case PSTRecipient.MAPI_CC:
                    mimeMessage.addRecipients(javax.mail.Message.RecipientType.CC, recipient.getSmtpAddress());
                    break;
                case PSTRecipient.MAPI_BCC:
                    mimeMessage.addRecipients(javax.mail.Message.RecipientType.BCC, recipient.getSmtpAddress());
                    break;
                }
            }
        }
    }

    private void setFromExchange(PSTMessage pstMessage, MimeMessage mimeMessage) throws MessagingException {
        String senderEmail = senderResolver.resolveSender(pstMessage);

        if (StringUtils.isNotBlank(senderEmail)) {
            mimeMessage.setFrom(new InternetAddress(senderEmail));
        } else {
            log.error(String.format("Could not find sender email address for message %d with sender %s",
                    pstMessage.getDescriptorNode(), pstMessage.getSenderEmailAddress()));
        }
    }

    private String buildSubject(String subject, List<String> strippedAttachments) {
        return "*** ATTACHMENTS REMOVED *** " + subject;
    }

    private String buildAttachmentMap(List<String> strippedAttachments) {
        StringBuilder builder = new StringBuilder();

        builder.append("========================================\n");
        builder.append(" R E M O V E D    A T T A C H M E N T S \n");
        builder.append("========================================\n");

        for (String attachment : strippedAttachments) {
            builder.append(attachment + "\n");
        }
        builder.append("========================================\n");

        return builder.toString();
    }

    private boolean isFromHeader(String header) {
        return header.toLowerCase().startsWith("from:");
    }

    private boolean isUsableHeader(String header) {
        String lowerHeader = header.toLowerCase();

        return !lowerHeader.startsWith("to:") && !lowerHeader.startsWith("cc:") && !lowerHeader.startsWith("bcc:");
    }

    private com.google.api.services.gmail.model.Message createMessageWithEmail(MimeMessage email)
            throws MessagingException, IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        email.writeTo(bytes);
        String encodedEmail = Base64.encodeBase64URLSafeString(bytes.toByteArray());
        com.google.api.services.gmail.model.Message message = new com.google.api.services.gmail.model.Message();
        message.setRaw(encodedEmail);
        return message;
    }
}