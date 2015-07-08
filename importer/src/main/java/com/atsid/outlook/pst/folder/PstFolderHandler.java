package com.atsid.outlook.pst.folder;

import com.pff.PSTFolder;

import java.util.List;

/**
 * Handler used by <code>PstParser</code> to process a folder when found.
 */
public interface PstFolderHandler {
    /**
     * Process the folder
     *
     * @param folder      Current folder to process
     * @param rootFolder  True if this is the root, false otherwise
     * @param folderNames List of folder names in the hierarchy
     */
    void processPstFolder(PSTFolder folder, Boolean rootFolder, List<String> folderNames);
}