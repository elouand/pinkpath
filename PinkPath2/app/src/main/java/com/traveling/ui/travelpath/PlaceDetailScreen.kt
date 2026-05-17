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
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.text.font.FontStyle
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

    fun translateType(type: String): String = when (type.lowercase()) {
        "restaurant" -> "Restaurant"
        "museum" -> "Musée"
        "park" -> "Parc"
        "cafe", "coffee_shop" -> "Café"
        "bar" -> "Bar"
        "hotel" -> "Hôtel"
        "attraction" -> "Attraction"
        "gallery" -> "Galerie"
        "bakery" -> "Boulangerie"
        "fast_food" -> "Restauration rapide"
        "theatre" -> "Théâtre"
        "cinema" -> "Cinéma"
        "library" -> "Bibliothèque"
        "supermarket" -> "Supermarché"
        "pharmacy" -> "Pharmacie"
        "hospital" -> "Hôpital"
        "school" -> "École"
        "church" -> "Église"
        "viewpoint" -> "Point de vue"
        "beach" -> "Plage"
        "garden" -> "Jardin"
        "playground" -> "Aire de jeux"
        else -> type.replaceFirstChar { it.uppercase() }
    }

    fun typeColor(type: String): Color = when (type.lowercase()) {
        "restaurant", "fast_food", "bakery" -> Color(0xFFE64A19)
        "cafe", "bar" -> Color(0xFF6D4C41)
        "museum", "gallery", "attraction" -> Color(0xFF1565C0)
        "park", "garden", "viewpoint" -> Color(0xFF2E7D32)
        "hotel" -> Color(0xFF1976D2)
        "cinema", "theatre" -> Color(0xFF6A1B9A)
        "hospital", "pharmacy" -> Color(0xFFC62828)
        else -> TravelingDeepPurple
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 80.dp)) {
        // 1. Hero image
        if (details.wikiImage != null) {
            AsyncImage(
                model = details.wikiImage,
                contentDescription = details.name,
                modifier = Modifier.fillMaxWidth().height(200.dp),
                contentScale = ContentScale.Crop
            )
        } else if (details.photos.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(200.dp).padding(horizontal = 16.dp)
            ) {
                items(details.photos) { photo ->
                    Card {
                        AsyncImage(
                            model = photo.url,
                            contentDescription = null,
                            modifier = Modifier.fillMaxHeight().width(300.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {
            // 2. Nom + type
            Text(details.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

            // 4. Chip catégorie
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = typeColor(details.type).copy(alpha = 0.15f),
                border = BorderStroke(1.dp, typeColor(details.type).copy(alpha = 0.5f))
            ) {
                Text(
                    translateType(details.type),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    color = typeColor(details.type),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // 3. Status ouvert/fermé
            if (details.isOpenNow != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (details.isOpenNow) Color(0xFF2E7D32).copy(alpha = 0.15f) else Color(0xFFC62828).copy(alpha = 0.15f)
                    ) {
                        Text(
                            if (details.isOpenNow) "Ouvert" else "Fermé",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            color = if (details.isOpenNow) Color(0xFF2E7D32) else Color(0xFFC62828),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    if (details.opening_hours != null) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        Text(details.opening_hours, color = Color.Gray, fontSize = 13.sp)
                    }
                }
            } else if (details.opening_hours != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = TravelingDeepPurple, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(details.opening_hours, fontSize = 13.sp)
                }
            }

            // 5. Résumé Wikipedia
            if (details.wikiSummary != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp).padding(top = 2.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            details.wikiSummary,
                            color = Color.Gray,
                            fontSize = 13.sp,
                            fontStyle = FontStyle.Italic,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 6. Infos de contact
            if (details.phone != null) {
                ClickableInfoItem(
                    icon = Icons.Default.Phone,
                    text = details.phone,
                    onClick = {
                        try { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${details.phone}"))) } catch (_: Exception) {}
                    }
                )
            }
            if (details.website != null) {
                ClickableInfoItem(
                    icon = Icons.Default.Language,
                    text = details.website,
                    onClick = {
                        try {
                            val url = if (details.website.startsWith("http")) details.website else "https://${details.website}"
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        } catch (_: Exception) {}
                    }
                )
            }
            if (details.email != null) {
                ClickableInfoItem(
                    icon = Icons.Default.Email,
                    text = details.email,
                    onClick = {
                        try { context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${details.email}"))) } catch (_: Exception) {}
                    }
                )
            }

            // 7. Détails pratiques
            if (details.address != null) {
                val addr = listOfNotNull(details.address.housenumber, details.address.street, details.address.city).joinToString(" ")
                if (addr.isNotBlank()) InfoItem(icon = Icons.Default.LocationOn, text = addr)
            }
            if (details.cuisine != null) {
                InfoItem(icon = Icons.Default.RestaurantMenu, text = details.cuisine.replaceFirstChar { it.uppercase() })
            }
            if (details.fee != null) {
                val feeText = when (details.fee.lowercase()) {
                    "yes" -> "Payant"
                    "no" -> "Gratuit"
                    else -> details.fee
                }
                InfoItem(icon = Icons.Default.Euro, text = feeText)
            }
            if (details.wheelchair != null) {
                val wText = when (details.wheelchair.lowercase()) {
                    "yes", "designated" -> "Accessible PMR"
                    "no" -> "Non accessible PMR"
                    "limited" -> "Accès PMR limité"
                    else -> details.wheelchair
                }
                InfoItem(icon = Icons.Default.Accessible, text = wText)
            }
            if (details.operator != null) {
                InfoItem(icon = Icons.Default.Business, text = "Exploitant : ${details.operator}")
            }
            if (details.brand != null) {
                InfoItem(icon = Icons.Default.Store, text = "Marque : ${details.brand}")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 8. Bouton Google Maps
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

            Spacer(modifier = Modifier.height(24.dp))

            // 9. Posts
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
}

@Composable
fun ClickableInfoItem(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 6.dp)
    ) {
        Icon(icon, contentDescription = null, tint = TravelingDeepPurple, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, color = TravelingDeepPurple)
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
