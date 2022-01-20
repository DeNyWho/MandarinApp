package com.example.mandarin.view.fragment

import android.os.Bundle
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mandarin.adapter.GroupsRecyclerAdapter
import com.example.mandarin.databinding.FragmentGroupsBinding
import com.example.mandarin.wrapper.WrapContentLinearLayoutManager
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.firebase.ui.database.ObservableSnapshotArray
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase


class GroupsFragment :
    Fragment(),
    GroupsRecyclerAdapter.ItemClickedListener,
    GroupsRecyclerAdapter.DataChangedListener {

    private var _binding: FragmentGroupsBinding? = null
    private val binding get() = _binding!!
    private lateinit var recyclerView: RecyclerView
    lateinit var dbRef: DatabaseReference
    private lateinit var currentUser: FirebaseUser
    private var adapter: GroupsRecyclerAdapter? = null
    private val uid: String
        get() = currentUser.uid


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentGroupsBinding.inflate(inflater, container, false)
        recyclerView = binding.groupRecyclerView
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = binding.groupRecyclerView
        dbRef = Firebase.database.reference
        currentUser = Firebase.auth.currentUser!!

        val groupsRef = dbRef.child("user-groups").child(uid)

        val options = FirebaseRecyclerOptions.Builder<String>()
            .setQuery(groupsRef, String::class.java)
            .setLifecycleOwner(viewLifecycleOwner)
            .build()

        adapter = GroupsRecyclerAdapter(
            options,
            this@GroupsFragment,
            this@GroupsFragment
        )
        recyclerView.layoutManager = WrapContentLinearLayoutManager(requireContext(),
            LinearLayoutManager.VERTICAL, false)
        recyclerView.adapter = adapter

    }

    override fun onItemClick(chatGroupId: String) {
        val action = MandarinFragmentDirections.actionHomeFragmentToGroupMessageFragment(chatGroupId)
        parentFragment?.findNavController()?.navigate(action)
    }

    override fun onDataAvailable(snapshotArray: ObservableSnapshotArray<String>) {
        binding.groupsProgressBar.visibility = GONE
        if(snapshotArray.isEmpty()) {
            binding.noGroupsLayout.visibility = VISIBLE
        } else {
            binding.noGroupsLayout.visibility = GONE
        }
    }
}