package com.atsid.outlook.app;

import com.atsid.outlook.pst.AttachmentExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

import javax.swing.*;

/**
 * SpringBoot <code>CommandLineRunner</code> that builds and runs email extraction application.
 */
@Configuration
@ComponentScan(basePackages = {"com.atsid.outlook", "com.atsid.exchange"})
@ImportResource("classpath:applicationContext.xml")
public class ExtractApplication implements CommandLineRunner {
    @Autowired
    private AttachmentExtractor attachmentExtractor;
    @Autowired
    private PromptScreen promptScreen;

    @Override
    public void run(String... strings) throws Exception {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                UIManager.put("swing.boldMetal", Boolean.FALSE);
                createAndShowPromptScreen();
            }
        });
    }

    /**
     * Helper method used to create and display prompt screen.
     */
    private void createAndShowPromptScreen() {
        JFrame frame = new JFrame("EmailAttachmentExtractionImportTool");

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(promptScreen);
        frame.pack();
        frame.setVisible(true);
    }
}