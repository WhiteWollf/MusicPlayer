package com.example.musicplayer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.musicplayer.Database.DatabaseController;
import com.example.musicplayer.Models.LoginUserModel;
import com.example.musicplayer.Models.UserModel;

public class LoginActivity extends AppCompatActivity {

    EditText emailTxt, passwordTxt;
    Button loginBtn, registrationBtn;
    DatabaseController db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        sharedPrefLoginCheck();

        emailTxt = findViewById(R.id.inpEmail);
        passwordTxt = findViewById(R.id.inpPassword);
        registrationBtn = findViewById(R.id.btnRegistration);
        loginBtn = findViewById(R.id.btnLogin);
        db=new DatabaseController(this);

        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (emailTxt.getText().toString().isEmpty() || passwordTxt.getText().toString().isEmpty()) {
                    Toast.makeText(LoginActivity.this, R.string.requiredfields, Toast.LENGTH_SHORT).show();
                } else {
                    boolean loginResult = db.login(new LoginUserModel(emailTxt.getText().toString(), passwordTxt.getText().toString()));
                    if (!loginResult) {
                        Toast.makeText(LoginActivity.this, R.string.loginunsuccessfull, Toast.LENGTH_LONG).show();
                    }else{
                            SharedPreferences sharedPreferences = getSharedPreferences("sharedPrefs", MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putBoolean("isLoggedIn", true);
                            UserModel userdata = db.getUserInfo(emailTxt.getText().toString());
                            editor.putString("email", userdata.getEmail());
                            editor.putString("userName", userdata.getUserName());
                            editor.apply();

                            Intent i = new Intent(LoginActivity.this, MusicPlayer.class);
                            startActivity(i);
                            finish();
                    }
                }

            }
        });

        registrationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(i);
                finish();
            }
        });

    } //onCreateEnd

    private void sharedPrefLoginCheck() {
        SharedPreferences sharedPreferences = getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        boolean loggedIn = sharedPreferences.getBoolean("isLoggedIn", false);
        if (loggedIn) {
                Intent i = new Intent(LoginActivity.this, MusicPlayer.class);
                startActivity(i);
                finish();
        }
    }
}