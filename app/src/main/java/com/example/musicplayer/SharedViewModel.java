package com.example.musicplayer;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SharedViewModel extends ViewModel {
    private final MutableLiveData<SpotifyTrack> currentTrack = new MutableLiveData<>();

    public void setCurrentTrack(SpotifyTrack track) {
        currentTrack.setValue(track);
    }

    public LiveData<SpotifyTrack> getCurrentTrack() {
        return currentTrack;
    }
}
