package com.atsid.outlook.pst.message;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.ListLabelsResponse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:applicationContext-test.xml" })
public class TestGMailLabeler {
    @Mock
    private Gmail mockGmailService;
    @Mock
    private Gmail.Users mockGmailUsersSerivce;
    @Mock
    private Gmail.Users.Labels mockGmailLabelsService;
    @Mock
    private Gmail.Users.Labels.List mockLabelsListService;
    @Mock
    private Gmail.Users.Labels.Create mockLabelsCreateService;
    private Label label1;
    private Label label2;
    private ListLabelsResponse listLabelsResponse;
    @Autowired
    private GMailLabeler labeler;

    @Before
    public void setup() throws IOException {
        MockitoAnnotations.initMocks(this);
        listLabelsResponse = new ListLabelsResponse();
        label1 = new Label();
        label2 = new Label();
        labeler.getAvailableLabels().clear();
        labeler.setEmailAddress("test@example.com");
        labeler.setGmailService(mockGmailService);
        label1.setId("1");
        label1.setName("Label 1");
        label2.setId("2");
        label2.setName("Label 2");
        Mockito.when(mockGmailService.users()).thenReturn(mockGmailUsersSerivce);
        Mockito.when(mockGmailUsersSerivce.labels()).thenReturn(mockGmailLabelsService);
        Mockito.when(mockGmailLabelsService.list("test@example.com")).thenReturn(mockLabelsListService);
    }

    @Test
    public void testLoadLabelsNoLabels() throws IOException {
        Mockito.when(mockLabelsListService.execute()).thenReturn(listLabelsResponse);
        listLabelsResponse.setLabels(new ArrayList<Label>());

        labeler.loadLabels();

        Mockito.verify(mockGmailService).users();
        Mockito.verify(mockGmailUsersSerivce).labels();
        Mockito.verify(mockGmailLabelsService).list(Mockito.eq("test@example.com"));
        Mockito.verify(mockLabelsListService).execute();

        Assert.assertEquals(0, labeler.getAvailableLabels().size());
    }

    @Test
    public void testLoadLabel() throws IOException {
        Mockito.when(mockLabelsListService.execute()).thenReturn(listLabelsResponse);
        listLabelsResponse.setLabels(new ArrayList<Label>());
        listLabelsResponse.getLabels().add(label1);
        listLabelsResponse.getLabels().add(label2);

        labeler.loadLabels();

        Mockito.verify(mockGmailService).users();
        Mockito.verify(mockGmailUsersSerivce).labels();
        Mockito.verify(mockGmailLabelsService).list(Mockito.eq("test@example.com"));
        Mockito.verify(mockLabelsListService).execute();

        Assert.assertEquals(2, labeler.getAvailableLabels().size());
        Assert.assertSame(label1, labeler.getAvailableLabels().get("Label 1"));
        Assert.assertSame(label2, labeler.getAvailableLabels().get("Label 2"));
    }

    @Test(expected = IOException.class)
    public void testLoadLabelsIOException() throws IOException {
        Mockito.when(mockLabelsListService.execute()).thenThrow(new IOException());
        listLabelsResponse.setLabels(new ArrayList<Label>());

        labeler.loadLabels();
    }

    @Test
    public void testGetAvailableLabel() {
        labeler.getAvailableLabels().put(GMailLabeler.PST_IMPORT_LABEL, label2);

        Assert.assertEquals(label2, labeler.getAvailableLabel(GMailLabeler.PST_IMPORT_LABEL));
    }

    @Test
    public void testGetLabelNotExist() throws IOException {
        List<String> folders = Arrays.asList("Label 1");
        labeler.getAvailableLabels().put(GMailLabeler.PST_IMPORT_LABEL, label2);
        Mockito.doAnswer(new Answer<Label>() {
            @Override
            public Label answer(InvocationOnMock invocation) throws Throwable {
                return label1;
            }
        }).when(mockLabelsCreateService).execute();
        Mockito.doAnswer(new Answer<Gmail.Users.Labels.Create>() {
            @Override
            public Gmail.Users.Labels.Create answer(InvocationOnMock invocation) throws Throwable {
                Label label = (Label) invocation.getArguments()[1];
                Assert.assertEquals("hide", label.getMessageListVisibility());
                Assert.assertEquals("labelShow", label.getLabelListVisibility());
                Assert.assertEquals("Label 1", label.getName());
                return mockLabelsCreateService;
            }
        }).when(mockGmailLabelsService).create(Mockito.eq("test@example.com"), Mockito.any(Label.class));

        Label result = labeler.getLabel(folders, "test@example.com");

        Assert.assertNotNull(result);
        Assert.assertSame(label1, result);
        Assert.assertSame(result, labeler.getAvailableLabels().get("Label 1"));

        Mockito.verify(mockGmailService).users();
        Mockito.verify(mockGmailUsersSerivce).labels();
        Mockito.verify(mockGmailLabelsService).create(Mockito.eq("test@example.com"), Mockito.any(Label.class));
        Mockito.verify(mockLabelsCreateService).execute();
    }

