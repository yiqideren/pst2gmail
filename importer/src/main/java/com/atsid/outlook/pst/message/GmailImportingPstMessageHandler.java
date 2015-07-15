package com.atsid.outlook.pst.message;

import com.atsid.outlook.pst.AttachmentExtractor;
import com.atsid.outlook.pst.GmailServiceFactory;
import com.google.api.client.googleapis.batch.BatchRequest;
import com.google.api.client.googleapis.batch.json.JsonBatchCallback;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.Message;
import com.pff.PSTMessage;
import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Handles the import of a single message from a PST into GMail using their import API.
 */
@Component
@Log4j
public class GmailImportingPstMessageHandler implements PstMessageHandler {
    @Autowired
    private GmailServiceFactory gmailServiceFactory;
    @Autowired
    private AttachmentExtractor extractor;
    @Autowired
    private MessageConverter converter;
    @Autowired
    private GMailLabelFactory labelFactory;
    @Value("${import.batch.size}")
    private int batchSize;
    @Value("${import.batch.connect.timeout}")
    private int connectTimeout;
    @Value("${import.batch.read.timeout}")
    private int readTimeout;
    private BatchRequest batch;

    @Override
    public void processEmailMessage(PSTMessage pstMessage, String outputPath, List<String> folderNames,
            String emailAddress) {
        try {
            Gmail gmailService = gmailServiceFactory.getGmailService(emailAddress);
            GMailLabeler labeler = labelFactory.getLabeler(gmailService, emailAddress);
            List<String> attachmentsRemoved = extractor.extractAttachments(pstMessage, outputPath);
            Message gmailMessage = converter.convertMessage(pstMessage, outputPath, attachmentsRemoved);
            Label label = labeler.getLabel(folderNames, emailAddress);

            checkBatch(gmailService);

            gmailMessage.setLabelIds(
                    Arrays.asList(label.getId(), labeler.getAvailableLabel(GMailLabeler.PST_IMPORT_LABEL).getId()));
            gmailService.users().messages().gmailImport(emailAddress, gmailMessage)
                        .queue(batch, new JsonBatchCallback<Message>() {
                            @Override
                            public void onFailure(GoogleJsonError e, HttpHeaders responseHeaders) throws IOException {
                                log.error(String.format("Caught exception while processing messages", e));
                            }

                            @Override
                            public void onSuccess(Message message, HttpHeaders responseHeaders) throws IOException {

                            }
                        });
            checkBatchForSending(gmailService);
        } catch (Exception ex) {
            log.error(String.format("Caught exception while processing message %d for %s",
                    pstMessage.getDescriptorNodeId(), emailAddress), ex);
        }

    }

    private void checkBatch(Gmail gmailService) {
        if (batch == null) {
            batch = getBatch(gmailService);
        }
    }

    private void checkBatchForSending(Gmail gmailService) throws IOException {
        if (batch.size() >= batchSize) {
            batch.execute();

            batch = getBatch(gmailService);
        }
    }

    private BatchRequest getBatch(Gmail gmailService) {
        return gmailService.batch(new HttpRequestInitializer() {
            @Override
            public void initialize(HttpRequest httpRequest) throws IOException {
                httpRequest.setConnectTimeout(connectTimeout);
                httpRequest.setReadTimeout(readTimeout);
            }
        });
    }
}