package de.webfilesys.util;

import de.webfilesys.ArcoirisBlog;
import de.webfilesys.Comment;
import de.webfilesys.GeoTag;
import de.webfilesys.MetaInfManager;
import de.webfilesys.attachment.AttachmentManager;
import de.webfilesys.config.BlogConfig;
import de.webfilesys.config.BlogConfigManager;
import de.webfilesys.daytitle.DayTitleManager;
import de.webfilesys.metainf.BlogMetaInfManager;
import de.webfilesys.state.BlogStateManager;
import de.webfilesys.user.TransientUser;
import de.webfilesys.user.UserManager;
import de.webfilesys.user.XmlUserManager;
import org.apache.log4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

public class UserMigrator {
    private static final Logger LOG = Logger.getLogger(UserMigrator.class);

    private static UserMigrator instance;

    public static synchronized UserMigrator getInstance() {
        if (instance == null) {
            instance = new UserMigrator();
        }
        return instance;
    }

    public void migrateAllUsers(boolean simulate, PrintWriter output) {
        UserManager userMgr = ArcoirisBlog.getInstance().getUserMgr();
        output.println("Automigration start");
        for (String userid : userMgr.getListOfUsers()) {
            TransientUser user = userMgr.getUser(userid);
            if ("blog".equals(user.getRole())) {
                output.println("<hr/>");
                output.println("<h3>migrating blog user: " + user.getUserid() + "</h3>");
                output.flush();
                migrateSingleUser(user, simulate, output);
            }
        }
    }

    public void migrateSingleUser(TransientUser user, boolean simulate, PrintWriter output) {
        UserManager userMgr = ArcoirisBlog.getInstance().getUserMgr();
        String userHomeDir = userMgr.getDocumentRoot(user.getUserid()).replace('/', File.separatorChar);

        String oldMetaInfFilePath = userHomeDir + File.separator + MetaInfManager.METAINF_FILE;
        File oldMetaInfFile = new File(oldMetaInfFilePath);
        if (!oldMetaInfFile.exists()) {
            output.print("<p>this user cannot be migrated: old metainf file not found: " + oldMetaInfFilePath + "</p>");
            return;
        }

        output.println("<pre>");

        HashMap<String, Boolean> dayTitlesMigrated = new HashMap<>();

        MetaInfManager metaInfMgr = MetaInfManager.getInstance();
        BlogMetaInfManager blogMetaInfMgr = BlogMetaInfManager.getInstance();

        File homeDirFile = new File(userHomeDir);

        File[] fileList = homeDirFile.listFiles();
        for (File file : fileList) {
            if (file.isFile()) {
                if (CommonUtils.isPictureFile(file)) {
                    migrateFile(file, user, userHomeDir, metaInfMgr, blogMetaInfMgr, simulate, output);

                    String blogDate = file.getName().substring(0, 10);
                    if (dayTitlesMigrated.get(blogDate) == null) {
                        migrateDayTitle(userHomeDir, blogDate, metaInfMgr, simulate, output);
                        dayTitlesMigrated.put(blogDate, Boolean.TRUE);
                    }
                }
            }
        }

        int unseenCommentCount = metaInfMgr.getUnseenCommentCount(userHomeDir);
        output.println("unseen comment count: " + unseenCommentCount);
        if (!simulate) {
            BlogStateManager.getInstance().setUnseenCommentCount(userHomeDir, unseenCommentCount);
        }

        boolean hasUnnotifiedComments = metaInfMgr.hasUnnotifiedComments(userHomeDir);
        output.println("hasUnnotifiedComments: " + hasUnnotifiedComments);
        if (!simulate) {
            BlogStateManager.getInstance().setUnnotifiedComments(userHomeDir, hasUnnotifiedComments);
        }

        migrateConfig(userHomeDir, metaInfMgr, simulate, output);

        if (!simulate) {
            File oldMetaInfFileMigrated = new File(oldMetaInfFile.getAbsolutePath() + "-migrated");
            oldMetaInfFile.renameTo(oldMetaInfFileMigrated);
        }

        output.println("</pre>");
    }

    private void migrateDayTitle(String homeDir, String blogDate, MetaInfManager metaInfMgr, boolean simulate, PrintWriter output) {
        String dayTitle = metaInfMgr.getDayTitle(homeDir, blogDate);
        if (dayTitle != null && !dayTitle.isEmpty()) {
            output.println("day title: " + dayTitle);
            if (!simulate) {
                DayTitleManager.getInstance().setDayTitle(homeDir, blogDate, dayTitle);
            }
        }
    }

