package de.webfilesys.config;

public class BlogConfig {

    public enum SortOrder {
        BLOG,
        DIARY;
    }

    private static final int DEFAULT_TITLE_PIC_POSITION = 50;

    private boolean stagedPublication = false;
    private boolean notifyOnNewComment = false;
    private boolean showSideCont = true;
    private SortOrder sortOrder = SortOrder.BLOG;
    private String titlePic = null;
    private String titleText = null;
    private int titlePicPosition = DEFAULT_TITLE_PIC_POSITION;

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

    public boolean isShowSideCont() {
        return showSideCont;
    }

    public void setShowSideCont(boolean showSideCont) {
        this.showSideCont = showSideCont;
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

    public int getTitlePicPosition() {
        return titlePicPosition;
    }

    public void setTitlePicPosition(int titlePicPosition) {
        this.titlePicPosition = titlePicPosition;
    }

    public void unsetTitlePicPosition() {
        titlePicPosition = DEFAULT_TITLE_PIC_POSITION;
    }

}
