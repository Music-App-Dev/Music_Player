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

public class AlbumDetailsAdapter extends RecyclerView.Adapter<AlbumDetailsAdapter.MyHolder> {
    private final Context mContext;
    public static ArrayList<SpotifyTrack> albumTracks;
    View view;
    public AlbumDetailsAdapter(Context mContext, ArrayList<SpotifyTrack> albumTracks){
        this.mContext = mContext;
        this.albumTracks = albumTracks;
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        view = LayoutInflater.from(mContext).inflate(R.layout.music_items, parent, false);
        return new MyHolder(view);
    }



    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, int position) {
        SpotifyTrack track = albumTracks.get(position);

        // Set track name
        holder.track_name.setText(track.getTrackName());

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
            intent.putParcelableArrayListExtra("trackList", albumTracks);
            mContext.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return albumTracks.size();
    }

    public class MyHolder extends RecyclerView.ViewHolder {
        ImageView album_image;
        TextView track_name;
        public MyHolder(@NonNull View itemView) {
            super(itemView);
            album_image = itemView.findViewById(R.id.music_img);
            track_name = itemView.findViewById(R.id.music_file_name);
        }
    }

    // Method to update the albums list in the adapter dynamically
    public void updateTracks(ArrayList<SpotifyTrack> newTracks) {
        albumTracks.clear();
        albumTracks.addAll(newTracks);
        notifyDataSetChanged();
    }

    private byte[] getAlbumArt(String uri){
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(uri.toString());
        return retriever.getEmbeddedPicture();
    }
}
