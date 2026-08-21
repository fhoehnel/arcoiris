package de.webfilesys.metainf;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.webfilesys.Comment;
import de.webfilesys.GeoTag;
import de.webfilesys.util.CommonUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class BlogMetaInfManager extends Thread {

    private static final Logger LOG = LogManager.getLogger(BlogMetaInfManager.class);

    private static final char CACHE_KEY_SEP = '/';
    
    public static final int STATUS_NONE = 0;
    public static final int STATUS_BLOG_EDIT = 1;
    public static final int STATUS_BLOG_PUBLISHED = 2;

    private static final int MAX_CACHE_SIZE = 500;

    private static BlogMetaInfManager instance;

    private final HashMap<String, MetaInfData> metaInfCache;

    private final HashMap<String, Boolean> cacheDirty;

    private BlogMetaInfManager() {
        metaInfCache = new HashMap<>();
        cacheDirty = new HashMap<>();
        this.start();
    }

    public static synchronized BlogMetaInfManager getInstance() {
        if (instance == null) {
            instance = new BlogMetaInfManager();
        }
        return instance;
    }

    private String getCacheKey(String path, String fileName) {
        return path + CACHE_KEY_SEP + fileName;
    }

    public void setDescription(String path, String fileName, String newDescription) {
        MetaInfData metaInfData = getOrCreateMetaInf(path, fileName);
        metaInfData.setDescription(newDescription);
        cacheDirty.put(getCacheKey(path, fileName), Boolean.TRUE);
    }

    public String getDescription(String absolutePath) {
        String[] partsOfPath = CommonUtils.splitPath(absolutePath);
        return getDescription(partsOfPath[0], partsOfPath[1]);
    }

    public String getDescription(String path, String fileName) {
        MetaInfData metaInfData = getOrLoadMetaInf(path, fileName);
        if (metaInfData == null) {
            return null;
        }
        return metaInfData.getDescription();
    }

    public void removeDescription(String absolutePath) {
        String[] partsOfPath = CommonUtils.splitPath(absolutePath);
        removeDescription(partsOfPath[0], partsOfPath[1]);
    }

    public void removeDescription(String path, String fileName) {
        MetaInfData metaInfData = getOrCreateMetaInf(path, fileName);
        metaInfData.setDescription(null);
        cacheDirty.put(getCacheKey(path, fileName), Boolean.TRUE);
    }

    public void setStatus(String absolutePath, int newStatus) {
        String[] partsOfPath = CommonUtils.splitPath(absolutePath);
        setStatus(partsOfPath[0], partsOfPath[1], newStatus);
    }

    public void setStatus(String path, String fileName, int newStatus) {
        MetaInfData metaInfData = getOrCreateMetaInf(path, fileName);
        metaInfData.setStatus(newStatus);
        cacheDirty.put(getCacheKey(path, fileName), Boolean.TRUE);
    }

    public int getStatus(String absolutePath) {
        String[] partsOfPath = CommonUtils.splitPath(absolutePath);
        return getStatus(partsOfPath[0], partsOfPath[1]);
    }

    public int getStatus(String path, String fileName) {
        MetaInfData metaInfData = getOrLoadMetaInf(path, fileName);
        if (metaInfData == null) {
            return STATUS_NONE;
        }
        return metaInfData.getStatus();
    }

    public void setGeoTag(String absolutePath, GeoTag newGeoTag) {
        String[] partsOfPath = CommonUtils.splitPath(absolutePath);
        setGeoTag(partsOfPath[0], partsOfPath[1], newGeoTag);
    }

    public void setGeoTag(String path, String fileName, GeoTag newGeoTag) {
        MetaInfData metaInfData = getOrCreateMetaInf(path, fileName);
        metaInfData.setGeoTag(newGeoTag);
        cacheDirty.put(getCacheKey(path, fileName), Boolean.TRUE);
    }

    public GeoTag getGeoTag(String absolutePath) {
        String[] partsOfPath = CommonUtils.splitPath(absolutePath);
        return getGeoTag(partsOfPath[0], partsOfPath[1]);
    }

    public GeoTag getGeoTag(String path, String fileName) {
        MetaInfData metaInfData = getOrLoadMetaInf(path, fileName);
        if (metaInfData == null) {
            return null;
        }
        return metaInfData.getGeoTag();
    }

    public void removeGeoTag(String path, String fileName) {
        MetaInfData metaInfData = getOrCreateMetaInf(path, fileName);
        metaInfData.setGeoTag(null);
        cacheDirty.put(getCacheKey(path, fileName), Boolean.TRUE);
    }

    public void addComment(String absolutePath, Comment newComment) {
        String[] partsOfPath = CommonUtils.splitPath(absolutePath);
        addComment(partsOfPath[0], partsOfPath[1], newComment);
    }

    public void addComment(String path, String fileName, Comment newComment) {
        MetaInfData metaInfData = getOrCreateMetaInf(path, fileName);
        metaInfData.addComment(newComment);
        cacheDirty.put(getCacheKey(path, fileName), Boolean.TRUE);
    }

    public List<Comment> getComments(String absolutePath) {
        String[] partsOfPath = CommonUtils.splitPath(absolutePath);
        return getComments(partsOfPath[0], partsOfPath[1]);
    }

    public List<Comment> getComments(String path, String fileName) {
        MetaInfData metaInfData = getOrLoadMetaInf(path, fileName);
        if (metaInfData == null) {
            return new ArrayList<>();
        }
        return metaInfData.getComments();
    }

    public int getCommentCount(String absolutePath) {
        String[] partsOfPath = CommonUtils.splitPath(absolutePath);
        return getCommentCount(partsOfPath[0], partsOfPath[1]);
    }

    public int getCommentCount(String path, String fileName) {
        MetaInfData metaInfData = getOrLoadMetaInf(path, fileName);
        if (metaInfData == null) {
            return 0;
        }
        return metaInfData.getComments() != null ? metaInfData.getComments().size() : 0;
    }

    public void removeComments(String absolutePath) {
        String[] partsOfPath = CommonUtils.splitPath(absolutePath);
        removeComments(partsOfPath[0], partsOfPath[1]);
    }

    public void removeComments(String path, String fileName) {
        MetaInfData metaInfData = getOrCreateMetaInf(path, fileName);
        metaInfData.setComments(new ArrayList<>());
        cacheDirty.put(getCacheKey(path, fileName), Boolean.TRUE);
    }

    public void setCommentsSeenByOwner(String absolutePath, boolean newVal) {
        String[] partsOfPath = CommonUtils.splitPath(absolutePath);
        setCommentsSeenByOwner(partsOfPath[0], partsOfPath[1], newVal);
    }

    public void setCommentsSeenByOwner(String path, String fileName, boolean newVal) {
        MetaInfData metaInfData = getOrCreateMetaInf(path, fileName);
        metaInfData.setCommentsSeenByOwner(newVal);
        cacheDirty.put(getCacheKey(path, fileName), Boolean.TRUE);
    }

    public boolean isCommentsSeenByOwner(String absolutePath) {
        String[] partsOfPath = CommonUtils.splitPath(absolutePath);
        return isCommentsSeenByOwner(partsOfPath[0], partsOfPath[1]);
    }

    public boolean isCommentsSeenByOwner(String path, String fileName) {
        MetaInfData metaInfData = getOrLoadMetaInf(path, fileName);
        return metaInfData != null && metaInfData.isCommentsSeenByOwner();
    }

    public boolean addLiker(String absolutePath, String visitorId) {
        String[] partsOfPath = CommonUtils.splitPath(absolutePath);
        return addLiker(partsOfPath[0], partsOfPath[1], visitorId);
    }

    public boolean addLiker(String path, String fileName, String visitorId) {
        MetaInfData metaInfData = getOrCreateMetaInf(path, fileName);
        if (metaInfData.addLiker(visitorId)) {
            cacheDirty.put(getCacheKey(path, fileName), Boolean.TRUE);
            return true;
        }
        return false;
    }

    public int getLikerCount(String absolutePath) {
        String[] partsOfPath = CommonUtils.splitPath(absolutePath);
        return getLikerCount(partsOfPath[0], partsOfPath[1]);
    }

    public int getLikerCount(String path, String fileName) {
        MetaInfData metaInfData = getOrLoadMetaInf(path, fileName);
        if (metaInfData.getLikers() != null) {
            return metaInfData.getLikers().size();
        }
        return 0;
    }

    public boolean alreadyLiked(String absolutePath, String visitorId) {
        String[] partsOfPath = CommonUtils.splitPath(absolutePath);
        return  alreadyLiked(partsOfPath[0], partsOfPath[1], visitorId);
    }

    public boolean alreadyLiked(String path, String fileName, String visitorId) {
        MetaInfData metaInfData = getOrLoadMetaInf(path, fileName);
        if (metaInfData.getLikers() == null) {
            return false;
        }
        for (String liker : metaInfData.getLikers()) {
            if (liker.equals(visitorId)) {
                return true;
            }
        }
        return false;
    }

    public boolean moveMetaInf(String path, String sourceFileName, String targetFileName) {
        File targetFile = new File(getMetaInfFilePath(path, targetFileName));
        if (targetFile.exists()) {
            LOG.error("target file of move operation already exists");
            return false;
        }
        MetaInfData targetMetaInf = metaInfCache.get(getCacheKey(path, targetFileName));
        if (targetMetaInf != null) {
            LOG.error("meta info already exist for target of move operation");
            return false;
        }
        File sourceFile = new File(getMetaInfFilePath(path, sourceFileName));
        MetaInfData sourceMetaInf = metaInfCache.get(getCacheKey(path, sourceFileName));
        if (sourceMetaInf == null) {
            if (!sourceFile.exists()) {
                // TODO: is this a valid constellation?
                LOG.info("source meta info of move operation does not exist");
                return false;
            }
            sourceMetaInf = loadMetaInfFromFile(path, sourceFileName);
        }
        metaInfCache.put(getCacheKey(path, targetFileName), sourceMetaInf);
        cacheDirty.put(getCacheKey(path, targetFileName), Boolean.TRUE);
        cacheDirty.remove(getCacheKey(path, sourceFileName));
        metaInfCache.remove(getCacheKey(path, sourceFileName));
        if (sourceFile.exists()) {
            if (!sourceFile.delete()) {
                LOG.error("source file of move operation could not be deleted: " + sourceFile.getAbsolutePath());
                return false;
            }
        }
        return true;
    }

    public void removeMetaInf(String path, String fileName) {
        metaInfCache.remove(getCacheKey(path, fileName));
        cacheDirty.remove(getCacheKey(path, fileName));
        File fileToRemove = new File(getMetaInfFilePath(path, fileName));
        if (fileToRemove.exists()) {
            if (!fileToRemove.delete()) {
                LOG.error("failed to remove metainf file: " + fileToRemove.getAbsolutePath());
            }
        }
    }

    /**
     * Cleanup the cache after deleting a user (all the files under it's home directory have already been deleted).
     * @param path the home dir of the deleted user
     */
    public void removeAllMetaInfOfUserFromCache(String path) {
        List<String> keysForDeletion = metaInfCache.keySet().stream().filter(key -> key.startsWith(path)).collect(Collectors.toList());
        keysForDeletion.forEach(key -> {
            LOG.info("removing metainf from cache for: " + key);
            cacheDirty.remove(key);
            metaInfCache.remove(key);
        });
    }

    private MetaInfData getOrLoadMetaInf(String path, String fileName) {
        MetaInfData metaInfData = metaInfCache.get(getCacheKey(path, fileName));
        if (metaInfData == null) {
            metaInfData = loadMetaInfFromFile(path, fileName);
        }
        return metaInfData;
    }

    private void limitCacheSize() {
        if (metaInfCache.size() > MAX_CACHE_SIZE) {
            String randomKey = metaInfCache.keySet().iterator().next();
            LOG.info("max cache size exceeded - removing random entry " + randomKey);
            if (cacheDirty.get(randomKey) != null) {
                saveMetaInfToFile(randomKey);
            }
            metaInfCache.remove(randomKey);
            cacheDirty.remove(randomKey);
        }
    }

    private MetaInfData getOrCreateMetaInf(String path, String fileName) {
        MetaInfData metaInfData = metaInfCache.get(getCacheKey(path, fileName));
        if (metaInfData == null) {
            metaInfData = loadMetaInfFromFile(path, fileName);
        }
        if (metaInfData == null) {
            limitCacheSize();
            metaInfData = new MetaInfData();
            metaInfCache.put(getCacheKey(path, fileName), metaInfData);
        }
        return metaInfData;
    }

    private String getMetaInfFilePath(String path, String fileName) {
        String fileNameWithoutExt = fileName.substring(0, fileName.lastIndexOf('.'));
        String metaInfFileName = fileNameWithoutExt + ".json";
        if (path.endsWith(File.separator)) {
            return path + metaInfFileName;
        }
        return path + File.separator + metaInfFileName;
    }

    private MetaInfData loadMetaInfFromFile(String path, String fileName) {
        limitCacheSize();
        String filePath = getMetaInfFilePath(path, fileName);
        File metaInfFile = new File(filePath);
        if (!metaInfFile.exists()) {
            MetaInfData newMetaInf = new MetaInfData();
            metaInfCache.put(getCacheKey(path, fileName), newMetaInf);
            return newMetaInf;
        }
        ObjectMapper mapper = new ObjectMapper();
        try {
            String json = Files.readString(Paths.get(filePath), StandardCharsets.UTF_8);
            MetaInfData metaInf = mapper.readValue(json, MetaInfData.class);
            metaInfCache.put(getCacheKey(path, fileName), metaInf);
            return metaInf;
        } catch (IOException ex) {
            LOG.error("failed to read metainf data for path " + filePath, ex);
            return null;
        }
    }

    private void saveMetaInfToFile(String cacheKey) {
        MetaInfData metaInfData = metaInfCache.get(cacheKey);
        if (metaInfData == null) {
            LOG.error("inconsistent cache dirty entry for key " + cacheKey);
            return;
        }
        
        int sepIdx = cacheKey.lastIndexOf(CACHE_KEY_SEP);
        String cachePath = cacheKey.substring(0, sepIdx);
        String cacheFileName = cacheKey.substring(sepIdx + 1);        
        String filePath = getMetaInfFilePath(cachePath, cacheFileName);
        String newFilePath = filePath + "-new";
        LOG.debug("saving meta info to file " + filePath);
        ObjectMapper mapper = new ObjectMapper();
        try {
            String jsonResult = mapper.writeValueAsString(metaInfData);
            Files.write(Paths.get(newFilePath), jsonResult.getBytes(StandardCharsets.UTF_8));
            File newFile = new File(newFilePath);
            if (newFile.exists() && newFile.canRead()) {
                boolean renameSuccess = true;
                File metaInfFile = new File(filePath);
                if (metaInfFile.exists()) {
                    boolean deleted = metaInfFile.delete();
                    if (!deleted) {
                        LOG.error("failed to remove old metainf file " + filePath);
                        renameSuccess = false;
                    } else {
                        renameSuccess = newFile.renameTo(metaInfFile);
                    }
                } else {
                    renameSuccess = newFile.renameTo(metaInfFile);
                }
                if (!renameSuccess) {
                    LOG.error("failed to rename metainf file " + newFilePath);
                }
            }
        } catch (IOException ex) {
            LOG.error("failed to write metainf to path " + filePath, ex);
        }
    }

    public synchronized void run() {
        boolean stop = false;

        while (!stop) {
            try {
                this.wait(60000);
                cacheDirty.keySet().forEach(this::saveMetaInfToFile);
                cacheDirty.clear();
            } catch (InterruptedException e) {
                cacheDirty.keySet().forEach(this::saveMetaInfToFile);
                LogManager.getLogger(getClass()).debug("BlogMetaInfManager ready for shutdown");
                stop = true;
            } catch (Throwable t) {
                LOG.error("unhandled exception in run", t);
            }
        }
    }
}
