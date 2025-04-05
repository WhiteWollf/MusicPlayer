package com.example.musicplayer.SongLibrary;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.musicplayer.MediaPlaybackService;

import com.example.musicplayer.R;
import com.example.musicplayer.databinding.FragmentSongListBinding;

import java.util.List;

/**
 * A fragment representing a list of Items.
 */
public class SongFragment extends Fragment {

    private FragmentSongListBinding binding;
    private RecyclerView recyclerView;
    private SongAdapter songAdapter;
    private MediaPlaybackService mediaPlaybackService;
    private boolean isBound = false;
    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public SongFragment() {
    }

    public void loadSongs() {
        final List<Song> songs = SongLoader.loadSongs(requireActivity());
        songAdapter = new SongAdapter(songs, new SongAdapter.OnSongClickListener() {
            @Override
            public void onSongClick(Song song) {
                if (isBound) {
                    int songIndex = songs.indexOf(song);
                    mediaPlaybackService.playSongs(songs, songIndex);
                }
                NavController navController = NavHostFragment.findNavController(SongFragment.this);
                navController.navigate(
                        R.id.navigation_playing,
                        null,
                        new NavOptions.Builder()
                                .setPopUpTo(R.id.navigation_library, true)
                                .build()
                );            }
        });
        recyclerView.setAdapter(songAdapter);
    }


    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d("serviceConnection", "Service has successfully connected");
            MediaPlaybackService.LocalBinder binder = (MediaPlaybackService.LocalBinder) service;
            mediaPlaybackService = binder.getService();
            isBound = true;
            loadSongs();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentSongListBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        recyclerView = binding.songRecyclerView;
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        Intent intent = new Intent(getActivity(), MediaPlaybackService.class);
        getActivity().startService(intent); // Start the MediaPlaybackService
        getActivity().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (isBound) {
            getActivity().unbindService(serviceConnection);
            isBound = false;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}