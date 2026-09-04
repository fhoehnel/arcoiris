package de.webfilesys.gui.xsl;

import java.io.File;
import java.io.PrintWriter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import de.webfilesys.metainf.BlogMetaInfManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Element;
import de.webfilesys.GeoTag;
import de.webfilesys.ArcoirisBlog;
import de.webfilesys.graphics.CameraExifData;
import de.webfilesys.util.CommonUtils;
import de.webfilesys.util.XmlUtil;

/**
 * @author Frank Hoehnel
 */
public class GoogleMultiLocationMapHandler extends XslRequestHandlerBase {
    private static final Logger LOG = LogManager.getLogger(GoogleMultiLocationMapHandler.class);

    public GoogleMultiLocationMapHandler(HttpServletRequest req, HttpServletResponse resp, HttpSession session, PrintWriter output, String uid) {
        super(req, resp, session, output, uid);
    }

    protected void process() {
        String path = getCwd();

        File folderFile = new File(path);

        Element geoDataElement = doc.createElement("geoData");

        doc.appendChild(geoDataElement);

        XmlUtil.setChildText(geoDataElement, "language", language, false);
        XmlUtil.setChildText(geoDataElement, "skin", userMgr.getCSS(uid), false);

        Element mapDataElement = doc.createElement("mapData");

        geoDataElement.appendChild(mapDataElement);

        XmlUtil.setChildText(mapDataElement, "zoomLevel", Integer.toString(3));

        Element markersElement = doc.createElement("markers");

        geoDataElement.appendChild(markersElement);

        File[] fileList = folderFile.listFiles();

        if (fileList != null) {
            for (File file : fileList) {
                if (CommonUtils.isPictureFile(file)) {
                    GeoTag geoTag = BlogMetaInfManager.getInstance().getGeoTag(file.getAbsolutePath());
                    if (geoTag != null) {
                        String infoText = geoTag.getInfoText();

                        if (infoText != null) {
                            infoText = removeEmojis(infoText);
                        }
                        addMarker(markersElement, geoTag.getLatitude(), geoTag.getLongitude(), infoText, file.getName());
                    } else {
                        String fileExt = CommonUtils.getFileExtension(file.getName());

                        if (fileExt.equals(".jpg") || fileExt.equals(".jpeg")) {
                            // use GPS coordinates from Exif data if present in the JPEG
                            // file
                            CameraExifData exifData = new CameraExifData(path);

                            if (exifData.hasExifData()) {
                                float gpsLatitude = exifData.getGpsLatitude();
                                float gpsLongitude = exifData.getGpsLongitude();

                                if ((gpsLatitude >= 0.0f) && (gpsLongitude >= 0.0f)) {
                                    String latitudeRef = exifData.getGpsLatitudeRef();

                                    if ((latitudeRef != null) && latitudeRef.equalsIgnoreCase("S")) {
                                        gpsLatitude = (-gpsLatitude);
                                    }

                                    String longitudeRef = exifData.getGpsLongitudeRef();

                                    if ((longitudeRef != null) && longitudeRef.equalsIgnoreCase("W")) {
                                        gpsLongitude = (-gpsLongitude);
                                    }

                                    addMarker(markersElement, gpsLatitude, gpsLongitude, null, file.getName());
                                }
                            }
                        }
                    }
                }
            }
        }

        String googleMapsAPIKey;
        if (req.getScheme().equalsIgnoreCase("https")) {
            googleMapsAPIKey = ArcoirisBlog.getInstance().getGoogleMapsAPIKeyHTTPS();
        } else {
            googleMapsAPIKey = ArcoirisBlog.getInstance().getGoogleMapsAPIKeyHTTP();
        }
        
        if (!CommonUtils.isEmpty(googleMapsAPIKey)) {
            XmlUtil.setChildText(geoDataElement, "googleMapsAPIKey", googleMapsAPIKey, false);
        }

        XmlUtil.setChildText(doc.getDocumentElement(), "contextRoot", req.getContextPath());

        processResponse("googleMapMulti.xsl", req);
    }

    private void addMarker(Element markersElement, float latitude, float longitude, String infoText, String fileName) {
        Element markerElement = doc.createElement("marker");

        markersElement.appendChild(markerElement);

        XmlUtil.setChildText(markerElement, "latitude", Float.toString(latitude), false);
        XmlUtil.setChildText(markerElement, "longitude", Float.toString(longitude), false);

        if ((infoText != null) && (!infoText.isEmpty())) {
            XmlUtil.setChildText(markerElement, "infoText", infoText.replace('\'',  '´'), false);
        }

        if (fileName != null) {
            XmlUtil.setChildText(markerElement, "fileName", fileName, false);
        }
    }

    private String removeEmojis(String infoText) {
        if ((infoText.indexOf('{') < 0) || (infoText.indexOf('}') < 0)) {
            return infoText;
        }

        StringBuffer buff = new StringBuffer(infoText.length());

        boolean ignore = false;

        for (int i = 0; i < infoText.length(); i++) {
            char c = infoText.charAt(i);

            if (c == '{') {
                ignore = true;
            } else if (c == '}') {
                ignore = false;
            } else {
                if (!ignore) {
                    buff.append(c);
                }
            }
        }

        return buff.toString();
    }

}