package com.example.mandarin.view.fragment

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.appcompat.view.ActionMode
import androidx.navigation.fragment.findNavController
import com.example.mandarin.R
import com.example.mandarin.adapter.FriendsAdapter
import com.example.mandarin.databinding.FragmentFriendsBinding
import com.example.mandarin.model.PrivateMessage
import com.example.mandarin.model.User
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage


class FriendsFragment : Fragment() {

    private lateinit var binding: FragmentFriendsBinding
    private lateinit var dbRef: DatabaseReference
    private lateinit var currentUser: FirebaseUser
    private var adapter: FriendsAdapter? = null
    private val uid: String
        get() = currentUser.uid
    private var selectedFriendsCount = 0
    private var actionMode: ActionMode? = null
    private var listOfSelectedFriends = arrayListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFriendsBinding.inflate(inflater, container, false)



        return binding.root
    }

    fun onItemLongCLicked(chateeId: String, view: View) {
        if (actionMode == null) {
            actionMode = (activity as AppCompatActivity?)!!
                .startSupportActionMode(actionModeCallBack)
        }

        updateSelection(chateeId, view)
    }

    fun onItemClick(chateeId: String, view: View) {
        if (actionMode == null) {
            val action = FriendsFragmentDirections
                .actionFriendsFragmentToPrivateMessageFragment(chateeId)
            findNavController().navigate(action)
        } else {
            updateSelection(chateeId, view)
        }
    }

    private fun updateSelection(chateeId: String, view: View) {
        if (!listOfSelectedFriends.contains(chateeId)) {
            listOfSelectedFriends.add(chateeId)
            selectedFriendsCount = listOfSelectedFriends.size
            actionMode?.title = selectedFriendsCount.toString()
        } else {
            listOfSelectedFriends.remove(chateeId)
            selectedFriendsCount = listOfSelectedFriends.size
            actionMode?.title = selectedFriendsCount.toString()
        }
        if (selectedFriendsCount == 0) {
            actionMode?.title = null
            actionMode?.finish()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbRef = Firebase.database.reference
        currentUser = Firebase.auth.currentUser!!

        dbRef.child("friends").child(currentUser.uid).get()
            .addOnSuccessListener { dataSnapshot ->
                val listOfUsers = arrayListOf<User>()
                dataSnapshot.children.forEach { snapShot ->
                    snapShot.getValue<User>()?.let { user -> listOfUsers.add(user) }
                    val adapter = FriendsAdapter(
                        requireContext(),
                        currentUser,
                        listOfUsers,
                        this@FriendsFragment,
                    )
                    Log.e("LISTUSRS", "${listOfUsers}")
                    binding.addGroupMembersRecyclerView.adapter = adapter
                }
            }


    }

    private val actionModeCallBack: ActionMode.Callback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            mode?.menuInflater?.inflate(R.menu.on_long_click_chat_menu, menu)
            mode?.title = "0"
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            return false
        }

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {

            return when (item?.itemId) {
                R.id.delete_chat_menu_item -> {
                    AlertDialog.Builder(requireContext())
                        .setMessage(
                            "Delete ${
                                singularOrPlural(
                                    listOfSelectedFriends,
                                    "this friend",
                                    "these friends"
                                )
                            }?"
                        )
                        .setPositiveButton("Yes") { dialog, which ->
                            listOfSelectedFriends.forEach { friendId ->
                                dbRef.child("friends").child(currentUser.uid)
                                    .get().addOnSuccessListener { dataSnapshot ->
                                        val listOffriendsIds = arrayListOf<String>()
                                        dataSnapshot.children.forEach { snapshot ->
                                            val friendIda = snapshot
                                                .getValue(User::class.java)?.userId
                                            if (friendIda != null) {
                                                listOffriendsIds.add(friendIda)
                                            }
                                        }
                                        if (listOffriendsIds.isNotEmpty()) {
                                            dbRef.child("/friends/${currentUser.uid}/${friendId}")
                                                .setValue(null)
                                            listOffriendsIds.forEach { messageId ->
                                                dbRef.child("friends").child(currentUser.uid)
                                                    .child(friendId).child(messageId).get()
                                                    .addOnSuccessListener { dataSnapshot ->
                                                        val message =
                                                            dataSnapshot.getValue<PrivateMessage>()
                                                        if (message?.image != null && message.fromUserId == currentUser.uid) {
                                                            Firebase.storage.getReferenceFromUrl(
                                                                message.image!!
                                                            ).delete()
                                                                .addOnCompleteListener { task ->
                                                                    if (task.isSuccessful) {
                                                                        val childUpdates =
                                                                            hashMapOf<String, Any?>(
                                                                                "/friends/${currentUser.uid}/${friendId}/" +
                                                                                        messageId to null,
                                                                                "/friends/${friendId}/${currentUser.uid}/" +
                                                                                        messageId to null
                                                                            )
                                                                        dbRef.updateChildren(
                                                                            childUpdates
                                                                        )
                                                                        Toast.makeText(
                                                                            requireContext(),
                                                                            "Deleted friend",
                                                                            Toast.LENGTH_LONG
                                                                        ).show()
                                                                        dbRef.child("friends").child(currentUser.uid).get()
                                                                            .addOnSuccessListener { dataSnapshot ->
                                                                                val listOfUsers = arrayListOf<User>()
                                                                                dataSnapshot.children.forEach { snapShot ->
                                                                                    snapShot.getValue<User>()?.let { user -> listOfUsers.add(user) }
                                                                                    val adapter = FriendsAdapter(
                                                                                        requireContext(),
                                                                                        currentUser,
                                                                                        listOfUsers,
                                                                                        this@FriendsFragment,
                                                                                    )
                                                                                    Log.e("LISTUSRS", "${listOfUsers}")
                                                                                    binding.addGroupMembersRecyclerView.adapter = adapter
                                                                                }
                                                                            }
                                                                    } else {
                                                                        Toast.makeText(
                                                                            requireContext(),
                                                                            "Failed to delete friend",
                                                                            Toast.LENGTH_LONG
                                                                        ).show()
                                                                    }
                                                                }
                                                        } else {
                                                            val childUpdates =
                                                                hashMapOf<String, Any?>(
                                                                    "/friends/${currentUser.uid}/${friendId}/" +
                                                                            messageId to null
                                                                )
                                                            dbRef.updateChildren(childUpdates)
                                                        }
                                                    }
                                            }
                                            Toast.makeText(
                                                requireContext(),
                                                "Deleted friend",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        } else {
                                            Toast.makeText(
                                                requireContext(),
                                                "Empty user",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                            }
                            dialog.dismiss()
                            mode?.finish()
                        }.setNegativeButton("No") { dialog, which ->
                            dialog.dismiss()
                        }.show()
                    true
                }
                else -> false
            }
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            listOfSelectedFriends.clear()
            selectedFriendsCount = listOfSelectedFriends.size
            adapter?.resetSelectedFriendsList()
            adapter?.restBackgroundOfSelectedViews()
            actionMode = null
        }

        private fun singularOrPlural(
            list: ArrayList<String>,
            singular: String,
            plural: String
        ): String {
            val size = list.size
            var output = ""
            if (list.isNotEmpty()) {
                output = if (size == 1) {
                    singular
                } else {
                    plural
                }
            }
            return output
        }

    }
}