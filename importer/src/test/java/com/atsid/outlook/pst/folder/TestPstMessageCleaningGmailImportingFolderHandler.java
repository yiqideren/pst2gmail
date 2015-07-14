package com.atsid.outlook.pst.folder;

import com.atsid.outlook.pst.ProgressUpdate;
import com.atsid.outlook.pst.folder.PstMessageCleaningGmailImportingFolderHandler;
import com.atsid.outlook.pst.message.PstMessageHandler;
import com.pff.PSTAppointment;
import com.pff.PSTContact;
import com.pff.PSTException;
import com.pff.PSTFolder;
import com.pff.PSTMessage;
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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:applicationContext-test.xml" })
public class TestPstMessageCleaningGmailImportingFolderHandler {
    @Mock
    private ProgressUpdate mockProgressUpdate;
    @Mock
    private PstMessageHandler mockMessageHandler;
    @Mock
    private PSTFolder mockFolder;
    @Mock
    private PSTContact mockContact;
    @Mock
    private PSTAppointment mockAppointment;
    @Mock
    private PSTMessage mockMessage1;
    @Mock
    private PSTMessage mockMessage2;
    @Mock
    private PSTMessage mockMessage3;
    private List<String> folderNames;
    @Autowired
    @InjectMocks
    private PstMessageCleaningGmailImportingFolderHandler folderHandler;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        folderHandler.setEmailAddress("test@example.com");
        folderHandler.setOutputPath("outputPath");
        folderNames = Arrays.asList("a", "b");
        folderHandler.setCount(0);
    }

    @Test
    public void testProcessPstFolderEmptyFolder() {
        Mockito.when(mockFolder.getContentCount()).thenReturn(0);

        folderHandler.processPstFolder(mockFolder, Boolean.TRUE, folderNames);

        Mockito.verify(mockFolder).getContentCount();
        Mockito.verifyNoMoreInteractions(mockFolder);
        Mockito.verifyZeroInteractions(mockMessageHandler);
        Mockito.verifyZeroInteractions(mockProgressUpdate);
    }

    @Test
    public void testProcessPstFolderWithContacts() throws PSTException, IOException {
        Mockito.when(mockFolder.getContentCount()).thenReturn(2);
        Mockito.when(mockFolder.getNextChild()).thenReturn(mockContact, mockMessage1, null);

        folderHandler.processPstFolder(mockFolder, Boolean.TRUE, folderNames);

        Mockito.verify(mockFolder).getContentCount();
        Mockito.verify(mockFolder, Mockito.atLeast(2)).getNextChild();
        Mockito.verify(mockMessageHandler)
               .processEmailMessage(Mockito.eq(mockMessage1), Mockito.eq("outputPath"), Mockito.eq(folderNames),
                       Mockito.eq("test@example.com"));
        Mockito.verify(mockProgressUpdate).updateProgress(1);
        Mockito.verifyNoMoreInteractions(mockMessageHandler);
        Mockito.verifyNoMoreInteractions(mockProgressUpdate);
    }

    @Test
    public void testProcessPstFolderWithAppointments() throws PSTException, IOException {
        Mockito.when(mockFolder.getContentCount()).thenReturn(2);
        Mockito.when(mockFolder.getNextChild()).thenReturn(mockAppointment, mockMessage1, null);

        folderHandler.processPstFolder(mockFolder, Boolean.TRUE, folderNames);

        Mockito.verify(mockFolder).getContentCount();
        Mockito.verify(mockFolder, Mockito.atLeast(2)).getNextChild();
        Mockito.verify(mockMessageHandler)
               .processEmailMessage(Mockito.eq(mockMessage1), Mockito.eq("outputPath"), Mockito.eq(folderNames),
                       Mockito.eq("test@example.com"));
        Mockito.verify(mockProgressUpdate).updateProgress(1);
        Mockito.verifyNoMoreInteractions(mockMessageHandler);
        Mockito.verifyNoMoreInteractions(mockProgressUpdate);
    }

    @Test
    public void testProcessPstFolderMessageHandlerError() throws PSTException, IOException {
        Mockito.when(mockFolder.getContentCount()).thenReturn(3);
        Mockito.when(mockFolder.getNextChild()).thenReturn(mockMessage1, mockMessage2, mockMessage3, null);
        Mockito.doThrow(new RuntimeException()).when(mockMessageHandler)
               .processEmailMessage(mockMessage2, "outputPath", folderNames, "test@example.com");

        folderHandler.processPstFolder(mockFolder, Boolean.TRUE, folderNames);

        Mockito.verify(mockFolder).getContentCount();
        Mockito.verify(mockFolder, Mockito.atLeast(3)).getNextChild();
        Mockito.verify(mockMessageHandler)
               .processEmailMessage(Mockito.eq(mockMessage1), Mockito.eq("outputPath"), Mockito.eq(folderNames),
                       Mockito.eq("test@example.com"));
        Mockito.verify(mockMessageHandler)
               .processEmailMessage(Mockito.eq(mockMessage2), Mockito.eq("outputPath"), Mockito.eq(folderNames),
                       Mockito.eq("test@example.com"));
        Mockito.verify(mockMessageHandler)
               .processEmailMessage(Mockito.eq(mockMessage3), Mockito.eq("outputPath"), Mockito.eq(folderNames),
                       Mockito.eq("test@example.com"));
        Mockito.verify(mockProgressUpdate).updateProgress(1);
        Mockito.verify(mockProgressUpdate).updateProgress(2);
        Mockito.verify(mockProgressUpdate).updateProgress(3);
        Mockito.verifyNoMoreInteractions(mockMessageHandler);
        Mockito.verifyNoMoreInteractions(mockProgressUpdate);
    }

    @Test
    public void testProcessPstFolderExceptionFirstMessageRetrieval() throws PSTException, IOException {
        Mockito.when(mockFolder.getContentCount()).thenReturn(3);
        Mockito.when(mockFolder.getNextChild()).thenThrow(new IOException());

        folderHandler.processPstFolder(mockFolder, Boolean.TRUE, folderNames);

        Mockito.verify(mockFolder).getContentCount();
        Mockito.verify(mockFolder).getNextChild();
        Mockito.verifyZeroInteractions(mockProgressUpdate);
        Mockito.verifyZeroInteractions(mockMessageHandler);
    }

    @Test
    public void testProcessPstFolderExceptionSecondMessageRetrieval() throws PSTException, IOException {
        Mockito.when(mockFolder.getContentCount()).thenReturn(3);
        Mockito.when(mockFolder.getNextChild()).thenReturn(mockMessage1).thenThrow(new IOException());

        folderHandler.processPstFolder(mockFolder, Boolean.TRUE, folderNames);

        Mockito.verify(mockFolder).getContentCount();
        Mockito.verify(mockFolder, Mockito.times(2)).getNextChild();
        Mockito.verify(mockMessageHandler)
               .processEmailMessage(Mockito.eq(mockMessage1), Mockito.eq("outputPath"), Mockito.eq(folderNames),
                       Mockito.eq("test@example.com"));
        Mockito.verify(mockProgressUpdate).updateProgress(1);
        Mockito.verifyNoMoreInteractions(mockMessageHandler);
        Mockito.verifyNoMoreInteractions(mockProgressUpdate);
    }

    @Test
    public void testProcessPstFolder() throws PSTException, IOException {
        Mockito.when(mockFolder.getContentCount()).thenReturn(3);
        Mockito.when(mockFolder.getNextChild()).thenReturn(mockMessage1, mockMessage2, mockMessage3, null);

        folderHandler.processPstFolder(mockFolder, Boolean.TRUE, folderNames);

        Mockito.verify(mockFolder).getContentCount();
        Mockito.verify(mockFolder, Mockito.atLeast(3)).getNextChild();
        Mockito.verify(mockMessageHandler)
               .processEmailMessage(Mockito.eq(mockMessage1), Mockito.eq("outputPath"), Mockito.eq(folderNames),
                       Mockito.eq("test@example.com"));
        Mockito.verify(mockMessageHandler)
               .processEmailMessage(Mockito.eq(mockMessage2), Mockito.eq("outputPath"), Mockito.eq(folderNames),
                       Mockito.eq("test@example.com"));
        Mockito.verify(mockMessageHandler)
               .processEmailMessage(Mockito.eq(mockMessage3), Mockito.eq("outputPath"), Mockito.eq(folderNames),
                       Mockito.eq("test@example.com"));
        Mockito.verify(mockProgressUpdate).updateProgress(1);
        Mockito.verify(mockProgressUpdate).updateProgress(2);
        Mockito.verify(mockProgressUpdate).updateProgress(3);
        Mockito.verifyNoMoreInteractions(mockMessageHandler);
        Mockito.verifyNoMoreInteractions(mockProgressUpdate);
    }
}