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
import de.webfilesys.gui.user.UserRequestHandler;
import de.webfilesys.servlet.UploadServlet;
import de.webfilesys.util.CommonUtils;
import de.webfilesys.util.XmlUtil;

/**
 * GPS track file viewer.
 * 
 * @author Frank Hoehnel
 */
public class GPXViewHandler extends UserRequestHandler {
	private static final String STYLESHEET_REF = "<?xml-stylesheet type=\"text/xsl\" href=\"$contextRoot/xsl/gpxViewer.xsl\"?>";

	private static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\" standalone=\"yes\"?>";

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
		if (req.getScheme().equalsIgnoreCase("https")) {
			googleMapsAPIKey = ArcoirisBlog.getInstance().getGoogleMapsAPIKeyHTTPS();
		} else {
			googleMapsAPIKey = ArcoirisBlog.getInstance().getGoogleMapsAPIKeyHTTP();
		}

		BufferedReader gpxReader = null;

		try {
			resp.setContentType("text/xml");

			gpxReader = new BufferedReader(new FileReader(filePath));

			XMLInputFactory factory = XMLInputFactory.newInstance();
			XMLStreamReader parser = factory.createXMLStreamReader(gpxReader);

			String tagName = null;

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
						tagName = parser.getLocalName();

						if (tagName.equals("gpx")) {
							output.println(XML_HEADER);
							output.println(STYLESHEET_REF.replace("$contextRoot", req.getContextPath()));

							output.println("<gpx>");

							if (!CommonUtils.isEmpty(googleMapsAPIKey)) {
								output.println("  <googleMapsAPIKey>" + googleMapsAPIKey + "</googleMapsAPIKey>");
							}
							output.println("  <filePath>" + CommonUtils.escapeForJavascript(filePath) + "</filePath>");
							
                            output.println("  <contextRoot>" + req.getContextPath() + "</contextRoot>");
						}

						if (tagName.equals("trk")) {
							output.println("<track>" + trackCounter + "</track>");
							trackCounter++;
						}

						break;

					case XMLStreamConstants.END_ELEMENT:

						tagName = parser.getLocalName();
						if (tagName.equals("gpx")) {
							output.println("</gpx>");
						}
						break;

					default:
						// System.out.println("unhandled event: " + event);
					}
				} catch (WstxParsingException epex) {
					Logger.getLogger(getClass()).warn("GPX parsing error", epex);
				}
			}

			output.flush();
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
	}
}
