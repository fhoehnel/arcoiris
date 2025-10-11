package de.webfilesys.config;

public class BlogConfig {

    public enum SortOrder {
        BLOG,
        DIARY;
    }

    private boolean stagedPublication = false;
    private boolean notifyOnNewComment = false;
    private SortOrder sortOrder = SortOrder.BLOG;
    private String titlePic = null;
    private String titleText = null;

    public boolean isStagedPublication() {
        return stagedPublication;
    }

    public void setStagedPublication(boolean stagedPublication) {
        this.stagedPublication = stagedPublication;
    }

    public boolean isNotifyOnNewComment() {
        return notifyOnNewComment;
    }

    public void setNotifyOnNewComment(boolean notifyOnNewComment) {
        this.notifyOnNewComment = notifyOnNewComment;
    }

    public SortOrder getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(SortOrder sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getTitlePic() {
        return titlePic;
    }

    public void setTitlePic(String titlePic) {
        this.titlePic = titlePic;
    }

    public String getTitleText() {
        return titleText;
    }

    public void setTitleText(String titleText) {
        this.titleText = titleText;
    }

}
