package com.example.musicplayer;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class RecentlyPlayedFragment extends Fragment {

    private RecyclerView recentlyPlayedSongsRecyclerView;
    private MusicAdapter musicAdapter;

    SharedPreferences sharedPreferences;
    String accessToken;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_songs, container, false);

        recentlyPlayedSongsRecyclerView = view.findViewById(R.id.recyclerView);
        recentlyPlayedSongsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));

        // Initialize the adapter with an empty list and set it to the RecyclerView
        musicAdapter = new MusicAdapter(getContext(), new ArrayList<>(), getChildFragmentManager());
        recentlyPlayedSongsRecyclerView.setAdapter(musicAdapter);

        sharedPreferences = getActivity().getSharedPreferences("SpotifyAuth", getContext().MODE_PRIVATE);
        accessToken = sharedPreferences.getString("access_token", null);

        if (accessToken != null) {
            Log.d("RecentlyPlayedFragment", "Fetching recently played songs...");
            getRecentlyPlayedSongs();
        } else {
            Log.e("RecentlyPlayedFragment", "Access token is null. Please authenticate.");
        }

        return view;
    }

    private void updateRecentlyPlayedSongs(List<SpotifyTrack> recentlyPlayedSongs) {
        // Update the adapter's data with the recently played songs
        musicAdapter.updateData(recentlyPlayedSongs);
        musicAdapter.notifyDataSetChanged();
    }

    private void getRecentlyPlayedSongs() {
        if (accessToken == null) {
            Log.e("RecentlyPlayedFragment", "Access token is null. Please authenticate.");
            return;
        }

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https://api.spotify.com/v1/me/player/recently-played?limit=20")
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        Log.d("RecentlyPlayedFragment", "Starting network request to fetch recently played songs.");

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("RecentlyPlayedFragment", "Failed to fetch recently played songs", e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    try {
                        JSONObject jsonObject = new JSONObject(responseData);
                        JSONArray items = jsonObject.getJSONArray("items");
                        ArrayList<SpotifyTrack> recentlyPlayedSongs = new ArrayList<>();

                        for (int i = 0; i < items.length(); i++) {
                            JSONObject songObject = items.getJSONObject(i);
                            String songName = songObject.getString("name");
                            String songId = songObject.getString("id");

                            // Extract album information
                            JSONObject albumObject = songObject.getJSONObject("album");
                            JSONArray imagesArray = albumObject.getJSONArray("images");
                            String imageUrl = imagesArray.getJSONObject(0).getString("url");

                            JSONArray artistsArray = songObject.getJSONArray("artists");
                            String artistName = artistsArray.getJSONObject(0).getString("name");

                            String duration = songObject.getString("duration_ms");

                            SpotifyTrack track = new SpotifyTrack(songName, artistName, albumObject.getString("name"), duration, imageUrl, songId);
                            recentlyPlayedSongs.add(track);
                        }
                        Log.d("RecentlyPlayedFragment", "Fetching recently played songs successful.");
                        getActivity().runOnUiThread(() -> updateRecentlyPlayedSongs(recentlyPlayedSongs));

                    } catch (JSONException e) {
                        Log.e("RecentlyPlayedFragment", "Failed to parse recently played songs JSON", e);
                    }
                }
            }
        });
    }
}