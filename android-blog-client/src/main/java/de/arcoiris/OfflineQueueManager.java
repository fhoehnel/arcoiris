package de.arcoiris;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.gson.Gson;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static de.arcoiris.FileComparator.SORT_BY_FILENAME;

/**
 * Created by fho on 24.09.2017.
 */

public class OfflineQueueManager {

    private static final String QUEUE_FOLDER = "blog-queue";

    private static final String METADATA_FILE_EXT = ".metadata";

    private static final String SWAP_FILE_NAME = "swap-";

    private static final int MAX_ERROR_COUNT = 5;

    private static OfflineQueueManager instance = null;

    private String queuePath;

    SimpleDateFormat dateFormat = null;

    private OfflineQueueManager(File applicationFilesDir) {
        queuePath = applicationFilesDir.getAbsolutePath() + File.separator + QUEUE_FOLDER;

        File queuePathFile = new File(queuePath);
        if (!queuePathFile.exists()) {
            queuePathFile.mkdirs();
        }

        dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    }

    public static OfflineQueueManager getInstance(File applicationFilesDir) {
        if (instance == null) {
            instance = new OfflineQueueManager(applicationFilesDir);
        }
        return instance;
    }

    public void queueBlogEntry(Context applicationContext, OfflineQueueMetaDataElem metaData, Uri pictureUri, String picturePath) {

        String pictureSource = null;

        if (pictureUri != null) {
            pictureSource = pictureUri.toString();
        } else {
            pictureSource = picturePath;
        }

        String imgFileExt = pictureSource.substring(pictureSource.lastIndexOf('.'));

        String queueFileBaseName = Long.toString(System.currentTimeMillis());

        String imgQueueFileName = queueFileBaseName + imgFileExt;

        File queueImgFile = new File(queuePath, imgQueueFileName);

        storePictureFile(applicationContext, pictureUri, picturePath, queueImgFile);

        /*
        String origPictureFileName = pictureSource.substring(pictureSource.lastIndexOf('/') + 1);

        metaData.setOrigPictureFileName(origPictureFileName);
        */

        storeMetaData(metaData, queueFileBaseName);
    }

    public void moveBlogEntryUp(String queueFileName) {
        String fileNameWithoutExt = queueFileName.substring(0, queueFileName.lastIndexOf('.'));

        OfflineQueueEntryInfo prevQueueEntry = null;
        OfflineQueueEntryInfo currentQueueEntry = null;

        for (OfflineQueueEntryInfo queueEntry : getQueuedFiles()) {
            File imgFile = queueEntry.getQueueImgFile();
            if (imgFile.getName().equals(queueFileName)) {
                currentQueueEntry = queueEntry;
                break;
            } else {
                prevQueueEntry = queueEntry;
            }
        }

        if (currentQueueEntry == null) {
            Log.e("arcoiris", "blog entry to move not found in offline queue: " + queueFileName);
            return;
        }

        if (prevQueueEntry == null) {
            Log.d("arcoiris", "blog entry to move up is already first entry: " + queueFileName);
            return;
        }

        OfflineQueueMetaDataElem prevMetaData = prevQueueEntry.getMetaData();

        OfflineQueueMetaDataElem currentMetaData = currentQueueEntry.getMetaData();

        Date swapDate = prevMetaData.getBlogDate();
        prevMetaData.setBlogDate(currentMetaData.getBlogDate());
        currentMetaData.setBlogDate(swapDate);

        String prevFileName = prevQueueEntry.getQueueImgFile().getName();
        String prevFileNameBase = prevFileName.substring(0, prevFileName.lastIndexOf('.'));

        storeMetaData(prevMetaData, prevFileNameBase);

        String currentFileName = currentQueueEntry.getQueueImgFile().getName();
        String currentFileNameBase = currentFileName.substring(0, currentFileName.lastIndexOf('.'));

        storeMetaData(currentMetaData, currentFileNameBase);
    }

