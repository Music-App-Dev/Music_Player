package com.example.musicplayer;

import static com.example.musicplayer.MainActivity.albums;
import static com.example.musicplayer.MainActivity.playlists;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;


public class PlaylistFragment extends Fragment {

    RecyclerView recyclerView;
    PlaylistAdapter playlistAdapter;
    ArrayList<SpotifyPlaylist> pendingPlaylists = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_playlist, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));

        if (!pendingPlaylists.isEmpty()) {
            // Use pending playlists if available
            playlistAdapter = new PlaylistAdapter(getContext(), new ArrayList<>(pendingPlaylists));
            pendingPlaylists.clear();
            Log.d("PLAYLISTPOG", "Setting adapter with pending playlists.");
        } else {
            playlistAdapter = new PlaylistAdapter(getContext(), playlists);
            Log.d("PLAYLISTPOG", "Setting adapter with current playlists.");
        }

        recyclerView.setAdapter(playlistAdapter);

        return view;
    }

    public void updatePlaylist(ArrayList<SpotifyPlaylist> newPlaylists) {
        if (recyclerView == null) {
            // Store playlists if RecyclerView is not yet initialized
            pendingPlaylists.clear();
            pendingPlaylists.addAll(newPlaylists);
            Log.d("PLAYLISTPOG", "updatePlaylist: Storing pending playlists. RecyclerView not ready.");
        } else {
            // Update directly if RecyclerView is ready
            playlistAdapter.updatePlaylist(newPlaylists);
            Log.d("PLAYLISTPOG", "updatePlaylist: Updating RecyclerView with " + newPlaylists.size() + " playlists.");
        }
    }
}