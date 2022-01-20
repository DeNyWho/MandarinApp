package com.example.mandarin.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.mandarin.R
import com.example.mandarin.databinding.ItemNewGroupMemberBinding
import com.example.mandarin.model.Profile
import com.example.mandarin.model.User
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase

class AddGroupMembersRecyclerAdapter(
    options: FirebaseRecyclerOptions<User>,
    private val currentUser: FirebaseUser?,
    private val clickListener: ItemClickedListener,
    private val context: Context
) :
    FirebaseRecyclerAdapter<User, AddGroupMembersRecyclerAdapter.GroupMemberViewHolder>(options) {

    private var memberSelectedList = arrayListOf<String>()

    interface ItemClickedListener {
        fun onItemClick(user: User)
        fun onItemSelected(userId: String, view: CheckBox)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupMemberViewHolder {
        val viewBinding = ItemNewGroupMemberBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return GroupMemberViewHolder(viewBinding)
    }

    override fun onBindViewHolder(holder: GroupMemberViewHolder, position: Int, model: User) {
        holder.bind(model, clickListener, context)
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }


    inner class GroupMemberViewHolder (private val itemBinding : ItemNewGroupMemberBinding)
        : RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(user: User, clickListener: ItemClickedListener, context: Context) =
            with(itemBinding) {

            if(user.userId == currentUser?.uid) {
                addGroupMemberCheckBox.isChecked = true
                addGroupMemberCheckBox.isEnabled = false
                if (!memberSelectedList.contains(user.userId)) {
                    user.userId?.let { memberSelectedList.add(it) }
                }
            } else {
                addGroupMemberCheckBox.setOnClickListener {
                    val clickedUserId = user.userId

                    if (memberSelectedList.contains(clickedUserId)) {
                        memberSelectedList.remove(clickedUserId)
                    } else {
                        memberSelectedList.add(clickedUserId!!)
                    }

                    clickListener.onItemSelected(user.userId!!, it as CheckBox)

                }
            }

            addGroupMemberCheckBox.isChecked = memberSelectedList.contains(user.userId)

            Firebase.database.reference.child("profiles").child(user.userId!!)
                .addListenerForSingleValueEvent(
                object : ValueEventListener {
                    @SuppressLint("SetTextI18n")
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val name = snapshot.getValue<Profile>()?.name

                        if (currentUser?.uid == user.userId) {
                            groupMemberNameTextView.text = "$name (You)"
                        } else {
                            groupMemberNameTextView.text = name
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                }
            )

            Firebase.database.reference.child("photos").child(user.userId!!)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val photo = snapshot.getValue<String>()
                        if (photo != null) {
                            Glide.with(context).load(photo)
                                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                                .into(groupMemberImageView)
                        } else {
                            Glide.with(context).load(R.drawable.ic_person_light_pearl)
                                .into(groupMemberImageView)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                }
            )

            this.root.setOnClickListener {
                clickListener.onItemClick(user)
            }
        }
    }

}