    public void deleteQueuedBlogEntry(String queueFileName) {
        OfflineQueueEntryInfo offlineEntryInfo = getQueuedFileByFileName(queueFileName);

        if (offlineEntryInfo == null) {
            Log.e("arcoiris", "blog entry to delete not found in offline queue: " + queueFileName);
            return;
        }

        File queueImgFile = offlineEntryInfo.getQueueImgFile();

        String queueFileBaseName = queueImgFile.getName().substring(0, queueImgFile.getName().lastIndexOf('.'));

        if (queueImgFile.exists()) {
            if (!queueImgFile.delete()) {
                Log.e("arcoiris", "failed to delete queue img file " + queueImgFile.getName());
            }
        } else {
            Log.e("arcoiris", "queue img file to delete not found: " + queueImgFile.getName());
        }

        String metaDataQueueFileName = queueFileBaseName + METADATA_FILE_EXT;

        File metaDataQueueFile = new File(queuePath, metaDataQueueFileName);

        if (metaDataQueueFile.exists()) {
            if (!metaDataQueueFile.delete()) {
                Log.e("arcoiris", "failed to delete metadata queue file " + metaDataQueueFile.getAbsolutePath());
            }
        } else {
            Log.e("arcoiris", "queue metadata file to delete not found: " + metaDataQueueFile.getAbsolutePath());
        }
    }

    public void changeQueuedBlogEntry(Context applicationContext, OfflineQueueMetaDataElem metaData, Uri pictureUri, String picturePath, String editedEntryQueueFileName) {
        OfflineQueueEntryInfo offlineEntryInfo = getQueuedFileByFileName(editedEntryQueueFileName);

        if (offlineEntryInfo == null) {
            Log.e("arcoiris", "blog entry to update not found in offline queue: " + editedEntryQueueFileName);
            return;
        }

        File queueImgFile = offlineEntryInfo.getQueueImgFile();

        String queueFileBaseName = queueImgFile.getName().substring(0, queueImgFile.getName().lastIndexOf('.'));

        if (queueImgFile.exists()) {
            if (!queueImgFile.delete()) {
                Log.e("arcoiris", "failed to delete old img file for blog entry to update: " + editedEntryQueueFileName);
            }
        }

        String metaDataQueueFileName = queueFileBaseName + METADATA_FILE_EXT;

        File metaDataQueueFile = new File(metaDataQueueFileName);
        if (metaDataQueueFile.exists()) {
            if (!metaDataQueueFile.delete()) {
                Log.e("arcoiris", "failed to delete old metadata file for blog entry to update: " + editedEntryQueueFileName);
            }
        }

        storePictureFile(applicationContext, pictureUri, picturePath, queueImgFile);

        storeMetaData(metaData, queueFileBaseName);
    }

    private void storeMetaData(OfflineQueueMetaDataElem metaData, String queueFileBaseName) {
        Gson gson = new Gson();
        String serializedMetaData = gson.toJson(metaData);

        String metaDataQueueFileName = queueFileBaseName + METADATA_FILE_EXT;

        File queueInfoFile = new File(queuePath, metaDataQueueFileName);

        PrintWriter metaDataOut = null;
        try {
            metaDataOut = new PrintWriter(new OutputStreamWriter(new FileOutputStream(queueInfoFile), "UTF-8"));
            metaDataOut.print(serializedMetaData);
        } catch (IOException ioex) {
            Log.e("arcoiris", "failed to save blog metadata in offline queue", ioex);
        } finally {
            if (metaDataOut != null) {
                try {
                    metaDataOut.close();
                } catch (Exception ex) {
                }
            }
        }
    }

    private void storePictureFile(Context applicationContext, Uri pictureUri, String picturePath, File queueImgFile) {
        BufferedOutputStream buffOut = null;
        InputStream pictureIn = null;
        BufferedInputStream buffIn = null;
        try {
            if (pictureUri != null) {
                pictureIn = applicationContext.getContentResolver().openInputStream(pictureUri);
            } else {
                pictureIn = new FileInputStream(new File(picturePath));
            }

            buffIn = new BufferedInputStream(pictureIn);

            buffOut = new BufferedOutputStream(new FileOutputStream(queueImgFile));

            byte[] buff = new byte[4096];

            int count;

            while (( count = buffIn.read(buff)) >= 0 ) {
                buffOut.write(buff, 0, count);
            }
            buffOut.flush();

        } catch (Exception ioex) {
            Log.e("arcoiris", "failed to save blog picture in offline queue", ioex);
        } finally {
            if (buffIn != null) {
                try {
                    buffIn.close();
                } catch (Exception closeEx) {
                }
            }
            if (buffOut != null) {
                try {
                    buffOut.close();
                } catch (Exception closeEx) {
                }
            }
        }
    }

