package com.example.mandarin.view.fragment

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
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
import com.example.mandarin.adapter.PrivateMessageRecyclerAdapter
import com.example.mandarin.contract.OpenDocumentContract
import com.example.mandarin.databinding.FragmentPrivateMessageBinding
import com.example.mandarin.model.*
import com.example.mandarin.observer.PrivateMessageScrollToBottomObserver
import com.example.mandarin.observer.SendButtonObserver
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
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.EmojiPopup
import com.vanniktech.emoji.google.GoogleEmojiProvider


class PrivateMessageFragment :
    Fragment(),
    PrivateMessageRecyclerAdapter.DataChangedListener,
    PrivateMessageRecyclerAdapter.ItemClickedListener {

    private val args: PrivateMessageFragmentArgs by navArgs()
    private var _binding: FragmentPrivateMessageBinding? = null
    private val binding get() = _binding!!
    private lateinit var dbRef: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: FirebaseUser
    private lateinit var adapter: PrivateMessageRecyclerAdapter
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
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
        inflater.inflate(R.menu.private_message_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.chatee_info_menu_item -> {
                val action = PrivateMessageFragmentDirections
                    .actionPrivateMessageFragmentToChateeInfoFragment(args.chateeId)
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
    ): View {
        _binding = FragmentPrivateMessageBinding.inflate(inflater, container, false)
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
        Log.e("CHAT ID ", "${args.chateeId}")

        dbRef.child("profiles").child(args.chateeId).get().addOnSuccessListener {
            dataSnapshot ->
            val profile = dataSnapshot.getValue<Profile>()
            if (profile != null) {
                (activity as AppCompatActivity?)?.supportActionBar?.title = profile.name
            }
        }

        val messagesRef = dbRef.child("user-messages").child(currentUser.uid)
            .child(args.chateeId)

        val options = FirebaseRecyclerOptions.Builder<PrivateMessage>()
            .setQuery(messagesRef, PrivateMessage::class.java)
            .setLifecycleOwner(viewLifecycleOwner)
            .build()

        adapter = PrivateMessageRecyclerAdapter(
            options,
            currentUser,
            this,
            this
        )

        manager =  WrapContentLinearLayoutManager(
            requireContext(),
            LinearLayoutManager.VERTICAL,
            false)

        manager.stackFromEnd = true
        recyclerView.layoutManager = manager
        recyclerView.adapter = adapter

        adapter.registerAdapterDataObserver(
            PrivateMessageScrollToBottomObserver(
                binding.messageRecyclerView,
                adapter,
                manager)
        )
        binding.messageEditText.addTextChangedListener(SendButtonObserver(binding.sendButton))

        binding.sendButton.setOnClickListener {

            if (binding.messageEditText.text?.trim()?.toString() != "") {

                val key = dbRef.child("user-messages").child(currentUser.uid)
                    .child(args.chateeId).push().key

                if(key != null) {
                    val privateMessage = PrivateMessage(
                        fromUserId = currentUser.uid,
                        toUserId = args.chateeId,
                        text = binding.messageEditText.text.toString(),
                        messageId = key
                    )

                    val childUpdates = hashMapOf<String, Any>(
                        "/user-messages/${currentUser.uid}/${args.chateeId}/$key" to privateMessage,
                        "/user-messages/recent-message/${currentUser.uid}/${args.chateeId}" to privateMessage,
                        "/user-messages/${args.chateeId}/${currentUser.uid}/$key" to privateMessage,
                        "/user-messages/recent-message/${args.chateeId}/${currentUser.uid}" to privateMessage
                    )
                    dbRef.updateChildren(childUpdates)

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
        val tempMessage = PrivateMessage(
            fromUserId = currentUser.uid,
            toUserId = args.chateeId,
            image = LOADING_IMAGE_URL
        )
        dbRef
            .child("user-messages")
            .child(currentUser.uid)
            .child(args.chateeId)
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
                        .child(args.chateeId)
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
                        val privateMessage = PrivateMessage(
                                fromUserId = currentUser.uid,
                                toUserId = args.chateeId,
                                image = uri.toString(),
                                messageId = key
                            )

                        val childUpdates = hashMapOf<String, Any>(
                            "/user-messages/${currentUser.uid}/${args.chateeId}/$key" to privateMessage,
                            "/user-messages/recent-message/${currentUser.uid}/${args.chateeId}" to privateMessage,
                            "/user-messages/${args.chateeId}/${currentUser.uid}/$key" to privateMessage,
                            "/user-messages/recent-message/${args.chateeId}/${currentUser.uid}" to privateMessage
                        )
                        dbRef.updateChildren(childUpdates)

                    }
            }
    }

    @SuppressLint("SetTextI18n")
    override fun onDataAvailable(snapshotArray: ObservableSnapshotArray<PrivateMessage>) {

        binding.progressBar.visibility = View.GONE
        binding.linearLayout.visibility = View.VISIBLE

        if (snapshotArray.isEmpty()) {
            dbRef.child("profiles").child(args.chateeId).get().addOnSuccessListener {
                    dataSnapshot ->
                val profile = dataSnapshot.getValue<Profile>()
                if (profile != null) {
                    binding.startChattingTextView.text =
                        "Start a conversation with ${profile.name}"
                }
            }
        } else {
            binding.startChattingTextView.visibility = View.GONE
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun onItemLongCLicked(message: PrivateMessage, view: View) {
        if (actionMode == null) {
            actionMode = (activity as AppCompatActivity?)!!
                .startSupportActionMode(actionModeCallBack)
        }
        if(message.messageId != null) {
            updateSelection(message.messageId!!, view)
        }
    }

    override fun onItemClicked(message: PrivateMessage, view: View) {
        if (actionMode != null) {
            if(message.messageId != null) {
                updateSelection(message.messageId!!, view)
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
                        .setMessage("Delete ${singularOrPlural(listOfSelectedMessages, "this message", "these messages")}?")
                        .setPositiveButton("Yes") { dialog, which ->
                            listOfSelectedMessages.forEach { messageId ->
                                dbRef.child("user-messages").child(currentUser.uid)
                                    .child(args.chateeId).child(messageId).get()
                                    .addOnSuccessListener { dataSnapshot ->
                                        val message = dataSnapshot.getValue<PrivateMessage>()
                                        if (message?.image != null && message.fromUserId == currentUser.uid) {
                                            Firebase.storage.getReferenceFromUrl(message.image!!).delete()
                                                .addOnCompleteListener { task ->
                                                    if (task.isSuccessful) {
                                                        val childUpdates = hashMapOf<String, Any?>(
                                                            "/user-messages/${currentUser.uid}/${args.chateeId}/" +
                                                                    messageId to null,
                                                            "/user-messages/${args.chateeId}/${currentUser.uid}/" +
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
                                                "/user-messages/${currentUser.uid}/${args.chateeId}/" +
                                                        messageId to null
                                            )
                                            dbRef.updateChildren(childUpdates)
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
            adapter.resetMessagesSelectedList()
            adapter.restBackgroundOfSelectedViews()
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
            actionMode ?.title = selectedMessagesCount.toString()
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

    companion object {
        private const val TAG = "PrivateMessageFragment"
        private const val LOADING_IMAGE_URL = "https://firebasestorage.googleapis.com/v0/b/" +
                "colley-c37ea.appspot.com/o/loading_gif%20copy.gif?alt=media&token=022770e5-9db3-" +
                "426c-9ee2-582b9d66fbac"
    }

}