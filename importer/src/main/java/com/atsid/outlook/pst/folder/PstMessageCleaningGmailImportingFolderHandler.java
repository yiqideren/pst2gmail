package com.atsid.outlook.pst.folder;

import com.atsid.outlook.app.ProgressUpdate;
import com.atsid.outlook.pst.message.PstMessageHandler;
import com.pff.PSTAppointment;
import com.pff.PSTContact;
import com.pff.PSTFolder;
import com.pff.PSTMessage;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Imports messages from PST folder into GMail.
 */
@Component("1pstMessageCleaningGmailImportingFolderHandler")
@Scope("prototype")
@Log4j
public class PstMessageCleaningGMailImportingFolderHandler implements PstFolderHandler {
    @Getter
    @Setter
    private String emailAddress;
    @Getter
    @Setter
    private String outputPath;
    @Setter
    private ProgressUpdate progressUpdate;
    @Setter
    private PstMessageHandler messageHandler;
    @Setter
    int count = 0;

    @Override
    public void processPstFolder(PSTFolder folder, Boolean rootFolder, List<String> folderNames) {
        if (folder.getContentCount() > 0) {
            PSTMessage message = getNextEmail(folder);

            while (message != null) {
                processMessage(folderNames, message);

                message = getNextEmail(folder);
            }
        }
    }

    /**
     * Processes an individual message.  If any errors are encountered during import an error is logged and import continues.
     *
     * @param folderNames List of folder names where this message resides
     * @param message     Message to be imported
     */
    private void processMessage(List<String> folderNames, PSTMessage message) {
        if (PSTContact.class.isAssignableFrom(message.getClass()) ||
            PSTAppointment.class.isAssignableFrom(message.getClass())) {
            System.out.println("Found non-message item " + message.getClass().toString());
        } else {
            count++;
            try {
                messageHandler.processEmailMessage(message, outputPath, folderNames, emailAddress);
            } catch (RuntimeException re) {
                log.error(String.format("Caught runtime exception while processing message %d for %s",
                        message.getDescriptorNodeId(), emailAddress), re);
            }
            progressUpdate.updateProgress(count);
        }
    }

    /**
     * Gets the next folder message entry from the folder.
     *
     * @param folder Folder to get next email message from.
     * @return Returns the next <code>PSTMessage</code> in this folder or null if none exist or an error was encounteredß
     */
    private PSTMessage getNextEmail(PSTFolder folder) {
        try {
            return (PSTMessage) folder.getNextChild();
        } catch (Exception ex) {
            log.error(String.format("Caught exception while processing folder %s", folder.getDisplayName()), ex);
            return null;
        }
    }
}