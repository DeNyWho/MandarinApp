package com.example.mandarin.adapter

import android.view.LayoutInflater
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.mandarin.R
import com.example.mandarin.databinding.ItemGroupBinding
import com.example.mandarin.model.GroupChat
import com.example.mandarin.model.GroupMessage
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.firebase.ui.database.ObservableSnapshotArray
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase

class GroupsRecyclerAdapter(
    options: FirebaseRecyclerOptions<String>,
    private val onDataChangedListener: DataChangedListener,
    private val clickListener: ItemClickedListener
)
    : FirebaseRecyclerAdapter<String, GroupsRecyclerAdapter.GroupViewHolder>(options) {

    interface DataChangedListener {
        fun onDataAvailable(snapshotArray: ObservableSnapshotArray<String>)
    }

    interface ItemClickedListener {
        fun onItemClick(chatGroupId: String)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val viewBinding = ItemGroupBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return GroupViewHolder(viewBinding)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int, model: String) {
        holder.bind(model, clickListener)
    }

    override fun onDataChanged() {
        super.onDataChanged()
        onDataChangedListener.onDataAvailable(snapshots)
    }

    class GroupViewHolder (private val itemBinding : ItemGroupBinding)
        : RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(chatGroupId: String, clickListener: ItemClickedListener) = with(itemBinding) {

            Firebase.database.reference.child("group-messages")
                .child("recent-message").child(chatGroupId).addListenerForSingleValueEvent(
                object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val recentMessage = snapshot.getValue<GroupMessage>()
                        if (recentMessage?.text != null) {
                            recentMessageImageView.visibility = GONE
                            recentMessageTextView.text = recentMessage.text
                            recentMessageTextView.visibility = VISIBLE
                        } else {
                            recentMessageImageView.visibility = VISIBLE
                            recentMessageTextView.visibility = GONE
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                }
            )

            Firebase.database.reference.child("groups-id-name-photo").child(chatGroupId)
                .addValueEventListener(
                object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val chatGroup = snapshot.getValue<GroupChat>()
                        if (chatGroup != null) {
                            groupNameTextView.text = chatGroup.name

                            when (chatGroup.groupPhoto) {
                                null ->
                                    Glide.with(root.context).load(R.drawable.ic_group)
                                        .into(groupImageView)
                                else ->
                                    Glide.with(root.context).load(chatGroup.groupPhoto)
                                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                                        .into(groupImageView)
                            }

                            root.setOnClickListener {
                                clickListener.onItemClick(chatGroupId)
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                }
            )
        }
    }
}