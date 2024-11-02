package com.example.musicplayer;

import static android.content.Context.BIND_AUTO_CREATE;
import static android.content.Context.MODE_PRIVATE;
import static com.example.musicplayer.MainActivity.ARTIST_TO_FRAG;
import static com.example.musicplayer.MainActivity.PATH_TO_FRAG;
import static com.example.musicplayer.MainActivity.SHOW_MINI_PLAYER;
import static com.example.musicplayer.MainActivity.SONG_TO_FRAG;
import static com.example.musicplayer.MusicService.musicFiles;
import static com.example.musicplayer.MusicService.position;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;


public class NowPlayingFragment extends Fragment implements ServiceConnection {

    ImageView nextBtn, albumArt;
    TextView artist, songName;
    FloatingActionButton playPauseBtn;
    View view;
    MusicService musicService;
    public static final String MUSIC_FILE_LAST_PLAYED = "LAST_PLAYED";
    public static final String MUSIC_FILE = "STORED_MUSIC";
    public static final String ARTIST_NAME = "ARTIST_NAME";
    public static final String SONG_NAME = "SONG_NAME";
    public NowPlayingFragment(){

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_now_playing, container, false);
        artist = view.findViewById(R.id.song_artist_miniPlayer);
        songName = view.findViewById(R.id.song_name_miniPlayer);
        albumArt = view.findViewById(R.id.bottom_album_art);
        nextBtn = view.findViewById(R.id.skip_next_button);
        playPauseBtn  = view.findViewById(R.id.play_miniPlayer);
        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(musicService != null){
                    musicService.nextBtnClicked();
                    if(getActivity() != null) {
                        SharedPreferences.Editor editor = getActivity().getSharedPreferences(MUSIC_FILE_LAST_PLAYED, MODE_PRIVATE)
                                .edit();
                        editor.putString(ARTIST_NAME, musicFiles
                                .get(position).getArtistName());
                        editor.putString(SONG_NAME, musicFiles
                                .get(position).getTrackName());
                        editor.apply();
                        SharedPreferences preferences = getActivity()
                                .getSharedPreferences(MUSIC_FILE_LAST_PLAYED, MODE_PRIVATE);
                        String path = preferences.getString(MUSIC_FILE, null);
                        String artistName = preferences.getString(ARTIST_NAME, null);
                        String song = preferences.getString(SONG_NAME, null);

                        if(path != null){
                            SHOW_MINI_PLAYER = true;
                            PATH_TO_FRAG = path;
                            ARTIST_TO_FRAG = artistName;
                            SONG_TO_FRAG = song;

                        } else {
                            SHOW_MINI_PLAYER = false;
                            PATH_TO_FRAG = null;
                            ARTIST_TO_FRAG = null;
                            SONG_TO_FRAG = null;
                        }
                        if(SHOW_MINI_PLAYER){
                            if(PATH_TO_FRAG != null){
                                displayAlbumArt();
                                songName.setText(SONG_TO_FRAG);
                                artist.setText(ARTIST_TO_FRAG);
                            }
                        }
                    }
                }
            }
        });
        playPauseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (musicService != null) {
                    musicService.playPauseButtonClicked();

                    // Use the callback to check the playing status and update the UI accordingly
                    musicService.isPlaying(isPlaying -> {
                        if (isPlaying) {
                            playPauseBtn.setImageResource(R.drawable.ic_pause);
                        } else {
                            playPauseBtn.setImageResource(R.drawable.ic_play);
                        }
                    });
                }
            }
        });
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if(SHOW_MINI_PLAYER){
            if(PATH_TO_FRAG != null){
                displayAlbumArt();
                songName.setText(SONG_TO_FRAG);
                artist.setText(ARTIST_TO_FRAG);
                Intent intent = new Intent(getContext(), MusicService.class);
                if(getContext() != null){
                    getContext().bindService(intent, this, Context.BIND_AUTO_CREATE);
                }
            }
        }
    }

    private void displayAlbumArt() {
        if (musicService != null && musicFiles != null && position >= 0 && position < musicFiles.size()) {
            String albumImageUrl = musicFiles.get(position).getAlbumImageUrl();

            // Load album art directly from Spotify URL
            if (albumImageUrl != null) {
                Glide.with(this).load(albumImageUrl).into(albumArt);  // Ensure `albumArt` is your ImageView for album art
            } else {
                albumArt.setImageResource(R.drawable.gradient_bg); // Set a default image if no album art URL is available
            }
        } else {
            Log.e("NowPlayingFragment", "Invalid song position or empty list in displayAlbumArt.");
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if(getContext() != null){
            getContext().unbindService(this);
        }
    }


    @Override
    public void onServiceConnected(ComponentName componentName, IBinder service) {
        MusicService.MyBinder binder = (MusicService.MyBinder) service;
        musicService = binder.getService();
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        musicService = null;
    }
}