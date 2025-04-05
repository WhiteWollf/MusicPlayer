package com.example.musicplayer.Models;

public class LoginUserModel {
    private String email;
    private  String password;

    public LoginUserModel(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }
}
