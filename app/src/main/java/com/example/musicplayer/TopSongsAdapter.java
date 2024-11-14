package com.example.musicplayer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class TopSongsAdapter extends RecyclerView.Adapter<TopSongsAdapter.SongViewHolder> {

    private final Context context;
    private List<SpotifyWrappedItem> songs;

    public TopSongsAdapter(Context context, List<SpotifyWrappedItem> songs) {
        this.context = context;
        this.songs = new ArrayList<>(songs);
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.music_items, parent, false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        SpotifyWrappedItem song = songs.get(position);
        holder.bind(song);
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    public void updateData(List<SpotifyWrappedItem> newSongs) {
        this.songs.addAll(newSongs);
        notifyDataSetChanged();
    }

    public static class SongViewHolder extends RecyclerView.ViewHolder {
        private final ImageView musicImg;
        private final TextView musicFileName;
        private final TextView artistName;
        private final ImageView menuMore;

        public SongViewHolder(@NonNull View itemView) {
            super(itemView);
            musicImg = itemView.findViewById(R.id.music_img);
            musicFileName = itemView.findViewById(R.id.music_file_name);
            artistName = itemView.findViewById(R.id.artist_name);
            menuMore = itemView.findViewById(R.id.menuMore);
        }

        public void bind(SpotifyWrappedItem song) {
            musicFileName.setText(song.getName());
            artistName.setText(song.getArtist());
            Glide.with(itemView.getContext())
                    .load(song.getImageUrl())
                    .placeholder(R.drawable.ic_launcher_background)
                    .into(musicImg);

            menuMore.setOnClickListener(v -> {

            });
        }
    }
}

