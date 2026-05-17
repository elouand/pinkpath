package com.traveling.ui.travelpath

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.traveling.domain.model.PlaceDetails
import com.traveling.ui.common.PostCard
import com.traveling.ui.theme.TravelingDeepPurple
import com.traveling.ui.theme.TravelingTagBlue
import com.traveling.ui.travelshare.AuthViewModel
import com.traveling.ui.travelshare.PostViewModel
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceDetailScreen(
    placeId: String,
    viewModel: PlaceViewModel = hiltViewModel(),
    mapViewModel: MapViewModel,
    postViewModel: PostViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onCreatePostClick: (String, Double, Double) -> Unit,
    onPostClick: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val mapState by mapViewModel.uiState.collectAsState()
    val posts by postViewModel.posts.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    val context = LocalContext.current
    var showTransportDialog by remember { mutableStateOf(false) }

    LaunchedEffect(placeId) {
        viewModel.loadPlaceDetails(placeId)
        postViewModel.loadPosts()
    }

    LaunchedEffect(mapState.currentRoute) {
        if (mapState.currentRoute != null && mapState.currentRoute!!.isNotEmpty()) {
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Détails du lieu") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TravelingDeepPurple,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            if (state is PlaceDetailState.Success) {
                val details = (state as PlaceDetailState.Success).details
                Column(horizontalAlignment = Alignment.End) {
                    SmallFloatingActionButton(
                        onClick = { onCreatePostClick(details.name, details.latitude, details.longitude) },
                        containerColor = TravelingDeepPurple,
                        contentColor = Color.White,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Créer un post")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    ExtendedFloatingActionButton(
                        onClick = { showTransportDialog = true },
                        containerColor = TravelingDeepPurple,
                        contentColor = Color.White,
                        icon = { 
                            if (mapState.isCalculatingRoute) 
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                            else 
                                Icon(Icons.Default.Directions, contentDescription = null) 
                        },
                        text = { Text("Y aller") }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val currentState = state) {
                is PlaceDetailState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = TravelingDeepPurple)
                is PlaceDetailState.Error -> Text(currentState.message, modifier = Modifier.align(Alignment.Center), color = Color.Red)
                is PlaceDetailState.Success -> PlaceDetailsContent(
                    details = currentState.details,
                    posts = posts.filter { it.title == currentState.details.name },
                    onPostClick = onPostClick,
                    onLikeClick = { postId ->
                        currentUser?.id?.toIntOrNull()?.let { userId ->
                            postViewModel.toggleLike(postId, userId)
                        }
                    }
                )
            }
        }

        if (showTransportDialog && state is PlaceDetailState.Success) {
            val details = (state as PlaceDetailState.Success).details
            AlertDialog(
                onDismissRequest = { showTransportDialog = false },
                title = { Text("Moyen de transport") },
                text = {
                    Column {
                        TransportOptionItem(Icons.AutoMirrored.Filled.DirectionsWalk, "À pied") {
                            calculateRoute(context, mapViewModel, details, "walking")
                            showTransportDialog = false
                        }
                        TransportOptionItem(Icons.AutoMirrored.Filled.DirectionsBike, "Vélo") {
                            calculateRoute(context, mapViewModel, details, "bicycle")
                            showTransportDialog = false
                        }
                        TransportOptionItem(Icons.Default.DirectionsCar, "Voiture") {
                            calculateRoute(context, mapViewModel, details, "driving")
                            showTransportDialog = false
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showTransportDialog = false }) { Text("Annuler") } }
            )
        }
    }
}

private fun calculateRoute(context: android.content.Context, mapViewModel: MapViewModel, details: PlaceDetails, mode: String) {
    val userLocation = mapViewModel.uiState.value.userLocation
    
    if (userLocation != null) {
        mapViewModel.calculateRoute(userLocation.latitude, userLocation.longitude, details.latitude, details.longitude, mode)
    } else {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val provider = GpsMyLocationProvider(context)
            val lastLocation = provider.lastKnownLocation
            if (lastLocation != null) {
                mapViewModel.calculateRoute(lastLocation.latitude, lastLocation.longitude, details.latitude, details.longitude, mode)
            } else {
                mapViewModel.calculateRoute(43.6107, 3.8767, details.latitude, details.longitude, mode)
            }
        } else {
            mapViewModel.calculateRoute(43.6107, 3.8767, details.latitude, details.longitude, mode)
        }
    }
}

@Composable
fun TransportOptionItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = TravelingDeepPurple)
        Spacer(Modifier.width(16.dp))
        Text(label)
    }
}

@Composable
fun PlaceDetailsContent(
    details: PlaceDetails,
    posts: List<com.traveling.domain.model.Post>,
    onPostClick: (String) -> Unit,
    onLikeClick: (String) -> Unit
) {
    val context = LocalContext.current
    
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).padding(bottom = 80.dp)) {
        if (details.photos.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.height(200.dp)) {
                items(details.photos) { photo ->
                    Card { AsyncImage(model = photo.url, contentDescription = null, modifier = Modifier.fillMaxHeight().width(300.dp), contentScale = ContentScale.Crop) }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        Text(details.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(details.type, color = TravelingDeepPurple)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedButton(
            onClick = {
                val gmmIntentUri = Uri.parse("geo:0,0?q=${details.latitude},${details.longitude}(${Uri.encode(details.name)})")
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                mapIntent.setPackage("com.google.android.apps.maps")
                if (mapIntent.resolveActivity(context.packageManager) == null) {
                    mapIntent.setPackage(null)
                }
                context.startActivity(mapIntent)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TravelingDeepPurple)
        ) {
            Icon(Icons.Default.Map, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Voir sur Google Maps")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (details.address != null) {
            val addr = listOfNotNull(details.address.housenumber, details.address.street, details.address.city).joinToString(" ")
            if (addr.isNotBlank()) InfoItem(icon = Icons.Default.LocationOn, text = addr)
        }
        if (details.phone != null) InfoItem(icon = Icons.Default.Phone, text = details.phone)
        if (details.website != null) InfoItem(icon = Icons.Default.Language, text = details.website)

        Spacer(modifier = Modifier.height(24.dp))

        Text("Posts sur ce lieu", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TravelingDeepPurple)
        Spacer(modifier = Modifier.height(8.dp))

        if (posts.isEmpty()) {
            Text("Aucun post pour le moment. Soyez le premier !", color = Color.Gray, fontSize = 14.sp)
        } else {
            posts.forEach { post ->
                PostCard(
                    title = post.content ?: post.title ?: "Sans titre",
                    tags = post.tags?.map { it to TravelingTagBlue } ?: emptyList(),
                    location = post.title ?: details.name,
                    author = post.authorName ?: "Anonyme",
                    authorProfileUrl = post.authorAvatar,
                    imageUrl = post.fullImageUrl,
                    likes = post.likes.toString(),
                    comments = post.commentsCount.toString(),
                    isLiked = post.isLiked,
                    onLikeClick = { onLikeClick(post.id) },
                    onClick = { onPostClick(post.id) },
                    sharedItinerary = post.sharedItinerary
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun InfoItem(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Icon(icon, contentDescription = null, tint = TravelingDeepPurple, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text)
    }
}
