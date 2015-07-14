package com.atsid.outlook.pst.message;

import com.atsid.outlook.pst.AttachmentExtractor;
import com.atsid.outlook.pst.GmailServiceFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.Message;
import com.pff.PSTMessage;
import lombok.Setter;
import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Handles the import of a single message from a PST into GMail using their import API.
 */
@Component
@Log4j
public class GmailImportingPstMessageHandler implements PstMessageHandler {
    @Setter
    private GmailServiceFactory gmailServiceFactory;
    @Autowired
    private AttachmentExtractor extractor;
    @Autowired
    private MessageConverter converter;
    @Autowired
    private GMailLabelFactory labelFactory;

    @Override
    public void processEmailMessage(PSTMessage pstMessage, String outputPath, List<String> folderNames,
            String emailAddress) {
        try {
            Gmail gmailService = gmailServiceFactory.getGmailService(emailAddress);
            GMailLabeler labeler = labelFactory.getLabeler(gmailService, emailAddress);
            List<String> attachmentsRemoved = extractor.extractAttachments(pstMessage, outputPath);
            Message gmailMessage = converter.convertMessage(pstMessage, outputPath, attachmentsRemoved);
            Label label = labeler.getLabel(folderNames, emailAddress);

            gmailMessage.setLabelIds(
                    Arrays.asList(label.getId(), labeler.getAvailableLabel(GMailLabeler.PST_IMPORT_LABEL).getId()));
            gmailService.users().messages().gmailImport(emailAddress, gmailMessage).execute();
        } catch (Exception ex) {
            log.error(
                    String.format("Caught exception while processing message %d for %s", pstMessage.getDescriptorNode(),
                            emailAddress), ex);
        }

    }
}