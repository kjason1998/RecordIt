package com.example.kevin.recordit.Model;

public class User {

    public String userName;
    public String userStatus;
    public String userImage;
    //compress image of userImage using zetbaitsu compressor
    public String userThumbImage;

    public User(){
        //default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public User(String username,String userStatus,String userImage,String userThumbImage){
        this.userName = username;
        this.userStatus = userStatus;
        this.userImage = userImage;
        this.userThumbImage = userThumbImage;
    }

    public String getUserThumbImage() {
        return userThumbImage;
    }

    public void setUserThumbImage(String userThumbImage) {
        this.userThumbImage = userThumbImage;
    }

    public String getUserImage() {
        return userImage;
    }

    public void setUserImage(String userImage) {
        this.userImage = userImage;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserStatus() {
        return userStatus;
    }

    public void setUserStatus(String userStatus) {
        this.userStatus = userStatus;
    }
}
