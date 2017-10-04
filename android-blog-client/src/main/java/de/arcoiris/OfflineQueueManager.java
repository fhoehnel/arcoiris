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

        Log.d("arcoiris", "queue img path: " + queueImgFile.getAbsolutePath());

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

    public int sendQueuedEntries(HashMap<String, String> checkedLogins, boolean simulate) {

        File queuePathFile = new File(queuePath);

        File[] filesOfDir = queuePathFile.listFiles();

        List<File> fileList = Arrays.asList(filesOfDir);

        HashMap<String, File> pictureFileMap = new HashMap<String, File>();

        int sentCount = 0;

        ArrayList<File> metadataFileList = new ArrayList<File>();

        for (File file : fileList) {
            if (file.isFile()) {
                if (file.getName().endsWith(METADATA_FILE_EXT)) {
                    metadataFileList.add(file);
                } else {
                    String key = file.getName().substring(0, file.getName().lastIndexOf('.'));
                    pictureFileMap.put(key, file);
                }
            }
        }

        Collections.sort(metadataFileList, new FileComparator(SORT_BY_FILENAME));

        int errorCount = 0;

        for (File metadataFile : metadataFileList) {
            String key = metadataFile.getName().substring(0, metadataFile.getName().lastIndexOf('.'));
            File pictureFile = pictureFileMap.get(key);

            BufferedReader metadataIn = null;

            try {
                metadataIn = new BufferedReader(new InputStreamReader(new FileInputStream(metadataFile), "UTF-8"));

                Gson gson = new Gson();

                OfflineQueueMetaDataElem metaData = gson.fromJson(metadataIn, OfflineQueueMetaDataElem.class);

                String loginKey = metaData.getServerUrl() + "~" + metaData.getUserid();

                String password = checkedLogins.get(loginKey);

                if (password != null) {
                    if (simulate) {
                        sentCount++;
                    } else {
                        if (sendBlogEntry(pictureFile, metaData, password, sentCount)) {

                            Log.d("arcoiris", "about to send metadata file " + metadataFile.getAbsolutePath() + " and picture file " + pictureFile.getAbsolutePath() + " to server " + metaData.getServerUrl());

                            sentCount++;

                            if (!pictureFile.delete()) {
                                Log.e("arcoiris", "failed to delete queued picture file " + pictureFile.getAbsolutePath());
                            }
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

        return sentCount;
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
            }
        } catch (FileNotFoundException fnfex) {
            Log.e("arcoiris", "picture file for queued blog entry not found", fnfex);
        }

        return success;
    }
}
