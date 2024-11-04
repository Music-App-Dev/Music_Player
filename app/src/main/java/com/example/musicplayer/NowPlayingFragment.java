package com.example.musicplayer;

import static android.content.Context.BIND_AUTO_CREATE;
import static android.content.Context.MODE_PRIVATE;
import static com.example.musicplayer.MainActivity.ARTIST_TO_FRAG;
import static com.example.musicplayer.MainActivity.PATH_TO_FRAG;
import static com.example.musicplayer.MainActivity.SHOW_MINI_PLAYER;
import static com.example.musicplayer.MainActivity.SONG_TO_FRAG;
import static com.example.musicplayer.MusicService.musicFiles;
import static com.example.musicplayer.MusicService.playMedia;
import static com.example.musicplayer.MusicService.position;


import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;


public class NowPlayingFragment extends Fragment implements ServiceConnection {

    private static final String TAG = "NowPlayingFragment";

    ImageView nextBtn, albumArt;
    TextView artist, songName;
    FloatingActionButton playPauseBtn;
    View view;
    MusicService musicService;
    public static final String MUSIC_FILE_LAST_PLAYED = "LAST_PLAYED";
    public static final String MUSIC_FILE = "STORED_MUSIC";
    public static final String ARTIST_NAME = "ARTIST_NAME";
    public static final String SONG_NAME = "SONG_NAME";

    public NowPlayingFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: Inflating layout for NowPlayingFragment");
        view = inflater.inflate(R.layout.fragment_now_playing, container, false);

        artist = view.findViewById(R.id.song_artist_miniPlayer);
        songName = view.findViewById(R.id.song_name_miniPlayer);
        albumArt = view.findViewById(R.id.bottom_album_art);
        nextBtn = view.findViewById(R.id.skip_next_button);
        playPauseBtn = view.findViewById(R.id.play_miniPlayer);
        playPauseBtn.setImageResource(R.drawable.ic_pause);

        setDefaultUI();

        view.setOnClickListener(v -> openPlayerActivity());

        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Next button clicked");
                if (musicService != null) {
                    Log.d(TAG, "MusicService is not null, invoking nextBtnClicked");
                    PlayerActivity.position = 1;
                    musicService.nextBtnClicked();
                    if (getActivity() != null) {
                        updateSharedPreferences();
                        updateUI();
                    } else {
                        Log.e(TAG, "Activity is null");
                    }
                } else {
                    Log.e(TAG, "MusicService is null");
                }
            }
        });

        playPauseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "PlayPause button clicked");
                if (musicService != null) {
                    musicService.playPauseButtonClicked();
                    musicService.isPlaying(isPlaying -> {
                        Log.d(TAG, "Is playing: " + isPlaying);
                        playPauseBtn.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
                    });
                } else {
                    Log.e(TAG, "MusicService is null");
                }
            }
        });
        return view;
    }

    private void openPlayerActivity() {
        if (musicFiles != null && position >= 0 && position < musicFiles.size()) {
            Intent intent = new Intent(getActivity(), PlayerActivity.class);
            intent.putParcelableArrayListExtra("trackList", new ArrayList<>(musicFiles)); // Pass the track list
            intent.putExtra("position", position); // Pass the current position
            startActivity(intent);
        } else {
            Log.e(TAG, "Cannot open PlayerActivity: invalid position or musicFiles is null");
        }
    }

    private void setDefaultUI() {
        Log.d(TAG, "Setting default UI");
        songName.setText(R.string.default_song_name);
        artist.setText(R.string.default_artist_name);
        playPauseBtn.setImageResource(R.drawable.ic_pause);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Binding service and registering receiver");
        Intent intent = new Intent(getContext(), MusicService.class);
        if (getContext() != null) {
            getContext().bindService(intent, this, Context.BIND_AUTO_CREATE);
        }

        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(trackChangeReceiver,
                new IntentFilter("TRACK_CHANGED"));

        if (SHOW_MINI_PLAYER && PATH_TO_FRAG != null) {
            updateUI();
        } else {
            setDefaultUI();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: Unbinding service and unregistering receiver");
        if (getContext() != null) {
            getContext().unbindService(this);
        }
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(trackChangeReceiver);
    }

    private final BroadcastReceiver trackChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Track change received, updating UI");
            updateUI();
        }
    };

    private void displayAlbumArt() {
        if (musicService != null && musicFiles != null && position >= 0 && position < musicFiles.size()) {
            String albumImageUrl = musicFiles.get(position).getAlbumImageUrl();
            Log.d(TAG, "Displaying album art: " + albumImageUrl);
            if (albumImageUrl != null) {
                Glide.with(this)
                        .load(albumImageUrl)
                        .placeholder(R.drawable.gradient_bg)
                        .into(albumArt);
            } else {
                albumArt.setImageResource(R.drawable.gradient_bg);
            }
        } else {
            Log.e(TAG, "Invalid position or musicFiles is null in displayAlbumArt");
        }
    }

    private void updateUI() {
        if (musicService != null) {
            int currentPosition = position;
            ArrayList<SpotifyTrack> currentMusicFiles = musicFiles;

            if (currentMusicFiles != null && currentPosition >= 0 && currentPosition < currentMusicFiles.size()) {
                SpotifyTrack currentTrack = currentMusicFiles.get(currentPosition);
                Log.d("NowPlayingFragment", "Updating UI with track: " + currentTrack.getTrackName());
                songName.setText(currentTrack.getTrackName());
                artist.setText(currentTrack.getArtistName());
                displayAlbumArt();
            } else {
                Log.e("NowPlayingFragment", "Invalid position or musicFiles is null in updateUI");
                setDefaultUI();
            }
        } else {
            Log.e("NowPlayingFragment", "MusicService is null in updateUI");
        }
    }

    private void updateSharedPreferences() {
        Log.d(TAG, "Updating SharedPreferences with current track info");
        SharedPreferences.Editor editor = getActivity().getSharedPreferences(MUSIC_FILE_LAST_PLAYED, MODE_PRIVATE).edit();
        editor.putString(MUSIC_FILE, musicFiles.get(position).getAlbumImageUrl());
        editor.putString(ARTIST_NAME, musicFiles.get(position).getArtistName());
        editor.putString(SONG_NAME, musicFiles.get(position).getTrackName());
        editor.apply();
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder service) {
        Log.d(TAG, "Service connected");
        MusicService.MyBinder binder = (MusicService.MyBinder) service;
        musicService = binder.getService();

        // Check musicFiles after service is connected
        if (musicService != null) {
            Log.d(TAG, "Checking musicFiles and position after service connected.");
            Log.d(TAG, "musicFiles size: " + (musicFiles != null ? musicFiles.size() : "null"));
            Log.d(TAG, "Current position: " + position);
        }

        updateUI();
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        Log.d(TAG, "Service disconnected");
        musicService = null;
    }
}