    public QueueSendResult sendQueuedEntries(HashMap<String, String> checkedLogins, boolean simulate) {
        int sentCount = 0;
        int entriesToSend = 0;
        int errorCount = 0;

        for (OfflineQueueEntryInfo queuedFile : getQueuedFiles()) {

            OfflineQueueMetaDataElem metaData = queuedFile.getMetaData();

            String loginKey = metaData.getServerUrl() + "~" + metaData.getUserid();

            String password = checkedLogins.get(loginKey);

            if (password != null) {
                if (simulate) {
                    entriesToSend++;
                } else {
                    if (sendBlogEntry(queuedFile.getQueueImgFile(), metaData, password, sentCount)) {

                        Log.d("arcoiris", "picture file and meta data for blog entry " + queuedFile.getQueueImgFile().getAbsolutePath() + " sent to server " + metaData.getServerUrl());

                        sentCount++;

                        String queueFileName = queuedFile.getQueueImgFile().getName().substring(0, queuedFile.getQueueImgFile().getName().lastIndexOf('.'));

                        if (!queuedFile.getQueueImgFile().delete()) {
                            Log.e("arcoiris", "failed to delete queued picture file " + queuedFile.getQueueImgFile().getAbsolutePath());
                        }

                        String metadataFileName = queueFileName + METADATA_FILE_EXT;

                        File metadataFile = new File(queuePath, metadataFileName);

                        if (!metadataFile.delete()) {
                            Log.e("arcoiris", "failed to delete queued metadata file " + metadataFile.getAbsolutePath());
                        }
                    } else {
                        errorCount++;
                        if (errorCount > MAX_ERROR_COUNT) {
                            break;
                        }
                    }
                }
            }
        }

        QueueSendResult sendResult = new QueueSendResult();

        if (simulate) {
            sendResult.setEntriesToSend(entriesToSend);
        } else {
            sendResult.setSuccess(sentCount);
            sendResult.setFailed(errorCount);
        }

        return sendResult;
    }

    public boolean existQueuedFiles() {
        File queuePathFile = new File(queuePath);

        File[] filesOfDir = queuePathFile.listFiles();

        return (filesOfDir.length > 0);
    }

    public OfflineQueueEntryInfo getQueuedFileByFileName(String fileName) {

        String fileNameWithoutExt = fileName.substring(0, fileName.lastIndexOf('.'));

        File metadataFile = null;
        File pictureFile = null;

        File queuePathFile = new File(queuePath);

        File[] filesOfDir = queuePathFile.listFiles();

        for (File file : filesOfDir) {
            if (file.isFile()) {
                if (file.getName().startsWith(fileNameWithoutExt)) {
                    if (file.getName().endsWith(METADATA_FILE_EXT)) {
                        metadataFile = file;
                    } else {
                        pictureFile = file;
                    }
                }
            }
        }

        if ((metadataFile == null) || (pictureFile == null)) {
            Log.e("arcoiris", "failed to load  metadata from offline queue for file " + fileName);
            return null;
        }

        Log.d("arcoiris", "getQueuedFileByFileName for " + fileName + " pictureFile = " + pictureFile.getName() + " metadataFile = " + metadataFile.getName());

        BufferedReader metadataIn = null;

        try {
            metadataIn = new BufferedReader(new InputStreamReader(new FileInputStream(metadataFile), "UTF-8"));

            Gson gson = new Gson();

            OfflineQueueMetaDataElem metaData = gson.fromJson(metadataIn, OfflineQueueMetaDataElem.class);

            OfflineQueueEntryInfo entryInfo = new OfflineQueueEntryInfo();

            entryInfo.setQueueImgFile(pictureFile);

            entryInfo.setMetaData(metaData);

            return entryInfo;
        } catch (IOException ioex) {
            Log.e("arcoiris", "failed to load  metadata from offline queue", ioex);
        } finally {
            if (metadataIn != null) {
                try {
                    metadataIn.close();
                } catch (IOException ioex) {
                }
            }
        }

        return null;
    }

    public Date getLatestEntryOfDay(Date selectedDate) {
        Date latestEntryOfDay = new Date(selectedDate.getTime());
        latestEntryOfDay.setHours(0);
        latestEntryOfDay.setMinutes(0);
        latestEntryOfDay.setSeconds(0);

        List<OfflineQueueEntryInfo> queuedFiles = getQueuedFiles();

        for (OfflineQueueEntryInfo entryInfo : queuedFiles) {
            Date blogDate = entryInfo.getMetaData().getBlogDate();
            if ((blogDate.getDate() == selectedDate.getDate()) && (blogDate.getMonth() == selectedDate.getMonth() && (blogDate.getYear() == selectedDate.getYear()))) {
                if (blogDate.getTime() > latestEntryOfDay.getTime()) {
                    latestEntryOfDay = blogDate;
                }
            }
        }

        return latestEntryOfDay;
    }

