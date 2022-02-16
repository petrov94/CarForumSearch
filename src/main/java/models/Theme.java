package models;

import java.util.List;

public class Theme {
    private final String name;
    private final List<Comment> comments;

    public Theme(){
        name = null;
        comments = null;
    }

    public Theme(String name, List<Comment> comments) {
        this.name = name;
        this.comments = comments;
    }

    public String getName() {
        return name;
    }

    public List<Comment> getComments() {
        return comments;
    }
}
