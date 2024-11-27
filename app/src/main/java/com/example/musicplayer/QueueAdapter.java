package com.example.musicplayer;

import android.annotation.SuppressLint;
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

public class QueueAdapter extends RecyclerView.Adapter<QueueAdapter.MyHolder> {
    private final Context mContext;
    public static ArrayList<SpotifyTrack> queueTracks;
    View view;
    public QueueAdapter(Context mContext, ArrayList<SpotifyTrack> albumTracks){
        this.mContext = mContext;
        this.queueTracks = albumTracks;
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        view = LayoutInflater.from(mContext).inflate(R.layout.music_items, parent, false);
        return new MyHolder(view);
    }



    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, int position) {
        SpotifyTrack track = queueTracks.get(position);

        // Set track name and album name
        holder.track_name.setText(track.getTrackName());
        holder.artist_name.setText(track.getArtistName());

        // Load album art
        String albumArtUrl = track.getAlbumImageUrl();
        if (albumArtUrl != null && !albumArtUrl.isEmpty()) {
            Glide.with(mContext).load(albumArtUrl).into(holder.album_image);
        } else {
            Glide.with(mContext).load(R.drawable.tab_indicator).into(holder.album_image); // Placeholder
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
            intent.putParcelableArrayListExtra("trackList", queueTracks);
            mContext.startActivity(intent);
        });

        holder.menuMore.setOnClickListener(v -> {
            PopupMenuHelper.showPopupMenu(mContext, v, track);
        });
    }

    @Override
    public int getItemCount() {
        return queueTracks.size();
    }

    public class MyHolder extends RecyclerView.ViewHolder {
        ImageView album_image, menuMore;;
        TextView track_name, artist_name;
        public MyHolder(@NonNull View itemView) {
            super(itemView);
            album_image = itemView.findViewById(R.id.music_img);
            track_name = itemView.findViewById(R.id.music_file_name);
            artist_name = itemView.findViewById(R.id.artist_name);
            menuMore = itemView.findViewById(R.id.menuMore);
        }
    }

    // Method to update the albums list in the adapter dynamically
    public void updateTracks(ArrayList<SpotifyTrack> newTracks) {
        queueTracks.clear();
        queueTracks.addAll(newTracks);
        notifyDataSetChanged();
    }

}

