package com.example.musicplayer;

import static com.example.musicplayer.MainActivity.REQUEST_CODE;
import static com.example.musicplayer.MainActivity.repeatBoolean;
import static com.example.musicplayer.MainActivity.shuffleBoolean;
import static com.example.musicplayer.MusicAdapter.mFiles;


import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.palette.graphics.Palette;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
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

import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.types.PlayerState;
import com.spotify.sdk.android.auth.AuthorizationClient;
import com.spotify.sdk.android.auth.AuthorizationRequest;
import com.spotify.sdk.android.auth.AuthorizationResponse;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PlayerActivity extends AppCompatActivity
        implements ActionPlaying, ServiceConnection {

    private TextView song_name, artist_name, duration_played, duration_total;
    private ImageView cover_art, nextBtn, prevBtn, backBtn, shuffleBtn, repeatBtn, addCircleIcon;
    private FloatingActionButton playPauseBtn;
    private SeekBar seekBar;
    public static int position = -1;
    public static ArrayList<SpotifyTrack> listSongs = new ArrayList<>();

    private Handler handler = new Handler();
    private MusicService musicService;
    private SpotifyAppRemote spotifyAppRemote;

    private static final String CLIENT_ID = "1e6a19b8b3364441b502d3c8c427ed6f";
    private static final String REDIRECT_URI = "com.example.musicplayer://callback";

    private RefreshListener refreshListener;

    boolean isSpotifyConnecting = false;

    private static final String SPOTIFY_BASE_URL = "https://api.spotify.com/v1/me/tracks";
    private OkHttpClient httpClient = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setFullScreen();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        initViews();
        getIntentMethod();
        setupListeners();

        loadSavedStates();


        // Retrieve the access token from SharedPreferences or initiate authorization
        String accessToken = getAccessToken();
        if (accessToken != null) {
            connectSpotifyRemote(accessToken);
        } else {
            startSpotifyAuth();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("currentSong", position); // Save the song index
        outState.putBoolean("shuffleBoolean", shuffleBoolean); // Save shuffle state
        outState.putBoolean("repeatBoolean", repeatBoolean); // Save repeat state
    }

    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            int savedSongIndex = savedInstanceState.getInt("currentSong", 0);
            shuffleBoolean = savedInstanceState.getBoolean("shuffleBoolean", false);
            repeatBoolean = savedInstanceState.getBoolean("repeatBoolean", false);
            seekBar.setProgress(savedSongIndex); // Restore seek position
            position = savedSongIndex; // Restore the song index

            // Resume playback from the saved position
            if (musicService != null && spotifyAppRemote != null) {
                musicService.playMedia(position, false);
                spotifyAppRemote.getPlayerApi().seekTo(savedSongIndex);
            }
            shuffleBtn.setImageResource(shuffleBoolean ? R.drawable.ic_shuffle_on : R.drawable.ic_shuffle_off);
            repeatBtn.setImageResource(repeatBoolean ? R.drawable.ic_repeat_on : R.drawable.ic_repeat_off);
        }
    }

    protected void startSpotifyAuth() {
        AuthorizationRequest.Builder builder = new AuthorizationRequest.Builder(CLIENT_ID, AuthorizationResponse.Type.TOKEN, REDIRECT_URI);
        builder.setScopes(new String[]{"streaming", "user-library-read", "user-modify-playback-state"}); // Add scopes as needed
        AuthorizationRequest request = builder.build();
        AuthorizationClient.openLoginActivity(this, REQUEST_CODE, request);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE) {
            AuthorizationResponse response = AuthorizationClient.getResponse(resultCode, data);
            switch (response.getType()) {
                case TOKEN:
                    String accessToken = response.getAccessToken();
                    Log.d("MainActivity", "Access Token received: " + accessToken);
                    break;
                case ERROR:
                    Log.e("MainActivity", "Authorization error: " + response.getError());
                    Toast.makeText(this, "Authorization failed: " + response.getError(), Toast.LENGTH_LONG).show();
                    break;
                default:
                    Log.e("MainActivity", "Unknown response type: " + response.getType());
                    Toast.makeText(this, "Authorization response type: " + response.getType(), Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }


    private void loadSavedStates() {
        SharedPreferences preferences = getSharedPreferences("musicPlayerPrefs", MODE_PRIVATE);
        shuffleBoolean = preferences.getBoolean("shuffleBoolean", false);
        repeatBoolean = preferences.getBoolean("repeatBoolean", false);

        shuffleBtn.setImageResource(shuffleBoolean ? R.drawable.ic_shuffle_on : R.drawable.ic_shuffle_off);
        repeatBtn.setImageResource(repeatBoolean ? R.drawable.ic_repeat_on : R.drawable.ic_repeat_off);
    }

    private void saveShuffleState(boolean state) {
        SharedPreferences preferences = getSharedPreferences("musicPlayerPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("shuffleBoolean", state);
        editor.apply();
    }

    private void saveRepeatState(boolean state) {
        SharedPreferences preferences = getSharedPreferences("musicPlayerPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("repeatBoolean", state);
        editor.apply();
    }


    private String getAccessToken() {
        SharedPreferences sharedPreferences = getSharedPreferences("SpotifyAuth", MODE_PRIVATE);
        return sharedPreferences.getString("access_token", null);
    }

    private void connectSpotifyRemote(String accessToken) {
        if (isSpotifyConnecting || (spotifyAppRemote != null && spotifyAppRemote.isConnected())) {
            Log.d("PlayerActivity", "Spotify is already connecting or connected.");
            return;
        }
        isSpotifyConnecting = true;
        Log.d("PlayerActivity", "Attempting to connect with Access Token: " + accessToken);

        if (accessToken != null) {
            ConnectionParams connectionParams = new ConnectionParams.Builder(CLIENT_ID)
                    .setRedirectUri(REDIRECT_URI)
                    .showAuthView(true)
                    .build();

            SpotifyAppRemote.connect(this, connectionParams, new Connector.ConnectionListener() {
                @Override
                public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                    PlayerActivity.this.spotifyAppRemote = spotifyAppRemote;
                    Log.d("Spotify", "Connected successfully");
                    SpotifyTrack selectedTrack = listSongs.get(position);
                    String spotifyUri = "spotify:track:" + selectedTrack.getTrackId();

                    // Example: Check if player is ready and handle post-connection setup
                    spotifyAppRemote.getPlayerApi().play(spotifyUri).setResultCallback(empty -> {
                        Log.d("Spotify", "Track started: " + spotifyUri);
                        handler.post(updateSeekBarRunnable); // Start updating seekbar
                    }).setErrorCallback(throwable -> Log.e("Spotify", "Error starting playback: " + throwable.getMessage()));
                }

                @Override
                public void onFailure(Throwable throwable) {
                    Log.e("Spotify", "Connection failed: " + throwable.getMessage());
                    if (throwable.getMessage().contains("AUTHENTICATION_FAILED") ||
                            throwable.getMessage().contains("Explicit user authorization is required")) {
                        Log.d("Spotify", "Authentication required. Starting auth flow.");
                        startSpotifyAuth(); // Initiates the Spotify authorization flow
                    } else {
                        Log.d("Spotify", "Generic failure. Showing user feedback.");
                        Toast.makeText(PlayerActivity.this, "Failed to connect to Spotify: " + throwable.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            });
        } else {
            Log.d("PlayerActivity", "Access Token is null. Redirecting to MainActivity for authentication.");
            Intent intent = new Intent(PlayerActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
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
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, this, BIND_AUTO_CREATE);

        if (spotifyAppRemote != null && musicService != null) {
            handler.post(updateSeekBarRunnable); // Start updating seekbar if not already started
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(updateSeekBarRunnable);

        if (spotifyAppRemote != null) {
            spotifyAppRemote.getPlayerApi().getPlayerState().setResultCallback(playerState -> {
                SharedPreferences.Editor editor = getSharedPreferences("musicPlayerPrefs", MODE_PRIVATE).edit();
                editor.putInt("savedPosition", (int) playerState.playbackPosition);
                editor.putInt("currentSong", position); // Save the current song position
                editor.apply();
            });
        }

        if (musicService != null) {
            musicService.unsubscribeFromPlayerStateUpdates(); // Unsubscribe from updates
        }
        unbindService(this);
    }

    private void setupListeners() {
        shuffleBtn.setOnClickListener(v -> {
            shuffleBoolean = !shuffleBoolean;
            shuffleBtn.setImageResource(shuffleBoolean ? R.drawable.ic_shuffle_on : R.drawable.ic_shuffle_off);
            saveShuffleState(shuffleBoolean);
        });

        repeatBtn.setOnClickListener(v -> {
            repeatBoolean = !repeatBoolean;
            repeatBtn.setImageResource(repeatBoolean ? R.drawable.ic_repeat_on : R.drawable.ic_repeat_off);
            saveRepeatState(repeatBoolean);
        });

        addCircleIcon.setOnClickListener(v -> toggleLike());

        backBtn.setOnClickListener(v -> finish());

        playPauseBtn.setOnClickListener(v -> playPauseBtnClicked());

        nextBtn.setOnClickListener(v -> nextBtnClicked());
        prevBtn.setOnClickListener(v -> prevBtnClicked());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (spotifyAppRemote != null && fromUser) {
                    Log.d("SeekBar", "User changed seek bar position to: " + progress);
                    spotifyAppRemote.getPlayerApi().seekTo(progress * 1000);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                Log.d("SeekBar", "User started tracking seek bar");
                handler.removeCallbacks(updateSeekBarRunnable); // Pause automatic updates
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Log.d("SeekBar", "User stopped tracking seek bar");
                handler.post(updateSeekBarRunnable); // Resume automatic updates
            }
        });
    }

    private final Runnable updateSeekBarRunnable = new Runnable() {
        @Override
        public void run() {
            if (spotifyAppRemote != null) {
                spotifyAppRemote.getPlayerApi().getPlayerState().setResultCallback(playerState -> {
                    if (playerState != null && playerState.track != null) {
                        int currentPosition = (int) (playerState.playbackPosition / 1000); // in seconds
                        int duration = (int) (playerState.track.duration / 1000); // in seconds
                        if (!seekBar.isPressed()) {
                            seekBar.setMax(duration);
                            seekBar.setProgress(currentPosition);
                            duration_played.setText(formattedTime(currentPosition));
                            duration_total.setText(formattedTime(duration));
                        }

                        // Track end detection
                        if (currentPosition >= duration - 1) {
                            Log.d("PlayerActivity", "Track end detected, transitioning to next track");
                            nextBtnClicked();
                        }
                    }
                    handler.postDelayed(this, 1000); // Continue updates

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
                    handler.post(updateSeekBarRunnable);
                } else {
                    playPauseBtn.setImageResource(R.drawable.ic_play);
                    musicService.showNotification(R.drawable.ic_play);
                    spotifyAppRemote.getPlayerApi().pause();
                    handler.removeCallbacks(updateSeekBarRunnable);
                }
            });
        }
    }


    public void changeTrack(boolean isNext) {
        seekBar.setProgress(0);
        duration_played.setText(formattedTime(0));

        if (spotifyAppRemote != null) {
            Log.d("MusicService", "Changing track, isNext: " + isNext);
            // Handle repeat mode
            if (repeatBoolean) {
                Log.d("MusicService", "Repeat mode enabled, replaying the current track at position: " + position);
                // Keep the current position for repeat mode
            } else if (shuffleBoolean) {
                // Shuffle mode logic
                position = getRandom(listSongs.size() - 1);
                Log.d("MusicService", "Shuffle mode enabled, playing random track at position: " + position);
            } else {
                // Normal next/previous track logic
                if (isNext) {
                    position = (position + 1) % listSongs.size();
                } else {
                    position = (position - 1 < 0) ? listSongs.size() - 1 : position - 1;
                }
                Log.d("MusicService", "Playing next/previous track at position: " + position);
            }

            SpotifyTrack selectedTrack = listSongs.get(position);
            String spotifyUri = "spotify:track:" + selectedTrack.getTrackId();
            MusicService.playMedia(position, false);
            SharedPreferences.Editor editor = getSharedPreferences(MainActivity.MUSIC_FILE_LAST_PLAYED, MODE_PRIVATE).edit();
            editor.putString(MainActivity.MUSIC_FILE, selectedTrack.getAlbumImageUrl());
            editor.putString(MainActivity.ARTIST_NAME, selectedTrack.getArtistName());
            editor.putString(MainActivity.SONG_NAME, selectedTrack.getTrackName());
            editor.apply();
            updateUIWithCurrentTrack(selectedTrack);

            // Set the flag to show the mini-player
            MainActivity.SHOW_MINI_PLAYER = true;

            spotifyAppRemote.getPlayerApi().play(spotifyUri).setResultCallback(empty -> {
                handler.post(updateSeekBarRunnable); // Start updating seekbar
                song_name.setText(selectedTrack.getTrackName());
                artist_name.setText(selectedTrack.getArtistName());
                Glide.with(this)
                        .asBitmap()
                        .load(selectedTrack.getAlbumImageUrl())
                        .placeholder(R.drawable.gradient_bg) // Set gradient as a placeholder
                        .into(new BitmapImageViewTarget(cover_art) {
                            @Override
                            protected void setResource(Bitmap resource) {
                                if (resource != null && !resource.isRecycled()) {
                                    ImageAnimation(PlayerActivity.this, cover_art, resource); // Smooth fade-in animation
                                } else {
                                    cover_art.setImageResource(R.drawable.gradient_bg); // Fallback to default gradient
                                }
                            }
                        });

                playPauseBtn.setImageResource(R.drawable.ic_pause);
                musicService.showNotification(R.drawable.ic_pause);
            }).setErrorCallback(error -> Log.e("MusicService", "Error playing track: " + error.getMessage()));
        }
    }


    public void nextBtnClicked() {
        Log.d("MusicService", "Next button clicked, current position: " + position);
        changeTrack(true);
        Log.d("MusicService", "UI update after next button, new position: " + position);
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

    private void getIntentMethod() {
        Intent intent = getIntent();

        // Get position and track details
        position = intent.getIntExtra("position", -1);
        listSongs = intent.getParcelableArrayListExtra("trackList");

        if (listSongs != null && !listSongs.isEmpty() && position >= 0 && position < listSongs.size()) {
            SpotifyTrack selectedTrack = listSongs.get(position);

            // Update UI with track details
            song_name.setText(selectedTrack.getTrackName());
            artist_name.setText(selectedTrack.getArtistName());
            Glide.with(this).load(selectedTrack.getAlbumImageUrl()).into(cover_art);

            String trackId = listSongs.get(position).getTrackId();
            checkIfTrackLiked(trackId, isLiked -> runOnUiThread(() -> {
                addCircleIcon.setImageResource(isLiked ? R.drawable.ic_heart_filled : R.drawable.ic_add_circle);
            }));

            // Retrieve saved playback position and track ID
            SharedPreferences preferences = getSharedPreferences("musicPlayerPrefs", MODE_PRIVATE);
            String savedTrackId = preferences.getString("currentTrackId", null);
            int savedPosition = preferences.getInt("savedPosition", 0);

            // Check if the current track matches the saved track ID
            if (savedTrackId != null && savedTrackId.equals("spotify:track:" + selectedTrack.getTrackId())) {
                Log.d("PlayerActivity", "Resuming playback at position: " + savedPosition);
                spotifyAppRemote.getPlayerApi().seekTo(savedPosition).setResultCallback(empty -> {
                    Log.d("PlayerActivity", "Seeked to saved position: " + savedPosition);
                }).setErrorCallback(error -> Log.e("PlayerActivity", "Error seeking to saved position: " + error.getMessage()));
            }

            playPauseBtn.setImageResource(R.drawable.ic_pause);
        } else {
            Log.e("PlayerActivity", "Invalid song position or empty list.");
            Toast.makeText(this, "No song found to play.", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Start service for music control
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
        addCircleIcon = findViewById(R.id.add_circle);
    }

    private void metaData() {
        SpotifyTrack track = listSongs.get(position);
        String albumImageUrl = track.getAlbumImageUrl();
        int durationTotal = Integer.parseInt(track.getDuration()) / 1000;
        duration_total.setText(formattedTime(durationTotal));

        if (albumImageUrl != null) {
            Glide.with(this)
                    .asBitmap()
                    .load(albumImageUrl)
                    .placeholder(R.drawable.gradient_bg) // Placeholder for loading
                    .into(new BitmapImageViewTarget(cover_art) {
                        @Override
                        protected void setResource(Bitmap resource) {
                            super.setResource(resource);
                            if (resource != null && !resource.isRecycled()) {
                                ImageAnimation(PlayerActivity.this, cover_art, resource);
                                extractPalette(resource); // Extract palette after image is loaded
                            } else {
                                Log.e("PlayerActivity", "Bitmap is invalid or null");
                                applyDefaultStyles(); // Fallback in case of an invalid Bitmap
                            }
                        }
                    });
        } else {
            cover_art.setImageResource(R.drawable.gradient_bg);
            applyDefaultStyles();
        }
    }

    // Toggle the liked state for the track
    private void toggleLike() {
        String trackId = listSongs.get(position).getTrackId();
        checkIfTrackLiked(trackId, isLiked -> {
            if (isLiked) {
                removeTrackFromLiked(trackId);
            } else {
                addTrackToLiked(trackId);
            }
            if (refreshListener != null) {
                refreshListener.onRefresh(); // Notify main activity/fragment to refresh
            }
        });
    }

    public interface Callback<T> {
        void onResult(T result);
    }

    private void checkIfTrackLiked(String trackId, Callback<Boolean> callback) {
        String url = SPOTIFY_BASE_URL + "/contains?ids=" + trackId;
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + getAccessToken())
                .build();

        httpClient.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                Log.e("SpotifyAPI", "Failed to check liked status", e);
                callback.onResult(false);  // Default to not liked if the request fails
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    boolean isLiked = false;
                    try {
                        isLiked = new JSONArray(response.body().string()).getBoolean(0);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    callback.onResult(isLiked);
                } else {
                    Log.e("SpotifyAPI", "Error checking liked status: " + response.message());
                    callback.onResult(false);  // Default to not liked if there's an error
                }
            }
        });
    }

    // Add a track to liked tracks
    private void addTrackToLiked(String trackId) {
        String url = "https://api.spotify.com/v1/me/tracks";
        String json = "{\"ids\": [\"" + trackId + "\"]}";
        RequestBody body = RequestBody.create(json, okhttp3.MediaType.get("application/json"));

        // Log to verify URL and JSON content
        Log.d("SpotifyAPI", "Request URL: " + url);
        Log.d("SpotifyAPI", "Request JSON: " + json);

        Request request = new Request.Builder()
                .url(url)
                .put(body)
                .addHeader("Authorization", "Bearer " + getAccessToken())
                .build();

        httpClient.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                Log.e("SpotifyAPI", "Failed to add track to liked", e);
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.d("SpotifyAPI", "Track added to liked successfully");
                    runOnUiThread(() -> addCircleIcon.setImageResource(R.drawable.ic_heart_filled));
                } else {
                    // Log detailed error message and status code
                    Log.e("SpotifyAPI", "Error adding track to liked: " + response.message() + ", Code: " + response.code());
                    Log.e("SpotifyAPI", "Response body: " + response.body().string());
                }
            }
        });
    }

    // Remove a track from liked tracks
    private void removeTrackFromLiked(String trackId) {
        String url = SPOTIFY_BASE_URL + "?ids=" + trackId;
        Request request = new Request.Builder()
                .url(url)
                .delete()
                .addHeader("Authorization", "Bearer " + getAccessToken())
                .build();

        httpClient.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                Log.e("SpotifyAPI", "Failed to remove track from liked", e);
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.d("SpotifyAPI", "Track removed from liked successfully");
                    runOnUiThread(() -> addCircleIcon.setImageResource(R.drawable.ic_add_circle));
                } else {
                    Log.e("SpotifyAPI", "Error removing track from liked: " + response.message());
                }
            }
        });
    }



    private void extractPalette(Bitmap bitmap) {
        Palette.from(bitmap).generate(palette -> {
            if (palette != null) {
                Palette.Swatch swatch = palette.getDominantSwatch();
                if (swatch != null) {
                    updateUIWithSwatch(swatch);
                } else {
                    applyDefaultStyles();
                }
            } else {
                applyDefaultStyles(); // Fallback if Palette generation fails
            }
        });
    }
    private void updateUIWithSwatch(Palette.Swatch swatch) {
        ImageView gradient = findViewById(R.id.imageViewGradient);
        RelativeLayout mContainer = findViewById(R.id.mContainer);

        GradientDrawable gradientDrawable = new GradientDrawable(
                GradientDrawable.Orientation.BOTTOM_TOP,
                new int[]{swatch.getRgb(), 0x00000000}
        );
        gradient.setBackground(gradientDrawable);

        GradientDrawable gradientDrawableBg = new GradientDrawable(
                GradientDrawable.Orientation.BOTTOM_TOP,
                new int[]{swatch.getRgb(), swatch.getRgb()}
        );
        mContainer.setBackground(gradientDrawableBg);

        song_name.setTextColor(swatch.getTitleTextColor());
        artist_name.setTextColor(swatch.getBodyTextColor());
    }

    private void applyDefaultStyles() {
        ImageView gradient = findViewById(R.id.imageViewGradient);
        RelativeLayout mContainer = findViewById(R.id.mContainer);

        GradientDrawable gradientDrawable = new GradientDrawable(
                GradientDrawable.Orientation.BOTTOM_TOP,
                new int[]{0xff000000, 0x00000000}
        );
        gradient.setBackground(gradientDrawable);

        GradientDrawable gradientDrawableBg = new GradientDrawable(
                GradientDrawable.Orientation.BOTTOM_TOP,
                new int[]{0xff000000, 0xff000000}
        );
        mContainer.setBackground(gradientDrawableBg);

        song_name.setTextColor(Color.WHITE);
        artist_name.setTextColor(Color.DKGRAY);
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

        Log.d("MusicService", "SERVICEISCONNECTED");


        // Ensure we have a valid list of songs and position
        if (listSongs != null && !listSongs.isEmpty() && position >= 0 && position < listSongs.size()) {

            SpotifyTrack currentTrack = listSongs.get(position);

            // Show notification and setup UI updates
            musicService.showNotification(R.drawable.ic_pause);
            updateUIWithCurrentTrack(currentTrack);

            // Listen to playback state updates via musicService
            musicService.subscribeToPlayerStateUpdates(playerState -> {
                if (playerState.track != null) {
                    int currentPosition = (int) (playerState.playbackPosition / 1000);
                    int duration = (int) (playerState.track.duration / 1000);

                    Log.d("MusicService", "SERVICE CURRENT POSITION " + currentPosition);
                    Log.d("MusicService", "TRACK DURATION " + duration);

                    seekBar.setMax(duration);
                    seekBar.setProgress(currentPosition);
                    duration_played.setText(formattedTime(currentPosition));
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
        Log.d("MusicService", "Updating UI with track: " + currentTrack.getTrackName());
        song_name.setText(currentTrack.getTrackName());
        artist_name.setText(currentTrack.getArtistName());
        metaData(); // Load cover art and other track-specific info
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        musicService = null;
    }
}