package com.example.musicplayer;

import java.util.ArrayList;

public class QueueManager {
    private static final ArrayList<SpotifyTrack> queueTracks = new ArrayList<>();

    public static void addTrackToQueue(SpotifyTrack track) {
        if (!queueTracks.contains(track)) {
            queueTracks.add(track);
        }
    }

    public static ArrayList<SpotifyTrack> getQueueTracks() {
        return queueTracks;
    }

    public static void clearQueue() {
        queueTracks.clear();
    }
}