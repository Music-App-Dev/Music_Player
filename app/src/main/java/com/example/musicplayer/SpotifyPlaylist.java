package com.example.musicplayer;

public class SpotifyPlaylist {

    private String albumName;
    private String imageUrl;

    private String playlistId;

    public SpotifyPlaylist(String albumName, String imageUrl, String playlistId) {
        this.albumName = albumName;
        this.imageUrl = imageUrl;
        this.playlistId = playlistId;
    }

    public String getPlaylistId() {
        return playlistId;
    }

    public void setPlaylistId(String albumId) {
        this.playlistId = albumId;
    }

    public String getPlaylistName() {
        return albumName;
    }

    public void setPlaylistName(String albumName) {
        this.albumName = albumName;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

}
