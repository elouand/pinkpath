package com.traveling.ui.travelpath

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.preference.PreferenceManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.traveling.domain.model.Place
import com.traveling.ui.theme.TravelingDeepPurple
import org.osmdroid.config.Configuration
import org.osmdroid.events.DelayedMapListener
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel = hiltViewModel(),
    onPlaceClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // États UI
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var selectedPlace by remember { mutableStateOf<Place?>(null) }
    var showTransportDialog by remember { mutableStateOf(false) }
    
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { hasLocationPermission = it }
    )

    val mapView = remember { MapView(context) }
    val myLocationOverlay = remember { MyLocationNewOverlay(GpsMyLocationProvider(context), mapView) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    mapView.onResume()
                    if (hasLocationPermission) myLocationOverlay.enableMyLocation()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    mapView.onPause()
                    myLocationOverlay.disableMyLocation()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
        if (!hasLocationPermission) launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            myLocationOverlay.enableMyLocation()
            myLocationOverlay.runOnFirstFix {
                val location = myLocationOverlay.myLocation
                if (location != null) {
                    ContextCompat.getMainExecutor(context).execute {
                        mapView.controller.animateTo(location)
                        viewModel.updateUserLocation(location.latitude, location.longitude)
                        viewModel.fetchNearbyPlaces(location.latitude, location.longitude)
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { 
                mapView.apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(16.0)
                    overlays.add(myLocationOverlay)

                    addMapListener(DelayedMapListener(object : MapListener {
                        override fun onScroll(event: ScrollEvent?): Boolean {
                            viewModel.fetchNearbyPlaces(mapCenter.latitude, mapCenter.longitude)
                            return true
                        }
                        override fun onZoom(event: ZoomEvent?): Boolean {
                            viewModel.fetchNearbyPlaces(mapCenter.latitude, mapCenter.longitude)
                            return true
                        }
                    }, 600))
                }
            },
            update = { view ->
                // Mise à jour des marqueurs
                val markersToRemove = view.overlays.filterIsInstance<Marker>()
                view.overlays.removeAll(markersToRemove)

                uiState.places.forEach { place ->
                    val marker = Marker(view)
                    marker.position = GeoPoint(place.latitude, place.longitude)
                    marker.title = place.name
                    
                    val t = place.type.lowercase()
                    val markerColor = when {
                        t.contains("restaurant") || t.contains("food") -> Color(0xFFE64A19)
                        t.contains("hotel") -> Color(0xFF1976D2)
                        t.contains("park") || t.contains("garden") -> Color(0xFF388E3C)
                        else -> TravelingDeepPurple
                    }
                    
                    val icon = ContextCompat.getDrawable(context, org.osmdroid.library.R.drawable.marker_default)?.mutate()
                    icon?.setTint(markerColor.toArgb())
                    marker.icon = icon
                    
                    marker.setOnMarkerClickListener { m, _ ->
                        selectedPlace = place
                        m.showInfoWindow()
                        true
                    }
                    view.overlays.add(marker)
                }

                // Mise à jour de l'itinéraire (Polyline)
                val existingPolylines = view.overlays.filterIsInstance<Polyline>()
                view.overlays.removeAll(existingPolylines)

                uiState.currentRoute?.let { points ->
                    val polyline = Polyline()
                    polyline.setPoints(points.map { GeoPoint(it.latitude, it.longitude) })
                    polyline.outlinePaint.color = TravelingDeepPurple.toArgb()
                    polyline.outlinePaint.strokeWidth = 10f
                    view.overlays.add(polyline)
                }

                view.invalidate()
            }
        )

        // Barre de recherche
        SearchBar(
            query = searchQuery,
            onQueryChange = { 
                searchQuery = it
                val center = mapView.mapCenter
                viewModel.onSearchQueryChanged(it, center.latitude, center.longitude)
            },
            onSearch = { isSearchActive = false },
            active = isSearchActive,
            onActiveChange = { isSearchActive = it },
            placeholder = { Text("Rechercher un lieu...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp).fillMaxWidth(if (isSearchActive) 1f else 0.9f)
        ) {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(uiState.searchResults) { result ->
                    ListItem(
                        headlineContent = { Text(result.properties.name ?: "Inconnu") },
                        supportingContent = { Text(result.properties.displayName) },
                        modifier = Modifier.clickable {
                            val point = GeoPoint(result.geometry.latitude, result.geometry.longitude)
                            if (result.properties.osmType == "N" && result.properties.osmId != null) {
                                onPlaceClick(result.properties.osmId.toString())
                            } else {
                                mapView.controller.animateTo(point)
                                mapView.controller.setZoom(17.0)
                                viewModel.fetchNearbyPlaces(point.latitude, point.longitude)
                            }
                            isSearchActive = false
                        }
                    )
                }
            }
        }

        // Floating Buttons
        Column(
            modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = if (selectedPlace != null) 200.dp else 32.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (uiState.currentRoute != null) {
                SmallFloatingActionButton(
                    onClick = { viewModel.clearRoute() },
                    containerColor = Color.Red,
                    contentColor = Color.White
                ) { Icon(Icons.Default.Clear, contentDescription = "Effacer trajet") }
            }
            SmallFloatingActionButton(
                onClick = { 
                    myLocationOverlay.myLocation?.let { 
                        mapView.controller.animateTo(it)
                        viewModel.updateUserLocation(it.latitude, it.longitude)
                    } 
                },
                containerColor = TravelingDeepPurple,
                contentColor = Color.White
            ) { Icon(Icons.Default.MyLocation, contentDescription = null) }
        }

        // Panneau de détails avec bouton "Y aller"
        AnimatedVisibility(
            visible = selectedPlace != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            selectedPlace?.let { place ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = place.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text(text = place.type, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                            }
                            IconButton(onClick = { selectedPlace = null }) { Icon(Icons.Default.Close, contentDescription = null) }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { onPlaceClick(place.id) }, modifier = Modifier.weight(1f)) {
                                Text("Détails")
                            }
                            Button(
                                onClick = { showTransportDialog = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = TravelingDeepPurple)
                            ) {
                                Icon(Icons.Default.Directions, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Y aller")
                            }
                        }
                    }
                }
            }
        }

        // Dialogue de sélection du transport
        if (showTransportDialog) {
            AlertDialog(
                onDismissRequest = { showTransportDialog = false },
                title = { Text("Moyen de transport") },
                text = {
                    Column {
                        TransportOption(Icons.Default.DirectionsWalk, "À pied") {
                            Log.d("MapScreen", "Calcul itinéraire: walking")
                            startRoute(viewModel, myLocationOverlay, selectedPlace, "walking")
                            showTransportDialog = false
                        }
                        TransportOption(Icons.Default.DirectionsBike, "Vélo") {
                            Log.d("MapScreen", "Calcul itinéraire: bicycle")
                            startRoute(viewModel, myLocationOverlay, selectedPlace, "bicycle")
                            showTransportDialog = false
                        }
                        TransportOption(Icons.Default.DirectionsCar, "Voiture") {
                            Log.d("MapScreen", "Calcul itinéraire: driving")
                            startRoute(viewModel, myLocationOverlay, selectedPlace, "driving")
                            showTransportDialog = false
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showTransportDialog = false }) { Text("Annuler") } }
            )
        }

        if (uiState.isCalculatingRoute) CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = TravelingDeepPurple)
    }
}

@Composable
fun TransportOption(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = TravelingDeepPurple)
        Spacer(Modifier.width(16.dp))
        Text(label)
    }
}

private fun startRoute(viewModel: MapViewModel, locationOverlay: MyLocationNewOverlay, destination: Place?, mode: String) {
    val start = locationOverlay.myLocation
    if (start != null && destination != null) {
        Log.d("MapScreen", "🚀 startRoute vers ${destination.name} en mode $mode")
        viewModel.updateUserLocation(start.latitude, start.longitude)
        viewModel.calculateRoute(
            start.latitude, start.longitude,
            destination.latitude, destination.longitude,
            mode
        )
    } else {
        Log.w("MapScreen", "⚠️ Position ou destination nulle")
    }
}
