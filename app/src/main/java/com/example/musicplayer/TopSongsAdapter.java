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

import java.util.List;

public class TopSongsAdapter extends RecyclerView.Adapter<TopSongsAdapter.ViewHolder> {

    private Context context;
    private List<SpotifyTrack> topSongs;

    public TopSongsAdapter(Context context, List<SpotifyTrack> topSongs) {
        this.context = context;
        this.topSongs = topSongs;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.music_items, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // Get the song at the current position
        SpotifyTrack song = topSongs.get(position);

        // Set the song's title and artist
        holder.songTitle.setText(song.getTrackName());
        holder.songArtist.setText(song.getArtistName());

        holder.playCount.setText("Plays: " + song.getDuration());

         Glide.with(context).load(song.getAlbumImageUrl()).into(holder.albumImage);
    }

    @Override
    public int getItemCount() {
        return topSongs.size();
    }

    public void updateData(List<SpotifyTrack> newSongs) {
        topSongs.clear();
        topSongs.addAll(newSongs);
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView albumImage, menuMore;
        TextView songTitle, songArtist, playCount;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            albumImage = itemView.findViewById(R.id.music_img);
            songTitle = itemView.findViewById(R.id.music_file_name);
            songArtist = itemView.findViewById(R.id.artist_name);
            playCount = itemView.findViewById(R.id.plays_count);
            menuMore = itemView.findViewById(R.id.menuMore);
        }
    }
}

