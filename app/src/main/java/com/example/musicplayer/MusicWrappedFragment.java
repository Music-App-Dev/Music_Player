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
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MusicWrappedFragment extends Fragment {

    private RecyclerView topSongsRecyclerView;
    private TopSongsAdapter topSongsAdapter;
    private TopSongsViewModel musicViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_songs, container, false);

        topSongsRecyclerView = view.findViewById(R.id.recyclerView);
        topSongsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));

        FragmentManager fragmentManager = getChildFragmentManager();

        topSongsAdapter = new TopSongsAdapter(getContext(), new ArrayList<SpotifyTrack>(), fragmentManager);
        topSongsRecyclerView.setAdapter(topSongsAdapter);

        musicViewModel = new ViewModelProvider(this).get(TopSongsViewModel.class);

        musicViewModel.getTopSongs().observe(getViewLifecycleOwner(), songs -> {
            if (songs != null) {
                updateTopSongs(songs);
            }
        });

        // Get the access token and fetch top songs
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("SpotifyAuth", getContext().MODE_PRIVATE);
        String accessToken = sharedPreferences.getString("access_token", null);

        if (accessToken != null) {
            musicViewModel.fetchTopSongs(accessToken);
        } else {
            Log.e("MusicWrappedFragment", "Access token is null. Please authenticate.");
        }

        return view;
    }

    private void updateTopSongs(List<SpotifyTrack> songs) {
        topSongsAdapter.updateData(songs);
    }
}


