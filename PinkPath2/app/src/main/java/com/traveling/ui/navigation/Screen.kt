package com.traveling.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Feed : Screen("feed") {
        fun createRoute(groupName: String? = null, search: String? = null) = 
            if (groupName != null) "feed?groupName=$groupName" 
            else if (search != null) "feed?search=$search"
            else "feed"
    }
    object Map : Screen("map")
    object Path : Screen("path")
    object Profile : Screen("profile")
    object PostDetail : Screen("post_detail/{postId}") {
        fun createRoute(postId: String) = "post_detail/$postId"
    }
    object CreatePost : Screen("create_post") {
        fun createRoute(location: String? = null, lat: Double? = null, lon: Double? = null) = 
            if (location != null) "create_post?location=$location&lat=$lat&lon=$lon" else "create_post"
    }
    object Login : Screen("login")
    object Signup : Screen("signup")
    object CreateGroup : Screen("create_group")
    object CreatePath : Screen("create_path")
    object GroupDetail : Screen("group_detail/{groupId}") {
        fun createRoute(groupId: Int) = "group_detail/$groupId"
    }
    object PlaceDetail : Screen("place_detail/{placeId}") {
        fun createRoute(placeId: String) = "place_detail/$placeId"
    }
}
