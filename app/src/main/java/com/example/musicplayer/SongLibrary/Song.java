package com.example.musicplayer.SongLibrary;

import android.net.Uri;
public class Song {
    private Uri songUri;
    private String albumCover;
    private String title;
    private String artist;

    // Constructor
    public Song(String albumCover, String title, String artist, Uri songUri) {
        this.albumCover = albumCover;
        this.title = title;
        this.artist = artist;
        this.songUri = songUri;
    }

    // Getter methods
    public String getAlbumCover() {
        return albumCover;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    // Setter methods
    public Uri getSongUri() {
        return songUri;
    }
}