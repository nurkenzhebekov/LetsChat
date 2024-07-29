package com.example.letschat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.example.letschat.databinding.FragmentLoginBinding
import com.example.letschat.utils.USER_EMAIL
import com.example.letschat.utils.USER_PWD
import com.example.letschat.utils.getString
import com.example.letschat.utils.putString
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoginFragment : BaseFragment<FragmentLoginBinding>() {

    override fun getFragmentBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentLoginBinding {
        return FragmentLoginBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.etxEmail.setText(getString(requireContext(), USER_EMAIL))
        binding.etxPassword.setText(getString(requireContext(), USER_PWD))

        binding.btnLogin.setOnClickListener {
            performLogin()
        }

        binding.txtRegister.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }
    }

    private fun performLogin() {
        val email = binding.etxEmail.text.toString()
        val password = binding.etxPassword.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(),
                "Please input all the fields!", Toast.LENGTH_SHORT).show()
            return
        }

        Firebase.auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                if (!it.isSuccessful) return@addOnCompleteListener
                putString(requireContext(), USER_EMAIL, email)
                putString(requireContext(), USER_PWD, password)

                Toast.makeText(requireContext(),
                    "Successfully logged in!", Toast.LENGTH_SHORT).show()

                findNavController().navigate(R.id.action_loginFragment_to_chatRoomFragment)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(),
                    "Failed to log in : ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onBackPressed() {
        //
    }
}