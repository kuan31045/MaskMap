package com.kappstudio.maskmap

import android.Manifest
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.kappstudio.maskmap.data.Feature
import com.kappstudio.maskmap.databinding.ActivityMapBinding
import com.permissionx.guolindev.PermissionX
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class MapActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener,
    GoogleMap.OnMapClickListener {

    private lateinit var binding: ActivityMapBinding

    private val TAG = "MapActivity"

    private var locationPermissionOk = false


    private lateinit var mContext: Context

    private lateinit var selectData: Feature

    private lateinit var mMap: GoogleMap
    private lateinit var mLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationCallBack: LocationCallback
    var locationIsUpdate = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)


        mContext = this
        mLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)


        binding.btnGetLocation.setOnClickListener {
            getPermission()
        }

        binding.clInfo.setOnClickListener {
            moveToPharmacy()
        }

    }


    private fun getPermission() {
        PermissionX.init(this)
            .permissions(Manifest.permission.ACCESS_FINE_LOCATION)
            .onExplainRequestReason { scope, deniedList ->
                scope.showRequestReasonDialog(deniedList, "?????????????????????????????????????????????", "OK", "Cancel")
            }
            .onForwardToSettings { scope, deniedList ->
                scope.showForwardToSettingsDialog(
                    deniedList,
                    "??????????????????????????????????????????????????????????????????????????????",
                    "OK",
                    "Cancel"
                )
            }
            .request { allGranted, grantedList, deniedList ->
                if (allGranted) {
                    locationPermissionOk = true
                    makeToast("????????????????????????...")
                    //gps
                    checkGPS()
                }
            }
    }

    private fun checkGPS() {

        val locationManager =
            mContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            AlertDialog.Builder(mContext)
                .setTitle("GPS????????????")
                .setMessage("??????????????????????????????GPS")
                .setPositiveButton("????????????") { _, _ ->
                    startActivityForResult(
                        Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), REQUEST_ENABLE_GPS
                    )
                }
                .setNegativeButton("??????", null)
                .show()
        } else {
            getNowLocation()
        }
    }

    private fun getNowLocation() {
        try {
            if (locationPermissionOk) {
                makeToast("????????????????????????...")

                binding.btnGetLocation.visibility = View.GONE

                var nowLocationCount = 0  //????????????

                // ????????????
                mMap?.isMyLocationEnabled = true

                val locationRequest = LocationRequest()
                locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                //????????????
                locationRequest.interval = 1000 //1???

                mLocationProviderClient.requestLocationUpdates(
                    locationRequest,

                    object : LocationCallback() {
                        override fun onLocationResult(locationResult: LocationResult?) {

                            locationCallBack = this
                            locationIsUpdate = true

                            locationResult ?: return
                            nowLocationCount++
                            val nowLocation = LatLng(
                                locationResult.lastLocation.latitude, //??????
                                locationResult.lastLocation.longitude //??????
                            )
                            Log.d(
                                TAG,
                                "???${nowLocationCount}?????????: $nowLocation"
                            )

                            //???????????????????????????
                            if (nowLocationCount == 1) {
                                mMap?.moveCamera(
                                    CameraUpdateFactory.newLatLngZoom(
                                        nowLocation, 15f
                                    )
                                )


                            }
                        }
                    },
                    null
                )

            } else {
                getPermission()
            }
        } catch (e: SecurityException) {
            makeToast("Exception:$e")
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_ENABLE_GPS -> {
                checkGPS()
            }
        }
    }

    fun makeToast(mText: String) {
        Toast.makeText(this, mText, Toast.LENGTH_SHORT).show()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        //???????????????????????????
        mMap = googleMap
        addPharmacyMark()

        //???????????????
        selectData = intent.getSerializableExtra("data") as Feature

        val defaultLocation = LatLng(
            selectData.geometry.coordinates[1], //??????
            selectData.geometry.coordinates[0]  //??????
        )
        mMap?.addMarker(
            MarkerOptions()
                .position(defaultLocation)
                .title(selectData.property.name)
                .snippet("??????:${selectData.property.mask_adult}, ??????:${selectData.property.mask_child}")
        ).showInfoWindow()

        mMap?.setOnMarkerClickListener(this)
        mMap?.setOnMapClickListener(this)

        setInfoLayout()
        moveToPharmacy()

    }


    private fun addPharmacyMark() {
        val stockSwitchOn = intent.getSerializableExtra("stockSwitchOn") as Boolean

        GlobalScope.launch {
            if (pharmacyInfo != null) {
                pharmacyInfo?.features?.forEach {
                    if (!stockSwitchOn || it.property.mask_adult > 0) {
                        val pharmacyLocation = LatLng(
                            it.geometry.coordinates[1], //??????
                            it.geometry.coordinates[0]  //??????
                        )

                        runOnUiThread {
                            mMap?.addMarker(
                                MarkerOptions()
                                    .position(pharmacyLocation)
                                    .title(it.property.name)
                                    .snippet("??????:${it.property.mask_adult}, ??????:${it.property.mask_child}")
                            )
                        }
                    }

                }

            }
        }

    }


    private fun moveToPharmacy() {
        mMap?.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(
                    selectData.geometry.coordinates[1], //??????
                    selectData.geometry.coordinates[0]  //??????
                ), 15f
            )
        )
    }


    override fun onMarkerClick(marker: Marker): Boolean {
        marker?.title?.let { title ->
            val filterData = pharmacyInfo?.features?.filter {
                it.property.name == (title) &&
                        it.geometry.coordinates[1] == marker?.position.latitude
                        &&
                        it.geometry.coordinates[0] == marker?.position.longitude
            }
            if (filterData?.size!! > 0) {
                selectData = filterData.first()
                setInfoLayout()
            } else {
                Log.d(TAG, "????????????")
            }

        }
        return false

    }

    private fun setInfoLayout() {
        binding.tvName.text = selectData.property.name
        binding.tvAdultQuantity.text = selectData.property.mask_adult.toString()
        binding.tvChildQuantity.text = selectData.property.mask_child.toString()
        binding.tvPhone.text = selectData.property.phone
        binding.tvAddress.text = selectData.property.address
        binding.tvUpdateTime.text = "??????????????????: ${selectData.property.updated}"

        binding.glInfo.setGuidelinePercent(0.62f)
        binding.svInfo.visibility = View.VISIBLE
    }

    override fun onMapClick(p0: LatLng) {
        Log.d(TAG, "Click mMap????????????")
        binding.svInfo.visibility = View.GONE
        binding.glInfo.setGuidelinePercent(1.0f)

    }

    override fun onBackPressed() {
        if (binding.svInfo.visibility == View.VISIBLE) {
            binding.svInfo.visibility = View.GONE
            binding.glInfo.setGuidelinePercent(1.0f)
        } else {
            finish()
        }
    }


}