    private void migrateConfig(String homeDir, MetaInfManager metaInfMgr, boolean simulate, PrintWriter output) {
        BlogConfigManager blogConfigMgr = BlogConfigManager.getInstance();

        boolean stagedPublication = metaInfMgr.isStagedPublication(homeDir);
        output.println("staged publication: " + stagedPublication);
        if (!simulate) {
            blogConfigMgr.setStagedPublication(homeDir, stagedPublication);
        }

        boolean notifyOnNewComment = metaInfMgr.isNotifyOnNewComment(homeDir);
        output.println("notifyOnNewComment: " + notifyOnNewComment);
        if (!simulate) {
            blogConfigMgr.setNotifyOnNewComment(homeDir, notifyOnNewComment);
        }

        int sortOrder = metaInfMgr.getSortOrder(homeDir);
        output.println("sort order: " + sortOrder);
        if (!simulate) {
            if (sortOrder == 1) {
                blogConfigMgr.setSortOrder(homeDir, BlogConfig.SortOrder.BLOG);
            } else if (sortOrder == 2) {
                blogConfigMgr.setSortOrder(homeDir, BlogConfig.SortOrder.DIARY);
            }
        }

        String titlePic =  metaInfMgr.getTitlePic(homeDir);
        output.println("title pic: " + titlePic);
        if (!simulate) {
            blogConfigMgr.setTitlePic(homeDir, titlePic);
        }

        String titleText = metaInfMgr.getDescription(homeDir, ".");
        output.println("title text: " + titleText);
        if (!simulate) {
            blogConfigMgr.setTitleText(homeDir, titleText);
        }
    }

    private void migrateFile(File file, TransientUser user, String homeDir, MetaInfManager metaInfMgr, BlogMetaInfManager blogMetaInfMgr,
                             boolean simulate, PrintWriter output) {
        output.println("migrating file: " + file.getAbsolutePath());

        String description = metaInfMgr.getDescription(homeDir, file.getName());
        output.println("  description: " + description);
        if (!simulate) {
            blogMetaInfMgr.setDescription(homeDir, file.getName(), description);
        }

        int status = metaInfMgr.getStatus(file.getAbsolutePath());
        output.println("  status: " + status);
        if (!simulate) {
            blogMetaInfMgr.setStatus(homeDir, file.getName(), status);
        }

        GeoTag geoTag = metaInfMgr.getGeoTag(homeDir, file.getName());
        if (geoTag != null) {
            output.println("  geoTag: " + geoTag.getLatitude() + ", " + geoTag.getLongitude());
        }
        if (!simulate) {
            blogMetaInfMgr.setGeoTag(homeDir, file.getName(), geoTag);
        }

        Vector<Comment> commentList = metaInfMgr.getListOfComments(homeDir, file.getName());
        if (commentList != null) {
            output.println("  comments:");
            for (Comment comment : commentList) {
                output.println("    comment: " + comment.getMessage());
                if (!simulate) {
                    blogMetaInfMgr.addComment(homeDir, file.getName(), comment);
                }
            }
        }

        int likes = metaInfMgr.getVisitorRatingCount(file.getAbsolutePath());
        if (likes > 0) {
            output.println("  likes: " + likes);
            if (!simulate) {
                for (int i = 0; i < likes; i++) {
                    blogMetaInfMgr.addLiker(homeDir, file.getName(), "anonymous-" + i);
                }
            }
        }

        boolean commentsSeenByOwner = metaInfMgr.isCommentsSeenByOwner(homeDir, file.getName());
        output.println("  comments seen by owner: "  + commentsSeenByOwner);
        if (!simulate) {
            blogMetaInfMgr.setCommentsSeenByOwner(homeDir, file.getName(), commentsSeenByOwner);
        }

        List<String> attachments = metaInfMgr.getListOfAttachments(homeDir, file.getName());
        if (attachments != null) {
            output.println("  attachments:");
            for (String attachment : attachments) {
                output.println("    attachment: " + attachment);
                if (!simulate) {
                    AttachmentManager.getInstance().addAttachment(homeDir, file.getName(), attachment);
                }
            }
        }
    }

}
