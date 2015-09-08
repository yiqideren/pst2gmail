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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
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
    @Value("${import.batch.enable}")
    private boolean useBatch;
    private BatchRequest batch;
    @Value("${import.error.subject.and.date}")
    private boolean detailedErrorLog;
    @Value("${import.retry.count}")
    private int retryCount;
    @Value("${import.retry.backoff.ms}")
    private int retrySleep;

    @Override
    public void processEmailMessage(PSTMessage pstMessage, String outputPath, List<String> folderNames,
                                    String emailAddress) {
        processEmailMessage(pstMessage, outputPath, folderNames, emailAddress, 0);
    }

    private void processEmailMessage(PSTMessage pstMessage, String outputPath, List<String> folderNames, String emailAddress, int currentRetry) {
        try {
            Gmail gmailService = gmailServiceFactory.getGmailService(emailAddress);
            GMailLabeler labeler = labelFactory.getLabeler(gmailService, emailAddress);
            List<String> attachmentsRemoved = extractor.extractAttachments(pstMessage, outputPath);
            Message gmailMessage = converter.convertMessage(pstMessage, outputPath, attachmentsRemoved);
            Label label = labeler.getLabel(folderNames, emailAddress);

            gmailMessage.setLabelIds(
                    Arrays.asList(label.getId(), labeler.getAvailableLabel(GMailLabeler.PST_IMPORT_LABEL).getId()));

            if (useBatch) {
                importBatch(emailAddress, gmailService, gmailMessage);
            } else {
                importNonBatch(emailAddress, gmailService, gmailMessage);
            }
        } catch (Exception ex) {
            logMessageFailure(pstMessage, emailAddress, currentRetry, ex);

            // If we have not reached our max retry count, backoff for 'retrySleep' ms and then try again
            if (currentRetry < retryCount) {
                try {
                    Thread.currentThread().sleep(retrySleep);
                } catch (InterruptedException e) {
                    log.error("Encountered thread interruption exception during backoff period");
                }
                processEmailMessage(pstMessage, outputPath, folderNames, emailAddress, ++currentRetry);
            } else {
                log.error("Max retries reached, will not try again");
            }
        }
    }

    /**
     * Helper method to log an error encountered when an email could not be processed.
     *
     * @param pstMessage   PST Message currently being processed
     * @param emailAddress Email address for account we are processing
     * @param retryCount   Current retry count
     * @param ex           Exception that was encountered
     */
    private void logMessageFailure(PSTMessage pstMessage, String emailAddress, int retryCount, Exception ex) {
        if (detailedErrorLog) {
            DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            String messageDate = formatter.format(pstMessage.getMessageDeliveryTime());
            String message =
                    String.format("Caught exception while processing message %d for %s retry %d\n\tSubject: %s\n\tDate: %s",
                            pstMessage.getDescriptorNodeId(), emailAddress, retryCount, pstMessage.getSubject(), messageDate);
            log.error(message, ex);
        } else {
            log.error(String.format("Caught exception while processing message %d for %s retry %d",
                    pstMessage.getDescriptorNodeId(), emailAddress, retryCount), ex);
        }
    }

    /**
     * Performs an import using GMail batch APIs.
     *
     * @param emailAddress Email address for account we are importing
     * @param gmailService GMail service client
     * @param gmailMessage Converted message to send
     * @throws IOException
     */
    private void importBatch(String emailAddress, Gmail gmailService, Message gmailMessage) throws IOException {
        checkBatch(gmailService);

        gmailService.users().messages().gmailImport(emailAddress, gmailMessage)
                .queue(batch, new JsonBatchCallback<Message>() {
                    @Override
                    public void onFailure(GoogleJsonError e, HttpHeaders responseHeaders) throws IOException {
                        log.error(String.format("Caught exception while processing messages: %s", e));
                    }

                    @Override
                    public void onSuccess(Message message, HttpHeaders responseHeaders) throws IOException {

                    }
                });
        checkBatchForSending(gmailService);
    }

    /**
     * Performs an import making direct calls to GMail APIs with each message.
     *
     * @param emailAddress Email address for account we are importing
     * @param gmailService GMail service client
     * @param gmailMessage Converted message to send
     * @throws IOException
     */
    private void importNonBatch(String emailAddress, Gmail gmailService, Message gmailMessage) throws IOException {

        gmailService.users().messages().gmailImport(emailAddress, gmailMessage).execute();
    }

    /**
     * Checks if batch is available and creates it if it isn't.
     *
     * @param gmailService GMail service client
     */
    private void checkBatch(Gmail gmailService) {
        if (batch == null) {
            batch = getBatch(gmailService);
        }
    }

    /**
     * Checks if the batch is ready to execute.  If ready, it executes and creates a new batch.
     *
     * @param gmailService GMail service client
     * @throws IOException
     */
    private void checkBatchForSending(Gmail gmailService) throws IOException {
        if (batch.size() >= batchSize) {
            batch.execute();

            batch = getBatch(gmailService);
        }
    }

    /**
     * Gets a new batch request from the GMail service client.
     *
     * @param gmailService GMail service client
     * @return Returns a new batch request with connect and read timeouts set appropriately
     */
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