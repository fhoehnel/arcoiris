package de.webfilesys.state;

public class BlogState {

    private boolean unnotifiedComments = false;
    private int unseenCommentCount = 0;

    public int getUnseenCommentCount() {
        return unseenCommentCount;
    }

    public void setUnseenCommentCount(int unseenCommentCount) {
        this.unseenCommentCount = unseenCommentCount;
    }

    public boolean hasUnnotifiedComments() {
        return unnotifiedComments;
    }

    public void setUnnotifiedComments(boolean unnotifiedComments) {
        this.unnotifiedComments = unnotifiedComments;
    }
}
