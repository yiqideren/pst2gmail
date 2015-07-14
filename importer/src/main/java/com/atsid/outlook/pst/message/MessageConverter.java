package com.atsid.outlook.pst.message;

import com.atsid.exchange.email.SenderResolver;
import com.google.api.client.util.Base64;
import com.google.api.services.gmail.model.Message;
import com.pff.PSTException;
import com.pff.PSTMessage;
import lombok.extern.log4j.Log4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.mail.Flags;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Converts <code>PSTMessage</code> into a <code>Message</code> using a <code>MimeMessage</code> instance as
 * a container.
 */
@Component
@Log4j
public class MessageConverter {
    @Autowired
    private SenderResolver senderResolver;
    @Autowired
    private MessageUtils messageUtils;
    @Autowired
    private MimeMessageFactory mimeMessageFactory;

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
        MimeMessage mimeMessage = mimeMessageFactory.getMimeMessage();
        MimeBodyPart text = mimeMessageFactory.getMimeBodyPart();
        MimeBodyPart html = mimeMessageFactory.getMimeBodyPart();
        MimeMultipart content = mimeMessageFactory.getMimemultipart("alternative");

        messageUtils.copyContent(pstMessage, strippedAttachments, text, html, content);
        mimeMessage.setContent(content);
        mimeMessage.setHeader("Content-Type", content.getContentType());

        messageUtils.copyRecipients(pstMessage, mimeMessage);
        messageUtils.copyHeaders(pstMessage, outputPath, mimeMessage);

        if (pstMessage.getSenderAddrtype().equals("EX")) {
            setFromExchange(pstMessage, mimeMessage);
        }

        messageUtils.updateSubject(pstMessage, !strippedAttachments.isEmpty(), mimeMessage);
        mimeMessage.setSentDate(pstMessage.getMessageDeliveryTime());
        mimeMessage.setFlag(Flags.Flag.ANSWERED, pstMessage.hasReplied());
        mimeMessage.setFlag(Flags.Flag.FLAGGED, pstMessage.isFlagged());
        mimeMessage.setFlag(Flags.Flag.SEEN, pstMessage.isRead());

        Message message = createGmailMessageFromMimeMessage(mimeMessage);

        message.set("internalDate", pstMessage.getMessageDeliveryTime().getTime());

        return message;
    }

    /**
     * Resolves an address from exchange to an email address.
     *
     * @param pstMessage  Message to extract email from
     * @param mimeMessage Mime message to place sender in
     * @throws MessagingException
     */
    private void setFromExchange(PSTMessage pstMessage, MimeMessage mimeMessage) throws MessagingException {
        String senderEmail = senderResolver.resolveSender(pstMessage);

        if (StringUtils.isNotBlank(senderEmail)) {
            mimeMessage.setFrom(new InternetAddress(senderEmail));
        } else {
            log.error(String.format("Could not find sender email address for message %d with sender %s",
                    pstMessage.getDescriptorNodeId(), pstMessage.getSenderEmailAddress()));
        }
    }

    /**
     * Creates a GMail <code>Message</code> model from a <code>MimeMessage</code>.
     *
     * @param email Mime message to convert
     * @return Returns a GMail Message model from the specified MimeMessage
     * @throws MessagingException
     * @throws IOException
     */
    private com.google.api.services.gmail.model.Message createGmailMessageFromMimeMessage(MimeMessage email)
            throws MessagingException, IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        email.writeTo(bytes);
        String encodedEmail = Base64.encodeBase64URLSafeString(bytes.toByteArray());
        com.google.api.services.gmail.model.Message message = new com.google.api.services.gmail.model.Message();
        message.setRaw(encodedEmail);

        return message;
    }

}