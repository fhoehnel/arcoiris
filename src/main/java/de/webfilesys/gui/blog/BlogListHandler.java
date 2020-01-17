package de.webfilesys.gui.blog;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.ProcessingInstruction;

import de.webfilesys.ArcoirisBlog;
import de.webfilesys.Constants;
import de.webfilesys.FileComparator;
import de.webfilesys.GeoTag;
import de.webfilesys.IconManager;
import de.webfilesys.MetaInfManager;
import de.webfilesys.graphics.BlogThumbnailHandler;
import de.webfilesys.graphics.ScaledImage;
import de.webfilesys.gui.xsl.XslRequestHandlerBase;
import de.webfilesys.servlet.VisitorServlet;
import de.webfilesys.util.CommonUtils;
import de.webfilesys.util.UTF8URLEncoder;
import de.webfilesys.util.XmlUtil;

public class BlogListHandler extends XslRequestHandlerBase {

    public static final int DEFAULT_PAGE_SIZE = 6;

    public static final String SESSION_KEY_BEFORE_DAY = "blogBeforeDay";

    public static final String SESSION_KEY_AFTER_DAY = "blogAfterDay";

    public BlogListHandler(HttpServletRequest req, HttpServletResponse resp, HttpSession session, PrintWriter output, String uid) {
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

        ProcessingInstruction xslRef = doc.createProcessingInstruction("xml-stylesheet", "type=\"text/xsl\" href=\"" + req.getContextPath() + "/xsl/blog/blogList.xsl\"");

        doc.insertBefore(xslRef, blogElement);

        XmlUtil.setChildText(blogElement, "language", language, false);
        XmlUtil.setChildText(blogElement, "skin", userMgr.getCSS(uid), false);

        if (readonly) {
            XmlUtil.setChildText(blogElement, "readonly", "true", false);
        }
        
        Boolean lowBandwidth = (Boolean) session.getAttribute(BlogSwitchLowBandwidthHandler.SESSION_KEY_LOW_BANDWIDTH);
        if (lowBandwidth != null) {
            XmlUtil.setChildText(blogElement, "lowBandwidthMode", "true");
        }

        XmlUtil.setChildText(blogElement, "attachmentMaxSize", Long.toString(ArcoirisBlog.getInstance().getAttachmentMaxSize()));

        String posInPage = req.getParameter("posInPage");
        
        if ((posInPage != null) && (!posInPage.isEmpty())) {
            XmlUtil.setChildText(blogElement, "posInPage", posInPage, false);
        }

        String positionToFile = getParameter("positionToFile");

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

            // TODO: determine screenWidth, screenHeight
            int screenWidth = Constants.DEFAULT_SCREEN_WIDTH;
            int screenHeight = Constants.DEFAULT_SCREEN_HEIGHT;

            // TODO: make configurable
            int thumbnailSize = 400;

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

            Date pageAfterDay = null;

            String afterDay = req.getParameter("afterDay");
            if (!CommonUtils.isEmpty(afterDay)) {
                try {
                    pageAfterDay = dateFormat.parse(afterDay);
                    session.removeAttribute(SESSION_KEY_BEFORE_DAY);
                    session.setAttribute(SESSION_KEY_AFTER_DAY, pageAfterDay);
                } catch (ParseException pex) {
                    Logger.getLogger(getClass()).error("invalid date format in paging parameter " + afterDay, pex);
                }
            }

            Date pageBeforeDay = null;

            String beforeDay = getParameter("beforeDay");

            if (!CommonUtils.isEmpty(beforeDay)) {
                try {
                    pageBeforeDay = dateFormat.parse(beforeDay);
                    session.removeAttribute(SESSION_KEY_AFTER_DAY);
                    session.setAttribute(SESSION_KEY_BEFORE_DAY, pageBeforeDay);
                } catch (ParseException pex) {
                    Logger.getLogger(getClass()).error("invalid date format in paging parameter " + beforeDay, pex);
                }
            }

            if ((pageAfterDay == null) && (pageBeforeDay == null)) {
                pageAfterDay = (Date) session.getAttribute(SESSION_KEY_AFTER_DAY);
                if (pageAfterDay == null) {
                    pageBeforeDay = (Date) session.getAttribute(SESSION_KEY_BEFORE_DAY);
                }
            }

            int daysPageSize = DEFAULT_PAGE_SIZE;

            int pageSize = userMgr.getPageSize(uid);

            if (pageSize >= 1) {
                daysPageSize = pageSize;
            }

            boolean firstPage = true;
            boolean lastPage = true;

            ArrayList<String> daysOnPage = new ArrayList<String>();

            int daysIncluded = 0;

            for (String blogDate : blogDays.keySet()) {
                try {
                    Date day = dateFormat.parse(blogDate);

                    if (pageBeforeDay != null) {
                        if (sortOrder == BlogDateComparator.SORT_ORDER_BLOG) {
                            if (day.getTime() < pageBeforeDay.getTime()) {
                                if (daysIncluded < daysPageSize) {
                                    daysOnPage.add(blogDate);
                                    daysIncluded++;
                                } else {
                                    lastPage = false;
                                }
                            } else {
                                firstPage = false;
                            }
                        } else { // BlogDateComparator.SORT_ORDER_DIARY
                            if (day.getTime() < pageBeforeDay.getTime()) {
                                daysOnPage.add(blogDate);
                                daysIncluded++;
                                if (daysIncluded > daysPageSize) {
                                    daysOnPage.remove(0);
                                    daysIncluded--;
                                    firstPage = false;
                                }
                            } else {
                                lastPage = false;
                            }
                        }
                    } else if (pageAfterDay != null) {
                        if (sortOrder == BlogDateComparator.SORT_ORDER_BLOG) {
                            if (day.getTime() > pageAfterDay.getTime()) {
                                daysOnPage.add(blogDate);
                                daysIncluded++;
                                if (daysIncluded > daysPageSize) {
                                    daysOnPage.remove(0);
                                    daysIncluded--;
                                    firstPage = false;
                                }
                            } else {
                                lastPage = false;
                            }
                        } else { // BlogDateComparator.SORT_ORDER_DIARY
                            if (day.getTime() > pageAfterDay.getTime()) {
                                if (daysIncluded < daysPageSize) {
                                    daysOnPage.add(blogDate);
                                    daysIncluded++;
                                } else {
                                    lastPage = false;
                                }
                            } else {
                                firstPage = false;
                            }
                        }
                    } else {
                        if (daysIncluded < daysPageSize) {
                            daysOnPage.add(blogDate);
                            daysIncluded++;
                        } else {
                            lastPage = false;
                        }
                    }
                } catch (ParseException pex) {
                    Logger.getLogger(getClass()).error("invalid blog date format in " + blogDate, pex);
                }
            }
            
            if (daysOnPage.size() == 0) { 
                Object[] dateKeys = blogDays.keySet().toArray();
                if (sortOrder == BlogDateComparator.SORT_ORDER_BLOG) {
                    // selected date is before the oldest blog entry - show the oldest entry
                    daysOnPage.add((String) dateKeys[dateKeys.length - 1]);
                    firstPage = (blogDays.size() < 2);
                    lastPage = true;
                } else {
                    if (pageAfterDay != null) {
                        daysOnPage.add((String) dateKeys[dateKeys.length - 1]);
                        firstPage = (blogDays.size() < 2);
                        lastPage = true;
                    } else {
                        daysOnPage.add((String) dateKeys[0]);
                        lastPage = (blogDays.size() < 2);
                        firstPage = true;
                    }
                }
            }
            
            if (daysOnPage.size() > 0) {
                String prevPageBefore = daysOnPage.get(0);
                String nextPageAfter = daysOnPage.get(daysOnPage.size() - 1);

                Element pagingElement = doc.createElement("paging");
                blogElement.appendChild(pagingElement);

                if (!firstPage) {
                    XmlUtil.setChildText(pagingElement, "prevPageBefore", prevPageBefore);
                    XmlUtil.setChildText(pagingElement, "firstDay", blogDays.firstKey());
                }

                if (!lastPage) {
                    XmlUtil.setChildText(pagingElement, "nextPageAfter", nextPageAfter);
                    XmlUtil.setChildText(pagingElement, "lastDay", blogDays.lastKey());
                }

                Date dateRangeFrom = null;
                Date dateRangeUntil = null;

                int globalEntryCounter = 0;

                for (String blogDate : daysOnPage) {
                    try {
                        Date day = dateFormat.parse(blogDate);

                        if (dateRangeUntil == null) {
                            dateRangeUntil = day;
                        }

                        dateRangeFrom = day;

                        Element blogDateElement = doc.createElement("blogDate");

                        blogEntriesElement.appendChild(blogDateElement);

                        XmlUtil.setChildText(blogDateElement, "plainDate", blogDate, false);
                        XmlUtil.setChildText(blogDateElement, "formattedDate", formatBlogDate(day), false);

                        ArrayList<File> entriesOfDay = blogDays.get(blogDate);

                        if ((entriesOfDay != null) && (entriesOfDay.size() > 0)) {

                            if (entriesOfDay.size() > 1) {
                                Collections.sort(entriesOfDay, new FileComparator());
                            }

                            Element dayEntriesElement = doc.createElement("dayEntries");

                            blogDateElement.appendChild(dayEntriesElement);

                            int i = 0;

                            for (File file : entriesOfDay) {

                                globalEntryCounter++;

                                Element fileElement = doc.createElement("file");

                                dayEntriesElement.appendChild(fileElement);

                                String actFilename = file.getName();

                                fileElement.setAttribute("name", actFilename);

                                fileElement.setAttribute("id", Integer.toString(i));

                                if (positionToFile != null) {
                                    if (actFilename.equals(positionToFile)) {
                                        XmlUtil.setChildText(blogElement, "posInPage", Integer.toString(globalEntryCounter), false);
                                    }
                                }

                                if (globalEntryCounter % 2 == 0) {
                                    XmlUtil.setChildText(fileElement, "align", "right");
                                } else {
                                    XmlUtil.setChildText(fileElement, "align", "left");
                                }

                                String description = null;

                                description = metaInfMgr.getDescription(file.getAbsolutePath());

                                if ((description != null) && (description.trim().length() > 0)) {
                                    Element descrElem = doc.createElement("description");
                                    fileElement.appendChild(descrElem);
                                    appendDescrFragments(description, descrElem);
                                }

                                int commentCount = metaInfMgr.countComments(file.getAbsolutePath());

                                XmlUtil.setChildText(fileElement, "comments", Integer.toString(commentCount));

                                if (!readonly) {
                                    if ((commentCount > 0) && (!metaInfMgr.isCommentsSeenByOwner(file.getAbsolutePath()))) {
                                        XmlUtil.setChildText(fileElement, "newComments", "true");
                                    }
                                }

                                int voteCount = metaInfMgr.getVisitorRatingCount(file.getAbsolutePath());

                                XmlUtil.setChildText(fileElement, "voteCount", Integer.toString(voteCount));

                                boolean imgFound = true;

                                ScaledImage scaledImage = null;

                                try {
                                    scaledImage = new ScaledImage(file.getAbsolutePath(), screenWidth - 100, screenHeight - 135);
                                } catch (IOException io1) {
                                    Logger.getLogger(getClass()).error("failed to get scaled image dimensions", io1);
                                    imgFound = false;
                                }

                                if (imgFound) {
                                    XmlUtil.setChildText(fileElement, "imgType", Integer.toString(scaledImage.getImageType()));
                                    XmlUtil.setChildText(fileElement, "xpix", Integer.toString(scaledImage.getRealWidth()));
                                    XmlUtil.setChildText(fileElement, "ypix", Integer.toString(scaledImage.getRealHeight()));

                                    int thumbWidth = 0;
                                    int thumbHeight = 0;

                                    boolean useThumbnail = true;
                                    
                                    String imgPath = BlogThumbnailHandler.getInstance().getPathOfExistingThumbnail(file.getAbsolutePath());

                                    if (imgPath == null) {
                                        useThumbnail = false;
                                        imgPath = file.getAbsolutePath();
                                        if ((scaledImage.getRealWidth() <= thumbnailSize) && (scaledImage.getRealHeight() <= thumbnailSize)) {
                                            thumbHeight = scaledImage.getRealHeight();
                                            thumbWidth = scaledImage.getRealWidth();
                                        } else {
                                            if (scaledImage.getRealHeight() > scaledImage.getRealWidth()) {
                                                thumbHeight = thumbnailSize;
                                                thumbWidth = scaledImage.getRealWidth() * thumbnailSize / scaledImage.getRealHeight();
                                            } else {
                                                thumbWidth = thumbnailSize;
                                                thumbHeight = scaledImage.getRealHeight() * thumbnailSize / scaledImage.getRealWidth();
                                            }
                                        }
                                    } else {
                                        ScaledImage thumbnailDimensions = BlogThumbnailHandler.getInstance().getThumbnailDimensions(file.getAbsolutePath());
                                        if (thumbnailDimensions != null) {
                                            thumbWidth = thumbnailDimensions.getRealWidth();
                                            thumbHeight = thumbnailDimensions.getRealHeight();

                                            XmlUtil.setChildText(fileElement, "thumbnail", "true");
                                            
                                            String origImgSrcUrl = req.getContextPath() + "/servlet?command=getFile&fileName=" + UTF8URLEncoder.encode(file.getName()) + "&cached=true";                                            
                                            
                                            XmlUtil.setChildText(fileElement, "origImgPath", origImgSrcUrl);
                                        }
                                    }

                                    XmlUtil.setChildText(fileElement, "thumbnailWidth", Integer.toString(thumbWidth));
                                    XmlUtil.setChildText(fileElement, "thumbnailHeight", Integer.toString(thumbHeight));

                                    int fullScreenWidth = scaledImage.getScaledWidth() + 20;

                                    if (fullScreenWidth < 600) {
                                        fullScreenWidth = 600;
                                    }

                                    XmlUtil.setChildText(fileElement, "fullScreenWidth", Integer.toString(fullScreenWidth));
                                    XmlUtil.setChildText(fileElement, "fullScreenHeight", Integer.toString(scaledImage.getScaledHeight()));

                                    String imgSrcUrl = req.getContextPath() + "/servlet?command=getFile&fileName=" + UTF8URLEncoder.encode(file.getName()) + "&cached=true";
                                    
                                    if (useThumbnail) {
                                        imgSrcUrl += "&thumb=true";
                                    } 

                                    XmlUtil.setChildText(fileElement, "imgPath", imgSrcUrl);

                                    XmlUtil.setChildText(fileElement, "pagePicCounter", Integer.toString(globalEntryCounter));

                                    String srcPathForScript = req.getContextPath() + "/servlet?command=getFile&fileName=" + UTF8URLEncoder.encode(file.getName()) + "&cached=true";

                                    XmlUtil.setChildText(fileElement, "imgPathForScript", srcPathForScript);

                                    ArrayList<String> attachments = metaInfMgr.getListOfAttachments(file.getAbsolutePath());
                                    
                                    if ((attachments != null) && (attachments.size() > 0)) {
                                        String attachmentFileName = attachments.get(0);
                                        if (isGpsTrack(attachmentFileName)) {
                                            XmlUtil.setChildText(fileElement, "geoTrack", attachmentFileName);
                                        } else {
                                            Element attachmentElem = doc.createElement("attachment");
                                            XmlUtil.setElementText(attachmentElem, attachmentFileName);
                                            String attachmentIcon = IconManager.getInstance().getIconForFileName(attachmentFileName);
                                            attachmentElem.setAttribute("icon", attachmentIcon);
                                            fileElement.appendChild(attachmentElem);
                                        }
                                    }
                                    
                                    GeoTag geoTag = metaInfMgr.getGeoTag(file.getAbsolutePath());

                                    if (geoTag != null) {
                                        Element geoTagElement = doc.createElement("geoTag");
                                        fileElement.appendChild(geoTagElement);

                                        XmlUtil.setChildText(geoTagElement, "latitude", Float.toString(geoTag.getLatitude()), false);
                                        XmlUtil.setChildText(geoTagElement, "longitude", Float.toString(geoTag.getLongitude()), false);

                                        String infoText = geoTag.getInfoText();
                                        if (infoText != null) {
                                            XmlUtil.setChildText(geoTagElement, "infoText", infoText, false);
                                        }

                                        int zoomFactor = geoTag.getZoomFactor();
                                        if (zoomFactor <= 0) {
                                            zoomFactor = 10;
                                        }

                                        XmlUtil.setChildText(geoTagElement, "zoomFactor", Integer.toString(zoomFactor), false);
                                    }

                                    if (readonly) {
                                        boolean alreadyRated = false;

                                        Hashtable<String, Boolean> ratedPictures = (Hashtable<String, Boolean>) session.getAttribute("ratedPictures");

                                        if (ratedPictures != null) {
                                            if (ratedPictures.get(file.getAbsolutePath()) != null) {
                                                alreadyRated = true;
                                            }
                                        }

                                        if (!alreadyRated) {
                                            String visitorId = (String) session.getAttribute(VisitorServlet.SESSION_ATTRIB_VISITOR_ID);
                                            if (visitorId != null) {
                                                if (metaInfMgr.getIdentifiedVisitorRating(visitorId, file.getAbsolutePath()) > 0) {
                                                    alreadyRated = true;
                                                }
                                            }
                                        }

                                        if (!alreadyRated) {
                                            XmlUtil.setChildText(fileElement, "ratingAllowed", "true");
                                        }
                                    }

                                    if (stagedPublication && (!readonly) && (metaInfMgr.getStatus(file.getAbsolutePath()) == MetaInfManager.STATUS_BLOG_EDIT)) {
                                        XmlUtil.setChildText(fileElement, "staged", "true");
                                    }
                                }

                                i++;
                            }
                        }

                    } catch (ParseException pex) {
                        Logger.getLogger(getClass()).error("invalid blog date format in " + blogDate, pex);
                    }
                }

                if (dateRangeFrom != null) {
                    if (sortOrder == BlogDateComparator.SORT_ORDER_BLOG) {
                        XmlUtil.setChildText(blogElement, "dateRangeFrom", formatBlogDate(dateRangeFrom));
                    } else {
                        XmlUtil.setChildText(blogElement, "dateRangeUntil", formatBlogDate(dateRangeFrom));
                    }
                }
                if (dateRangeUntil != null) {
                    if (sortOrder == BlogDateComparator.SORT_ORDER_BLOG) {
                        XmlUtil.setChildText(blogElement, "dateRangeUntil", formatBlogDate(dateRangeUntil));
                    } else {
                        XmlUtil.setChildText(blogElement, "dateRangeFrom", formatBlogDate(dateRangeUntil));
                    }
                }
            } else {
                if (pageBeforeDay != null) {
                    XmlUtil.setChildText(blogElement, "dateRangeUntil", formatBlogDate(pageBeforeDay));
                }
            }
        } else {
            XmlUtil.setChildText(blogElement, "empty", "true");
        }

