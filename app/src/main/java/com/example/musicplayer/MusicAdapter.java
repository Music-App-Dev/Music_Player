package com.example.musicplayer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import androidx.appcompat.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.MyViewHolder> {

    private Context mContext;
    public static ArrayList<SpotifyTrack> mFiles;

    public MusicAdapter(Context mContext, ArrayList<SpotifyTrack> mFiles){
        this.mFiles = mFiles;
        this.mContext = mContext;
    }

    public void updateData(List<SpotifyTrack> newTracks) {
        this.mFiles = new ArrayList<>(newTracks);
        notifyDataSetChanged();
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
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(mContext, PlayerActivity.class);
            intent.putExtra("position", position); // Pass the exact position
            intent.putParcelableArrayListExtra("trackList", mFiles); // Pass the full list of tracks
            mContext.startActivity(intent);
        });

        holder.menuMore.setOnClickListener(v -> {
            PopupMenuHelper.showPopupMenu(mContext, v, track);
        });
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
