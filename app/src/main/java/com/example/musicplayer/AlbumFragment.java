package com.example.musicplayer;

import static com.example.musicplayer.MainActivity.albums;
import static com.example.musicplayer.MainActivity.musicFiles;

import android.media.MediaMetadataRetriever;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;


public class AlbumFragment extends Fragment {

    RecyclerView recyclerView;
    AlbumAdapter albumAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_album, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));

        albumAdapter = new AlbumAdapter(getContext(), albums);
        recyclerView.setAdapter(albumAdapter);

        Log.d("AlbumFragment", "Setting up RecyclerView with " + (albums != null ? albums.size() : "null") + " albums.");

        // Initialize the adapter and set it to RecyclerView
        if (!albums.isEmpty()) {
            Log.d("AlbumFragment", "AlbumAdapter set with " + albums.size() + " albums.");
            updateAlbumList(albums);
        } else {
            Log.d("AlbumFragment", "No albums to display in onCreateView.");
        }

        return view;
    }

    // Method to update the albums in the adapter
    public void updateAlbumList(ArrayList<SpotifyAlbum> newAlbums) {
        Log.d("AlbumFragment", "updateAlbumList: Received " + newAlbums.size() + " albums");

        if (albumAdapter != null) {
            // Initialize the adapter if it hasn't been initialized yet
            albumAdapter = new AlbumAdapter(getContext(), newAlbums);
            recyclerView.setAdapter(albumAdapter);
            Log.d("AlbumFragment", "AlbumAdapter initialized in updateAlbumList with " + newAlbums.size() + " albums.");
        } else {
            albumAdapter.updateAlbums(newAlbums);
            Log.d("AlbumFragment", "Album list updated with " + newAlbums.size() + " albums.");
        }
    }
}