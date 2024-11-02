package com.example.musicplayer;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class SpotifyTrack implements Parcelable {
    private String trackName;
    private String artistName;
    private String albumName;
    private String duration;
    private String albumImageUrl;
    private String trackId;

    // Constructor
    public SpotifyTrack(String trackName, String artistName, String albumName, String duration, String albumImageUrl, String trackId) {
        this.trackName = trackName;
        this.artistName = artistName;
        this.albumName = albumName;
        this.duration = duration;
        this.albumImageUrl = albumImageUrl;
        this.trackId = trackId;
    }

    // Parcelable implementation
    protected SpotifyTrack(Parcel in) {
        trackName = in.readString();
        artistName = in.readString();
        albumName = in.readString();
        duration = in.readString();
        albumImageUrl = in.readString();
        trackId = in.readString();
    }

    public static final Creator<SpotifyTrack> CREATOR = new Creator<SpotifyTrack>() {
        @Override
        public SpotifyTrack createFromParcel(Parcel in) {
            return new SpotifyTrack(in);
        }

        @Override
        public SpotifyTrack[] newArray(int size) {
            return new SpotifyTrack[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(trackName);
        dest.writeString(artistName);
        dest.writeString(albumName);
        dest.writeString(duration);
        dest.writeString(albumImageUrl);
        dest.writeString(trackId);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    // Getter methods for the properties
    public String getTrackName() { return trackName; }
    public String getArtistName() { return artistName; }
    public String getAlbumName() { return albumName; }
    public String getDuration() { return duration; }
    public String getAlbumImageUrl() { return albumImageUrl; }
    public String getTrackId() { return trackId; }
}
