package de.webfilesys;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import de.webfilesys.util.CommonUtils;
import de.webfilesys.util.XmlUtil;

/**
 * Voting by anonymous visitors is obsolet now. The code for anonymous visitors
 * should be removed in a future release. TODO: synchronize more fine granular
 * 
 * @author fho
 */
public class MetaInfManager extends Thread {
    public static final String METAINF_FILE = "_metainf.fmweb";

    public static final int STATUS_NONE = 0;
    public static final int STATUS_BLOG_EDIT = 1;
    public static final int STATUS_BLOG_PUBLISHED = 2;

    private static MetaInfManager metaInfMgr = null;

    private Hashtable<String, Element> dirList = null;

    private Hashtable<String, Boolean> cacheDirty = null;
    
    DocumentBuilder builder;

    private MetaInfManager() {
        builder = null;

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException pcex) {
            Logger.getLogger(getClass()).error(pcex);
        }

        dirList = new Hashtable<String, Element>();

        cacheDirty = new Hashtable<String, Boolean>();

        this.start();
    }

    public static synchronized MetaInfManager getInstance() {
        if (metaInfMgr == null) {
            metaInfMgr = new MetaInfManager();
        }

        return (metaInfMgr);
    }

    public void saveMetaInfFile(String path) {
        Element metaInfRoot = (Element) dirList.get(path);

        if (metaInfRoot == null) {
            return;
        }

        synchronized (this) {
            String metaInfFileName = null;

            if (path.endsWith(File.separator)) {
                metaInfFileName = path + METAINF_FILE;
            } else {
                metaInfFileName = path + File.separator + METAINF_FILE;
            }

            NodeList metaInfList = metaInfRoot.getElementsByTagName("metainf");

            if ((metaInfList == null) || (metaInfList.getLength() == 0)) {
                File metaInfFile = new File(metaInfFileName);

                if (metaInfFile.exists() && metaInfFile.canWrite()) {
                    if (Logger.getLogger(getClass()).isDebugEnabled()) {
                        Logger.getLogger(getClass()).debug("removing empty meta inf file " + metaInfFileName);
                    }

                    metaInfFile.delete();
                }

                return;
            }

            if (Logger.getLogger(getClass()).isDebugEnabled()) {
                Logger.getLogger(getClass()).debug("saving meta info to file: " + metaInfFileName);
            }

            OutputStreamWriter xmlOutFile = null;

            try {
                FileOutputStream fos = new FileOutputStream(metaInfFileName);

                xmlOutFile = new OutputStreamWriter(fos, "UTF-8");

                XmlUtil.writeToStream(metaInfRoot, xmlOutFile);

                xmlOutFile.flush();
            } catch (IOException ioex) {
                Logger.getLogger(getClass()).error("error saving metainf file : " + metaInfFileName, ioex);
            } finally {
                if (xmlOutFile != null) {
                    try {
                        xmlOutFile.close();
                    } catch (Exception ex) {
                    }
                }
            }
        }
    }

    public Element loadMetaInfFile(String path) {
        String metaInfFileName = null;

        if (path.endsWith(File.separator)) {
            metaInfFileName = path + METAINF_FILE;
        } else {
            metaInfFileName = path + File.separator + METAINF_FILE;
        }

        File metaInfFile = new File(metaInfFileName);

        if ((!metaInfFile.exists()) || (!metaInfFile.canRead())) {
            return (null);
        }

        synchronized (this) {
            Document doc = null;

            FileInputStream fis = null;

            try {
                fis = new FileInputStream(metaInfFile);

                InputSource inputSource = new InputSource(fis);

                inputSource.setEncoding("UTF-8");

                if (Logger.getLogger(getClass()).isDebugEnabled()) {
                    Logger.getLogger(getClass()).debug("reading meta info from " + metaInfFileName);
                }

                doc = builder.parse(inputSource);
            } catch (SAXException saxex) {
                Logger.getLogger(getClass()).error("Failed to load metainf file : " + metaInfFileName, saxex);
            } catch (IOException ioex) {
                Logger.getLogger(getClass()).error("Failed to load metainf file : " + metaInfFileName, ioex);
            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (Exception ex) {
                    }
                }
            }

            if (doc == null) {
                return (null);
            }

            return doc.getDocumentElement();
        }
    }

    public boolean dirHasMetaInf(String path) {
        Element metaInfRoot = (Element) dirList.get(path);

        if (metaInfRoot == null) {
            metaInfRoot = loadMetaInfFile(path);

            if (metaInfRoot != null) {
                dirList.put(path, metaInfRoot);
            }
        }

        return (metaInfRoot != null);
    }

    public Element getMetaInfElement(String absoluteFileName) {
        String[] partsOfPath = CommonUtils.splitPath(absoluteFileName);
        return (getMetaInfElement(partsOfPath[0], partsOfPath[1]));
    }

    public synchronized Element getMetaInfElement(String path, String fileName) {
        Element metaInfRoot = null;

        metaInfRoot = (Element) dirList.get(path);

        if (metaInfRoot == null) {
            metaInfRoot = loadMetaInfFile(path);

            if (metaInfRoot == null) {
                return (null);
            } else {
                dirList.put(path, metaInfRoot);
            }
        }

        NodeList metaInfList = metaInfRoot.getElementsByTagName("metainf");

        if (metaInfList == null) {
            return null;
        }

        int listLength = metaInfList.getLength();

        for (int i = 0; i < listLength; i++) {
            Element metaInfElement = (Element) metaInfList.item(i);

            String key = metaInfElement.getAttribute("filename");

            if ((key != null) && key.equals(fileName)) {
                return (metaInfElement);
            }
        }

        return (null);
    }

    public Element createMetaInfElement(String absoluteFileName) {
        String[] partsOfPath = CommonUtils.splitPath(absoluteFileName);
        return (createMetaInfElement(partsOfPath[0], partsOfPath[1]));
    }

    protected Element createMetaInfElement(String path, String fileName) {
        synchronized (this) {
            Element metaInfRoot = (Element) dirList.get(path);

            if (metaInfRoot == null) {
                metaInfRoot = loadMetaInfFile(path);
            }

            if (metaInfRoot == null) {
                Document doc = builder.newDocument();

                metaInfRoot = doc.createElement("metainfroot");

                dirList.put(path, metaInfRoot);
            }

            Element metaInfElement = metaInfRoot.getOwnerDocument().createElement("metainf");

            metaInfElement.setAttribute("filename", fileName);

            metaInfRoot.appendChild(metaInfElement);

            cacheDirty.put(path, new Boolean(true));

            return (metaInfElement);
        }
    }

    public String getDescription(String absoluteFileName) {
        Element metaInfElement = getMetaInfElement(absoluteFileName);

        if (metaInfElement == null) {
            return (null);
        }

        return (XmlUtil.getChildText(metaInfElement, "description"));
    }

    public String getDescription(String path, String fileName) {
        Element metaInfElement = getMetaInfElement(path, fileName);

        if (metaInfElement == null) {
            return (null);
        }

        return (XmlUtil.getChildText(metaInfElement, "description"));
    }

    public String getShortDescription(String absoluteFileName, int maxLength) {
        Element metaInfElement = getMetaInfElement(absoluteFileName);

        if (metaInfElement == null) {
            return (null);
        }

        String description = XmlUtil.getChildText(metaInfElement, "description");

        if ((description.length() > maxLength) && (maxLength > 3)) {
            description = description.substring(0, maxLength - 3) + "...";
        }

        return (description);
    }

    public String getShortDescription(String path, String fileName, int maxLength) {
        Element metaInfElement = getMetaInfElement(path, fileName);

        if (metaInfElement == null) {
            return (null);
        }

        String description = XmlUtil.getChildText(metaInfElement, "description");

        if ((description.length() > maxLength) && (maxLength > 3)) {
            description = description.substring(0, maxLength - 3) + "...";
        }

        return (description);
    }

    public void setDescription(String path, String newDescription) {
        String[] partsOfPath = CommonUtils.splitPath(path);
        setDescription(partsOfPath[0], partsOfPath[1], newDescription);
    }

    public void setDescription(String path, String fileName, String newDescription) {
        synchronized (this) {
            Element metaInfElement = getMetaInfElement(path, fileName);

            if (metaInfElement == null) {
                metaInfElement = createMetaInfElement(path, fileName);
            }

            XmlUtil.setChildText(metaInfElement, "description", newDescription, true);

            cacheDirty.put(path, new Boolean(true));
            // saveMetaInfFile(path);
        }
    }

    public void setTitlePic(String path, String titlePicFileName) {
        synchronized (this) {
            Element metaInfElement = getMetaInfElement(path, ".");

            if (metaInfElement == null) {
                metaInfElement = createMetaInfElement(path, ".");
            }

            XmlUtil.setChildText(metaInfElement, "titlePic", titlePicFileName);

            cacheDirty.put(path, new Boolean(true));
        }
    }

    public String getTitlePic(String path) {
        Element metaInfElement = getMetaInfElement(path, ".");

        if (metaInfElement == null) {
            return null;
        }

        return XmlUtil.getChildText(metaInfElement, "titlePic");
    }

    public void unsetTitlePic(String path) {
        synchronized (this) {
            Element metaInfElement = getMetaInfElement(path, ".");

            if (metaInfElement == null) {
                return;
            }

            Element titlePicElem = XmlUtil.getChildByTagName(metaInfElement, "titlePic");
            if (titlePicElem != null) {
                metaInfElement.removeChild(titlePicElem);
            }

            cacheDirty.put(path, new Boolean(true));
        }
    }

    public boolean moveMetaInf(String currentPath, String srcFileName, String destFileName) {
        synchronized (this) {
            Element destMetaInfElement = getMetaInfElement(currentPath, destFileName);

            if (destMetaInfElement != null) {
                removeMetaInf(currentPath, destFileName);
            }

            boolean moved = false;

            Element srcMetaInfElement = getMetaInfElement(currentPath, srcFileName);

            if (srcMetaInfElement != null) {
                srcMetaInfElement.setAttribute("filename", destFileName);
                moved = true;
            }

            cacheDirty.put(currentPath, new Boolean(true));

            return moved;
        }
    }

    public void removeMetaInf(String absoluteFileName) {
        int separatorIdx = absoluteFileName.lastIndexOf(File.separator);

        if (separatorIdx < 0) {
            separatorIdx = absoluteFileName.lastIndexOf('/');
        }

        if (separatorIdx < 0) {
            // removeDescription(path,absoluteFileName);
            return;
        }

        removeMetaInf(absoluteFileName.substring(0, separatorIdx), absoluteFileName.substring(separatorIdx + 1));
    }

    public void removeMetaInf(String path, String fileName) {
        synchronized (this) {
            Element metaInfElement = getMetaInfElement(path, fileName);

            if (metaInfElement != null) {
                Element metaInfRoot = (Element) dirList.get(path);

                metaInfRoot.removeChild(metaInfElement);

                cacheDirty.put(path, new Boolean(true));
            }
        }
    }

    public void removeDescription(String absoluteFileName) {
        synchronized (this) {
            Element metaInfElement = getMetaInfElement(absoluteFileName);

            if (metaInfElement != null) {
                Element descElement = XmlUtil.getChildByTagName(metaInfElement, "description");

                if (descElement != null) {
                    metaInfElement.removeChild(descElement);
                }
            }
        }
    }

    public void removePath(String path) {
        synchronized (this) {
            dirList.remove(path);
            cacheDirty.remove(path);
        }
    }
    
    public void addAttachment(String absoluteFileName, String attachmentName) {
        String[] partsOfPath = CommonUtils.splitPath(absoluteFileName);
        addAttachment(partsOfPath[0], partsOfPath[1], attachmentName);
    }

    public void addAttachment(String path, String fileName, String attachmentName) {
        synchronized(this) {
            Element metaInfElement = getMetaInfElement(path,fileName);
            
            if (metaInfElement == null) {
                metaInfElement = createMetaInfElement(path,fileName);
            }

            Document doc = metaInfElement.getOwnerDocument();

            Element attachmentListElement = XmlUtil.getChildByTagName(metaInfElement, "comments");

            if (attachmentListElement == null) {
                attachmentListElement = doc.createElement("attachments");

                metaInfElement.appendChild(attachmentListElement);
            }

            Element attachmentElement = doc.createElement("attachment");

            attachmentListElement.appendChild(attachmentElement);

            XmlUtil.setElementText(attachmentElement, attachmentName, false);
            
            cacheDirty.put(path, Boolean.TRUE);
        }
    }

    public void removeAttachments(String absoluteFileName) {
        String[] partsOfPath = CommonUtils.splitPath(absoluteFileName);
        removeComments(partsOfPath[0], partsOfPath[1]);
    }

    public void removeAttachments(String path, String fileName) {
        synchronized(this) {
            Element metaInfElement = getMetaInfElement(path,fileName);
            
            if (metaInfElement == null) {
                return;
            }
        
            Element attachmentListElement = XmlUtil.getChildByTagName(metaInfElement, "attachments");

            if (attachmentListElement == null) {
                return;
            }

            metaInfElement.removeChild(attachmentListElement);

            cacheDirty.put(path, Boolean.TRUE);
        }
    }
    
    public ArrayList<String> getListOfAttachments(String absoluteFileName) {
        String[] partsOfPath = CommonUtils.splitPath(absoluteFileName);
        return(getListOfAttachments(partsOfPath[0], partsOfPath[1]));
    }

    public ArrayList<String> getListOfAttachments(String path, String fileName) {
        Element metaInfElement = getMetaInfElement(path,fileName);

        if (metaInfElement == null) {
            return null;
        }

        Element attachmentListElement = XmlUtil.getChildByTagName(metaInfElement, "attachments");

        if (attachmentListElement == null) {
            return null;
        }
        
        NodeList attachmentList = attachmentListElement.getElementsByTagName("attachment");

        if (attachmentList == null) {
            return null;
        }

        int listLength = attachmentList.getLength();

        if (listLength == 0) {
            return null;
        }

        ArrayList<String> listOfAttachments = new ArrayList<String>();

        for (int i = 0; i < listLength; i++)
        {
            Element attachmentElement = (Element) attachmentList.item(i);

            String attachmentName = XmlUtil.getElementText(attachmentElement);

            listOfAttachments.add(attachmentName);
        }

        return(listOfAttachments);
    }
    
    public void addComment(String absoluteFileName, Comment newComment) {
        String[] partsOfPath = CommonUtils.splitPath(absoluteFileName);
        addComment(partsOfPath[0], partsOfPath[1], newComment);
    }

    public void addComment(String path, String fileName, Comment newComment) {
        synchronized (this) {
            Element metaInfElement = getMetaInfElement(path, fileName);

            if (metaInfElement == null) {
                metaInfElement = createMetaInfElement(path, fileName);
            }

            Document doc = metaInfElement.getOwnerDocument();

            Element commentListElement = XmlUtil.getChildByTagName(metaInfElement, "comments");

            if (commentListElement == null) {
                commentListElement = doc.createElement("comments");

                metaInfElement.appendChild(commentListElement);
            }

            Element commentElement = doc.createElement("comment");

            commentListElement.appendChild(commentElement);

            XmlUtil.setChildText(commentElement, "user", newComment.getUser(), false);
            XmlUtil.setChildText(commentElement, "time", Long.toString(newComment.getCreationTime()), false);
            XmlUtil.setChildText(commentElement, "message", newComment.getMessage(), true);

            cacheDirty.put(path, new Boolean(true));
        }
    }

    public void removeComments(String absoluteFileName) {
        String[] partsOfPath = CommonUtils.splitPath(absoluteFileName);
        removeComments(partsOfPath[0], partsOfPath[1]);
    }

    public void removeComments(String path, String fileName) {
        synchronized (this) {
            Element metaInfElement = getMetaInfElement(path, fileName);

            if (metaInfElement == null) {
                return;
            }

            Element commentListElement = XmlUtil.getChildByTagName(metaInfElement, "comments");

            if (commentListElement == null) {
                return;
            }

            metaInfElement.removeChild(commentListElement);

            cacheDirty.put(path, new Boolean(true));
        }
    }

    public void setCommentsSeenByOwner(String absoluteFileName, boolean newVal) {
        String[] partsOfPath = CommonUtils.splitPath(absoluteFileName);
        setCommentsSeenByOwner(partsOfPath[0], partsOfPath[1], newVal);
    }

    public void setCommentsSeenByOwner(String path, String fileName, boolean newVal) {
        synchronized (this) {
            Element metaInfElement = getMetaInfElement(path, fileName);

            if (metaInfElement == null) {
                return;
            }

            Element commentListElement = XmlUtil.getChildByTagName(metaInfElement, "comments");

            if (commentListElement == null) {
                return;
            }

            commentListElement.setAttribute("seenByOwner", Boolean.toString(newVal));

            cacheDirty.put(path, new Boolean(true));
        }
    }

    public void setUnnotifiedComments(String path, boolean newVal) {
        synchronized (this) {
            Element metaInfElement = getMetaInfElement(path, ".");

            if (metaInfElement == null) {
                metaInfElement = createMetaInfElement(path, ".");
            }
            
            Element newCommentsElem = XmlUtil.getChildByTagName(metaInfElement, "newComments");
            if (newVal) {
                if (newCommentsElem == null) {
                    XmlUtil.setChildText(metaInfElement, "newComments", "true");
                }
            } else {
                if (newCommentsElem != null) {
                    metaInfElement.removeChild(newCommentsElem);
                }
            }
            cacheDirty.put(path, new Boolean(true));
        }
    }
    
    public boolean hasUnnotifiedComments(String path) {
        Element metaInfElement = getMetaInfElement(path, ".");

        if (metaInfElement == null) {
            return false;
        }

        Element newCommentsElem = XmlUtil.getChildByTagName(metaInfElement, "newComments");
        
        return (newCommentsElem != null);
    }
    
    public boolean isCommentsSeenByOwner(String absoluteFileName) {
        String[] partsOfPath = CommonUtils.splitPath(absoluteFileName);
        return isCommentsSeenByOwner(partsOfPath[0], partsOfPath[1]);
    }

    public boolean isCommentsSeenByOwner(String path, String fileName) {
        Element metaInfElement = getMetaInfElement(path, fileName);

        if (metaInfElement == null) {
            return false;
        }

        Element commentListElement = XmlUtil.getChildByTagName(metaInfElement, "comments");

        if (commentListElement == null) {
            return false;
        }

        String seenByOwner = commentListElement.getAttribute("seenByOwner");
        if (seenByOwner == null) {
            return false;
        }
        return (seenByOwner.equals("true"));
    }

    public Vector<Comment> getListOfComments(String absoluteFileName) {
        String[] partsOfPath = CommonUtils.splitPath(absoluteFileName);
        return (getListOfComments(partsOfPath[0], partsOfPath[1]));
    }

    public Vector<Comment> getListOfComments(String path, String fileName) {
        Element metaInfElement = getMetaInfElement(path, fileName);

        if (metaInfElement == null) {
            return (null);
        }

        Element commentListElement = XmlUtil.getChildByTagName(metaInfElement, "comments");

        if (commentListElement == null) {
            return (null);
        }

        NodeList commentList = commentListElement.getElementsByTagName("comment");

        if (commentList == null) {
            return (null);
        }

        int listLength = commentList.getLength();

        if (listLength == 0) {
            return (null);
        }

        Vector listOfComments = new Vector();

        for (int i = 0; i < listLength; i++) {
            Element commentElement = (Element) commentList.item(i);

            String user = XmlUtil.getChildText(commentElement, "user");
            String message = XmlUtil.getChildText(commentElement, "message");

            String tmp = XmlUtil.getChildText(commentElement, "time");

            long creationTime = 0L;

            try {
                creationTime = Long.parseLong(tmp);
            } catch (NumberFormatException nfex) {
                Logger.getLogger(getClass()).warn("invalid creation time: " + tmp);
            }

            Comment comment = new Comment(user, new Date(creationTime), message);

            listOfComments.add(comment);
        }

        return (listOfComments);
    }

    public int countComments(String absoluteFileName) {
        String[] partsOfPath = CommonUtils.splitPath(absoluteFileName);
        return (countComments(partsOfPath[0], partsOfPath[1]));
    }

    public int countComments(String path, String fileName) {
        Element metaInfElement = getMetaInfElement(path, fileName);

        if (metaInfElement == null) {
            return (0);
        }

        Element commentListElement = XmlUtil.getChildByTagName(metaInfElement, "comments");

        if (commentListElement == null) {
            return (0);
        }

        NodeList commentList = commentListElement.getElementsByTagName("comment");

        if (commentList == null) {
            return (0);
        }

        return (commentList.getLength());
    }

    public void setOwnerRating(String path, int rating) {
        String[] partsOfPath = CommonUtils.splitPath(path);
        setOwnerRating(partsOfPath[0], partsOfPath[1], rating);
    }

    public void setOwnerRating(String path, String fileName, int rating) {
        synchronized (this) {
            Element metaInfElement = getMetaInfElement(path, fileName);

            if (metaInfElement == null) {
                metaInfElement = createMetaInfElement(path, fileName);
            }

            Document doc = metaInfElement.getOwnerDocument();

            Element ratingElement = XmlUtil.getChildByTagName(metaInfElement, "rating");

            if (ratingElement == null) {
                ratingElement = doc.createElement("rating");

                metaInfElement.appendChild(ratingElement);
            }

            XmlUtil.setChildText(ratingElement, "owner", Integer.toString(rating));

            cacheDirty.put(path, new Boolean(true));
        }
    }

    public int getOwnerRating(String path) {
        Element metaInfElement = getMetaInfElement(path);

        if (metaInfElement == null) {
            return (-1);
        }

        Element ratingElement = XmlUtil.getChildByTagName(metaInfElement, "rating");

        if (ratingElement == null) {
            return (-1);
        }

        String ownerRating = XmlUtil.getChildText(ratingElement, "owner");

        if (ownerRating == null) {
            return (-1);
        }

        int rating = (-1);

        try {
            rating = Integer.parseInt(ownerRating);
        } catch (NumberFormatException nfe) {
        }

        return (rating);
    }

    public void addIdentifiedVisitorRating(String visitorId, String path, int rating) {
        String[] partsOfPath = CommonUtils.splitPath(path);
        addIdentifiedVisitorRating(visitorId, partsOfPath[0], partsOfPath[1], rating);
    }

    public void addIdentifiedVisitorRating(String visitorId, String path, String fileName, int rating) {
        synchronized (this) {
            Element metaInfElement = getMetaInfElement(path, fileName);

            if (metaInfElement == null) {
                metaInfElement = createMetaInfElement(path, fileName);
            }

            Document doc = metaInfElement.getOwnerDocument();

            Element ratingElement = XmlUtil.getChildByTagName(metaInfElement, "rating");

            if (ratingElement == null) {
                ratingElement = doc.createElement("rating");
                metaInfElement.appendChild(ratingElement);
            }

            Element visitorElem = getIdentifiedVisitorElem(ratingElement, visitorId);

            if (visitorElem == null) {
                visitorElem = doc.createElement("guest");
                visitorElem.setAttribute("id", visitorId);
                ratingElement.appendChild(visitorElem);
            }

            XmlUtil.setElementText(visitorElem, Integer.toString(rating));

            cacheDirty.put(path, Boolean.TRUE);
        }
    }

    public int getIdentifiedVisitorRating(String visitorId, String path) {
        Element metaInfElement = getMetaInfElement(path);

        if (metaInfElement == null) {
            return (-1);
        }

        Element ratingElement = XmlUtil.getChildByTagName(metaInfElement, "rating");

        if (ratingElement == null) {
            return (-1);
        }

        Element identifiedVisitor = getIdentifiedVisitorElem(ratingElement, visitorId);

        if (identifiedVisitor == null) {
            return (-1);
        }

        String voteVal = XmlUtil.getElementText(identifiedVisitor);

        try {
            return Integer.parseInt(voteVal);
        } catch (Exception ex) {
            Logger.getLogger(getClass()).error("failed to get identified visitor rating for visitor " + visitorId, ex);
        }

        return (-1);
    }

    private Element getIdentifiedVisitorElem(Element ratingElement, String visitorId) {
        if ((ratingElement == null) || CommonUtils.isEmpty(visitorId)) {
            return null;
        }

        NodeList identifiedVisitors = ratingElement.getElementsByTagName("guest");

        if (identifiedVisitors != null) {
            int listLength = identifiedVisitors.getLength();
            for (int i = 0; i < listLength; i++) {
                Element identifiedVisitor = (Element) identifiedVisitors.item(i);
                String identifiedVisitorId = identifiedVisitor.getAttribute("id");
                if ((identifiedVisitorId != null) && identifiedVisitorId.equals(visitorId)) {
                    return identifiedVisitor;
                }
            }
        }

        return null;
    }

    public void addVisitorRating(String path, int rating) {
        String partsOfPath[] = CommonUtils.splitPath(path);
        addVisitorRating(partsOfPath[0], partsOfPath[1], rating);
    }

    public void addVisitorRating(String path, String fileName, int rating) {
        synchronized (this) {
            Element metaInfElement = getMetaInfElement(path, fileName);

            if (metaInfElement == null) {
                metaInfElement = createMetaInfElement(path, fileName);
            }

            Document doc = metaInfElement.getOwnerDocument();

            Element ratingElement = XmlUtil.getChildByTagName(metaInfElement, "rating");

            if (ratingElement == null) {
                ratingElement = doc.createElement("rating");

                metaInfElement.appendChild(ratingElement);
            }

            Element visitorElement = XmlUtil.getChildByTagName(metaInfElement, "visitor");

            if (visitorElement == null) {
                visitorElement = doc.createElement("visitor");

                ratingElement.appendChild(visitorElement);
            }

            Element voteElement = doc.createElement("vote");

            visitorElement.appendChild(voteElement);

            XmlUtil.setElementText(voteElement, Integer.toString(rating));

            cacheDirty.put(path, new Boolean(true));
        }
    }

    public PictureRating getPictureRating(String path) {
        String[] partsOfPath = CommonUtils.splitPath(path);
        return (getPictureRating(partsOfPath[0], partsOfPath[1]));
    }

    public PictureRating getPictureRating(String path, String fileName) {
        Element metaInfElement = getMetaInfElement(path, fileName);

        if (metaInfElement == null) {
            return null;
        }

        Element ratingElement = XmlUtil.getChildByTagName(metaInfElement, "rating");

        if (ratingElement == null) {
            return null;
        }

        PictureRating pictureRating = null;

        String ownerRating = XmlUtil.getChildText(ratingElement, "owner");

        if (!CommonUtils.isEmpty(ownerRating)) {
            try {
                int rating = Integer.parseInt(ownerRating);
                pictureRating = new PictureRating();
                pictureRating.setOwnerRating(rating);
            } catch (NumberFormatException nfe) {
                Logger.getLogger(getClass()).error("invalid owner rating value: " + ownerRating, nfe);
            }
        }

        int voteSum = 0;

        int voteCount = 0;

        // anonymous visitors
        Element visitorElement = XmlUtil.getChildByTagName(metaInfElement, "visitor");

        if (visitorElement != null) {
            NodeList voteList = visitorElement.getElementsByTagName("vote");

            if (voteList != null) {
                int listLength = voteList.getLength();

                for (int i = 0; i < listLength; i++) {
                    Element voteElement = (Element) voteList.item(i);

                    String vote = XmlUtil.getElementText(voteElement);

                    int voteVal = (-1);

                    try {
                        voteVal = Integer.parseInt(vote);
                        voteSum += voteVal;
                        voteCount++;
                    } catch (NumberFormatException nfe) {
                        Logger.getLogger(getClass()).error("invalid vote value: " + vote);
                    }
                }
            }
        }

        // identified visitors (by permanent cookie)
        NodeList identifiedVisitors = ratingElement.getElementsByTagName("guest");

        if (identifiedVisitors != null) {
            int listLength = identifiedVisitors.getLength();
            for (int i = 0; i < listLength; i++) {
                Element identifiedVisitor = (Element) identifiedVisitors.item(i);

                String vote = XmlUtil.getElementText(identifiedVisitor);
                try {
                    int voteVal = Integer.parseInt(vote);
                    voteSum += voteVal;
                    voteCount++;
                } catch (NumberFormatException nfe) {
                    Logger.getLogger(getClass()).error("invalid vote value: " + vote);
                }
            }
        }

        if (voteCount > 0) {
            if (pictureRating == null) {
                pictureRating = new PictureRating();
            }

            pictureRating.setNumberOfVotes(voteCount);

            float averageVote = ((float) voteSum) / ((float) voteCount);

            pictureRating.setAverageVoteVal(averageVote);

            pictureRating.setAverageVisitorRating(Math.round(averageVote));

            pictureRating.setVisitorRatingSum(voteSum);
        }

        return pictureRating;
    }

    public int getVisitorRatingCount(String path) {
        Element metaInfElement = getMetaInfElement(path);

        if (metaInfElement == null) {
            return (0);
        }

        Element ratingElement = XmlUtil.getChildByTagName(metaInfElement, "rating");

        if (ratingElement == null) {
            return (0);
        }

        int ratingCount = 0;

        // anonymous visitors
        Element visitorElement = XmlUtil.getChildByTagName(metaInfElement, "visitor");

        if (visitorElement != null) {
            NodeList voteList = visitorElement.getElementsByTagName("vote");

            if (voteList != null) {
                ratingCount += voteList.getLength();
            }
        }

        // identified visitors (by permanent cookie)
        NodeList identifiedVisitors = ratingElement.getElementsByTagName("guest");

        if (identifiedVisitors != null) {
            ratingCount += identifiedVisitors.getLength();
        }

        return ratingCount;
    }

    public int getVisitorRatingStarSum(String path) {
        Element metaInfElement = getMetaInfElement(path);

        if (metaInfElement == null) {
            return (0);
        }

        Element ratingElement = XmlUtil.getChildByTagName(metaInfElement, "rating");

        if (ratingElement == null) {
            return (0);
        }

        int starSum = 0;

        // anonymous visitors
        Element visitorElement = XmlUtil.getChildByTagName(metaInfElement, "visitor");

        if (visitorElement != null) {
            NodeList voteList = visitorElement.getElementsByTagName("vote");

            if (voteList != null) {
                int listLength = voteList.getLength();
                for (int i = 0; i < listLength; i++) {
                    Element voteElem = (Element) voteList.item(i);
                    String vote = XmlUtil.getElementText(voteElem);

                    try {
                        starSum += Integer.parseInt(vote);
                    } catch (Exception ex) {
                        Logger.getLogger(getClass()).error("invalid vote value: " + vote);
                    }
                }
            }
        }

        // identified visitors
        NodeList identifiedVisitors = ratingElement.getElementsByTagName("guest");

        if (identifiedVisitors != null) {
            int listLength = identifiedVisitors.getLength();
            for (int i = 0; i < listLength; i++) {
                Element identifiedVisitor = (Element) identifiedVisitors.item(i);
                String vote = XmlUtil.getElementText(identifiedVisitor);

                try {
                    starSum += Integer.parseInt(vote);
                } catch (Exception ex) {
                    Logger.getLogger(getClass()).error("invalid vote value: " + vote);
                }
            }
        }

        return starSum;
    }

    public void removeCategoryByName(String absoluteFileName, String categoryName) {
        String[] partsOfPath = CommonUtils.splitPath(absoluteFileName);
        removeCategoryByName(partsOfPath[0], partsOfPath[1], categoryName);
    }

    public boolean removeCategoryByName(String path, String fileName, String categoryName) {
        synchronized (this) {
            Element metaInfElement = getMetaInfElement(path, fileName);

            if (metaInfElement == null) {
                return (false);
            }

            Element catListElement = XmlUtil.getChildByTagName(metaInfElement, "categories");

            if (catListElement == null) {
                return (false);
            }

            NodeList categoryList = catListElement.getElementsByTagName("category");

            if (categoryList == null) {
                return (false);
            }

            int listLength = categoryList.getLength();

            if (listLength == 0) {
                return (false);
            }

            Element categoryToRemove = null;

            for (int i = 0; (categoryToRemove == null) && (i < listLength); i++) {
                Element categoryElement = (Element) categoryList.item(i);

                String catName = XmlUtil.getChildText(categoryElement, "name");

                if ((catName != null) && catName.equals(categoryName)) {
                    categoryToRemove = categoryElement;
                }
            }

            if (categoryToRemove != null) {
                catListElement.removeChild(categoryToRemove);

                cacheDirty.put(path, new Boolean(true));

                return (true);
            }

            return (false);
        }

    }

    public void removeCategories(String absoluteFileName) {
        String[] partsOfPath = CommonUtils.splitPath(absoluteFileName);
        removeCategories(partsOfPath[0], partsOfPath[1]);
    }

    public void removeCategories(String path, String fileName) {
        synchronized (this) {
            Element metaInfElement = getMetaInfElement(path, fileName);

            if (metaInfElement == null) {
                return;
            }

            Element catListElement = XmlUtil.getChildByTagName(metaInfElement, "categories");

            if (catListElement == null) {
                return;
            }

            metaInfElement.removeChild(catListElement);

            cacheDirty.put(path, new Boolean(true));
        }
    }

    public void setGeoTag(String absoluteFileName, GeoTag geoTag) {
        String[] partsOfPath = CommonUtils.splitPath(absoluteFileName);
        setGeoTag(partsOfPath[0], partsOfPath[1], geoTag);
    }

    public void setGeoTag(String path, String fileName, GeoTag newGeoTag) {
        synchronized (this) {
            Element metaInfElement = getMetaInfElement(path, fileName);

            if (metaInfElement == null) {
                metaInfElement = createMetaInfElement(path, fileName);
            }

            Document doc = metaInfElement.getOwnerDocument();

            Element geoTagElement = XmlUtil.getChildByTagName(metaInfElement, "geoTag");

            if (geoTagElement == null) {
                geoTagElement = doc.createElement("geoTag");

                metaInfElement.appendChild(geoTagElement);
            }

            XmlUtil.setChildText(geoTagElement, "latitude", Float.toString(newGeoTag.getLatitude()), false);
            XmlUtil.setChildText(geoTagElement, "longitude", Float.toString(newGeoTag.getLongitude()), false);
            XmlUtil.setChildText(geoTagElement, "zoomFactor", Integer.toString(newGeoTag.getZoomFactor()), false);

            if (newGeoTag.getInfoText() != null) {
                XmlUtil.setChildText(geoTagElement, "infoText", newGeoTag.getInfoText(), true);
            }

            cacheDirty.put(path, new Boolean(true));
        }
    }

    public void removeGeoTag(String absoluteFileName) {
        String[] partsOfPath = CommonUtils.splitPath(absoluteFileName);
        removeGeoTag(partsOfPath[0], partsOfPath[1]);
    }

    public void removeGeoTag(String path, String fileName) {
        synchronized (this) {
            Element metaInfElement = getMetaInfElement(path, fileName);

            if (metaInfElement == null) {
                return;
            }

            Element geoTagElement = XmlUtil.getChildByTagName(metaInfElement, "geoTag");

            if (geoTagElement == null) {
                return;
            }

            metaInfElement.removeChild(geoTagElement);

            cacheDirty.put(path, new Boolean(true));
        }
    }

    public GeoTag getGeoTag(String absoluteFileName) {
        String[] partsOfPath = CommonUtils.splitPath(absoluteFileName);
        return (getGeoTag(partsOfPath[0], partsOfPath[1]));
    }

    public GeoTag getGeoTag(String path, String fileName) {
        Element metaInfElement = getMetaInfElement(path, fileName);

        if (metaInfElement == null) {
            return (null);
        }

        Element geoTagElement = XmlUtil.getChildByTagName(metaInfElement, "geoTag");

        if (geoTagElement == null) {
            return (null);
        }

        String latitude = XmlUtil.getChildText(geoTagElement, "latitude");
        String longitude = XmlUtil.getChildText(geoTagElement, "longitude");
        String zoomFactor = XmlUtil.getChildText(geoTagElement, "zoomFactor");

        GeoTag geoTag = new GeoTag(latitude, longitude, zoomFactor);

        String infoText = XmlUtil.getChildText(geoTagElement, "infoText");

        if (infoText != null) {
            geoTag.setInfotext(infoText);
        }

        return (geoTag);
    }

    /**
     * Removes a backward link from the link target file to the linking file.
     * 
     * @param sourcePath
     *            path of the directory containing the link
     * @param linkName
     *            name of the link
     * @param linkTargetPath
     *            path of the link target file
     */
    public void removeReverseLink(String sourcePath, String linkName, String linkTargetPath) {
        synchronized (this) {
            String linkPath = null;
            if (sourcePath.endsWith(File.separator)) {
                linkPath = sourcePath + linkName;
            } else {
                linkPath = sourcePath + File.separator + linkName;
            }

            Element metaInfElement = getMetaInfElement(linkTargetPath);

            if (metaInfElement == null) {
                return;
            }

            Element linkedByElement = XmlUtil.getChildByTagName(metaInfElement, "linkedBy");

            if (linkedByElement == null) {
                return;
            }

            NodeList linkingFileList = linkedByElement.getElementsByTagName("linkingFile");

            if (linkingFileList == null) {
                return;
            }

            int listLength = linkingFileList.getLength();

            for (int i = 0; i < listLength; i++) {
                Element linkingFileElement = (Element) linkingFileList.item(i);

                String linkingFilePath = XmlUtil.getElementText(linkingFileElement);

                if (linkPath.equals(linkingFilePath)) {
                    linkedByElement.removeChild(linkingFileElement);

                    if (listLength == 1) {
                        metaInfElement.removeChild(linkedByElement);

                        cacheDirty.put(CommonUtils.getParentDir(linkTargetPath), new Boolean(true));
                    }
                    return;
                }
            }
        }

    }

    /**
     * TODO: never tested because not used up to now
     * 
     * @param path
     *            path of the link target file
     * @param newLinkingFiles
     *            list of Strings (path of linking files)
     */
    public void addLinkingFiles(String path, ArrayList newLinkingFiles) {
        if ((newLinkingFiles == null) || (newLinkingFiles.size() == 0)) {
            return;
        }

        synchronized (this) {
            Element metaInfElement = getMetaInfElement(path);

            if (metaInfElement == null) {
                metaInfElement = createMetaInfElement(path);
            }

            Document doc = metaInfElement.getOwnerDocument();

            Element linkedByListElement = XmlUtil.getChildByTagName(metaInfElement, "linkedBy");

            if (linkedByListElement == null) {
                linkedByListElement = doc.createElement("linkedBy");
                metaInfElement.appendChild(linkedByListElement);
            }

            Iterator iter = newLinkingFiles.iterator();

            while (iter.hasNext()) {
                String linkingFilePath = (String) iter.next();

                Element linkedByElement = doc.createElement("linkingFile");

                XmlUtil.setElementText(linkedByElement, linkingFilePath);

                linkedByListElement.appendChild(linkedByElement);
            }

            cacheDirty.put(CommonUtils.getParentDir(path), new Boolean(true));
        }

    }

    public Vector getListOfLinks(String path) {
        Element metaInfElement = getMetaInfElement(path, ".");

        if (metaInfElement == null) {
            return (null);
        }

        Element linkListElement = XmlUtil.getChildByTagName(metaInfElement, "links");

        if (linkListElement == null) {
            return (null);
        }

        NodeList linkList = linkListElement.getElementsByTagName("link");

        if (linkList == null) {
            return (null);
        }

        int listLength = linkList.getLength();

        if (listLength == 0) {
            return (null);
        }

        Vector listOfLinks = new Vector();

        for (int i = 0; i < listLength; i++) {
            Element linkElement = (Element) linkList.item(i);

            String name = XmlUtil.getChildText(linkElement, "name");
            String creator = XmlUtil.getChildText(linkElement, "creator");
            String destPath = XmlUtil.getChildText(linkElement, "destPath");

            String tmp = XmlUtil.getChildText(linkElement, "creationTime");

            long creationTime = 0L;

            try {
                creationTime = Long.parseLong(tmp);
            } catch (NumberFormatException nfex) {
                Logger.getLogger(getClass()).warn("invalid creation time: " + tmp);
            }

            FileLink link = new FileLink(name, destPath, creator, new Date(creationTime));

            listOfLinks.add(link);
        }

        // Collections.sort(listOfLinks, new
        // FileLinkComparator(FileLinkComparator.SORT_BY_NAME));

        return (listOfLinks);
    }

    public void incrementDownloads(String path) {
        String dir = null;

        String fileName = null;

        int separatorIdx = path.lastIndexOf('/');

        if (separatorIdx < 0) {
            separatorIdx = path.lastIndexOf(File.separatorChar);
        }

        if (separatorIdx < 0) {
            return;
        }

        if (separatorIdx > 0) {
            dir = path.substring(0, separatorIdx);
            fileName = path.substring(separatorIdx + 1);
        } else {
            if (separatorIdx == 0) {
                dir = path.substring(0, 1);
                fileName = path.substring(1);
            }
        }

        if (File.separatorChar != '/') {
            dir = dir.replace('/', File.separatorChar);
        }

        incrementDownloads(dir, fileName);
    }

    public void incrementDownloads(String path, String fileName) {
        synchronized (this) {
            Element metaInfElement = getMetaInfElement(path, fileName);

            if (metaInfElement == null) {
                metaInfElement = createMetaInfElement(path, fileName);
            }

            String oldValue = XmlUtil.getChildText(metaInfElement, "downloads");

            int downloadNum = 0;

            if (oldValue != null) {
                try {
                    downloadNum = Integer.parseInt(oldValue);
                } catch (NumberFormatException nfex) {
                }
            }

            downloadNum++;

            XmlUtil.setChildText(metaInfElement, "downloads", Integer.toString(downloadNum), false);

            cacheDirty.put(path, new Boolean(true));
            // saveMetaInfFile(path);
        }
    }

    public int getNumberOfDownloads(String absoluteFileName) {
        Element metaInfElement = getMetaInfElement(absoluteFileName);

        if (metaInfElement == null) {
            return (0);
        }

        int downLoadNum = 0;

        String temp = XmlUtil.getChildText(metaInfElement, "downloads");

        if (temp != null) {
            try {
                downLoadNum = Integer.parseInt(temp);
            } catch (NumberFormatException nfex) {
            }
        }

        return (downLoadNum);
    }

    public Vector getTopDownloadList(String path) {
        Element metaInfRoot = (Element) dirList.get(path);

        if (metaInfRoot == null) {
            metaInfRoot = loadMetaInfFile(path);

            if (metaInfRoot == null) {
                return (null);
            } else {
                dirList.put(path, metaInfRoot);
            }
        }

        Vector sortedTopList = new Vector();

        NodeList metaInfList = metaInfRoot.getElementsByTagName("metainf");

        if (metaInfList == null) {
            return (null);
        }

        int listLength = metaInfList.getLength();

        for (int i = 0; i < listLength; i++) {
            Element metaInfElement = (Element) metaInfList.item(i);

            String filename = metaInfElement.getAttribute("filename");

            if (filename != null) {
                String temp = XmlUtil.getChildText(metaInfElement, "downloads");

                if (temp != null) {
                    try {
                        int downloadNumber = Integer.parseInt(temp);

                        if (downloadNumber > 0) {
                            sortedTopList.add(metaInfElement);
                        }
                    } catch (NumberFormatException nfex) {
                    }
                }
            }
        }

        if (sortedTopList.size() > 1) {
            Collections.sort(sortedTopList, new MetaInfComparator(MetaInfComparator.SORT_BY_DOWNLOADS));
        }

        return (sortedTopList);
    }

    public Date getStatisticsResetDate(String path) {
        Element metaInfRoot = (Element) dirList.get(path);

        if (metaInfRoot == null) {
            metaInfRoot = loadMetaInfFile(path);

            if (metaInfRoot == null) {
                return (null);
            } else {
                dirList.put(path, metaInfRoot);
            }
        }

        String resetDateString = XmlUtil.getChildText(metaInfRoot, "statsResetDate");

        if (resetDateString == null) {
            return (null);
        }

        try {
            return (new Date(Long.parseLong(resetDateString)));
        } catch (NumberFormatException nfex) {
        }

        return (null);
    }

    public void resetStatistics(String path) {
        synchronized (this) {
            Element metaInfRoot = (Element) dirList.get(path);

            if (metaInfRoot == null) {
                metaInfRoot = loadMetaInfFile(path);

                if (metaInfRoot == null) {
                    return;
                } else {
                    dirList.put(path, metaInfRoot);
                }
            }

            XmlUtil.setChildText(metaInfRoot, "statsResetDate", Long.toString(System.currentTimeMillis()));

            NodeList metaInfList = metaInfRoot.getElementsByTagName("metainf");

            if (metaInfList != null) {
                int listLength = metaInfList.getLength();

                for (int i = 0; i < listLength; i++) {
                    Element metaInfElement = (Element) metaInfList.item(i);

                    Element downloadElement = XmlUtil.getChildByTagName(metaInfElement, "downloads");

                    if (downloadElement != null) {
                        metaInfElement.removeChild(downloadElement);
                    }
                }
            }

            cacheDirty.put(path, new Boolean(true));
        }
    }

    public boolean isStagedPublication(String path) {

        Element metaInfElement = getMetaInfElement(path, ".");

        if (metaInfElement == null) {
            return false;
        }

        String temp = XmlUtil.getChildText(metaInfElement, "stagedPublication");

        if (temp == null) {
            return false;
        }

        return Boolean.valueOf(temp);
    }

    public void setStagedPublication(String path, boolean publicateStaged) {
        synchronized (this) {
            Element metaInfElement = getMetaInfElement(path, ".");

            if (metaInfElement == null) {
                metaInfElement = createMetaInfElement(path, ".");
            }

            Document doc = metaInfElement.getOwnerDocument();

            Element stagedPublicationElement = XmlUtil.getChildByTagName(metaInfElement, "stagedPublication");

            if (stagedPublicationElement == null) {
                stagedPublicationElement = doc.createElement("stagedPublication");
                metaInfElement.appendChild(stagedPublicationElement);
            }

            XmlUtil.setElementText(stagedPublicationElement, Boolean.toString(publicateStaged));

            cacheDirty.put(path, new Boolean(true));
        }
    }

    public boolean isNotifyOnNewComment(String path) {

        Element metaInfElement = getMetaInfElement(path, ".");

        if (metaInfElement == null) {
            return false;
        }

        String temp = XmlUtil.getChildText(metaInfElement, "notifyOnNewComment");

        if (temp == null) {
            return false;
        }

        return Boolean.valueOf(temp);
    }

    public void setNotifyOnNewComment(String path, boolean notify) {
        synchronized (this) {
            Element metaInfElement = getMetaInfElement(path, ".");

            if (metaInfElement == null) {
                metaInfElement = createMetaInfElement(path, ".");
            }

            Document doc = metaInfElement.getOwnerDocument();

            Element notifyElement = XmlUtil.getChildByTagName(metaInfElement, "notifyOnNewComment");

            if (notifyElement == null) {
                notifyElement = doc.createElement("notifyOnNewComment");
                metaInfElement.appendChild(notifyElement);
            }

            XmlUtil.setElementText(notifyElement, Boolean.toString(notify));

            cacheDirty.put(path, new Boolean(true));
        }
    }
    
    public int getSortOrder(String path) {

        Element metaInfElement = getMetaInfElement(path, ".");

        if (metaInfElement == null) {
            return 0;
        }

        String temp = XmlUtil.getChildText(metaInfElement, "sortOrder");

        if (CommonUtils.isEmpty(temp)) {
            return 0;
        }

        return Integer.valueOf(temp);
    }

    public void setSortOrder(String path, int sortOrder) {
        synchronized (this) {
            Element metaInfElement = getMetaInfElement(path, ".");

            if (metaInfElement == null) {
                metaInfElement = createMetaInfElement(path, ".");
            }

            Document doc = metaInfElement.getOwnerDocument();

            Element sortElement = XmlUtil.getChildByTagName(metaInfElement, "sortOrder");

            if (sortElement == null) {
                sortElement = doc.createElement("sortOrder");
                metaInfElement.appendChild(sortElement);
            }

            XmlUtil.setElementText(sortElement, Integer.toString(sortOrder));

            cacheDirty.put(path, new Boolean(true));
        }
    }
    
    public int getStatus(String absoluteFileName) {
        Element metaInfElement = getMetaInfElement(absoluteFileName);

        if (metaInfElement == null) {
            return STATUS_NONE;
        }

        int status = STATUS_NONE;

        String temp = XmlUtil.getChildText(metaInfElement, "status");

        if (temp != null) {
            try {
                status = Integer.parseInt(temp);
            } catch (NumberFormatException nfex) {
            }
        }

        return (status);
    }

    public void setStatus(String path, int newStatus) {
        String[] partsOfPath = CommonUtils.splitPath(path);
        setStatus(partsOfPath[0], partsOfPath[1], newStatus);
    }

    public void setStatus(String path, String fileName, int newStatus) {
        synchronized (this) {
            Element metaInfElement = getMetaInfElement(path, fileName);

            if (metaInfElement == null) {
                metaInfElement = createMetaInfElement(path, fileName);
            }

            Document doc = metaInfElement.getOwnerDocument();

            Element statusElement = XmlUtil.getChildByTagName(metaInfElement, "status");

            if (statusElement == null) {
                statusElement = doc.createElement("status");
                metaInfElement.appendChild(statusElement);
            }

            XmlUtil.setElementText(statusElement, Integer.toString(newStatus));

            cacheDirty.put(path, new Boolean(true));
        }
    }

    /**
     * Explicitly remove the meta information of a directory from the cache.
     * Used in the search function which can cause hundreds of meta info files
     * to get loaded.
     * 
     * @param dirPath
     *            the absolute path of the directory
     */
    public void releaseMetaInf(String dirPath) {
        synchronized (this) {
            if (dirList.get(dirPath) != null) {
                dirList.remove(dirPath);
            }

            if (cacheDirty.containsKey(dirPath)) {
                cacheDirty.remove(dirPath);
            }
        }

    }

    public synchronized void run() {
        int counter = 0;
        boolean stop = false;

        while (!stop) {
            try {
                this.wait(60000);

                Enumeration cacheDirtyList = cacheDirty.keys();

                while (cacheDirtyList.hasMoreElements()) {
                    String path = (String) cacheDirtyList.nextElement();

                    saveMetaInfFile(path);

                    cacheDirty.remove(path);
                }

                if (counter == 15) {
                    counter = 0;

                    if (dirList.size() > 0) {
                        synchronized (dirList) {
                            Logger.getLogger(getClass()).debug("removing " + dirList.size() + " elements from metainf cache");

                            dirList.clear();
                        }
                    }
                }

                counter++;
            } catch (InterruptedException e) {
                Enumeration cacheDirtyList = cacheDirty.keys();

                while (cacheDirtyList.hasMoreElements()) {
                    String path = (String) cacheDirtyList.nextElement();

                    saveMetaInfFile(path);
                }

                Logger.getLogger(getClass()).debug("MetaInfmanager ready for shutdown");

                stop = true;
            }
        }
    }

    public boolean isMetaInfFile(String path) {
        return (path.endsWith(METAINF_FILE));
    }

}
