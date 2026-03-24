package de.webfilesys.gui.xsl;

import de.webfilesys.*;
import de.webfilesys.graphics.CameraExifData;
import de.webfilesys.gui.user.UserRequestHandler;
import de.webfilesys.metainf.BlogMetaInfManager;
import de.webfilesys.util.CommonUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.PrintWriter;
import java.net.URLEncoder;

public class OpenStreetMapMultiPOIHandler extends UserRequestHandler {

    public OpenStreetMapMultiPOIHandler(
            HttpServletRequest req,
            HttpServletResponse resp,
            HttpSession session,
            PrintWriter output,
            String uid) {
        super(req, resp, session, output, uid);
    }

    protected void process() {
        resp.setContentType("text/plain; charset=UTF-8");

        output.println("lat\tlon\ttitle\tdescription\ticon\ticonSize\ticonOffset");

        String path = getCwd();
        File folderFile = new File(path);

        File[] fileList = folderFile.listFiles();
        if (fileList != null) {
            for (File file : fileList) {
                if (CommonUtils.isPictureFile(file)) {
                    GeoTag geoTag = BlogMetaInfManager.getInstance().getGeoTag(file.getAbsolutePath());
                    if (geoTag != null) {
                        String infoText = geoTag.getInfoText();
                        if (infoText == null || infoText.isEmpty()) {
                            infoText = BlogMetaInfManager.getInstance().getDescription(file.getAbsolutePath());
                        }
                        if (infoText == null) {
                            infoText = "";
                        }
                        infoText = removeEmojis(infoText);
                        infoText = CommonUtils.truncate(infoText,30);
                        addMarker(geoTag.getLatitude(), geoTag.getLongitude(), infoText, file.getName());
                    } else {
                        String fileExt = CommonUtils.getFileExtension(file.getName());
                        if (fileExt.equals(".jpg") || fileExt.equals(".jpeg")) {
                            // use GPS coordinates from Exif data if present in the JPEG file
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
                                    addMarker(gpsLatitude, gpsLongitude, "", file.getName());
                                }
                            }
                        }
                    }
                }
            }
        }
        output.flush();
    }

    private void addMarker(float latitude, float longitude, String infoText, String fileName) {
        String descrText = "<img src=\"" + req.getContextPath() + "/servlet?command=getFile&amp;fileName=" + URLEncoder.encode(fileName) + "&amp;cached=true&amp;thumb=true\">";

        output.print(latitude);
        output.print('\t');
        output.print(longitude);
        output.print('\t');
        output.print(infoText);
        output.print('\t');
        output.print(descrText);
        output.print('\t');
        output.print(req.getContextPath() + "/images/OSMaps.png");
        output.print('\t');
        output.print("32,32");
        output.print('\t');
        output.println("-16,-16");
    }

     private String removeEmojis(String text) {
         return text.replaceAll("\\{emoji-[0-9][0-9]}", "");
     }

}