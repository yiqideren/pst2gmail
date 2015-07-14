package com.atsid.outlook.pst.message;

import org.springframework.stereotype.Component;

import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.util.Properties;

/**
 * Factory for creating various <code>MimeMessage</code> components.
 */
@Component
public class MimeMessageFactory {
    private static Properties props;
    private static Session session;

    static {
        props = System.getProperties();
        session = Session.getInstance(props, null);
    }

    /**
     * Gets a new <code>MimeMessage</code>
     *
     * @return Returns a new <code>MimeMessage</code>
     */
    public MimeMessage getMimeMessage() {
        return new MimeMessage(session);
    }

    /**
     * Gets a new <code>MimeBodyPart</code>
     *
     * @return Returns a new <code>MimeBodyPart</code>
     */
    public MimeBodyPart getMimeBodyPart() {
        return new MimeBodyPart();
    }

    /**
     * Gets a new <code>MimeMultipart</code>
     *
     * @param subtype Subtype to pass to <code>MimeMultipart</code> constructor
     * @return Returns new <code>MimeMultipart</code> instance with specified subtype
     */
    public MimeMultipart getMimemultipart(String subtype) {
        return new MimeMultipart(subtype);
    }
}