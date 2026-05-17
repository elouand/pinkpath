package com.traveling.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Feed : Screen("feed")
    object Map : Screen("map")
    object Path : Screen("path")
    object Profile : Screen("profile")
    object Login : Screen("login")
    object Signup : Screen("signup")
    
    object CreatePost : Screen("create_post?location={location}&lat={lat}&lon={lon}") {
        fun createRoute(location: String? = null, lat: Double? = null, lon: Double? = null) = 
            "create_post".let { base ->
                val params = mutableListOf<String>()
                location?.let { params.add("location=$it") }
                lat?.let { params.add("lat=$it") }
                lon?.let { params.add("lon=$it") }
                if (params.isEmpty()) base else "$base?${params.joinToString("&")}"
            }
    }

    object CreateGroup : Screen("create_group")
    object CreatePath : Screen("create_path")
    object EditItinerary : Screen("edit_itinerary")

    object PostDetail : Screen("post_detail/{postId}") {
        fun createRoute(postId: String) = "post_detail/$postId"
    }

    object PlaceDetail : Screen("place_detail/{placeId}") {
        fun createRoute(placeId: String) = "place_detail/$placeId"
    }
    
    object GroupDetail : Screen("group_detail/{groupId}") {
        fun createRoute(groupId: Int) = "group_detail/$groupId"
    }

    object UserProfile : Screen("user_profile/{userId}") {
        fun createRoute(userId: Int) = "user_profile/$userId"
    }

    object Notifications : Screen("notifications")
}
