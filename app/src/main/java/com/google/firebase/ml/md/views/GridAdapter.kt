package com.google.firebase.ml.md.views

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.ml.md.R

class GridAdapter(val width: Int, val height: Int, val onClick : ((Int) -> Unit)?) : RecyclerView.Adapter<GridAdapter.GridViewHolder>() {

    class GridViewHolder(itemView: View)
        : RecyclerView.ViewHolder(itemView) {
        fun bindTo(onClick: ((Int) -> Unit)?) {
            itemView.setOnClickListener { onClick?.invoke(adapterPosition) }
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        recyclerView.layoutManager = NotScrollGridLayoutManager(recyclerView.context, width)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GridViewHolder =
            GridViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_touch_grid, parent, false))

    override fun getItemCount(): Int = width * height

    override fun onBindViewHolder(holder: GridViewHolder, position: Int) = holder.bindTo(onClick)

    class NotScrollGridLayoutManager(context: Context?, spanCount: Int) : GridLayoutManager(context, spanCount) {
        override fun canScrollVertically(): Boolean = false
    }
}