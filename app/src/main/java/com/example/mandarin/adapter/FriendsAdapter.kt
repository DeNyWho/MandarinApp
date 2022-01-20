package com.example.mandarin.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mandarin.R
import com.example.mandarin.databinding.ItemWhoToMessageBinding
import com.example.mandarin.model.Profile
import com.example.mandarin.model.User
import com.example.mandarin.view.fragment.FriendsFragment
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase

class FriendsAdapter(
    private val context: Context,
    private val currentUser: FirebaseUser?,
    private var listOfUsers: ArrayList<User>,
    private val clickListener: FriendsFragment
) :
    RecyclerView.Adapter<FriendsAdapter.UserViewHolder>() {
    private var friendsSelectedList = arrayListOf<String>()
    private var viewsSelectedList = arrayListOf<View>()

    fun resetSelectedFriendsList() {
        friendsSelectedList.clear()
    }
    fun restBackgroundOfSelectedViews() {

        viewsSelectedList.forEach { view ->
            view.setBackgroundResource(R.color.white)
        }
        viewsSelectedList.clear()
    }


    interface ItemClickedListener {
        fun onItemClick(chateeId: String, view: View)
        fun onItemLongCLicked(chateeId: String, view: View)
    }

    inner class UserViewHolder (private val itemBinding : ItemWhoToMessageBinding)
        : RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(currentUser: FirebaseUser?, user: User, context: Context, clickListener: FriendsFragment)  =
            with(itemBinding) {
                val chateeId = if (user.userId == currentUser?.uid) user.userId as String
                else user.userId as String

                Firebase.database.reference.child("profiles").child(user.userId!!).get().addOnSuccessListener {
                        dataSnapshot ->
                    val name = dataSnapshot.getValue<Profile>()
                    userNameTextView.text = name!!.name

                }
                Firebase.database.reference.child("photos").child(user.userId!!).get()
                    .addOnSuccessListener { dataSnapshot ->
                        val photo = dataSnapshot.getValue<String>()
                        if (photo != null) {
                            Glide.with(context).load(photo).into(userImageView)
                        } else {
                            Glide.with(context).load(R.drawable.ic_person).into(userImageView)
                        }
                    }

                root.setOnClickListener { root ->
                    if (friendsSelectedList.isNotEmpty()) {
                        if (friendsSelectedList.contains(chateeId)) {
                            friendsSelectedList.remove(chateeId)
                            root.setBackgroundResource(R.drawable.ripple_effect_curved_edges_16dp)
                        } else {
                            friendsSelectedList.add(chateeId)
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
                    if (friendsSelectedList.contains(chateeId)) {
                        friendsSelectedList.remove(chateeId)
                        root.setBackgroundResource(R.drawable.ripple_effect_curved_edges_16dp)
                    } else {
                        friendsSelectedList.add(chateeId)
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


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val viewBinding = ItemWhoToMessageBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(viewBinding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val member = listOfUsers[position]
        holder.bind(currentUser, member, context, clickListener)
    }

    override fun getItemCount(): Int {
        return listOfUsers.size
    }



}