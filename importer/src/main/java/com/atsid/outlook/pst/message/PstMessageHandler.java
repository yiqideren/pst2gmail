package com.atsid.outlook.pst.message;

import com.pff.PSTMessage;

import java.util.List;

public interface PstMessageHandler {
    void processEmailMessage(PSTMessage pstMessage, String outputPath, List<String> folderNames,
            String accountEmailAddress);
}