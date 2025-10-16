package de.webfilesys.metainf;

import de.webfilesys.Comment;
import de.webfilesys.GeoTag;

import java.util.ArrayList;

public class MetaInfData {

    private String description;

    private int status;

    private GeoTag geoTag;

    private ArrayList<Comment> comments;

    private ArrayList<String> likers;

    private boolean commentsSeenByOwner;

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public GeoTag getGeoTag() {
        return geoTag;
    }

    public void setGeoTag(GeoTag geoTag) {
        this.geoTag = geoTag;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setComments(ArrayList<Comment> comments) {
        this.comments = comments;
    }

    public ArrayList<Comment> getComments() {
        return comments;
    }

    public boolean isCommentsSeenByOwner() {
        return commentsSeenByOwner;
    }

    public void setCommentsSeenByOwner(boolean commentsSeenByOwner) {
        this.commentsSeenByOwner = commentsSeenByOwner;
    }

    public void addComment(Comment comment) {
        if (comments == null) {
            comments = new ArrayList<>();
        }
        comments.add(comment);
    }

    public boolean addLiker(String visitorId) {
        if (likers == null) {
            likers = new ArrayList<>();
        }
        boolean alreadyLiked = likers.contains(visitorId);
        if (!alreadyLiked) {
            likers.add(visitorId);
        }
        return !alreadyLiked;
    }

    public ArrayList<String> getLikers() {
        return likers;
    }

    public void setLikers(ArrayList<String> likers) {
        this.likers = likers;
    }
}
