package com.example.mandarin.view.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import com.example.mandarin.R
import com.example.mandarin.model.Profile
import com.example.mandarin.model.User
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase

class SignInActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var dbRef: DatabaseReference
    private val signInIntent = AuthUI.getInstance()
        .createSignInIntentBuilder()
        .setLogo(R.drawable.signin_image)
        .setAvailableProviders(listOf(
            AuthUI.IdpConfig.PhoneBuilder().build(),
        ))
        .setTheme(R.style.Theme_Colley)
        .build()
    private val signIn: ActivityResultLauncher<Intent> =
        registerForActivityResult(FirebaseAuthUIActivityResultContract()) { result ->
            this.onSignInResult(result) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dbRef = Firebase.database.reference
        auth = Firebase.auth
        authenticateUser()

    }

    private fun authenticateUser() {
        if (auth.currentUser == null) {
            signIn.launch(signInIntent)
        } else {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun onSignInResult(result: FirebaseAuthUIAuthenticationResult) {
        if (result.resultCode == RESULT_OK) {
            addUserToDataBase()
        }
    }

    private fun addUserToDataBase() {

        dbRef.child("users").child(auth.currentUser?.uid!!).get().addOnSuccessListener {
            dataSnapshot ->
            val user = dataSnapshot.getValue(User::class.java)
            if ( user == null) {
                val intent = Intent(this, RegisterActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }


        dbRef.child("profiles").child(auth.currentUser?.uid!!).addListenerForSingleValueEvent(
            object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val profile = snapshot.getValue<Profile>()
                    if (profile == null) {
                        dbRef.child("profiles").child(auth.currentUser?.uid!!)
                            .setValue(Profile(auth.currentUser?.displayName, auth.currentUser?.phoneNumber))
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            }
        )
    }

}