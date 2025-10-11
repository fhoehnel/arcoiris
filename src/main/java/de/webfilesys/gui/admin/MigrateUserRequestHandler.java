package de.webfilesys.gui.admin;

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
import de.webfilesys.util.CommonUtils;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.swing.*;
import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

public class MigrateUserRequestHandler extends AdminRequestHandler {

    private static final Logger LOG = Logger.getLogger(MigrateUserRequestHandler.class);

    public MigrateUserRequestHandler(HttpServletRequest req, HttpServletResponse resp, HttpSession session, PrintWriter output, String uid) {
        super(req, resp, session, output, uid);
    }

    protected void process() {
        String userid = getParameter("userid");
        boolean simulate = getParameter("simulate") != null;

        TransientUser user = userMgr.getUser(userid);
        if (user == null) {
            LOG.error("user not found: " + userid);
            return;
        }

        output.print("<html>");
        output.print("<head>");

        output.print("<title> arcoiris Blog Administration: Migrate User </title>");

        output.println("<link rel=\"stylesheet\" type=\"text/css\" href=\"" + req.getContextPath() + "/styles/common.css\">");
        output.println("<link rel=\"stylesheet\" type=\"text/css\" href=\"" + req.getContextPath() + "/styles/admin.css\">");

        output.print("</head>");
        output.print("<body>");

        if (simulate) {
            headLine("arcoiris Blog Administration: Simulate User Migration: " + userid);
        } else {
            headLine("arcoiris Blog Administration: Migrate User " + userid);
        }
        output.flush();

        migrateUser(user, simulate, output);

        output.println("<hr/>");
        if (simulate) {
            output.println("<p>migration simulation completed</p>");
        } else {
            output.println("<p>migration completed</p>");
        }

        output.println("<br/>");
        output.println("<input type=\"button\" value=\"Return to user list\" onclick=\"window.location.href='" + req.getContextPath() + "/servlet?command=admin&cmd=userList'\">");

        output.println("<script type=\"text/javascript\">setTimeout(\"window.scrollTo(0,1000000)\",1000);</script>");

        output.println("</body>");

        output.println("</html>");
        output.flush();
    }

    private void migrateUser(TransientUser user, boolean simulate, PrintWriter output) {
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
