package com.example.mandarin.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.mandarin.R
import com.example.mandarin.databinding.ItemPrivateMessageCurrentUserBinding
import com.example.mandarin.databinding.ItemPrivateMessageCurrentUserSameBinding
import com.example.mandarin.databinding.ItemPrivateMessageOtherUserBinding
import com.example.mandarin.databinding.ItemPrivateMessageOtherUserSameBinding
import com.example.mandarin.glide.GlideImageLoader
import com.example.mandarin.model.PrivateMessage
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.firebase.ui.database.ObservableSnapshotArray
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

class PrivateMessageRecyclerAdapter(
    options: FirebaseRecyclerOptions<PrivateMessage>,
    private val currentUser: FirebaseUser?,
    private val clickListener: ItemClickedListener,
    private val onDataChangedListener: DataChangedListener
) : FirebaseRecyclerAdapter<PrivateMessage, RecyclerView.ViewHolder>(options) {

    private var messagesSelectedList = arrayListOf<String>()
    private var viewsSelectedList = arrayListOf<View>()

    fun resetMessagesSelectedList() {
        messagesSelectedList.clear()
    }
    fun restBackgroundOfSelectedViews() {
        viewsSelectedList.forEach { view ->
            view.setBackgroundResource(R.color.white)
        }
        viewsSelectedList.clear()
    }

    interface DataChangedListener {
        fun onDataAvailable(snapshotArray: ObservableSnapshotArray<PrivateMessage>)
    }

    interface ItemClickedListener {
        fun onItemLongCLicked(message: PrivateMessage, view: View)
        fun onItemClicked(message: PrivateMessage, view: View)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        var viewHolder : RecyclerView.ViewHolder? = null

        when (viewType) {
            VIEW_TYPE_CURRENT_USER -> {
                val view = inflater.inflate(
                    R.layout.item_private_message_current_user,
                    parent,
                    false)
                val binding = ItemPrivateMessageCurrentUserBinding.bind(view)
                viewHolder = CurrentUserPrivateMessageViewHolder(binding)
            }
            VIEW_TYPE_OTHER_USER -> {
                val view = inflater.inflate(
                    R.layout.item_private_message_other_user,
                    parent,
                    false)
                val binding = ItemPrivateMessageOtherUserBinding.bind(view)
                viewHolder = OtherUserPrivateMessageViewHolder(binding)
            }
            VIEW_TYPE_CURRENT_USER_SAME -> {
                val view = inflater.inflate(
                    R.layout.item_private_message_current_user_same,
                    parent,
                    false)
                val binding = ItemPrivateMessageCurrentUserSameBinding.bind(view)
                viewHolder = CurrentUserPrivateMessageViewHolderSame(binding)
            }
            VIEW_TYPE_OTHER_USER_SAME -> {
                val view = inflater.inflate(
                    R.layout.item_private_message_other_user_same,
                    parent,
                    false)
                val binding = ItemPrivateMessageOtherUserSameBinding.bind(view)
                viewHolder = OtherUserPrivateMessageViewHolderSame(binding)
            }
        }
        return viewHolder!!
    }


    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        model: PrivateMessage
    ) {

        val uid = currentUser?.uid

        if (snapshots[position].fromUserId != uid) {
            if (snapshots.size != 0) {
                if(position > 0 && snapshots[position].fromUserId ==
                    snapshots[position - 1].fromUserId) {
                    (holder as OtherUserPrivateMessageViewHolderSame).bind(model, position)
                } else {
                    (holder as OtherUserPrivateMessageViewHolder).bind(model, position)
                }
            }

        } else {
            if (snapshots.size != 0) {
                if(position > 0 && snapshots[position].fromUserId ==
                    snapshots[position - 1].fromUserId) {
                    (holder as CurrentUserPrivateMessageViewHolderSame).bind(model, position)
                } else {
                    (holder as CurrentUserPrivateMessageViewHolder).bind(model, position)
                }
            }
        }

    }

    override fun onDataChanged() {
        super.onDataChanged()
        onDataChangedListener.onDataAvailable(snapshots)
    }


    override fun getItemViewType(position: Int): Int {

        var viewType = 0

        val uid = currentUser?.uid
        if (snapshots[position].fromUserId != uid) {
            if (snapshots.size != 0) {
                viewType = if(position > 0 && snapshots[position].fromUserId ==
                    snapshots[position - 1].fromUserId) {
                   VIEW_TYPE_OTHER_USER_SAME
                } else {
                    VIEW_TYPE_OTHER_USER
                }
            }
        } else {
            if (snapshots.size != 0) {
                viewType = if(position > 0 && snapshots[position].fromUserId ==
                    snapshots[position - 1].fromUserId) {
                    VIEW_TYPE_CURRENT_USER_SAME
                } else {
                    VIEW_TYPE_CURRENT_USER
                }
            }
        }
        return viewType
    }

    inner class CurrentUserPrivateMessageViewHolder(
        private val itemBinding: ItemPrivateMessageCurrentUserBinding
    )
        : RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(message: PrivateMessage, itemPosition: Int) = with(itemBinding) {

            if (message.text != null) {
                currentUserMessageTextView.text = message.text
                currentUserMessageTextView.visibility = VISIBLE
            } else {
                currentUserMessageTextView.visibility = GONE
            }
            if (message.image != null) {
                loadImageIntoView(currentUserMessagePhotoImageView, message.image!!, photoProgressBar)
                currentUserMessagePhotoImageView.visibility = VISIBLE
            } else {
                currentUserMessagePhotoImageView.visibility = GONE
                photoProgressBar.visibility = GONE
            }

            root.setOnClickListener { root ->
                if(messagesSelectedList.isNotEmpty()) {
                if (messagesSelectedList.contains(message.messageId)) {
                    messagesSelectedList.remove(message.messageId)
                    root.setBackgroundResource(R.color.white)
                } else {
                    message.messageId?.let { id -> messagesSelectedList.add(id) }
                    root.setBackgroundResource(R.color.lightest_pearl)
                }
                    if (viewsSelectedList.contains(root)){
                        viewsSelectedList.remove(root)
                    } else {
                        viewsSelectedList.add(root)
                    }
                clickListener.onItemClicked(message, root)
                }
            }

            root.setOnLongClickListener {
                if (messagesSelectedList.contains(message.messageId)) {
                    messagesSelectedList.remove(message.messageId)
                    root.setBackgroundResource(R.color.white)
                } else {
                    message.messageId?.let { id -> messagesSelectedList.add(id) }
                    root.setBackgroundResource(R.color.lightest_pearl)
                }
                if (viewsSelectedList.contains(root)){
                    viewsSelectedList.remove(root)
                } else {
                    viewsSelectedList.add(root)
                }
                clickListener.onItemLongCLicked(message, it)
                true
            }
            if(messagesSelectedList.contains(message.messageId)) {
                root.setBackgroundResource(R.color.lightest_pearl)
            } else {
                root.setBackgroundResource(R.color.white)
            }

        }

    }

    inner class CurrentUserPrivateMessageViewHolderSame(
        private val itemBinding: ItemPrivateMessageCurrentUserSameBinding
    ) :
        RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(message: PrivateMessage, itemPosition: Int) = with (itemBinding) {

            if (message.text != null) {
                currentUserMessageTextView.text = message.text
                currentUserMessageTextView.visibility = VISIBLE
            } else {
                currentUserMessageTextView.visibility = GONE
            }

            if (message.image != null) {
                loadImageIntoView(
                    itemBinding.currentUserMessagePhotoImageView,
                    message.image!!,
                    photoProgressBar)
                currentUserMessagePhotoImageView.visibility = VISIBLE
            } else {
                currentUserMessagePhotoImageView.visibility = GONE
                photoProgressBar.visibility = GONE
            }

            root.setOnClickListener {
                if(messagesSelectedList.isNotEmpty()) {
                    if (messagesSelectedList.contains(message.messageId)) {
                        messagesSelectedList.remove(message.messageId)
                        root.setBackgroundResource(R.color.white)
                    } else {
                        message.messageId?.let { id -> messagesSelectedList.add(id) }
                        root.setBackgroundResource(R.color.lightest_pearl)
                    }
                    if (viewsSelectedList.contains(root)){
                        viewsSelectedList.remove(root)
                    } else {
                        viewsSelectedList.add(root)
                    }
                    clickListener.onItemClicked(message, it)
                }
            }

            root.setOnLongClickListener {
                if (messagesSelectedList.contains(message.messageId)) {
                    messagesSelectedList.remove(message.messageId)
                    root.setBackgroundResource(R.color.white)
                } else {
                    message.messageId?.let { id -> messagesSelectedList.add(id) }
                    root.setBackgroundResource(R.color.lightest_pearl)
                }
                if (viewsSelectedList.contains(root)){
                    viewsSelectedList.remove(root)
                } else {
                    viewsSelectedList.add(root)
                }
                clickListener.onItemLongCLicked(message, it)
                true
            }
            if(messagesSelectedList.contains(message.messageId)) {
                root.setBackgroundResource(R.color.lightest_pearl)
            } else {
                root.setBackgroundResource(R.color.white)
            }

        }

    }

    inner class OtherUserPrivateMessageViewHolder(
        private val itemBinding: ItemPrivateMessageOtherUserBinding)
        : RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(message: PrivateMessage, itemPosition: Int) = with(itemBinding) {

            if (message.text != null) {
                messageTextView.text = message.text
                itemBinding.messageTextView.visibility = VISIBLE
            } else {
                messageTextView.visibility = GONE
            }

            if (message.image != null) {
                loadImageIntoView(messagePhotoImageView, message.image!!, photoProgressBar)
                messagePhotoImageView.visibility = VISIBLE
            } else {
                messagePhotoImageView.visibility = GONE
                photoProgressBar.visibility = GONE
            }

            root.setOnClickListener {
                if(messagesSelectedList.isNotEmpty()) {
                    if (messagesSelectedList.contains(message.messageId)) {
                        messagesSelectedList.remove(message.messageId)
                        root.setBackgroundResource(R.color.white)
                    } else {
                        message.messageId?.let { id -> messagesSelectedList.add(id) }
                        root.setBackgroundResource(R.color.lightest_pearl)
                    }
                    if (viewsSelectedList.contains(root)){
                        viewsSelectedList.remove(root)
                    } else {
                        viewsSelectedList.add(root)
                    }
                    clickListener.onItemClicked(message, it)
                }
            }

            root.setOnLongClickListener {
                if (messagesSelectedList.contains(message.messageId)) {
                    messagesSelectedList.remove(message.messageId)
                    root.setBackgroundResource(R.color.white)
                } else {
                    message.messageId?.let { id -> messagesSelectedList.add(id) }
                    root.setBackgroundResource(R.color.lightest_pearl)
                }
                if (viewsSelectedList.contains(root)){
                    viewsSelectedList.remove(root)
                } else {
                    viewsSelectedList.add(root)
                }
                clickListener.onItemLongCLicked(message, it)
                true
            }
            if(messagesSelectedList.contains(message.messageId)) {
                root.setBackgroundResource(R.color.lightest_pearl)
            } else {
                root.setBackgroundResource(R.color.white)
            }

        }

    }

    inner class OtherUserPrivateMessageViewHolderSame(
        private val itemBinding: ItemPrivateMessageOtherUserSameBinding)
        : RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(message: PrivateMessage, itemPosition: Int) = with(itemBinding) {

            if (message.text != null) {
                messageTextView.text = message.text
                messageTextView.visibility = VISIBLE
            } else {
                messageTextView.visibility = GONE
            }

            if (message.image != null) {
                loadImageIntoView(messagePhotoImageView, message.image!!, photoProgressBar)
                messagePhotoImageView.visibility = VISIBLE
            } else {
                messagePhotoImageView.visibility = GONE
                photoProgressBar.visibility = GONE
            }

            root.setOnClickListener {
                if(messagesSelectedList.isNotEmpty()) {
                    if (messagesSelectedList.contains(message.messageId)) {
                        messagesSelectedList.remove(message.messageId)
                        root.setBackgroundResource(R.color.white)
                    } else {
                        message.messageId?.let { id -> messagesSelectedList.add(id) }
                        root.setBackgroundResource(R.color.lightest_pearl)
                    }
                    if (viewsSelectedList.contains(root)){
                        viewsSelectedList.remove(root)
                    } else {
                        viewsSelectedList.add(root)
                    }
                    clickListener.onItemClicked(message, it)
                }
            }

            root.setOnLongClickListener { root ->
                if (messagesSelectedList.contains(message.messageId)) {
                    messagesSelectedList.remove(message.messageId)
                    root.setBackgroundResource(R.color.white)
                } else {
                    message.messageId?.let { id -> messagesSelectedList.add(id) }
                    root.setBackgroundResource(R.color.lightest_pearl)
                }
                if (viewsSelectedList.contains(root)){
                    viewsSelectedList.remove(root)
                } else {
                    viewsSelectedList.add(root)
                }
                clickListener.onItemLongCLicked(message, root)
                true
            }
            if(messagesSelectedList.contains(message.messageId)) {
                root.setBackgroundResource(R.color.lightest_pearl)
            } else {
                root.setBackgroundResource(R.color.white)
            }
        }
    }

    private fun loadImageIntoView(
        imageView: ShapeableImageView,
        photoUrl: String,
        photoProgressBar: ProgressBar) {

        photoProgressBar.visibility = VISIBLE
        if (photoUrl.startsWith("gs://")) {
            val storageReference = Firebase.storage.getReferenceFromUrl(photoUrl)
            storageReference.downloadUrl
                .addOnSuccessListener { uri ->
                    val downloadUrl = uri.toString()
                    val options = RequestOptions()
                        .error(R.drawable.ic_downloading)
                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    imageView.visibility = VISIBLE
                    GlideImageLoader(imageView, photoProgressBar).load(downloadUrl, options)
                }
        } else {
            val options = RequestOptions()
                .error(R.drawable.ic_downloading)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            imageView.visibility = VISIBLE
            GlideImageLoader(imageView, photoProgressBar).load(photoUrl, options)
        }
    }

    companion object {
        const val TAG = "MessageAdapter"

        //four view types
        var VIEW_TYPE_CURRENT_USER = 0
        var VIEW_TYPE_OTHER_USER = 1
        var VIEW_TYPE_CURRENT_USER_SAME = 2
        var VIEW_TYPE_OTHER_USER_SAME = 3
    }

}

