package com.example.musicplayer;

public class SpotifyPlaylist extends SpotifyItem {
    private String playlistName;
    private String imageUrl;
    private String playlistId;

    public SpotifyPlaylist(String playlistName, String imageUrl, String playlistId) {
        this.playlistName = playlistName;
        this.imageUrl = imageUrl;
        this.playlistId = playlistId;
    }

    public String getPlaylistName() {
        return playlistName;
    }

    @Override
    public String getType() {
        return "playlist";
    }

    public void setPlaylistName(String playlistName) {
        this.playlistName = playlistName;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    @Override
    public String getId() {
        return playlistId;
    }

    public void setPlaylistId(String playlistId) {
        this.playlistId = playlistId;
    }

    @Override
    public String getName() {
        return playlistName;
    }

    @Override
    public String getImageUrl() {
        return imageUrl;
    }
}
