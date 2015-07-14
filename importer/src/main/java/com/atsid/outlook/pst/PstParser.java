package com.atsid.outlook.pst;

import com.atsid.outlook.app.ProgressUpdate;
import com.atsid.outlook.pst.folder.PstFolderHandler;
import com.atsid.outlook.pst.folder.PstMessageCleaningGmailImportingFolderHandler;
import com.atsid.outlook.pst.folder.PstMessageCountingFolderHandler;
import com.atsid.outlook.pst.message.PstMessageHandler;
import com.pff.PSTException;
import com.pff.PSTFile;
import com.pff.PSTFolder;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class PstParser implements ApplicationContextAware {
    @Setter
    private ApplicationContext applicationContext;
    @Value("${ignored.folders}")
    private String ignoredFolderString;
    private List<String> ignoredFolders;

    @PostConstruct
    private void createIgnoredFoldersList() {
        String[] folders = ignoredFolderString.split(",");
        ignoredFolders = new ArrayList<>();

        for (String folder : folders) {
            ignoredFolders.add(folder.trim());
        }
    }

    public int countEmails(String pstFileName) throws IOException, PSTException {
        PstMessageCountingFolderHandler folderHandler = getCountingHandler();
        PSTFile pstFile = openPstFile(pstFileName);

        processPstFolder(pstFile.getRootFolder(), folderHandler, Boolean.TRUE, new ArrayList<String>());

        return folderHandler.getMessageCount();
    }

    public void processPst(String pstFileName, String emailAddress, String outputPath, PstMessageHandler messageHandler,
            ProgressUpdate progressUpdate) throws IOException, PSTException {
        PstMessageCleaningGmailImportingFolderHandler folderHandler = getProcessingHandler();
        PSTFile pstFile = openPstFile(pstFileName);

        folderHandler.setEmailAddress(emailAddress);
        folderHandler.setOutputPath(outputPath);
        folderHandler.setMessageHandler(messageHandler);
        folderHandler.setProgressUpdate(progressUpdate);

        processPstFolder(pstFile.getRootFolder(), folderHandler, Boolean.TRUE, new ArrayList<String>());
    }

    public void processPstFolder(PSTFolder folder, PstFolderHandler folderHandler, Boolean rootFolder,
            List<String> folderNames) throws PSTException, IOException {
        if (folder.hasSubfolders() && folderIsNotIgnored(folder)) {
            if (!ignoreFolderLabel(folder)) {
                folderNames.add(folder.getDisplayName());
            }

            for (PSTFolder childFolder : folder.getSubFolders()) {
                processPstFolder(childFolder, folderHandler, Boolean.FALSE, folderNames);
            }

            folderHandler.processPstFolder(folder, rootFolder, folderNames);

            if (!ignoreFolderLabel(folder)) {
                folderNames.remove(folder.getDisplayName());
            }
        } else if (folderIsNotIgnored(folder)) {
            if (!ignoreFolderLabel(folder)) {
                folderNames.add(folder.getDisplayName());
            }

            folderHandler.processPstFolder(folder, rootFolder, folderNames);

            if (!ignoreFolderLabel(folder)) {
                folderNames.remove(folder.getDisplayName());
            }
        }
    }

    private boolean ignoreFolderLabel(PSTFolder folder) {
        List<String> noLabelFolders = Arrays.asList("Top of Information Store", "");

        return noLabelFolders.contains(folder.getDisplayName());
    }

    private boolean folderIsNotIgnored(PSTFolder folder) {
        return !ignoredFolders.contains(folder.getDisplayName());
    }

    private PstMessageCleaningGmailImportingFolderHandler getProcessingHandler() {
        return applicationContext.getBean(PstMessageCleaningGmailImportingFolderHandler.class);
    }

    private PstMessageCountingFolderHandler getCountingHandler() {
        return applicationContext.getBean(PstMessageCountingFolderHandler.class);
    }

    private PSTFile openPstFile(String pstFile) throws IOException, PSTException {
        return new PSTFile(pstFile);
    }
}