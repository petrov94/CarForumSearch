package models;

public class Topic {

    private String href;
    private String topic;

    public Topic(String href, String topic) {
        this.href = href;
        this.topic = topic;
    }

    public String getHref() {
        return href;
    }

    public String getTopic() {
        return topic;
    }
}
