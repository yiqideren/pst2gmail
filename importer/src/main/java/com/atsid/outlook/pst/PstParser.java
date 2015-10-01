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

/**
 * Parses Microsoft Outlook PST files.
 */
@Component
public class PstParser implements ApplicationContextAware {
    @Setter
    private ApplicationContext applicationContext;
    @Value("${ignored.folders}")
    private String ignoredFolderString;
    private List<String> ignoredFolders;

    /**
     * Helper method called after string injects properties to populate the list of ignored folders.
     */
    @PostConstruct
    private void createIgnoredFoldersList() {
        String[] folders = ignoredFolderString.split(",");
        ignoredFolders = new ArrayList<>();

        for (String folder : folders) {
            ignoredFolders.add(folder.trim());
        }
    }

    /**
     * Counts the number of emails in the specified PST file, ignoring folders listed in ignored.folders property.
     *
     * @param pstFileName Full file path to the PST file to count messages
     * @return Returns the number of email messages in the specified PST file
     * @throws IOException
     * @throws PSTException
     */
    public int countEmails(String pstFileName) throws IOException, PSTException {
        PstMessageCountingFolderHandler folderHandler = getCountingHandler();
        PSTFile pstFile = openPstFile(pstFileName);

        processPstFolder(pstFile.getRootFolder(), folderHandler, Boolean.TRUE, new ArrayList<String>());

        return folderHandler.getMessageCount();
    }

    /**
     * Processes the PST file and executes the messageHandler for each message encountered ignoring folders listed in
     * ignored.folders property..
     *
     * @param pstFileName    Full file path to the PST file to process
     * @param emailAddress   Email address of user who this PST file belongs to
     * @param outputPath     Full path to a folder on the filesystem where content is placed
     * @param messageHandler Handler to call for each message that is encountered
     * @param progressUpdate Progress update handler to call to indicate progress has been made
     * @throws IOException
     * @throws PSTException
     */
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

    /**
     * Helper method used to recursively process folders in the PST file ignoring folders identified in ignored.folders
     * property.
     *
     * @param folder        PST Folder to process
     * @param folderHandler Handler to be called to process each folder
     * @param rootFolder    Root folder in the PST file
     * @param folderNames   List of folder names leading up to the current folder that acts as a path to this folder
     * @throws PSTException
     * @throws IOException
     */
    private void processPstFolder(PSTFolder folder, PstFolderHandler folderHandler, Boolean rootFolder,
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

    /**
     * Helper method used to identify if a folder should not be checked for a label.
     *
     * @param folder PST folder to determine if it should be excluded from checking labels
     * @return Returns true if it should be ignored, false otherwise
     */
    private boolean ignoreFolderLabel(PSTFolder folder) {
        List<String> noLabelFolders = Arrays.asList("Top of Information Store", "");

        return noLabelFolders.contains(folder.getDisplayName());
    }

    /**
     * Helper method to determine if folder should not be ignored.
     *
     * @param folder PST folder to determine if it should not be ignored
     * @return Returns true if folder should not be ignored, false otherwise
     */
    private boolean folderIsNotIgnored(PSTFolder folder) {
        return !ignoredFolders.contains(folder.getDisplayName());
    }

    /**
     * Helper method used to retrieve folder processing handler from spring context.
     *
     * @return Returns folder processing handler
     */
    private PstMessageCleaningGmailImportingFolderHandler getProcessingHandler() {
        return applicationContext.getBean(PstMessageCleaningGmailImportingFolderHandler.class);
    }

    /**
     * Helper method used to retrieve folder counting processing handler from spring context.
     *
     * @return Returns folder counting processing handler
     */
    private PstMessageCountingFolderHandler getCountingHandler() {
        return applicationContext.getBean(PstMessageCountingFolderHandler.class);
    }

    /**
     * Opens up PST file for reading.
     *
     * @param pstFile PST file we are opening for processing
     * @return Returns a <code>PSTFile</code> instance opened on the specified PST file
     * @throws IOException
     * @throws PSTException
     */
    private PSTFile openPstFile(String pstFile) throws IOException, PSTException {
        return new PSTFile(pstFile);
    }
}