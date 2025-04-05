package com.example.musicplayer;

import android.content.Intent;
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
import com.example.musicplayer.Models.RegisterUserModel;

public class RegisterActivity extends AppCompatActivity {

    EditText usernameTxt, emailTxt, passwordTxt, passwordConfirmTxt;
    Button backBtn, registrationBtn;
    DatabaseController db;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        usernameTxt = findViewById(R.id.inpUserName);
        emailTxt = findViewById(R.id.inpEmail);
        passwordTxt = findViewById(R.id.inpPassword);
        passwordConfirmTxt = findViewById(R.id.inpPasswordAgain);
        registrationBtn = findViewById(R.id.btnRegistration);
        backBtn = findViewById(R.id.btnBack);
        db=new DatabaseController(this);

        registrationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userName = usernameTxt.getText().toString();
                String email = emailTxt.getText().toString();
                String pass = passwordTxt.getText().toString();
                String passConf = passwordConfirmTxt.getText().toString();
                if(userName.isEmpty() || email.isEmpty() || pass.isEmpty() || passConf.isEmpty()){
                    Toast.makeText(RegisterActivity.this, R.string.requiredfields, Toast.LENGTH_SHORT).show();
                } else if(pass.equals(passConf)){
                    if(!db.checkUserExists(email)){
                        if(db.register(new RegisterUserModel(userName, email, pass, passConf))){
                            Toast.makeText(RegisterActivity.this, R.string.registrationsuccess, Toast.LENGTH_SHORT).show();
                            Intent i = new Intent(RegisterActivity.this, LoginActivity.class);
                            startActivity(i);
                            finish();
                        } else {
                            Toast.makeText(RegisterActivity.this, R.string.registrationfail, Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(RegisterActivity.this, R.string.emailexists, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(RegisterActivity.this, R.string.unmatchingpasswords, Toast.LENGTH_SHORT).show();
                }
            }
        });


        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(i);
                finish();
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent i = new Intent(RegisterActivity.this, LoginActivity.class);
        startActivity(i);
        finish();
    }
}