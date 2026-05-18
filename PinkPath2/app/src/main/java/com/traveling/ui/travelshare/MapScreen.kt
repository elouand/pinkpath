package com.traveling.ui.travelshare

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.traveling.data.remote.PhotonFeature
import com.traveling.domain.model.Post
import com.traveling.ui.common.TravelingSearchBar
import com.traveling.ui.theme.TravelingDeepPurple
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onNavigateToFeed: (String) -> Unit,
    onNavigateToCreatePost: (String, Double, Double) -> Unit,
    viewModel: PostViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val posts by viewModel.posts.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<PhotonFeature>>(emptyList()) }
    var selectedLocation by remember { mutableStateOf<PhotonFeature?>(null) }
    
    val sheetState = rememberModalBottomSheetState()
    var isSheetVisible by remember { mutableStateOf(false) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(48.8566, 2.3522), 10f)
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.length > 2 && selectedLocation?.properties?.name != searchQuery) {
            delay(500)
            viewModel.searchLocation(searchQuery).onSuccess { list ->
                suggestions = list.filter { !it.properties.name.isNullOrBlank() }
            }
        } else if (searchQuery.length <= 2) {
            suggestions = emptyList()
        }
    }

    LaunchedEffect(selectedLocation) {
        selectedLocation?.let {
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(
                    LatLng(it.geometry.latitude, it.geometry.longitude), 
                    15f
                )
            )
            isSheetVisible = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(mapType = MapType.NORMAL, isTrafficEnabled = false),
            uiSettings = MapUiSettings(zoomControlsEnabled = true, compassEnabled = true),
            onMapClick = {
                if (isSheetVisible) {
                    scope.launch {
                        sheetState.hide()
                        isSheetVisible = false
                        selectedLocation = null
                    }
                }
            }
        ) {
            posts.filter { it.latitude != null && it.longitude != null }.forEach { post ->
                Marker(
                    state = MarkerState(position = LatLng(post.latitude!!, post.longitude!!)),
                    title = post.title ?: "Post",
                    onClick = {
                        isSheetVisible = true
                        false
                    }
                )
            }

            selectedLocation?.let { location ->
                Marker(
                    state = MarkerState(
                        position = LatLng(location.geometry.latitude, location.geometry.longitude)
                    ),
                    title = location.properties.name ?: "Lieu recherché",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                    onClick = {
                        isSheetVisible = true
                        false
                    }
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter)
        ) {
            TravelingSearchBar(
                placeholder = "Rechercher un lieu",
                initialValue = searchQuery,
                onValueChange = { searchQuery = it }
            )
            
            if (suggestions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column {
                        suggestions.take(5).forEach { feature ->
                            ListItem(
                                headlineContent = { Text(feature.properties.name ?: "") },
                                supportingContent = { Text(feature.properties.displayName) },
                                modifier = Modifier.clickable {
                                    selectedLocation = feature
                                    searchQuery = feature.properties.name ?: ""
                                    suggestions = emptyList()
                                }
                            )
                        }
                    }
                }
            }
        }

        if (isSheetVisible && (selectedLocation != null || posts.any { it.latitude != null })) {
            ModalBottomSheet(
                onDismissRequest = { 
                    isSheetVisible = false
                    selectedLocation = null
                },
                sheetState = sheetState,
                containerColor = Color.White
            ) {
                val locName = selectedLocation?.properties?.name ?: "Lieu sélectionné"
                val lat = selectedLocation?.geometry?.latitude ?: 0.0
                val lon = selectedLocation?.geometry?.longitude ?: 0.0

                val nearbyPosts = posts.filter { post ->
                    val pLat = post.latitude ?: return@filter false
                    val pLon = post.longitude ?: return@filter false
                    val dLat = pLat - lat
                    val dLon = pLon - lon
                    (dLat * dLat + dLon * dLon) < 0.01
                }
                LocationDetailContent(
                    locationName = locName,
                    posts = nearbyPosts,
                    onViewPosts = { 
                        isSheetVisible = false
                        onNavigateToFeed(locName)
                    },
                    onLocationClick = {
                        val gmmIntentUri = Uri.parse("geo:$lat,$lon?q=$locName")
                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                        mapIntent.setPackage("com.google.android.apps.maps")
                        context.startActivity(mapIntent)
                    },
                    onPostPhoto = {
                        isSheetVisible = false
                        onNavigateToCreatePost(locName, lat, lon)
                    }
                )
            }
        }
    }
}

@Composable
fun LocationDetailContent(
    locationName: String?,
    posts: List<Post>,
    onViewPosts: () -> Unit,
    onLocationClick: () -> Unit,
    onPostPhoto: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = locationName ?: "Lieu inconnu",
            style = MaterialTheme.typography.headlineMedium,
            color = TravelingDeepPurple,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        if (posts.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(posts) { post ->
                    Box(modifier = Modifier.size(150.dp, 100.dp)) {
                        if (post.fullImageUrl != null) {
                            AsyncImage(
                                model = post.fullImageUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = Color.LightGray.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = post.content ?: "",
                                    modifier = Modifier.padding(8.dp),
                                    fontSize = 12.sp,
                                    maxLines = 4
                                )
                            }
                        }
                        
                        if (post.audioUrl != null) {
                            Icon(
                                Icons.Outlined.Mic,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
                            )
                        }
                    }
                }
            }
        } else {
            Text("Aucun post pour ce lieu", color = Color.Gray)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MapActionButton(icon = Icons.Default.Language, label = "Voir les posts", onClick = onViewPosts)
            MapActionButton(icon = Icons.AutoMirrored.Filled.DirectionsWalk, label = "Itinéraire", onClick = onLocationClick)
            MapActionButton(icon = Icons.AutoMirrored.Filled.Send, label = "Poster une photo", onClick = onPostPhoto)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun MapActionButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Icon(icon, contentDescription = null, tint = TravelingDeepPurple, modifier = Modifier.size(32.dp))
        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TravelingDeepPurple)
    }
}
