package com.example.musicplayer;

import static com.example.musicplayer.MainActivity.musicFiles;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

public class QueueFragment extends Fragment {

    private RecyclerView queueRecyclerView;
    private QueueAdapter queueAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_songs, container, false);

        // Initialize RecyclerView
        queueRecyclerView = view.findViewById(R.id.recyclerView);
        queueRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));

        // Retrieve and print the queue from QueueManager
        ArrayList<SpotifyTrack> queue = QueueManager.getQueueTracks();
        Log.d("QueueFragment", "Queue size: " + queue.size());
        for (SpotifyTrack track : queue) {
            Log.d("QueueFragment", "Track in queue: " + track.getTrackName());
        }

        // Initialize the adapter with the queue tracks
        queueAdapter = new QueueAdapter(requireContext(), queue);
        queueRecyclerView.setAdapter(queueAdapter);

        return view;
    }
}