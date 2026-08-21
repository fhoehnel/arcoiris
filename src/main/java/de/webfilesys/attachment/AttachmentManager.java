package de.webfilesys.attachment;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.webfilesys.servlet.UploadServlet;
import de.webfilesys.util.CommonUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class AttachmentManager {

    private static final Logger LOG = LogManager.getLogger(AttachmentManager.class);
    public static final String ATTACHMENT_INDEX_JSON_FILE_NAME = "attachments.json";
    public static final String ATTACHMENT_INDEX_JSON_NEW_FILE_NAME = "attachments-new.json";

    // key: path (unique per user), value: map of attachments (key: filename)
    private HashMap<String, HashMap<String, List<BlogAttachment>>> attachmentMap = new HashMap<>();

    private static AttachmentManager instance;

    public AttachmentManager() {
        attachmentMap = new HashMap<>();
    }

    public static synchronized AttachmentManager getInstance() {
        if (instance == null) {
            instance = new AttachmentManager();
        }
        return instance;
    }

    private boolean isGpsTrack(String attachmentFileName) {
        return attachmentFileName.endsWith(".GPX") || attachmentFileName.endsWith(".gpx");
    }

    public void addAttachment(String path, String blogPicFileName, String attachmentFileName) {
        BlogAttachment attachment = new BlogAttachment();
        attachment.setFileName(attachmentFileName);
        attachment.setType(isGpsTrack(attachmentFileName) ? BlogAttachment.AttachmentType.GPX : BlogAttachment.AttachmentType.OTHER);

        HashMap<String, List<BlogAttachment>> userAttachmentMap = attachmentMap.get(path);
        if (userAttachmentMap == null) {
            userAttachmentMap = loadAttachmentsOfUser(path);
        }
        if (userAttachmentMap == null) {
            userAttachmentMap = new HashMap<>();
        }
        List<BlogAttachment> attachments = userAttachmentMap.get(blogPicFileName);
        if (attachments == null) {
            attachments = new ArrayList<>();
            userAttachmentMap.put(blogPicFileName, attachments);
        }
        attachments.add(attachment);
        saveAttachmentsOfUser(path, userAttachmentMap);
    }

    public List<String> getAttachments(String absolutePath) {
        String[] partsOfPath = CommonUtils.splitPath(absolutePath);
        return getAttachments(partsOfPath[0], partsOfPath[1]);
    }

    public List<String> getAttachments(String path, String blogPicFileName) {
        HashMap<String, List<BlogAttachment>> userAttachmentMap = attachmentMap.get(path);
        if (userAttachmentMap == null) {
            userAttachmentMap = loadAttachmentsOfUser(path);
        }
        if (!userAttachmentMap.containsKey(blogPicFileName)) {
            return new  ArrayList<>();
        }
        return userAttachmentMap.get(blogPicFileName).stream().map(BlogAttachment::getFileName).collect(Collectors.toList());
    }

    public boolean removeAttachments(String path, String blogPicFileName) {

        boolean overAllSuccess = true;
        List<String> attachmentNames = getAttachments(path, blogPicFileName);
        for (String attachmentName : attachmentNames) {
            StringBuffer filePath = new StringBuffer(path);
            if (!path.endsWith(File.separator)) {
                filePath.append(File.separatorChar);
            }
            filePath.append(UploadServlet.SUBDIR_ATTACHMENT);
            filePath.append(File.separator);
            filePath.append(attachmentName);

            boolean success = false;
            File attachmentFile = new File(filePath.toString());
            if (attachmentFile.exists() && attachmentFile.isFile() && attachmentFile.canWrite()) {
                if (attachmentFile.delete()) {
                    success = true;
                }
            }
            if (!success) {
                LogManager.getLogger(getClass()).error("failed to delete attachment file " + filePath);
                overAllSuccess = false;
            }
        }
        HashMap<String, List<BlogAttachment>> userAttachmentMap = attachmentMap.get(path);
        if (userAttachmentMap != null) {
            userAttachmentMap.remove(blogPicFileName);
            saveAttachmentsOfUser(path, userAttachmentMap);
        }
        return overAllSuccess;
    }

    public boolean moveAttachments(String path, String sourceFileName, String targetFileName) {
        HashMap<String, List<BlogAttachment>> userAttachmentMap = attachmentMap.get(path);
        if (userAttachmentMap == null) {
            userAttachmentMap = loadAttachmentsOfUser(path);
        }
        List<BlogAttachment> attachmentsOfTarget = userAttachmentMap.get(targetFileName);
        if (attachmentsOfTarget != null && !attachmentsOfTarget.isEmpty()) {
            LOG.error("attachments already exist for target of move operation: " + targetFileName);
            return false;
        }
        List<BlogAttachment> attachmentsOfSource = userAttachmentMap.get(sourceFileName);
        if (attachmentsOfSource != null) {
            userAttachmentMap.put(targetFileName, attachmentsOfSource);
            userAttachmentMap.remove(sourceFileName);
            saveAttachmentsOfUser(path, userAttachmentMap);
        }
        return true;
    }

    /**
     * Cleanup the cache after deleting a user (all the files under it's home directory have already been deleted).
     * @param path the home dir of the deleted user
     */
    public void removeAllAttachmentsOfUserFromCache(String path) {
        attachmentMap.remove(path);
    }

    public boolean gpxTracksExist(String path) {
        HashMap<String, List<BlogAttachment>> userAttachmentMap = attachmentMap.get(path);
        if (userAttachmentMap == null) {
            userAttachmentMap = loadAttachmentsOfUser(path);
        }
        boolean gpxTrackFound = false;
        Iterator<Map.Entry<String, List<BlogAttachment>>> iter = userAttachmentMap.entrySet().iterator();
        while (!gpxTrackFound && iter.hasNext()) {
            Map.Entry<String, List<BlogAttachment>> entry = iter.next();
            List<BlogAttachment> attachments = entry.getValue();
            for (BlogAttachment attachment : attachments) {
                if (isGpsTrack(attachment.getFileName())) {
                    gpxTrackFound = true;
                }
            }
        }
        return gpxTrackFound;
    }

    private void saveAttachmentsOfUser(String path, HashMap<String, List<BlogAttachment>> userAttachments) {
        String attachmentIdxNewFilePath = path + File.separator + ATTACHMENT_INDEX_JSON_NEW_FILE_NAME;
        ObjectMapper mapper = new ObjectMapper();
        try {
            String jsonResult = mapper.writeValueAsString(userAttachments);
            Files.write(Paths.get(attachmentIdxNewFilePath), jsonResult.getBytes(StandardCharsets.UTF_8));
            File newFile = new File(attachmentIdxNewFilePath);
            if (newFile.exists() && newFile.canRead()) {
                boolean renameSuccess = true;
                File attachmentIdxFile = new File(path + File.separator + ATTACHMENT_INDEX_JSON_FILE_NAME);
                if (attachmentIdxFile.exists()) {
                    boolean deleted = attachmentIdxFile.delete();
                    if (!deleted) {
                        LOG.error("failed to remove old attachment index file");
                    } else {
                        renameSuccess = newFile.renameTo(attachmentIdxFile);
                    }
                } else {
                    renameSuccess = newFile.renameTo(attachmentIdxFile);
                }
                if (!renameSuccess) {
                    LOG.error("failed to rename attachment index file " + attachmentIdxNewFilePath);
                }
            }
        } catch (IOException ex) {
            LOG.error("failed to write attachment index to path " + path, ex);
        }
    }

    private HashMap<String, List<BlogAttachment>> loadAttachmentsOfUser(String path) {
        String attachmentIdxFilePath = path + File.separator + ATTACHMENT_INDEX_JSON_FILE_NAME;
        File attachmentIdxFile = new File(attachmentIdxFilePath);
        if (!attachmentIdxFile.exists()) {
            return new HashMap<>();
        }
        ObjectMapper mapper = new ObjectMapper();
        try {
            String json = Files.readString(Paths.get(attachmentIdxFilePath), StandardCharsets.UTF_8);
            TypeReference<HashMap<String, List<BlogAttachment>>> typeRef = new TypeReference<>() {};
            HashMap<String, List<BlogAttachment>> userAttachmentMap = mapper.readValue(json, typeRef);
            attachmentMap.put(path, userAttachmentMap);
            return userAttachmentMap;
        } catch (IOException ex) {
            LOG.error("failed to read attachment index for path " + path, ex);
            return new HashMap<>();
        }
    }

}
