package com.example.musicplayer;

import static com.example.musicplayer.MainActivity.combPlayAlbums;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

public class CombinedAdapter extends RecyclerView.Adapter<CombinedAdapter.MyHolder> {
    private final Context mContext;
    private final ArrayList<SpotifyItem> combinedFiles;
    View view;
    public CombinedAdapter(Context mContext, ArrayList<SpotifyItem> combinedFiles){
        this.mContext = mContext;
        this.combinedFiles = combinedFiles;
    }

    @NonNull
    @Override
    public CombinedAdapter.MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        view = LayoutInflater.from(mContext).inflate(R.layout.combined_item, parent, false);
        return new CombinedAdapter.MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, int position) {
        SpotifyItem combined = combPlayAlbums.get(position);
        holder.combined_name.setText(combined.getName());

        // Load album/playlist image using Glide
        Glide.with(mContext)
                .load(combined.getImageUrl())
                .placeholder(R.drawable.tab_indicator)
                .into(holder.combined_image);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent;
                if ("album".equals(combined.getType())) {
                    intent = new Intent(mContext, AlbumDetails.class);
                } else {
                    intent = new Intent(mContext, PlaylistDetails.class);
                }

                intent.putExtra("combinedId", combined.getId());
                intent.putExtra("combinedName", combined.getName());
                intent.putExtra("combinedImageUrl", combined.getImageUrl());
                intent.putExtra("combinedType", combined.getType());
                mContext.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return combinedFiles.size();
    }

    public class MyHolder extends RecyclerView.ViewHolder {
        ImageView combined_image;
        TextView combined_name;
        public MyHolder(@NonNull View itemView) {
            super(itemView);
            combined_image = itemView.findViewById(R.id.album_playlist_image);
            combined_name = itemView.findViewById(R.id.album_playlist_name);
        }
    }

    // Method to update the album list and notify the adapter
    public void updateAlbums(ArrayList<SpotifyItem> newAlbums) {
        combinedFiles.clear();
        combinedFiles.addAll(newAlbums);
        notifyDataSetChanged();
    }
}