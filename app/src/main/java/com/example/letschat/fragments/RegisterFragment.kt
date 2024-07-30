package com.example.letschat.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.example.letschat.R
import com.example.letschat.databinding.FragmentRegisterBinding
import com.example.letschat.models.UserModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class RegisterFragment : BaseFragment<FragmentRegisterBinding>() {

    override fun getFragmentBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentRegisterBinding {
        return FragmentRegisterBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnRegister.setOnClickListener {
            performRegister()
        }

        binding.txtLogin.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }
    }

    private fun performRegister() {

        val nickName = binding.etxNickname.text.toString()
        val email = binding.etxEmail.text.toString()
        val password = binding.etxPassword.text.toString()
        val confirmPassword = binding.etxConfirmPassword.text.toString()
        if (nickName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(requireContext(), "Please input all the fields!", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(requireContext(), "Please input password again!", Toast.LENGTH_SHORT).show()
            return
        }

        signUpMember(nickName, email, password)
    }

    private fun signUpMember(nickName: String, email: String, password: String) {

        Firebase.auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                if (!it.isSuccessful) return@addOnCompleteListener
                Toast.makeText(requireContext(), "Successfully created!", Toast.LENGTH_SHORT).show()
                val userUid = it.result.user!!.uid
                saveUserToFirebaseDB(UserModel(nickName, userUid, null, null))
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to create!", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveUserToFirebaseDB(userModel: UserModel) {

        Firebase.database.reference
            .child("users")
            .child(userModel.userUid)
            .setValue(userModel)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Successfully saved!", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_registerFragment_to_chatRoomFragment)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to save!", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onBackPressed() {
        //
    }
}