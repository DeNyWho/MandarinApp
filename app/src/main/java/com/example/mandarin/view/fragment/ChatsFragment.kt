package com.example.mandarin.view.fragment

import android.os.Bundle
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mandarin.R
import com.example.mandarin.adapter.ChatsRecyclerAdapter
import com.example.mandarin.databinding.FragmentPrivateChatsBinding
import com.example.mandarin.model.PrivateChat
import com.example.mandarin.model.PrivateMessage
import com.example.mandarin.view.dialog.NewMessageBottomSheetDialogFragment
import com.example.mandarin.wrapper.WrapContentLinearLayoutManager
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.firebase.ui.database.ObservableSnapshotArray
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

class ChatsFragment :
    Fragment(),
    ChatsRecyclerAdapter.DataChangedListener,
    ChatsRecyclerAdapter.ItemClickedListener {

    private var _binding: FragmentPrivateChatsBinding? = null
    private val binding get() = _binding!!
    private lateinit var dbRef: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: FirebaseUser
    private var adapter: ChatsRecyclerAdapter? = null
    private var manager: WrapContentLinearLayoutManager? = null
    private lateinit var recyclerView: RecyclerView
    private var actionMode: ActionMode? = null
    private var selectedChatsCount = 0
    private var listOfSelectedChats = arrayListOf<String>()
    private val uid: String
        get() = currentUser.uid


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPrivateChatsBinding.inflate(inflater, container, false)
        recyclerView = binding.privateMessagesRecyclerView
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbRef = Firebase.database.reference

        auth = Firebase.auth

        currentUser = auth.currentUser!!

        val chatsRef = dbRef.child("user-messages").child("recent-message")
            .child(uid)

        val options = FirebaseRecyclerOptions.Builder<PrivateChat>()
            .setQuery(chatsRef, PrivateChat::class.java)
            .setLifecycleOwner(viewLifecycleOwner)
            .build()

        adapter = ChatsRecyclerAdapter(
            options,
            requireContext(),
            currentUser,
            this@ChatsFragment,
            this@ChatsFragment)

        manager = WrapContentLinearLayoutManager(
            requireContext(),
            LinearLayoutManager.VERTICAL,
            false)
        recyclerView.layoutManager = manager
        recyclerView.adapter = adapter
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun onDataAvailable(snapshotArray: ObservableSnapshotArray<PrivateChat>) {
        binding.privateMessagesProgressBar.visibility = GONE

        if (snapshotArray.isEmpty()) {
            binding.noChatsLayout.visibility = VISIBLE
        } else {
            binding.noChatsLayout.visibility = GONE
        }
    }

    override fun onItemLongCLicked(chateeId: String, view: View) {
        if (actionMode == null) {
            actionMode = (activity as AppCompatActivity?)!!
                .startSupportActionMode(actionModeCallBack)
        }

        updateSelection(chateeId, view)
    }

    override fun onItemClick(chateeId: String, view: View) {
        if (actionMode == null) {
            val action = MandarinFragmentDirections
                .actionHomeFragmentToPrivateMessageFragment(chateeId)
            findNavController().navigate(action)
        } else {
            updateSelection(chateeId, view)
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
                            "Delete ${singularOrPlural(listOfSelectedChats, 
                                "this chat", 
                                "these chats")}?")
                        .setPositiveButton("Yes") { dialog, which ->
                            listOfSelectedChats.forEach { chatId ->
                                dbRef.child("user-messages").child(currentUser.uid)
                                    .child(chatId)
                                    .get().addOnSuccessListener { dataSnapshot ->
                                        val listOfMessageIds = arrayListOf<String>()
                                        dataSnapshot.children.forEach { snapshot ->
                                            val messageId = snapshot
                                                .getValue(PrivateMessage::class.java)?.messageId
                                            if (messageId != null) {
                                                listOfMessageIds.add(messageId)
                                            }
                                        }
                                        if (listOfMessageIds.isNotEmpty()) {
                                            dbRef.child("/user-messages/recent-message/${currentUser.uid}/${chatId}").setValue(null)
                                            listOfMessageIds.forEach { messageId ->
                                                dbRef.child("user-messages").child(currentUser.uid)
                                                    .child(chatId).child(messageId).get()
                                                    .addOnSuccessListener { dataSnapshot ->
                                                        val message = dataSnapshot.getValue<PrivateMessage>()
                                                        if (message?.image != null && message.fromUserId == currentUser.uid) {
                                                            Firebase.storage.getReferenceFromUrl(message.image!!).delete()
                                                                .addOnCompleteListener { task ->
                                                                    if (task.isSuccessful) {
                                                                        val childUpdates = hashMapOf<String, Any?>(
                                                                            "/user-messages/${currentUser.uid}/${chatId}/" +
                                                                                    messageId to null,
                                                                            "/user-messages/${chatId}/${currentUser.uid}/" +
                                                                                    messageId to null
                                                                        )
                                                                        dbRef.updateChildren(childUpdates)
                                                                        Toast.makeText(
                                                                            requireContext(),
                                                                            "Deleted media and message from both ends",
                                                                            Toast.LENGTH_LONG).show()
                                                                    } else {
                                                                        Toast.makeText(
                                                                            requireContext(),
                                                                            "Failed to delete media, hence message from both ends",
                                                                            Toast.LENGTH_LONG).show()
                                                                    }
                                                                }
                                                        } else {
                                                            val childUpdates = hashMapOf<String, Any?>(
                                                                "/user-messages/${currentUser.uid}/${chatId}/" +
                                                                        messageId to null
                                                            )
                                                            dbRef.updateChildren(childUpdates)
                                                        }
                                                    }
                                            }
                                            Toast.makeText(
                                                requireContext(),
                                                "Deleted chat",
                                                Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(
                                                requireContext(),
                                                "Empty chat",
                                                Toast.LENGTH_LONG).show()
                                        }
                                    }
                            }
                            dialog.dismiss()
                            mode?.finish()
                        }.setNegativeButton("No") { dialog, which ->
                            dialog.dismiss()
                        }.show()
                    true
                } else -> false
            }
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            listOfSelectedChats.clear()
            selectedChatsCount = listOfSelectedChats.size
            adapter?.resetSelectedChatsList()
            adapter?.restBackgroundOfSelectedViews()
            actionMode = null
        }
    }


    private fun singularOrPlural(list: ArrayList<String>, singular: String, plural: String): String {
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

    private fun updateSelection(chateeId: String, view: View) {
        if (!listOfSelectedChats.contains(chateeId)) {
            listOfSelectedChats.add(chateeId)
            selectedChatsCount = listOfSelectedChats.size
            actionMode?.title = selectedChatsCount.toString()
        } else {
            listOfSelectedChats.remove(chateeId)
            selectedChatsCount = listOfSelectedChats.size
            actionMode?.title = selectedChatsCount.toString()
        }
        if (selectedChatsCount == 0) {
            actionMode?.title = null
            actionMode?.finish()
        }
    }

}