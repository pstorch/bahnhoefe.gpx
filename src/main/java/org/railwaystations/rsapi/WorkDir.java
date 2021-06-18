package org.railwaystations.rsapi;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class WorkDir {

    private final File photosDir;
    private final File inboxDir;
    private final File inboxProcessedDir;
    private final File inboxToProcessDir;

    public WorkDir(@Value("${workDir}") final String workDir) {
        this.photosDir = new File(workDir, "photos");
        this.inboxDir = new File(workDir, "inbox");
        this.inboxProcessedDir = new File(inboxDir, "processed");
        this.inboxToProcessDir = new File(inboxDir, "toprocess");
    }

    public File getPhotosDir() {
        return photosDir;
    }

    public File getInboxDir() {
        return inboxDir;
    }

    public File getInboxProcessedDir() {
        return inboxProcessedDir;
    }

    public File getInboxToProcessDir() {
        return inboxToProcessDir;
    }

}
