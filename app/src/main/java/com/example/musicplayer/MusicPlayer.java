package com.example.musicplayer;

import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import com.example.musicplayer.databinding.ActivityMusicPlayerBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MusicPlayer extends AppCompatActivity {
    private ActivityMusicPlayerBinding binding;
    private ActivityResultLauncher<String[]> requestPermissionLauncher;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMusicPlayerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Prompt for runtime permissions (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher =
                    registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), isGranted -> {
                        if (!isGranted.containsValue(false)) {
                            Toast.makeText(MusicPlayer.this, R.string.permissionError, Toast.LENGTH_LONG).show();
                        }else {
                        }
                    });

            requestPermissionLauncher.launch(new String[] {
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.READ_MEDIA_AUDIO,
                    android.Manifest.permission.READ_MEDIA_VIDEO,
                    android.Manifest.permission.POST_NOTIFICATIONS
            });
        } else {
            // Prompt for READ_EXTERNAL_STORAGE permission (Android 9-12)
            requestPermissionLauncher =
                    registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), isGranted -> {
                        if (!isGranted.getOrDefault(android.Manifest.permission.READ_EXTERNAL_STORAGE, false)) {
                            Toast.makeText(MusicPlayer.this, R.string.permissionError, Toast.LENGTH_LONG).show();
                        }

                    });

            requestPermissionLauncher.launch(new String[] {
                    Manifest.permission.READ_EXTERNAL_STORAGE
            });
        }
    }
    @Override
    protected void onResume() {
        super.onResume();

        BottomNavigationView navView = findViewById(R.id.nav_view);
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupWithNavController(binding.navView, navController);
    }
}