package com.traveling.ui.travelshare

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.traveling.domain.model.Place
import com.traveling.ui.common.PostCard
import com.traveling.ui.theme.TravelingDeepPurple
import com.traveling.ui.theme.TravelingTagBlue
import com.traveling.ui.travelpath.MapViewModel
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onPostClick: (String) -> Unit,
    onNavigateToMap: () -> Unit = {},
    onUserClick: (Int) -> Unit = {},
    mapViewModel: MapViewModel,
    viewModel: PostViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val posts by viewModel.posts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    val mapState by mapViewModel.uiState.collectAsState()
    val context = LocalContext.current

    val homePosts = remember(posts, mapState.userLocation) {
        val publicPosts = posts.filter { it.isPublic }
        val userLoc = mapState.userLocation
        if (userLoc != null) {
            val withCoords = publicPosts.filter { it.latitude != null && it.longitude != null }
            val nearby = withCoords
                .sortedBy { haversineKm(userLoc.latitude, userLoc.longitude, it.latitude!!, it.longitude!!) }
                .take(5)
            if (nearby.size >= 5) {
                nearby
            } else {
                val nearbyIds = nearby.map { it.id }.toSet()
                val filler = publicPosts.filter { it.id !in nearbyIds }.take(5 - nearby.size)
                nearby + filler
            }
        } else {
            publicPosts.take(5)
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }
    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { hasLocationPermission = it }
    )

    LaunchedEffect(isLoggedIn) { viewModel.loadPosts() }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            try {
                val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                @Suppress("MissingPermission")
                val loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                if (loc != null) {
                    mapViewModel.updateUserLocation(loc.latitude, loc.longitude)
                    mapViewModel.fetchNearbyPlaces(loc.latitude, loc.longitude)
                }
            } catch (_: Exception) {}
        } else {
            locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            SearchBar(
                query = searchQuery,
                onQueryChange = {
                    searchQuery = it
                    mapViewModel.onSearchQueryChanged(it)
                },
                onSearch = { isSearchActive = false },
                active = isSearchActive,
                onActiveChange = { active ->
                    isSearchActive = active
                    if (!active) {
                        searchQuery = ""
                        mapViewModel.onSearchQueryChanged("")
                    }
                },
                placeholder = { Text("Rechercher un lieu…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (isSearchActive) 0.dp else 16.dp)
                    .padding(top = if (isSearchActive) 0.dp else 8.dp)
            ) {
                if (mapState.isSearching) {
                    Box(
                        Modifier.fillMaxWidth().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = TravelingDeepPurple)
                    }
                } else if (mapState.searchResults.isEmpty() && searchQuery.isNotEmpty()) {
                    Box(
                        Modifier.fillMaxWidth().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Aucun résultat", color = Color.Gray)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(mapState.searchResults) { result ->
                            ListItem(
                                headlineContent = { Text(result.properties.name ?: "Inconnu") },
                                supportingContent = { Text(result.properties.displayName, maxLines = 1) },
                                leadingContent = {
                                    Icon(Icons.Default.LocationOn, null, tint = TravelingDeepPurple)
                                },
                                modifier = Modifier.clickable {
                                    isSearchActive = false
                                    searchQuery = ""
                                    mapViewModel.onSearchQueryChanged("")
                                    mapViewModel.setPendingCenter(
                                        result.geometry.latitude,
                                        result.geometry.longitude
                                    )
                                    onNavigateToMap()
                                }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }

            if (!isSearchActive) {
                PullToRefreshBox(
                    isRefreshing = isLoading,
                    onRefresh = { viewModel.loadPosts() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        item { Spacer(modifier = Modifier.height(8.dp)) }

                        item {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Autour de moi",
                                    style = MaterialTheme.typography.displayMedium,
                                    color = TravelingDeepPurple
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                NearbyPlacesRow(
                                    places = mapState.places,
                                    isLoading = mapState.isLoading,
                                    onPlaceClick = { place ->
                                        mapViewModel.setPendingCenter(place.latitude, place.longitude)
                                        onNavigateToMap()
                                    }
                                )
                            }
                        }

                        item {
                            val label = if (mapState.userLocation != null) "Posts près de toi" else "Posts populaires"
                            Text(
                                text = label,
                                style = MaterialTheme.typography.displayMedium,
                                color = TravelingDeepPurple,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }

                        if (homePosts.isEmpty() && !isLoading) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Aucun post disponible", color = Color.Gray)
                                }
                            }
                        } else {
                            items(items = homePosts, key = { it.id }) { post ->
                                val tags = remember(post.tags) {
                                    post.tags?.map { it to TravelingTagBlue } ?: emptyList()
                                }
                                PostCard(
                                    title = post.content ?: post.title ?: "Sans titre",
                                    tags = tags,
                                    location = post.title ?: "Inconnu",
                                    author = post.authorName ?: "Anonyme",
                                    authorProfileUrl = post.authorAvatar,
                                    imageUrl = post.fullImageUrl,
                                    likes = post.likes.toString(),
                                    comments = post.commentsCount.toString(),
                                    isLiked = post.isLiked,
                                    onLikeClick = {
                                        currentUser?.id?.toIntOrNull()?.let { userId ->
                                            viewModel.toggleLike(post.id, userId)
                                        }
                                    },
                                    onClick = { onPostClick(post.id) },
                                    sharedItinerary = post.sharedItinerary,
                                    onReportClick = { viewModel.reportPost(post.id) },
                                    onAuthorClick = post.authorId?.let { { onUserClick(it) } }
                                )
                            }
                        }

                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }
}

private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
    return r * 2 * asin(sqrt(a))
}

@Composable
fun NearbyPlacesRow(
    places: List<Place>,
    isLoading: Boolean,
    onPlaceClick: (Place) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        if (isLoading || places.isEmpty()) {
            items(5) {
                Box(
                    modifier = Modifier
                        .size(width = 140.dp, height = 100.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.LightGray.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = TravelingDeepPurple,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Place, null, tint = Color.Gray)
                    }
                }
            }
        } else {
            items(places.take(12)) { place ->
                NearbyPlaceCard(place = place, onClick = { onPlaceClick(place) })
            }
        }
    }
}

