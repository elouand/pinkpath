package com.traveling.data.local

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.traveling.domain.model.SavedItinerary
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItineraryLocalStore @Inject constructor(@ApplicationContext context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("itinerary_cache", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveCachedItineraries(itineraries: List<SavedItinerary>) {
        prefs.edit().putString("cached_itineraries", gson.toJson(itineraries)).apply()
    }

    fun getCachedItineraries(): List<SavedItinerary> {
        val json = prefs.getString("cached_itineraries", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<SavedItinerary>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (_: Exception) { emptyList() }
    }

    fun getLikedIds(): Set<Int> {
        val json = prefs.getString("liked_itineraries", null) ?: return emptySet()
        return try {
            val type = object : TypeToken<Set<Int>>() {}.type
            gson.fromJson(json, type) ?: emptySet()
        } catch (_: Exception) { emptySet() }
    }

    fun toggleLike(id: Int): Boolean {
        val liked = getLikedIds().toMutableSet()
        val isNowLiked = if (liked.contains(id)) { liked.remove(id); false } else { liked.add(id); true }
        prefs.edit().putString("liked_itineraries", gson.toJson(liked)).apply()
        return isNowLiked
    }

    fun isLiked(id: Int): Boolean = getLikedIds().contains(id)
}
