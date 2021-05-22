package com.kappstudio.maskmap.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kappstudio.maskmap.data.Feature
import com.kappstudio.maskmap.databinding.ItemViewBinding

class MainAdapter(private val itemClickListener: IItemClickListener) :
    RecyclerView.Adapter<MainAdapter.MyViewHolder>() {

    var pharmacyList: List<Feature> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    class MyViewHolder(val itemViewBinding: ItemViewBinding) :
        RecyclerView.ViewHolder(itemViewBinding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemViewBinding =
            ItemViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(itemViewBinding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.itemViewBinding.tvName.text = pharmacyList[position].property.name
        holder.itemViewBinding.tvAdultQuantity.text =
            pharmacyList[position].property.mask_adult.toString()
        holder.itemViewBinding.tvChildQuantity.text =
            pharmacyList[position].property.mask_child.toString()
        holder.itemViewBinding.tvAddress.text =
            pharmacyList[position].property.address

        holder.itemViewBinding.clItem.setOnClickListener {
            itemClickListener.onItemClickListener(pharmacyList[position])
        }
    }

    override fun getItemCount(): Int {
        return pharmacyList.size
    }

    interface IItemClickListener {
        fun onItemClickListener(data: Feature)
    }

}