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

    public static void fetchAlbumTracks(Context context, String albumId, String albumName, String albumImageUrl, SpotifyAlbumCallback callback) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("SpotifyAuth", MODE_PRIVATE);
        String accessToken = sharedPreferences.getString("access_token", null);

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
                    ArrayList<SpotifyTrack> tracks = parseTracks(responseData, albumName, albumImageUrl);

                    callback.onSuccess(tracks, albumImageUrl);
                } else {
                    callback.onFailure(new Exception("Failed with response code: " + response.code()));
                }
            }
        });
    }


    private interface AlbumArtCallback {
        void onAlbumArtFetched(String albumArtUrl);
        void onFailure(Exception e);
    }

    private static void fetchAlbumArtUrl(String albumId, String accessToken, AlbumArtCallback callback) {
        OkHttpClient client = new OkHttpClient();
        String url = "https://api.spotify.com/v1/albums/" + albumId;

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onFailure(e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    try {
                        JSONObject jsonObject = new JSONObject(responseData);
                        JSONArray imagesArray = jsonObject.getJSONArray("images");
                        if (imagesArray.length() > 0) {
                            String albumArtUrl = imagesArray.getJSONObject(0).getString("url");
                            callback.onAlbumArtFetched(albumArtUrl);
                        } else {
                            callback.onAlbumArtFetched(null);
                        }
                    } catch (Exception e) {
                        callback.onFailure(e);
                    }
                } else {
                    callback.onFailure(new Exception("Failed with response code: " + response.code()));
                }
            }
        });
    }


    private static ArrayList<SpotifyTrack> parseTracks(String jsonData, String albumName, String albumImageUrl) {
        ArrayList<SpotifyTrack> tracks = new ArrayList<>();
        try {
            JSONObject jsonObject = new JSONObject(jsonData);
            JSONArray itemsArray = jsonObject.getJSONArray("items");

            for (int i = 0; i < itemsArray.length(); i++) {
                JSONObject trackObject = itemsArray.getJSONObject(i);

                // Extracting relevant details directly
                String trackName = trackObject.getString("name");
                String artistName = trackObject.getJSONArray("artists").getJSONObject(0).getString("name");
                String trackId = trackObject.getString("id");
                String duration = trackObject.getString("duration_ms");

                // Note: The album name and image URL need to be fetched separately or passed into this method if needed
                SpotifyTrack track = new SpotifyTrack(trackName, artistName, albumName, duration, albumImageUrl, trackId);
                tracks.add(track);
            }
        } catch (Exception e) {
            Log.e("SpotifyApiHelper", "Error parsing track data", e);
        }

        return tracks;
    }
}