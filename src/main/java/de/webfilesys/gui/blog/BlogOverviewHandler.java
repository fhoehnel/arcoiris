package de.webfilesys.gui.blog;

import java.io.File;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import de.webfilesys.FileComparator;
import de.webfilesys.MetaInfManager;
import de.webfilesys.gui.xsl.XslRequestHandlerBase;
import de.webfilesys.util.CommonUtils;
import de.webfilesys.util.UTF8URLEncoder;
import de.webfilesys.util.XmlUtil;

public class BlogOverviewHandler extends XslRequestHandlerBase {

    private static final long DAY_OFFSET_AFTER = 25l * 60l * 60l * 1000l;  // 25 hours to catch winter/summer time
    private static final long DAY_OFFSET_BEFORE = 23l * 60l * 60l * 1000l; // 23 hours to catch winter/summer time
    
    public BlogOverviewHandler(HttpServletRequest req, HttpServletResponse resp, HttpSession session, PrintWriter output, String uid) {
        super(req, resp, session, output, uid);
    }

    protected void process() {

        String currentPath = userMgr.getDocumentRoot(uid).replace('/', File.separatorChar);

        if (session.getAttribute("cwd") == null) {
            session.setAttribute("cwd", currentPath);
        }

        MetaInfManager metaInfMgr = MetaInfManager.getInstance();

        String blogTitle = metaInfMgr.getDescription(currentPath, ".");

        if (CommonUtils.isEmpty(blogTitle)) {
            blogTitle = "arcoiris blog";
        }

        Element blogElement = doc.createElement("blog");

        doc.appendChild(blogElement);

        XmlUtil.setChildText(blogElement, "language", language, false);
        XmlUtil.setChildText(blogElement, "skin", userMgr.getCSS(uid), false);

        if (readonly) {
            XmlUtil.setChildText(blogElement, "readonly", "true", false);
        }
        
        Boolean lowBandwidth = (Boolean) session.getAttribute(BlogSwitchLowBandwidthHandler.SESSION_KEY_LOW_BANDWIDTH);
        if (lowBandwidth != null) {
            XmlUtil.setChildText(blogElement, "lowBandwidthMode", "true");
        }

        XmlUtil.setChildText(blogElement, "blogTitle", blogTitle, false);

        String blogTitlePic = metaInfMgr.getTitlePic(currentPath);

        if (!CommonUtils.isEmpty(blogTitlePic)) {

            String titlePicPath;
            if (currentPath.endsWith(File.separator)) {
                titlePicPath = currentPath + blogTitlePic;
            } else {
                titlePicPath = currentPath + File.separator + blogTitlePic;
            }

            String titlePicSrc = req.getContextPath() + "/servlet?command=getFile&filePath=" + UTF8URLEncoder.encode(titlePicPath) + "&cached=true";

            XmlUtil.setChildText(blogElement, "blogTitlePic", titlePicSrc, false);
        }

        Element blogEntriesElement = doc.createElement("blogEntries");

        blogElement.appendChild(blogEntriesElement);

        int sortOrder = metaInfMgr.getSortOrder(currentPath);        
        if (sortOrder == 0) {
            sortOrder = BlogDateComparator.SORT_ORDER_BLOG;
        }
        
        XmlUtil.setChildText(blogElement, "sortOrder", Integer.toString(sortOrder), false);
        
        TreeMap<String, ArrayList<File>> blogDays = new TreeMap<String, ArrayList<File>>(new BlogDateComparator(sortOrder));

        boolean stagedPublication = metaInfMgr.isStagedPublication(currentPath);

        File blogDir = new File(currentPath);
        
        if (!blogDir.exists() || (!blogDir.isDirectory()) || (!blogDir.canRead())) {
            Logger.getLogger(getClass()).error("home directory of user " + uid + " is not a readable directory: " + currentPath);
            return;
        }

        File[] filesInDir = blogDir.listFiles();

        for (int i = 0; i < filesInDir.length; i++) {
            if (filesInDir[i].isFile() && filesInDir[i].canRead()) {

                if (CommonUtils.isPictureFile(filesInDir[i])) {

                    if ((!readonly) || (!stagedPublication) || (metaInfMgr.getStatus(filesInDir[i].getAbsolutePath()) != MetaInfManager.STATUS_BLOG_EDIT)) {

                        String fileName = filesInDir[i].getName();
                        if (fileName.length() >= 10) {
                            String blogDate = fileName.substring(0, 10);

                            ArrayList<File> entriesOfDay = blogDays.get(blogDate);
                            if (entriesOfDay == null) {
                                entriesOfDay = new ArrayList<File>();
                                blogDays.put(blogDate, entriesOfDay);
                            }
                            entriesOfDay.add(filesInDir[i]);
                        }
                    }
                }
            }
        }

        if (blogDays.size() > 0) {

            int globalPicCounter = 0;
            
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

            for (String blogDate : blogDays.keySet()) {
                try {
                    Date day = dateFormat.parse(blogDate);

                    Element blogDateElement = doc.createElement("blogDate");

                    blogEntriesElement.appendChild(blogDateElement);

                    XmlUtil.setChildText(blogDateElement, "plainDate", blogDate, false);
                    XmlUtil.setChildText(blogDateElement, "formattedDate", formatBlogDate(day), false);

                    Date theDayAfter = new Date(day.getTime() + DAY_OFFSET_AFTER);
                    XmlUtil.setChildText(blogDateElement, "dayAfter", dateFormat.format(theDayAfter), false);
                    
                    Date theDayBefore = new Date(day.getTime() - DAY_OFFSET_BEFORE);
                    XmlUtil.setChildText(blogDateElement, "dayBefore", dateFormat.format(theDayBefore), false);
                    
                    String dayTitle = metaInfMgr.getDayTitle(getCwd(), blogDate);
                    if (!CommonUtils.isEmpty(dayTitle)) {
                        XmlUtil.setChildText(blogDateElement, "dayTitle", dayTitle, true);
                    }
                    
                    ArrayList<File> entriesOfDay = blogDays.get(blogDate);

                    if ((entriesOfDay != null) && (entriesOfDay.size() > 0)) {

                        if (entriesOfDay.size() > 1) {
                            Collections.sort(entriesOfDay, new FileComparator());
                        }

                        Element dayEntriesElement = doc.createElement("dayEntries");

                        blogDateElement.appendChild(dayEntriesElement);

                        for (File file : entriesOfDay) {

                            Element fileElement = doc.createElement("file");

                            dayEntriesElement.appendChild(fileElement);

                            String actFilename = file.getName();

                            fileElement.setAttribute("name", actFilename);

                            fileElement.setAttribute("id", Integer.toString(globalPicCounter));

                            String srcPathForScript = req.getContextPath() + "/servlet?command=getFile&fileName=" + UTF8URLEncoder.encode(file.getName()) + "&cached=true";

                            XmlUtil.setChildText(fileElement, "imgPathForScript", srcPathForScript);
                            
                            String thumbSrcUrl = srcPathForScript + "&thumb=true";                                            
                            
                            XmlUtil.setChildText(fileElement, "imgPath", thumbSrcUrl);

                            if (stagedPublication && (!readonly) && (metaInfMgr.getStatus(file.getAbsolutePath()) == MetaInfManager.STATUS_BLOG_EDIT)) {
                                XmlUtil.setChildText(fileElement, "staged", "true");
                            }
                            
                            globalPicCounter++;
                        }
                    }

                } catch (ParseException pex) {
                    Logger.getLogger(getClass()).error("invalid blog date format in " + blogDate, pex);
                }
            } 
        } else {
            XmlUtil.setChildText(blogElement, "empty", "true");
        }

        processResponse("blog/blogOverview.xsl", req);
    }

}
