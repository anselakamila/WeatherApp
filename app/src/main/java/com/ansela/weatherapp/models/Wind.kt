package com.ansela.weatherapp.models

import com.google.gson.annotations.SerializedName

data class Wind (
    @field:SerializedName("speed")
    val speed: Double,

    @field:SerializedName("deg")
    val deg: Int,

    @field:SerializedName("lon")
    val gust: Double,
)


