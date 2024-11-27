package com.example.musicplayer;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.SearchViewHolder> {

    private ArrayList<SpotifyTrack> searchResults;

    private Context mContext;



    public SearchAdapter(Context mContext, ArrayList<SpotifyTrack> searchResults) {
        this.mContext = mContext;
        this.searchResults = searchResults;
    }



    @NonNull
    @Override
    public SearchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.music_items, parent, false);
        return new SearchViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SearchViewHolder holder, int position) {
        SpotifyTrack track = searchResults.get(position);
        holder.songTitle.setText(track.getTrackName());
        holder.artistName.setText(track.getArtistName());
        Glide.with(holder.itemView.getContext())
                .load(track.getAlbumImageUrl()) // URL of the album image
                .placeholder(R.drawable.gradient_bg) // optional placeholder image while loading
                .into(holder.albumUrl); // Target ImageView

        // Add additional data binding as needed

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(mContext, PlayerActivity.class);
            intent.putExtra("position", position); // Pass the exact position
            intent.putParcelableArrayListExtra("trackList", searchResults); // Pass the full list of tracks
            mContext.startActivity(intent);
        });

        holder.menuMore.setOnClickListener(v -> {
            PopupMenuHelper.showPopupMenu(mContext, v, track);
        });
    }


    @Override
    public int getItemCount() {
        return searchResults.size();
    }

    public static class SearchViewHolder extends RecyclerView.ViewHolder {
        TextView songTitle;
        TextView artistName;

        ImageView albumUrl, menuMore;
        public SearchViewHolder(@NonNull View itemView) {
            super(itemView);
            songTitle = itemView.findViewById(R.id.music_file_name);
            artistName = itemView.findViewById(R.id.artist_name);
            albumUrl = itemView.findViewById(R.id.music_img);
            menuMore = itemView.findViewById(R.id.menuMore);
        }
    }
}