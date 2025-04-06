package com.example.musicplayer;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.provider.MediaStore;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.os.Binder;
import android.util.Log;

import androidx.core.content.ContextCompat;
import androidx.media.MediaBrowserServiceCompat;

import com.example.musicplayer.SongLibrary.Song;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MediaPlaybackService extends MediaBrowserServiceCompat {
    //Songs
    private List<Song> songList;
    private int currentSongIndex;

    //MediaPlayer
    private MediaSessionCompat mediaSession;
    private MediaPlayer mediaPlayer;
    private MediaNotificationManager mediaNotificationManager;

    //CurrentSong
    private String currentTitle;
    private String currentArtist;
    private Bitmap currentAlbumArt;
    private Song currentSong;
    private static final int NOTIFICATION_ID = 1;
    private OnSongChangedListener onSongChangedListener;

    //Repeat and shuffle
    private boolean isRepeat = false;
    private boolean isShuffle = false;
    boolean isSwitchingSongs = false;
    private List<Integer> shuffledIndices;
    private int currentIndex = 0;
    private boolean userHasSkipped = false;

    // SharedPreferences fields
    private static final String PREFS_NAME = "songPref";
    private static final String PREF_CURRENT_SONG_INDEX = "currentSongIndex";
    private static final String PREF_SONG_POSITION = "songPosition";
    private static final Handler handler = new Handler();
    public static final String ACTION_PLAY_PAUSE_CHANGED = "com.example.musicplayer.PLAY_PAUSE_CHANGED";

    private void sendPlayPauseState() {
        Intent intent = new Intent(ACTION_PLAY_PAUSE_CHANGED);
        intent.putExtra("isPlaying", isPlaying());  // Assuming isPlaying() is a method that checks if the music is playing
        sendBroadcast(intent);
    }

    public interface OnSongChangedListener {
        void onSongChanged(Song song);
    }

    private void saveSongInfo() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(PREF_CURRENT_SONG_INDEX, currentSongIndex);
        editor.putInt(PREF_SONG_POSITION, mediaPlayer.getCurrentPosition());
        editor.apply();
    }

    // Binding MediaPlaybackService
    public class LocalBinder extends Binder {
        public MediaPlaybackService getService() {
            return MediaPlaybackService.this;
        }
    }

    public void setOnSongChangedListener(OnSongChangedListener listener) {
        this.onSongChangedListener = listener;
    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    public void setRepeat(boolean isRepeat) {
        this.isRepeat = isRepeat;
    }

    public boolean getRepeat(){
        return this.isRepeat;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new LocalBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        PendingIntent mediaSessionIntent = PendingIntent.getService(this, 0, new Intent(this, MediaPlaybackService.class), PendingIntent.FLAG_IMMUTABLE);

        mediaSession = new MediaSessionCompat(this, "MusicPlayer", null, mediaSessionIntent);
        mediaPlayer = new MediaPlayer();

        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onCommand(String command, Bundle extras, ResultReceiver cb) {
                switch (command) {
                    case "ACTION_PLAY":
                        onPlay();
                        break;
                    case "ACTION_PAUSE":
                        onPause();
                        break;
                    case "ACTION_SKIP_TO_NEXT":
                        onSkipToNext();
                        break;
                    case "ACTION_SKIP_TO_PREVIOUS":
                        onSkipToPrevious();
                        break;
                    case "ACTION_STOP":
                        onStop();
                        break;
                    case "ACTION_SEEK_TO":
                        int seekPosition = extras.getInt("seek_position", 0);
                        onSeekTo(seekPosition);
                        break;
                    default:
                        super.onCommand(command, extras, cb);
                }
            }

            @Override
            public void onPlay() {
                super.onPlay();
                mediaPlayer.start();
                mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                        .setState(PlaybackStateCompat.STATE_PLAYING, mediaPlayer.getCurrentPosition(), 1)
                        .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS | PlaybackStateCompat.ACTION_STOP | PlaybackStateCompat.ACTION_SEEK_TO)
                        .build());
                mediaNotificationManager.updateNotification(currentTitle, currentArtist, currentAlbumArt, true, mediaPlayer.getDuration(), mediaPlayer.getCurrentPosition());
                updateNotificationSeekBar();
                sendPlayPauseState();
            }

            @Override
            public void onPause() {
                super.onPause();
                mediaPlayer.pause();
                saveSongInfo();
                mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                        .setState(PlaybackStateCompat.STATE_PAUSED, mediaPlayer.getCurrentPosition(), 1)
                        .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS | PlaybackStateCompat.ACTION_STOP)
                        .build());
                mediaNotificationManager.updateNotification(currentTitle, currentArtist, currentAlbumArt, false, mediaPlayer.getDuration(), mediaPlayer.getCurrentPosition());
                sendPlayPauseState();
            }

            @Override
            public void onStop() {
                super.onStop();
                mediaPlayer.stop();
                saveSongInfo();
                mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                        .setState(PlaybackStateCompat.STATE_STOPPED, mediaPlayer.getCurrentPosition(), 1)
                        .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
                        .build());
                stopForeground(true);
            }

            @Override
            public void onSkipToNext() {
                super.onSkipToNext();
                userHasSkipped = true;
                if (isRepeat) {
                    playSong(songList.get(currentSongIndex));
                } else {
                    if (isShuffle) {
                        if (!shuffledIndices.isEmpty()) {
                            currentIndex = (currentIndex + 1) % shuffledIndices.size();
                            currentSongIndex = shuffledIndices.get(currentIndex);
                            playSong(songList.get(currentSongIndex));
                        } else {
                            currentSongIndex = 0;
                            playSong(songList.get(currentSongIndex));
                        }
                    } else {
                        if (currentSongIndex < songList.size() - 1) {
                            currentSongIndex++;
                        } else {
                            currentSongIndex = 0;
                        }
                        playSong(songList.get(currentSongIndex));
                    }
                }
            }

            @Override
            public void onSkipToPrevious() {
                super.onSkipToPrevious();
                userHasSkipped = true;
                if (isRepeat) {
                    playSong(songList.get(currentSongIndex));
                } else {
                    if (isShuffle) {
                        currentIndex = (currentIndex - 1 + shuffledIndices.size()) % shuffledIndices.size();
                        currentSongIndex = shuffledIndices.get(currentIndex);
                    } else {
                        if (currentSongIndex > 0) {
                            currentSongIndex--;
                        } else {
                            currentSongIndex = songList.size() - 1;
                        }
                    }
                    playSong(songList.get(currentSongIndex));
                }
            }

            @Override
            public void onSeekTo(long pos) {
                super.onSeekTo(pos);
                if (mediaPlayer != null) {
                    mediaPlayer.seekTo((int)pos);
                    mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                            .setState(PlaybackStateCompat.STATE_PLAYING, mediaPlayer.getCurrentPosition(), 1)
                            .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS | PlaybackStateCompat.ACTION_STOP | PlaybackStateCompat.ACTION_SEEK_TO)
                            .build());
                    mediaNotificationManager.updateNotification(currentTitle, currentArtist, currentAlbumArt, isPlaying(), mediaPlayer.getDuration(), mediaPlayer.getCurrentPosition());
                }
            }
        });

        mediaSession.setActive(true);
        mediaNotificationManager = new MediaNotificationManager(this, mediaSession);

        setSessionToken(mediaSession.getSessionToken());

        // Shared preferences song check
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (prefs.contains(PREF_CURRENT_SONG_INDEX) && prefs.contains(PREF_SONG_POSITION)) {
            currentSongIndex = prefs.getInt(PREF_CURRENT_SONG_INDEX, 0);
            int songPosition = prefs.getInt(PREF_SONG_POSITION, 0);

            if (songList != null && !songList.isEmpty()) {
                playSong(songList.get(currentSongIndex));
                mediaPlayer.seekTo(songPosition);
            }
        }
    }

    // Helper functions for MediaBrowserService and MediaPlaybackService
    @Override
    public BrowserRoot onGetRoot(String clientPackageName, int clientUid, Bundle rootHints) {
        return new BrowserRoot("root", null);
    }

    @Override
    public void onLoadChildren(final String parentMediaId, final Result<List<MediaBrowserCompat.MediaItem>> result) {
        result.sendResult(new ArrayList<>());
    }
    public void playSong(Song song) {
        currentTitle = song.getTitle();
        currentArtist = song.getArtist();
        currentAlbumArt = getAlbumArt(song.getAlbumCover());
        if(currentAlbumArt == null){
            Drawable drawable = ContextCompat.getDrawable(this, R.drawable.playlogo);
            this.currentAlbumArt = ((BitmapDrawable) drawable).getBitmap();

        }
        currentSong = song;
        try {
            isSwitchingSongs = true;
            mediaPlayer.reset();
            mediaPlayer.setDataSource(this, song.getSongUri());
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.start();
                    mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                            .setState(PlaybackStateCompat.STATE_PLAYING, mediaPlayer.getCurrentPosition(), 1)
                            .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS | PlaybackStateCompat.ACTION_STOP | PlaybackStateCompat.ACTION_SEEK_TO)
                            .build());
                    Notification notification = mediaNotificationManager.showNotification(currentTitle, currentArtist, currentAlbumArt, mp.isPlaying(), mediaPlayer.getDuration(),mediaPlayer.getCurrentPosition());
                    startForeground(NOTIFICATION_ID, notification);
                    isSwitchingSongs = false;
                }
            });
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    if (!isSwitchingSongs && !userHasSkipped) {
                    if (isRepeat) {
                            playSong(song);
                            } else {
                                if (isShuffle) {
                                    currentIndex = (currentIndex + 1) % shuffledIndices.size();
                                    playSong(songList.get(shuffledIndices.get(currentIndex)));
                                } else {
                                if (currentSongIndex < songList.size() - 1) {
                                    currentSongIndex++;
                                    playSong(songList.get(currentSongIndex));
                                    updateNotificationSeekBar();
                                } else {
                                    mediaPlayer.stop();
                                }
                            }
                        }
                    }
                    userHasSkipped = false;
                }
            });
            if (onSongChangedListener != null) {
                onSongChangedListener.onSongChanged(song);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void playSongs(List<Song> songs, int startIndex) {
        songList = songs;
        currentSongIndex = startIndex;
        playSong(songList.get(currentSongIndex));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        mediaSession.setActive(false);
        mediaSession.release();
    }

    // Media action handler
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case "ACTION_PLAY":
                        mediaSession.getController().getTransportControls().play();
                        break;
                    case "ACTION_PAUSE":
                        mediaSession.getController().getTransportControls().pause();
                        break;
                    case "ACTION_SKIP_TO_NEXT":
                        mediaSession.getController().getTransportControls().skipToNext();
                        break;
                    case "ACTION_SKIP_TO_PREVIOUS":
                        mediaSession.getController().getTransportControls().skipToPrevious();
                        break;
                    case "ACTION_STOP":
                        mediaSession.getController().getTransportControls().stop();
                        break;
                    case "ACTION_SEEK_TO":
                        int seekPosition = intent.getIntExtra("seek_position", 0);
                        if (mediaPlayer != null) {
                            mediaPlayer.seekTo(seekPosition);
                        }
                        mediaNotificationManager.updateNotification(currentTitle, currentArtist, currentAlbumArt, isPlaying(), mediaPlayer.getDuration(), mediaPlayer.getCurrentPosition());
                        break;
                }
            }
        }
        return START_NOT_STICKY;
    }

    public Bitmap getAlbumArt(String albumArtUri) {
        Bitmap bitmap = null;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), Uri.parse(albumArtUri));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    public Song getCurrentSong() {
        return currentSong;
    }

    public void setShuffle(boolean isShuffle) {
        this.isShuffle = isShuffle;
        if (isShuffle) {
            shuffleIndices();
        } else {
            currentIndex = shuffledIndices.indexOf(currentSongIndex);
        }
    }

    public boolean getShuffle(){
        return this.isShuffle;
    }

    private void shuffleIndices() {
        shuffledIndices = new ArrayList<>();
        for (int i = 0; i < songList.size(); i++) {
            shuffledIndices.add(i);
        }
        Collections.shuffle(shuffledIndices);
        currentIndex = shuffledIndices.indexOf(currentSongIndex);
    }

    public int getSongDuration() {
        if (mediaPlayer != null) {
            return mediaPlayer.getDuration();
        } else {
            return 0;
        }
    }

    public int getCurrentPosition() {
        if (mediaPlayer != null) {
            return mediaPlayer.getCurrentPosition();
        } else {
            return 0;
        }
    }

    private void updateNotificationSeekBar() {
        if(this.isPlaying()) {
            mediaNotificationManager.updateNotification(currentTitle, currentArtist, currentAlbumArt, this.isPlaying(), mediaPlayer.getDuration(), mediaPlayer.getCurrentPosition());
            if (this.isPlaying()) {
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        updateNotificationSeekBar();
                    }
                };
                handler.postDelayed(runnable, 1000);
            }
        }
    }

}
