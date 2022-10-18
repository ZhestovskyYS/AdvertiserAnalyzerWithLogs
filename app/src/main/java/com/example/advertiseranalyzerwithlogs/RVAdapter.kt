package com.example.advertiseranalyzerwithlogs

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.advertiseranalyzerwithlogs.databinding.ItemLayoutBinding

class RVAdapter(
    private val outputList: List<String>,
) : RecyclerView.Adapter<RVAdapter.ViewHolder>() {

    override fun getItemCount() = outputList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(
            ItemLayoutBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(outputList[position])
    }

    class ViewHolder(private val binding: ItemLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(text: String) {
            binding.root.text = text
        }
    }
}