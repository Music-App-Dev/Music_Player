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

public class AlbumAdapter extends RecyclerView.Adapter<AlbumAdapter.MyHolder> {
    private final Context mContext;
    private final ArrayList<SpotifyAlbum> albumFiles;
    View view;
    public AlbumAdapter(Context mContext, ArrayList<SpotifyAlbum> albumFiles){
        this.mContext = mContext;
        this.albumFiles = albumFiles;
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        view = LayoutInflater.from(mContext).inflate(R.layout.album_item, parent, false);
        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, final int position) {
        SpotifyAlbum album = albumFiles.get(position);
        holder.album_name.setText(album.getAlbumName());

        // Load album image using Glide
        Glide.with(mContext)
                .load(album.getImageUrl())
                .placeholder(R.drawable.tab_indicator) // Placeholder in case image URL is null
                .into(holder.album_image);

        // Set click listener to open album details
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, AlbumDetails.class);
                intent.putExtra("albumId", album.getAlbumId()); // Pass album ID to AlbumDetails activity
                intent.putExtra("albumName", album.getAlbumName());
                mContext.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return albumFiles.size();
    }

    public class MyHolder extends RecyclerView.ViewHolder {
        ImageView album_image;
        TextView album_name;
        public MyHolder(@NonNull View itemView) {
            super(itemView);
            album_image = itemView.findViewById(R.id.album_image);
            album_name = itemView.findViewById(R.id.album_name);
        }
    }

    // Method to update the album list and notify the adapter
    public void updateAlbums(ArrayList<SpotifyAlbum> newAlbums) {
        albumFiles.clear();
        albumFiles.addAll(newAlbums);
        notifyDataSetChanged();
    }

    private byte[] getAlbumArt(String uri){
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(uri.toString());
        return retriever.getEmbeddedPicture();
    }
}
