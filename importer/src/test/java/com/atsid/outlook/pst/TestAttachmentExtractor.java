package com.atsid.outlook.pst;

import com.google.common.io.Files;
import com.pff.PSTAttachment;
import com.pff.PSTException;
import com.pff.PSTMessage;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:applicationContext-test.xml" })
public class TestAttachmentExtractor {
    @Mock
    private AttachmentLogger mockLogger;
    @Mock
    private PSTMessage mockMessage;
    @Mock
    private PSTAttachment mockAttachment;
    private InputStream inputStream;
    private File tmpDir;
    @Autowired
    @InjectMocks
    private AttachmentExtractor extractor;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        inputStream = getClass().getClassLoader().getResourceAsStream("test-file.txt");
        tmpDir = Files.createTempDir();
    }

    @After
    public void teardown() {
        IOUtils.closeQuietly(inputStream);
        tmpDir.delete();
    }

    @Test
    public void testExtractAttachmentIoException() throws IOException, PSTException {
        IOException ex = new IOException();
        Mockito.when(mockMessage.getNumberOfAttachments()).thenReturn(1);
        Mockito.when(mockMessage.getAttachment(0)).thenReturn(mockAttachment);
        Mockito.when(mockMessage.getDescriptorNodeId()).thenReturn(1L);
        Mockito.when(mockAttachment.getFileInputStream()).thenThrow(ex);
        Mockito.when(mockAttachment.getLongFilename()).thenReturn("test-file.txt");
        Mockito.when(mockAttachment.getDisplayName()).thenReturn("test-file");

        extractor.extractAttachments(mockMessage, tmpDir.getAbsolutePath());

        Mockito.verify(mockMessage).getAttachment(Mockito.eq(0));
        Mockito.verify(mockAttachment).getFileInputStream();
        Mockito.verify(mockLogger)
               .logAttachmentError(Mockito.eq(1L), Mockito.eq("test-file.txt"), Mockito.eq("test-file"),
                       Mockito.eq(tmpDir.getAbsolutePath()), Mockito.eq(ex));
    }

    @Test
    public void testExtractAttachmentNoAttachments() {
        Mockito.when(mockMessage.getNumberOfAttachments()).thenReturn(0);
        Mockito.when(mockMessage.getDescriptorNodeId()).thenReturn(1L);

        extractor.extractAttachments(mockMessage, tmpDir.getAbsolutePath());

        Mockito.verify(mockMessage).getDescriptorNodeId();
        Mockito.verify(mockMessage).getNumberOfAttachments();
        Mockito.verifyNoMoreInteractions(mockMessage);
        Mockito.verifyZeroInteractions(mockAttachment);
        Mockito.verifyZeroInteractions(mockLogger);
    }

    @Test
    public void testExtractAttachment() throws PSTException, IOException {
        Mockito.when(mockMessage.getNumberOfAttachments()).thenReturn(1);
        Mockito.when(mockMessage.getAttachment(0)).thenReturn(mockAttachment);
        Mockito.when(mockMessage.getDescriptorNodeId()).thenReturn(1L);
        Mockito.when(mockAttachment.getFileInputStream()).thenReturn(inputStream);
        Mockito.when(mockAttachment.getLongFilename()).thenReturn("test-file.txt");
        Mockito.when(mockAttachment.getDisplayName()).thenReturn("test-file");

        List<String> attachments = extractor.extractAttachments(mockMessage, tmpDir.getAbsolutePath());

        Mockito.verify(mockMessage).getAttachment(Mockito.eq(0));
        Mockito.verify(mockAttachment).getFileInputStream();
        Mockito.verify(mockLogger)
               .logAttachmentRemoved(Mockito.eq(1L), Mockito.eq("test-file.txt"), Mockito.eq("test-file"),
                       Mockito.eq(tmpDir.getAbsolutePath()));

        String fileContents = FileUtils.readFileToString(
                new File(tmpDir.getAbsoluteFile() + File.separator + "1" + File.separator + "test-file.txt"));
        Assert.assertEquals("This is a test", fileContents);
        Assert.assertEquals(1, attachments.size());
        Assert.assertTrue(attachments.contains("1" + File.separator + "test-file.txt"));
    }
}