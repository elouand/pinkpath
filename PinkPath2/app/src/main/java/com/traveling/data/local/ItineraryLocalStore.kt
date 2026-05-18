package com.traveling.data.local

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.traveling.domain.model.ItineraryFull
import com.traveling.domain.model.ItineraryVariant
import com.traveling.domain.model.SavedItinerary
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItineraryLocalStore @Inject constructor(@ApplicationContext context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("itinerary_cache", Context.MODE_PRIVATE)
    private val gson = Gson()

    // ── Liste des itinéraires sauvegardés ──────────────────────────────
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

    // ── Détail complet d'un itinéraire (étapes) ────────────────────────
    fun saveCachedItineraryFull(full: ItineraryFull) {
        prefs.edit().putString("itinerary_full_${full.id}", gson.toJson(full)).apply()
    }

    fun getCachedItineraryFull(id: Int): ItineraryFull? {
        val json = prefs.getString("itinerary_full_$id", null) ?: return null
        return try { gson.fromJson(json, ItineraryFull::class.java) } catch (_: Exception) { null }
    }

    // ── Dernières variantes générées (pour navigation hors-ligne) ──────
    fun saveCachedVariants(variants: List<ItineraryVariant>) {
        prefs.edit().putString("cached_variants", gson.toJson(variants)).apply()
    }

    fun getCachedVariants(): List<ItineraryVariant> {
        val json = prefs.getString("cached_variants", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<ItineraryVariant>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (_: Exception) { emptyList() }
    }

    // ── Likes locaux ───────────────────────────────────────────────────
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
