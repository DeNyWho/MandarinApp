package com.example.mandarin.view.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.mandarin.R
import com.example.mandarin.databinding.FragmentContactsBinding
import com.example.mandarin.databinding.FragmentMandarinBinding
import com.example.mandarin.view.dialog.AddGroupBottomSheetDialogFragment
import com.example.mandarin.view.dialog.NewMessageBottomSheetDialogFragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class Contacts:
    Fragment(){

    private var newMessageBottomSheetDialog: NewMessageBottomSheetDialogFragment? = null
    private var _binding: FragmentContactsBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewPager: ViewPager2
    private lateinit var viewPagerAdapter: FragmentStateAdapter

    override fun onStart() {
        super.onStart()
        (activity as AppCompatActivity).supportActionBar?.title = "Contacts"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentContactsBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
            viewPager = binding.homeFragmentViewPager
            viewPagerAdapter = object : FragmentStateAdapter(childFragmentManager, lifecycle) {

                private val fragments = arrayOf(
                    PeopleFragment(),
                    FriendsFragment(),
                )

                override fun createFragment(position: Int) = fragments[position]

                override fun getItemCount(): Int = fragments.size
            }

            viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            })

            with(binding) {
                viewPager.adapter = viewPagerAdapter

                TabLayoutMediator(homeFragmentTabLayout, viewPager) { tab, position ->
                    tab.text = when (position) {
                        0 -> "People"
                        1 -> "Friends"
                        else -> getString(R.string.groups_tab_name)
                    }
                }.attach()
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

}