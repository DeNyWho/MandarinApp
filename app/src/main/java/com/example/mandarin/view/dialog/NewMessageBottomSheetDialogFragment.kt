package com.example.mandarin.view.dialog

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.mandarin.adapter.WhoToMessageRecyclerAdapter
import com.example.mandarin.databinding.BottomSheetDialogFragmentNewMessageBinding
import com.example.mandarin.model.User
import com.example.mandarin.view.fragment.MandarinFragmentDirections
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase

class NewMessageBottomSheetDialogFragment (
    private val parentContext: Context
        ) :
    BottomSheetDialogFragment(),
    WhoToMessageRecyclerAdapter.ItemClickedListener {

    private var _binding: BottomSheetDialogFragmentNewMessageBinding? = null
    private val binding get() = _binding!!
    private lateinit var dbRef: DatabaseReference
    private lateinit var currentUser: FirebaseUser
    private lateinit var recyclerView: RecyclerView
    private var selectedUserId: String? = null
        get() = currentUser.uid


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetDialogFragmentNewMessageBinding
            .inflate(inflater, container, false)
        recyclerView = binding.addGroupMembersRecyclerView
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbRef = Firebase.database.reference
        currentUser = Firebase.auth.currentUser!!

        dbRef.child("friends").child(currentUser.uid).get().addOnSuccessListener {
            dataSnapshot ->
            val listOfUsers = arrayListOf<User>()
                dataSnapshot.children.forEach { snapShot ->
                    snapShot.getValue<User>()?.let { user -> listOfUsers.add(user) }
                    val adapter = WhoToMessageRecyclerAdapter(
                        this@NewMessageBottomSheetDialogFragment,
                        parentContext,
                        listOfUsers
                    )
                    recyclerView.adapter = adapter
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun onItemClick(user: User) {
        selectedUserId = user.userId
        Log.e("USERID","${user.userId}")

        val action = MandarinFragmentDirections.actionHomeFragmentToPrivateMessageFragment(user.userId!!)
        findNavController().navigate(action)


    }

}