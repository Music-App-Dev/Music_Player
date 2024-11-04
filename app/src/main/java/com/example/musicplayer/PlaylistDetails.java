package com.example.musicplayer;


import static com.example.musicplayer.MainActivity.albums;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
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

        recyclerView = findViewById(R.id.recylcerView);
        playlistPhoto = findViewById(R.id.playlistPhoto);
        backBtn = findViewById(R.id.back_btn_playlist);

        playlistName = getIntent().getStringExtra("playlistName");
        playlistId = getIntent().getStringExtra("playlistId");
        playlistImageUrl = getIntent().getStringExtra("playlistImageUrl");

        playlistTopName = findViewById(R.id.playlist_details_playlist_name);
        playlistTopName.setText(playlistName);

        playlistDetailsAdapter = new PlaylistDetailsAdapter(this, playlistSongs);
        recyclerView.setAdapter(playlistDetailsAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        fetchSpotifyPlaylistDetails(playlistId);

        backBtn.setOnClickListener(v -> finish());
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
