package com.pranshulgg.watchmaster.data.local.mapper

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.pranshulgg.watchmaster.data.TvCreditsDto
import com.pranshulgg.watchmaster.data.TvGenre
import com.pranshulgg.watchmaster.data.TvReviewsDto
import com.pranshulgg.watchmaster.data.TvWatchProvidersDto
import com.pranshulgg.watchmaster.data.local.entity.TvBundle
import com.pranshulgg.watchmaster.data.local.entity.TvBundleEntity


private val gson = Gson()
fun TvBundle.toEntity(): TvBundleEntity =
    TvBundleEntity(
        id = id,
        name = name,
        overview = overview,
        runtime = runtime,
        poster_path = poster_path,
        season_number = season_number,
        backdrop_path = backdrop_path,
        backdropOverride = backdropOverride,
        genresJson = gson.toJson(genres),
        creditsJson = gson.toJson(credits),
        watchProvidersJson = watchProviders?.let { gson.toJson(it) },
        reviewsJson = gson.toJson(reviews),
        cachedAt = System.currentTimeMillis()
    )

fun TvBundleEntity.toDomain(): TvBundle =
    TvBundle(
        id = id,
        name = name,
        overview = overview,
        runtime = runtime,
        poster_path = poster_path,
        backdrop_path = backdrop_path,
        backdropOverride = backdropOverride,
        season_number = season_number,
        genres = gson.fromJson(
            genresJson,
            object : TypeToken<List<TvGenre>>() {}.type
        ),
        credits = gson.fromJson(creditsJson, TvCreditsDto::class.java),
        watchProviders = watchProvidersJson?.let {
            gson.fromJson(it, TvWatchProvidersDto::class.java)
        },
        reviews = gson.fromJson(reviewsJson, TvReviewsDto::class.java),
        cachedAt = cachedAt
    )