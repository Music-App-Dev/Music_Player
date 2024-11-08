package com.example.musicplayer;

import android.content.Context;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

public class PlaylistDetailsAdapter extends RecyclerView.Adapter<PlaylistDetailsAdapter.MyHolder> {
    private final Context mContext;
    public static ArrayList<SpotifyTrack> playlistTracks;
    View view;
    public PlaylistDetailsAdapter(Context mContext, ArrayList<SpotifyTrack> playlistTracks){
        this.mContext = mContext;
        this.playlistTracks = playlistTracks;
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        view = LayoutInflater.from(mContext).inflate(R.layout.music_items, parent, false);
        return new MyHolder(view);
    }



    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, int position) {
        SpotifyTrack track = playlistTracks.get(position);

        // Set track name and playlist name
        holder.track_name.setText(track.getTrackName());
        holder.artist_name.setText(track.getArtistName());

        // Load album art
        String playlistImageUrl = track.getAlbumImageUrl();
        if (playlistImageUrl != null && !playlistImageUrl.isEmpty()) {
            Glide.with(mContext).load(playlistImageUrl).into(holder.playlist_image);
        } else {
            Glide.with(mContext).load(R.drawable.tab_indicator).into(holder.playlist_image); // Placeholder
        }

        // Handle item click
        holder.itemView.setOnClickListener(view -> {
            Intent intent = new Intent(mContext, PlayerActivity.class);
            intent.putExtra("trackId", track.getTrackId());
            intent.putExtra("trackName", track.getTrackName());
            intent.putExtra("artistName", track.getArtistName());
            intent.putExtra("albumName", track.getAlbumName());
            intent.putExtra("albumImageUrl", track.getAlbumImageUrl());
            intent.putExtra("position", position);
            intent.putParcelableArrayListExtra("trackList", playlistTracks);
            mContext.startActivity(intent);
        });

    }

    @Override
    public int getItemCount() {
        return playlistTracks.size();
    }

    public class MyHolder extends RecyclerView.ViewHolder {
        ImageView playlist_image;
        TextView track_name, artist_name;
        public MyHolder(@NonNull View itemView) {
            super(itemView);
            playlist_image = itemView.findViewById(R.id.music_img);
            track_name = itemView.findViewById(R.id.music_file_name);
            artist_name = itemView.findViewById(R.id.artist_name);
        }
    }
}

