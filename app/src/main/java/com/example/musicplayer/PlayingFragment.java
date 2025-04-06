package com.example.musicplayer;

import static android.content.Context.RECEIVER_NOT_EXPORTED;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;

import com.bumptech.glide.Glide;
import com.example.musicplayer.SongLibrary.Song;

import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

public class PlayingFragment extends Fragment {

    private MediaPlaybackService mediaPlaybackService;
    private TextView songTitle, songArtist, userName, email;
    private ImageButton playButton, skipForward, skipBackward, repeatButton, shuffleButton;
    private ImageView albumCover;
    private Button logout;

    private boolean isBound = false;

    private boolean isRepeat = false;
    private boolean isShuffle = false;

    private SeekBar seekBar;
    private Handler handler = new Handler();

    private Runnable seekBarUpdater;

    private BroadcastReceiver playPauseReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && intent.getAction() != null && intent.getAction().equals(MediaPlaybackService.ACTION_PLAY_PAUSE_CHANGED)) {
                boolean isPlaying = intent.getBooleanExtra("isPlaying", false);
                updatePlayButton(isPlaying);
            }
        }
    };

    private void updatePlayButton(boolean isPlaying) {
        if (isPlaying) {
            playButton.setBackgroundResource(R.drawable.media_pause);  // Pause button image
        } else {
            playButton.setBackgroundResource(R.drawable.media_play);  // Play button image
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d("asads", "onStart: start");

        startSeekBarUpdater();
        Intent intent = new Intent(getActivity(), MediaPlaybackService.class);
        getActivity().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    int newPosition = (int) (((float) progress / 100) * mediaPlaybackService.getSongDuration());

                    Intent intent = new Intent(getContext(), MediaPlaybackService.class);
                    intent.setAction("ACTION_SEEK_TO");
                    intent.putExtra("seek_position", newPosition);
                    getActivity().startService(intent);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //
            }
        });
        IntentFilter filter = new IntentFilter(MediaPlaybackService.ACTION_PLAY_PAUSE_CHANGED);
        getActivity().registerReceiver(playPauseReceiver, filter, RECEIVER_NOT_EXPORTED);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_playing, container, false);
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d("serviceConnection", "Service has successfully connected");
            MediaPlaybackService.LocalBinder binder = (MediaPlaybackService.LocalBinder) service;
            mediaPlaybackService = binder.getService();
            mediaPlaybackService.setOnSongChangedListener(new MediaPlaybackService.OnSongChangedListener() {

                @Override
                public void onSongChanged(Song song) {
                    Song currentSong = mediaPlaybackService.getCurrentSong();
                    if (currentSong != null) {
                        songTitle.setText(currentSong.getTitle());
                        songArtist.setText(currentSong.getArtist());

                        String albumArtUri = currentSong.getAlbumCover();
                        if (albumArtUri != null) {
                            Glide.with(getActivity())
                                    .load(albumArtUri)
                                    .error(R.drawable.no_album_art)
                                    .into(albumCover);
                        }
                    }

                    seekBar.setMax(100);
                    updateSeekBar();
                }
            });
            isBound = true;

            if (mediaPlaybackService.isPlaying()) {
                playButton.setBackgroundResource(R.drawable.media_pause);
            } else {
                playButton.setBackgroundResource(R.drawable.media_play);
            }

            Song currentSong = mediaPlaybackService.getCurrentSong();
            if (currentSong != null) {
                songTitle.setText(currentSong.getTitle());
                songArtist.setText(currentSong.getArtist());

                String albumArtUri = currentSong.getAlbumCover();
                if (albumArtUri != null) {
                    Glide.with(PlayingFragment.this)
                            .load(albumArtUri)
                            .error(R.drawable.no_album_art)
                            .into(albumCover);
                } else {

                albumCover.setImageResource(R.drawable.no_album_art);
                }
            }
            if (mediaPlaybackService.isPlaying()) {
                    playButton.setBackgroundResource(R.drawable.media_pause);
            } else {
                 playButton.setBackgroundResource(R.drawable.media_play);
            }
            isRepeat = mediaPlaybackService.getRepeat();
            isShuffle = mediaPlaybackService.getShuffle();

            if (isRepeat) {
                repeatButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#34C95C")));
            }

            if (isShuffle) {
                shuffleButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#34C95C")));
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d("asads", "onViewCreated: created");
        albumCover = view.findViewById(R.id.album_cover);
        songTitle = view.findViewById(R.id.song_title);
        songArtist = view.findViewById(R.id.song_album);
        playButton = view.findViewById(R.id.play_button);
        skipForward = view.findViewById(R.id.skip_forward);
        skipBackward = view.findViewById(R.id.skip_backward);
        repeatButton = view.findViewById(R.id.track_repeat);
        shuffleButton = view.findViewById(R.id.shuffle);
        seekBar = view.findViewById(R.id.song_seek_bar);
        email = view.findViewById(R.id.email);
        userName = view.findViewById(R.id.username);
        logout = view.findViewById(R.id.logout);

        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE);
        String emailText = sharedPreferences.getString("email", null);
        String userNameText = sharedPreferences.getString("userName", null);
        email.setText(emailText);
        userName.setText(userNameText);

        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mediaPlaybackService != null && isBound) {
                    Intent intent = new Intent(getActivity(), MediaPlaybackService.class);
                    intent.setAction("ACTION_PAUSE");
                    getActivity().startService(intent);

                    mediaPlaybackService.setOnSongChangedListener(null);
                    requireActivity().unbindService(serviceConnection);
                    isBound = false;
                }

                Intent stopIntent = new Intent(getActivity(), MediaPlaybackService.class);
                requireActivity().stopService(stopIntent);

                SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.clear();
                editor.apply();

                Intent intent = new Intent(getActivity(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);

                // 5. Finish current activity
                requireActivity().finish();
            }
        });

        if (email != null && userName != null) {
            Log.d("SharedPrefs", "Email: " + email);
            Log.d("SharedPrefs", "Username: " + userName);
        } else {
            Log.d("SharedPrefs", "No user data found");
        }

        ColorStateList inactiveStateList = ColorStateList.valueOf(Color.GRAY);
        repeatButton.setBackgroundTintList(inactiveStateList);
        shuffleButton.setBackgroundTintList(inactiveStateList);
        skipBackward.setBackgroundTintList(inactiveStateList);
        skipForward.setBackgroundTintList(inactiveStateList);
        playButton.setBackgroundTintList(inactiveStateList);

        Intent intent = new Intent(getActivity(), MediaPlaybackService.class);
        getActivity().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), MediaPlaybackService.class);
                if (mediaPlaybackService.isPlaying()) {
                    intent.setAction("ACTION_PAUSE");
                    playButton.setBackgroundResource(R.drawable.media_play);
                } else {
                    intent.setAction("ACTION_PLAY");
                    playButton.setBackgroundResource(R.drawable.media_pause);
                }
                getActivity().startService(intent);
            }
        });

        skipForward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mediaPlaybackService.getCurrentSong()!=null) {
                    Intent intent = new Intent(getActivity(), MediaPlaybackService.class);
                    intent.setAction("ACTION_SKIP_TO_NEXT");
                    getActivity().startService(intent);
                }
            }
        });

        skipBackward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mediaPlaybackService.getCurrentSong()!=null){
                    Intent intent = new Intent(getActivity(), MediaPlaybackService.class);
                    intent.setAction("ACTION_SKIP_TO_PREVIOUS");
                    getActivity().startService(intent);
                }
            }
        });

        repeatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isBound) {
                    isRepeat = !isRepeat;
                    mediaPlaybackService.setRepeat(isRepeat);
                    if (isRepeat) {
                        ColorStateList colorStateList = ColorStateList.valueOf(Color.parseColor("#34C95C"));
                        repeatButton.setBackgroundTintList(colorStateList);
                    } else {
                        ColorStateList inactiveStateList = ColorStateList.valueOf(Color.GRAY);
                        repeatButton.setBackgroundTintList(inactiveStateList);
                    }
                }
            }
        });

        shuffleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isBound && mediaPlaybackService.getCurrentSong()!=null) {
                    isShuffle = !isShuffle;
                    if (isShuffle) {
                        ColorStateList colorStateList = ColorStateList.valueOf(Color.parseColor("#34C95C"));
                        shuffleButton.setBackgroundTintList(colorStateList);
                    } else {
                        ColorStateList inactiveStateList = ColorStateList.valueOf(Color.GRAY);
                        shuffleButton.setBackgroundTintList(inactiveStateList);
                    }
                }
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        stopSeekBarUpdater();
        if (isBound) {
            mediaPlaybackService.setOnSongChangedListener(null);
            getActivity().unbindService(serviceConnection);
            isBound = false;
        }
        getActivity().unregisterReceiver(playPauseReceiver);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (isBound) {
            getActivity().unbindService(serviceConnection);
            isBound = false;
        }
    }

    private void updateSeekBar() {
        if (mediaPlaybackService != null) {
            int progress = (int) (((float) mediaPlaybackService.getCurrentPosition() / mediaPlaybackService.getSongDuration()) * 100);
            seekBar.setProgress(progress);
            if (!mediaPlaybackService.isPlaying()) {
                playButton.setBackgroundResource(R.drawable.media_play);
            } else {
                playButton.setBackgroundResource(R.drawable.media_pause);
            }
            if (mediaPlaybackService.isPlaying()) {
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        updateSeekBar();
                    }
                };
                handler.postDelayed(runnable, 1000);
            }
        }
    }

    private void startSeekBarUpdater() {
        stopSeekBarUpdater();
        seekBarUpdater = new Runnable() {
            @Override
            public void run() {
                updateSeekBar();
                handler.postDelayed(this, 1000);
            }
        };
        handler.postDelayed(seekBarUpdater, 1000);
    }

    private void stopSeekBarUpdater() {
        if (seekBarUpdater != null) {
            handler.removeCallbacks(seekBarUpdater);
            seekBarUpdater = null;
        }
    }

}