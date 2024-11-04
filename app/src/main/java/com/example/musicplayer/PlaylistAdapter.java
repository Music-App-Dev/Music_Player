package com.example.musicplayer;

import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.content.Context;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.MyHolder> {
    private final Context mContext;
    private final ArrayList<SpotifyPlaylist> playlistFiles;
    View view;
    public PlaylistAdapter(Context mContext, ArrayList<SpotifyPlaylist> playlistFiles){
        this.mContext = mContext;
        this.playlistFiles = playlistFiles;
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        view = LayoutInflater.from(mContext).inflate(R.layout.playlist_item, parent, false);
        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, final int position) {
        SpotifyPlaylist playlist = playlistFiles.get(position);
        holder.playlist_name.setText(playlist.getPlaylistName());

        Glide.with(mContext)
                .load(playlist.getImageUrl())
                .placeholder(R.drawable.tab_indicator) // Placeholder in case image URL is null
                .into(holder.playlist_image);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, PlaylistDetails.class);
                intent.putExtra("playlistId", playlist.getPlaylistId()); // Pass album ID to AlbumDetails activity
                intent.putExtra("playlistName", playlist.getPlaylistName());
                intent.putExtra("playlistImageUrl", playlist.getImageUrl());
                mContext.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return playlistFiles.size();
    }

    public class MyHolder extends RecyclerView.ViewHolder {
        ImageView playlist_image;
        TextView playlist_name;
        public MyHolder(@NonNull View itemView) {
            super(itemView);
            playlist_image = itemView.findViewById(R.id.playlist_image);
            playlist_name = itemView.findViewById(R.id.playlist_name);
        }
    }

    public void updatePlaylist(ArrayList<SpotifyPlaylist> newPlaylists) {
        playlistFiles.clear();
        playlistFiles.addAll(newPlaylists);
    }

}
