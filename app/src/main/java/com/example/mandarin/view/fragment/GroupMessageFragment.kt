package com.example.mandarin.view.fragment

import android.net.Uri
import android.os.Bundle
import android.view.*
import android.view.View.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mandarin.R
import com.example.mandarin.adapter.GroupMessageRecyclerAdapter
import com.example.mandarin.contract.OpenDocumentContract
import com.example.mandarin.databinding.FragmentGroupMessageBinding
import com.example.mandarin.model.GroupMessage
import com.example.mandarin.observer.GroupMessageScrollToBottomObserver
import com.example.mandarin.observer.SendButtonObserver
import com.example.mandarin.wrapper.WrapContentLinearLayoutManager
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.EmojiPopup
import com.vanniktech.emoji.google.GoogleEmojiProvider


class GroupMessageFragment :
    Fragment(),
    GroupMessageRecyclerAdapter.DataChangedListener,
    GroupMessageRecyclerAdapter.ItemClickedListener {

    private val args: GroupMessageFragmentArgs by navArgs()
    private var _binding: FragmentGroupMessageBinding? = null
    private val binding get() = _binding!!
    private lateinit var dbRef: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: FirebaseUser
    private var adapter: GroupMessageRecyclerAdapter? = null
    private lateinit var manager: WrapContentLinearLayoutManager
    private lateinit var recyclerView: RecyclerView
    private val openDocument = registerForActivityResult(OpenDocumentContract()) { uri ->
        if(uri != null) {
            onImageSelected(uri)
        }
    }
    private var actionMode: ActionMode? = null
    private var listOfSelectedMessages = arrayListOf<String>()
    private var selectedMessagesCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true);
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
        inflater.inflate(R.menu.group_message_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.group_info_menu_item -> {
                val action = GroupMessageFragmentDirections
                    .actionGroupMessageFragmentToGroupInfoFragment(args.groupId)
                findNavController().navigate(action)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private val emojiPopup by lazy { EmojiPopup.Builder.fromRootView(binding.Smiles).build(binding.messageEditText)  }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentGroupMessageBinding.inflate(inflater, container, false)
        EmojiManager.install(GoogleEmojiProvider())
        recyclerView = binding.messageRecyclerView
        binding.Smiles.setOnClickListener{
            emojiPopup.toggle()
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbRef = Firebase.database.reference
        auth = Firebase.auth
        currentUser = auth.currentUser!!

        dbRef.child("groups").child(args.groupId).child("name")
            .addListenerForSingleValueEvent(
            object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val groupName = snapshot.getValue<String>()
                    if (groupName != null) {
                        (activity as AppCompatActivity?)?.supportActionBar?.title = groupName
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            }
        )

        val messagesRef = dbRef.child("group-messages").child(args.groupId)

        val options = FirebaseRecyclerOptions.Builder<GroupMessage>()
            .setQuery(messagesRef, GroupMessage::class.java)
            .setLifecycleOwner(viewLifecycleOwner)
            .build()

        adapter = GroupMessageRecyclerAdapter(
            options,
            currentUser,
            this,
            this,
            requireContext())
        manager =  WrapContentLinearLayoutManager(
            requireContext(),
            LinearLayoutManager.VERTICAL,
            false)
        manager.stackFromEnd = true
        recyclerView.layoutManager = manager
        recyclerView.adapter = adapter

        adapter?.registerAdapterDataObserver(
            GroupMessageScrollToBottomObserver(
                binding.messageRecyclerView,
                adapter!!,
                manager)
        )

        binding.messageEditText.addTextChangedListener(SendButtonObserver(binding.sendButton))

        binding.sendButton.setOnClickListener {
            if (binding.messageEditText.text?.trim()?.toString() != "") {
                val key = dbRef.child("user-messages").child(args.groupId).push().key

                if(key != null) {
                    val groupMessage = GroupMessage(
                        userId = currentUser.uid,
                        text = binding.messageEditText.text.toString(),
                        messageId = key
                    )
                    dbRef.child("group-messages").child(args.groupId).child(key)
                        .setValue(groupMessage)
                    dbRef.child("group-messages").child("recent-message")
                        .child(args.groupId).setValue(groupMessage)
                    binding.messageEditText.setText("")
                } else {
                    Toast.makeText(requireContext(), "Unsuccessful", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.addMessageImageView.setOnClickListener {
            openDocument.launch(arrayOf("image/*"))
        }

    }

    private fun onImageSelected(uri: Uri) {

        val tempMessage = GroupMessage(
            userId = currentUser.uid,
            image = LOADING_IMAGE_URL
        )
        dbRef
            .child("group-messages")
            .child(args.groupId)
            .push()
            .setValue(
                tempMessage,
                DatabaseReference.CompletionListener { databaseError, databaseReference ->
                    if (databaseError != null) {
                        return@CompletionListener
                    }
                    val key = databaseReference.key
                    val storageReference = Firebase.storage
                        .getReference(currentUser.uid)
                        .child(key!!)
                        .child(uri.lastPathSegment!!)
                    putImageInStorage(storageReference, uri, key)
                })
    }

    private fun putImageInStorage(storageReference: StorageReference, uri: Uri, key: String?) {
        storageReference.putFile(uri)
            .addOnSuccessListener(
                requireActivity()
            ) { taskSnapshot ->
                taskSnapshot.metadata!!.reference!!.downloadUrl
                    .addOnSuccessListener { uri ->
                        val groupMessage =
                            GroupMessage(
                                userId = currentUser.uid,
                                image = uri.toString(),
                                messageId = key
                            )
                        dbRef
                            .child("group-messages")
                            .child(args.groupId)
                            .child(key!!)
                            .setValue(groupMessage)
                        dbRef.child("group-messages").child("recent-message")
                            .child(args.groupId).setValue(groupMessage)
                    }
            }
    }


    override fun onStop() {
        super.onStop()
        binding.linearLayout.visibility = VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun onDataAvailable() {
        binding.progressBar.visibility = GONE
        binding.linearLayout.visibility = VISIBLE
    }

    companion object {
        private const val TAG = "GroupMessageFragment"
        private const val LOADING_IMAGE_URL = "https://firebasestorage.googleapis.com/v0/b/colley-" +
                "c37ea.appspot.com/o/loading_gif%20copy.gif?alt=media&token=022770e5-9db3-426c" +
                "-9ee2-582b9d66fbac"
    }

    override fun onItemLongCLicked(message: GroupMessage, view: View) {
        if (actionMode == null) {
            actionMode = (activity as AppCompatActivity?)!!
                .startSupportActionMode(actionModeCallBack)
        }
        if(message.messageId != null) {
            updateSelection(message.messageId!!, view)
        }
    }

    override fun onUserClicked(userId: String, view: View) {
        val action = GroupMessageFragmentDirections
            .actionGroupMessageFragmentToUserInfoFragment(userId)
        findNavController().navigate(action)
    }

    override fun onItemClicked(message: GroupMessage, root: View) {
        if (actionMode != null) {
            if(message.messageId != null) {
                updateSelection(message.messageId!!, root)
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
                            "Delete " + singularOrPlural(
                                listOfSelectedMessages,
                                "this message",
                                "these messages"
                            ) + "? \nGroup members can only delete their own messages"
                        )
                        .setPositiveButton("Yes") { dialog, which ->
                            listOfSelectedMessages.forEach { messageId ->
                                dbRef.child("group-messages").child(args.groupId)
                                    .child(messageId).get()
                                    .addOnSuccessListener { dataSnapshot ->
                                        val message = dataSnapshot.getValue<GroupMessage>()
                                        if (message?.userId == currentUser.uid) {
                                            if(message.image != null) {
                                                Firebase.storage.getReferenceFromUrl(message.image!!)
                                                    .delete()
                                                    .addOnCompleteListener { task ->
                                                        if (task.isSuccessful) {
                                                            Toast.makeText(
                                                                requireContext(),
                                                                "Deleted media",
                                                                Toast.LENGTH_LONG).show()
                                                        } else {
                                                            Toast.makeText(
                                                                requireContext(),
                                                                "Failed to delete media",
                                                                Toast.LENGTH_LONG).show()
                                                        }
                                                    }
                                            }
                                            dbRef.child("group-messages")
                                                .child(args.groupId)
                                                .child(messageId).setValue(null)
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
            listOfSelectedMessages.clear()
            selectedMessagesCount = listOfSelectedMessages.size
            adapter?.resetMessagesSelectedList()
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

    private fun updateSelection(messageId: String, view: View) {
        if (!listOfSelectedMessages.contains(messageId)) {
            listOfSelectedMessages.add(messageId)
            selectedMessagesCount = listOfSelectedMessages.size
            actionMode?.title = selectedMessagesCount.toString()
        } else {
            listOfSelectedMessages.remove(messageId)
            selectedMessagesCount = listOfSelectedMessages.size
            actionMode?.title = selectedMessagesCount.toString()
        }
        if (selectedMessagesCount == 0) {
            actionMode?.title = null
            actionMode?.finish()
        }
    }
}