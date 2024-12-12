package com.example.musicplayer

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ArtistAdapter(
    private val context: Context,
    private val artistList: List<SpotifyArtist>
) : RecyclerView.Adapter<ArtistAdapter.ArtistViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtistViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.artist_items, parent, false)
        return ArtistViewHolder(view)
    }

    override fun onBindViewHolder(holder: ArtistViewHolder, position: Int) {
        val artist = artistList[position]
        Log.d("ArtistAdapter", "Binding artist at position $position: ${artist.artistName}")
        holder.artistName.text = artist.artistName
        Glide.with(context)
            .load(artist.artistImageUrl)
            .placeholder(R.drawable.ic_launcher_background)
            .into(holder.artistImage)
        holder.itemView.setOnClickListener {
            Log.d("ArtistAdapter", "Clicked on artist: ${artist.artistName}")
        }
    }

    override fun getItemCount(): Int = artistList.size

    inner class ArtistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val artistName: TextView = itemView.findViewById(R.id.artist_name)
        val artistImage: ImageView = itemView.findViewById(R.id.artist_image)
    }
}
