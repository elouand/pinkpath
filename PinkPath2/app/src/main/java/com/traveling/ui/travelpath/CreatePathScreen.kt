package com.traveling.ui.travelpath

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.traveling.domain.model.ItineraryStep
import com.traveling.domain.model.ItineraryVariant
import com.traveling.domain.model.LocationPoint
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

    var pathName by remember { mutableStateOf("") }
    var locationQuery by remember { mutableStateOf("") }
    var locations by remember { mutableStateOf<List<LocationPoint>>(emptyList()) }
    var durationMinutes by remember { mutableStateOf(60f) }
    var selectedMode by remember { mutableStateOf("walking") }
    var selectedActivities by remember { mutableStateOf(setOf("Restauration", "Culture", "Loisirs")) }
    var wantsGoodWeather by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var variantToSave by remember { mutableStateOf<ItineraryVariant?>(null) }
    var saveNameInput by remember { mutableStateOf("") }

    // Navigate back after successful save
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
            }

            // Suggestions
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

            // Lieux sélectionnés
            if (locations.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                    locations.forEachIndexed { index, loc ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(TravelingDeepPurple.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Stars, null, tint = TravelingDeepPurple, modifier = Modifier.size(16.dp))
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
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
                    Text(
                        text = if (wantsGoodWeather) "☀️" else "🌤️",
                        fontSize = 28.sp
                    )
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

            Spacer(modifier = Modifier.height(24.dp))

            // ── Erreur ────────────────────────────────────────────────
            uiState.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
            }

            // ── Bouton Calculer ───────────────────────────────────────
            Button(
                onClick = {
                    viewModel.clearError()
                    if (locations.isEmpty()) {
                        return@Button
                    }
                    viewModel.generateItineraries(
                        locations = locations,
                        durationMinutes = durationMinutes.roundToInt(),
                        mode = selectedMode,
                        activities = selectedActivities.toList(),
                        wantsGoodWeather = wantsGoodWeather
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
                    Text(
                        "Choisissez votre itinéraire",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = TravelingDeepPurple,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Panneau météo (si beau temps demandé)
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
                                }
                            )
                        }
                    }
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
    onChoose: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(variant.name, style = MaterialTheme.typography.titleLarge, color = TravelingDeepPurple, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoChip(Icons.Default.Schedule, "${variant.estimatedDuration} min")
                    InfoChip(Icons.Default.Straighten, "${(variant.estimatedDistance / 1000f).let { if (it < 1f) "${variant.estimatedDistance} m" else "${"%.1f".format(it)} km" }}")
                }
            }

            Text(variant.description, color = Color.Gray, fontSize = 13.sp, modifier = Modifier.padding(vertical = 8.dp))

            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            Text("${variant.steps.size} étape(s)", fontWeight = FontWeight.Medium, fontSize = 13.sp, color = TravelingDeepPurple)
            Spacer(modifier = Modifier.height(4.dp))

            variant.steps.take(4).forEachIndexed { index, step ->
                StepRow(index = index + 1, step = step)
            }
            if (variant.steps.size > 4) {
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
        }
    }
}

@Composable
private fun StepRow(index: Int, step: ItineraryStep) {
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
        Column {
            Text(step.name, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            step.type?.let { Text(it, fontSize = 11.sp, color = Color.Gray) }
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
        modifier = modifier.height(90.dp).clickable { onClick() }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(4.dp)
        ) {
            Icon(icon, null, modifier = Modifier.size(32.dp), tint = if (isSelected) TravelingDeepPurple else Color.Gray)
            Spacer(modifier = Modifier.height(4.dp))
            Text(name, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                color = if (isSelected) TravelingDeepPurple else Color.Gray)
        }
    }
}

private fun Set<String>.toggle(item: String): Set<String> =
    if (contains(item)) minus(item) else plus(item)

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
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
