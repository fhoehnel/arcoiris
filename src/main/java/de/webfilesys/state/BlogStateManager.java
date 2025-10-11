package de.webfilesys.state;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

public class BlogStateManager {

    private static final Logger LOG = Logger.getLogger(BlogStateManager.class);

    public static final String BLOG_STATE_JSON_FILE_NAME = "blogState.json";
    public static final String BLOG_STATE_JSON_NEW_FILE_NAME = "blogState-new.json";

    private static BlogStateManager instance;

    public static synchronized BlogStateManager getInstance() {
        if (instance == null) {
            instance = new BlogStateManager();
        }
        return instance;
    }

    // key: path (unique per user), value: state of blog
    private final HashMap<String, BlogState> stateMap = new HashMap<>();

    public void setUnnotifiedComments(String path, boolean unseenComments) {
        BlogState state = getOrLoadBlogState(path);
        state.setUnnotifiedComments(unseenComments);
        saveBlogState(path, state);
    }

    public boolean hasUnnotifiedComments(String path) {
        BlogState state = getOrLoadBlogState(path);
        return state.hasUnnotifiedComments();
    }

    public void setUnseenCommentCount(String path, int unseenCommentCount) {
        BlogState state = getOrLoadBlogState(path);
        state.setUnseenCommentCount(unseenCommentCount);
        saveBlogState(path, state);
    }

    public void incrUnseenCommentCount(String path) {
        BlogState state = getOrLoadBlogState(path);
        state.setUnseenCommentCount(state.getUnseenCommentCount() + 1);
        saveBlogState(path, state);
    }

    public void decrUnseenCommentCount(String path) {
        BlogState state = getOrLoadBlogState(path);
        if (state.getUnseenCommentCount() < 1) {
            LOG.warn("unseen comment count cannot be decremented");
            return;
        }
        state.setUnseenCommentCount(state.getUnseenCommentCount() - 1);
        saveBlogState(path, state);
    }

    public int getUnseenCommentCount(String path) {
        BlogState state = getOrLoadBlogState(path);
        return state.getUnseenCommentCount();
    }

    private BlogState getOrLoadBlogState(String path) {
        BlogState state = stateMap.get(path);
        if (state == null) {
            state = loadBlogState(path);
            stateMap.put(path, state);
        }
        return state;
    }

    private void saveBlogState(String path, BlogState blogState) {
        String blogStateNewFilePath = path + File.separator + BLOG_STATE_JSON_NEW_FILE_NAME;
        ObjectMapper mapper = new ObjectMapper();
        try {
            String jsonResult = mapper.writeValueAsString(blogState);
            Files.write(Paths.get(blogStateNewFilePath), jsonResult.getBytes(StandardCharsets.UTF_8));
            File newFile = new File(blogStateNewFilePath);
            if (newFile.exists() && newFile.canWrite()) {
                boolean renameSuccess = true;
                File blogStateFile = new File(path + File.separator + BLOG_STATE_JSON_FILE_NAME);
                if (blogStateFile.exists()) {
                    boolean deleted = blogStateFile.delete();
                    if (!deleted) {
                        LOG.error("failed to remove old blog state file");
                    } else {
                        renameSuccess = newFile.renameTo(blogStateFile);
                    }
                } else {
                    renameSuccess = newFile.renameTo(blogStateFile);
                }
                if (!renameSuccess) {
                    LOG.error("failed to rename blog state file " + blogStateNewFilePath);
                }
            }
        } catch (IOException ex) {
            LOG.error("failed to write blog state to path " + path, ex);
        }
    }

    private BlogState loadBlogState(String path) {
        String blogStateFilePath = path + File.separator + BLOG_STATE_JSON_FILE_NAME;
        File blogStateFile = new File(blogStateFilePath);
        if (!blogStateFile.exists()) {
            return new BlogState();
        }
        ObjectMapper mapper = new ObjectMapper();
        try {
            String json = Files.readString(Paths.get(blogStateFilePath), StandardCharsets.UTF_8);
            return mapper.readValue(json, BlogState.class);
        } catch (IOException ex) {
            LOG.error("failed to read blog state for path " + path, ex);
            return new BlogState();
        }
    }

}
