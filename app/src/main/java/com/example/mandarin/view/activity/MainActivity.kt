package com.example.mandarin.view.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.mandarin.R
import com.example.mandarin.databinding.ActivityMainBinding
import com.example.mandarin.glide.GlideImageLoader
import com.example.mandarin.model.Contact
import com.example.mandarin.model.Profile
import com.example.mandarin.model.User
import com.example.mandarin.objects.FirebaseManager
import com.firebase.ui.auth.AuthUI
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.kotlinandroidextensions.GroupieViewHolder
import io.reactivex.rxjava3.internal.util.NotificationLite.getValue

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var auth: FirebaseAuth
    private lateinit var dbRef: DatabaseReference
    private lateinit var header: View
    private lateinit var photoEventListener: ValueEventListener
    private lateinit var profileEventListener: ValueEventListener
    private lateinit var contactsEventListener: ValueEventListener


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        setSupportActionBar(view.findViewById(R.id.toolbar))

        navController = findNavController(R.id.mainNavGraphFragmentContainerView)
        drawerLayout = binding.mainActivityDrawerLayout

        drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
            override fun onDrawerClosed(drawerView: View) {}
            override fun onDrawerStateChanged(newState: Int) {}
            override fun onDrawerOpened(drawerView: View) {
                dbRef.child("photos").child(auth.currentUser?.uid!!)
                    .addListenerForSingleValueEvent(photoEventListener)
                dbRef.child("profiles").child(auth.currentUser?.uid!!)
                    .addListenerForSingleValueEvent(profileEventListener)
                dbRef.child("contacts").child(auth.currentUser?.uid!!)
                    .addListenerForSingleValueEvent(contactsEventListener)
            }

        })

        appBarConfiguration = AppBarConfiguration(navController.graph, drawerLayout)
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.mainActivityNavigationView.setupWithNavController(navController)

        dbRef = Firebase.database.reference
        auth = Firebase.auth

        header = binding.mainActivityNavigationView.getHeaderView(0)

        setUpUserHome()

    }

    private fun setUpUserHome() {

        val imageView = header.findViewById<ShapeableImageView>(R.id.profileImageView)
        val photoProgressBar = header.findViewById<ProgressBar>(R.id.headerPhotoProgressBar)

        photoEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val photo = snapshot.getValue<String>()

                if (photo == null) {
                    Glide.with(this@MainActivity).load(R.drawable.ic_person_light_pearl)
                        .into(imageView)
                    photoProgressBar?.visibility = View.GONE
                } else {
                    val options = RequestOptions()
                        .error(R.drawable.ic_downloading)
                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)

                    imageView.visibility = View.VISIBLE
                    GlideImageLoader(imageView, photoProgressBar).load(photo, options);
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }

        header.findViewById<TextView>(R.id.profileEmailTextView).text = auth.currentUser?.email

        profileEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val profile = snapshot.getValue<Profile>()
                if (profile != null) {
                    header.findViewById<TextView>(R.id.profileNameTextView).text = profile.name
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }

        contactsEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val contact = snapshot.getValue<Contact>()
            }

            override fun onCancelled(error: DatabaseError) {}


        }
    }

    override fun onSupportNavigateUp() = navController.navigateUp(appBarConfiguration) ||
            super.onSupportNavigateUp()

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
            super.onBackPressed()
        }
    }

    fun logOut(item: MenuItem) {
        AlertDialog.Builder(this)
            .setMessage("Confirm logout?")
            .setPositiveButton("Yes") {
                    dialog, which -> AuthUI.getInstance().signOut(this).addOnSuccessListener {
                val intent = Intent(this, SignInActivity::class.java)
                startActivity(intent)
                finish()
            }

            }.setNegativeButton("No") {
                    dialog, which -> dialog.dismiss()
            }.show()
    }


}

