package com.example.storybridge_android.ui.reading

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.storybridge_android.R

data class PageThumbnail(
    val pageIndex: Int,
    val imageBase64: String?
)

class ThumbnailAdapter(
    private val onThumbnailClick: (Int) -> Unit
) : RecyclerView.Adapter<ThumbnailAdapter.ThumbnailViewHolder>() {

    private val thumbnails = mutableListOf<PageThumbnail>()

    inner class ThumbnailViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val thumbnailImage: ImageView = view.findViewById(R.id.thumbnailImage)
        val pageNumber: TextView = view.findViewById(R.id.pageNumber)

        @SuppressLint("SetTextI18n")
        fun bind(thumbnail: PageThumbnail) {
            // 커버 페이지(0)는 표시하지 않으므로, pageIndex를 그대로 사용
            pageNumber.text = "Page ${thumbnail.pageIndex}"

            // Decode and display image
            if (thumbnail.imageBase64 != null) {
                try {
                    val decodedBytes = Base64.decode(thumbnail.imageBase64, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    thumbnailImage.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    // Failed to decode, show placeholder
                    thumbnailImage.setImageResource(android.R.color.white)
                }
            } else {
                // No image, show placeholder
                thumbnailImage.setImageResource(android.R.color.white)
            }

            // Click listener
            itemView.setOnClickListener {
                onThumbnailClick(thumbnail.pageIndex)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThumbnailViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_thumbnail, parent, false)
        return ThumbnailViewHolder(view)
    }

    override fun onBindViewHolder(holder: ThumbnailViewHolder, position: Int) {
        holder.bind(thumbnails[position])
    }

    override fun getItemCount(): Int = thumbnails.size

    fun submitList(newThumbnails: List<PageThumbnail>) {
        thumbnails.clear()
        thumbnails.addAll(newThumbnails)
        notifyDataSetChanged()
    }
}
