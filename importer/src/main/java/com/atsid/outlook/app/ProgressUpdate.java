package com.atsid.outlook.app;

/**
 * Reports progress for an in-progress task to the UI.
 */
public interface ProgressUpdate {
    /**
     * Reports current progress based on the number of units, NOT percentage.
     *
     * @param progress The number of completed units
     */
    void updateProgress(int progress);
}