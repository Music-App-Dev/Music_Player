package com.example.musicplayer;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TopSongsViewModel extends AndroidViewModel {

    private MutableLiveData<ArrayList<SpotifyTrack>> topSongsLiveData = new MutableLiveData<>();
    private boolean topSongsFetched = false;

    public TopSongsViewModel(Application application) {
        super(application);
    }

    public LiveData<ArrayList<SpotifyTrack>> getTopSongs() {
        return topSongsLiveData;
    }

    public void fetchTopSongs(String accessToken) {
        if (topSongsFetched) {
            return;
        }

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https://api.spotify.com/v1/me/top/tracks?limit=20")
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                topSongsFetched = false;
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    try {
                        JSONObject jsonObject = new JSONObject(responseData);
                        JSONArray items = jsonObject.getJSONArray("items");
                        ArrayList<SpotifyTrack> topSongs = new ArrayList<>();

                        for (int i = 0; i < items.length(); i++) {
                            JSONObject trackObject = items.getJSONObject(i);
                            String trackName = trackObject.getString("name");
                            String trackId = trackObject.getString("id");

                            JSONObject albumObject = trackObject.getJSONObject("album");
                            JSONArray imagesArray = albumObject.getJSONArray("images");
                            String albumImageUrl = imagesArray.getJSONObject(0).getString("url");

                            JSONArray artistsArray = trackObject.getJSONArray("artists");
                            String artistName = artistsArray.getJSONObject(0).getString("name");

                            String duration = trackObject.getString("duration_ms");

                            SpotifyTrack spotifyTrack = new SpotifyTrack(
                                    trackName,
                                    artistName,
                                    albumObject.getString("name"),
                                    duration,
                                    albumImageUrl,
                                    trackId
                            );
                            topSongs.add(spotifyTrack);
                        }

                        topSongsFetched = true;
                        topSongsLiveData.postValue(topSongs);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }
}
