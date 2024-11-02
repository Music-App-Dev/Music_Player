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

        String accessToken = getAccessToken(this);
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
                    loadSpotifyData();  // Ensure data is loaded after token retrieval
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

    public static String getAccessToken(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("SpotifyAuth", MODE_PRIVATE);
        return sharedPreferences.getString("access_token", null); // Returns null if the token isn't stored
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

        // Log each album's details
//        for (int i = 0; i < albumList.size(); i++) {
//            SpotifyAlbum album = albumList.get(i);
//            Log.d("AlbumInfo", "Album " + (i + 1) + ":");
//            Log.d("AlbumInfo", "  Name: " + album.getAlbumName());
//            Log.d("AlbumInfo", "  Artist: " + album.getArtistName());
//            Log.d("AlbumInfo", "  Image URL: " + album.getImageUrl());
//            Log.d("AlbumInfo", "  Album ID: " + album.getAlbumId());
//        }

        // Check if AlbumFragment exists and update its data
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
        loadSpotifyTracks();
    }

    protected void startSpotifyAuth() {
        AuthorizationRequest.Builder builder = new AuthorizationRequest.Builder(CLIENT_ID, AuthorizationResponse.Type.TOKEN, REDIRECT_URI);
        builder.setScopes(new String[]{"streaming", "user-library-read"});
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
                loadSpotifyTracks();
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

    private void loadSpotifyTracks() {
        SharedPreferences sharedPreferences = getSharedPreferences("SpotifyAuth", MODE_PRIVATE);
        String accessToken = sharedPreferences.getString("access_token", null);

        if (accessToken == null) {
            Log.e("MainActivity", "Access token is missing or invalid. Re-authentication may be required.");
            return; // Exit if there's no valid access token
        }

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https://api.spotify.com/v1/me/tracks")
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("MainActivity", "Failed to fetch tracks", e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    Log.d("MainActivity", "Spotify API Response: " + responseData);  // Log the full response

                    try {
                        JSONObject jsonObject = new JSONObject(responseData);
                        JSONArray items = jsonObject.getJSONArray("items");

                        // Initialize musicFiles and add tracks
                        ArrayList<SpotifyTrack> muFiles = new ArrayList<>();
                        for (int i = 0; i < items.length(); i++) {
                            JSONObject trackObject = items.getJSONObject(i).getJSONObject("track");

                            String trackName = trackObject.getString("name");
                            String artistName = trackObject.getJSONArray("artists").getJSONObject(0).getString("name");
                            String albumName = trackObject.getJSONObject("album").getString("name");
                            String albumImageUrl = trackObject.getJSONObject("album").getJSONArray("images").getJSONObject(0).getString("url");
                            String trackId = trackObject.getString("id");
                            String duration = trackObject.getString("duration_ms");

                            muFiles.add(new SpotifyTrack(trackName, artistName, albumName, duration, albumImageUrl, trackId));
                        }

                        Log.d("MainActivity", "Fetched " + muFiles.size() + " tracks from Spotify.");  // Log the number of fetched tracks
                        runOnUiThread(() -> displayTracks(muFiles));

                    } catch (JSONException e) {
                        Log.e("MainActivity", "Failed to parse track JSON", e);
                    }
                } else {
                    Log.e("MainActivity", "Failed with response code: " + response.code());
                }
            }
        });
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
        SongsFragment.musicAdapter.updateList(myFiles);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        SharedPreferences.Editor editor = getSharedPreferences(MY_SORT_PREF, MODE_PRIVATE).edit();
        if (item.getItemId() == R.id.by_name) {
            editor.putString("sorting", "sortByName");
            editor.apply();
            this.recreate();
        } else if (item.getItemId() == R.id.by_date) {
            editor.putString("sorting", "sortByDate");
            editor.apply();
            this.recreate();
        } else if (item.getItemId() == R.id.by_duration) {
            editor.putString("sorting", "sortByDuration");
            editor.apply();
            this.recreate();
        }
        return super.onOptionsItemSelected(item);
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