        processResponse("blog/blogList.xsl", req, true);
    }

    private void appendDescrFragments(String description, Element descrElem) {
        StringTokenizer descrParser = new StringTokenizer(description, "{}[]", true);

        boolean emojiStarted = false;
        boolean linkStarted = false;
        String textFragment = null;
        String emojiName = null;
        String linkData = null;

        while (descrParser.hasMoreTokens()) {
            String token = descrParser.nextToken();
            if (token.equals("{")) {
                if (!emojiStarted) {
                    if (textFragment != null) {
                        Element fragmentElem = doc.createElement("fragment");
                        XmlUtil.setElementText(fragmentElem, textFragment);
                        descrElem.appendChild(fragmentElem);
                        textFragment = null;
                    }
                    emojiStarted = true;
                }
            } else if (token.equals("}")) {
                if (emojiStarted) {
                    if (emojiName != null) {
                        Element emojiElem = doc.createElement("emoji");
                        XmlUtil.setElementText(emojiElem, emojiName);
                        descrElem.appendChild(emojiElem);
                        emojiName = null;
                    }
                    emojiStarted = false;
                }
            } else if (token.equals("[")) {
                if (!linkStarted) {
                    if (textFragment != null) {
                        Element fragmentElem = doc.createElement("fragment");
                        XmlUtil.setElementText(fragmentElem, textFragment);
                        descrElem.appendChild(fragmentElem);
                        textFragment = null;
                    }
                    linkStarted = true;
                }
            } else if (token.equals("]")) {
                if (linkStarted) {
                    if (linkData != null) {
                        StringTokenizer linkParser = new StringTokenizer(linkData, "\",");
                        if (linkParser.hasMoreTokens()) {
                            String linkLabel = linkParser.nextToken();
                            if (linkParser.hasMoreTokens()) {
                                String linkUrl = linkParser.nextToken();
                                Element linkElem = doc.createElement("link");
                                XmlUtil.setChildText(linkElem, "label", linkLabel);
                                XmlUtil.setChildText(linkElem, "url", linkUrl);
                                descrElem.appendChild(linkElem);
                            }
                        }
                        linkData = null;
                    }
                    linkStarted = false;
                }
            } else {
                if (emojiStarted) {
                    emojiName = token;
                } else if (linkStarted) {
                    linkData = token;
                } else {
                    textFragment = token;
                }
            }
        }
        if (textFragment != null) {
            Element fragmentElem = doc.createElement("fragment");
            XmlUtil.setElementText(fragmentElem, textFragment);
            descrElem.appendChild(fragmentElem);
        }
    }

    private boolean isGpsTrack(String attachmentFileName) {
        return attachmentFileName.endsWith(".GPX") || attachmentFileName.endsWith(".gpx");
    }

}
