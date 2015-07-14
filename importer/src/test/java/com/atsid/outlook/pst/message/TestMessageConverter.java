package com.atsid.outlook.pst.message;

import com.atsid.exchange.email.SenderResolver;
import com.google.api.services.gmail.model.Message;
import com.pff.PSTException;
import com.pff.PSTMessage;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.mail.Flags;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:applicationContext-test.xml" })
public class TestMessageConverter {
    @Mock
    private SenderResolver mockSenderResolver;
    @Mock
    private MessageUtils mockMessageUtils;
    @Mock
    private MimeMessageFactory mockMimeMessageFactory;
    @Mock
    private MimeMessage mockMimeMessage;
    @Mock
    private MimeBodyPart mockText;
    @Mock
    private MimeBodyPart mockHtml;
    @Mock
    private PSTMessage mockPstMessage;
    private List<String> attachments;
    private Date currentDate;
    @Mock
    private MimeMultipart mockContent;
    @Autowired
    @InjectMocks
    private MessageConverter converter;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        attachments = new ArrayList<>();
        currentDate = new Date();

        // This makes these tests a little brittle as they depend on calling the factory in the
        // correct order.  If order of calling is reversed, some of these tests will fail.
        Mockito.when(mockMimeMessageFactory.getMimeBodyPart()).thenReturn(mockText, mockHtml);
        Mockito.when(mockMimeMessageFactory.getMimemultipart("alternative")).thenReturn(mockContent);
        Mockito.when(mockMimeMessageFactory.getMimeMessage()).thenReturn(mockMimeMessage);
        Mockito.when(mockContent.getContentType()).thenReturn("contentType");
        Mockito.when(mockPstMessage.getMessageDeliveryTime()).thenReturn(currentDate);
        Mockito.when(mockPstMessage.hasReplied()).thenReturn(true);
        Mockito.when(mockPstMessage.isFlagged()).thenReturn(false);
        Mockito.when(mockPstMessage.isRead()).thenReturn(true);
    }

    @Test
    public void testConvertMessageNonExchange() throws PSTException, MessagingException, IOException {
        Mockito.when(mockPstMessage.getSenderAddrtype()).thenReturn("SMTP");

        Message message = converter.convertMessage(mockPstMessage, "output", attachments);

        Mockito.verify(mockMessageUtils)
               .copyContent(Mockito.eq(mockPstMessage), Mockito.eq(attachments), Mockito.eq(mockText),
                       Mockito.eq(mockHtml), Mockito.eq(mockContent));
        Mockito.verify(mockMimeMessage).setContent(Mockito.eq(mockContent));
        Mockito.verify(mockMimeMessage).setHeader(Mockito.eq("Content-Type"), Mockito.eq("contentType"));
        Mockito.verify(mockMessageUtils).copyRecipients(Mockito.eq(mockPstMessage), Mockito.eq(mockMimeMessage));
        Mockito.verify(mockMessageUtils)
               .copyHeaders(Mockito.eq(mockPstMessage), Mockito.eq("output"), Mockito.eq(mockMimeMessage));
        Mockito.verify(mockMessageUtils)
               .updateSubject(Mockito.eq(mockPstMessage), Mockito.eq(false), Mockito.eq(mockMimeMessage));
        Mockito.verify(mockPstMessage).getSenderAddrtype();
        Mockito.verify(mockMimeMessage).setSentDate(Mockito.eq(currentDate));
        Mockito.verify(mockMimeMessage).setFlag(Mockito.eq(Flags.Flag.ANSWERED), Mockito.eq(true));
        Mockito.verify(mockMimeMessage).setFlag(Mockito.eq(Flags.Flag.FLAGGED), Mockito.eq(false));
        Mockito.verify(mockMimeMessage).setFlag(Mockito.eq(Flags.Flag.SEEN), Mockito.eq(true));

        Assert.assertEquals(currentDate.getTime(), message.get("internalDate"));
    }

    @Test
    public void testConvertMessageExchangeMessageResolvable() throws PSTException, MessagingException, IOException {
        Mockito.when(mockPstMessage.getSenderAddrtype()).thenReturn("EX");
        Mockito.when(mockSenderResolver.resolveSender(mockPstMessage)).thenReturn("test@example.com");

        Message message = converter.convertMessage(mockPstMessage, "output", attachments);

        Mockito.verify(mockMessageUtils)
               .copyContent(Mockito.eq(mockPstMessage), Mockito.eq(attachments), Mockito.eq(mockText),
                       Mockito.eq(mockHtml), Mockito.eq(mockContent));
        Mockito.verify(mockMimeMessage).setContent(Mockito.eq(mockContent));
        Mockito.verify(mockMimeMessage).setHeader(Mockito.eq("Content-Type"), Mockito.eq("contentType"));
        Mockito.verify(mockMessageUtils).copyRecipients(Mockito.eq(mockPstMessage), Mockito.eq(mockMimeMessage));
        Mockito.verify(mockMessageUtils)
               .copyHeaders(Mockito.eq(mockPstMessage), Mockito.eq("output"), Mockito.eq(mockMimeMessage));
        Mockito.verify(mockMessageUtils)
               .updateSubject(Mockito.eq(mockPstMessage), Mockito.eq(false), Mockito.eq(mockMimeMessage));
        Mockito.verify(mockPstMessage).getSenderAddrtype();
        Mockito.verify(mockMimeMessage).setSentDate(Mockito.eq(currentDate));
        Mockito.verify(mockMimeMessage).setFlag(Mockito.eq(Flags.Flag.ANSWERED), Mockito.eq(true));
        Mockito.verify(mockMimeMessage).setFlag(Mockito.eq(Flags.Flag.FLAGGED), Mockito.eq(false));
        Mockito.verify(mockMimeMessage).setFlag(Mockito.eq(Flags.Flag.SEEN), Mockito.eq(true));
        Mockito.verify(mockMimeMessage).setFrom(Mockito.any(InternetAddress.class));
        Assert.assertEquals(currentDate.getTime(), message.get("internalDate"));
    }

    @Test
    public void testConvertMessageExchangeMessageWithoutSenderEmail()
            throws PSTException, MessagingException, IOException {
        Mockito.when(mockPstMessage.getSenderAddrtype()).thenReturn("EX");
        Mockito.when(mockSenderResolver.resolveSender(mockPstMessage)).thenReturn("");

        Message message = converter.convertMessage(mockPstMessage, "output", attachments);

        Mockito.verify(mockMessageUtils)
               .copyContent(Mockito.eq(mockPstMessage), Mockito.eq(attachments), Mockito.eq(mockText),
                       Mockito.eq(mockHtml), Mockito.eq(mockContent));
        Mockito.verify(mockMimeMessage).setContent(Mockito.eq(mockContent));
        Mockito.verify(mockMimeMessage).setHeader(Mockito.eq("Content-Type"), Mockito.eq("contentType"));
        Mockito.verify(mockMessageUtils).copyRecipients(Mockito.eq(mockPstMessage), Mockito.eq(mockMimeMessage));
        Mockito.verify(mockMessageUtils)
               .copyHeaders(Mockito.eq(mockPstMessage), Mockito.eq("output"), Mockito.eq(mockMimeMessage));
        Mockito.verify(mockMessageUtils)
               .updateSubject(Mockito.eq(mockPstMessage), Mockito.eq(false), Mockito.eq(mockMimeMessage));
        Mockito.verify(mockPstMessage).getSenderAddrtype();
        Mockito.verify(mockMimeMessage).setSentDate(Mockito.eq(currentDate));
        Mockito.verify(mockMimeMessage).setFlag(Mockito.eq(Flags.Flag.ANSWERED), Mockito.eq(true));
        Mockito.verify(mockMimeMessage).setFlag(Mockito.eq(Flags.Flag.FLAGGED), Mockito.eq(false));
        Mockito.verify(mockMimeMessage).setFlag(Mockito.eq(Flags.Flag.SEEN), Mockito.eq(true));
        Assert.assertEquals(currentDate.getTime(), message.get("internalDate"));
    }
}