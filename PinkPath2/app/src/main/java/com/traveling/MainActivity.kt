package com.traveling

import android.os.Bundle
import android.preference.PreferenceManager
import java.io.File
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.traveling.ui.navigation.Screen
import com.traveling.ui.theme.TravelingDeepPurple
import com.traveling.ui.theme.TravelingTheme
import com.traveling.ui.travelpath.CreateGroupScreen
import com.traveling.ui.travelpath.CreatePathScreen
import com.traveling.ui.travelpath.EditItineraryScreen
import com.traveling.ui.travelpath.GroupDetailScreen
import com.traveling.ui.travelpath.ItineraryViewModel
import com.traveling.ui.travelpath.MapScreen
import com.traveling.ui.travelpath.MapViewModel
import com.traveling.ui.travelpath.PlaceDetailScreen
import com.traveling.ui.travelpath.TravelPathScreen
import com.traveling.ui.travelshare.AuthViewModel
import com.traveling.ui.travelshare.CreatePostScreen
import com.traveling.ui.travelshare.FeedScreen
import com.traveling.ui.travelshare.HomeScreen
import com.traveling.ui.travelshare.LoginScreen
import com.traveling.ui.travelshare.PostDetailScreen
import com.traveling.ui.travelshare.PostViewModel
import com.traveling.ui.travelshare.ProfileScreen
import com.traveling.ui.travelshare.SignupScreen
import dagger.hilt.android.AndroidEntryPoint
import org.osmdroid.config.Configuration

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Configuration OSMDroid
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        Configuration.getInstance().userAgentValue = packageName
        Configuration.getInstance().osmdroidTileCache = File(cacheDir, "osmdroid")

        enableEdgeToEdge()
        setContent {
            TravelingTheme {
                MainNavigation()
            }
        }
    }
}

