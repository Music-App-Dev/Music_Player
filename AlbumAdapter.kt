package com.example.musicplayer

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class AlbumAdapter(
    private val mContext: Context,
    private val albumsList: ArrayList<SpotifyAlbum>
) : RecyclerView.Adapter<AlbumAdapter.MyHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        val view = LayoutInflater.from(mContext).inflate(R.layout.music_items, parent, false)
        return MyHolder(view)
    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        val album = albumsList[position]
        holder.albumName.text = album.albumName
        Glide.with(mContext)
            .load(album.imageUrl)
            .into(holder.albumImage)
    }

    override fun getItemCount(): Int {
        return albumsList.size
    }

    inner class MyHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var albumImage: ImageView = itemView.findViewById(R.id.music_img) // Replace with correct ID
        var albumName: TextView = itemView.findViewById(R.id.music_file_name) // Replace with correct ID
    }
}
