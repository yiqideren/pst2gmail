package com.atsid.outlook.pst;

import com.pff.PSTException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;

@Component
public class PromptScreen extends JPanel implements ActionListener, PropertyChangeListener {
    private JButton startButton;
    private JButton browsePstFile;
    private JButton browseOutputDirectory;
    private JButton browseJsonFile;
    private JFileChooser pstFile;
    private JFileChooser outputDirectory;
    private JFileChooser jsonFile;
    private JTextField emailAddress;
    private JTextField pstFilePathText;
    private JTextField outputDirectoryText;
    private JTextField jsonFileText;
    private JLabel pstLabel;
    private JLabel outputLabel;
    private JLabel emailLabel;
    private JLabel jsonFileLabel;
    private JProgressBar progressBar;
    @Autowired
    ExtractionWorker worker;
    @Autowired
    PstParser pstParser;

    public PromptScreen() {
        super(new GridLayout(4, 1));

        pstLabel = new JLabel("PST File");
        outputLabel = new JLabel("Output Directory");
        emailLabel = new JLabel("Email Address");
        jsonFileLabel = new JLabel("Google auth JSON file");
        pstFile = new JFileChooser();
        outputDirectory = new JFileChooser();
        jsonFile = new JFileChooser();
        emailAddress = new JTextField("");
        pstFilePathText = new JTextField("", 25);
        outputDirectoryText = new JTextField("", 25);
        jsonFileText = new JTextField("");
        startButton = new JButton("Start");
        browsePstFile = new JButton("Browse ...");
        browseOutputDirectory = new JButton("Browse ...");
        browseJsonFile = new JButton("Browse ...");
        progressBar = new JProgressBar();

        pstLabel.setLabelFor(pstFilePathText);
        outputLabel.setLabelFor(outputDirectoryText);
        emailLabel.setLabelFor(emailAddress);
        jsonFileLabel.setLabelFor(jsonFileText);

        browseOutputDirectory.addActionListener(this);
        browsePstFile.addActionListener(this);
        browseJsonFile.addActionListener(this);
        startButton.addActionListener(this);

        DocumentListener documentListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                checkStartEnable();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                checkStartEnable();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                checkStartEnable();
            }
        };

        pstFilePathText.getDocument().addDocumentListener(documentListener);
        outputDirectoryText.getDocument().addDocumentListener(documentListener);
        jsonFileText.getDocument().addDocumentListener(documentListener);
        emailAddress.getDocument().addDocumentListener(documentListener);

        FileFilter pstFilter = new FileNameExtensionFilter("Outlook PST File", "pst");
        FileFilter jsonFilter = new FileNameExtensionFilter("JSON File", "json");
        pstFile.setFileFilter(pstFilter);
        jsonFile.setFileFilter(jsonFilter);
        outputDirectory.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        JPanel panel1 = new JPanel(new GridLayout(3, 3));
        JPanel panel2 = new JPanel(new GridLayout(1, 2));

        panel1.add(pstLabel);
        panel1.add(pstFilePathText);
        panel1.add(browsePstFile);
        panel1.add(outputLabel);
        panel1.add(outputDirectoryText);
        panel1.add(browseOutputDirectory);
        panel1.add(jsonFileLabel);
        panel1.add(jsonFileText);
        panel1.add(browseJsonFile);

        panel2.add(emailLabel);
        panel2.add(emailAddress);

        add(panel1);
        add(panel2);
        add(progressBar);
        add(startButton);

        addPropertyChangeListener(this);
        progressBar.setVisible(false);
        startButton.setEnabled(false);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == browseOutputDirectory) {
            int retVal = outputDirectory.showOpenDialog(PromptScreen.this);

            if (retVal == JFileChooser.APPROVE_OPTION) {
                outputDirectoryText.setText(outputDirectory.getSelectedFile().getAbsolutePath());
            }
        } else if (e.getSource() == browsePstFile) {
            int retVal = pstFile.showOpenDialog(PromptScreen.this);

            if (retVal == JFileChooser.APPROVE_OPTION) {
                pstFilePathText.setText(pstFile.getSelectedFile().getAbsolutePath());
            }
        } else if (e.getSource() == browseJsonFile) {
            int retVal = jsonFile.showOpenDialog(PromptScreen.this);

            if (retVal == JFileChooser.APPROVE_OPTION) {
                jsonFileText.setText(jsonFile.getSelectedFile().getAbsolutePath());
            }
        } else if (e.getSource() == startButton) {
            try {
                worker.addPropertyChangeListener(this);
                worker.setPstFile(pstFilePathText.getText());
                worker.setEmailAddress(emailAddress.getText());
                worker.setOutputDirectoryText(outputDirectoryText.getText());
                worker.setJsonCredentialFile(jsonFileText.getText());
                int messageCount = pstParser.countEmails(pstFilePathText.getText());
                progressBar.setMinimum(0);
                progressBar.setMaximum(100);
                progressBar.setValue(0);
                progressBar.setStringPainted(true);
                progressBar.setVisible(true);
                System.out.println(String.format("Found %d emails", messageCount));
                worker.setItemCount(messageCount);
                startButton.setEnabled(false);
                worker.execute();
            } catch (IOException e1) {
                e1.printStackTrace();
            } catch (PSTException e1) {
                e1.printStackTrace();
            }
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ("progress".equals(evt.getPropertyName())) {
            progressBar.setValue((Integer) evt.getNewValue());
        }
    }

    private void checkStartEnable() {
        startButton.setEnabled(!(jsonFileText.getText().isEmpty() || pstFilePathText.getText().isEmpty() ||
                                 outputDirectoryText.getText().isEmpty() || emailAddress.getText().isEmpty()));
    }
}