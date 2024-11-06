package com.example.musicplayer;

import static com.example.musicplayer.MainActivity.musicFiles;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;


public class SongsFragment extends Fragment {
    RecyclerView recyclerView;
    static MusicAdapter musicAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_songs, container, false);

        // Initialize RecyclerView
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));

        // Initialize the adapter with an empty list initially
        musicAdapter = new MusicAdapter(getContext(), musicFiles, getChildFragmentManager());
        recyclerView.setAdapter(musicAdapter);

        return view;
    }

    public MusicAdapter getMusicAdapter() {
        return musicAdapter; // Assuming musicAdapter is an instance variable in SongsFragment
    }

    private ArrayList<SpotifyTrack> fullMusicList = new ArrayList<>();  // Store the full list here

    // Method to initialize or set the full list
    public void setFullMusicList(ArrayList<SpotifyTrack> musicFiles) {
        this.fullMusicList.clear();
        this.fullMusicList.addAll(musicFiles);
        this.updateMusicList(fullMusicList);  // Update display initially
    }

    public ArrayList<SpotifyTrack> getFullMusicList() {
        return new ArrayList<>(fullMusicList);  // Return a copy to avoid accidental modifications
    }

    public MusicAdapter getAdapter() {
        return musicAdapter;
    }

    public void updateMusicList(ArrayList<SpotifyTrack> newTracks) {
        // Ensure recyclerView is not null before updating
        if (recyclerView != null && musicAdapter != null) {
            musicFiles.clear();
            musicFiles.addAll(newTracks);
            musicAdapter.updateList(newTracks);
            musicAdapter.notifyDataSetChanged();
            Log.d("SongsFragment", "Music list updated with " + newTracks.size() + " tracks.");
        } else {
            Log.e("SongsFragment", "RecyclerView or Adapter not initialized.");
        }
    }
}