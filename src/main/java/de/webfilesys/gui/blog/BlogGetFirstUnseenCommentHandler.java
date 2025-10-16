package de.webfilesys.gui.blog;

import java.io.File;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import de.webfilesys.config.BlogConfig;
import de.webfilesys.config.BlogConfigManager;
import de.webfilesys.metainf.BlogMetaInfManager;
import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import de.webfilesys.gui.ajax.XmlRequestHandlerBase;
import de.webfilesys.util.CommonUtils;
import de.webfilesys.util.XmlUtil;

/**
 * @author Frank Hoehnel
 */
public class BlogGetFirstUnseenCommentHandler extends XmlRequestHandlerBase {

    private static final Logger LOG = Logger.getLogger(BlogGetFirstUnseenCommentHandler.class);
    private static final long MILLIS_ONE_DAY = 25l * 60l * 60l * 1000l; // 25 hours for daylight saving time period switch
    
    public BlogGetFirstUnseenCommentHandler(HttpServletRequest req, HttpServletResponse resp, HttpSession session, PrintWriter output, String uid) {
        super(req, resp, session, output, uid);
    }

    protected void process() {

        if (!checkWriteAccess()) {
            return;
        }

        String currentPath = userMgr.getDocumentRoot(uid).replace('/', File.separatorChar);
        
        boolean stagedPublication = BlogConfigManager.getInstance().isStagedPublication(currentPath);

        File blogDir = new File(currentPath);
        
        if (!blogDir.exists() || (!blogDir.isDirectory()) || (!blogDir.canRead())) {
            LOG.error("home directory of user " + uid + " is not a readable directory: " + currentPath);
            return;
        }

        String firstCommentFileName = null;
        
        File[] filesInDir = blogDir.listFiles();

        List<File> fileList = Arrays.asList(filesInDir);
        if (BlogConfigManager.getInstance().getSortOrder(currentPath) == BlogConfig.SortOrder.DIARY) {
            fileList.sort(new Comparator<File>() {
                @Override
                public int compare(File o1, File o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            });
        } else {
            fileList.sort(new Comparator<File>() {
                @Override
                public int compare(File o1, File o2) {
                    return o2.getName().compareTo(o1.getName());
                }
            });
        }

        for (File file : fileList) {
            if (file.isFile() && file.canRead()) {
                if (CommonUtils.isPictureFile(file)) {
                    if ((!stagedPublication) || (BlogMetaInfManager.getInstance().getStatus(file.getAbsolutePath()) != BlogMetaInfManager.STATUS_BLOG_EDIT)) {
                        String fileName = file.getName();
                        if (fileName.length() >= 10) {
                            int commentCount = BlogMetaInfManager.getInstance().getCommentCount(currentPath, fileName);
                            if ((commentCount > 0) && (!BlogMetaInfManager.getInstance().isCommentsSeenByOwner(currentPath, fileName))) {
                                firstCommentFileName = fileName;
                                break;
                            }
                        }
                    }
                }
            }
        }
        
        String firstCommentDate = firstCommentFileName.substring(0, 10);

        SimpleDateFormat linkDateFormat = new SimpleDateFormat("yyyy-MM-dd");

        String formattedLinkDate = "";
        
        try {
            Date blogDate = linkDateFormat.parse(firstCommentDate);

            Date linkDate = new Date(blogDate.getTime() + MILLIS_ONE_DAY);

            formattedLinkDate = linkDateFormat.format(linkDate);
        } catch (Exception ex) {
            Logger.getLogger(getClass()).error("failed to calculate date link", ex);
        }
        
        Element resultElement = doc.createElement("result");

        XmlUtil.setChildText(resultElement, "fileName", firstCommentFileName);

        XmlUtil.setChildText(resultElement, "linkDate", formattedLinkDate);

        doc.appendChild(resultElement);

        processResponse();
    }
    
}