    public List<OfflineQueueEntryInfo> getQueuedFiles() {
        File queuePathFile = new File(queuePath);

        File[] filesOfDir = queuePathFile.listFiles();

        HashMap<String, File> pictureFileMap = new HashMap<String, File>();

        int sentCount = 0;

        ArrayList<File> metadataFileList = new ArrayList<File>();

        for (File file : filesOfDir) {
            if (file.isFile()) {
                if (file.getName().endsWith(METADATA_FILE_EXT)) {
                    metadataFileList.add(file);
                } else {
                    String key = file.getName().substring(0, file.getName().lastIndexOf('.'));
                    pictureFileMap.put(key, file);
                }
            }
        }

        // Collections.sort(metadataFileList, new FileComparator(SORT_BY_FILENAME));

        ArrayList offlineInfoList = new ArrayList<OfflineQueueEntryInfo>();

        for (File metadataFile : metadataFileList) {
            String key = metadataFile.getName().substring(0, metadataFile.getName().lastIndexOf('.'));
            File pictureFile = pictureFileMap.get(key);

            if (pictureFile == null) {
                Log.e("arcoiris", "deleting zombie metadata file without image file: " + metadataFile.getName());
                if (!metadataFile.delete()) {
                    Log.e("arcoiris", "failed to delete zombie metadata file " + metadataFile.getAbsolutePath());
                }
            } else {
                BufferedReader metadataIn = null;

                try {
                    metadataIn = new BufferedReader(new InputStreamReader(new FileInputStream(metadataFile), "UTF-8"));

                    Gson gson = new Gson();

                    OfflineQueueMetaDataElem metaData = gson.fromJson(metadataIn, OfflineQueueMetaDataElem.class);

                    OfflineQueueEntryInfo entryInfo = new OfflineQueueEntryInfo();

                    // entryInfo.setOrigPictureFileName(metaData.getOrigPictureFileName());

                    entryInfo.setQueueImgFile(pictureFile);

                    entryInfo.setMetaData(metaData);

                    offlineInfoList.add(entryInfo);
                } catch (IOException ioex) {
                    Log.e("arcoiris", "failed to load  metadata from offline queue", ioex);
                } finally {
                    if (metadataIn != null) {
                        try {
                            metadataIn.close();
                        } catch (IOException ioex) {
                        }
                    }
                }
            }
        }

        if (offlineInfoList.size() > 1) {
            Collections.sort(offlineInfoList, new OfflineQueueComparator(OfflineQueueComparator.SORT_BY_DATE));
        }

        return offlineInfoList;
    }

    private boolean sendBlogEntry(File pictureFile, OfflineQueueMetaDataElem metaData, String password, int counter) {
        Date now = new Date();

        Date blogDate = new Date(metaData.getBlogDate().getTime());

        blogDate.setHours(now.getHours());
        blogDate.setMinutes(now.getMinutes());
        blogDate.setSeconds(now.getSeconds());

        String destFileName = dateFormat.format(blogDate) + "-" + (blogDate.getTime() + counter)  + ".jpg";

        boolean success = false;

        try {
            ServerCommunicator serverCommunicator = ServerCommunicator.getInstance();

            InputStream pictureIn = new FileInputStream(pictureFile);

            if (serverCommunicator.sendPicture(metaData.getServerUrl(), metaData.getUserid(), password, destFileName, pictureIn)) {

                if (serverCommunicator.sendDescription(metaData.getServerUrl(), metaData.getUserid(), password, destFileName, metaData.getBlogText(), metaData.getGeoLocation())) {

                    if (metaData.isPublishImmediately()) {
                        if (serverCommunicator.sendPublishRequest(metaData.getServerUrl(), metaData.getUserid(), password, destFileName)) {
                            success = true;
                        }
                    } else {
                        success = true;
                    }
                }

                if (!success) {
                    serverCommunicator.cancelEntry(metaData.getServerUrl(), metaData.getUserid(), password, destFileName);
                }
            }
        } catch (FileNotFoundException fnfex) {
            Log.e("arcoiris", "picture file for queued blog entry not found", fnfex);
        }

        return success;
    }
}
