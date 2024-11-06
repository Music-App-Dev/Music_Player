package com.example.musicplayer;


import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

public class PlaylistDetails extends AppCompatActivity {

    RecyclerView recyclerView;
    ImageView playlistPhoto, backBtn;
    String playlistName, playlistId, playlistImageUrl;
    ArrayList<SpotifyTrack> playlistSongs = new ArrayList<>();
    PlaylistDetailsAdapter playlistDetailsAdapter;
    TextView playlistTopName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_details);

        displayPlaylistDetails();
        fetchSpotifyPlaylistDetails(playlistId);
    }

    private void displayPlaylistDetails() {
        recyclerView = findViewById(R.id.recyclerView);
        playlistPhoto = findViewById(R.id.playlistPhoto);
        playlistTopName = findViewById(R.id.playlistTitle);
        backBtn = findViewById(R.id.back_btn_playlist); // Ensure this matches your layout ID

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbar.setNavigationOnClickListener(v -> finish());

        backBtn.setOnClickListener(v -> finish()); // Set listener here

        playlistDetailsAdapter = new PlaylistDetailsAdapter(this, playlistSongs);
        recyclerView.setAdapter(playlistDetailsAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        playlistName = getIntent().getStringExtra("combinedName");
        playlistId = getIntent().getStringExtra("combinedId");
        playlistImageUrl = getIntent().getStringExtra("combinedImageUrl");

        playlistTopName.setText(playlistName);
        Glide.with(this).load(playlistImageUrl).into(playlistPhoto);
    }

    private void fetchSpotifyPlaylistDetails(String playlistId) {
        Log.d("PlaylistDetails", "Fetching details for playlist ID: " + playlistId);
        SpotifyApiHelper.fetchPlaylistTracks(this, playlistId, new SpotifyApiHelper.SpotifyPlaylistCallback() {
            @Override
            public void onSuccess(ArrayList<SpotifyTrack> tracks) {
                Log.d("PlaylistDetails", "Tracks fetched: " + tracks.size());

                runOnUiThread(() -> {
                    playlistSongs.clear();
                    playlistSongs.addAll(tracks);
                    Log.d("PlaylistDetails", "Playlist songs size: " + playlistSongs.size());
                    playlistDetailsAdapter.notifyDataSetChanged();

                    // Display the playlist image at the top
                    if (playlistImageUrl != null && !playlistImageUrl.isEmpty()) {
                        Glide.with(PlaylistDetails.this).load(playlistImageUrl).into(playlistPhoto);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                Log.e("PlaylistDetails", "Failed to fetch playlist tracks", e);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!playlistSongs.isEmpty()) {
            playlistDetailsAdapter.notifyDataSetChanged();
        }
    }

}
