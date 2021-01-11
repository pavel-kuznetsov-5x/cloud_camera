package com.spqrta.cloudvideo.screens.video

import android.view.View
import com.bumptech.glide.Glide
import com.spqrta.camera2demo.utility.recycler.BaseAdapter
import com.spqrta.cloudvideo.R
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_gallery_image.view.*

class GalleryAdapter : BaseAdapter<String, GalleryAdapter.VhImage>() {

    override val itemLayoutResource = R.layout.item_gallery_image

    override fun createViewHolder(view: View, baseClickListener: (Int) -> Unit): VhImage {
        return VhImage(view, baseClickListener)
    }

    class VhImage(override val containerView: View, clickListener: ((Int) -> Unit)) :
        BaseVh<String>(containerView, clickListener), LayoutContainer {

        override fun bind(item: String) {
            super.bind(item)
            Glide.with(containerView)
                .load(item)
                .into(containerView.ivImage)
        }
    }

}