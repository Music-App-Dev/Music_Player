package com.example.musicplayer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.HashSet;

public class AlbumDetails extends AppCompatActivity {

    RecyclerView recyclerView;
    ImageView albumPhoto, backBtn;
    String albumName, albumId, albumImageUrl;
    ArrayList<SpotifyTrack> albumSongs = new ArrayList<>();
    AlbumDetailsAdapter albumDetailsAdapter;
    TextView albumTopName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album_details);

        displayPlaylistDetails();
        fetchSpotifyAlbumDetails(albumId);
    }

    private void displayPlaylistDetails() {
        recyclerView = findViewById(R.id.recyclerView);
        albumPhoto = findViewById(R.id.albumPhoto);
        albumTopName = findViewById(R.id.albumTitle);
        backBtn = findViewById(R.id.back_btn_album); // Ensure this matches your layout ID

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbar.setNavigationOnClickListener(v -> finish());

        backBtn.setOnClickListener(v -> finish()); // Set listener here

        albumDetailsAdapter = new AlbumDetailsAdapter(this, albumSongs);
        recyclerView.setAdapter(albumDetailsAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        albumName = getIntent().getStringExtra("combinedName");
        albumId = getIntent().getStringExtra("combinedId");
        albumImageUrl = getIntent().getStringExtra("combinedImageUrl");

        albumTopName.setText(albumName);
        Glide.with(this).load(albumImageUrl).into(albumPhoto);
    }

    private void fetchSpotifyAlbumDetails(String albumId) {
        SpotifyApiHelper.fetchAlbumTracks(this, albumId, albumName, albumImageUrl, new SpotifyApiHelper.SpotifyAlbumCallback() {
            @Override
            public void onSuccess(ArrayList<SpotifyTrack> tracks, String albumArtUrl) {
                Log.d("AlbumDetails", "Tracks fetched: " + tracks.size());  // Log track count
                Log.d("AlbumDetails", "Album art URL: " + albumArtUrl);

                runOnUiThread(() -> {
                    albumSongs.clear();
                    albumSongs.addAll(tracks);
                    Log.d("AlbumDetails", "Album songs size: " + albumSongs.size());  // Ensure tracks are added

                    albumDetailsAdapter.notifyDataSetChanged(); // Notify adapter of data change
                    Log.d("AlbumDetails", "Adapter notified");

                    if (albumArtUrl != null && !albumArtUrl.isEmpty()) {
                        Glide.with(AlbumDetails.this).load(albumArtUrl).into(albumPhoto);
                    } else {
                        Glide.with(AlbumDetails.this).load(R.drawable.gradient_bg).into(albumPhoto);
                    }

                    setupRecyclerView();
                });
            }

            @Override
            public void onFailure(Exception e) {
                Log.e("AlbumDetails", "Failed to fetch album tracks", e);
            }
        });
    }


    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        albumDetailsAdapter = new AlbumDetailsAdapter(this, albumSongs);
        recyclerView.setAdapter(albumDetailsAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!(albumSongs.size() < 1)){
            albumDetailsAdapter = new AlbumDetailsAdapter(this, albumSongs);
            recyclerView.setAdapter(albumDetailsAdapter);
            recyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        }
    }

}