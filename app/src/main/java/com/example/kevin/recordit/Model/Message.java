package com.example.kevin.recordit.Model;

public class Message {
    private String message;
    private Boolean readStatus;
    private Long time;
    private String type;
    private String fromUserId;

    public Message() {
    }

    public Message(String message, Boolean readStatus, Long time, String type
    , String fromUserId) {
        this.message = message;
        this.readStatus = readStatus;
        this.time = time;
        this.type = type;
        this.fromUserId = fromUserId;

    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Boolean getReadStatus() {
        return readStatus;
    }

    public void setReadStatus(Boolean readStatus) {
        this.readStatus = readStatus;
    }

    public Long getTime() {
        return time;
    }

    public String getType() {
        return type;
    }

    public String getFromUserId() {
        return fromUserId;
    }

    public void setFromUserId(String fromUserId) {
        this.fromUserId = fromUserId;
    }

    @Override
    public String toString() {
        return "Message{" +
                "message='" + message + '\'' +
                '}';
    }
}
