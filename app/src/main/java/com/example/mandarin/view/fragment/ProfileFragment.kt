package com.example.mandarin.view.fragment

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.mandarin.R
import com.example.mandarin.contract.OpenDocumentContract
import com.example.mandarin.databinding.FragmentProfileBinding
import com.example.mandarin.glide.GlideImageLoader
import com.example.mandarin.model.Profile
import com.example.mandarin.view.dialog.EditBioBottomSheetDialogFragment
import com.example.mandarin.view.dialog.EditProfileBottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import android.text.Html

import android.graphics.PorterDuff

import android.graphics.Color

import android.graphics.drawable.ColorDrawable
import android.view.Window
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController


class ProfileFragment :
    Fragment(),
    EditProfileBottomSheetDialogFragment.EditProfileListener,
    EditBioBottomSheetDialogFragment.EditBioListener {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding
    private var editProfileBottomSheetDialog: EditProfileBottomSheetDialogFragment? = null
    private var editBioBottomSheetDialog: EditBioBottomSheetDialogFragment? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var dbRef: DatabaseReference
    val uid: String
        get() = Firebase.auth.currentUser!!.uid
    private val openDocument = registerForActivityResult(OpenDocumentContract()) { uri ->
        if(uri != null) {
            onImageSelected(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dbRef = Firebase.database.reference
        auth = Firebase.auth

    }

    override fun onStart() {
        super.onStart()
        (activity as AppCompatActivity).supportActionBar!!.hide()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getProfile()
        getBio()
        getPhoto()

        binding?.back?.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_homeFragment)

        }

        binding?.editInfoTextView?.setOnClickListener {

            val bundle = bundleOf(
                "nameKey" to binding?.nameTextView?.text.toString(),
                "phoneKey" to binding?.phoneNameTextView?.text.toString()
            )

            editProfileBottomSheetDialog = EditProfileBottomSheetDialogFragment(
                requireContext(),
                this)
            editProfileBottomSheetDialog?.arguments = bundle
            editProfileBottomSheetDialog?.show(childFragmentManager, null)
        }

        binding?.editBioTextView?.setOnClickListener {
            editBioBottomSheetDialog = EditBioBottomSheetDialogFragment(
                requireContext(),
                this)
            editBioBottomSheetDialog?.arguments =
                bundleOf("bioKey" to binding?.bioTextView?.text.toString())
            editBioBottomSheetDialog?.show(childFragmentManager, null)
        }

        binding?.addPhotoFab?.setOnClickListener {
            openDocument.launch(arrayOf("image/*"))
        }
    }

    private fun getProfile() {
        dbRef.child("profiles").child(uid).get().addOnSuccessListener {
                dataSnapshot ->
            val profile = dataSnapshot.getValue<Profile>()
            if (profile != null) {
                    binding?.nameTextView?.text = profile.name
                    binding?.phoneNameTextView?.text = profile.phone
            }
        }
    }

    private fun getPhoto() {
        dbRef.child("photos").child(uid).get().addOnSuccessListener {
                dataSnapshot ->
            val photo = dataSnapshot.getValue<String>()
                if (photo == null) {
                    binding?.profilePhotoImageView?.let {
                        Glide.with(requireContext()).load(R.drawable.ic_person_light_pearl)
                            .into(it)
                    }
                    binding?.photoProgressBar?.visibility = GONE
                } else {
                    val options = RequestOptions()
                        .error(R.drawable.ic_downloading)
                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)

                    binding?.profilePhotoImageView?.visibility = VISIBLE
                    GlideImageLoader(binding?.profilePhotoImageView, binding?.photoProgressBar).load(photo, options);
                }

        }
    }

    private fun getBio() {
        dbRef.child("bios").child(uid).get().addOnSuccessListener {
                dataSnapshot ->
            val bio = dataSnapshot.getValue<String>()
            if (bio == null || bio == "") {
                binding?.bioTextView?.hint = "Talk about yourself"
                binding?.bioTextView?.text = bio
            } else {
                binding?.bioTextView?.text = bio
            }
        }
    }

    private fun onImageSelected(uri: Uri) {
        binding?.photoProgressBar?.visibility = VISIBLE
                    val storageReference = Firebase.storage
                        .getReference(uid)
                        .child("$uid-profile-photo")
                    putImageInStorage(storageReference, uri)
    }

    private fun putImageInStorage(storageReference: StorageReference, uri: Uri) {
        storageReference.putFile(uri)
            .addOnSuccessListener(
                requireActivity()
            ) { taskSnapshot ->
                taskSnapshot.metadata!!.reference!!.downloadUrl
                    .addOnSuccessListener { uri ->
                        dbRef.child("photos").child(uid).setValue(uri.toString())
                            .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                getPhoto()
                            }
                        }
                    }
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun onEditProfile() {
        getProfile()
    }

    override fun onEditBio() {
        getBio()
    }

}
