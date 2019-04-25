package com.example.kevin.recordit.Model;

public class Message {
    private String message;
    private Boolean readStatus;
    private Long timeSent;
    private String messageType;
    private String fromUserId;

    public Message() {
    }

    public Message(String message, Boolean readStatus, Long timeSent, String messageType
    , String fromUserId) {
        this.message = message;
        this.readStatus = readStatus;
        this.timeSent = timeSent;
        this.messageType = messageType;
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

    public Long getTimeSent() {
        return timeSent;
    }

    public void setTimeSent(Long timeSent) {
        this.timeSent = timeSent;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
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
                ", readStatus=" + readStatus +
                ", timeSent=" + timeSent +
                ", messageType='" + messageType + '\'' +
                ", fromUserId='" + fromUserId + '\'' +
                '}';
    }
}
