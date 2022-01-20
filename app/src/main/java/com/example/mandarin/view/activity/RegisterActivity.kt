package com.example.mandarin.view.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.mandarin.R
import com.example.mandarin.contract.OpenDocumentContract
import com.example.mandarin.databinding.ActivityRegisterBinding
import com.example.mandarin.glide.GlideImageLoader
import com.example.mandarin.model.Profile
import com.example.mandarin.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var dbRef: DatabaseReference
    private lateinit var auth: FirebaseAuth
    val uid: String
        get() = Firebase.auth.currentUser!!.uid
    private val openDocument = registerForActivityResult(OpenDocumentContract()) { uri ->
        if(uri != null) {
            onImageSelected(uri)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbRef = Firebase.database.reference
        auth = Firebase.auth

        with(binding){
            photoProgressBar.visibility = View.GONE
           saveAboutButton.setOnClickListener {
               if(editNameEditText.text.toString().isNotEmpty()) {
                   dbRef.child("users").child(auth.currentUser?.uid!!)
                       .setValue(User(auth.currentUser?.uid, auth.currentUser?.phoneNumber))
                   dbRef.child("profiles").child(auth.currentUser?.uid!!)
                       .setValue(Profile(name = editNameEditText.text.toString(), phone = auth.currentUser!!.phoneNumber))
                   val intent = Intent(this@RegisterActivity, MainActivity::class.java)
                   startActivity(intent)
                   finish()
               }
               else editNameEditText.error = "Name is required field"
           }
            image.setOnClickListener {
                openDocument.launch(arrayOf("image/*"))
            }
        }
    }

    private fun onImageSelected(uri: Uri) {
        binding.photoProgressBar.visibility = View.VISIBLE
        val storageReference = Firebase.storage
            .getReference(uid)
            .child("$uid-profile-photo")
        putImageInStorage(storageReference, uri)
    }

    private fun getPhoto() {
        dbRef.child("photos").child(uid).get().addOnSuccessListener {
                dataSnapshot ->
            val photo = dataSnapshot.getValue<String>()
            if (photo == null) {
                binding.image.let {
                    Glide.with(this).load(R.drawable.ic_person_light_pearl)
                        .into(it)
                }
                binding.photoProgressBar.visibility = View.GONE
            } else {
                val options = RequestOptions()
                    .error(R.drawable.ic_downloading)
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)

                binding.image.visibility = View.VISIBLE
                GlideImageLoader(binding.image, binding.photoProgressBar).load(photo, options)
            }
        }
    }

    private fun putImageInStorage(storageReference: StorageReference, uri: Uri) {
        storageReference.putFile(uri)
            .addOnSuccessListener(
                this
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
}