package com.example.mandarin.view.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.mandarin.adapter.PeopleAdapter
import com.example.mandarin.databinding.FragmentPeopleBinding
import com.example.mandarin.model.User
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase

class PeopleFragment : Fragment() {

    private lateinit var binding: FragmentPeopleBinding
    private lateinit var dbRef: DatabaseReference
    private lateinit var currentUser: FirebaseUser
    private val uid: String
        get() = currentUser.uid

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPeopleBinding.inflate(inflater, container, false)



        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbRef = Firebase.database.reference
        currentUser = Firebase.auth.currentUser!!

        dbRef.child("users").get().addOnSuccessListener {
                dataSnapshot ->
            val listOfUsers = arrayListOf<User>()
            dataSnapshot.children.forEach { snapShot ->
                snapShot.getValue<User>()?.let { user -> listOfUsers.add(user) }
                val adapter = PeopleAdapter(
                    requireContext(),
                    listOfUsers
                )
                Log.e("LISTUSRS","${listOfUsers}")
                binding.addGroupMembersRecyclerView.adapter = adapter
            }
        }

    }
}