package de.webfilesys.gui.user;

import java.io.File;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import de.webfilesys.GeoTag;
import de.webfilesys.MetaInfManager;
import de.webfilesys.graphics.CameraExifData;
import de.webfilesys.util.CommonUtils;

/**
 * Points of interest for Open Street Maps.
 * 
 * @author Frank Hoehnel
 */
public class OpenStreetMapPOIHandler extends UserRequestHandler {
    public OpenStreetMapPOIHandler(HttpServletRequest req, HttpServletResponse resp, HttpSession session, PrintWriter output, String uid) {
        super(req, resp, session, output, uid);
    }

    protected void process() {
        String fileName = getParameter("fileName");

        if (CommonUtils.isEmpty(fileName)) {
            Logger.getLogger(getClass()).warn("fileName parameter missing");
            return;
        }

        String filePath = getCwd();
        if (filePath.endsWith(File.separator)) {
            filePath = filePath + fileName;
        } else {
            filePath = filePath + File.separator + fileName;
        }

        File file = new File(filePath);

        if (!file.exists()) {
            Logger.getLogger(getClass()).error("file not found: " + filePath);
            return;
        }

        MetaInfManager metaInfMgr = MetaInfManager.getInstance();

        GeoTag geoTag = metaInfMgr.getGeoTag(filePath);

        float latitude = Float.NEGATIVE_INFINITY;
        float longitude = Float.NEGATIVE_INFINITY;
        String infoText = "";
        String description = metaInfMgr.getDescription(filePath);
        if ((description == null) || (description.trim().length() == 0)) {
            description = fileName;
        }

        if (geoTag != null) {
            latitude = geoTag.getLatitude();
            longitude = geoTag.getLongitude();
            infoText = geoTag.getInfoText();
            if ((infoText == null) || (infoText.trim().length() == 0)) {
                infoText = fileName;
            }
        } else {
            String fileExt = CommonUtils.getFileExtension(fileName);

            if (fileExt.equals(".jpg") || fileExt.equals(".jpeg")) {
                // use GPS coordinates from Exif data if present in the JPEG
                // file
                CameraExifData exifData = new CameraExifData(filePath);

                if (exifData.hasExifData()) {
                    latitude = exifData.getGpsLatitude();
                    longitude = exifData.getGpsLongitude();

                    if ((latitude >= 0.0f) && (longitude >= 0.0f)) {
                        String latitudeRef = exifData.getGpsLatitudeRef();

                        if ((latitudeRef != null) && latitudeRef.equalsIgnoreCase("S")) {
                            latitude = (-latitude);
                        }

                        String longitudeRef = exifData.getGpsLongitudeRef();

                        if ((longitudeRef != null) && longitudeRef.equalsIgnoreCase("W")) {
                            longitude = (-longitude);
                        }
                    }
                }
            }
        }

        if ((longitude == Float.NEGATIVE_INFINITY) || (latitude == Float.NEGATIVE_INFINITY)) {
            Logger.getLogger(getClass()).error("No Geo Tag / GPS Exif data exists for file/folder " + filePath);

            return;
        }

        resp.setContentType("text/plain");
        output.println("lat\tlon\ttitle\tdescription\ticon\ticonSize\ticonOffset");
        output.print(latitude);
        output.print('\t');
        output.print(longitude);
        output.print('\t');
        output.print(infoText);
        output.print('\t');
        output.print(description);
        output.print('\t');
        output.print(req.getContextPath() + "/images/OSMaps.png");
        output.print('\t');
        output.print("32,32");
        output.print('\t');
        output.println("-16,-16");
        output.flush();
    }

}