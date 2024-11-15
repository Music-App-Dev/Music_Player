package com.example.musicplayer;

import com.example.musicplayer.MusicWrappedFragment;
import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;


import com.google.android.material.tabs.TabLayout;
import com.spotify.android.appremote.api.SpotifyAppRemote;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;

import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.PlayerState;
import com.spotify.protocol.types.Track;
import com.spotify.sdk.android.auth.AuthorizationClient;
import com.spotify.sdk.android.auth.AuthorizationRequest;
import com.spotify.sdk.android.auth.AuthorizationResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;



public class MainActivity extends AppCompatActivity implements SearchView.OnQueryTextListener, RefreshListener {

    public static final int REQUEST_CODE = 1;
    static ArrayList<SpotifyTrack> musicFiles = new ArrayList<>();
    static ArrayList<SpotifyItem> combPlayAlbums = new ArrayList<>();
    static boolean shuffleBoolean = false, repeatBoolean = false;
    private String MY_SORT_PREF = "SortOrder";
    public static final String MUSIC_FILE_LAST_PLAYED = "LAST_PLAYED";
    public static final String MUSIC_FILE = "STORED_MUSIC";
    public static boolean SHOW_MINI_PLAYER = false;

    public static String PATH_TO_FRAG = null;
    public static String ARTIST_TO_FRAG = null;
    public static String SONG_TO_FRAG = null;
    public static final String ARTIST_NAME = "ARTIST_NAME";
    public static final String SONG_NAME = "SONG_NAME";

    private static final String CLIENT_ID = "1e6a19b8b3364441b502d3c8c427ed6f";
    private static final String REDIRECT_URI = "com.example.musicplayer://callback";
    private SpotifyAppRemote spotifyAppRemote;

    private ViewPagerAdapter viewPagerAdapter;

    private boolean albumsFetched = false;
    private boolean playlistsFetched = false;

    private int likedTracksFetched = 0;
    private int albumTracksFetched = 0;
    private int playlistTracksFetched = 0;

