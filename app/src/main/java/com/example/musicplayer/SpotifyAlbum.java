package com.example.musicplayer;

public class SpotifyAlbum {
    private String albumName;
    private String artistName;
    private String imageUrl;

    private String albumId;

    public SpotifyAlbum(String albumName, String artistName, String imageUrl, String albumId) {
        this.albumName = albumName;
        this.artistName = artistName;
        this.imageUrl = imageUrl;
        this.albumId = albumId;
    }

    public String getAlbumId() {
        return albumId;
    }

    public void setAlbumId(String albumId) {
        this.albumId = albumId;
    }

    public String getAlbumName() {
        return albumName;
    }

    public void setAlbumName(String albumName) {
        this.albumName = albumName;
    }

    public String getArtistName() {
        return artistName;
    }

    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
