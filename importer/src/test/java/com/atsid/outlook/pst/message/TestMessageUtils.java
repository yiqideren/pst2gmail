package com.atsid.outlook.pst.message;

import com.atsid.outlook.pst.AttachmentLogger;
import com.pff.PSTException;
import com.pff.PSTMessage;
import com.pff.PSTRecipient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:applicationContext-test.xml" })
public class TestMessageUtils {
    @Autowired
    @InjectMocks
    private MessageUtils utils;
    @Mock
    private PSTMessage pstMessage;
    @Mock
    private MimeMessage mimeMessage;
    @Mock
    private AttachmentLogger attachmentLogger;
    @Value("${attachment.subject.prefix}")
    private String attachmentSubjectPrefix;
    @Value("${attachment.summary.body.header}")
    private String attachmentBodyHeader;
    @Value("${attachment.summary.body.footer}")
    private String attachmentBodyFooter;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testUpdateSubjectNoAttachments() throws MessagingException {
        Mockito.when(pstMessage.getSubject()).thenReturn("Subject");

        utils.updateSubject(pstMessage, false, mimeMessage);

        Mockito.verify(pstMessage).getSubject();
        Mockito.verify(mimeMessage).setSubject(Mockito.eq("Subject"));
    }

    @Test
    public void testUpdateSubject() throws MessagingException {
        Mockito.when(pstMessage.getSubject()).thenReturn("Subject");

        utils.updateSubject(pstMessage, true, mimeMessage);

        Mockito.verify(pstMessage).getSubject();
        Mockito.verify(mimeMessage).setSubject(Mockito.eq("prefixSubject"));
    }

    @Test
    public void testCopyContentNullAttachmentsTextOnly() throws MessagingException {
        MimeBodyPart text = Mockito.mock(MimeBodyPart.class);
        MimeBodyPart html = Mockito.mock(MimeBodyPart.class);
        MimeMultipart content = Mockito.mock(MimeMultipart.class);

        Mockito.when(pstMessage.getBodyHTML()).thenReturn("");
        Mockito.when(pstMessage.getBody()).thenReturn("body");

        utils.copyContent(pstMessage, null, text, html, content);

        Mockito.verify(pstMessage).getBody();
        Mockito.verify(pstMessage).getBodyHTML();
        Mockito.verify(text).setText(Mockito.eq("body"));
        Mockito.verify(content).addBodyPart(Mockito.eq(text));
        Mockito.verifyZeroInteractions(html);
    }

    @Test
    public void testCopyContentNoStrippedAttachmentsTextOnly() throws MessagingException {
        MimeBodyPart text = Mockito.mock(MimeBodyPart.class);
        MimeBodyPart html = Mockito.mock(MimeBodyPart.class);
        MimeMultipart content = Mockito.mock(MimeMultipart.class);
        List<String> strippedAttachments = new ArrayList<>();

        Mockito.when(pstMessage.getBodyHTML()).thenReturn("");
        Mockito.when(pstMessage.getBody()).thenReturn("body");

        utils.copyContent(pstMessage, strippedAttachments, text, html, content);

        Mockito.verify(pstMessage).getBody();
        Mockito.verify(pstMessage).getBodyHTML();
        Mockito.verify(text).setText(Mockito.eq("body"));
        Mockito.verify(content).addBodyPart(Mockito.eq(text));
        Mockito.verifyZeroInteractions(html);
    }

    @Test
    public void testCopyContentStrippedAttachmentsTextOnly() throws MessagingException {
        MimeBodyPart text = Mockito.mock(MimeBodyPart.class);
        MimeBodyPart html = Mockito.mock(MimeBodyPart.class);
        MimeMultipart content = Mockito.mock(MimeMultipart.class);
        List<String> strippedAttachments = new ArrayList<>();
        StringBuilder builder = new StringBuilder();

        builder.append(attachmentBodyHeader);
        builder.append("attachment-1\n");
        builder.append("attachment-2\n");
        builder.append(attachmentBodyFooter);
        builder.append("body");

        Mockito.when(pstMessage.getBodyHTML()).thenReturn("");
        Mockito.when(pstMessage.getBody()).thenReturn("body");
        strippedAttachments.add("attachment-1");
        strippedAttachments.add("attachment-2");

        utils.copyContent(pstMessage, strippedAttachments, text, html, content);

        Mockito.verify(pstMessage).getBody();
        Mockito.verify(pstMessage).getBodyHTML();
        Mockito.verify(text).setText(Mockito.eq(builder.toString()));
        Mockito.verify(content).addBodyPart(Mockito.eq(text));
        Mockito.verifyZeroInteractions(html);
    }

