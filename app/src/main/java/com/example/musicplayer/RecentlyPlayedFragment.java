package com.example.musicplayer;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class RecentlyPlayedFragment extends Fragment {

    private RecyclerView recentlyPlayedRecyclerView;
    private RecentlyPlayedAdapter recentlyPlayedAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_songs, container, false);


        fetchRecentlyPlayedTracks();

        // Initialize RecyclerView
        recentlyPlayedRecyclerView = view.findViewById(R.id.recyclerView);
        recentlyPlayedRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));

        // Initialize the adapter with an empty list
        recentlyPlayedAdapter = new RecentlyPlayedAdapter(requireContext(), new ArrayList<>());
        recentlyPlayedRecyclerView.setAdapter(recentlyPlayedAdapter);
        return view;
    }

    private void fetchRecentlyPlayedTracks() {
        SpotifyApiHelper.fetchRecentlyPlayed(requireContext(), new SpotifyApiHelper.SpotifyPlaylistCallback() {
            @Override
            public void onSuccess(ArrayList<SpotifyTrack> tracksReceived) {
                Log.d("TestFetch", "Successfully fetched recently played tracks:");
                for (SpotifyTrack track : tracksReceived) {
                    Log.d("TestFetch", "Track: " + track.getTrackName() + ", Artist: " + track.getArtistName() + ", Album: " + track.getAlbumName());
                }
                requireActivity().runOnUiThread(() -> recentlyPlayedAdapter.updateTracks(tracksReceived));
            }

            @Override
            public void onFailure(Exception e) {
                Log.e("RecentlyPlayedFragment", "Error fetching recently played tracks", e);
            }
        });
    }
}