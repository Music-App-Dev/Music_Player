package com.example.musicplayer;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import androidx.appcompat.widget.SearchView;

import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.PorterDuff;

import java.util.ArrayList;
// SearchActivity.java
public class SearchActivity extends AppCompatActivity{

    private RecyclerView searchResultsRecyclerView;
    private SearchAdapter searchAdapter;
    private ArrayList<SpotifyTrack> searchResults;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setFullScreen();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        searchResults = new ArrayList<>();
        searchAdapter = new SearchAdapter(this, searchResults);

        searchResultsRecyclerView = findViewById(R.id.search_recycler_view);
        searchResultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        searchResultsRecyclerView.setAdapter(searchAdapter);

        // Setting up search bar
        SearchView searchView = findViewById(R.id.search_view);
        searchView.setQueryHint("Search for your songs here");


        searchView.setFocusable(true);
        searchView.setIconified(false);  // Keeps the search bar expanded by default
        searchView.setOnClickListener(view -> searchView.setIconified(false));

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // Call the Spotify search function here when the query is submitted
                searchSpotify(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false; // Optional: You can implement live search here
            }
        });

        findViewById(R.id.back_button).setOnClickListener(view -> finish());
    }


    private void setFullScreen() {
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }


    private void searchSpotify(String query) {
        SpotifyApiHelper.searchTracks(this, query, new SpotifyApiHelper.SpotifySearchCallback() {
            @Override
            public void onSearchSuccess(ArrayList<SpotifyTrack> tracks) {
                runOnUiThread(() -> {
                    searchResults.clear();
                    searchResults.addAll(tracks);
                    searchAdapter.notifyDataSetChanged();
                });
            }

            @Override
            public void onSearchFailure(Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(SearchActivity.this, "Search failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}
