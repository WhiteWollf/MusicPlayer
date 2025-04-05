package com.example.musicplayer.Models;

public class RegisterUserModel {
    private String userName;
    private String email;
    private  String password;
    private  String passwordConfirm;

    public RegisterUserModel() {
    }

    public RegisterUserModel(String userName, String email, String password, String passwordConfirm) {
        this.userName = userName;
        this.email = email;
        this.password = password;
        this.passwordConfirm = passwordConfirm;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPasswordConfirm() {
        return passwordConfirm;
    }

    public void setPasswordConfirm(String passwordConfirm) {
        this.passwordConfirm = passwordConfirm;
    }
}
