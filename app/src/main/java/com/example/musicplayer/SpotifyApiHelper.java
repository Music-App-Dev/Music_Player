package com.example.musicplayer;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SpotifyApiHelper {

    public interface SpotifyAlbumCallback {
        void onSuccess(ArrayList<SpotifyTrack> tracks, String albumArtUrl);
        void onFailure(Exception e);
    }

    public interface SpotifyPlaylistCallback {
        void onSuccess(ArrayList<SpotifyTrack> tracks);
        void onFailure(Exception e);
    }

    public interface SpotifyPlaylistsCallback {
        void onPlaylistFetched(ArrayList<SpotifyItem> playlists); // For fetching playlists
        void onTrackAddedToPlaylist(); // For confirming a track was added
        void onFailure(Exception e); // For handling failures
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

    public static void fetchPlaylistTracks(Context context, String playlistId, SpotifyPlaylistCallback callback) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("SpotifyAuth", MODE_PRIVATE);
        String accessToken = sharedPreferences.getString("access_token", null);

        if (accessToken == null) {
            callback.onFailure(new Exception("Access token is null or invalid"));
            return;
        }

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("https://api.spotify.com/v1/playlists/" + playlistId + "/tracks")
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
                    ArrayList<SpotifyTrack> tracks = parsePlaylistTracks(responseData);

                    callback.onSuccess(tracks);
                } else {
                    Log.e("SpotifyApiHelper", "Failed response: " + response.code() + ", " + response.body().string());
                    callback.onFailure(new Exception("Failed with response code: " + response.code()));
                }
            }
        });
    }


    public static void fetchUserId(Context context, SpotifyUserIdCallback callback) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("SpotifyAuth", MODE_PRIVATE);
        String accessToken = sharedPreferences.getString("access_token", null);

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("https://api.spotify.com/v1/me")
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
                    try {
                        JSONObject jsonObject = new JSONObject(response.body().string());
                        String userId = jsonObject.getString("id");
                        callback.onSuccess(userId);
                    } catch (Exception e) {
                        callback.onFailure(e);
                    }
                } else {
                    callback.onFailure(new Exception("Failed with response code: " + response.code()));
                }
            }
        });
    }

    public interface SpotifyUserIdCallback {
        void onSuccess(String userId);
        void onFailure(Exception e);
    }

    public static void addToPlaylist(Context context, String playlistId, String trackUri, SpotifyPlaylistsCallback callback) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("SpotifyAuth", MODE_PRIVATE);
        String accessToken = sharedPreferences.getString("access_token", null);

        if (accessToken == null) {
            Log.e("addToPlaylist", "Access token is null. Please authenticate.");
            callback.onFailure(new Exception("Access token is null. Please authenticate."));
            return;
        }

        OkHttpClient client = new OkHttpClient();
        JSONObject postBody = new JSONObject();
        try {
            JSONArray urisArray = new JSONArray();
            urisArray.put(trackUri);  // Ensure trackUri is like "spotify:track:<track_id>"
            postBody.put("uris", urisArray);
            Log.d("addToPlaylist", "Request body created: " + postBody.toString());
        } catch (Exception e) {
            Log.e("addToPlaylist", "Failed to create JSON request body", e);
            callback.onFailure(e);
            return;
        }

        RequestBody body = RequestBody.create(postBody.toString(), MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url("https://api.spotify.com/v1/playlists/" + playlistId + "/tracks")
                .addHeader("Authorization", "Bearer " + accessToken)
                .post(body)
                .build();

        Log.d("addToPlaylist", "Request created with URL: " + request.url().toString());

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("addToPlaylist", "API call failed", e);
                callback.onFailure(e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.d("addToPlaylist", "Track successfully added to playlist.");
                    callback.onTrackAddedToPlaylist();
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    Log.e("addToPlaylist", "Failed with response code: " + response.code() + ", Error: " + errorBody);
                    callback.onFailure(new Exception("Failed with response code: " + response.code() + ", Error: " + errorBody));
                }
            }
        });
    }

    public interface SpotifySearchCallback {
        void onSearchSuccess(ArrayList<SpotifyTrack> tracks);
        void onSearchFailure(Exception e);
    }

    public static void searchTracks(Context context, String query, SpotifySearchCallback callback) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("SpotifyAuth", MODE_PRIVATE);
        String accessToken = sharedPreferences.getString("access_token", null);

        if (accessToken == null) {
            callback.onSearchFailure(new Exception("Access token is null or invalid"));
            return;
        }

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https://api.spotify.com/v1/search?q=" + Uri.encode(query) + "&type=track")
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onSearchFailure(e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseData = response.body().string();
                        JSONObject jsonObject = new JSONObject(responseData);
                        JSONArray itemsArray = jsonObject.getJSONObject("tracks").getJSONArray("items");

                        ArrayList<SpotifyTrack> tracks = new ArrayList<>();
                        for (int i = 0; i < itemsArray.length(); i++) {
                            JSONObject trackObject = itemsArray.getJSONObject(i);
                            SpotifyTrack track = parseTrackFromJson(trackObject);
                            tracks.add(track);
                        }
                        callback.onSearchSuccess(tracks);
                    } catch (JSONException e) {
                        callback.onSearchFailure(e);
                    }
                } else {
                    callback.onSearchFailure(new Exception("Failed with response code: " + response.code()));
                }
            }
        });
    }


    private static ArrayList<SpotifyTrack> parsePlaylistTracks(String jsonData) {
        ArrayList<SpotifyTrack> tracks = new ArrayList<>();
        try {
            // Log the raw JSON response for inspection
            Log.d("SpotifyApiHelper", "Playlist Tracks JSON: " + jsonData);

            JSONObject jsonObject = new JSONObject(jsonData);
            JSONArray itemsArray = jsonObject.getJSONArray("items");

            for (int i = 0; i < itemsArray.length(); i++) {
                JSONObject trackWrapper = itemsArray.getJSONObject(i);
                JSONObject trackObject = trackWrapper.getJSONObject("track");

                // Extracting track details
                String trackName = trackObject.getString("name");
                String artistName = trackObject.getJSONArray("artists").getJSONObject(0).getString("name");
                String trackId = trackObject.getString("id");
                String duration = trackObject.getString("duration_ms");

                Log.d("SpotifyApiHelper", "Track Name: " + trackName);
                Log.d("SpotifyApiHelper", "Artist Name: " + artistName);
                Log.d("SpotifyApiHelper", "Track ID: " + trackId);
                Log.d("SpotifyApiHelper", "Duration: " + duration);

                // Extracting album details
                JSONObject albumObject = trackObject.getJSONObject("album");
                String albumName = albumObject.getString("name");
                String albumImageUrl = "";

                try {
                    albumImageUrl = albumObject.getJSONArray("images").getJSONObject(0).getString("url");
                    Log.d("SpotifyApiHelper", "Album Name: " + albumName);
                    Log.d("SpotifyApiHelper", "Album Image URL: " + albumImageUrl);
                } catch (JSONException e) {
                    Log.w("SpotifyApiHelper", "Album image URL missing for track: " + trackName);
                }

                // Create a SpotifyTrack object and add it to the list
                SpotifyTrack track = new SpotifyTrack(trackName, artistName, albumName, duration, albumImageUrl, trackId);
                tracks.add(track);
            }
        } catch (JSONException e) {
            Log.e("SpotifyApiHelper", "JSON structure error in playlist track data", e);
        } catch (Exception e) {
            Log.e("SpotifyApiHelper", "Unexpected error parsing playlist track data", e);
        }

        return tracks;
    }

    public static void createPlaylist(Context context, String userId, String playlistName, String description, SpotifyPlaylistsCallback callback) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("SpotifyAuth", MODE_PRIVATE);
        String accessToken = sharedPreferences.getString("access_token", null);

        if (accessToken == null) {
            callback.onFailure(new Exception("Access token is null. Please authenticate."));
            return;
        }

        OkHttpClient client = new OkHttpClient();

        JSONObject postBody = new JSONObject();
        try {
            postBody.put("name", playlistName);
            postBody.put("description", description);
            postBody.put("public", false);  // Set to false for private playlists
        } catch (Exception e) {
            callback.onFailure(e);
            return;
        }

        RequestBody body = RequestBody.create(postBody.toString(), MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url("https://api.spotify.com/v1/users/" + userId + "/playlists")
                .addHeader("Authorization", "Bearer " + accessToken)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onFailure(e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    callback.onTrackAddedToPlaylist();  // Can be replaced with a more appropriate callback
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    callback.onFailure(new Exception("Failed with response code: " + response.code() + ", Error: " + errorBody));
                }
            }
        });
    }

    public static void addTrackToPlaylist(Context context, String playlistId, String trackUri, SpotifyPlaylistsCallback callback) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("SpotifyAuth", MODE_PRIVATE);
        String accessToken = sharedPreferences.getString("access_token", null);

        if (accessToken == null) {
            callback.onFailure(new Exception("Access token is null. Please authenticate."));
            return;
        }

        OkHttpClient client = new OkHttpClient();

        JSONObject postBody = new JSONObject();
        try {
            JSONArray uris = new JSONArray();
            uris.put(trackUri);
            postBody.put("uris", uris);
        } catch (Exception e) {
            callback.onFailure(e);
            return;
        }

        RequestBody body = RequestBody.create(postBody.toString(), MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url("https://api.spotify.com/v1/playlists/" + playlistId + "/tracks")
                .addHeader("Authorization", "Bearer " + accessToken)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onFailure(e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    callback.onTrackAddedToPlaylist(); // Notify success
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    callback.onFailure(new Exception("Failed with response code: " + response.code() + ", Error: " + errorBody));
                }
            }
        });
    }


    public static void fetchUserPlaylists(Context context, SpotifyPlaylistsCallback callback) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("SpotifyAuth", MODE_PRIVATE);
        String accessToken = sharedPreferences.getString("access_token", null);

        if (accessToken == null) {
            callback.onFailure(new Exception("Access token is null. Please authenticate."));
            return;
        }

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https://api.spotify.com/v1/me/playlists")
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
                    ArrayList<SpotifyItem> playlists = parseUserPlaylists(responseData);
                    callback.onPlaylistFetched(playlists);
                } else {
                    callback.onFailure(new Exception("Failed with response code: " + response.code()));
                }
            }
        });
    }

    private static ArrayList<SpotifyItem> parseUserPlaylists(String jsonData) {
        ArrayList<SpotifyItem> playlists = new ArrayList<>();
        try {
            JSONObject jsonObject = new JSONObject(jsonData);
            JSONArray itemsArray = jsonObject.getJSONArray("items");

            for (int i = 0; i < itemsArray.length(); i++) {
                JSONObject playlistObject = itemsArray.getJSONObject(i);
                String playlistName = playlistObject.getString("name");
                String playlistId = playlistObject.getString("id");
                String playlistImageUrl = "";

                JSONArray imagesArray = playlistObject.getJSONArray("images");
                if (imagesArray.length() > 0) {
                    playlistImageUrl = imagesArray.getJSONObject(0).getString("url");
                }

                // Create SpotifyItem with "playlist" type
                SpotifyItem item = new SpotifyPlaylist(playlistName, playlistImageUrl, playlistId);
                playlists.add(item);
            }
        } catch (Exception e) {
            Log.e("SpotifyApiHelper", "Error parsing playlist data", e);
        }

        return playlists;
    }


    private static SpotifyTrack parseTrackFromJson(JSONObject trackObject) throws JSONException {
        String trackName = trackObject.getString("name");
        String artistName = trackObject.getJSONArray("artists").getJSONObject(0).getString("name");
        String trackId = trackObject.getString("id");
        String duration = trackObject.getString("duration_ms");
        String albumName = trackObject.getJSONObject("album").getString("name");
        String albumImageUrl = trackObject.getJSONObject("album").getJSONArray("images").getJSONObject(0).getString("url");

        return new SpotifyTrack(trackName, artistName, albumName, duration, albumImageUrl, trackId);
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