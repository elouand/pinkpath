package com.traveling.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Feed : Screen("feed")
    object Map : Screen("map")
    object Path : Screen("path")
    object Profile : Screen("profile")
    object PostDetail : Screen("post_detail/{postId}") {
        fun createRoute(postId: String) = "post_detail/$postId"
    }
    object CreatePost : Screen("create_post")
    object Login : Screen("login")
    object Signup : Screen("signup")
    object CreateGroup : Screen("create_group")
    object CreatePath : Screen("create_path")
}
