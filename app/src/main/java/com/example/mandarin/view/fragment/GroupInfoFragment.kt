package com.example.mandarin.view.fragment

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.mandarin.R
import com.example.mandarin.adapter.GroupMembersRecyclerAdapter
import com.example.mandarin.contract.OpenDocumentContract
import com.example.mandarin.databinding.FragmentGroupInfoBinding
import com.example.mandarin.glide.GlideImageLoader
import com.example.mandarin.model.Profile
import com.example.mandarin.view.dialog.AddMoreGroupMemberBottomSheetDialogFragment
import com.example.mandarin.view.dialog.EditGroupAboutBottomSheetDialogFragment
import com.example.mandarin.view.dialog.EditGroupNameBottomSheetDialogFragment
import com.example.mandarin.view.dialog.MemberInteractionBottomSheetDialogFragment
import com.example.mandarin.wrapper.WrapContentLinearLayoutManager
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage


class GroupInfoFragment :
    Fragment(),
    GroupMembersRecyclerAdapter.ItemClickedListener,
    EditGroupAboutBottomSheetDialogFragment.EditGroupAboutListener,
    EditGroupNameBottomSheetDialogFragment.EditGroupNameListener{

    private val args: GroupInfoFragmentArgs by navArgs()
    private var _binding: FragmentGroupInfoBinding? = null
    private val binding get() = _binding!!
    lateinit var recyclerView: RecyclerView
    private lateinit var dbRef: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: FirebaseUser
    private var adapter: GroupMembersRecyclerAdapter? = null
    private var editGroupAboutBottomSheetDialog: EditGroupAboutBottomSheetDialogFragment? = null
    private var addMoreGroupMemberSheetDialog: AddMoreGroupMemberBottomSheetDialogFragment? = null
    private var memberInteractionSheetDialog: MemberInteractionBottomSheetDialogFragment? = null
    private var editGroupNameBottomSheetDialog: EditGroupNameBottomSheetDialogFragment? = null
    val uid: String
        get() = currentUser.uid
    private val openDocument = registerForActivityResult(OpenDocumentContract()) { groupImageUri ->
        if(groupImageUri != null) {
            onImageSelected(groupImageUri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentGroupInfoBinding.inflate(inflater, container, false)
        recyclerView = binding.groupMembersRecyclerView
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbRef = Firebase.database.reference
        auth = Firebase.auth
        currentUser = auth.currentUser!!
        getGroupName()
        getGroupPhoto()
        getGroupDescription()
        val messagesRef = dbRef.child("groups").child(args.groupId).child("members")

        val options = FirebaseRecyclerOptions.Builder<String>()
            .setQuery(messagesRef, String::class.java)
            .setLifecycleOwner(viewLifecycleOwner)
            .build()

        adapter = GroupMembersRecyclerAdapter(
            options,
            currentUser,
            this,
            requireContext(),
            args.groupId)

        recyclerView.layoutManager =
            WrapContentLinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        recyclerView.adapter = adapter

        binding.editAboutTextView.setOnClickListener {
            editGroupAboutBottomSheetDialog = EditGroupAboutBottomSheetDialogFragment(
                requireContext(), this)
            editGroupAboutBottomSheetDialog?.arguments = bundleOf(
                "aboutKey" to binding.groupDescriptionTextView.text.toString(),
                "groupIdKey" to args.groupId
            )
            editGroupAboutBottomSheetDialog?.show(childFragmentManager, null)
        }

        binding.addPhotoButton.setOnClickListener {
            openDocument.launch(arrayOf("image/*"))
        }

        binding.editGroupNameButton.setOnClickListener {
            editGroupNameBottomSheetDialog =
                EditGroupNameBottomSheetDialogFragment(requireContext(), this)
            editGroupNameBottomSheetDialog?.arguments = bundleOf(
                "groupNameKey" to binding.groupNameTextView.text.toString(),
                "groupIdKey" to args.groupId
            )
            editGroupNameBottomSheetDialog?.show(childFragmentManager, null)
        }

        binding.addGroupMemberTextView.setOnClickListener {
                addMoreGroupMemberSheetDialog = AddMoreGroupMemberBottomSheetDialogFragment(
                    requireContext()
                )

                addMoreGroupMemberSheetDialog?.arguments =
                    bundleOf("groupIdKey" to args.groupId)
                addMoreGroupMemberSheetDialog?.show(childFragmentManager, null)
        }

        binding.leaveGroupTextView.setOnClickListener {
            dbRef.child("groups").child(args.groupId).child("groupAdmins")
                .addListenerForSingleValueEvent(
                object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val admins = snapshot.getValue<ArrayList<String>>()
                        if (admins != null && admins.contains(uid)) {
                            context?.let { context -> Toast.makeText(context,
                                "You cannot leave the group while an admin",
                                Toast.LENGTH_LONG).show()
                            }
                        } else {
                            context?.let { context ->
                                AlertDialog.Builder(context)
                                    .setMessage("Are you sure you want to leave this group?")
                                    .setPositiveButton("Yes") { dialog, _ ->
                                        dbRef.child("groups").child(args.groupId)
                                            .child("members").runTransaction(
                                            object : Transaction.Handler {
                                                override fun doTransaction(currentData: MutableData): Transaction.Result {
                                                    val list = currentData.getValue<ArrayList<String>>()
                                                        ?: return Transaction.success(currentData)
                                                    if (list.contains(uid)) {
                                                        list.remove(uid)
                                                    }
                                                    currentData.value = list
                                                    return Transaction.success(currentData)
                                                }

                                                override fun onComplete(
                                                    error: DatabaseError?,
                                                    committed: Boolean,
                                                    currentData: DataSnapshot?
                                                ) {
                                                    if (committed && error == null) {
                                                        dbRef.child("user-groups").child(uid).runTransaction(
                                                            object : Transaction.Handler {
                                                                override fun doTransaction(
                                                                    currentData: MutableData
                                                                ): Transaction.Result {
                                                                    val listOfGroups = currentData.getValue<ArrayList<String>>()
                                                                        ?: return Transaction.success(currentData)
                                                                    if (listOfGroups.contains(args.groupId)) {
                                                                        listOfGroups.remove(args.groupId)
                                                                    }
                                                                    currentData.value = listOfGroups
                                                                    return Transaction.success(currentData)
                                                                }

                                                                override fun onComplete(
                                                                    error: DatabaseError?,
                                                                    committed: Boolean,
                                                                    currentData: DataSnapshot?
                                                                ) {}

                                                            }
                                                        )
                                                        Snackbar.make(requireView(), "You are no longer a member of this group", Snackbar.LENGTH_LONG).show()
                                                    } else { }
                                                }

                                            }
                                        )
                                        dialog.dismiss()
                                    }.setNegativeButton("No") { dialog, _ -> dialog.dismiss()
                                    }.show()
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                }
            )
        }


    }

    private fun onImageSelected(groupImageUri: Uri) {
        binding.photoProgressBar.visibility = VISIBLE
        val storageReference = Firebase.storage
            .getReference(args.groupId)
            .child("${auth.currentUser?.uid!!}-group-photo")
        putImageInStorage(storageReference, groupImageUri)
    }

    private fun putImageInStorage(storageReference: StorageReference, groupImageUri: Uri) {
        storageReference.putFile(groupImageUri)
            .addOnSuccessListener(
                requireActivity()
            ) { taskSnapshot ->
                taskSnapshot.metadata!!.reference!!.downloadUrl
                    .addOnSuccessListener { uri ->
                        dbRef.child("groups-id-name-photo").child(args.groupId)
                            .child("groupPhoto").setValue(uri.toString())
                            .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(
                                    requireContext(),
                                    "Unsuccessful",
                                    Toast.LENGTH_SHORT).show()
                                getGroupPhoto()
                            } else {
                               Toast.makeText(
                                   requireContext(),
                                   "Unsuccessful",
                                   Toast.LENGTH_LONG).show()
                            }
                        }
                    }
            }
            .addOnFailureListener(requireActivity()) { e ->
                Toast.makeText(requireContext(), "Unsuccessful", Toast.LENGTH_LONG).show()
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun onItemClick(memberId: String) {
        if (memberId != uid) {
            memberInteractionSheetDialog =
                MemberInteractionBottomSheetDialogFragment(requireContext())
            memberInteractionSheetDialog?.arguments = bundleOf("memberIdKey" to memberId)
            memberInteractionSheetDialog?.show(childFragmentManager, null)
        }
    }

    override fun onItemLongCLicked(memberId: String) {
        if(memberId == uid) {
            Toast.makeText(
                requireContext(),
                "Leave the group instead, you cannot remove yourself",
                Toast.LENGTH_LONG).show()
        } else  {
            dbRef.child("groups").child(args.groupId).child("groupAdmins")
                .addListenerForSingleValueEvent(
                object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val groupAdmins = snapshot.getValue<ArrayList<String>>()
                        if (groupAdmins?.contains(currentUser.uid) == true) {
                            dbRef.child("profiles").child(memberId).addListenerForSingleValueEvent(
                                object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        val profile = snapshot.getValue<Profile>()
                                        if (profile != null) {
                                            //open dialog
                                            AlertDialog.Builder(requireContext())
                                                .setMessage("Remove ${profile.name} from this group?")
                                                .setPositiveButton("Yes") {
                                                        dialog, _ ->
                                                    dbRef.child("groups").child(args.groupId).child("members").runTransaction(
                                                        object : Transaction.Handler {
                                                            override fun doTransaction(currentData: MutableData): Transaction.Result {
                                                                val list = currentData.getValue<ArrayList<String>>()
                                                                    ?: return Transaction.success(currentData)
                                                                if (list.contains(memberId)) {
                                                                    list.remove(memberId)
                                                                }
                                                                currentData.value = list
                                                                return Transaction.success(currentData)
                                                            }

                                                            override fun onComplete(
                                                                error: DatabaseError?,
                                                                committed: Boolean,
                                                                currentData: DataSnapshot?
                                                            ) {
                                                                if (committed && error == null) {

                                                                    dbRef.child("user-groups").child(memberId).runTransaction(
                                                                        object : Transaction.Handler {
                                                                            override fun doTransaction(
                                                                                currentData: MutableData
                                                                            ): Transaction.Result {
                                                                                val listOfGroups = currentData.getValue<ArrayList<String>>()
                                                                                    ?: return Transaction.success(currentData)
                                                                                if (listOfGroups.contains(args.groupId)) {
                                                                                    listOfGroups.remove(args.groupId)
                                                                                }
                                                                                currentData.value = listOfGroups
                                                                                return Transaction.success(currentData)
                                                                            }

                                                                            override fun onComplete(
                                                                                error: DatabaseError?,
                                                                                committed: Boolean,
                                                                                currentData: DataSnapshot?
                                                                            ) {
                                                                                if (committed && error == null) {
                                                                                   if(groupAdmins.contains(memberId)) {
                                                                                       dbRef.child("groups").child(args.groupId).child("groupAdmins").runTransaction(
                                                                                           object :
                                                                                               Transaction.Handler {
                                                                                               override fun doTransaction(
                                                                                                   currentData: MutableData
                                                                                               ): Transaction.Result {
                                                                                                   val listOfAdmins = currentData.getValue<ArrayList<String>>()
                                                                                                       ?: return Transaction.success(currentData)
                                                                                                   if (listOfAdmins.contains(memberId)) {
                                                                                                       listOfAdmins.remove(memberId)
                                                                                                   }
                                                                                                   currentData.value = listOfAdmins
                                                                                                   return Transaction.success(currentData)
                                                                                               }

                                                                                               override fun onComplete(
                                                                                                   error: DatabaseError?,
                                                                                                   committed: Boolean,
                                                                                                   currentData: DataSnapshot?
                                                                                               ) {}

                                                                                           }
                                                                                       )
                                                                                   }

                                                                                }
                                                                            }

                                                                        }
                                                                    )
                                                                    Snackbar.make(requireView(), "${profile.name} removed successfully", Snackbar.LENGTH_LONG).show()
                                                                }
                                                            }

                                                        }
                                                    )
                                                    dialog.dismiss()
                                                }.setNegativeButton("No") {
                                                        dialog, _ -> dialog.dismiss()
                                                }.show()
                                        }
                                    }

                                    override fun onCancelled(error: DatabaseError) {}
                                }
                            )
                        } else {
                            Toast.makeText(requireContext(), "Only group admins can remove members", Toast.LENGTH_LONG).show()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                }
            )
        }

    }


    override fun onGroupAboutChanged() {
        getGroupDescription()
    }

    private fun getGroupDescription() {
        dbRef.child("groups").child(args.groupId).child("description").get()
            .addOnSuccessListener {  dataSnapShot ->
                val description = dataSnapShot.getValue(String::class.java)
                if (description != null) {
                    binding.groupDescriptionTextView.text = description
                } else {
                    binding.groupDescriptionTextView.hint = "Describe this group"
                }
            }
    }

    private fun getGroupName() {
        dbRef.child("groups-id-name-photo").child(args.groupId).child("name")
            .get().addOnSuccessListener {  dataSnapShot ->
                val groupName = dataSnapShot.getValue(String::class.java)
                if (groupName != null) {
                    binding.groupNameTextView.text = groupName
                }
            }
    }

    private fun getGroupPhoto() {
        dbRef.child("groups-id-name-photo").child(args.groupId)
            .child("groupPhoto").get().addOnSuccessListener { dataSnapShot ->
                val photoUrl = dataSnapShot.getValue(String::class.java)
                if (photoUrl == null) {
                    Glide.with(requireContext()).load(R.drawable.ic_group)
                        .into(binding.groupPhotoImageView)
                    binding.photoProgressBar.visibility = GONE
                } else {
                    val options = RequestOptions()
                        .error(R.drawable.ic_downloading)
                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)

                    binding.groupPhotoImageView.visibility = VISIBLE
                    GlideImageLoader(binding.groupPhotoImageView, binding.photoProgressBar)
                        .load(photoUrl, options)
                }
            }
    }

    override fun onGroupNameChanged() {
        getGroupName()
    }

}