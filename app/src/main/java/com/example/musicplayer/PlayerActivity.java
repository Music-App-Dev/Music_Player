package com.example.musicplayer;

import static com.example.musicplayer.AlbumDetailsAdapter.albumFiles;
import static com.example.musicplayer.MainActivity.repeatBoolean;
import static com.example.musicplayer.MainActivity.shuffleBoolean;
import static com.example.musicplayer.MusicAdapter.mFiles;
import static com.spotify.sdk.android.auth.AccountsQueryParameters.CLIENT_ID;
import static com.spotify.sdk.android.auth.AccountsQueryParameters.REDIRECT_URI;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import androidx.core.content.ContextCompat;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;

import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;

import java.util.ArrayList;
import java.util.Random;

public class PlayerActivity extends AppCompatActivity
        implements ActionPlaying, ServiceConnection {

    private TextView song_name, artist_name, duration_played, duration_total;
    private ImageView cover_art, nextBtn, prevBtn, backBtn, shuffleBtn, repeatBtn;
    private FloatingActionButton playPauseBtn;
    private SeekBar seekBar;
    public static int position = -1;
    public static ArrayList<SpotifyTrack> listSongs = new ArrayList<>();

    private Handler handler = new Handler();
    private MusicService musicService;
    private SpotifyAppRemote spotifyAppRemote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        initViews();
        getIntentMethod();

        // Retrieve the access token from SharedPreferences
        String accessToken = getAccessToken();
        if (accessToken != null) {
            connectSpotifyRemote();
        } else {
            // If no token, redirect to MainActivity for re-authentication
            Toast.makeText(this, "Please authenticate with Spotify.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }



    private String getAccessToken() {
        SharedPreferences sharedPreferences = getSharedPreferences("SpotifyAuth", MODE_PRIVATE);
        return sharedPreferences.getString("access_token", null);
    }

    private void connectSpotifyRemote() {
        ConnectionParams connectionParams = new ConnectionParams.Builder(CLIENT_ID)
                .setRedirectUri(REDIRECT_URI)
                .showAuthView(true)
                .build();

        SpotifyAppRemote.connect(this, connectionParams, new Connector.ConnectionListener() {
            @Override
            public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                PlayerActivity.this.spotifyAppRemote = spotifyAppRemote;
                Log.d("Spotify", "Connected successfully");
                testPlayRandomSong(); // Start playing a random song
            }

            @Override
            public void onFailure(Throwable throwable) {
                Log.e("Spotify", "Connection failed: " + throwable.getMessage());
                if (throwable.getMessage().contains("AUTHENTICATION_FAILED")) {
                    Toast.makeText(PlayerActivity.this, "Authentication failed. Please log in again.", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(PlayerActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(PlayerActivity.this, "Failed to connect to Spotify. Check your connection.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            musicService.showNotification(R.drawable.ic_pause);
        } else {
            Toast.makeText(this, "Notification permission is required to show notifications.", Toast.LENGTH_SHORT).show();
        }
    }

    private void setFullScreen() {
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, this, BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (spotifyAppRemote != null) {
            spotifyAppRemote.getPlayerApi().getPlayerState().setResultCallback(playerState -> {
                SharedPreferences.Editor editor = getSharedPreferences("musicPlayerPrefs", MODE_PRIVATE).edit();
                editor.putInt("savedPosition", (int) playerState.playbackPosition);
                editor.putInt("currentSong", position); // Save the current song position
                editor.apply();
            });
        }
        unbindService(this);
    }

    private void setupListeners() {
        shuffleBtn.setOnClickListener(v -> {
            shuffleBoolean = !shuffleBoolean;
            shuffleBtn.setImageResource(shuffleBoolean ? R.drawable.ic_shuffle_on : R.drawable.ic_shuffle_off);
        });

        repeatBtn.setOnClickListener(v -> {
            repeatBoolean = !repeatBoolean;
            repeatBtn.setImageResource(repeatBoolean ? R.drawable.ic_repeat_on : R.drawable.ic_repeat_off);
        });

        backBtn.setOnClickListener(v -> finish());

        playPauseBtn.setOnClickListener(v -> playPauseBtnClicked());

        nextBtn.setOnClickListener(v -> nextBtnClicked());
        prevBtn.setOnClickListener(v -> prevBtnClicked());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (spotifyAppRemote != null && fromUser) {
                    spotifyAppRemote.getPlayerApi().seekTo(progress * 1000);
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private final Runnable updateSeekBarRunnable = new Runnable() {
        @Override
        public void run() {
            if (spotifyAppRemote != null) {
                spotifyAppRemote.getPlayerApi().subscribeToPlayerState().setEventCallback(playerState -> {
                    int currentPosition = (int) (playerState.playbackPosition / 1000); // in seconds
                    seekBar.setProgress(currentPosition);
                    duration_played.setText(formattedTime(currentPosition));
                    handler.postDelayed(updateSeekBarRunnable, 1000);
                });
            }
        }
    };

    public void playPauseBtnClicked() {
        if (spotifyAppRemote != null) {
            spotifyAppRemote.getPlayerApi().getPlayerState().setResultCallback(playerState -> {
                if (playerState.isPaused) {
                    playPauseBtn.setImageResource(R.drawable.ic_pause);
                    musicService.showNotification(R.drawable.ic_pause);
                    spotifyAppRemote.getPlayerApi().resume();
                } else {
                    playPauseBtn.setImageResource(R.drawable.ic_play);
                    musicService.showNotification(R.drawable.ic_play);
                    spotifyAppRemote.getPlayerApi().pause();
                }
            });
        }
    }

    private void changeTrack(boolean isNext) {
        if (spotifyAppRemote != null) {
            if (shuffleBoolean && !repeatBoolean) {
                position = getRandom(listSongs.size() - 1);
            } else if (!shuffleBoolean && !repeatBoolean) {
                position = isNext ? (position + 1) % listSongs.size() : (position - 1 < 0 ? listSongs.size() - 1 : position - 1);
            }
            SpotifyTrack selectedTrack = listSongs.get(position);
            String spotifyUri = selectedTrack.getTrackId();

            spotifyAppRemote.getPlayerApi().play(spotifyUri).setResultCallback(empty -> {
                song_name.setText(selectedTrack.getTrackName());
                artist_name.setText(selectedTrack.getArtistName());
                playPauseBtn.setImageResource(R.drawable.ic_pause);
                musicService.showNotification(R.drawable.ic_pause);

                spotifyAppRemote.getPlayerApi().subscribeToPlayerState().setEventCallback(playerState -> {
                    if (playerState.track != null) {
                        int duration = (int) (playerState.track.duration / 1000);
                        seekBar.setMax(duration);
                        seekBar.setProgress((int) (playerState.playbackPosition / 1000));
                    }
                });
            }).setErrorCallback(error -> Log.e("MusicService", "Error playing track: " + error.getMessage()));
        }
    }

    public void nextBtnClicked() {
        changeTrack(true);
    }

    public void prevBtnClicked() {
        changeTrack(false);
    }

    private int getRandom(int i) {
        return new Random().nextInt(i + 1);
    }

    private String formattedTime(int currentTime) {
        String minutes = String.valueOf(currentTime / 60);
        String seconds = String.valueOf(currentTime % 60);
        return seconds.length() == 1 ? minutes + ":0" + seconds : minutes + ":" + seconds;
    }

    private void testPlayRandomSong() {
        if (spotifyAppRemote != null && listSongs != null && !listSongs.isEmpty()) {
            // Pick a random track from the list
            int randomIndex = new Random().nextInt(listSongs.size());
            SpotifyTrack randomTrack = listSongs.get(1);
            String spotifyUri = randomTrack.getTrackId();

            // Log to check which song is playing
            Log.d("TestPlay", "Attempting to play: " + randomTrack.getTrackName());

            // Play the random track
            spotifyAppRemote.getPlayerApi().play(spotifyUri).setResultCallback(empty -> {
                // Update UI with the current track details
                updateUIWithCurrentTrack(randomTrack);
                playPauseBtn.setImageResource(R.drawable.ic_pause);
                Log.d("TestPlay", "Playing: " + randomTrack.getTrackName());
            }).setErrorCallback(error -> {
                Log.e("TestPlay", "Error playing track: " + error.getMessage());
                Toast.makeText(this, "Error playing track: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            });
        } else {
            Toast.makeText(this, "Spotify Remote is not connected or song list is empty.", Toast.LENGTH_SHORT).show();
            Log.e("TestPlay", "Spotify Remote not connected or listSongs is empty.");
        }
    }
    private void getIntentMethod() {
        Intent intent = getIntent();
        position = intent.getIntExtra("position", -1);
        listSongs = intent.getParcelableArrayListExtra("trackList");

        if (listSongs != null && !listSongs.isEmpty() && position >= 0 && position < listSongs.size()) {
            playPauseBtn.setImageResource(R.drawable.ic_pause);
        } else {
            Log.e("PlayerActivity", "Invalid song position or empty list.");
            Toast.makeText(this, "No song found to play.", Toast.LENGTH_SHORT).show();
            finish();
        }

        Intent serviceIntent = new Intent(this, MusicService.class);
        serviceIntent.putExtra("servicePosition", position);
        startService(serviceIntent);
    }

    private void initViews() {
        song_name = findViewById(R.id.song_name);
        artist_name = findViewById(R.id.song_artist);
        duration_played = findViewById(R.id.durationPlayed);
        duration_total = findViewById(R.id.durationTotal);
        cover_art = findViewById(R.id.cover_art);
        nextBtn = findViewById(R.id.id_next);
        prevBtn = findViewById(R.id.id_prev);
        backBtn = findViewById(R.id.back_btn);
        shuffleBtn = findViewById(R.id.id_shuffle);
        repeatBtn = findViewById(R.id.id_repeat);
        playPauseBtn = findViewById(R.id.play_pause);
        seekBar = findViewById(R.id.seekbar);
    }

    private void metaData() {
        SpotifyTrack track = listSongs.get(position);
        String albumImageUrl = track.getAlbumImageUrl();
        int durationTotal = Integer.parseInt(track.getDuration()) / 1000;
        duration_total.setText(formattedTime(durationTotal));

        if (albumImageUrl != null) {
            Glide.with(this).load(albumImageUrl).into(cover_art);
        } else {
            cover_art.setImageResource(R.drawable.gradient_bg);
        }
    }

    public void ImageAnimation(Context context, ImageView imageView, Bitmap bitmap) {
        Animation animOut = AnimationUtils.loadAnimation(context, android.R.anim.fade_out);
        Animation animIn = AnimationUtils.loadAnimation(context, android.R.anim.fade_in);
        animOut.setAnimationListener(new Animation.AnimationListener() {
            @Override public void onAnimationEnd(Animation animation) {
                imageView.setImageBitmap(bitmap);
                imageView.startAnimation(animIn);
            }
            @Override public void onAnimationStart(Animation animation) {}
            @Override public void onAnimationRepeat(Animation animation) {}
        });
        imageView.startAnimation(animOut);
    }

    public void onServiceConnected(ComponentName componentName, IBinder service) {
        MusicService.MyBinder myBinder = (MusicService.MyBinder) service;
        musicService = myBinder.getService();
        musicService.setCallBack(this);

        // Get saved playback position and song data
        SharedPreferences sharedPreferences = getSharedPreferences("musicPlayerPrefs", MODE_PRIVATE);
        int savedPosition = sharedPreferences.getInt("savedPosition", -1);
        int savedSong = sharedPreferences.getInt("currentSong", -1);

        // Ensure we have a valid list of songs and position
        if (listSongs != null && !listSongs.isEmpty() && position >= 0 && position < listSongs.size()) {
            SpotifyTrack currentTrack = listSongs.get(position);

            // Set saved playback position if available and matches current song
            if (savedPosition != -1 && savedSong == position) {
                musicService.seekTo(savedPosition); // Use musicService to control playback
            }

            // Show notification and setup UI updates
            musicService.showNotification(R.drawable.ic_pause);
            updateUIWithCurrentTrack(currentTrack);

            // Listen to playback state updates via musicService
            musicService.subscribeToPlayerStateUpdates(playerState -> {
                if (playerState.track != null) {
                    int duration = (int) (playerState.track.duration / 1000); // Duration in seconds
                    seekBar.setMax(duration);
                    seekBar.setProgress((int) (playerState.playbackPosition / 1000)); // Current position in seconds
                    duration_played.setText(formattedTime((int) (playerState.playbackPosition / 1000)));
                    duration_total.setText(formattedTime(duration));
                }
            });
        } else {
            Log.e("PlayerActivity", "Invalid song position or empty list.");
            Toast.makeText(this, "No song to play", Toast.LENGTH_SHORT).show();
        }
    }

    // Helper method to update the UI with the current track's metadata
    private void updateUIWithCurrentTrack(SpotifyTrack currentTrack) {
        song_name.setText(currentTrack.getTrackName());
        artist_name.setText(currentTrack.getArtistName());
        metaData(); // Load cover art and other track-specific info
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        musicService = null;
    }
}