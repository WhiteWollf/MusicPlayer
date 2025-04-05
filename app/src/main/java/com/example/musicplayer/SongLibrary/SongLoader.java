package com.example.musicplayer.SongLibrary;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class SongLoader {

    public static List<Song> loadSongs(Context context) {
        List<Song> songs = new ArrayList<>();
        ContentResolver contentResolver = context.getContentResolver();
        Uri songUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor songCursor = contentResolver.query(songUri, null, null, null, null);

        if (songCursor != null && songCursor.moveToFirst()) {
            int songTitle = songCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int songArtist = songCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int songAlbumId = songCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);

            // Construct a Song object for each file returned
            do {
                String currentTitle = songCursor.getString(songTitle);
                String currentArtist = songCursor.getString(songArtist);
                long currentAlbumId = songCursor.getLong(songAlbumId);

                @SuppressLint("Range") long id = songCursor.getLong(songCursor.getColumnIndex(MediaStore.Audio.Media._ID));
                Uri contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);


                // Query the URI for the AlbumArt (Obtaining the real file path has been restricted after Android 10.0)
                String currentAlbumArt = String.valueOf(ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"),currentAlbumId));
                songs.add(new Song(currentAlbumArt, currentTitle, currentArtist, contentUri));
            } while (songCursor.moveToNext());
        }
        // Close songCursor when reaching the end of list
        if (songCursor != null) {
            songCursor.close();
        }
        return songs;
    }
}
