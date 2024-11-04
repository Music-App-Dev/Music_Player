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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.MyViewHolder> {

    private Context mContext;
    public static ArrayList<SpotifyTrack> mFiles;


    public MusicAdapter(Context mContext, ArrayList<SpotifyTrack> mFiles){
        this.mFiles = mFiles;
        this.mContext = mContext;
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
        Log.d("ALBUMIMAGEURLS", "Binding track: " + track.getTrackName() + " with Album Image URL: " + track.getAlbumImageUrl());
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
                    showCreatePlaylistDialog();
                    return true;
                }
                return false;
            });
        });
    }

    private void showAddToPlaylistDialog(SpotifyTrack track) {
        SpotifyApiHelper.fetchUserPlaylists(mContext, new SpotifyApiHelper.SpotifyPlaylistsCallback() {
            @Override
            public void onPlaylistFetched(ArrayList<SpotifyPlaylist> playlists) {
                ((Activity) mContext).runOnUiThread(() -> {
                    String[] playlistNames = playlists.stream()
                            .map(SpotifyPlaylist::getPlaylistName)
                            .toArray(String[]::new);
                    String trackUri = "spotify:track:" + track.getTrackId();
                    new AlertDialog.Builder(mContext)
                            .setTitle("Add to Playlist")
                            .setItems(playlistNames, (dialog, which) -> {
                                String selectedPlaylistId = playlists.get(which).getPlaylistId();
                                SpotifyApiHelper.addToPlaylist(mContext, selectedPlaylistId, trackUri, new SpotifyApiHelper.SpotifyPlaylistsCallback() {
                                    @Override
                                    public void onPlaylistFetched(ArrayList<SpotifyPlaylist> playlists) {

                                    }

                                    @Override
                                    public void onTrackAddedToPlaylist() {
                                        ((Activity) mContext).runOnUiThread(() ->
                                                Toast.makeText(mContext, "Track added to playlist!", Toast.LENGTH_SHORT).show()
                                        );
                                    }

                                    @Override
                                    public void onFailure(Exception e) {
                                        ((Activity) mContext).runOnUiThread(() ->
                                                Toast.makeText(mContext, "Failed to add track.", Toast.LENGTH_SHORT).show()
                                        );
                                    }
                                });
                            })
                            .show();
                });
            }

            @Override
            public void onTrackAddedToPlaylist() {

            }

            @Override
            public void onFailure(Exception e) {
                ((Activity) mContext).runOnUiThread(() ->
                        Toast.makeText(mContext, "Failed to fetch playlists.", Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void showCreatePlaylistDialog() {
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
                                public void onPlaylistFetched(ArrayList<SpotifyPlaylist> playlists) {

                                }

                                @Override
                                public void onTrackAddedToPlaylist() {
                                    ((Activity) mContext).runOnUiThread(() ->
                                            Toast.makeText(mContext, "Playlist created!", Toast.LENGTH_SHORT).show()
                                    );
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
