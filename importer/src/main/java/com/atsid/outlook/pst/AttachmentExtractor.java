package com.atsid.outlook.pst;

import com.pff.PSTAttachment;
import com.pff.PSTMessage;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Extracts email attachments from a <code>PSTMessage</code> and saves them to a folder on disk.
 */
@Component
public class AttachmentExtractor {
    @Autowired
    private AttachmentLogger attachmentLogger;

    /**
     * Extracts attachments from a PST email message and saves them in the specified output folder.
     *
     * @param message    PSTMessage to extract attachments from
     * @param outputPath Output folder to place attachments in
     * @return Returns true if attachments were extracted, false otherwise
     */
    public List<String> extractAttachments(PSTMessage message, String outputPath) {
        List<String> removedAttachments = new ArrayList<>();
        Long emailId = message.getDescriptorNodeId();

        if (message.getNumberOfAttachments() > 0) {
            for (int i = 0; i < message.getNumberOfAttachments(); ++i) {
                PSTAttachment attachment = null;

                try {
                    attachment = message.getAttachment(i);
                    String fileName = attachment.getLongFilename();

                    extractAttachment(attachment.getFileInputStream(), emailId, fileName, outputPath);
                    attachmentLogger.logAttachmentRemoved(emailId, fileName, attachment.getDisplayName(), outputPath);
                    removedAttachments.add(emailId + File.separator + fileName);
                } catch (Exception ex) {
                    String fileName = attachment == null ? "" : attachment.getLongFilename();
                    String displayName = attachment == null ? "" : attachment.getDisplayName();

                    attachmentLogger.logAttachmentError(emailId, fileName, displayName, outputPath, ex);
                }
            }
        }

        return removedAttachments;
    }

    /**
     * Copies an attachment out to a file on the filesystem using the emailId as a key folder.
     *
     * @param stream     Attachment input stream
     * @param emailId    Email id to use as key for storage location
     * @param fileName   Name of attachment on disk
     * @param outputPath The absolute path to where content gets written on disk
     * @throws IOException Throws if there is an issue copying the file using IOUtils
     */
    private void extractAttachment(InputStream stream, Long emailId, String fileName, String outputPath)
            throws IOException {
        File outputDirectory = new File(outputPath + File.separator + emailId);

        outputDirectory.mkdirs();

        try (FileOutputStream output = new FileOutputStream(
                new File(outputPath + File.separator + emailId + File.separator + fileName))) {
            IOUtils.copy(stream, output);
        } finally {
            IOUtils.closeQuietly(stream);
        }
    }
}