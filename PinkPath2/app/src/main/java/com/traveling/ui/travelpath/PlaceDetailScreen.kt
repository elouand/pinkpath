package com.traveling.ui.travelpath

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.traveling.domain.model.PlaceDetails
import com.traveling.ui.theme.TravelingDeepPurple
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceDetailScreen(
    placeId: String,
    viewModel: PlaceViewModel = hiltViewModel(),
    mapViewModel: MapViewModel, // Reçoit le ViewModel partagé
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val mapState by mapViewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showTransportDialog by remember { mutableStateOf(false) }

    LaunchedEffect(placeId) {
        Log.d("PlaceDetail", "🔍 Chargement des détails pour: $placeId")
        viewModel.loadPlaceDetails(placeId)
    }

    // Si le trajet est calculé avec succès dans le ViewModel partagé, on quitte
    LaunchedEffect(mapState.currentRoute) {
        if (mapState.currentRoute != null && mapState.currentRoute!!.isNotEmpty()) {
            Log.d("PlaceDetail", "✅ Trajet reçu avec succès, retour à la carte")
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
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val currentState = state) {
                is PlaceDetailState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = TravelingDeepPurple)
                is PlaceDetailState.Error -> Text(currentState.message, modifier = Modifier.align(Alignment.Center), color = Color.Red)
                is PlaceDetailState.Success -> PlaceDetailsContent(details = currentState.details)
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
        Log.d("PlaceDetail", "📍 Position de départ (via ViewModel): ${userLocation.latitude}, ${userLocation.longitude}")
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
fun PlaceDetailsContent(details: PlaceDetails) {
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
        
        // Bouton Google Maps
        OutlinedButton(
            onClick = {
                val gmmIntentUri = Uri.parse("geo:0,0?q=${details.latitude},${details.longitude}(${Uri.encode(details.name)})")
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                // On essaie d'ouvrir l'application Google Maps spécifiquement
                mapIntent.setPackage("com.google.android.apps.maps")
                
                // Si Google Maps n'est pas installé, on ouvre une application de cartes générique
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
            InfoItem(icon = Icons.Default.LocationOn, text = addr)
        }
        if (details.phone != null) InfoItem(icon = Icons.Default.Phone, text = details.phone)
        if (details.website != null) InfoItem(icon = Icons.Default.Language, text = details.website)
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
@Composable
fun InfoSection(title: String, content: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(text = content, style = MaterialTheme.typography.bodyMedium)
    }
}
