package de.arcoiris;

import java.io.File;

/**
 * Created by fho on 08.10.2017.
 */

public class OfflineQueueEntryInfo {
    File queueImgFile;

    String origPictureFileName;

    OfflineQueueMetaDataElem metaData;

    public void setQueueImgFile(File newVal) {
        queueImgFile = newVal;
    }

    public File getQueueImgFile() {
        return queueImgFile;
    }

    public void setOrigPictureFileName(String newVal) {
        origPictureFileName = newVal;
    }

    public String getOrigPictureFileName() {
        return origPictureFileName;
    }

    public void setMetaData(OfflineQueueMetaDataElem newVal) {
        metaData = newVal;
    }

    public OfflineQueueMetaDataElem getMetaData() {
        return metaData;

    }
}
