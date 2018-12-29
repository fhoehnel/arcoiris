package de.webfilesys.gui.blog;

import java.io.File;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import de.webfilesys.MetaInfManager;
import de.webfilesys.gui.ajax.XmlRequestHandlerBase;
import de.webfilesys.util.CommonUtils;
import de.webfilesys.util.XmlUtil;

/**
 * @author Frank Hoehnel
 */
public class BlogGetFirstUnseenCommentHandler extends XmlRequestHandlerBase {

    private static final long MILLIS_ONE_DAY = 24l * 60l * 60l * 1000l;
    
    public BlogGetFirstUnseenCommentHandler(HttpServletRequest req, HttpServletResponse resp, HttpSession session, PrintWriter output, String uid) {
        super(req, resp, session, output, uid);
    }

    protected void process() {

        if (!checkWriteAccess()) {
            return;
        }

        String currentPath = userMgr.getDocumentRoot(uid).replace('/', File.separatorChar);
        
        MetaInfManager metaInfMgr = MetaInfManager.getInstance();

        boolean stagedPublication = metaInfMgr.isStagedPublication(currentPath);
        
        File blogDir = new File(currentPath);
        
        if (!blogDir.exists() || (!blogDir.isDirectory()) || (!blogDir.canRead())) {
            Logger.getLogger(getClass()).error("home directory of user " + uid + " is not a readable directory: " + currentPath);
            return;
        }

        String firstCommentFileName = null;
        
        File[] filesInDir = blogDir.listFiles();

        for (int i = 0; i < filesInDir.length; i++) {
            if (filesInDir[i].isFile() && filesInDir[i].canRead()) {

                if (CommonUtils.isPictureFile(filesInDir[i])) {

                    if ((!stagedPublication) || (metaInfMgr.getStatus(filesInDir[i].getAbsolutePath()) != MetaInfManager.STATUS_BLOG_EDIT)) {

                        String fileName = filesInDir[i].getName();
                        if (fileName.length() >= 10) {
                            
                            int commentCount = metaInfMgr.countComments(currentPath, fileName);
                            if ((commentCount > 0) && (!metaInfMgr.isCommentsSeenByOwner(currentPath, fileName))) {
                                
                                if ((firstCommentFileName == null) || fileName.compareTo(firstCommentFileName) > 0) {
                                    firstCommentFileName = fileName;
                                }
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
