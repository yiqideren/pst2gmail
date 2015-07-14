package com.atsid.outlook.pst.message;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.ListLabelsResponse;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component("gmailLabeler")
@Scope("prototype")
public class GMailLabeler {
    @Setter
    private Gmail gmailService;
    @Setter
    private String emailAddress;
    @Getter(AccessLevel.PROTECTED)
    private Map<String, Label> availableLabels = new HashMap<>();
    public static final String PST_IMPORT_LABEL = "PST Import";

    public GMailLabeler() {
    }

    public GMailLabeler(Gmail gmailService, String emailAddress) {
        this.gmailService = gmailService;
        this.emailAddress = emailAddress;
    }

    protected void loadLabels() throws IOException {
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

    public Label getLabel(List<String> folderNames, String emailAddress) throws IOException {
        String label = "";

        for (String folderName : folderNames) {
            String cleanLabel = sanitizeLabel(convertIfReserved(folderName));
            label += StringUtils.isEmpty(label) ? cleanLabel : "/" + cleanLabel;

            createLabelIfNotExist(label, emailAddress);
        }

        createLabelIfNotExist(PST_IMPORT_LABEL, emailAddress);

        return availableLabels.get(label);
    }

    public Label getAvailableLabel(String labelName) {
        return availableLabels.get(labelName);
    }

    private void createLabelIfNotExist(String labelName, String emailAddress) throws IOException {
        if (!availableLabels.containsKey(labelName)) {
            Label gmailLabel = new Label();
            gmailLabel.setName(labelName);
            gmailLabel.setLabelListVisibility("labelShow");
            gmailLabel.setMessageListVisibility("hide");
            gmailLabel = gmailService.users().labels().create(emailAddress, gmailLabel).execute();

            availableLabels.put(labelName, gmailLabel);
        }
    }

    private String sanitizeLabel(String label) {
        return label.replaceAll("/", "_").replaceAll("-", "_");
    }
}