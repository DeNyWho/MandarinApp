package com.example.mandarin.view.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.mandarin.R
import com.example.mandarin.databinding.FragmentMandarinBinding
import com.example.mandarin.view.dialog.AddGroupBottomSheetDialogFragment
import com.example.mandarin.view.dialog.NewMessageBottomSheetDialogFragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayoutMediator


class MandarinFragment:
    Fragment(),
    AddGroupBottomSheetDialogFragment.AddGroupFabListener{

    private var newMessageBottomSheetDialog: NewMessageBottomSheetDialogFragment? = null
    private var _binding: FragmentMandarinBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewPager: ViewPager2
    private lateinit var viewPagerAdapter: FragmentStateAdapter
    lateinit var homeFab: FloatingActionButton
    private lateinit var addGroupBottomSheetDialog: AddGroupBottomSheetDialogFragment

    override fun onStart() {
        super.onStart()
        (activity as AppCompatActivity).supportActionBar?.show()
        (activity as AppCompatActivity).supportActionBar?.title = "Mandarin"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMandarinBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        homeFab = binding.homeFab

        viewPager = binding.homeFragmentViewPager
        viewPagerAdapter = object : FragmentStateAdapter(childFragmentManager, lifecycle) {
            private val fragments = arrayOf(
                    ChatsFragment(),
                    GroupsFragment(),
            )

            override fun createFragment(position: Int) = fragments[position]

            override fun getItemCount(): Int = fragments.size
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if(homeFab.visibility == GONE) homeFab.visibility = VISIBLE
                when (position) {
                    0 -> homeFab.setImageResource(R.drawable.ic_issues)
                    1 -> homeFab.setImageResource(R.drawable.ic_post)
                    else -> homeFab.setImageResource(R.drawable.ic_add_group)
                }
            }
        })

        with(binding) {
            viewPager.adapter = viewPagerAdapter

            TabLayoutMediator(homeFragmentTabLayout, viewPager) { tab, position ->
                tab.text = when(position) {
                    0 -> "Messages"
                    1 -> "Groups"
                    else -> getString(R.string.groups_tab_name)
                }
            }.attach()
        }

        binding.homeFab.setOnClickListener {
            when (viewPager.currentItem) {
                0 -> {
                    newMessageBottomSheetDialog = NewMessageBottomSheetDialogFragment(requireContext())
                    newMessageBottomSheetDialog?.show(childFragmentManager, null)

                }
                1 -> {
                    addGroupBottomSheetDialog = AddGroupBottomSheetDialogFragment(
                        requireContext(),
                        requireView(),
                        this)
                    addGroupBottomSheetDialog.show(parentFragmentManager, null)

                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun enableFab(enabled: Boolean) {
        binding.homeFab.isEnabled = enabled
    }


}