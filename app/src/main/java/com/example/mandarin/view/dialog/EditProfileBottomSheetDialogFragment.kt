package com.example.mandarin.view.dialog

import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.mandarin.R
import com.example.mandarin.databinding.BottomSheetDialogFragmentEditProfileBinding
import com.example.mandarin.model.Profile
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase


class EditProfileBottomSheetDialogFragment (
    private val parentContext: Context,
    private val editProfileListener: EditProfileListener
        )
    : BottomSheetDialogFragment() {

    private var _binding: BottomSheetDialogFragmentEditProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var dbRef: DatabaseReference
    private lateinit var currentUser: FirebaseUser
    private val uid: String
        get() = currentUser.uid

    interface EditProfileListener {
        fun onEditProfile()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetDialogFragmentEditProfileBinding
            .inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbRef = Firebase.database.reference
        currentUser = Firebase.auth.currentUser!!

        val bundledName = arguments?.getString("nameKey")
        val bundledPhone = arguments?.getString("phoneKey")

        with(binding) {
            editNameEditText.setText(bundledName)
            editPhoneEditText.setText(bundledPhone )
        }


        with(binding) {
            saveProfileButton.setOnClickListener {

                val name = editNameEditText.text.toString()
                val phone = editPhoneEditText.text.toString()

                if( TextUtils.isEmpty(name.trim()) ||
                    TextUtils.isEmpty(phone.trim())) {
                    Toast.makeText(parentContext, "Fields cannot be empty", Toast.LENGTH_LONG)
                        .show()
                } else {
                    val profile = Profile(name, phone)
                    saveProfile(profile)
                }
            }
        }
    }

    private fun saveProfile(profile: Profile) {

        setEditingEnabled(false)

        dbRef.child("profiles").child(uid).setValue(profile).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(parentContext, "Updated", Toast.LENGTH_LONG).show()
                editProfileListener.onEditProfile()
                this.dismiss()
            } else {
                Toast.makeText(parentContext, "Unsuccessful", Toast.LENGTH_LONG).show()
                setEditingEnabled(true)
                binding.saveProfileButton.text = getString(R.string.retry_text)
            }
        }
    }

    private fun setEditingEnabled(enabled: Boolean) {
        with(binding) {
            editNameEditText.isEnabled = enabled
            editPhoneEditText.isEnabled = enabled
            saveProfileButton.isEnabled = enabled
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

}