package de.webfilesys.gui.ajax;

import java.io.File;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.w3c.dom.Element;

import de.webfilesys.MetaInfManager;
import de.webfilesys.util.XmlUtil;

/**
 * @author Frank Hoehnel
 * 
 */
public class CheckForGPXTrackHandler extends XmlRequestHandlerBase {

    public CheckForGPXTrackHandler(HttpServletRequest req, HttpServletResponse resp, HttpSession session, PrintWriter output, String uid) {
        super(req, resp, session, output, uid);
    }

    protected void process() {
        String path = getCwd();

        MetaInfManager metaInfMgr = MetaInfManager.getInstance();

        boolean gpxTrackExist = false;
        
        File folderFile = new File(path);

        File[] fileList = folderFile.listFiles();

        for (int k = 0; (!gpxTrackExist) && (k < fileList.length); k++) {
            List<String> attachments = metaInfMgr.getListOfAttachments(fileList[k].getAbsolutePath());
            if (attachments != null) {
                for (int i = 0; (!gpxTrackExist) && (i < attachments.size()); i++) {
                    if (isGpsTrack(attachments.get(i))) {
                        gpxTrackExist = true;
                    }
                }
            }
        }

        Element resultElement = doc.createElement("result");

        XmlUtil.setElementText(resultElement, Boolean.toString(gpxTrackExist));

        doc.appendChild(resultElement);

        processResponse();
    }

    private boolean isGpsTrack(String attachmentFileName) {
        return attachmentFileName.endsWith(".GPX") || attachmentFileName.endsWith(".gpx");
    }
}
