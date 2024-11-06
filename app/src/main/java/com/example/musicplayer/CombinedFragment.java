package com.example.musicplayer;

import static com.example.musicplayer.MainActivity.combPlayAlbums;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;


public class CombinedFragment extends Fragment {

    RecyclerView recyclerView;
    CombinedAdapter combinedAdapter;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_combined, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));

        combinedAdapter = new CombinedAdapter(getContext(), combPlayAlbums);
        recyclerView.setAdapter(combinedAdapter);

        Log.d("CombinedFragment", "Setting up RecyclerView with " + (combPlayAlbums != null ? combPlayAlbums.size() : "null") + " albums.");

        // Initialize the adapter and set it to RecyclerView
        if (!combPlayAlbums.isEmpty()) {
            Log.d("CombinedFragment", "CombinedAdapter set with " + combPlayAlbums.size() + " albums.");
            updateAlbumPlayList(combPlayAlbums);
        } else {
            Log.d("CombinedFragment", "No albums to display in onCreateView.");
        }

        return view;
    }

    public void refreshCombinedList(ArrayList<SpotifyItem> updatedList) {
        if (combinedAdapter != null) {
            combinedAdapter.updateAlbums(updatedList);
            combinedAdapter.notifyDataSetChanged();
            Log.d("CombinedFragment", "Combined list refreshed with " + updatedList.size() + " items.");
        } else {
            Log.d("CombinedFragment", "Adapter is null, cannot refresh.");
        }
    }

    // Method to update the albums in the adapter
    public void updateAlbumPlayList(ArrayList<SpotifyItem> newAlbums) {
        Log.d("AlbumFragment", "updateAlbumList: Received " + newAlbums.size() + " albums");

        if (combinedAdapter != null) {
            // Initialize the adapter if it hasn't been initialized yet
            combinedAdapter = new CombinedAdapter(getContext(), newAlbums);
            recyclerView.setAdapter(combinedAdapter);
            Log.d("CombinedFragment", "AlbumAdapter initialized in updateAlbumList with " + newAlbums.size() + " albums.");
        } else {
            combinedAdapter.updateAlbums(newAlbums);
            Log.d("CombinedFragment", "Album list updated with " + newAlbums.size() + " albums.");
        }
    }
}