    @Test
    public void testGetLabelAlreadyCreated() throws IOException {
        List<String> folders = Arrays.asList("Label 1");
        labeler.getAvailableLabels().put(GMailLabeler.PST_IMPORT_LABEL, label2);
        labeler.getAvailableLabels().put("Label 1", label1);

        Label result = labeler.getLabel(folders, "test@example.com");

        Assert.assertNotNull(result);
        Assert.assertSame(label1, result);
        Assert.assertSame(result, labeler.getAvailableLabels().get("Label 1"));

        Mockito.verifyZeroInteractions(mockGmailService);
        Mockito.verifyZeroInteractions(mockGmailUsersSerivce);
        Mockito.verifyZeroInteractions(mockGmailLabelsService);
        Mockito.verifyZeroInteractions(mockLabelsCreateService);
    }

    @Test
    public void testGetLabelNestedLabels() throws IOException {
        List<String> folders = Arrays.asList("Label 1", "a");
        labeler.getAvailableLabels().put(GMailLabeler.PST_IMPORT_LABEL, label2);
        Mockito.doAnswer(new Answer<Label>() {
            @Override
            public Label answer(InvocationOnMock invocation) throws Throwable {
                return label1;
            }
        }).when(mockLabelsCreateService).execute();
        Mockito.doAnswer(new Answer<Gmail.Users.Labels.Create>() {
            int call = 0;

            @Override
            public Gmail.Users.Labels.Create answer(InvocationOnMock invocation) throws Throwable {
                Label label = (Label) invocation.getArguments()[1];
                if (call == 0) {
                    Assert.assertEquals("hide", label.getMessageListVisibility());
                    Assert.assertEquals("labelShow", label.getLabelListVisibility());
                    Assert.assertEquals("Label 1", label.getName());
                } else if (call == 1) {
                    Assert.assertEquals("hide", label.getMessageListVisibility());
                    Assert.assertEquals("labelShow", label.getLabelListVisibility());
                    Assert.assertEquals("Label 1/a", label.getName());
                } else {
                    Assert.fail("Should not be here....");
                }
                ++call;

                return mockLabelsCreateService;
            }
        }).when(mockGmailLabelsService).create(Mockito.eq("test@example.com"), Mockito.any(Label.class));

        Label result = labeler.getLabel(folders, "test@example.com");

        Assert.assertNotNull(result);
        Assert.assertSame(label1, result);
        Assert.assertSame(result, labeler.getAvailableLabels().get("Label 1/a"));
        Assert.assertEquals(3, labeler.getAvailableLabels().size());
        Assert.assertSame(label1, labeler.getAvailableLabels().get("Label 1"));

        Mockito.verify(mockGmailService, Mockito.atLeast(1)).users();
        Mockito.verify(mockGmailUsersSerivce, Mockito.atLeast(1)).labels();
        Mockito.verify(mockGmailLabelsService, Mockito.times(2))
               .create(Mockito.eq("test@example.com"), Mockito.any(Label.class));
        Mockito.verify(mockLabelsCreateService, Mockito.times(2)).execute();
    }

    @Test
    public void testGetLabelIllegalCharacters() throws IOException {
        List<String> folders = Arrays.asList("Label/1-");
        labeler.getAvailableLabels().put(GMailLabeler.PST_IMPORT_LABEL, label2);
        Mockito.doAnswer(new Answer<Label>() {
            @Override
            public Label answer(InvocationOnMock invocation) throws Throwable {
                return label1;
            }
        }).when(mockLabelsCreateService).execute();
        Mockito.doAnswer(new Answer<Gmail.Users.Labels.Create>() {
            @Override
            public Gmail.Users.Labels.Create answer(InvocationOnMock invocation) throws Throwable {
                Label label = (Label) invocation.getArguments()[1];
                Assert.assertEquals("hide", label.getMessageListVisibility());
                Assert.assertEquals("labelShow", label.getLabelListVisibility());
                Assert.assertEquals("Label_1_", label.getName());
                return mockLabelsCreateService;
            }
        }).when(mockGmailLabelsService).create(Mockito.eq("test@example.com"), Mockito.any(Label.class));

        Label result = labeler.getLabel(folders, "test@example.com");

        Assert.assertNotNull(result);
        Assert.assertSame(label1, result);
        Assert.assertSame(result, labeler.getAvailableLabels().get("Label_1_"));

        Mockito.verify(mockGmailService).users();
        Mockito.verify(mockGmailUsersSerivce).labels();
        Mockito.verify(mockGmailLabelsService).create(Mockito.eq("test@example.com"), Mockito.any(Label.class));
        Mockito.verify(mockLabelsCreateService).execute();
    }

    @Test
    public void testGetLabelReserved() throws IOException {
        List<String> inboxFolders = Arrays.asList("Inbox");
        List<String> sentFolders = Arrays.asList("Sent Items");
        labeler.getAvailableLabels().put(GMailLabeler.PST_IMPORT_LABEL, label2);
        labeler.getAvailableLabels().put("INBOX", label1);
        labeler.getAvailableLabels().put("SENT", label2);

        Label result = labeler.getLabel(inboxFolders, "test@example.com");

        Assert.assertNotNull(result);
        Assert.assertSame(label1, result);
        Assert.assertSame(result, labeler.getAvailableLabels().get("INBOX"));

        Label result2 = labeler.getLabel(sentFolders, "test@example.com");

        Assert.assertNotNull(result2);
        Assert.assertSame(label2, result2);
        Assert.assertSame(result2, labeler.getAvailableLabels().get("SENT"));

        Mockito.verifyZeroInteractions(mockGmailService);
        Mockito.verifyZeroInteractions(mockGmailUsersSerivce);
        Mockito.verifyZeroInteractions(mockGmailLabelsService);
        Mockito.verifyZeroInteractions(mockLabelsCreateService);
    }
}