@Composable
fun MainNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    val authViewModel: AuthViewModel = hiltViewModel()
    val postViewModel: PostViewModel = hiltViewModel()
    val itineraryViewModel: ItineraryViewModel = hiltViewModel()
    val mapViewModel: MapViewModel = hiltViewModel()

    Scaffold(
        bottomBar = {
            val hideBottomBar = currentRoute?.startsWith("post_detail") == true ||
                                currentRoute?.startsWith("place_detail") == true ||
                                currentRoute?.startsWith("group_detail") == true ||
                                currentRoute?.startsWith("create_post") == true ||
                                currentRoute == Screen.Login.route ||
                                currentRoute == Screen.Signup.route ||
                                currentRoute == Screen.CreateGroup.route ||
                                currentRoute == Screen.CreatePath.route ||
                                currentRoute == Screen.EditItinerary.route
            
            if (!hideBottomBar) {
                NavigationBar(containerColor = TravelingDeepPurple, contentColor = Color.White) {
                    NavigationBarItem(
                        selected = currentRoute == Screen.Home.route,
                        onClick = { navController.navigate(Screen.Home.route) { popUpTo(navController.graph.startDestinationId); launchSingleTop = true } },
                        icon = { Icon(Icons.Default.Home, null, tint = Color.White) }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Screen.Feed.route,
                        onClick = { navController.navigate(Screen.Feed.route) { popUpTo(navController.graph.startDestinationId); launchSingleTop = true } },
                        icon = { Icon(Icons.Default.Language, null, tint = Color.White) }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Screen.Map.route,
                        onClick = { navController.navigate(Screen.Map.route) { popUpTo(navController.graph.startDestinationId); launchSingleTop = true } },
                        icon = { Icon(Icons.Default.Map, null, tint = Color.White) }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Screen.Path.route,
                        onClick = { navController.navigate(Screen.Path.route) { popUpTo(navController.graph.startDestinationId); launchSingleTop = true } },
                        icon = { Icon(Icons.AutoMirrored.Filled.DirectionsWalk, null, tint = Color.White) }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Screen.Profile.route,
                        onClick = { navController.navigate(Screen.Profile.route) { popUpTo(navController.graph.startDestinationId); launchSingleTop = true } },
                        icon = { Icon(Icons.Default.AccountCircle, null, tint = Color.White) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController = navController, startDestination = Screen.Home.route, modifier = Modifier.padding(innerPadding)) {
            composable(Screen.Home.route) {
                HomeScreen(viewModel = postViewModel, mapViewModel = mapViewModel, onPostClick = { navController.navigate(Screen.PostDetail.createRoute(it)) }, onNavigateToMap = { navController.navigate(Screen.Map.route) { popUpTo(navController.graph.startDestinationId); launchSingleTop = true } })
            }
            composable(Screen.Feed.route) {
                FeedScreen(viewModel = postViewModel, authViewModel = authViewModel, onPostClick = { navController.navigate(Screen.PostDetail.createRoute(it)) }, onCreatePostClick = { navController.navigate(Screen.CreatePost.route) })
            }
            composable(Screen.Map.route) {
                MapScreen(viewModel = mapViewModel, itineraryViewModel = itineraryViewModel, onPlaceClick = { navController.navigate(Screen.PlaceDetail.createRoute(it)) })
            }
            composable(Screen.PlaceDetail.route, arguments = listOf(navArgument("placeId") { type = NavType.StringType })) { backStackEntry ->
                PlaceDetailScreen(
                    placeId = backStackEntry.arguments?.getString("placeId") ?: "",
                    mapViewModel = mapViewModel,
                    onBack = { navController.popBackStack() },
                    onCreatePostClick = { loc, lat, lon -> navController.navigate(Screen.CreatePost.createRoute(loc, lat, lon)) },
                    onPostClick = { navController.navigate(Screen.PostDetail.createRoute(it)) }
                )
            }
            composable(Screen.PostDetail.route, arguments = listOf(navArgument("postId") { type = NavType.StringType })) { backStackEntry ->
                PostDetailScreen(postId = backStackEntry.arguments?.getString("postId") ?: "", onBack = { navController.popBackStack() }, viewModel = postViewModel, authViewModel = authViewModel, itineraryViewModel = itineraryViewModel)
            }
            composable(
                route = Screen.CreatePost.route,
                arguments = listOf(
                    navArgument("location") { type = NavType.StringType; nullable = true },
                    navArgument("lat") { type = NavType.StringType; nullable = true }, // NavArguments are strings by default in URL
                    navArgument("lon") { type = NavType.StringType; nullable = true }
                )
            ) { backStackEntry ->
                CreatePostScreen(
                    onBack = { navController.popBackStack() },
                    initialLocation = backStackEntry.arguments?.getString("location"),
                    initialLat = backStackEntry.arguments?.getString("lat")?.toDoubleOrNull(),
                    initialLon = backStackEntry.arguments?.getString("lon")?.toDoubleOrNull(),
                    viewModel = postViewModel,
                    authViewModel = authViewModel
                )
            }
            composable(Screen.Profile.route) {
                ProfileScreen(viewModel = authViewModel, onLoginClick = { navController.navigate(Screen.Login.route) }, onSignupClick = { navController.navigate(Screen.Signup.route) })
            }
            composable(Screen.Login.route) {
                LoginScreen(viewModel = authViewModel, onLoginSuccess = { navController.navigate(Screen.Profile.route) { popUpTo(Screen.Login.route) { inclusive = true } } })
            }
            composable(Screen.Signup.route) {
                SignupScreen(viewModel = authViewModel, onSignupSuccess = { navController.navigate(Screen.Profile.route) { popUpTo(Screen.Signup.route) { inclusive = true } } })
            }
            composable(Screen.Path.route) {
                TravelPathScreen(
                    onCreatePathClick = { navController.navigate(Screen.CreatePath.route) },
                    onCreateGroupClick = { navController.navigate(Screen.CreateGroup.route) },
                    onGroupClick = { navController.navigate(Screen.GroupDetail.createRoute(it)) },
                    onNavigateToMap = { navController.navigate(Screen.Map.route) { popUpTo(navController.graph.startDestinationId); launchSingleTop = true } },
                    onNavigateToEditItinerary = { navController.navigate(Screen.EditItinerary.route) },
                    itineraryViewModel = itineraryViewModel
                )
            }
            composable(Screen.EditItinerary.route) {
                EditItineraryScreen(onBack = { navController.popBackStack() }, viewModel = itineraryViewModel, postViewModel = postViewModel, authViewModel = authViewModel)
            }
            composable(Screen.CreateGroup.route) { CreateGroupScreen(onBack = { navController.popBackStack() }) }
            composable(Screen.CreatePath.route) { CreatePathScreen(onBack = { navController.popBackStack() }, viewModel = itineraryViewModel) }
            composable(Screen.GroupDetail.route, arguments = listOf(navArgument("groupId") { type = NavType.IntType })) { backStackEntry ->
                GroupDetailScreen(groupId = backStackEntry.arguments?.getInt("groupId") ?: 0, onBack = { navController.popBackStack() }, onNavigateToGroupFeed = { navController.navigate(Screen.Feed.route + "?groupName=$it") }, onPostClick = { navController.navigate(Screen.PostDetail.createRoute(it)) })
            }
        }
    }
}
