package com.example.letschat.fragments

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.example.letschat.R
import com.example.letschat.databinding.FragmentPermissionBinding
import com.google.android.material.snackbar.Snackbar

class PermissionFragment : BaseFragment<FragmentPermissionBinding>() {

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private val permissions = mutableListOf(
        android.Manifest.permission.POST_NOTIFICATIONS
    ).apply {
        //
    }

    private val permissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions.all { it.value }) {
            onPermissionGranted()
        } else {
            view?.let { v ->
                Snackbar.make(v, "This app cannot work without permissions.", Snackbar.LENGTH_INDEFINITE)
                    .setAction("OK") {
                        ActivityCompat.finishAffinity(requireActivity())
                    }
                    .show()
            }
        }
    }

    override fun getFragmentBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentPermissionBinding {
        return FragmentPermissionBinding.inflate(inflater, container, false)
    }

    override fun onBackPressed() {
        //
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        checkAndGetPermissions()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun checkAndGetPermissions() {
        if (allPermissionsGranted())
            onPermissionGranted()
        else
            permissionRequest.launch(permissions.toTypedArray())
    }

    private fun onPermissionGranted() {
        findNavController().navigate(R.id.action_permissionFragment_to_chatRoomFragment)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun allPermissionsGranted() = permissions.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

}