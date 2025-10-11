package de.webfilesys.attachment;

public class BlogAttachment {

    public enum AttachmentType {
        GPX,
        OTHER
    }

    private String fileName;
    private AttachmentType type;

    public AttachmentType getType() {
        return type;
    }

    public void setType(AttachmentType type) {
        this.type = type;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

}
