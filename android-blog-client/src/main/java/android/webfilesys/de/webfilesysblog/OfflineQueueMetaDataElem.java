package android.webfilesys.de.webfilesysblog;

import com.google.android.gms.maps.model.LatLng;

import java.util.Date;

/**
 * Created by fho on 24.09.2017.
 */

public class OfflineQueueMetaDataElem {
    private Date blogDate;

    private String blogText;

    private LatLng geoLocation;

    private boolean publishImmediately;

    private String serverUrl;

    private String userid;

    public void setBlogDate(Date newVal) {
        blogDate = newVal;
    }

    public Date getBlogDate() {
        return blogDate;
    }

    public void setBlogText(String newVal) {
        blogText = newVal;
    }

    public String getBlogText() {
        return blogText;
    }

    public void setGeoLocation(LatLng newVal) {
        geoLocation = newVal;
    }

    public LatLng getGeoLocation() {
        return geoLocation;
    }

    public void setPublishImmediately(boolean newVal) {
        publishImmediately = newVal;
    }

    public boolean isPublishImmediately() {
        return publishImmediately;
    }

    public void setServerUrl(String newVal) {
        serverUrl = newVal;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setUserid(String newVal) {
        userid = newVal;
    }

    public String getUserid() {
        return userid;
    }
}
