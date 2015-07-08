package com.atsid.outlook.pst;

import lombok.extern.log4j.Log4j;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

@Component
@Log4j
/**
 * Logger that outputs various information to indicate an attachment was stripped from an email or that an error was
 * encountered during the extraction process.
 */ public class AttachmentLogger {
    private static final String ATTACHMENT_LOG_STRING = "Extracted attachment %s (%s) from email %d%n";
    private static final String ERROR_LOG_STRING =
            "Error extracting attachment from %s (%s) from email %d with error %s%n";
    private static final String SENDER_ERROR_STRING =
            "Could not resolve sender %s to proper email address in email with id %d%n";

    /**
     * Logs an attachment extraction to an attachment.log file in the output path.
     *
     * @param emailId        Id of email from PST
     * @param fileName       File name of attachment
     * @param attachmentName Attachment name
     * @param outputPath     Location to write attachment.log file
     */
    public void logAttachmentRemoved(Long emailId, String fileName, String attachmentName, String outputPath) {
        try {
            FileUtils.writeStringToFile(getAttachmentLogFile(outputPath),
                    String.format(ATTACHMENT_LOG_STRING, fileName, attachmentName, emailId), true);
        } catch (IOException ioe) {
            log.error("Could not log attachment extraction due to error.", ioe);
        }
    }

    /**
     * Logs an attachment extraction error to an error.log file in the output path.
     *
     * @param emailId        Id of email from PST
     * @param fileName       File name of attachment if available
     * @param attachmentName Name of attachment if available
     * @param outputPath     Output path to write error.log file
     * @param exception      Exception encountered that caused extraction error.
     */
    public void logAttachmentError(Long emailId, String fileName, String attachmentName, String outputPath,
            Exception exception) {
        try {
            FileUtils.writeStringToFile(getErrorLogFile(outputPath),
                    String.format(ERROR_LOG_STRING, fileName, attachmentName, emailId, exception.getMessage()), true);
        } catch (IOException ioe) {
            log.error("Could not log attachment extraction error due to error.", ioe);
        }
    }

    public void logSenderError(Long emailId, String outputPath, String sender) {
        try {
            FileUtils.writeStringToFile(getErrorLogFile(outputPath),
                    String.format(SENDER_ERROR_STRING, sender, emailId));
        } catch (IOException ioe) {
            log.error("Could not log sender error due to error.", ioe);
        }
    }

    private File getAttachmentLogFile(String outputPath) {
        return new File(outputPath + File.separator + "attachments.log");
    }

    private File getErrorLogFile(String outputPath) {
        return new File(outputPath + File.separator + "error.log");
    }
}