@Composable
fun NearbyPlaceCard(place: Place, onClick: () -> Unit) {
    val t = place.type.lowercase()
    val bgColor = when {
        t.contains("restaurant") || t.contains("cafe") || t.contains("bar") ||
            t.contains("bakery") || t.contains("fast_food") -> Color(0xFFE64A19)
        t.contains("park") || t.contains("garden") -> Color(0xFF388E3C)
        t.contains("museum") || t.contains("gallery") || t.contains("attraction") -> Color(0xFF1976D2)
        t.contains("hotel") -> Color(0xFF7B1FA2)
        t.contains("cinema") || t.contains("theatre") -> Color(0xFFF57C00)
        else -> TravelingDeepPurple
    }
    val icon = when {
        t.contains("restaurant") || t.contains("cafe") || t.contains("bar") ||
            t.contains("bakery") || t.contains("fast_food") -> Icons.Default.Restaurant
        t.contains("park") || t.contains("garden") -> Icons.Default.Park
        t.contains("museum") || t.contains("gallery") -> Icons.Default.Museum
        t.contains("hotel") -> Icons.Default.Hotel
        else -> Icons.Default.Place
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .size(width = 140.dp, height = 100.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(bgColor)) {
            Icon(
                icon, null,
                tint = Color.White.copy(alpha = 0.25f),
                modifier = Modifier
                    .size(56.dp)
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                Text(
                    place.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 12.sp
                )
                Text(
                    place.type.replaceFirstChar { it.uppercase() },
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 10.sp
                )
            }
        }
    }
}
