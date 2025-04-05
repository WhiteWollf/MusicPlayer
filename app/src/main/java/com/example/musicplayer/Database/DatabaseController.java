package com.example.musicplayer.Database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import com.example.musicplayer.Models.LoginUserModel;
import com.example.musicplayer.Models.RegisterUserModel;
import com.example.musicplayer.Models.UserModel;

import at.favre.lib.crypto.bcrypt.BCrypt;


public class DatabaseController extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "Musicplayer.db";

    public DatabaseController(@Nullable Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS users(user_name TEXT, email TEXT PRIMARY KEY, password TEXT)");

        insertDefaultUser(db);
    }

    private void insertDefaultUser(SQLiteDatabase db) {
        // Check manually if user already exists using rawQuery on the passed `db`
        Cursor cursor = db.rawQuery("SELECT * FROM users WHERE email = ?", new String[]{"user@example.com"});
        boolean exists = false;

        if (cursor != null) {
            exists = cursor.getCount() > 0;
            cursor.close();
        }

        if (!exists) {
            String userPassword = BCrypt.withDefaults().hashToString(12, "user".toCharArray());
            ContentValues values = new ContentValues();
            values.put("user_name", "UserName");
            values.put("email", "user@example.com");
            values.put("password", userPassword);
            db.insert("users", null, values);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        db.execSQL("DROP TABLE IF EXISTS users");
    }


    //Account database manipulation
    public Boolean register(RegisterUserModel user) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("user_name", user.getUserName());
        values.put("email", user.getEmail());
        String hashedPass = BCrypt.withDefaults().hashToString(12, user.getPassword().toCharArray());
        values.put("password", hashedPass);
        if(!user.getPassword().equals(user.getPasswordConfirm()))
        {
            return false;
        }
        try {
            long result = db.insert("users", null, values);
            return result != -1;
        } catch (Exception e) {
            return false;
        }

    }

    /*
    public boolean addUser(UserModel user){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("user_name", user.getUserName());;
        values.put("email", user.getEmail());
        values.put("password", user.getPassword());
        try {
            long result = db.insert("users", null, values);
            return result != -1;
        } catch (Exception e) {
            return false;
        }
    }
    */
    public boolean login(LoginUserModel user) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT * FROM users WHERE email = ?", new String[]{user.getEmail()});
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                int passwordColumnIndex = cursor.getColumnIndex("password");
                String hashedPassword = cursor.getString(passwordColumnIndex);
                return BCrypt.verifyer().verify(user.getPassword().toCharArray(), hashedPassword).verified;
            }
            return false;
        } finally {
            cursor.close();
        }

    }

    public Boolean checkUserExists(String email) {
        SQLiteDatabase db = this.getWritableDatabase();
        try (Cursor cursor = db.rawQuery("SELECT * FROM users WHERE email = ?", new String[]{email})) {

            if (cursor.getCount() > 0) {
                return true;
            }
        }
        return false;
    }

    public UserModel getUserInfo(String email) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = null;
        UserModel u = new UserModel();
        try {
            cursor = db.rawQuery("SELECT * FROM users WHERE email = ?", new String[]{email});
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                u.setEmail(email);
                u.setUserName(cursor.getString((int) cursor.getColumnIndex("user_name")));

                return u;
            }
            return u;
        } finally {
            cursor.close();
        }
    }

    public boolean updateUserName(String email, String userName) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("user_name", userName);
        int result = db.update("users", values, "email" + "=?", new String[]{email});
        db.close();
        return result != -1;
    }

}
