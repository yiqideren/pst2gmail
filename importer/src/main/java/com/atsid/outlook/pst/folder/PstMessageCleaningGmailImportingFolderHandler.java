package com.atsid.outlook.pst.folder;

import com.atsid.outlook.pst.AttachmentExtractor;
import com.atsid.outlook.pst.ProgressUpdate;
import com.atsid.outlook.pst.message.MessageConverter;
import com.atsid.outlook.pst.message.PstMessageHandler;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.ListLabelsResponse;
import com.google.api.services.gmail.model.Message;
import com.pff.PSTAppointment;
import com.pff.PSTContact;
import com.pff.PSTException;
import com.pff.PSTFolder;
import com.pff.PSTMessage;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.mail.MessagingException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component("pstMessageCleaningGmailImportingFolderHandler")
@Scope("prototype")
public class PstMessageCleaningGmailImportingFolderHandler implements PstFolderHandler {
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
    private Gmail gmailService;
    @Autowired
    private AttachmentExtractor extractor;
    @Autowired
    private MessageConverter converter;
    int count = 0;
    private static final String PST_IMPORT_LABEL = "PST Import";
    private Map<String, Label> availableLabels = new HashMap<>();

    @Override
    public void processPstFolder(PSTFolder folder, Boolean rootFolder, List<String> folderNames) {
        if (folder.getContentCount() > 0) {
            try {
                PSTMessage message = getNextEmail(folder);

                while (message != null) {
                    if (PSTContact.class == message.getClass() || PSTAppointment.class == message.getClass()) {
                        System.out.println("Found non-message item " + message.getClass().toString());
                    } else {
                        List<String> attachmentsRemoved = extractor.extractAttachments(message, outputPath);
                        Message gmailMessage = converter.convertMessage(message, outputPath, attachmentsRemoved);

                        count++;
                        progressUpdate.updateProgress(count);
                        try {
                            Label label = getLabel(folderNames);
                            gmailMessage.setLabelIds(
                                    Arrays.asList(label.getId(), availableLabels.get(PST_IMPORT_LABEL).getId()));
                            gmailService.users().messages().gmailImport(emailAddress, gmailMessage).execute();
                        } catch (GoogleJsonResponseException ex) {
                            System.out.println("Current message: " + message.getDescriptorNodeId());
                            ex.printStackTrace();
                        }
                    }

                    message = getNextEmail(folder);
                }
            } catch (MessagingException e) {
                e.printStackTrace();
            } catch (PSTException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private Label getLabel(List<String> folderNames) throws IOException {
        String label = "";

        if (availableLabels.isEmpty()) {
            loadLabels();
        }

        for (String folderName : folderNames) {
            String cleanLabel = sanitizeLabel(convertIfReserved(folderName));
            label += StringUtils.isEmpty(label) ? cleanLabel : "/" + cleanLabel;

            createLabelIfNotExist(label);
        }

        createLabelIfNotExist(PST_IMPORT_LABEL);

        return availableLabels.get(label);
    }

    private void createLabelIfNotExist(String labelName) throws IOException {
        if (!availableLabels.containsKey(labelName)) {
            Label gmailLabel = new Label();
            gmailLabel.setName(labelName);
            gmailLabel.setLabelListVisibility("labelShow");
            gmailLabel.setMessageListVisibility("hide");
            gmailLabel = gmailService.users().labels().create(emailAddress, gmailLabel).execute();

            availableLabels.put(labelName, gmailLabel);
        }
    }

    private void loadLabels() throws IOException {
        ListLabelsResponse labelsResponse = gmailService.users().labels().list(emailAddress).execute();

        for (Label label : labelsResponse.getLabels()) {
            availableLabels.put(label.getName(), label);
        }
    }

    private String convertIfReserved(String label) {
        if (label.equals("Inbox")) {
            return "INBOX";
        } else if (label.equals("Sent Items")) {
            return "SENT";
        }

        return label;
    }

    private String sanitizeLabel(String label) {
        return label.replaceAll("/", "_").replaceAll("-", "_");
    }

    private PSTMessage getNextEmail(PSTFolder folder) throws PSTException, IOException {
        return (PSTMessage) folder.getNextChild();
    }
}