    @Test
    public void testCopyContentNullAttachmentsHtml() throws MessagingException {
        MimeBodyPart text = Mockito.mock(MimeBodyPart.class);
        MimeBodyPart html = Mockito.mock(MimeBodyPart.class);
        MimeMultipart content = Mockito.mock(MimeMultipart.class);

        Mockito.when(pstMessage.getBodyHTML()).thenReturn("html");
        Mockito.when(pstMessage.getBody()).thenReturn("body");

        utils.copyContent(pstMessage, null, text, html, content);

        Mockito.verify(pstMessage).getBody();
        Mockito.verify(pstMessage, Mockito.atLeast(1)).getBodyHTML();
        Mockito.verify(text).setText(Mockito.eq("body"));
        Mockito.verify(content).addBodyPart(Mockito.eq(text));
        Mockito.verify(html).setContent(Mockito.eq("html"), Mockito.eq("text/html"));
        Mockito.verify(content).addBodyPart(Mockito.eq(html));
    }

    @Test
    public void testCopyContentNoStrippedAttachmentsHtml() throws MessagingException {
        MimeBodyPart text = Mockito.mock(MimeBodyPart.class);
        MimeBodyPart html = Mockito.mock(MimeBodyPart.class);
        MimeMultipart content = Mockito.mock(MimeMultipart.class);
        List<String> strippedAttachments = new ArrayList<>();
        StringBuilder builder = new StringBuilder();

        builder.append(attachmentBodyHeader);
        builder.append("attachment-1\n");
        builder.append("attachment-2\n");
        builder.append(attachmentBodyFooter);

        Mockito.when(pstMessage.getBodyHTML()).thenReturn("html");
        Mockito.when(pstMessage.getBody()).thenReturn("body");
        strippedAttachments.add("attachment-1");
        strippedAttachments.add("attachment-2");

        utils.copyContent(pstMessage, strippedAttachments, text, html, content);

        Mockito.verify(pstMessage).getBody();
        Mockito.verify(pstMessage, Mockito.atLeast(1)).getBodyHTML();
        Mockito.verify(text).setText(Mockito.eq(builder.toString() + "body"));
        Mockito.verify(content).addBodyPart(Mockito.eq(text));
        Mockito.verify(html)
               .setContent(Mockito.eq("<pre>" + builder.toString() + "</pre>html"), Mockito.eq("text/html"));
        Mockito.verify(content).addBodyPart(Mockito.eq(html));
    }

    @Test
    public void testCopyContentStrippedAttachmentsHtml() throws MessagingException {
        MimeBodyPart text = Mockito.mock(MimeBodyPart.class);
        MimeBodyPart html = Mockito.mock(MimeBodyPart.class);
        MimeMultipart content = Mockito.mock(MimeMultipart.class);
        List<String> strippedAttachments = new ArrayList<>();

        Mockito.when(pstMessage.getBodyHTML()).thenReturn("html");
        Mockito.when(pstMessage.getBody()).thenReturn("body");

        utils.copyContent(pstMessage, strippedAttachments, text, html, content);

        Mockito.verify(pstMessage).getBody();
        Mockito.verify(pstMessage, Mockito.atLeast(1)).getBodyHTML();
        Mockito.verify(text).setText(Mockito.eq("body"));
        Mockito.verify(content).addBodyPart(Mockito.eq(text));
        Mockito.verify(html).setContent(Mockito.eq("html"), Mockito.eq("text/html"));
        Mockito.verify(content).addBodyPart(Mockito.eq(html));
    }

    @Test
    public void testCopyHeadersEmptyHeaders() throws MessagingException {
        Mockito.when(pstMessage.getTransportMessageHeaders()).thenReturn("");

        utils.copyHeaders(pstMessage, "", mimeMessage);

        Mockito.verify(pstMessage).getTransportMessageHeaders();
        Mockito.verifyNoMoreInteractions(pstMessage);
        Mockito.verifyZeroInteractions(mimeMessage);
    }

    @Test
    public void testCopyHeadersContinuedLine() throws MessagingException {
        Mockito.when(pstMessage.getTransportMessageHeaders()).thenReturn("blah:\n blah2\nto: test@me.com");

        utils.copyHeaders(pstMessage, "", mimeMessage);

        Mockito.verify(pstMessage).getTransportMessageHeaders();
        Mockito.verify(mimeMessage).addHeaderLine(Mockito.eq("blah:\n blah2"));
        Mockito.verifyNoMoreInteractions(pstMessage);
        Mockito.verifyNoMoreInteractions(mimeMessage);
    }

    @Test
    public void testCopyHeadersSmtpSender() throws MessagingException {
        Mockito.when(pstMessage.getTransportMessageHeaders())
               .thenReturn("blah: blah2\nto: test@me.com\nfrom: blah@blah.com\nto: me@bogus.com");
        Mockito.when(pstMessage.getSenderAddrtype()).thenReturn("SMTP");
        Mockito.when(pstMessage.getSenderEmailAddress()).thenReturn("valid@test.com");

        utils.copyHeaders(pstMessage, "", mimeMessage);

        Mockito.verify(pstMessage).getTransportMessageHeaders();
        Mockito.verify(pstMessage).getSenderEmailAddress();
        Mockito.verify(pstMessage).getSenderAddrtype();
        Mockito.verify(mimeMessage).addHeaderLine(Mockito.eq("blah: blah2"));
        Mockito.verify(mimeMessage).setFrom(Mockito.eq(new InternetAddress("valid@test.com")));
        Mockito.verifyNoMoreInteractions(pstMessage);
        Mockito.verifyNoMoreInteractions(mimeMessage);
    }

