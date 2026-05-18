package com.traveling.ui.travelpath

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.traveling.domain.model.ItineraryStep
import com.traveling.domain.model.ItineraryVariant
import com.traveling.domain.model.LocationPoint
import com.traveling.domain.model.StepDetailResponse
import com.traveling.domain.model.WeatherDay
import com.traveling.ui.theme.TravelingDeepPurple
import com.traveling.ui.travelshare.AuthTextField
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePathScreen(
    onBack: () -> Unit,
    viewModel: ItineraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // ── State ─────────────────────────────────────────────────────────────
    var pathName by remember { mutableStateOf("") }
    var locationQuery by remember { mutableStateOf("") }
    var locations by remember { mutableStateOf<List<LocationPoint>>(emptyList()) }

    // ── GPS ───────────────────────────────────────────────────────────────
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    fun applyGpsLocation() {
        try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            @Suppress("MissingPermission")
            val loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            if (loc != null) {
                val gpsPoint = LocationPoint(loc.latitude, loc.longitude, "Ma position")
                locations = listOf(gpsPoint) + locations.filter { it.name != "Ma position" }
            }
        } catch (_: Exception) {}
    }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasLocationPermission = granted
        if (granted) applyGpsLocation()
    }

    fun useGpsLocation() {
        if (hasLocationPermission) applyGpsLocation()
        else locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    var durationMinutes by remember { mutableStateOf(60f) }
    var selectedMode by remember { mutableStateOf("walking") }
    var selectedActivities by remember { mutableStateOf(setOf("Restauration", "Culture", "Loisirs")) }
    var wantsGoodWeather by remember { mutableStateOf(false) }
    var budget by remember { mutableStateOf(0f) }
    var effortLevel by remember { mutableStateOf("normal") }
    var weatherSensitivity by remember { mutableStateOf(setOf<String>()) }
    var timeSlot by remember { mutableStateOf("all") }

    var showSaveDialog by remember { mutableStateOf(false) }
    var variantToSave by remember { mutableStateOf<ItineraryVariant?>(null) }
    var saveNameInput by remember { mutableStateOf("") }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            viewModel.clearSaveSuccess()
            onBack()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null, tint = TravelingDeepPurple)
                }
                Text(
                    "Créer un itinéraire",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TravelingDeepPurple,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Nom ──────────────────────────────────────────────────
            AuthTextField(
                value = pathName,
                onValueChange = { pathName = it },
                placeholder = "Nom de l'itinéraire (optionnel)"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Recherche de lieux ────────────────────────────────────
            Text("Lieux de départ / à inclure", fontWeight = FontWeight.Bold, color = TravelingDeepPurple, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = locationQuery,
                    onValueChange = {
                        locationQuery = it
                        viewModel.searchLocation(it)
                    },
                    placeholder = { Text("Rechercher un lieu…") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        if (locationQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                locationQuery = ""
                                viewModel.clearSearchResults()
                            }) { Icon(Icons.Default.Clear, null) }
                        }
                    }
                )
                // Bouton GPS
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (locations.any { it.name == "Ma position" })
                        TravelingDeepPurple else TravelingDeepPurple.copy(alpha = 0.1f),
                    modifier = Modifier
                        .size(56.dp)
                        .clickable { useGpsLocation() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.MyLocation,
                            contentDescription = "Ma position",
                            tint = if (locations.any { it.name == "Ma position" })
                                Color.White else TravelingDeepPurple,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            if (uiState.searchResults.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column {
                        uiState.searchResults.take(5).forEach { feature ->
                            ListItem(
                                headlineContent = { Text(feature.properties.name ?: "") },
                                supportingContent = { Text(feature.properties.displayName, maxLines = 1) },
                                leadingContent = { Icon(Icons.Default.LocationOn, null, tint = TravelingDeepPurple) },
                                modifier = Modifier.clickable {
                                    val point = LocationPoint(
                                        lat = feature.geometry.latitude,
                                        lon = feature.geometry.longitude,
                                        name = feature.properties.name ?: "Lieu"
                                    )
                                    if (locations.none { it.name == point.name }) {
                                        locations = locations + point
                                    }
                                    locationQuery = ""
                                    viewModel.clearSearchResults()
                                }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }

            if (locations.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                    locations.forEachIndexed { index, loc ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (loc.name == "Ma position") TravelingDeepPurple.copy(alpha = 0.15f)
                                    else TravelingDeepPurple.copy(alpha = 0.08f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (loc.name == "Ma position") Icons.Default.MyLocation else Icons.Default.Stars,
                                null,
                                tint = TravelingDeepPurple,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(loc.name, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                            IconButton(
                                onClick = { locations = locations.toMutableList().also { it.removeAt(index) } },
                                modifier = Modifier.size(24.dp)
                            ) { Icon(Icons.Default.Close, null, tint = Color.Gray, modifier = Modifier.size(16.dp)) }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Durée ─────────────────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Durée souhaitée", fontWeight = FontWeight.Bold, color = TravelingDeepPurple)
                Text("${durationMinutes.roundToInt()} min", fontWeight = FontWeight.Bold, color = TravelingDeepPurple)
            }
            Slider(
                value = durationMinutes,
                onValueChange = { durationMinutes = it },
                valueRange = 30f..240f,
                steps = 13,
                colors = SliderDefaults.colors(thumbColor = TravelingDeepPurple, activeTrackColor = TravelingDeepPurple),
                modifier = Modifier.fillMaxWidth()
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("30 min", fontSize = 12.sp, color = Color.Gray)
                Text("4h", fontSize = 12.sp, color = Color.Gray)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Mode de déplacement ────────────────────────────────────
            Text("Mode de déplacement", fontWeight = FontWeight.Bold, color = TravelingDeepPurple, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ModeButton(
                    icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                    label = "Effort modéré",
                    isSelected = selectedMode == "walking",
                    onClick = { selectedMode = "walking" },
                    modifier = Modifier.weight(1f)
                )
                ModeButton(
                    icon = Icons.AutoMirrored.Filled.DirectionsRun,
                    label = "Gros effort",
                    isSelected = selectedMode == "jogging",
                    onClick = { selectedMode = "jogging" },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Activités ─────────────────────────────────────────────
            Text("Activités", fontWeight = FontWeight.Bold, color = TravelingDeepPurple, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActivityToggle(
                    name = "Restauration",
                    icon = Icons.Default.Restaurant,
                    isSelected = selectedActivities.contains("Restauration"),
                    modifier = Modifier.weight(1f),
                    onClick = { selectedActivities = selectedActivities.toggle("Restauration") }
                )
                ActivityToggle(
                    name = "Culture",
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    isSelected = selectedActivities.contains("Culture"),
                    modifier = Modifier.weight(1f),
                    onClick = { selectedActivities = selectedActivities.toggle("Culture") }
                )
                ActivityToggle(
                    name = "Loisirs",
                    icon = Icons.Default.SportsTennis,
                    isSelected = selectedActivities.contains("Loisirs"),
                    modifier = Modifier.weight(1f),
                    onClick = { selectedActivities = selectedActivities.toggle("Loisirs") }
                )
                ActivityToggle(
                    name = "Découverte",
                    icon = Icons.Default.Park,
                    isSelected = selectedActivities.contains("Découverte"),
                    modifier = Modifier.weight(1f),
                    onClick = { selectedActivities = selectedActivities.toggle("Découverte") }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Budget max ────────────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Euro, null, tint = TravelingDeepPurple, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Budget max", fontWeight = FontWeight.Bold, color = TravelingDeepPurple)
                }
                Text(
                    if (budget == 0f) "Illimité" else "${budget.roundToInt()} €",
                    fontWeight = FontWeight.Bold,
                    color = if (budget == 0f) Color.Gray else TravelingDeepPurple
                )
            }
            Slider(
                value = budget,
                onValueChange = { budget = it },
                valueRange = 0f..200f,
                steps = 19,
                colors = SliderDefaults.colors(thumbColor = TravelingDeepPurple, activeTrackColor = TravelingDeepPurple),
                modifier = Modifier.fillMaxWidth()
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Illimité", fontSize = 12.sp, color = Color.Gray)
                Text("200 €", fontSize = 12.sp, color = Color.Gray)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Niveau d'effort ───────────────────────────────────────
            Text("Niveau d'effort", fontWeight = FontWeight.Bold, color = TravelingDeepPurple, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(4.dp))
            Text("Adapte les distances entre les étapes", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EffortChip(
                    label = "Normal",
                    emoji = "🚶",
                    isSelected = effortLevel == "normal",
                    onClick = { effortLevel = "normal" },
                    modifier = Modifier.weight(1f)
                )
                EffortChip(
                    label = "Réduit",
                    emoji = "🧓",
                    isSelected = effortLevel == "reduced",
                    onClick = { effortLevel = "reduced" },
                    modifier = Modifier.weight(1f)
                )
                EffortChip(
                    label = "Minimal",
                    emoji = "♿",
                    isSelected = effortLevel == "minimal",
                    onClick = { effortLevel = "minimal" },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Météo ─────────────────────────────────────────────────
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (wantsGoodWeather) Color(0xFFFFF8E1) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp,
                    if (wantsGoodWeather) Color(0xFFFFC107) else Color.LightGray
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { wantsGoodWeather = !wantsGoodWeather }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = if (wantsGoodWeather) "☀️" else "🌤️", fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Beau temps",
                            fontWeight = FontWeight.Bold,
                            color = if (wantsGoodWeather) Color(0xFF795548) else Color.Gray,
                            fontSize = 15.sp
                        )
                        Text(
                            "Voir les meilleurs jours météo",
                            fontSize = 12.sp,
                            color = if (wantsGoodWeather) Color(0xFF9E7B3A) else Color.Gray
                        )
                    }
                    Switch(
                        checked = wantsGoodWeather,
                        onCheckedChange = { wantsGoodWeather = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFFFFC107)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Sensibilité météo ─────────────────────────────────────
            Text("Sensibilité météo", fontWeight = FontWeight.Bold, color = TravelingDeepPurple, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(4.dp))
            Text("Privilégie les lieux couverts selon vos contraintes", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SensitivityChip(
                    label = "Chaleur",
                    emoji = "🌡️",
                    isSelected = weatherSensitivity.contains("chaleur"),
                    onClick = { weatherSensitivity = weatherSensitivity.toggle("chaleur") },
                    modifier = Modifier.weight(1f)
                )
                SensitivityChip(
                    label = "Froid",
                    emoji = "🥶",
                    isSelected = weatherSensitivity.contains("froid"),
                    onClick = { weatherSensitivity = weatherSensitivity.toggle("froid") },
                    modifier = Modifier.weight(1f)
                )
                SensitivityChip(
                    label = "Humidité",
                    emoji = "💧",
                    isSelected = weatherSensitivity.contains("humidite"),
                    onClick = { weatherSensitivity = weatherSensitivity.toggle("humidite") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Créneau horaire ───────────────────────────────────────
            Text("Créneau horaire", fontWeight = FontWeight.Bold, color = TravelingDeepPurple, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TimeSlotChip(
                    label = "Tout",
                    emoji = "🕐",
                    isSelected = timeSlot == "all",
                    onClick = { timeSlot = "all" },
                    modifier = Modifier.weight(1f)
                )
                TimeSlotChip(
                    label = "Matin",
                    emoji = "🌅",
                    isSelected = timeSlot == "matin",
                    onClick = { timeSlot = "matin" },
                    modifier = Modifier.weight(1f)
                )
                TimeSlotChip(
                    label = "Aprés-midi",
                    emoji = "☀️",
                    isSelected = timeSlot == "après-midi",
                    onClick = { timeSlot = "après-midi" },
                    modifier = Modifier.weight(1f)
                )
                TimeSlotChip(
                    label = "Soir",
                    emoji = "🌆",
                    isSelected = timeSlot == "soir",
                    onClick = { timeSlot = "soir" },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Erreur ────────────────────────────────────────────────
            uiState.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
            }

            // ── Bouton Calculer ───────────────────────────────────────
            Button(
                onClick = {
                    viewModel.clearError()
                    if (locations.isEmpty()) return@Button
                    viewModel.generateItineraries(
                        locations = locations,
                        durationMinutes = durationMinutes.roundToInt(),
                        mode = selectedMode,
                        activities = selectedActivities.toList(),
                        wantsGoodWeather = wantsGoodWeather,
                        budget = budget.roundToInt(),
                        effortLevel = effortLevel,
                        weatherSensitivity = weatherSensitivity.toList(),
                        timeSlot = timeSlot
                    )
                },
                enabled = locations.isNotEmpty() && !uiState.isGenerating,
                modifier = Modifier.fillMaxWidth(0.85f).height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TravelingDeepPurple),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (uiState.isGenerating) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text("Calculer l'itinéraire", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (locations.isEmpty()) {
                Text("Ajoutez au moins un lieu de départ", color = Color.Gray, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // ── Bottom sheet variantes ────────────────────────────────────
        if (uiState.variants != null) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.clearVariants() },
                containerColor = MaterialTheme.colorScheme.background,
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Choisissez votre itinéraire",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = TravelingDeepPurple
                        )
                        TextButton(onClick = { viewModel.clearVariants() }) {
                            Icon(Icons.Default.Tune, null, tint = TravelingDeepPurple, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Ajuster", color = TravelingDeepPurple)
                        }
                    }
                    Spacer(Modifier.height(8.dp))

                    uiState.goodWeatherDays?.let { days ->
                        WeatherDaysPanel(days = days)
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        items(uiState.variants ?: emptyList()) { variant ->
                            VariantCard(
                                variant = variant,
                                isSaving = uiState.isSaving,
                                onChoose = {
                                    variantToSave = variant
                                    saveNameInput = if (pathName.isNotBlank()) pathName else variant.name
                                    showSaveDialog = true
                                },
                                onStepDetails = { step ->
                                    viewModel.loadStepDetails(step.name, step.lat, step.lon)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // ── Bottom sheet détails d'une étape ──────────────────────────────
    if (uiState.isLoadingStepDetail || uiState.stepDetail != null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.clearStepDetail() },
            containerColor = MaterialTheme.colorScheme.background,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            if (uiState.isLoadingStepDetail) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TravelingDeepPurple)
                }
            } else {
                uiState.stepDetail?.let { detail ->
                    StepDetailSheet(detail = detail, onDismiss = { viewModel.clearStepDetail() })
                }
            }
        }
    }

    // ── Dialog confirmation nom ────────────────────────────────────────
    if (showSaveDialog && variantToSave != null) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Nommer l'itinéraire") },
            text = {
                OutlinedTextField(
                    value = saveNameInput,
                    onValueChange = { saveNameInput = it },
                    placeholder = { Text("Nom de l'itinéraire") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSaveDialog = false
                        val name = saveNameInput.ifBlank { variantToSave!!.name }
                        viewModel.saveItinerary(name, variantToSave!!, selectedMode)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TravelingDeepPurple)
                ) { Text("Sauvegarder") }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("Annuler") }
            }
        )
    }
}

@Composable
private fun VariantCard(
    variant: ItineraryVariant,
    isSaving: Boolean,
    onChoose: () -> Unit,
    onStepDetails: (ItineraryStep) -> Unit
) {
    var expandedSteps by remember { mutableStateOf(false) }

    // Collect all available photos across steps (Wikipedia first, then community)
    val allPhotos = remember(variant) {
        variant.steps.flatMap { step ->
            listOfNotNull(step.photoUrl) + step.postPhotoUrls
        }.distinct().take(6)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            // ── Bande de photos illustrant l'itinéraire ──────────────────
            if (allPhotos.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    items(allPhotos) { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            modifier = Modifier
                                .height(130.dp)
                                .width(if (allPhotos.size == 1) 600.dp else 160.dp)
                                .clip(RoundedCornerShape(0.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
            // Header: name + duration/distance chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(variant.name, style = MaterialTheme.typography.titleLarge, color = TravelingDeepPurple, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    InfoChip(Icons.Default.Schedule, "${variant.estimatedDuration} min")
                    InfoChip(Icons.Default.Straighten, "${(variant.estimatedDistance / 1000f).let { if (it < 1f) "${variant.estimatedDistance} m" else "${"%.1f".format(it)} km" }}")
                }
            }

            Text(variant.description, color = Color.Gray, fontSize = 13.sp, modifier = Modifier.padding(vertical = 6.dp))

            // Metrics row
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Budget
                val allCostsReal = variant.steps.all { it.costIsReal }
                if (variant.estimatedBudget > 0) {
                    MetricChip(
                        icon = Icons.Default.Euro,
                        label = "${if (allCostsReal) "" else "~"}${variant.estimatedBudget} €",
                        color = Color(0xFF4CAF50)
                    )
                } else {
                    MetricChip(icon = Icons.Default.Euro, label = "Gratuit", color = Color(0xFF4CAF50))
                }
                // Effort
                MetricChip(
                    icon = Icons.Default.FitnessCenter,
                    label = "Effort ${"★".repeat(variant.effortScore)}${"☆".repeat(5 - variant.effortScore)}",
                    color = when (variant.effortScore) {
                        1, 2 -> Color(0xFF2196F3)
                        3 -> Color(0xFFFF9800)
                        else -> Color(0xFFF44336)
                    }
                )
                // Slot
                variant.suggestedSlot?.let { slot ->
                    MetricChip(
                        icon = when (slot) {
                            "matin" -> Icons.Default.WbTwilight
                            "soir" -> Icons.Default.Nightlight
                            else -> Icons.Default.WbSunny
                        },
                        label = slot.replaceFirstChar { it.uppercase() },
                        color = TravelingDeepPurple
                    )
                }
            }

            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth().clickable { expandedSteps = !expandedSteps },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${variant.steps.size} étape(s)", fontWeight = FontWeight.Medium, fontSize = 13.sp, color = TravelingDeepPurple)
                Icon(
                    if (expandedSteps) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null,
                    tint = TravelingDeepPurple,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))

            val stepsToShow = if (expandedSteps) variant.steps else variant.steps.take(4)
            stepsToShow.forEachIndexed { index, step ->
                StepRow(index = index + 1, step = step, onDetailsClick = { onStepDetails(step) })
            }
            if (!expandedSteps && variant.steps.size > 4) {
                Text("+ ${variant.steps.size - 4} étape(s)…", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onChoose,
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = TravelingDeepPurple),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Choisir cet itinéraire", fontWeight = FontWeight.Bold)
                }
            }
            } // inner Column (padding)
        } // outer Column
    } // Card
}

@Composable
private fun StepRow(index: Int, step: ItineraryStep, onDetailsClick: () -> Unit) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = TravelingDeepPurple,
                modifier = Modifier.size(20.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(index.toString(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            if (!step.photoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = step.photoUrl,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(step.name, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                step.type?.let { Text(it, fontSize = 11.sp, color = Color.Gray) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    step.openingHours?.let {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(11.dp))
                            Spacer(Modifier.width(3.dp))
                            Text(it, fontSize = 10.sp, color = Color(0xFF4CAF50))
                        }
                    }
                    if (step.avgCost == 0) {
                        Text("Gratuit", fontSize = 10.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Medium)
                    } else if (step.avgCost > 0) {
                        val prefix = if (step.costIsReal) "" else "~"
                        Text("${prefix}${step.avgCost} €", fontSize = 10.sp,
                            color = if (step.costIsReal) Color(0xFF1565C0) else Color.Gray,
                            fontWeight = if (step.costIsReal) FontWeight.Medium else FontWeight.Normal
                        )
                    }
                }
            }
            TextButton(
                onClick = onDetailsClick,
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
            ) {
                Icon(Icons.Default.Info, null, tint = TravelingDeepPurple, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(3.dp))
                Text("Détails", fontSize = 11.sp, color = TravelingDeepPurple)
            }
        }
        // Photos de la communauté
        if (step.postPhotoUrls.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.padding(start = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.People, null, tint = Color.Gray, modifier = Modifier.size(10.dp))
                Spacer(Modifier.width(2.dp))
                step.postPhotoUrls.take(4).forEach { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(6.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoChip(icon: ImageVector, label: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = TravelingDeepPurple.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = TravelingDeepPurple, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, fontSize = 12.sp, color = TravelingDeepPurple, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun MetricChip(icon: ImageVector, label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text(label, fontSize = 11.sp, color = color, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun ModeButton(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) TravelingDeepPurple else TravelingDeepPurple.copy(alpha = 0.08f),
        modifier = modifier.height(56.dp).clickable { onClick() }
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Icon(icon, null, tint = if (isSelected) Color.White else TravelingDeepPurple)
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, color = if (isSelected) Color.White else TravelingDeepPurple, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun ActivityToggle(
    name: String,
    icon: ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) TravelingDeepPurple.copy(alpha = 0.15f) else Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, if (isSelected) TravelingDeepPurple else Color.LightGray),
        modifier = modifier.height(80.dp).clickable { onClick() }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(4.dp)
        ) {
            Icon(icon, null, modifier = Modifier.size(26.dp), tint = if (isSelected) TravelingDeepPurple else Color.Gray)
            Spacer(modifier = Modifier.height(4.dp))
            Text(name, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                color = if (isSelected) TravelingDeepPurple else Color.Gray)
        }
    }
}

@Composable
private fun EffortChip(
    label: String,
    emoji: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) TravelingDeepPurple else TravelingDeepPurple.copy(alpha = 0.07f),
        modifier = modifier.height(64.dp).clickable { onClick() }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(emoji, fontSize = 20.sp)
            Spacer(Modifier.height(2.dp))
            Text(label, fontSize = 11.sp, color = if (isSelected) Color.White else TravelingDeepPurple, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SensitivityChip(
    label: String,
    emoji: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Color(0xFF1565C0).copy(alpha = 0.12f) else Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) Color(0xFF1565C0) else Color.LightGray),
        modifier = modifier.height(56.dp).clickable { onClick() }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(emoji, fontSize = 16.sp)
            Text(label, fontSize = 10.sp, color = if (isSelected) Color(0xFF1565C0) else Color.Gray, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TimeSlotChip(
    label: String,
    emoji: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) TravelingDeepPurple else TravelingDeepPurple.copy(alpha = 0.07f),
        modifier = modifier.height(56.dp).clickable { onClick() }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(emoji, fontSize = 16.sp)
            Text(label, fontSize = 10.sp, color = if (isSelected) Color.White else TravelingDeepPurple, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}

private fun Set<String>.toggle(item: String): Set<String> =
    if (contains(item)) minus(item) else plus(item)

@Composable
private fun StepDetailSheet(
    detail: StepDetailResponse,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                detail.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TravelingDeepPurple,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, null, tint = Color.Gray)
            }
        }

        // Photo Wikipedia en bannière
        if (!detail.wikiPhotoUrl.isNullOrBlank()) {
            Spacer(Modifier.height(12.dp))
            AsyncImage(
                model = detail.wikiPhotoUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
        }

        // Résumé Wikipedia
        if (!detail.wikiSummary.isNullOrBlank()) {
            Spacer(Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = TravelingDeepPurple.copy(alpha = 0.06f)
            ) {
                Row(modifier = Modifier.padding(10.dp)) {
                    Icon(
                        Icons.Default.Info, null,
                        tint = TravelingDeepPurple,
                        modifier = Modifier.size(16.dp).padding(top = 2.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(detail.wikiSummary, fontSize = 13.sp, color = Color.DarkGray, lineHeight = 19.sp)
                }
            }
        }

        // Posts de la communauté
        if (detail.communityPosts.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.People, null, tint = TravelingDeepPurple, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "Photos de la communauté (${detail.communityPosts.size})",
                    fontWeight = FontWeight.Bold,
                    color = TravelingDeepPurple,
                    fontSize = 14.sp
                )
            }
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(detail.communityPosts) { post ->
                    Column(modifier = Modifier.width(140.dp)) {
                        AsyncImage(
                            model = post.photoUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(140.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop
                        )
                        post.authorPseudo?.let {
                            Text(
                                "@$it",
                                fontSize = 10.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        post.description?.let {
                            Text(
                                it,
                                fontSize = 11.sp,
                                color = Color.DarkGray,
                                maxLines = 2,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        } else {
            Spacer(Modifier.height(12.dp))
            Text(
                "Aucun post de la communauté pour ce lieu.",
                fontSize = 13.sp,
                color = Color.Gray,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun WeatherDaysPanel(days: List<WeatherDay>) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFFFF8E1),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFC107))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("☀️", fontSize = 18.sp)
                Spacer(Modifier.width(6.dp))
                Text(
                    "Meilleurs jours pour votre sortie",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF795548),
                    fontSize = 14.sp
                )
            }
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(days) { day ->
                    WeatherDayChip(day = day)
                }
            }
        }
    }
}

@Composable
private fun WeatherDayChip(day: WeatherDay) {
    val bgColor = when {
        day.isGood -> Color(0xFFE8F5E9)
        day.condition.contains("Pluie") || day.condition.contains("Orage") || day.condition.contains("Averse") -> Color(0xFFFFEBEE)
        else -> Color(0xFFF5F5F5)
    }
    val borderColor = when {
        day.isGood -> Color(0xFF4CAF50)
        day.condition.contains("Pluie") || day.condition.contains("Orage") || day.condition.contains("Averse") -> Color(0xFFEF5350)
        else -> Color.LightGray
    }
    val emoji = when {
        day.condition == "Ensoleillé" -> "☀️"
        day.condition == "Peu nuageux" -> "🌤️"
        day.condition == "Nuageux" -> "☁️"
        day.condition == "Brouillard" -> "🌫️"
        day.condition.contains("Bruine") -> "🌦️"
        day.condition.contains("Pluie") -> "🌧️"
        day.condition.contains("Neige") -> "❄️"
        day.condition.contains("Averse") -> "🌦️"
        day.condition.contains("Orage") -> "⛈️"
        else -> "🌡️"
    }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 20.sp)
            Spacer(Modifier.height(2.dp))
            Text(day.label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
            Text(day.condition, fontSize = 10.sp, color = Color.Gray)
        }
    }
}
