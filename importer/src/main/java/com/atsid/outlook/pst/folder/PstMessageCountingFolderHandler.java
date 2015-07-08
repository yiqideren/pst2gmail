package com.atsid.outlook.pst.folder;

import com.pff.PSTFolder;
import lombok.Getter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("pstMessageCountingFolderHandler")
@Scope("prototype")
public class PstMessageCountingFolderHandler implements PstFolderHandler {
    @Getter
    private int messageCount = 0;

    @Override
    public void processPstFolder(PSTFolder folder, Boolean rootFolder, List<String> folderNames) {
        if (!rootFolder || rootFolder && !folder.hasSubfolders()) {
            messageCount += folder.getContentCount();
        }
    }
}
