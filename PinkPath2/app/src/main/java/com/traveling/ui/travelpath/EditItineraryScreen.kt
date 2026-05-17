package com.traveling.ui.travelpath

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.traveling.domain.model.ItineraryStepFull
import com.traveling.ui.theme.TravelingDeepPurple
import com.traveling.ui.travelshare.AuthViewModel
import com.traveling.ui.travelshare.PostViewModel

@Composable
fun EditItineraryScreen(
    onBack: () -> Unit,
    viewModel: ItineraryViewModel = hiltViewModel(),
    postViewModel: PostViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val shareSuccess by postViewModel.shareSuccess.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
    val userGroups by postViewModel.groups.collectAsState()
    val editItinerary = uiState.editItinerary
    val context = LocalContext.current

    var steps by remember(editItinerary?.id) {
        mutableStateOf(editItinerary?.steps ?: emptyList())
    }
    var locationQuery by remember { mutableStateOf("") }
    var showShareDialog by remember { mutableStateOf(false) }
    var shareDescription by remember { mutableStateOf("") }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            viewModel.clearSaveSuccess()
            onBack()
        }
    }

    LaunchedEffect(shareSuccess) {
        if (shareSuccess) postViewModel.clearShareSuccess()
    }

    if (uiState.isLoadingItinerary && editItinerary == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = TravelingDeepPurple)
        }
        return
    }

    if (editItinerary == null) {
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, null, tint = TravelingDeepPurple)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Infos & modification",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TravelingDeepPurple,
                        fontWeight = FontWeight.Bold
                    )
                    Text(editItinerary.name, color = Color.Gray, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Actions rapides : Partager + Télécharger PDF ───────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        if (isLoggedIn) {
                            shareDescription = ""
                            showShareDialog = true
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    enabled = isLoggedIn,
                    border = androidx.compose.foundation.BorderStroke(1.dp, TravelingDeepPurple)
                ) {
                    Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp), tint = TravelingDeepPurple)
                    Spacer(Modifier.width(6.dp))
                    Text("Partager", color = TravelingDeepPurple, fontSize = 13.sp)
                }
                Button(
                    onClick = { viewModel.downloadPdf(editItinerary.id, editItinerary.name, context) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = TravelingDeepPurple),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Télécharger", fontSize = 13.sp)
                }
            }

            if (shareSuccess) {
                Spacer(Modifier.height(6.dp))
                Text("Itinéraire partagé avec succès !", color = Color(0xFF388E3C), fontSize = 13.sp)
            }
            if (!isLoggedIn) {
                Spacer(Modifier.height(4.dp))
                Text("Connectez-vous pour partager", color = Color.Gray, fontSize = 11.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Étapes", fontWeight = FontWeight.Bold, color = TravelingDeepPurple, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                itemsIndexed(steps) { index, step ->
                    EditStepRow(
                        step = step,
                        index = index,
                        isFirst = index == 0,
                        isLast = index == steps.size - 1,
                        onMoveUp = {
                            val list = steps.toMutableList()
                            val tmp = list[index]; list[index] = list[index - 1]; list[index - 1] = tmp
                            steps = list
                        },
                        onMoveDown = {
                            val list = steps.toMutableList()
                            val tmp = list[index]; list[index] = list[index + 1]; list[index + 1] = tmp
                            steps = list
                        },
                        onRemove = {
                            steps = steps.toMutableList().also { it.removeAt(index) }
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Ajouter un lieu", fontWeight = FontWeight.Bold, color = TravelingDeepPurple, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = locationQuery,
                        onValueChange = {
                            locationQuery = it
                            viewModel.searchLocation(it)
                        },
                        placeholder = { Text("Rechercher un lieu…") },
                        modifier = Modifier.fillMaxWidth(),
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

                if (uiState.searchResults.isNotEmpty()) {
                    item {
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
                                            val newStep = ItineraryStepFull(
                                                id = 0,
                                                order = steps.size,
                                                name = feature.properties.name ?: "Lieu",
                                                type = null,
                                                latitude = feature.geometry.latitude,
                                                longitude = feature.geometry.longitude
                                            )
                                            steps = steps + newStep
                                            locationQuery = ""
                                            viewModel.clearSearchResults()
                                        }
                                    )
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }

        // Bouton sauvegarder (flottant en bas)
        Button(
            onClick = { viewModel.updateItinerary(editItinerary.id, editItinerary.name, steps) },
            enabled = steps.isNotEmpty() && !uiState.isSaving,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(0.85f)
                .padding(bottom = 24.dp)
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TravelingDeepPurple),
            shape = RoundedCornerShape(14.dp)
        ) {
            if (uiState.isSaving) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Sauvegarder", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }

    // ── Dialog de partage ───────────────────────────────────────────────
    if (showShareDialog) {
        val selectedGroupIds = remember { mutableStateListOf<Int>() }
        var isPublic by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showShareDialog = false },
            title = { Text("Partager l'itinéraire") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Où souhaitez-vous partager « ${editItinerary.name} » ?",
                        color = Color.Gray, fontSize = 13.sp
                    )
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isPublic, onCheckedChange = { isPublic = it })
                        Text("🌍 Public")
                    }

                    if (userGroups.isNotEmpty()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text("Mes Groupes :", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        userGroups.forEach { group ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = selectedGroupIds.contains(group.id),
                                    onCheckedChange = { checked ->
                                        if (checked) selectedGroupIds.add(group.id)
                                        else selectedGroupIds.remove(group.id)
                                    }
                                )
                                Text("👥 ${group.name}")
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = shareDescription,
                        onValueChange = { shareDescription = it },
                        placeholder = { Text("Ajouter une description (optionnel)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val userId = currentUser?.id?.toIntOrNull() ?: return@Button
                        
                        if (isPublic) {
                            postViewModel.shareItinerary(editItinerary.id, userId, shareDescription, true, null)
                        }
                        
                        selectedGroupIds.forEach { groupId ->
                            postViewModel.shareItinerary(editItinerary.id, userId, shareDescription, false, groupId)
                        }
                        
                        showShareDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TravelingDeepPurple),
                    enabled = isPublic || selectedGroupIds.isNotEmpty()
                ) {
                    Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Partager")
                }
            },
            dismissButton = {
                TextButton(onClick = { showShareDialog = false }) { Text("Annuler") }
            }
        )
    }
}

@Composable
private fun EditStepRow(
    step: ItineraryStepFull,
    index: Int,
    isFirst: Boolean,
    isLast: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(TravelingDeepPurple.copy(alpha = 0.06f), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = TravelingDeepPurple,
            modifier = Modifier.size(26.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text((index + 1).toString(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(step.name, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            step.type?.let { Text(it, fontSize = 11.sp, color = Color.Gray) }
        }

        IconButton(onClick = onMoveUp, enabled = !isFirst, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Default.KeyboardArrowUp, null,
                tint = if (!isFirst) TravelingDeepPurple else Color.LightGray,
                modifier = Modifier.size(20.dp)
            )
        }
        IconButton(onClick = onMoveDown, enabled = !isLast, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Default.KeyboardArrowDown, null,
                tint = if (!isLast) TravelingDeepPurple else Color.LightGray,
                modifier = Modifier.size(20.dp)
            )
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Delete, null, tint = Color(0xFFE53935), modifier = Modifier.size(18.dp))
        }
    }
}
