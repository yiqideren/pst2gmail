package com.atsid.outlook.pst.message;

import com.pff.PSTMessage;

public interface PstMessageHandler {
    void processEmailMessage(PSTMessage pstMessage);
}