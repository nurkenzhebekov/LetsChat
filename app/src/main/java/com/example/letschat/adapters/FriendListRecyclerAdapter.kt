package com.example.letschat.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.example.letschat.databinding.ItemFriendBinding
import com.example.letschat.models.UserModel

class FriendListRecyclerAdapter : RecyclerView.Adapter<FriendListRecyclerAdapter.CustomHolder>() {

    private val friendList = mutableListOf<UserModel>()

    var onItemClickListener: ((String) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemFriendBinding.inflate(inflater, parent, false)
        return CustomHolder(binding)
    }

    override fun getItemCount(): Int = friendList.size

    override fun onBindViewHolder(holder: CustomHolder, position: Int) {
        holder.bind(friendList[position])
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setData(list: List<UserModel>) {
        friendList.clear()
        friendList.addAll(list)
        notifyDataSetChanged()
    }

    inner class CustomHolder(private val binding: ItemFriendBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(user: UserModel) {

            binding.apply {
                if (user.userImageUri != "") {
                    itemFriendImageview.load(user.userImageUri) {
                        transformations(CircleCropTransformation())
                    }
                }

                itemFriendName.text = user.userName

                root.setOnClickListener {
                    onItemClickListener?.invoke(user.userUid)
                }
            }

        }
    }
}