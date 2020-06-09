/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.firebase.ml.md.productsearch

import android.content.res.Resources
import android.graphics.Rect
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.ml.md.R
import com.google.firebase.ml.md.models.Product

/** Powers the bottom card carousel for displaying the preview of product search result.  */
class PreviewCardAdapter(
    private val searchedObjectList: List<SearchedObject>,
    private val previewCordClickedListener: (searchedObject: SearchedObject) -> Any
) : RecyclerView.Adapter<PreviewCardAdapter.CardViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        return CardViewHolder(
                LayoutInflater.from(parent.context)
                        .inflate(R.layout.products_preview_card, parent, false)
        )
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        val searchedObject = searchedObjectList[position]
        holder.bindProducts(searchedObject.productList)
        holder.itemView.setOnClickListener { previewCordClickedListener.invoke(searchedObject) }
    }

    override fun getItemCount(): Int = searchedObjectList.size

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        recyclerView.addItemDecoration(CardItemDecoration(recyclerView.resources))
        //LinearSnapHelper().attachToRecyclerView(recyclerView)
    }

    class CardViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val imageView: ImageView = itemView.findViewById(R.id.card_image)
        private val titleView: TextView = itemView.findViewById(R.id.card_title)
        private val subtitleView: TextView = itemView.findViewById(R.id.card_subtitle)
        private val imageSize: Int = itemView.resources.getDimensionPixelOffset(R.dimen.preview_card_image_size)

        internal fun bindProducts(products: List<Product>) {
            if (products.isEmpty()) {
                imageView.visibility = View.GONE
                titleView.setText(R.string.static_image_card_no_result_title)
                subtitleView.setText(R.string.static_image_card_no_result_subtitle)
            } else {
                val topProduct = products[0]
                imageView.visibility = View.VISIBLE
                imageView.setImageDrawable(null)
                if (!TextUtils.isEmpty(topProduct.imageUrl)) {
                    ImageDownloadTask(imageView, imageSize).execute(topProduct.imageUrl)
                } else {
                    imageView.setImageResource(R.drawable.logo_google_cloud)
                }
                titleView.text = topProduct.title
                subtitleView.text = itemView
                        .resources
                        .getString(R.string.static_image_preview_card_subtitle, products.size - 1)
            }
        }
    }

    private class CardItemDecoration constructor(resources: Resources) : RecyclerView.ItemDecoration() {

        private val cardSpacing: Int = resources.getDimensionPixelOffset(R.dimen.preview_card_spacing)

        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
            val adapterPosition = parent.getChildAdapterPosition(view)
            outRect.left = if (adapterPosition == 0) cardSpacing * 2 else cardSpacing
            val adapter = parent.adapter ?: return
            if (adapterPosition == adapter.itemCount - 1) {
                outRect.right = cardSpacing
            }
        }
    }


}
