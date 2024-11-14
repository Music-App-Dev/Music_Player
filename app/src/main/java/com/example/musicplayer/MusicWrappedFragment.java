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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MusicWrappedFragment extends Fragment {

    private RecyclerView topSongsRecyclerView;
    private TopSongsAdapter topSongsAdapter;
    private boolean topSongsFetched = false;
    private List<SpotifyWrappedItem> savedTopSongs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_songs, container, false);

        topSongsRecyclerView = view.findViewById(R.id.recyclerView);
        topSongsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));

        topSongsAdapter = new TopSongsAdapter(getContext(), new ArrayList<>());
        topSongsRecyclerView.setAdapter(topSongsAdapter);

        // Check if the saved instance state has top songs data
        if (savedInstanceState != null) {
            List<SpotifyWrappedItem> topSongs = savedInstanceState.getParcelableArrayList("top_songs");
            if (topSongs != null) {
                topSongsFetched = true;
                updateTopSongs(topSongs); // Restore data if available
            }
        } else {
            fetchTopSongs(); // Fetch data if not restored
        }

        return view;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save the top songs list (if fetched) to the bundle
        if (topSongsFetched) {
            outState.putParcelableArrayList("top_songs", new ArrayList<>(savedTopSongs));
        }
    }

    private void fetchTopSongs() {
        if (topSongsFetched) {
            Log.d("MusicWrappedFragment", "Top songs already fetched, skipping fetch.");
            return;
        }

        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("SpotifyAuth", getContext().MODE_PRIVATE);
        String accessToken = sharedPreferences.getString("access_token", null);

        if (accessToken == null) {
            Log.e("MusicWrappedFragment", "Access token is null. Please authenticate.");
            return;
        }

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https://api.spotify.com/v1/me/top/tracks?limit=20")
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("MusicWrappedFragment", "Failed to fetch top songs", e);
                topSongsFetched = false;
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    try {
                        JSONObject jsonObject = new JSONObject(responseData);
                        JSONArray items = jsonObject.getJSONArray("items");
                        List<SpotifyWrappedItem> topSongs = new ArrayList<>();

                        for (int i = 0; i < items.length(); i++) {
                            JSONObject trackObject = items.getJSONObject(i);
                            String songName = trackObject.getString("name");
                            String songId = trackObject.getString("id");

                            JSONObject albumObject = trackObject.getJSONObject("album");
                            JSONArray imagesArray = albumObject.getJSONArray("images");
                            String imageUrl = imagesArray.getJSONObject(0).getString("url");

                            JSONArray artistsArray = trackObject.getJSONArray("artists");
                            String artistName = artistsArray.getJSONObject(0).getString("name");
                            topSongs.add(new SpotifyWrappedItem(songName, songId, "song", imageUrl, artistName));
                        }

                        topSongsFetched = true;
                        savedTopSongs = topSongs;
                        getActivity().runOnUiThread(() -> updateTopSongs(topSongs));

                    } catch (Exception e) {
                        Log.e("MusicWrappedFragment", "Failed to parse top songs JSON", e);
                    }
                } else {
                    Log.e("MusicWrappedFragment", "Failed to fetch top songs, response code: " + response.code());
                }
            }
        });
    }

    public void updateTopSongs(List<SpotifyWrappedItem> songs) {
        savedTopSongs = songs;
        topSongsAdapter.updateData(songs);
    }
}
