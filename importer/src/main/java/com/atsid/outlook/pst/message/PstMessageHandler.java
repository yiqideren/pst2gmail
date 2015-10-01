package com.atsid.outlook.pst.message;

import com.pff.PSTMessage;

import java.util.List;

/**
 * Defines an interface used by the <code>PstParser</code> to process PST files.
 */
public interface PstMessageHandler {
    /**
     * Processes an email message encountered while walking through a PST file.
     *
     * @param pstMessage          PSTMessage that we are currently processing
     * @param outputPath          Full path to output location where we can dump content
     * @param folderNames         List of names of folders leading up to where this message was found
     * @param accountEmailAddress Email address for account we are processing
     */
    void processEmailMessage(PSTMessage pstMessage, String outputPath, List<String> folderNames,
                             String accountEmailAddress);
}