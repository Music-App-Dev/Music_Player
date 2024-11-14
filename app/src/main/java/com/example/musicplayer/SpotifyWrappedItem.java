package com.example.musicplayer;

import android.os.Parcel;
import android.os.Parcelable;

public class SpotifyWrappedItem implements Parcelable {

    private String songName;
    private String songId;
    private String type;
    private String imageUrl;
    private String artistName;

    public SpotifyWrappedItem(String songName, String songId, String type, String imageUrl, String artistName) {
        this.songName = songName;
        this.songId = songId;
        this.type = type;
        this.imageUrl = imageUrl;
        this.artistName = artistName;
    }

    // Getter methods
    public String getName() {
        return songName;
    }

    public String getArtist() {
        return artistName;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    // Parcelable implementation
    protected SpotifyWrappedItem(Parcel in) {
        songName = in.readString();
        songId = in.readString();
        type = in.readString();
        imageUrl = in.readString();
        artistName = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(songName);
        dest.writeString(songId);
        dest.writeString(type);
        dest.writeString(imageUrl);
        dest.writeString(artistName);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<SpotifyWrappedItem> CREATOR = new Creator<SpotifyWrappedItem>() {
        @Override
        public SpotifyWrappedItem createFromParcel(Parcel in) {
            return new SpotifyWrappedItem(in);
        }

        @Override
        public SpotifyWrappedItem[] newArray(int size) {
            return new SpotifyWrappedItem[size];
        }
    };
}

