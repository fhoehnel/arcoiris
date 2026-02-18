package de.webfilesys.gui.xsl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.log4j.Logger;

import com.ctc.wstx.exc.WstxParsingException;

import de.webfilesys.ArcoirisBlog;
import de.webfilesys.servlet.UploadServlet;
import de.webfilesys.util.CommonUtils;
import de.webfilesys.util.XmlUtil;
import org.w3c.dom.Element;

/**
 * GPS track file viewer.
 * 
 * @author Frank Hoehnel
 */
public class GPXViewHandler extends XslRequestHandlerBase  {

	public GPXViewHandler(
    		HttpServletRequest req, 
    		HttpServletResponse resp,
            HttpSession session,
            PrintWriter output, 
            String uid) {
        super(req, resp, session, output, uid);
	}	
	
	protected void process() {
	
        String attachmentName = getParameter("attachmentName");

        String mapType = getParameter("mapType");

        String cwd = getCwd();
        
        StringBuffer attachmentFilePath = new StringBuffer(cwd);
        
        if (!cwd.endsWith(File.separator)) {
            attachmentFilePath.append(File.separatorChar);
        }

        attachmentFilePath.append(UploadServlet.SUBDIR_ATTACHMENT);
        attachmentFilePath.append(File.separator);
        attachmentFilePath.append(attachmentName);
	    
        String filePath = attachmentFilePath.toString();
        
		String googleMapsAPIKey = null;
        if (!"osm".equalsIgnoreCase(mapType)) {
            if (req.getScheme().equalsIgnoreCase("https")) {
                googleMapsAPIKey = ArcoirisBlog.getInstance().getGoogleMapsAPIKeyHTTPS();
            } else {
                googleMapsAPIKey = ArcoirisBlog.getInstance().getGoogleMapsAPIKeyHTTP();
            }
        }

        Element gpxElem = doc.createElement("gpx");
        doc.appendChild(gpxElem);

		BufferedReader gpxReader = null;

		try {
			resp.setContentType("text/xml");

			gpxReader = new BufferedReader(new FileReader(filePath));

			XMLInputFactory factory = XMLInputFactory.newInstance();
			XMLStreamReader parser = factory.createXMLStreamReader(gpxReader);

			boolean documentEnd = false;

			int trackCounter = 0;

			while (!documentEnd) {
				try {
					int event = parser.next();

					switch (event) {
					case XMLStreamConstants.END_DOCUMENT:
						parser.close();
						documentEnd = true;
						break;

					case XMLStreamConstants.START_DOCUMENT:
						break;

					case XMLStreamConstants.START_ELEMENT:
						String tagName = parser.getLocalName();

                        if (tagName.equals("gpx")) {
                            if (!CommonUtils.isEmpty(googleMapsAPIKey)) {
                                XmlUtil.setChildText(gpxElem, "googleMapsAPIKey", googleMapsAPIKey);
                            }
                            XmlUtil.setChildText(gpxElem, "filePath", CommonUtils.escapeForJavascript(filePath));
                            XmlUtil.setChildText(gpxElem, "language", language);
                            XmlUtil.setChildText(gpxElem, "contextRoot", req.getContextPath());
                        }
                        if (tagName.equals("trk")) {
                            Element trackElem = doc.createElement("track");
                            XmlUtil.setElementText(trackElem, Integer.toString(trackCounter));
                            gpxElem.appendChild(trackElem);
                            trackCounter++;
                        }
						break;
					}
				} catch (WstxParsingException epex) {
					Logger.getLogger(getClass()).warn("GPX parsing error", epex);
				}
			}
		} catch (IOException e) {
			Logger.getLogger(getClass()).error("failed to read GPX file", e);
		} catch (XMLStreamException xmlEx) {
			Logger.getLogger(getClass()).error("error parsing XML stream", xmlEx);
		} catch (Exception e) {
			Logger.getLogger(getClass()).error("failed to transform GPX file", e);
		} finally {
			if (gpxReader != null) {
				try {
					gpxReader.close();
				} catch (Exception ex) {
				}
			}
		}
        String xslFileName = "osm".equalsIgnoreCase(mapType) ? "gpxOSMViewer.xsl" : "gpxViewer.xsl";
        processResponse(xslFileName, req);
	}
}
