package models;

public class Comment {
    private final String comment;

    public Comment() {
        comment = null;
    }

    public Comment(String comment) {
        this.comment = comment;
    }

    public String getComment() {
        return comment;
    }
}
