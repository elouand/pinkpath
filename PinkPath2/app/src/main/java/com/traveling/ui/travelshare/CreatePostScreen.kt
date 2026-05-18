package com.traveling.ui.travelshare

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.traveling.data.remote.PhotonFeature
import com.traveling.data.remote.PhotonGeometry
import com.traveling.data.remote.PhotonProperties
import com.traveling.ui.theme.TravelingDeepPurple
import com.traveling.util.uriToFile
import kotlinx.coroutines.delay
import java.io.File
import androidx.compose.foundation.BorderStroke

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreatePostScreen(
    onBack: () -> Unit,
    initialLocation: String? = null,
    initialLat: Double? = null,
    initialLon: Double? = null,
    viewModel: PostViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uploadSuccess by viewModel.uploadSuccess.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    val userGroups by viewModel.groups.collectAsState()
    val suggestedTags by viewModel.suggestedTags.collectAsState()
    val isSuggestingTags by viewModel.isSuggestingTags.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()

    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var description by remember { mutableStateOf("") }
    var tagInput by remember { mutableStateOf("") }
    val selectedTags = remember { mutableStateListOf<String>() }

    var selectedGroup by remember { mutableStateOf<com.traveling.domain.model.Group?>(null) }
    var groupDropdownExpanded by remember { mutableStateOf(false) }

    var locationQuery by remember { mutableStateOf(initialLocation ?: "") }
    var suggestions by remember { mutableStateOf<List<PhotonFeature>>(emptyList()) }
    var selectedLocation by remember { 
        mutableStateOf<PhotonFeature?>(
            if (initialLocation != null && initialLat != null && initialLon != null) {
                PhotonFeature(
                    geometry = PhotonGeometry(listOf(initialLon, initialLat)),
                    properties = PhotonProperties(name = initialLocation, country = "", city = "")
                )
            } else null
        ) 
    }

    // Audio state
    var isRecording by remember { mutableStateOf(false) }
    var audioFile by remember { mutableStateOf<File?>(null) }
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }

    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> imageUri = uri }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(context, "Permission micro refusée", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadUserGroups()
    }

    LaunchedEffect(uploadSuccess) {
        if (uploadSuccess) {
            Toast.makeText(context, "Post publié !", Toast.LENGTH_SHORT).show()
            viewModel.resetUploadStatus()
            onBack()
        }
    }

    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    LaunchedEffect(locationQuery) {
        if (locationQuery.length > 2 && selectedLocation?.properties?.name != locationQuery) {
            selectedLocation = null
            delay(500)
            val result = viewModel.searchLocation(locationQuery)
            result.getOrNull()?.let { list ->
                suggestions = list.filter { !it.properties.name.isNullOrBlank() }
            }
        } else if (locationQuery.length <= 2) {
            suggestions = emptyList()
            selectedLocation = null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                mediaRecorder?.stop()
                mediaRecorder?.release()
            } catch (e: Exception) {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Ajouter un post",
            style = MaterialTheme.typography.displayMedium,
            color = TravelingDeepPurple,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        // Image selection
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = TravelingDeepPurple.copy(alpha = 0.1f),
            modifier = Modifier
                .size(200.dp)
                .clickable { imageLauncher.launch("image/*") }
        ) {
            if (imageUri != null) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(32.dp)),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = null,
                    tint = TravelingDeepPurple,
                    modifier = Modifier
                        .size(60.dp)
                        .padding(60.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Audio controls
        CreateOptionBox(
            title = if (isRecording) "Enregistrement..." else if (audioFile != null) "Changer audio" else "Ajouter note audio",
            icon = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
            color = if (isRecording) Color.Red else TravelingDeepPurple,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (isRecording) {
                        try {
                            mediaRecorder?.apply {
                                stop()
                                release()
                            }
                            mediaRecorder = null
                            isRecording = false
                        } catch (e: Exception) {
                            mediaRecorder = null
                            isRecording = false
                        }
                    } else {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else {
                            try {
                                val file = File(context.cacheDir, "post_audio_${System.currentTimeMillis()}.m4a")
                                val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    MediaRecorder(context)
                                } else {
                                    MediaRecorder()
                                }
                                recorder.apply {
                                    setAudioSource(MediaRecorder.AudioSource.MIC)
                                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                                    setOutputFile(file.absolutePath)
                                    prepare()
                                    start()
                                }
                                audioFile = file
                                mediaRecorder = recorder
                                isRecording = true
                            } catch (e: Exception) {
                                Toast.makeText(context, "Impossible d'utiliser le micro", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Group selection
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { groupDropdownExpanded = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (selectedGroup == null) "🌍 Destination : Public" else "👥 Groupe : ${selectedGroup!!.name}")
                Icon(Icons.Default.ArrowDropDown, null)
            }
            DropdownMenu(
                expanded = groupDropdownExpanded,
                onDismissRequest = { groupDropdownExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("🌍 Public") },
                    onClick = {
                        selectedGroup = null
                        groupDropdownExpanded = false
                    }
                )
                userGroups.forEach { group ->
                    DropdownMenuItem(
                        text = { Text("👥 ${group.name}") },
                        onClick = {
                            selectedGroup = group
                            groupDropdownExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Location
        OutlinedTextField(
            value = locationQuery,
            onValueChange = { locationQuery = it },
            label = { Text("Lieu") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        if (suggestions.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                suggestions.forEach { feature ->
                    ListItem(
                        headlineContent = { Text(feature.properties.name ?: "") },
                        supportingContent = { Text(feature.properties.displayName) },
                        modifier = Modifier.clickable {
                            selectedLocation = feature
                            locationQuery = feature.properties.name ?: ""
                            suggestions = emptyList()
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tags Preview
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            selectedTags.forEach { tag ->
                InputChip(
                    selected = true,
                    onClick = { selectedTags.remove(tag) },
                    label = { Text(tag) },
                    trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(16.dp)) }
                )
            }
        }

        // Tags Input
        OutlinedTextField(
            value = tagInput,
            onValueChange = { tagInput = it },
            label = { Text("Ajouter des tags") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            trailingIcon = {
                IconButton(onClick = {
                    val trimmed = tagInput.trim().lowercase()
                    if (trimmed.isNotBlank() && !selectedTags.contains(trimmed)) {
                        selectedTags.add(trimmed)
                        tagInput = ""
                    }
                }) {
                    Icon(Icons.Default.Add, null)
                }
            }
        )

        // Bouton suggestion IA (visible dès qu'un lieu est sélectionné dans la liste)
        if (selectedLocation != null) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    val props = selectedLocation!!.properties
                    val place = listOfNotNull(props.name, props.street, props.city, props.country)
                        .distinct()
                        .filter { it.isNotBlank() }
                        .joinToString(", ")
                    if (place.isNotBlank()) viewModel.suggestTagsFromPlace(place, imageUri)
                },
                enabled = !isSuggestingTags,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, TravelingDeepPurple.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSuggestingTags) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = TravelingDeepPurple)
                    Spacer(Modifier.width(8.dp))
                    Text("Analyse en cours...", color = TravelingDeepPurple)
                } else {
                    Icon(Icons.Default.Lightbulb, null, tint = TravelingDeepPurple, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Suggérer des tags par IA", color = TravelingDeepPurple)
                }
            }
        }

        // Tags suggérés par IA
        if (suggestedTags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = TravelingDeepPurple.copy(alpha = 0.06f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Tags suggérés — appuie pour ajouter :", fontSize = 12.sp, color = Color.Gray)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        suggestedTags.forEach { tag ->
                            val alreadyAdded = selectedTags.contains(tag)
                            FilterChip(
                                selected = alreadyAdded,
                                onClick = {
                                    if (alreadyAdded) selectedTags.remove(tag)
                                    else selectedTags.add(tag)
                                },
                                label = { Text(tag, fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(
                                        if (alreadyAdded) Icons.Default.Check else Icons.Default.Add,
                                        null, modifier = Modifier.size(14.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TravelingDeepPurple,
                                    selectedLabelColor = Color.White,
                                    selectedLeadingIconColor = Color.White
                                )
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Description
        TextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (isLoading) {
            CircularProgressIndicator(color = TravelingDeepPurple)
        } else {
            Button(
                onClick = {
                    if (imageUri != null && selectedLocation != null) {
                        try {
                            val file = uriToFile(context, imageUri!!)
                            viewModel.uploadPost(
                                image = file,
                                audio = audioFile,
                                description = description,
                                typeLieu = selectedLocation!!.properties.name ?: "Inconnu",
                                latitude = selectedLocation!!.geometry.latitude,
                                longitude = selectedLocation!!.geometry.longitude,
                                isPublic = selectedGroup == null,
                                authorId = currentUser?.id,
                                groupId = selectedGroup?.id,
                                tags = selectedTags.joinToString(",")
                            )
                        } catch (e: Exception) {
                            Toast.makeText(context, "Erreur fichier", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Image et lieu requis", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TravelingDeepPurple)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, null)
                Spacer(Modifier.width(8.dp))
                Text("Publier", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CreateOptionBox(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Text(title, color = color, fontWeight = FontWeight.Medium)
        }
    }
}
