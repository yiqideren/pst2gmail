package com.atsid.outlook.pst.folder;

import com.google.common.base.Preconditions;
import com.pff.PSTFolder;
import lombok.Getter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * PST Folder handler that simply counts entries in non-root folders.  Items are only counted in root folders if it
 * has no sub-folders.
 */
@Component("1pstMessageCountingFolderHandler")
@Scope("prototype")
public class PstMessageCountingFolderHandler implements PstFolderHandler {
    @Getter
    private int messageCount = 0;

    @Override
    public void processPstFolder(PSTFolder folder, Boolean rootFolder, List<String> folderNames) {
        Preconditions.checkArgument(folder != null, "folder cannot be null");

        if (!rootFolder || rootFolder && !folder.hasSubfolders()) {
            messageCount += folder.getContentCount();
        }
    }
}