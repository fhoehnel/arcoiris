package de.webfilesys.gui.xsl;

import de.webfilesys.attachment.AttachmentManager;
import de.webfilesys.servlet.UploadServlet;
import de.webfilesys.util.CommonUtils;
import de.webfilesys.util.XmlUtil;
import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.PrintWriter;
import java.util.List;

/**
 * @author Frank Hoehnel
 */
public class MultiOSMTrackHandler extends XslRequestHandlerBase {
	private static final Logger LOG = Logger.getLogger(MultiOSMTrackHandler.class);

	public MultiOSMTrackHandler(HttpServletRequest req, HttpServletResponse resp, HttpSession session,
                                PrintWriter output, String uid) {
		super(req, resp, session, output, uid);
	}

	protected void process() {
		String currentPath = getCwd();
        
        Element gpxTrackElem = doc.createElement("gpxTracks");

        doc.appendChild(gpxTrackElem);
        
        Element gpxFileListElem = doc.createElement("gpxFiles");
        
        gpxTrackElem.appendChild(gpxFileListElem);

        File folderFile = new File(currentPath);

        File[] fileList = folderFile.listFiles();

        for (File file : fileList) {
            List<String> attachments = AttachmentManager.getInstance().getAttachments(file.getAbsolutePath());
            if (attachments != null) {
                for (String attachment : attachments) {
                    if (isGpsTrack(attachment)) {
                        String attachmentsPath = CommonUtils.joinFilesysPath(currentPath, UploadServlet.SUBDIR_ATTACHMENT);
                        String filePath = CommonUtils.joinFilesysPath(attachmentsPath, attachment);
                        
                        Element gpxFileElem = doc.createElement("gpxFile");
                        XmlUtil.setElementText(gpxFileElem, CommonUtils.escapeForJavascript(filePath));
                        gpxFileListElem.appendChild(gpxFileElem);
                    }
                }
            }
        }

        XmlUtil.setChildText(doc.getDocumentElement(), "contextRoot", req.getContextPath());

        processResponse("multiOSMTracks.xsl", req);
	}
	
    private boolean isGpsTrack(String attachmentFileName) {
        return attachmentFileName.endsWith(".GPX") || attachmentFileName.endsWith(".gpx");
    }
	
}