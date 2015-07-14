package com.atsid.outlook.pst.message;

import com.google.api.services.gmail.Gmail;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:applicationContext-test.xml" })
public class TestGMailLabelFactory {
    @Mock
    private Gmail mockGmailService;
    @Mock
    private ApplicationContext mockApplicationContext;
    @Mock
    private GMailLabeler mockLabeler;
    @Autowired
    @InjectMocks
    private GMailLabelFactory labelFactory;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        labelFactory.getLabelers().clear();
    }

    @Test
    public void testGetLabelerNotExist() throws IOException {
        Mockito.when(mockApplicationContext.getBean("gmailLabeler", mockGmailService, "testing"))
               .thenReturn(mockLabeler);

        GMailLabeler result = labelFactory.getLabeler(mockGmailService, "testing");

        Assert.assertNotNull(result);
        Assert.assertSame(mockLabeler, result);
        Mockito.verify(mockLabeler).loadLabels();
    }

    @Test
    public void testGetLabelerCached() throws IOException {
        Mockito.when(mockApplicationContext.getBean("gmailLabeler", mockGmailService, "testing"))
               .thenReturn(mockLabeler);

        GMailLabeler result1 = labelFactory.getLabeler(mockGmailService, "testing");

        Assert.assertNotNull(result1);
        Assert.assertSame(mockLabeler, result1);
        Mockito.reset(mockApplicationContext, mockLabeler, mockGmailService);

        GMailLabeler result2 = labelFactory.getLabeler(mockGmailService, "testing");

        Assert.assertNotNull(result2);
        Assert.assertSame(mockLabeler, result2);
        Mockito.verifyZeroInteractions(mockApplicationContext);
        Mockito.verifyZeroInteractions(mockLabeler);
        Mockito.verifyZeroInteractions(mockGmailService);
    }

    @Test
    public void testGetLabelerFailedInit() throws IOException {
        Mockito.when(mockApplicationContext.getBean("gmailLabeler", mockGmailService, "testing"))
               .thenReturn(mockLabeler);
        Mockito.doThrow(new IOException()).when(mockLabeler).loadLabels();

        GMailLabeler result = labelFactory.getLabeler(mockGmailService, "testing");

        Assert.assertNull(result);
        Mockito.verify(mockLabeler).loadLabels();
    }
}