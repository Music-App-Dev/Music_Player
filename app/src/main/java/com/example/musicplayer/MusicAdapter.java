package com.example.musicplayer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import androidx.appcompat.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.MyViewHolder> {

    private Context mContext;
    public static ArrayList<SpotifyTrack> mFiles;
    private FragmentManager fragmentManager; // Add FragmentManager

    public MusicAdapter(Context mContext, ArrayList<SpotifyTrack> mFiles, FragmentManager fragmentManager){
        this.mFiles = mFiles;
        this.mContext = mContext;
        this.fragmentManager = fragmentManager;
    }


    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.music_items, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        SpotifyTrack track = mFiles.get(position);
        holder.file_name.setText(track.getTrackName());
        holder.artist_name.setText(track.getArtistName());
        Glide.with(mContext).load(track.getAlbumImageUrl()).into(holder.album_art);
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(mContext, PlayerActivity.class);
            intent.putExtra("position", position); // Pass the exact position
            intent.putParcelableArrayListExtra("trackList", mFiles); // Pass the full list of tracks
            mContext.startActivity(intent);
        });

        holder.menuMore.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(mContext, v);
            popupMenu.getMenuInflater().inflate(R.menu.popup, popupMenu.getMenu());
            popupMenu.show();
            popupMenu.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.add_playlist) {
                    showAddToPlaylistDialog(track);
                    return true;
                } else if (item.getItemId() == R.id.create_playlist) {
                    showCreatePlaylistDialog(track);
                    return true;
                }
                return false;
            });
        });
    }

    private void showAddToPlaylistDialog(SpotifyTrack track) {
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
                                                CombinedFragment combinedFragment = (CombinedFragment) fragmentManager.findFragmentByTag("YOUR_FRAGMENT_TAG");
                                                if (combinedFragment != null) {
                                                    combinedFragment.refreshCombinedList(playlists);
                                                }
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

    public interface OnPlaylistCreatedListener {
        void onPlaylistCreated();
    }

    private OnPlaylistCreatedListener playlistCreatedListener;

    public void setOnPlaylistCreatedListener(OnPlaylistCreatedListener listener) {
        this.playlistCreatedListener = listener;
    }

    private void showCreatePlaylistDialog(SpotifyTrack track) {
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
                                                                CombinedFragment combinedFragment = (CombinedFragment) fragmentManager.findFragmentByTag("YOUR_FRAGMENT_TAG");
                                                                if (combinedFragment != null) {
                                                                    combinedFragment.refreshCombinedList(playlists);
                                                                }
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
                                                if (playlistCreatedListener != null) {
                                                    playlistCreatedListener.onPlaylistCreated();
                                                }
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


    @Override
    public int getItemCount() {
        return mFiles.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        TextView file_name, artist_name;
        ImageView album_art, menuMore;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            file_name = itemView.findViewById(R.id.music_file_name);
            artist_name = itemView.findViewById(R.id.artist_name);
            album_art = itemView.findViewById(R.id.music_img);
            menuMore = itemView.findViewById(R.id.menuMore);
        }
    }

    public void updateList(ArrayList<SpotifyTrack> trackList) {
        mFiles.clear();
        mFiles.addAll(trackList);
        notifyDataSetChanged();
    }
}