    @Test
    public void testCopyHeadersSmtpSenderAddressError() throws MessagingException {
        Mockito.when(pstMessage.getTransportMessageHeaders())
               .thenReturn("blah: blah2\nto: test@me.com\nfrom: blah@blah.com\nto: me@bogus.com");
        Mockito.when(pstMessage.getSenderAddrtype()).thenReturn("SMTP");
        Mockito.when(pstMessage.getSenderEmailAddress()).thenReturn("valid test.com");
        Mockito.when(pstMessage.getDescriptorNodeId()).thenReturn(1L);

        utils.copyHeaders(pstMessage, "OP", mimeMessage);

        Mockito.verify(pstMessage).getTransportMessageHeaders();
        Mockito.verify(pstMessage, Mockito.atLeast(1)).getSenderEmailAddress();
        Mockito.verify(pstMessage).getSenderAddrtype();
        Mockito.verify(pstMessage, Mockito.atLeast(1)).getDescriptorNodeId();
        Mockito.verify(mimeMessage).addHeaderLine(Mockito.eq("blah: blah2"));
        Mockito.verify(attachmentLogger).logSenderError(Mockito.eq(1L), Mockito.eq("OP"), Mockito.eq("valid test.com"));
        Mockito.verifyNoMoreInteractions(pstMessage);
        Mockito.verifyNoMoreInteractions(mimeMessage);
    }

    @Test
    public void testCopyHeadersNonSmtpSenderAddressError() throws MessagingException {
        Mockito.when(pstMessage.getTransportMessageHeaders())
               .thenReturn("blah: blah2\nto: test@me.com\nfrom: valid test.com\nto: bogus@test.com");
        Mockito.when(pstMessage.getSenderAddrtype()).thenReturn("EX");
        Mockito.when(pstMessage.getDescriptorNodeId()).thenReturn(1L);

        utils.copyHeaders(pstMessage, "OP", mimeMessage);

        Mockito.verify(pstMessage).getTransportMessageHeaders();
        Mockito.verify(pstMessage).getSenderAddrtype();
        Mockito.verify(pstMessage, Mockito.atLeast(1)).getDescriptorNodeId();
        Mockito.verify(mimeMessage).addHeaderLine(Mockito.eq("blah: blah2"));
        Mockito.verify(attachmentLogger).logSenderError(Mockito.eq(1L), Mockito.eq("OP"), Mockito.eq("valid test.com"));
        Mockito.verifyNoMoreInteractions(pstMessage);
        Mockito.verifyNoMoreInteractions(mimeMessage);
    }

    @Test
    public void testCopyRecipientNoRecipients() throws PSTException, IOException, MessagingException {
        Mockito.when(pstMessage.getNumberOfRecipients()).thenReturn(0);

        utils.copyRecipients(pstMessage, mimeMessage);

        Mockito.verify(pstMessage).getNumberOfRecipients();
        Mockito.verifyNoMoreInteractions(pstMessage);
        Mockito.verifyZeroInteractions(mimeMessage);
    }

    @Test
    public void testCopyRecipient() throws PSTException, IOException, MessagingException {
        PSTRecipient recip1 = Mockito.mock(PSTRecipient.class);
        PSTRecipient recip2 = Mockito.mock(PSTRecipient.class);
        PSTRecipient recip3 = Mockito.mock(PSTRecipient.class);

        Mockito.when(pstMessage.getNumberOfRecipients()).thenReturn(3);
        Mockito.when(pstMessage.getRecipient(0)).thenReturn(recip1);
        Mockito.when(pstMessage.getRecipient(1)).thenReturn(recip2);
        Mockito.when(pstMessage.getRecipient(2)).thenReturn(recip3);
        Mockito.when(recip1.getRecipientType()).thenReturn(PSTRecipient.MAPI_TO);
        Mockito.when(recip1.getSmtpAddress()).thenReturn("to@test.com");
        Mockito.when(recip2.getRecipientType()).thenReturn(PSTRecipient.MAPI_CC);
        Mockito.when(recip2.getSmtpAddress()).thenReturn("cc@test.com");
        Mockito.when(recip3.getRecipientType()).thenReturn(PSTRecipient.MAPI_BCC);
        Mockito.when(recip3.getSmtpAddress()).thenReturn("bcc@test.com");

        utils.copyRecipients(pstMessage, mimeMessage);

        Mockito.verify(pstMessage, Mockito.atLeast(1)).getNumberOfRecipients();
        Mockito.verify(pstMessage).getRecipient(Mockito.eq(0));
        Mockito.verify(pstMessage).getRecipient(Mockito.eq(1));
        Mockito.verify(pstMessage).getRecipient(Mockito.eq(2));
        Mockito.verify(mimeMessage).addRecipients(MimeMessage.RecipientType.TO, "to@test.com");
        Mockito.verify(mimeMessage).addRecipients(MimeMessage.RecipientType.CC, "cc@test.com");
        Mockito.verify(mimeMessage).addRecipients(MimeMessage.RecipientType.BCC, "bcc@test.com");
    }
}