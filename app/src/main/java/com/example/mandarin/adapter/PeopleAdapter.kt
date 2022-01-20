package com.example.mandarin.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mandarin.R
import com.example.mandarin.databinding.ItemWhoToMessageBinding
import com.example.mandarin.model.Profile
import com.example.mandarin.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase


class PeopleAdapter(
    private val context: Context,
    private val listOfUsers: ArrayList<User>
) :
    RecyclerView.Adapter<PeopleAdapter.UserViewHolder>() {


    interface ItemClickedListener {
        fun onItemClick(user: User)
    }

    inner class UserViewHolder (private val itemBinding : ItemWhoToMessageBinding)
        : RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(user: User, context: Context) =
            with(itemBinding) {

                Firebase.database.reference.child("profiles").child(user.userId!!).get().addOnSuccessListener {
                        dataSnapshot ->
                    val name = dataSnapshot.getValue<Profile>()
                    userNameTextView.text = name!!.name

                }

                Firebase.database.reference.child("photos").child(user.userId!!).get().addOnSuccessListener {
                        dataSnapshot ->
                val photo = dataSnapshot.getValue<String>()
                if (photo != null) {
                    Glide.with(context).load(photo).into(userImageView)
                }
                    else {
                        Glide.with(context).load(R.drawable.ic_person).into(userImageView)
                }
            }

                this.root.setOnClickListener {
                     var auth: FirebaseAuth = Firebase.auth
                    Firebase.database.reference.child("friends")
                        .child(auth.currentUser!!.uid)
                        .child(user.userId!!)
                        .setValue(user)
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
        holder.bind(member, context)
    }

    override fun getItemCount(): Int {
        return listOfUsers.size
    }

}