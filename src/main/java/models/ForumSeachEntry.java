package models;

public class ForumSeachEntry {
    public final long postId;
    public final String content;

    public ForumSeachEntry(long postId, String content) {
        this.postId = postId;
        this.content = content;
    }

    public long getPostId() {
        return postId;
    }

    public String getContent() {
        return content;
    }
}
