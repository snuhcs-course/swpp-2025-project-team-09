package com.example.storybridge_android.ui.landing

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.storybridge_android.R

class TutorialPagerAdapter(
    private val pages: List<TutorialPage>
) : RecyclerView.Adapter<TutorialPagerAdapter.TutorialViewHolder>() {

    inner class TutorialViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.tutorialImage)
        val descView: TextView = view.findViewById(R.id.instructionText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TutorialViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tutorial_page, parent, false)
        return TutorialViewHolder(view)
    }

    override fun onBindViewHolder(holder: TutorialViewHolder, position: Int) {
        val page = pages[position]
        holder.imageView.setImageResource(page.imageRes)
        holder.descView.text = page.description
    }

    override fun getItemCount() = pages.size
}