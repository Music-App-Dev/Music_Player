package com.example.musicplayer;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SpotifyApiHelper {

    public interface SpotifyAlbumCallback {
        void onSuccess(ArrayList<SpotifyTrack> tracks, String albumArtUrl);
        void onFailure(Exception e);
    }

    public static void fetchAlbumTracks(Context context, String albumId, SpotifyAlbumCallback callback) {

        String accessToken = getAccessToken(context);
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("https://api.spotify.com/v1/albums/" + albumId + "/tracks")
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    ArrayList<SpotifyTrack> tracks = parseTracks(responseData);
                    String albumArtUrl = getAlbumArtUrl(albumId, accessToken);  // Fetch album art URL
                    callback.onSuccess(tracks, albumArtUrl);
                } else {
                    callback.onFailure(new Exception("Failed with response code: " + response.code()));
                }
            }
        });
    }

    private static String getAccessToken(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("SpotifyAuth", MODE_PRIVATE);
        return sharedPreferences.getString("access_token", null);
    }

    private static ArrayList<SpotifyTrack> parseTracks(String jsonData) {
        ArrayList<SpotifyTrack> tracks = new ArrayList<>();
        try {
            JSONObject jsonObject = new JSONObject(jsonData);
            JSONArray itemsArray = jsonObject.getJSONArray("items");

            for (int i = 0; i < itemsArray.length(); i++) {
                JSONObject item = itemsArray.getJSONObject(i);
                JSONObject trackObject = item.getJSONObject("track");

                // Extracting relevant details from the track
                String trackName = trackObject.getString("name");
                String artistName = trackObject.getJSONArray("artists").getJSONObject(0).getString("name");
                String albumName = trackObject.getJSONObject("album").getString("name");
                String albumImageUrl = trackObject.getJSONObject("album").getJSONArray("images").getJSONObject(0).getString("url");
                String trackId = trackObject.getString("id");
                String duration = trackObject.getString("duration_ms");

                // Optional: Retrieve album art URL
                String albumArtUrl = null;
                JSONArray imagesArray = trackObject.getJSONObject("album").getJSONArray("images");
                if (imagesArray.length() > 0) {
                    albumArtUrl = imagesArray.getJSONObject(0).getString("url"); // Get the first image URL (usually the largest)
                }

                SpotifyTrack track = new SpotifyTrack(trackName, artistName, albumName, duration, albumImageUrl, trackId);
                tracks.add(track);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return tracks;
    }

    private static String getAlbumArtUrl(String albumId, String accessToken) {
        OkHttpClient client = new OkHttpClient();
        String url = "https://api.spotify.com/v1/albums/" + albumId;

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        final String[] albumArtUrl = {null}; // To store the album art URL

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    try {
                        JSONObject jsonObject = new JSONObject(responseData);
                        JSONArray imagesArray = jsonObject.getJSONArray("images");
                        if (imagesArray.length() > 0) {
                            albumArtUrl[0] = imagesArray.getJSONObject(0).getString("url"); // Use the first image URL
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    System.err.println("Failed to get album art with response code: " + response.code());
                }
            }
        });

        return albumArtUrl[0]; // Will be null if request fails or no image is found
    }
}