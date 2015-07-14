package com.atsid.outlook.pst.folder;

import com.pff.PSTFolder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:applicationContext-test.xml" })
public class TestPstMessageCountingFolderHandler {
    @Autowired
    @Qualifier("1pstMessageCountingFolderHandler")
    private PstMessageCountingFolderHandler folderHandler;
    @Mock
    private PSTFolder pstFolder;
    private int startingCount;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        Mockito.when(pstFolder.getContentCount()).thenReturn(5);
        startingCount = folderHandler.getMessageCount();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testProcessPstFolderNullFolder() {
        folderHandler.processPstFolder(null, Boolean.TRUE, new ArrayList<String>());
    }

    @Test
    public void testProcessPstFolderRootFolderWithSubs() {
        Mockito.when(pstFolder.hasSubfolders()).thenReturn(true);

        folderHandler.processPstFolder(pstFolder, Boolean.TRUE, new ArrayList<String>());

        Mockito.verify(pstFolder).hasSubfolders();
        Assert.assertEquals(startingCount, folderHandler.getMessageCount());
    }

    @Test
    public void testProcessPstFolderRootFolderWithoutSubs() {
        Mockito.when(pstFolder.hasSubfolders()).thenReturn(false);

        folderHandler.processPstFolder(pstFolder, Boolean.TRUE, new ArrayList<String>());

        Mockito.verify(pstFolder).hasSubfolders();
        Mockito.verify(pstFolder).getContentCount();
        Assert.assertEquals(startingCount + 5, folderHandler.getMessageCount());
    }

    @Test
    public void testProcessPstFolderNonRootFolderWithSubs() {
        Mockito.when(pstFolder.hasSubfolders()).thenReturn(true);

        folderHandler.processPstFolder(pstFolder, Boolean.FALSE, new ArrayList<String>());

        Mockito.verify(pstFolder).getContentCount();
        Assert.assertEquals(startingCount + 5, folderHandler.getMessageCount());
    }

    @Test
    public void testProcessPstFolderNonRootFolderWithoutSubs() {
        Mockito.when(pstFolder.hasSubfolders()).thenReturn(false);

        folderHandler.processPstFolder(pstFolder, Boolean.FALSE, new ArrayList<String>());

        Mockito.verify(pstFolder).getContentCount();
        Assert.assertEquals(startingCount + 5, folderHandler.getMessageCount());
    }
}