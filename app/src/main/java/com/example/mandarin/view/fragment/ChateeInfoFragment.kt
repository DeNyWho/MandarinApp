package com.example.mandarin.view.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.mandarin.R
import com.example.mandarin.databinding.FragmentChateeInfoBinding
import com.example.mandarin.glide.GlideImageLoader
import com.example.mandarin.model.Profile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase

class ChateeInfoFragment : Fragment() {

    private val args: ChateeInfoFragmentArgs by navArgs()
    private var _binding: FragmentChateeInfoBinding? = null
    private val binding get() = _binding
    private lateinit var auth: FirebaseAuth
    private lateinit var dbRef: DatabaseReference
    val uid: String
        get() = Firebase.auth.currentUser!!.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dbRef = Firebase.database.reference

        auth = Firebase.auth
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentChateeInfoBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbRef.child("profiles").child(args.chateeId).get().addOnSuccessListener {
            dataSnapshot ->
            val profile = dataSnapshot.getValue<Profile>()
            if (profile != null) {
                binding?.nameTextView?.text = profile.name
                binding?.schoolNameTextView?.text = profile.phone
            }
        }

        dbRef.child("bios").child(args.chateeId).get().addOnSuccessListener {
            dataSnapshot ->
            val bio = dataSnapshot.getValue<String>()
            if (bio == null || bio == "") {
                binding?.bioTextView?.hint = "User hasn't written about themself yet"
                binding?.bioTextView?.text = bio
            } else {
                binding?.bioTextView?.text = bio
            }
        }

        dbRef.child("photos").child(args.chateeId).get().addOnSuccessListener {
            dataSnapshot ->
            val photo = dataSnapshot.getValue<String>()
            if (photo == null) {
                binding?.profilePhotoImageView?.let {
                    Glide.with(requireContext()).load(R.drawable.ic_person_light_pearl).into(it)
                }
                binding?.photoProgressBar?.visibility = View.GONE
            } else {
                val options = RequestOptions()
                    .error(R.drawable.ic_downloading)
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)

                binding?.profilePhotoImageView?.visibility = View.VISIBLE
                GlideImageLoader(binding?.profilePhotoImageView, binding?.photoProgressBar)
                    .load(photo, options);
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

}