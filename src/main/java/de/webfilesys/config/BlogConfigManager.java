package de.webfilesys.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

public class BlogConfigManager {

    private static final Logger LOG = Logger.getLogger(BlogConfigManager.class);

    public static final String BLOG_CONFIG_JSON_FILE_NAME = "blogConfig.json";
    public static final String BLOG_CONFIG_JSON_NEW_FILE_NAME = "blogConfig-new.json";


    private static BlogConfigManager instance;

    public static synchronized BlogConfigManager getInstance() {
        if (instance == null) {
            instance = new BlogConfigManager();
        }
        return instance;
    }

    // key: path (unique per user), value: configuration of blog
    private final HashMap<String, BlogConfig> configMap = new HashMap<>();

    public void setStagedPublication(String path, boolean stagedPublication) {
        BlogConfig config = getOrLoadBlogConfig(path);
        config.setStagedPublication(stagedPublication);
        saveBlogConfig(path, config);
    }

    public void setNotifyOnNewComment(String path, boolean notifyOnNewComment) {
        BlogConfig config = getOrLoadBlogConfig(path);
        config.setNotifyOnNewComment(notifyOnNewComment);
        saveBlogConfig(path, config);
    }

    public void setSortOrder(String path, BlogConfig.SortOrder sortOrder) {
        BlogConfig config = getOrLoadBlogConfig(path);
        config.setSortOrder(sortOrder);
        saveBlogConfig(path, config);
    }

    public void setTitleText(String path, String titleText) {
        BlogConfig config = getOrLoadBlogConfig(path);
        config.setTitleText(titleText);
        saveBlogConfig(path, config);
    }

    public void setTitlePic(String path, String titlePic) {
        BlogConfig config = getOrLoadBlogConfig(path);
        config.setTitlePic(titlePic);
        saveBlogConfig(path, config);
    }

    public void unsetTitlePic(String path) {
        BlogConfig config = getOrLoadBlogConfig(path);
        config.setTitlePic(null);
        saveBlogConfig(path, config);
    }

    public boolean isStagedPublication(String path) {
        BlogConfig config = getOrLoadBlogConfig(path);
        return config.isStagedPublication();
    }

    public boolean isNotifyOnNewComment(String path) {
        BlogConfig config = getOrLoadBlogConfig(path);
        return config.isNotifyOnNewComment();
    }

    public BlogConfig.SortOrder getSortOrder(String path) {
        BlogConfig config = getOrLoadBlogConfig(path);
        return config.getSortOrder();
    }

    public String getTitlePic(String path) {
        BlogConfig config = getOrLoadBlogConfig(path);
        return config.getTitlePic();
    }

    public BlogConfig getConfig(String path) {
        return getOrLoadBlogConfig(path);
    }

    public void setConfig(String path, BlogConfig config) {
        saveBlogConfig(path, config);
    }

    private BlogConfig getOrLoadBlogConfig(String path) {
        BlogConfig config = configMap.get(path);
        if (config == null) {
            config = loadBlogConfig(path);
            configMap.put(path, config);
        }
        return config;
    }

    private void saveBlogConfig(String path, BlogConfig blogConfig) {
        String blogConfigNewFilePath = path + File.separator + BLOG_CONFIG_JSON_NEW_FILE_NAME;
        ObjectMapper mapper = new ObjectMapper();
        try {
            String jsonResult = mapper.writeValueAsString(blogConfig);
            Files.write(Paths.get(blogConfigNewFilePath), jsonResult.getBytes(StandardCharsets.UTF_8));
            File newFile = new File(blogConfigNewFilePath);
            if (newFile.exists() && newFile.canWrite()) {
                boolean renameSuccess = true;
                File blogConfigFile = new File(path + File.separator + BLOG_CONFIG_JSON_FILE_NAME);
                if (blogConfigFile.exists()) {
                    boolean deleted = blogConfigFile.delete();
                    if (!deleted) {
                        LOG.error("failed to remove old blog config file");
                    } else {
                        renameSuccess = newFile.renameTo(blogConfigFile);
                    }
                } else {
                    renameSuccess = newFile.renameTo(blogConfigFile);
                }
                if (!renameSuccess) {
                    LOG.error("failed to rename blog config file " + blogConfigNewFilePath);
                }
            }
        } catch (IOException ex) {
            LOG.error("failed to write blog config to path " + path, ex);
        }
    }

    private BlogConfig loadBlogConfig(String path) {
        String blogConfigFilePath = path + File.separator + BLOG_CONFIG_JSON_FILE_NAME;
        File blogConfigFile = new File(blogConfigFilePath);
        if (!blogConfigFile.exists()) {
            return new BlogConfig();
        }
        ObjectMapper mapper = new ObjectMapper();
        try {
            String json = Files.readString(Paths.get(blogConfigFilePath), StandardCharsets.UTF_8);
            return mapper.readValue(json, BlogConfig.class);
        } catch (IOException ex) {
            LOG.error("failed to read blog config for path " + path, ex);
            return new BlogConfig();
        }
    }

}