    private ArrayList<SpotifyTrack> allTracks = new ArrayList<>();
    private HashSet<String> trackIds = new HashSet<>();

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission();
        }

        startSpotifyAuth();
        initViewPager();

        String accessToken = getAccessToken();
        if (accessToken != null) {
            loadSpotifyData();  // Fetch albums and tracks immediately if already authenticated
             // Fetch top artists
            getUserTopSongs();

            MusicWrappedFragment fragment = new MusicWrappedFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
        }

        }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        Uri uri = intent.getData();
        if (uri != null && uri.toString().startsWith(REDIRECT_URI)) {
            AuthorizationResponse response = AuthorizationResponse.fromUri(uri);

            switch (response.getType()) {
                case TOKEN:
                    String accessToken = response.getAccessToken();
                    Log.d("MainActivity", "Access Token received: " + accessToken);
                    saveAccessToken(accessToken);
                    fetchAllSpotifyData();
                    connectToSpotify();
                    break;

                case ERROR:
                    Log.e("MainActivity", "Authorization error: " + response.getError());
                    break;
            }
        }
    }

    private void saveAccessToken(String token) {
        SharedPreferences sharedPreferences = getSharedPreferences("SpotifyAuth", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("access_token", token);
        editor.apply();
    }

    public String getAccessToken() {
        SharedPreferences sharedPreferences = getSharedPreferences("SpotifyAuth", MODE_PRIVATE);
        return sharedPreferences.getString("access_token", null);
    }

    private void getUserSavedAlbums() {
        if (albumsFetched) {
            Log.d("MainActivity", "Albums already fetched, skipping fetch.");
            return;
        }

        SharedPreferences sharedPreferences = getSharedPreferences("SpotifyAuth", MODE_PRIVATE);
        String accessToken = sharedPreferences.getString("access_token", null);

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https://api.spotify.com/v1/me/albums")
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("MainActivity", "Failed to fetch albums", e);
                albumsFetched = false;  // Allow re-fetch on failure
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    albumsFetched = true;  // Set to true only on success
                    String responseData = response.body().string();
                    try {
                        JSONObject jsonObject = new JSONObject(responseData);
                        JSONArray items = jsonObject.getJSONArray("items");
                        ArrayList<SpotifyItem> albumList = new ArrayList<>();

                        for (int i = 0; i < items.length(); i++) {
                            JSONObject albumObject = items.getJSONObject(i).getJSONObject("album");
                            String albumName = albumObject.getString("name");
                            String artistName = albumObject.getJSONArray("artists").getJSONObject(0).getString("name");
                            String imageUrl = albumObject.getJSONArray("images").getJSONObject(0).getString("url");
                            String albumId = albumObject.getString("id");

                            albumList.add(new SpotifyAlbum(albumName, artistName, imageUrl, albumId));
                        }

                        runOnUiThread(() -> displayCombined(albumList));

                    } catch (JSONException e) {
                        Log.e("MainActivity", "Failed to parse album JSON", e);
                    }
                } else {
                    albumsFetched = false;  // Allow re-fetch if the response is unsuccessful
                    if (response.code() == 429) {
                        String retryAfter = response.header("Retry-After");
                        Log.e("MainActivity", "Rate limit exceeded. Retry after: " + retryAfter + " seconds.");
                    } else {
                        Log.e("MainActivity", "Failed with response code: " + response.code());
                    }
                }
            }
        });
    }

    private void getUserSavedPlaylists() {
        if (playlistsFetched) {
            Log.d("MainActivity", "Playlists already fetched, skipping fetch.");
            return;
        }

        SharedPreferences sharedPreferences = getSharedPreferences("SpotifyAuth", MODE_PRIVATE);
        String accessToken = sharedPreferences.getString("access_token", null);

        if (accessToken == null) {
            Log.e("MainActivity", "Access token is null. Please authenticate.");
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
                Log.e("MainActivity", "Failed to fetch playlists", e);
                playlistsFetched = false;  // Reset flag to allow retry on failure
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    try {
                        JSONObject jsonObject = new JSONObject(responseData);
                        JSONArray items = jsonObject.getJSONArray("items");
                        ArrayList<SpotifyItem> playList = new ArrayList<>();

                        for (int i = 0; i < items.length(); i++) {
                            JSONObject playlistObject = items.getJSONObject(i);
                            String playlistName = playlistObject.optString("name", "Unknown Playlist");
                            String playlistId = playlistObject.optString("id", "No ID");

                            JSONArray images = playlistObject.optJSONArray("images");
                            String imageUrl = (images != null && images.length() > 0) ?
                                    images.getJSONObject(0).optString("url", "No Image") : "No Image";

                            playList.add(new SpotifyPlaylist(playlistName, imageUrl, playlistId));
                        }

                        playlistsFetched = true;  // Set to true only after successful fetch
                        runOnUiThread(() -> displayCombined(playList));

                    } catch (JSONException e) {
                        Log.e("MainActivity", "Failed to parse playlist JSON", e);
                        playlistsFetched = false;  // Reset on JSON parse error
                    }
                } else if (response.code() == 429) {
                    String retryAfter = response.header("Retry-After");
                    Log.e("MainActivity", "Rate limit exceeded. Retry after: " + retryAfter + " seconds.");
                    playlistsFetched = false;  // Allow retry if rate limit was hit
                } else {
                    Log.e("MainActivity", "Failed with response code: " + response.code());
                    playlistsFetched = false;  // Reset if any other error occurs
                }
            }
        });
    }

    private void displayCombined(ArrayList<SpotifyItem> combinedList) {
        combPlayAlbums.addAll(combinedList); // Add albums to combined list
        Log.d("MainActivity", "Displaying " + combPlayAlbums.size() + " albums.");
        displayCombinedItems(); // Call the method to update the combined view
    }


    private void getUserTopSongs() {
        SharedPreferences sharedPreferences = getSharedPreferences("SpotifyAuth", MODE_PRIVATE);
        String accessToken = sharedPreferences.getString("access_token", null);

        if (accessToken == null) {
            Log.e("MainActivity", "Access token is null. Please authenticate.");
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
                Log.e("MainActivity", "Failed to fetch top songs", e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    try {
                        JSONObject jsonObject = new JSONObject(responseData);
                        JSONArray items = jsonObject.getJSONArray("items");
                        ArrayList<SpotifyTrack> topSongs = new ArrayList<>();

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
                            topSongs.add(track);
                        }

                        runOnUiThread(() -> updateTopSongs(topSongs));

                    } catch (JSONException e) {
                        Log.e("MainActivity", "Failed to parse top songs JSON", e);
                    }
                }
            }
        });
    }

    private void updateTopSongs(List<SpotifyTrack> topSongs) {
        RecyclerView recyclerViewTopSongs = findViewById(R.id.recyclerViewTopSongs);

        MusicAdapter musicAdapter = new MusicAdapter(this, new ArrayList<>(topSongs), getSupportFragmentManager());

        recyclerViewTopSongs.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewTopSongs.setAdapter(musicAdapter);
    }







    private void connectToSpotify() {
        ConnectionParams connectionParams = new ConnectionParams.Builder(CLIENT_ID)
                .setRedirectUri(REDIRECT_URI)
                .showAuthView(true)
                .build();
        SpotifyAppRemote.connect(this, connectionParams, new Connector.ConnectionListener() {
            @Override
            public void onConnected(SpotifyAppRemote remote) {
                spotifyAppRemote = remote;
                Log.d("MainActivity", "Connected to Spotify");

                // Pass spotifyAppRemote to MusicService for playback control
                Intent serviceIntent = new Intent(MainActivity.this, MusicService.class);
                MusicService.setSpotifyAppRemote(spotifyAppRemote);
                startService(serviceIntent);
            }

            @Override
            public void onFailure(Throwable error) {
                Log.e("MainActivity", "Failed to connect to Spotify", error);
            }
        });
    }

    private void displayTracks(ArrayList<SpotifyTrack> trackList) {
        musicFiles.clear();
        musicFiles.addAll(trackList);
        Log.d("MainActivity", "Displaying " + musicFiles.size() + " tracks.");

        SongsFragment songsFragment = (SongsFragment) viewPagerAdapter.getFragment(0);
        if (songsFragment != null) {
            songsFragment.setFullMusicList(trackList);
        } else {
            Log.e("MainActivity", "SongsFragment is not available to update.");
        }
    }

    private void loadSpotifyData() {
        allTracks.clear();
        trackIds.clear();
        loadSpotifyTracks(allTracks, trackIds);
        getUserSavedAlbums();
        getUserSavedPlaylists();
    }


    private void displayCombinedItems() {
        runOnUiThread(() -> {
            CombinedFragment combinedFragment = (CombinedFragment) viewPagerAdapter.getFragment(1); // Adjust the index to match the position of CombinedFragment
            if (combinedFragment != null) {
                combinedFragment.updateAlbumPlayList(combPlayAlbums); // Update the combined list in the fragment
            } else {
                Log.e("MainActivity", "CombinedFragment is not available to update.");
            }
        });
    }

    @Override
    public void onRefresh() {
        allTracks.clear();
        trackIds.clear();
        playlistsFetched = false;
        albumsFetched = false;
        // Fetch liked tracks
        loadSpotifyTracks(allTracks, trackIds);
        // Display combined items after all fetching completes
        displayCombinedItems();
    }

    protected void startSpotifyAuth() {
        AuthorizationRequest.Builder builder = new AuthorizationRequest.Builder(CLIENT_ID, AuthorizationResponse.Type.TOKEN, REDIRECT_URI);
        builder.setScopes(new String[]{
                "streaming",
                "user-library-read",
                "user-library-modify",
                "user-modify-playback-state",
                "playlist-modify-public",
                "playlist-modify-private",
                "playlist-read-collaborative",
                "playlist-read-private",
                "user-top-read"
        });

        AuthorizationRequest request = builder.build();
        AuthorizationClient.openLoginActivity(this, REQUEST_CODE, request);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE) {
            AuthorizationResponse response = AuthorizationClient.getResponse(resultCode, data);
            switch (response.getType()) {
                case TOKEN:
                    String accessToken = response.getAccessToken();
                    Log.d("MainActivity", "Access Token received: " + accessToken);
                    saveAccessToken(accessToken);
                    connectToSpotify();
                    loadSpotifyData();
                    break;
                case ERROR:
                    Log.e("MainActivity", "Authorization error: " + response.getError());
                    Toast.makeText(this, "Authorization failed: " + response.getError(), Toast.LENGTH_LONG).show();
                    break;
                default:
                    Log.e("MainActivity", "Unknown response type: " + response.getType());
                    Toast.makeText(this, "Authorization response type: " + response.getType(), Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void permission() {
        ArrayList<String> permissions = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO);
        }
        if (!permissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toArray(new String[0]), REQUEST_CODE);
        } else {
            initViewPager();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            if (allPermissionsGranted) {
                loadSpotifyTracks(allTracks, trackIds);
                initViewPager();
            } else {
                Toast.makeText(this, "Permission Denied. Some features may not work.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void fetchAllSpotifyData() {
        // Clear allTracks and trackIds, but not combPlayAlbums
        allTracks.clear();
        trackIds.clear();
        // Fetch albums and playlists metadata first
        getUserSavedAlbums();
        getUserSavedPlaylists();

        // Fetch liked tracks
        loadSpotifyTracks(allTracks, trackIds);

        // Display combined items after all fetching completes
        displayCombinedItems();
    }

    private void initViewPager() {
        ViewPager viewPager = findViewById(R.id.viewpager);
        TabLayout tabLayout = findViewById(R.id.tab_layout);

        viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());

        // Initialize albumFragment and add it to ViewPager

        viewPagerAdapter.addFragments(new SongsFragment(), "Liked Songs");
        viewPagerAdapter.addFragments(new CombinedFragment(), "Albums/Playlists");
        viewPagerAdapter.addFragments(new MusicWrappedFragment(), "Your Top 20");

        viewPager.setAdapter(viewPagerAdapter);
        tabLayout.setupWithViewPager(viewPager);
    }

    public static class ViewPagerAdapter extends FragmentPagerAdapter {

        private ArrayList<Fragment> fragments;
        private ArrayList<String> titles;
        public ViewPagerAdapter(@NonNull FragmentManager fm) {
            super(fm);
            this.fragments = new ArrayList<>();
            this.titles = new ArrayList<>();
        }

        public Fragment getFragment(int position) {
            return fragments.get(position);
        }

        void addFragments(Fragment fragment, String title){
            fragments.add(fragment);
            titles.add(title);
        }
        @NonNull
        @Override
        public Fragment getItem(int position) {
            return fragments.get(position);
        }

        @Override
        public int getCount() {
            return fragments.size();
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position){
            return titles.get(position);
        }
    }

    private void loadSpotifyTracks(ArrayList<SpotifyTrack> allTracks, HashSet<String> trackIds) {
        SharedPreferences sharedPreferences = getSharedPreferences("SpotifyAuth", MODE_PRIVATE);
        String accessToken = sharedPreferences.getString("access_token", null);

        if (accessToken == null) {
            Log.e("MainActivity", "Access token is missing or invalid. Re-authentication may be required.");
            return;
        }

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https://api.spotify.com/v1/me/tracks")
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("MainActivity", "Failed to fetch liked tracks", e);
                checkAllFetchesCompleted();  // Check completion to update the UI even if this request fails
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    likedTracksFetched = 1;
                    checkIfAllFetchesCompleted();

                    String responseData = response.body().string();
                    try {
                        JSONObject jsonObject = new JSONObject(responseData);
                        JSONArray items = jsonObject.getJSONArray("items");

                        synchronized (allTracks) {
                            for (int i = 0; i < items.length(); i++) {
                                JSONObject trackObject = items.getJSONObject(i).getJSONObject("track");
                                String trackId = trackObject.getString("id");

                                if (!trackIds.contains(trackId)) {
                                    allTracks.add(parseTrackObject(trackObject, null));  // Pass null for album
                                    trackIds.add(trackId);
                                }
                            }
                        }

                        Log.d("MainActivity", "Liked tracks fetched: " + items.length());
                        checkAllFetchesCompleted();

                    } catch (JSONException e) {
                        Log.e("MainActivity", "Failed to parse liked tracks JSON", e);
                        checkAllFetchesCompleted();
                    }
                } else if (response.code() == 429) {
                    String retryAfter = response.header("Retry-After");
                    Log.e("MainActivity", "Rate limit exceeded. Retry after: " + retryAfter + " seconds.");
                } else {
                    Log.e("MainActivity", "Failed with response code: " + response.code());
                    checkAllFetchesCompleted();
                }
            }
        });
    }

    private void checkIfAllFetchesCompleted() {
        Log.d("CHECKFETCH", "FETCH CHECK: " + likedTracksFetched + " ALBUM " + albumTracksFetched + "PLAYLISTS" + playlistTracksFetched);
        if (likedTracksFetched > 0 && albumTracksFetched > 0 && playlistTracksFetched > 0) {
            runOnUiThread(() -> {
                SharedPreferences preferences = getSharedPreferences(MY_SORT_PREF, MODE_PRIVATE);
                String sortOrder = preferences.getString("sorting", "sortByName");
                sortSpotifyTracks(allTracks, sortOrder);
                displayTracks(allTracks);
            });
        } else {
            Log.d("MainActivity", "Waiting for all fetches to complete.");
        }
    }


    private void checkAllFetchesCompleted() {
        if (allTracks.isEmpty() || combPlayAlbums.isEmpty()) {
            Log.d("MainActivity", "No tracks to display, playlists, or no albums found.");
            return;
        }

        runOnUiThread(() -> {
            SharedPreferences preferences = getSharedPreferences(MY_SORT_PREF, MODE_PRIVATE);
            String sortOrder = preferences.getString("sorting", "sortByName");
            sortSpotifyTracks(allTracks, sortOrder);
            displayTracks(allTracks);
        });
    }

    private SpotifyTrack parseTrackObject(JSONObject trackObject, @Nullable SpotifyItem item) throws JSONException {
        String trackName = trackObject.getString("name");
        String artistName = trackObject.getJSONArray("artists").getJSONObject(0).getString("name");
        String albumName = item != null ? item.getName() : trackObject.getJSONObject("album").getString("name");
        String albumImageUrl = item != null ? item.getImageUrl() : trackObject.getJSONObject("album").getJSONArray("images").getJSONObject(0).getString("url");
        String trackId = trackObject.getString("id");
        String duration = trackObject.getString("duration_ms");

        return new SpotifyTrack(trackName, artistName, albumName, duration, albumImageUrl, trackId);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search, menu);
        MenuItem searchItem = menu.findItem(R.id.search_option);
        searchItem.setOnMenuItemClickListener(item -> {
            Intent intent = new Intent(MainActivity.this, SearchActivity.class);
            startActivity(intent);
            return true;
        });

        MenuItem refreshItem = menu.findItem((R.id.refresh_option));
        refreshItem.setOnMenuItemClickListener(item -> {
            loadSpotifyData();
            return true;
        });

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        String userInput = newText.toLowerCase();
        SongsFragment songsFragment = (SongsFragment) viewPagerAdapter.getFragment(0);

        if (songsFragment == null) {
            Log.e("MainActivity", "SongsFragment is not available to update.");
            return false;
        }

        if (userInput.isEmpty()) {
            // Reset to the original list when the search query is cleared
            songsFragment.updateMusicList(songsFragment.getFullMusicList());
        } else {
            // Filter the full list
            ArrayList<SpotifyTrack> filteredList = new ArrayList<>();
            for (SpotifyTrack song : songsFragment.getFullMusicList()) {
                if (song.getTrackName().toLowerCase().contains(userInput)) {
                    filteredList.add(song);
                }
            }
            songsFragment.updateMusicList(filteredList);
        }

        // Refresh the adapter to show the updated list
        songsFragment.getAdapter().notifyDataSetChanged();  // Make sure to implement getAdapter if needed
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        SharedPreferences.Editor editor = getSharedPreferences(MY_SORT_PREF, MODE_PRIVATE).edit();
        String sortOrder = null;

        if (item.getItemId() == R.id.by_name) {
            sortOrder = "sortByName";
        } else if (item.getItemId() == R.id.by_album) {
            sortOrder = "sortByAlbum";
        } else if (item.getItemId() == R.id.by_duration) {
            sortOrder = "sortByDuration";
        } else if (item.getItemId() == R.id.search_option) { // Handle search menu item click
            Intent intent = new Intent(this, SearchActivity.class);
            startActivity(intent); // Start the SearchActivity
            return true;
        }

        if (sortOrder != null) {
            editor.putString("sorting", sortOrder);
            editor.apply();
            fetchAllSpotifyData();
        }

        return super.onOptionsItemSelected(item);
    }

    private void sortSpotifyTracks(ArrayList<SpotifyTrack> tracks, String sortOrder) {
        switch (sortOrder) {
            case "sortByName":
                tracks.sort((t1, t2) -> t1.getTrackName().compareToIgnoreCase(t2.getTrackName()));
                break;
            case "sortByAlbum":
                tracks.sort((t1, t2) -> t1.getAlbumName().compareToIgnoreCase(t2.getAlbumName()));
                break;
            default:
                Log.w("SortDebug", "Unknown sort order: " + sortOrder);
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences preferences = getSharedPreferences(MUSIC_FILE_LAST_PLAYED, MODE_PRIVATE);
        String path = preferences.getString(MUSIC_FILE, null);
        String artist = preferences.getString(ARTIST_NAME, null);
        String song = preferences.getString(SONG_NAME, null);

        if(path != null){
            SHOW_MINI_PLAYER = true;
            PATH_TO_FRAG = path;
            ARTIST_TO_FRAG = artist;
            SONG_TO_FRAG = song;

        } else {
            SHOW_MINI_PLAYER = false;
            PATH_TO_FRAG = null;
            ARTIST_TO_FRAG = null;
            SONG_TO_FRAG = null;
        }
    }
}