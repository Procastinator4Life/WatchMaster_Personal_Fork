package com.pranshulgg.watchmaster.feature.person

import com.google.gson.annotations.SerializedName

data class PersonEntity(
    val name: String,
    val biography: String? = null,
    val birthday: String? = null,
    val gender: Int = 0,

    @SerializedName("profile_path")
    val profilePath: String? = null,

    @SerializedName("place_of_birth")
    val placeOfBirth: String? = null,

    @SerializedName("known_for_department")
    val knownForDepartment: String? = null,

    val deathday: String? = null
)