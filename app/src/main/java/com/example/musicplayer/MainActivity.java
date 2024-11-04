package com.example.musicplayer;


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

public class MainActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {

    public static final int REQUEST_CODE = 1;
    static ArrayList<SpotifyTrack> musicFiles = new ArrayList<>();
    static ArrayList<SpotifyAlbum> albums = new ArrayList<>();
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
    private AlbumFragment albumFragment;
    private ViewPagerAdapter viewPagerAdapter;

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
                    loadSpotifyTracks(allTracks, trackIds);
                    fetchTracksFromAlbums(allTracks, trackIds);
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
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    try {
                        JSONObject jsonObject = new JSONObject(responseData);
                        JSONArray items = jsonObject.getJSONArray("items");
                        ArrayList<SpotifyAlbum> albumList = new ArrayList<>();

                        for (int i = 0; i < items.length(); i++) {
                            JSONObject albumObject = items.getJSONObject(i).getJSONObject("album");
                            String albumName = albumObject.getString("name");
                            String artistName = albumObject.getJSONArray("artists").getJSONObject(0).getString("name");
                            String imageUrl = albumObject.getJSONArray("images").getJSONObject(0).getString("url");
                            String albumId = albumObject.getString("id");

                            albumList.add(new SpotifyAlbum(albumName, artistName, imageUrl, albumId));
                        }

                        // Update UI with the albumList data (e.g., update a RecyclerView)
                        runOnUiThread(() -> displayAlbums(albumList));

                    } catch (JSONException e) {
                        Log.e("MainActivity", "Failed to parse album JSON", e);
                    }
                } else {
                    Log.e("MainActivity", "Failed with response code: " + response.code());
                }
            }
        });
    }


    private void displayAlbums(ArrayList<SpotifyAlbum> albumList) {
        albums.clear();
        albums.addAll(albumList);
        Log.d("MainActivity", "Displaying " + albums.size() + " albums.");

        AlbumFragment albumFragment = (AlbumFragment) viewPagerAdapter.getFragment(1);
        if (albumFragment != null) {
            albumFragment.updateAlbumList(albums);
        }
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
            songsFragment.updateMusicList(trackList);
        } else {
            Log.e("MainActivity", "SongsFragment is not available to update.");
        }
    }

    private void loadSpotifyData() {
        getUserSavedAlbums();
        loadSpotifyTracks(allTracks, trackIds);
        fetchTracksFromAlbums(allTracks, trackIds);
    }

    protected void startSpotifyAuth() {
        AuthorizationRequest.Builder builder = new AuthorizationRequest.Builder(CLIENT_ID, AuthorizationResponse.Type.TOKEN, REDIRECT_URI);
        builder.setScopes(new String[]{"streaming", "user-library-read", "user-modify-playback-state"});
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
                fetchTracksFromAlbums(allTracks, trackIds);
                initViewPager();
            } else {
                Toast.makeText(this, "Permission Denied. Some features may not work.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void initViewPager() {
        ViewPager viewPager = findViewById(R.id.viewpager);
        TabLayout tabLayout = findViewById(R.id.tab_layout);
        viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());

        // Initialize albumFragment and add it to ViewPager
        albumFragment = new AlbumFragment();
        viewPagerAdapter.addFragments(new SongsFragment(), "Songs");
        viewPagerAdapter.addFragments(albumFragment, "Albums");

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
                } else {
                    Log.e("MainActivity", "Failed with response code: " + response.code());
                    checkAllFetchesCompleted();
                }
            }
        });
    }

    private void fetchTracksFromAlbums(ArrayList<SpotifyTrack> allTracks, HashSet<String> trackIds) {
        String accessToken = getAccessToken();
        OkHttpClient client = new OkHttpClient();
        final int[] completedRequests = {0};

        for (SpotifyAlbum album : albums) {
            Request albumTracksRequest = new Request.Builder()
                    .url("https://api.spotify.com/v1/albums/" + album.getAlbumId() + "/tracks")
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .build();

            client.newCall(albumTracksRequest).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e("MainActivity", "Failed to fetch tracks for album: " + album.getAlbumName(), e);
                    checkAllRequestsCompleted();
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String responseData = response.body().string();
                        try {
                            JSONObject jsonObject = new JSONObject(responseData);
                            JSONArray items = jsonObject.getJSONArray("items");

                            synchronized (allTracks) {
                                for (int i = 0; i < items.length(); i++) {
                                    JSONObject trackObject = items.getJSONObject(i);
                                    String trackId = trackObject.getString("id");

                                    if (!trackIds.contains(trackId)) {
                                        allTracks.add(parseTrackObject(trackObject, album));
                                        trackIds.add(trackId);
                                    }
                                }
                            }

                            Log.d("MainActivity", "Tracks fetched from album: " + album.getAlbumName() + ", Tracks: " + items.length());

                        } catch (JSONException e) {
                            Log.e("MainActivity", "Failed to parse album tracks JSON", e);
                        }
                    } else {
                        Log.e("MainActivity", "Failed with response code: " + response.code());
                    }
                    checkAllRequestsCompleted();
                }

                private void checkAllRequestsCompleted() {
                    synchronized (completedRequests) {
                        completedRequests[0]++;
                        if (completedRequests[0] == albums.size()) {
                            checkAllFetchesCompleted();
                        }
                    }
                }
            });
        }
    }

    private void checkAllFetchesCompleted() {
        if (allTracks.isEmpty() || albums.size() == 0) {
            Log.d("MainActivity", "No tracks to display or no albums found.");
            return;
        }

        runOnUiThread(() -> {
            SharedPreferences preferences = getSharedPreferences(MY_SORT_PREF, MODE_PRIVATE);
            String sortOrder = preferences.getString("sorting", "sortByName");
            sortSpotifyTracks(allTracks, sortOrder);
            displayTracks(allTracks);
        });
    }

    private SpotifyTrack parseTrackObject(JSONObject trackObject, @Nullable SpotifyAlbum album) throws JSONException {
        String trackName = trackObject.getString("name");
        String artistName = trackObject.getJSONArray("artists").getJSONObject(0).getString("name");
        String albumName = album != null ? album.getAlbumName() : trackObject.getJSONObject("album").getString("name");
        String albumImageUrl = album != null ? album.getImageUrl() : trackObject.getJSONObject("album").getJSONArray("images").getJSONObject(0).getString("url");
        String trackId = trackObject.getString("id");
        String duration = trackObject.getString("duration_ms");

        return new SpotifyTrack(trackName, artistName, albumName, duration, albumImageUrl, trackId);
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search, menu);
        MenuItem menuItem = menu.findItem(R.id.search_option);
        SearchView searchView = (SearchView) menuItem.getActionView();
        searchView.setOnQueryTextListener(this);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        String userInput = newText.toLowerCase();
        ArrayList<SpotifyTrack> myFiles = new ArrayList<>();

        for(SpotifyTrack song : musicFiles){
            if (song.getTrackName().toLowerCase().contains(userInput)){
                myFiles.add(song);
            }
        }
        if (newText.isEmpty()) {
            SongsFragment.musicAdapter.updateList(musicFiles);  // Reset to full list if search is cleared
        } else {
            SongsFragment.musicAdapter.updateList(myFiles); // Show filtered list
        }
        SongsFragment.musicAdapter.notifyDataSetChanged();
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
        }

        if (sortOrder != null) {
            editor.putString("sorting", sortOrder);
            editor.apply();

            loadSpotifyTracks(allTracks, trackIds);
            fetchTracksFromAlbums(allTracks, trackIds);  // Reload the tracks with the new sort order
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
