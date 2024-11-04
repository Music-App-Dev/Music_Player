package com.example.musicplayer;

import static com.example.musicplayer.ApplicationClass.ACTION_NEXT;
import static com.example.musicplayer.ApplicationClass.ACTION_PLAY;
import static com.example.musicplayer.ApplicationClass.ACTION_PREVIOUS;
import static com.example.musicplayer.ApplicationClass.CHANNEL_ID_2;
import static com.example.musicplayer.MainActivity.REQUEST_CODE;
import static com.example.musicplayer.MainActivity.repeatBoolean;
import static com.example.musicplayer.MainActivity.shuffleBoolean;
import static com.example.musicplayer.PlayerActivity.listSongs;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.media.session.MediaSessionCompat;
import android.telecom.Call;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.util.Consumer;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bumptech.glide.Glide;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.PlayerState;
import com.spotify.sdk.android.auth.AuthorizationClient;
import com.spotify.sdk.android.auth.AuthorizationRequest;
import com.spotify.sdk.android.auth.AuthorizationResponse;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class MusicService extends Service {
    private static final String TAG = "MusicService";
    private static SpotifyAppRemote spotifyAppRemote;

    private final IBinder mBinder = new MyBinder();
    public static ArrayList<SpotifyTrack> musicFiles = new ArrayList<>();
    public static int position = -1;
    private ActionPlaying actionPlaying;

    public static final String MUSIC_FILE_LAST_PLAYED = "LAST_PLAYED";
    public static final String MUSIC_FILE = "STORED_MUSIC";
    public static final String ARTIST_NAME = "ARTIST_NAME";
    public static final String SONG_NAME = "SONG_NAME";
    private static final String CLIENT_ID = "1e6a19b8b3364441b502d3c8c427ed6f";
    private static final String REDIRECT_URI = "com.example.musicplayer://callback";

    private Subscription<PlayerState> playerStateSubscription;

    public class MyBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }

    }


    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (spotifyAppRemote == null || !spotifyAppRemote.isConnected()) {
            connectToSpotify();
        }

        if (intent != null) {
            int myPosition = intent.getIntExtra("servicePosition", -1);
            String actionName = intent.getStringExtra("ActionName");

            if (myPosition != -1) {
                playMedia(myPosition);
            }

            if (actionName != null) {
                switch (actionName) {
                    case "playPause":
                        playPauseButtonClicked();
                        break;
                    case "next":
                        nextBtnClicked();
                        break;
                    case "previous":
                        previousBtnClicked();
                        break;
                }
            }
        } else {
            Log.w(TAG, "Received null intent in onStartCommand");
        }
        return START_STICKY;
    }

    private void connectToSpotify() {
        ConnectionParams connectionParams = new ConnectionParams.Builder(CLIENT_ID)
                .setRedirectUri(REDIRECT_URI)
                .showAuthView(true)
                .build();

        SpotifyAppRemote.connect(getApplicationContext(), connectionParams,
                new Connector.ConnectionListener() {
                    @Override
                    public void onConnected(SpotifyAppRemote appRemote) {
                        spotifyAppRemote = appRemote;
                        Log.d(TAG, "Connected to Spotify");
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        Log.e(TAG, "Failed to connect to Spotify", throwable);
                    }
                });
    }


    public void seekTo(int positionInMillis) {
        if (spotifyAppRemote != null) {
            spotifyAppRemote.getPlayerApi().getPlayerState().setResultCallback(playerState -> {
                if (playerState != null && playerState.track != null) {
                    if (!playerState.track.isPodcast && !playerState.track.isEpisode) {
                        spotifyAppRemote.getPlayerApi().seekTo(positionInMillis)
                                .setResultCallback(empty -> Log.d(TAG, "Seeked to position: " + positionInMillis))
                                .setErrorCallback(error -> {
                                    Log.e(TAG, "Error seeking to position", error);
                                });
                    } else {
                        Log.w(TAG, "Cannot seek in this track type.");
                    }
                } else {
                    Log.w(TAG, "PlayerState or Track is null. Retrying seek...");
                }
            }).setErrorCallback(error -> Log.e(TAG, "Error fetching player state", error));
        } else {
            Log.w(TAG, "SpotifyAppRemote is null. Falling back to Web API.");
        }
    }



    private String getAccessToken() {
        SharedPreferences sharedPreferences = getSharedPreferences("SpotifyAuth", MODE_PRIVATE);
        return sharedPreferences.getString("access_token", null);
    }

    public void subscribeToPlayerStateUpdates(Consumer<PlayerState> callback) {
        if (spotifyAppRemote != null && playerStateSubscription == null) {
            playerStateSubscription = (Subscription<PlayerState>) spotifyAppRemote.getPlayerApi().subscribeToPlayerState()
                    .setEventCallback(playerState -> {
                        if (playerState != null && playerState.track != null) {
                            Log.d(TAG, "Track: " + playerState.track.name +
                                    ", Artist: " + playerState.track.artist.name +
                                    ", Duration: " + playerState.track.duration +
                                    ", IsPodcast: " + playerState.track.isPodcast +
                                    ", IsEpisode: " + playerState.track.isEpisode);
                        } else {
                            Log.w(TAG, "PlayerState or Track is null.");
                        }
                        callback.accept(playerState);
                    })
                    .setErrorCallback(error -> Log.e(TAG, "Error subscribing to player state", error));
        }
    }

    public void unsubscribeFromPlayerStateUpdates() {
        if (playerStateSubscription != null) {
            playerStateSubscription.cancel();
            playerStateSubscription = null;
        }
    }
    // Define a Callback interface to handle asynchronous results
    interface Callback<T> {
        void onResult(T result);
    }

    public void playMedia(int startPosition) {
        if (listSongs != null && startPosition >= 0 && startPosition < listSongs.size()) {
            musicFiles = listSongs;
            position = startPosition;

            SpotifyTrack selectedTrack = musicFiles.get(position);
            Log.d(TAG, "playMedia: Playing track at position: " + position + ", Track: " + selectedTrack.getTrackName());

        } else {
            Log.e(TAG, "Invalid position or empty playlist.");
        }

    }

    private void saveLastPlayed(String trackUri) {
        SharedPreferences.Editor editor = getSharedPreferences(MUSIC_FILE_LAST_PLAYED, MODE_PRIVATE).edit();
        editor.putString(MUSIC_FILE, trackUri);
        editor.putString(ARTIST_NAME, musicFiles.get(position).getArtistName());
        editor.putString(SONG_NAME, musicFiles.get(position).getTrackName());
        editor.apply();
    }

    void isPlaying(Callback<Boolean> callback) {
        if (spotifyAppRemote != null) {
            spotifyAppRemote.getPlayerApi().subscribeToPlayerState().setEventCallback(playerState -> {
                boolean isPlaying = !playerState.isPaused;
                callback.onResult(isPlaying); // Pass result back to caller
            }).setErrorCallback(error -> {
                Log.e("MusicService", "Error fetching player state", error);
                callback.onResult(false); // Return false if there was an error
            });
        } else {
            callback.onResult(false); // If SpotifyAppRemote is null, assume not playing
        }
    }

    public static void setSpotifyAppRemote(SpotifyAppRemote appRemote) {
        spotifyAppRemote = appRemote;
    }

    void setCallBack(ActionPlaying actionPlaying) {
        this.actionPlaying = actionPlaying;
    }

    void showNotification(int playPauseBtn) {
        Intent intent = new Intent(this, PlayerActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        Intent prevIntent = new Intent(this, NotificationReceiver.class).setAction(ACTION_PREVIOUS);
        PendingIntent prevPending = PendingIntent.getBroadcast(this, 0, prevIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent pauseIntent = new Intent(this, NotificationReceiver.class).setAction(ACTION_PLAY);
        PendingIntent pausePending = PendingIntent.getBroadcast(this, 0, pauseIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent nextIntent = new Intent(this, NotificationReceiver.class).setAction(ACTION_NEXT);
        PendingIntent nextPending = PendingIntent.getBroadcast(this, 0, nextIntent, PendingIntent.FLAG_IMMUTABLE);

        SpotifyTrack currentTrack = musicFiles.get(position);
        String imageUrl = currentTrack.getAlbumImageUrl();

        // Load image asynchronously
        new Thread(() -> {
            Bitmap thumb = BitmapFactory.decodeResource(getResources(), R.drawable.gradient_bg); // Default image

            try {
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    thumb = Glide.with(this)
                            .asBitmap()
                            .load(imageUrl)
                            .submit()
                            .get();
                }
            } catch (Exception e) {
                Log.e("MusicService", "Error loading album image for notification", e);
            }

            // Build and show the notification on the main thread
            Bitmap finalThumb = thumb;
            new Handler(Looper.getMainLooper()).post(() -> {
                Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID_2)
                        .setSmallIcon(playPauseBtn)
                        .setLargeIcon(finalThumb)
                        .setContentTitle(currentTrack.getTrackName())
                        .setContentText(currentTrack.getArtistName())
                        .addAction(R.drawable.ic_skip_previous, "Previous", prevPending)
                        .addAction(playPauseBtn, "Pause/Play", pausePending)
                        .addAction(R.drawable.ic_skip_next, "Next", nextPending)
                        .setStyle(new androidx.media.app.NotificationCompat.MediaStyle())
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setOnlyAlertOnce(true)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        .setContentIntent(contentIntent)
                        .build();

                startForeground(5, notification);
            });
        }).start();
    }

    void nextBtnClicked() {
        if (musicFiles != null && !musicFiles.isEmpty()) {
            playMedia(PlayerActivity.position); // Ensure the media is played from the new position
        } else {
            Log.e(TAG, "nextBtnClicked: musicFiles is empty or null");
        }
    }

    void playPauseButtonClicked() {
        if (actionPlaying != null) {
            actionPlaying.playPauseBtnClicked();
        }
    }

    void previousBtnClicked() {
        if (musicFiles != null && !musicFiles.isEmpty()) {
            if (shuffleBoolean) {
                position = new Random().nextInt(musicFiles.size());
            } else {
                position = (position - 1 < 0) ? (musicFiles.size() - 1) : (position - 1); // Loop back to end
            }

            playMedia(position); // Ensure the media is played from the new position
        } else {
            Log.e(TAG, "previousBtnClicked: musicFiles is empty or null");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (spotifyAppRemote != null) {
            SpotifyAppRemote.disconnect(spotifyAppRemote);
        }
    }
}