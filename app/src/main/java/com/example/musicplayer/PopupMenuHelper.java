package com.example.musicplayer;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.FragmentManager;

import java.util.ArrayList;

public class PopupMenuHelper {

    private static Context mContext;

    public static void showPopupMenu(Context context, View anchor, SpotifyTrack track) {
        mContext = context;
        PopupMenu popupMenu = new PopupMenu(context, anchor);
        popupMenu.getMenuInflater().inflate(R.menu.popup, popupMenu.getMenu());

        popupMenu.show();
        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.add_playlist) {
                showAddToPlaylistDialog(track);
                return true;
            } else if (item.getItemId() == R.id.create_playlist) {
                showCreatePlaylistDialog(track);
                return true;
            } else if (item.getItemId() == R.id.add_queue) {
                QueueManager.addTrackToQueue(track);
                Toast.makeText(context, "Added to Queue: " + track.getTrackName(), Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });
    }

    private static void showAddToPlaylistDialog(SpotifyTrack track) {
        SpotifyApiHelper.fetchUserPlaylists(mContext, new SpotifyApiHelper.SpotifyPlaylistsCallback() {
            @Override
            public void onPlaylistFetched(ArrayList<SpotifyItem> playlists) {
                ((Activity) mContext).runOnUiThread(() -> {


                    // Inflate the dialog layout
                    LayoutInflater inflater = LayoutInflater.from(mContext);
                    View dialogView = inflater.inflate(R.layout.dialog_playlist_search, null);


                    // Find views
                    SearchView searchView = dialogView.findViewById(R.id.searching_view);
                    ListView listView = dialogView.findViewById(R.id.listing_view);

                    // Create an adapter for the ListView
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(mContext,
                            android.R.layout.simple_list_item_1,
                            playlists.stream().map(SpotifyItem::getName).toArray(String[]::new));
                    listView.setAdapter(adapter);

                    // Filter the ListView based on SearchView input
                    searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                        @Override
                        public boolean onQueryTextSubmit(String query) {
                            return false; // Let SearchView handle submission
                        }

                        @Override
                        public boolean onQueryTextChange(String newText) {
                            adapter.getFilter().filter(newText);
                            return false; // Return false to let SearchView handle changes
                        }
                    });

                    // Handle item clicks
                    listView.setOnItemClickListener((parent, view, position, id) -> {
                        String selectedPlaylistName = adapter.getItem(position);
                        SpotifyItem selectedPlaylist = playlists.stream()
                                .filter(item -> item.getName().equals(selectedPlaylistName))
                                .findFirst()
                                .orElse(null);

                        if (selectedPlaylist != null) {
                            String trackUri = "spotify:track:" + track.getTrackId();
                            SpotifyApiHelper.addToPlaylist(mContext, selectedPlaylist.getId(), trackUri, new SpotifyApiHelper.SpotifyPlaylistsCallback() {
                                @Override
                                public void onTrackAddedToPlaylist() {
                                    ((Activity) mContext).runOnUiThread(() -> {
                                                Toast.makeText(mContext, "Track added to playlist!", Toast.LENGTH_SHORT).show();
                                            }
                                    );
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    ((Activity) mContext).runOnUiThread(() ->
                                            Toast.makeText(mContext, "Failed to add track.", Toast.LENGTH_SHORT).show()
                                    );
                                }

                                @Override
                                public void onPlaylistFetched(ArrayList<SpotifyItem> playlists) {
                                    // Not used for adding track
                                }
                            });
                        }
                    });

                    // Show the dialog
                    new AlertDialog.Builder(mContext)
                            .setTitle("Add to Playlist")
                            .setView(dialogView)
                            .setNegativeButton("Cancel", null)
                            .show();

                });


            }

            @Override
            public void onTrackAddedToPlaylist() {
                // Not used here
            }

            @Override
            public void onFailure(Exception e) {
                ((Activity) mContext).runOnUiThread(() ->
                        Toast.makeText(mContext, "Failed to fetch playlists.", Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private static void showCreatePlaylistDialog(SpotifyTrack track) {
        EditText input = new EditText(mContext);
        new AlertDialog.Builder(mContext)
                .setTitle("Create Playlist")
                .setMessage("Enter playlist name:")
                .setView(input)
                .setPositiveButton("Create", (dialog, which) -> {
                    String playlistName = input.getText().toString().trim();

                    SpotifyApiHelper.fetchUserId(mContext, new SpotifyApiHelper.SpotifyUserIdCallback() {
                        @Override
                        public void onSuccess(String userId) {
                            SpotifyApiHelper.createPlaylist(mContext, userId, playlistName, "New playlist created", new SpotifyApiHelper.SpotifyPlaylistsCallback() {

                                @Override
                                public void onPlaylistFetched(ArrayList<SpotifyItem> playlists) {

                                }

                                @Override
                                public void onTrackAddedToPlaylist() {
                                    ((Activity) mContext).runOnUiThread(() ->
                                            Toast.makeText(mContext, "Playlist created!", Toast.LENGTH_SHORT).show()
                                    );

                                    // After playlist creation, fetch all playlists
                                    SpotifyApiHelper.fetchUserPlaylists(mContext, new SpotifyApiHelper.SpotifyPlaylistsCallback() {

                                        @Override
                                        public void onPlaylistFetched(ArrayList<SpotifyItem> playlists) {
                                            for (SpotifyItem playlist : playlists) {
                                                if (playlist.getName().equals(playlistName)) {
                                                    // Add the track to this playlist
                                                    String trackUri = "spotify:track:" + track.getTrackId();
                                                    SpotifyApiHelper.addTrackToPlaylist(mContext, playlist.getId(), trackUri, new SpotifyApiHelper.SpotifyPlaylistsCallback() {
                                                        @Override
                                                        public void onPlaylistFetched(ArrayList<SpotifyItem> playlists) {

                                                        }
                                                        @Override
                                                        public void onTrackAddedToPlaylist() {
                                                            ((Activity) mContext).runOnUiThread(() -> {
                                                                Toast.makeText(mContext, "Track added to playlist!", Toast.LENGTH_SHORT).show();
                                                            });
                                                        }

                                                        @Override
                                                        public void onFailure(Exception e) {
                                                            ((Activity) mContext).runOnUiThread(() ->
                                                                    Toast.makeText(mContext, "Failed to add track to playlist.", Toast.LENGTH_SHORT).show()
                                                            );
                                                        }
                                                    });
                                                    break;
                                                }
                                            }
                                            ((Activity) mContext).runOnUiThread(() -> {
                                                Toast.makeText(mContext, "Track added to playlist!", Toast.LENGTH_SHORT).show();
                                            });
                                        }
                                        @Override
                                        public void onTrackAddedToPlaylist() {
                                            // Not used here
                                        }

                                        @Override
                                        public void onFailure(Exception e) {
                                            ((Activity) mContext).runOnUiThread(() ->
                                                    Toast.makeText(mContext, "Failed to fetch playlists.", Toast.LENGTH_SHORT).show()
                                            );
                                        }
                                    });
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    ((Activity) mContext).runOnUiThread(() ->
                                            Toast.makeText(mContext, "Failed to create playlist.", Toast.LENGTH_SHORT).show()
                                    );
                                }
                            });
                        }

                        @Override
                        public void onFailure(Exception e) {
                            ((Activity) mContext).runOnUiThread(() ->
                                    Toast.makeText(mContext, "Failed to fetch user ID.", Toast.LENGTH_SHORT).show()
                            );
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}