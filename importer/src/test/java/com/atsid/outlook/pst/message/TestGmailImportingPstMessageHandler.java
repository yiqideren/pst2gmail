package com.atsid.outlook.pst.message;

import com.atsid.outlook.pst.AttachmentExtractor;
import com.atsid.outlook.pst.GmailServiceFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Label;
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

import javax.mail.MessagingException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:applicationContext-test.xml" })
public class TestGmailImportingPstMessageHandler {
    private static final String EMAIL_ADDRESS = "test@example.com";
    @Mock
    private GmailServiceFactory mockServiceFactory;
    @Mock
    private GMailLabeler mockLabeler;
    @Mock
    private MessageConverter mockMessageConverter;
    @Mock
    private AttachmentExtractor mockExtractor;
    @Mock
    private GMailLabelFactory mockLabelFactory;
    @Mock
    private PSTMessage mockPstMessage;
    private Message mockGmailMessage;
    @Mock
    private Gmail mockGmailService;
    @Mock
    private Gmail.Users mockGmailUsersSerivce;
    @Mock
    private Gmail.Users.Messages mockGmailMessagesService;
    @Mock
    private Gmail.Users.Messages.GmailImport mockGmailImportService;
    private Label mockLabel1;
    private Label mockLabel2;
    @Autowired
    @InjectMocks
    private GmailImportingPstMessageHandler messageHandler;
    private List<String> folderNames;
    private List<String> attachmentList;

    @Before
    public void setup() throws IOException, PSTException, MessagingException {
        MockitoAnnotations.initMocks(this);
        folderNames = new ArrayList<>();
        attachmentList = new ArrayList<>();
        mockGmailMessage = new Message();
        mockLabel1 = new Label();
        mockLabel2 = new Label();

        mockLabel1.setId("label-1");
        mockLabel2.setId("label-2");

        Mockito.when(mockServiceFactory.getGmailService(EMAIL_ADDRESS)).thenReturn(mockGmailService);
        Mockito.when(mockLabelFactory.getLabeler(mockGmailService, EMAIL_ADDRESS)).thenReturn(mockLabeler);
        Mockito.when(mockExtractor.extractAttachments(mockPstMessage, "output")).thenReturn(attachmentList);
        Mockito.when(mockMessageConverter.convertMessage(mockPstMessage, "output", attachmentList))
               .thenReturn(mockGmailMessage);
        Mockito.when(mockGmailService.users()).thenReturn(mockGmailUsersSerivce);
        Mockito.when(mockGmailUsersSerivce.messages()).thenReturn(mockGmailMessagesService);
        Mockito.when(mockGmailMessagesService.gmailImport(EMAIL_ADDRESS, mockGmailMessage))
               .thenReturn(mockGmailImportService);
        Mockito.when(mockLabeler.getLabel(folderNames, EMAIL_ADDRESS)).thenReturn(mockLabel1);
        Mockito.when(mockLabeler.getAvailableLabel(GMailLabeler.PST_IMPORT_LABEL)).thenReturn(mockLabel2);
    }

    /**
     * Tests processEmailMessage in non-batch mode.  Currently processEmailMessage in batch mode
     * is not tested due to Google making methods final so we can't mock them.
     *
     * @throws IOException
     * @throws PSTException
     * @throws MessagingException
     */
    @Test
    public void testProcessEmailMessage() throws IOException, PSTException, MessagingException {
        messageHandler.processEmailMessage(mockPstMessage, "output", folderNames, EMAIL_ADDRESS);

        Mockito.verify(mockServiceFactory).getGmailService(Mockito.eq(EMAIL_ADDRESS));
        Mockito.verify(mockLabelFactory).getLabeler(Mockito.eq(mockGmailService), Mockito.eq(EMAIL_ADDRESS));
        Mockito.verify(mockExtractor).extractAttachments(Mockito.eq(mockPstMessage), Mockito.eq("output"));
        Mockito.verify(mockMessageConverter)
               .convertMessage(Mockito.eq(mockPstMessage), Mockito.eq("output"), Mockito.eq(attachmentList));
        Mockito.verify(mockLabeler).getLabel(Mockito.eq(folderNames), Mockito.eq(EMAIL_ADDRESS));
        Mockito.verify(mockGmailService).users();
        Mockito.verify(mockGmailUsersSerivce).messages();
        Mockito.verify(mockGmailMessagesService).gmailImport(Mockito.eq(EMAIL_ADDRESS), Mockito.eq(mockGmailMessage));
        Mockito.verify(mockGmailImportService).execute();

        Assert.assertTrue(mockGmailMessage.getLabelIds().contains("label-1"));
        Assert.assertTrue(mockGmailMessage.getLabelIds().contains("label-2"));
    }

    @Test
    public void testProcessEmailMessageLabelerError() throws IOException, PSTException, MessagingException {
        Mockito.when(mockLabeler.getLabel(folderNames, EMAIL_ADDRESS)).thenThrow(new IOException());
        messageHandler.processEmailMessage(mockPstMessage, "output", folderNames, EMAIL_ADDRESS);

        Mockito.verify(mockServiceFactory).getGmailService(Mockito.eq(EMAIL_ADDRESS));
        Mockito.verify(mockLabeler).getLabel(Mockito.eq(folderNames), Mockito.eq(EMAIL_ADDRESS));
        Mockito.verifyZeroInteractions(mockGmailImportService);
    }

    @Test
    public void testProcessEmailMessageGmailServiceFactoryError() throws IOException, PSTException, MessagingException {
        Mockito.when(mockServiceFactory.getGmailService(EMAIL_ADDRESS)).thenThrow(new IOException());
        messageHandler.processEmailMessage(mockPstMessage, "output", folderNames, EMAIL_ADDRESS);

        Mockito.verify(mockServiceFactory).getGmailService(Mockito.eq(EMAIL_ADDRESS));
        Mockito.verifyZeroInteractions(mockLabelFactory);
        Mockito.verifyZeroInteractions(mockGmailImportService);
    }

    @Test
    public void testProcessEmailMessageNullLabeler() throws IOException, PSTException, MessagingException {
        Mockito.when(mockLabeler.getLabel(folderNames, EMAIL_ADDRESS)).thenReturn(null);
        messageHandler.processEmailMessage(mockPstMessage, "output", folderNames, EMAIL_ADDRESS);

        Mockito.verify(mockServiceFactory).getGmailService(Mockito.eq(EMAIL_ADDRESS));
        Mockito.verify(mockLabeler).getLabel(Mockito.eq(folderNames), Mockito.eq(EMAIL_ADDRESS));
        Mockito.verifyZeroInteractions(mockGmailImportService);
    }
}