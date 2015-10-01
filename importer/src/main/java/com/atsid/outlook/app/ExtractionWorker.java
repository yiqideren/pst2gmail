package com.atsid.outlook.app;

import com.atsid.outlook.pst.GmailServiceFactory;
import com.atsid.outlook.pst.PstParser;
import com.atsid.outlook.pst.message.GmailImportingPstMessageHandler;
import lombok.Setter;
import lombok.extern.log4j.Log4j;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.swing.SwingWorker;

/**
 * <code>SwingWorker</code> that kicks off the import process in the background.
 */
@Component
@Log4j
public class ExtractionWorker extends SwingWorker<Void, Void> {

    @Autowired
    PstParser pstParser;
    @Setter
    private String pstFile;
    @Setter
    private String emailAddress;
    @Setter
    private String outputDirectoryText;
    @Setter
    private String jsonCredentialFile;
    @Setter
    private int itemCount;
    @Autowired
    private GmailServiceFactory gmailServiceFactory;
    @Autowired
    private GmailImportingPstMessageHandler gmailMessageHandler;

    @Override
    protected Void doInBackground() throws Exception {
        setProgress(0);
        log.info("Starting processing");
        DateTime startDate = new DateTime();
        ProgressUpdate progressUpdate = new ProgressUpdate() {
            @Override
            public void updateProgress(int progress) {
                int newProgress = (int) Math.floor(((double) progress / (double) itemCount) * 100.0);
                setProgress(newProgress > 100 ? 100 : newProgress);
            }
        };
        try {
            gmailServiceFactory.addCredentials(emailAddress, jsonCredentialFile);
            pstParser.processPst(pstFile, emailAddress, outputDirectoryText, gmailMessageHandler, progressUpdate);
            setProgress(100);
        } catch (Exception ex) {
            log.error("Caught exception", ex);
        } finally {
            DateTime endDate = new DateTime();
            Duration duration = new Duration(endDate.getMillis() - startDate.getMillis());
            PeriodFormatter formatter =
                    new PeriodFormatterBuilder().appendHours().appendSuffix("h").appendMinutes().appendSuffix("m")
                            .appendSeconds().appendSuffix("s").toFormatter();
            String formatted = formatter.print(duration.toPeriod());
            log.info("Duration: " + formatted);
        }

        log.info("Done processing");

        return null;
    }

    @Override
    public void done() {
    }
}
