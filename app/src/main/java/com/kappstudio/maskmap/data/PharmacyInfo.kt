package com.kappstudio.maskmap.data

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class PharmacyInfo(
    val features: List<Feature>,
    @SerializedName("type")
    val type: String      //可自定義名稱
):Serializable

data class Feature(
    val geometry: Geometry,
    @SerializedName("properties")
    val property: Property,
    val type: String
):Serializable

data class Geometry(
    val coordinates: List<Double>,
    val type: String
):Serializable

data class Property(
    val address: String,
    val available: String,
    val county: String,
    val cunli: String,
    val custom_note: String,
    val id: String,
    val mask_adult: Int,
    val mask_child: Int,
    val name: String,
    val note: String,
    val phone: String,
    val service_periods: String,
    val town: String,
    val updated: String,
    val website: String
):Serializable
