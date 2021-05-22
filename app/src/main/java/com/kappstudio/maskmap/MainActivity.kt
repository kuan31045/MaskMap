package com.kappstudio.maskmap

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.kappstudio.maskmap.util.CountyUtil
import com.kappstudio.maskmap.util.OkHttpUtil
import com.kappstudio.maskmap.util.OkHttpUtil.Companion.mOkHttpUtil
import com.kappstudio.maskmap.adapter.MainAdapter
import com.kappstudio.maskmap.data.Feature
import com.kappstudio.maskmap.data.PharmacyInfo
import com.kappstudio.maskmap.databinding.ActivityMainBinding
import okhttp3.*

var pharmacyInfo: PharmacyInfo? = null

class MainActivity : AppCompatActivity(), MainAdapter.IItemClickListener {

    lateinit var sharedPreferences: SharedPreferences
    private var firstSet = true

    private val TAG = "MainActivity"

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewManager: RecyclerView.LayoutManager
    private lateinit var viewAdapter: MainAdapter

    private var selectCounty: String = ""
    private var selectTown: String = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        pharmacyInfo = null

        initView()

        getPharmaciesData()

        binding.switchStock.setOnClickListener {
            updateRecyclerView()
        }

    }

    private fun initView() {


        //Spinner
        val adapterCounty = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            CountyUtil.getAllCountiesName()
        )
        binding.spnCounty.adapter = adapterCounty
        binding.spnCounty.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                selectCounty = binding.spnCounty.selectedItem.toString()
                setSpinnerTown(selectCounty)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        binding.spnTown.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                selectTown = binding.spnTown.selectedItem.toString()
                if (pharmacyInfo != null) {
                    updateRecyclerView()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        sharedPreferences = getSharedPreferences("myPreferences", Context.MODE_PRIVATE)
        if (sharedPreferences.contains("SELECT_COUNTY")) {
            binding.spnCounty.setSelection(sharedPreferences.getInt("SELECT_COUNTY", 0))
        }

        //RecyclerView
        viewManager = LinearLayoutManager(this)
        viewAdapter = MainAdapter(this)
        binding.recyclerView.apply {
            layoutManager = viewManager
            adapter = viewAdapter
        }
    }

    private fun updateRecyclerView() {
        var filterData: List<Feature>? = if (binding.switchStock.isChecked) {
            pharmacyInfo?.features?.filter {
                it.property.county == selectCounty
                        && it.property.town == selectTown
                        && it.property.mask_adult > 0       //僅顯示有貨
            }
        } else {
            pharmacyInfo?.features?.filter {
                it.property.county == selectCounty
                        && it.property.town == selectTown
            }
        }

        if (filterData != null) {
            viewAdapter.pharmacyList = filterData
        }
        if (filterData?.toString()  == "[]") {
            binding.tvNoData.visibility = View.VISIBLE
        }else{
            binding.tvNoData.visibility = View.GONE
        }
    }

    private fun setSpinnerTown(selectCounty: String) {
        val adapterTown = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            CountyUtil.getTownsByCountyName(selectCounty)
        )
        binding.spnTown.adapter = adapterTown

        if (firstSet) {
            if (sharedPreferences.contains("SELECT_TOWN")) {
                Log.d(TAG, "取得位置${sharedPreferences.getInt("SELECT_TOWN", 0)}")
                binding.spnTown.setSelection(sharedPreferences.getInt("SELECT_TOWN", 0))
                firstSet = false
            }
        }

    }

    private fun getPharmaciesData() {
        binding.pbLoad.visibility = View.VISIBLE

        mOkHttpUtil.getAsync(PHARMACIES_DATA_URL, object : OkHttpUtil.ICallback {
            override fun onResponse(response: Response) {
                val pharmaciesData = response.body?.string()  //response.body只能取出一次

                pharmacyInfo = Gson().fromJson(pharmaciesData, PharmacyInfo::class.java)

                runOnUiThread {
                    updateRecyclerView()

                    binding.pbLoad.visibility = View.GONE
                }
            }

            override fun onFailure(e: okio.IOException) {
                Log.d(TAG, "onFailure $e")
                runOnUiThread {
                    binding.pbLoad.visibility = View.GONE
                }
            }
        })

    }

    override fun onItemClickListener(data: Feature) {
        Log.d(TAG, data.property.id)

        val intent = Intent(this, MapActivity::class.java)
        intent.putExtra("data", data)

        if(binding.switchStock.isChecked){
            intent.putExtra("stockSwitchOn", true)
        }else{
            intent.putExtra("stockSwitchOn", false)
        }

        startActivity(intent)
    }

    //儲存選定的Spinner
    override fun onStop() {

        sharedPreferences = getSharedPreferences("myPreferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt("SELECT_COUNTY", binding.spnCounty.selectedItemPosition)
        editor.putInt("SELECT_TOWN", binding.spnTown.selectedItemPosition)
        Log.d(TAG, "選取位置${binding.spnTown.selectedItemPosition}")
        editor.commit()

        super.onStop()
    }

}
