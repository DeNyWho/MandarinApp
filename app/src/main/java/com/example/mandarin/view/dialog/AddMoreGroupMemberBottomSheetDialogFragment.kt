package com.example.mandarin.view.dialog

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mandarin.adapter.AddMoreGroupMembersRecyclerAdapter
import com.example.mandarin.databinding.FragmentAddGroupMemberBottomSheetDialogBinding
import com.example.mandarin.model.User
import com.example.mandarin.wrapper.WrapContentLinearLayoutManager
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase

class AddMoreGroupMemberBottomSheetDialogFragment(
    private val groupContext: Context
) : BottomSheetDialogFragment(), AddMoreGroupMembersRecyclerAdapter.ItemClickedListener {

    private var _binding: FragmentAddGroupMemberBottomSheetDialogBinding? = null
    private val binding get() = _binding!!
    private lateinit var dbRef: DatabaseReference
    private lateinit var currentUser: FirebaseUser
    private lateinit var recyclerView: RecyclerView
    private var adapter: AddMoreGroupMembersRecyclerAdapter? = null
    private var selectedMembersCount = 0
    private val selectedMembersList = arrayListOf<String>()
    private var listOfExistingMembers: ArrayList<String>? = null
    private var bundledGroupId: String? = null
    private val uid: String
        get() = currentUser.uid



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAddGroupMemberBottomSheetDialogBinding
            .inflate(inflater, container, false)
        recyclerView = binding.addGroupMembersRecyclerView
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbRef = Firebase.database.reference
        currentUser = Firebase.auth.currentUser!!

        bundledGroupId = arguments?.getString("groupIdKey")


        dbRef.child("groups").child(bundledGroupId!!).child("members").get()
            .addOnSuccessListener { dataSnapshot ->
                val membersList = dataSnapshot.getValue<ArrayList<String>>()
                if (membersList == null || membersList.isEmpty()) {
                    listOfExistingMembers = arrayListOf()
                    selectedMembersList.add(uid)
                } else {
                    listOfExistingMembers = membersList
                }

                val usersRef =  dbRef.child("friends").child(currentUser.uid)

                val options = FirebaseRecyclerOptions.Builder<User>()
                    .setQuery(usersRef, User::class.java)
                    .setLifecycleOwner(viewLifecycleOwner)
                    .build()

                adapter = AddMoreGroupMembersRecyclerAdapter(
                    options,
                    listOfExistingMembers!!,
                    currentUser,
                    this,
                    groupContext)

                recyclerView.layoutManager = WrapContentLinearLayoutManager(
                    groupContext,
                    LinearLayoutManager.VERTICAL,
                    false)
                recyclerView.adapter = adapter
            }


            binding.addMemberButton.setOnClickListener {
                dbRef.child("groups").child(bundledGroupId!!).child("members")
                    .runTransaction(
                    object : Transaction.Handler {
                        override fun doTransaction(currentData: MutableData): Transaction.Result {
                            var list = currentData.getValue<ArrayList<String>>()
                            if(list == null) {
                                list = arrayListOf()
                            }
                            list.addAll(selectedMembersList)
                            currentData.value = list
                            return Transaction.success(currentData)
                        }

                        override fun onComplete(
                            error: DatabaseError?,
                            committed: Boolean,
                            currentData: DataSnapshot?
                        ) {

                            if (committed && error == null) {

                                selectedMembersList.forEach {
                                    dbRef.child("user-groups").child(it).runTransaction(
                                        object : Transaction.Handler {
                                            override fun doTransaction(currentData: MutableData):
                                                    Transaction.Result {
                                                val listOfGroups =
                                                    currentData.getValue<ArrayList<String>>()
                                                return if (listOfGroups == null) {
                                                    currentData.value = arrayListOf(bundledGroupId)
                                                    Transaction.success(currentData)
                                                } else {
                                                    listOfGroups.add(bundledGroupId!!)
                                                    currentData.value = listOfGroups
                                                    Transaction.success(currentData)
                                                }

                                            }

                                            override fun onComplete(
                                                error: DatabaseError?,
                                                committed: Boolean,
                                                currentData: DataSnapshot?
                                            ) {}

                                        }
                                    )
                                }

                                when (selectedMembersList.size) {
                                    0 -> {
                                        Toast.makeText(
                                            groupContext,
                                            "No new member selected",
                                            Toast.LENGTH_LONG).show()
                                    }
                                    1 -> {
                                        Toast.makeText(
                                            groupContext,
                                            "1 new member added successfully",
                                            Toast.LENGTH_LONG).show()
                                    }
                                    else -> {
                                        Toast.makeText(
                                            groupContext,
                                            "${selectedMembersList.size} " +
                                                    "new members added successfully",
                                            Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                            this@AddMoreGroupMemberBottomSheetDialogFragment.dismiss()
                        }

                    }
                )
            }

    }


    override fun onItemClick(user: User) {
        
    }

    override fun onItemSelected(userId: String, view: CheckBox) {
        if (view.isChecked) {
            selectedMembersCount++
            binding.selectedMemberCountTextView.text = selectedMembersCount.toString()
            selectedMembersList.add(userId)
        } else {
            selectedMembersCount--
            binding.selectedMemberCountTextView.text = selectedMembersCount.toString()
            selectedMembersList.remove(userId)
        }
        when (selectedMembersCount) {
            0 -> binding.selectedMemberCountTextView.visibility = GONE
            else -> binding.selectedMemberCountTextView.visibility = VISIBLE
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

}