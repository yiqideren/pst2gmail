package com.atsid.outlook.app;

import com.atsid.outlook.pst.PstParser;
import lombok.Setter;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.swing.SwingWorker;

@Component
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

    @Override
    protected Void doInBackground() throws Exception {
        setProgress(0);
        System.out.println("Starting processing");
        DateTime startDate = new DateTime();
        ProgressUpdate progressUpdate = new ProgressUpdate() {
            @Override
            public void updateProgress(int progress) {
                int newProgress = (int) Math.floor(((double) progress / (double) itemCount) * 100.0);
                setProgress(newProgress > 100 ? 100 : newProgress);
            }
        };
        try {
            pstParser.processPst(pstFile, emailAddress, outputDirectoryText, null, jsonCredentialFile, progressUpdate);
            setProgress(100);
        } catch (Exception ex) {
            System.out.println("Caught exception");
            ex.printStackTrace();
        } finally {
            DateTime endDate = new DateTime();
            Duration duration = new Duration(endDate.getMillis() - startDate.getMillis());
            PeriodFormatter formatter =
                    new PeriodFormatterBuilder().appendHours().appendSuffix("h").appendMinutes().appendSuffix("m")
                                                .appendSeconds().appendSuffix("s").toFormatter();
            String formatted = formatter.print(duration.toPeriod());
            System.out.println("Duration: " + formatted);
        }
        System.out.println("Done processing");
        return null;
    }

    @Override
    public void done() {
    }
}
