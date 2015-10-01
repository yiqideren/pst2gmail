package com.atsid.outlook.pst.message;

import com.atsid.outlook.pst.AttachmentLogger;
import com.pff.PSTException;
import com.pff.PSTMessage;
import com.pff.PSTRecipient;
import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.IOException;
import java.util.List;

/**
 * Various utility methods for interacting with <code>PSTMessage</code>, <code>MimeMessage</code> and <code>Message</code>
 * classes.
 */
@Log4j
@Component
public class MessageUtils {
    @Value("${attachment.subject.prefix}")
    private String attachmentSubjectPrefix;
    @Value("${attachment.summary.body.header}")
    private String attachmentBodyHeader;
    @Value("${attachment.summary.body.footer}")
    private String attachmentBodyFooter;
    @Autowired
    private AttachmentLogger attachmentLogger;

    /**
     * Updates the subject to indicate that attachments have been removed.  If no attachments are removed, subject
     * is copied as-is from the <code>PSTMessage</code>.
     *
     * @param pstMessage         Email message from PST file
     * @param attachmentsRemoved Flag to indicate if attachments have been removed.
     * @param mimeMessage        Mime message used in the conversion process
     * @throws MessagingException
     */
    public void updateSubject(PSTMessage pstMessage, boolean attachmentsRemoved, MimeMessage mimeMessage)
            throws MessagingException {
        if (attachmentsRemoved) {
            mimeMessage.setSubject(buildSubject(pstMessage.getSubject()));
        } else {
            mimeMessage.setSubject(pstMessage.getSubject());
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
    public void copyContent(PSTMessage pstMessage, List<String> strippedAttachments, MimeBodyPart text,
                            MimeBodyPart html, MimeMultipart content) throws MessagingException {
        if (strippedAttachments == null || strippedAttachments.isEmpty()) {
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
                html.setContent(buildHtmlContent(attachmentMap, pstMessage.getBodyHTML()), "text/html");
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
    public void copyHeaders(PSTMessage pstMessage, String outputPath, MimeMessage mimeMessage)
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
     * Copies recipients from <code>PSTMessage</code> to <code>MimeMessage</code>.
     *
     * @param pstMessage  Email message from PST file
     * @param mimeMessage Mime message to copy recipients to
     * @throws PSTException
     * @throws IOException
     * @throws MessagingException
     */
    public void copyRecipients(PSTMessage pstMessage, MimeMessage mimeMessage)
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
        String sender;

        if (!"SMTP".equals(pstMessage.getSenderAddrtype())) {
            sender = header.substring(6);
        } else {
            sender = pstMessage.getSenderEmailAddress();
        }

        try {
            mimeMessage.setFrom(new InternetAddress(sender));
        } catch (AddressException ae) {
            log.error(String.format("Error parsing sender %s while processing email %d", sender,
                    pstMessage.getDescriptorNodeId()));
            attachmentLogger.logSenderError(pstMessage.getDescriptorNodeId(), outputPath, sender);
        }
    }

    /**
     * Helper method used to determine if header is a FROM header.
     *
     * @param header header to check
     * @return Returns true if it is a FROM header, false otherwise
     */
    private boolean isFromHeader(String header) {
        return header.toLowerCase().startsWith("from:");
    }

    /**
     * Helper method used to identify if the header is one that contains a recipient header type.
     *
     * @param header Header to check
     * @return Returns true if  header is a recipient type, false otherwise
     */
    private boolean isUsableHeader(String header) {
        String lowerHeader = header.toLowerCase();

        return !lowerHeader.startsWith("to:") && !lowerHeader.startsWith("cc:") && !lowerHeader.startsWith("bcc:");
    }

    /**
     * Helper method used to build HTML body content for message.
     *
     * @param attachmentMap Content of attachments that have been removed for this message
     * @param bodyHtml      Body HTML for the remainder of the message
     * @return Returns a properly formatted HTMl message with the attachments prefixed into the message using PRE tags
     * to preserve formatting.
     */
    private String buildHtmlContent(String attachmentMap, String bodyHtml) {
        return "<pre>" + attachmentMap + "</pre>" + bodyHtml;

    }

    /**
     * Helper method used to build modified subject to indicate attachments have been stripped.
     *
     * @param subject Subject to modify
     * @return Returns a modified subject indicating attachments have been stripped
     */
    private String buildSubject(String subject) {
        return attachmentSubjectPrefix + subject;
    }

    /**
     * Helper method used to build a string table to identify what attachments have been removed for this message.
     *
     * @param strippedAttachments List of stripped attachments from the current message
     * @return Returns a formatted string that contains the list of attachments stripped from the current message
     */
    private String buildAttachmentMap(List<String> strippedAttachments) {
        StringBuilder builder = new StringBuilder();

        builder.append(attachmentBodyHeader);

        for (String attachment : strippedAttachments) {
            builder.append(attachment + "\n");
        }

        builder.append(attachmentBodyFooter);

        return builder.toString();
    }
}