package com.example.mandarin.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.mandarin.R
import com.example.mandarin.databinding.ItemChatBinding
import com.example.mandarin.model.PrivateChat
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.firebase.ui.database.ObservableSnapshotArray
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase

class ChatsRecyclerAdapter(
    options: FirebaseRecyclerOptions<PrivateChat>,
    private val context: Context,
    private val currentUser: FirebaseUser?,
    private val onDataChangedListener: DataChangedListener,
    private val clickListener: ItemClickedListener
)
    : FirebaseRecyclerAdapter<PrivateChat, ChatsRecyclerAdapter.PrivateMessageViewHolder>(options) {

    private var chatsSelectedList = arrayListOf<String>()
    private var viewsSelectedList = arrayListOf<View>()

    fun resetSelectedChatsList() {
        chatsSelectedList.clear()
    }
    fun restBackgroundOfSelectedViews() {

        viewsSelectedList.forEach { view ->
            view.setBackgroundResource(R.color.white)
        }
        viewsSelectedList.clear()
    }

    interface DataChangedListener {
        fun onDataAvailable(snapshotArray: ObservableSnapshotArray<PrivateChat>)
    }

    interface ItemClickedListener {
        fun onItemClick(chateeId: String, view: View)
        fun onItemLongCLicked(chateeId: String, view: View)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PrivateMessageViewHolder {
        val viewBinding = ItemChatBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return PrivateMessageViewHolder(viewBinding)
    }

    override fun onBindViewHolder(
        holder: PrivateMessageViewHolder,
        position: Int,
        model: PrivateChat) {
        holder.bind(currentUser, model, context, clickListener)
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun onDataChanged() {
        super.onDataChanged()
        onDataChangedListener.onDataAvailable(snapshots)
    }

    inner class PrivateMessageViewHolder (private val itemBinding : ItemChatBinding)
        : RecyclerView.ViewHolder(itemBinding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(
            currentUser: FirebaseUser?,
            chat: PrivateChat,
            context: Context,
            clickListener: ItemClickedListener) = with(itemBinding) {

            val chateeId = if (chat.fromUserId == currentUser?.uid) chat.toUserId as String
                            else chat.fromUserId as String

            Firebase.database.reference.child("profiles").child(chateeId)
                .child("name").get().addOnSuccessListener { dataSnapshot ->
                val name = dataSnapshot.getValue(String::class.java)
                    if(name != null) {
                        personNameTextView.text = name
                    } else {
                        personNameTextView.text = "Unknown user"
                    }
            }

            if(chat.text != null) {
                recentMessageImageView.visibility = GONE
                recentMessageTextView.text = chat.text
                recentMessageTextView.visibility  = VISIBLE
            }

            if (chat.image != null) {
                recentMessageTextView.visibility  = GONE
                recentMessageImageView.visibility = VISIBLE
            }

            Firebase.database.reference.child("photos").child(chateeId).get()
                .addOnSuccessListener { dataSnapshot ->
                    val photo = dataSnapshot.getValue<String>()
                    if (photo != null) {
                        Glide.with(context).load(photo).diskCacheStrategy(
                        DiskCacheStrategy.RESOURCE).into(personImageView)
                } else {
                        Glide.with(context).load(R.drawable.ic_person_light_pearl)
                            .into(personImageView)
                    }

            }

            root.setOnClickListener { root ->
                if (chatsSelectedList.isNotEmpty()) {
                    if (chatsSelectedList.contains(chateeId)) {
                        chatsSelectedList.remove(chateeId)
                        root.setBackgroundResource(R.drawable.ripple_effect_curved_edges_16dp)
                    } else {
                        chatsSelectedList.add(chateeId)
                        root.setBackgroundResource(R.drawable.selected_chat_background)
                    }
                    if (viewsSelectedList.contains(root)){
                        viewsSelectedList.remove(root)
                    } else {
                        viewsSelectedList.add(root)
                    }
                }
                clickListener.onItemClick(chateeId, root)
            }

            root.setOnLongClickListener { root ->
                if (chatsSelectedList.contains(chateeId)) {
                    chatsSelectedList.remove(chateeId)
                    root.setBackgroundResource(R.drawable.ripple_effect_curved_edges_16dp)
                } else {
                    chatsSelectedList.add(chateeId)
                    root.setBackgroundResource(R.drawable.selected_chat_background)
                }
                if (viewsSelectedList.contains(root)){
                    viewsSelectedList.remove(root)
                } else {
                    viewsSelectedList.add(root)
                }
                clickListener.onItemLongCLicked(chateeId, root)
                true
            }
        }

    }

}
