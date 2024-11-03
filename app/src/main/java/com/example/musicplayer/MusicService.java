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


    // Additional methods
    public void seekTo(int positionInMillis) {
        if (spotifyAppRemote != null) {
            spotifyAppRemote.getPlayerApi().getPlayerState().setResultCallback(playerState -> {
                if (playerState != null && playerState.track != null) {
                    if (!playerState.track.isPodcast && !playerState.track.isEpisode) {
                        spotifyAppRemote.getPlayerApi().seekTo(positionInMillis)
                                .setResultCallback(empty -> Log.d(TAG, "Seeked to position: " + positionInMillis))
                                .setErrorCallback(error -> {
                                    Log.e(TAG, "Error seeking to position", error);
                                    fallbackSeekTo(positionInMillis);
                                });
                    } else {
                        Log.w(TAG, "Cannot seek in this track type.");
                    }
                } else {
                    Log.w(TAG, "PlayerState or Track is null. Retrying seek...");
                    retrySeek(positionInMillis);
                }
            }).setErrorCallback(error -> Log.e(TAG, "Error fetching player state", error));
        } else {
            Log.w(TAG, "SpotifyAppRemote is null. Falling back to Web API.");
            fallbackSeekTo(positionInMillis);
        }
    }


    private void fallbackSeekTo(int positionInMillis) {
        String accessToken = getAccessToken(); // Retrieve your access token
        if (accessToken == null) {
            Log.e(TAG, "Access token is null, cannot seek using Web API.");
            return;
        }

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https://api.spotify.com/v1/me/player/seek?position_ms=" + positionInMillis)
                .addHeader("Authorization", "Bearer " + accessToken)
                .put(RequestBody.create("", null)) // PUT request needs an empty body
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onResponse(@NonNull okhttp3.Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Seeked to position via Web API: " + positionInMillis);
                } else {
                    Log.e(TAG, "Failed to seek via Web API: " + response.message());
                }
            }

            @Override
            public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                Log.e(TAG, "Error seeking via Web API", e);

            }

        });
    }


    private void retrySeek(int positionInMillis) {
        new Handler(Looper.getMainLooper()).postDelayed(() -> seekTo(positionInMillis), 1000); // Retry after 1 second
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
            String spotifyUri = "spotify:track:" + selectedTrack.getTrackId();

            if (spotifyAppRemote != null) {
                spotifyAppRemote.getPlayerApi().play(spotifyUri)
                        .setResultCallback(empty -> Log.d(TAG, "Playing track: " + selectedTrack.getTrackName()))
                        .setErrorCallback(error -> Log.e(TAG, "Error playing track", error));
            } else {
                Log.e(TAG, "SpotifyAppRemote not connected, cannot play track.");
            }
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

    void start() {
        if (spotifyAppRemote != null) {
            spotifyAppRemote.getPlayerApi().resume();
        }
    }

    void pause() {
        if (spotifyAppRemote != null) {
            spotifyAppRemote.getPlayerApi().pause();
        }
    }

    void stop() {
        if (spotifyAppRemote != null) {
            spotifyAppRemote.getPlayerApi().pause();
        }
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
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID_2)
                .setSmallIcon(playPauseBtn)
                .setContentTitle(currentTrack.getTrackName())
                .setContentText(currentTrack.getArtistName())
                .addAction(R.drawable.ic_skip_previous, "Previous", prevPending)
                .addAction(playPauseBtn, "Pause/Play", pausePending)
                .addAction(R.drawable.ic_skip_next, "Next", nextPending)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOnlyAlertOnce(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle())
                .setContentIntent(contentIntent)
                .build();

        startForeground(1, notification);
    }

    void nextBtnClicked() {
        if (actionPlaying != null) {
            actionPlaying.nextBtnClicked();
        }
    }

    void playPauseButtonClicked() {
        if (actionPlaying != null) {
            actionPlaying.playPauseBtnClicked();
        }
    }

    void previousBtnClicked() {
        if (actionPlaying != null) {
            actionPlaying.prevBtnClicked();
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