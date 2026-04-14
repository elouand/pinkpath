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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.traveling.data.remote.PhotonFeature
import com.traveling.ui.theme.TravelingDeepPurple
import com.traveling.util.uriToFile
import kotlinx.coroutines.delay
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreatePostScreen(
    onBack: () -> Unit,
    viewModel: PostViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uploadSuccess by viewModel.uploadSuccess.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    val userGroups by viewModel.groups.collectAsState()

    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var description by remember { mutableStateOf("") }
    var tagInput by remember { mutableStateOf("") }
    val selectedTags = remember { mutableStateListOf<String>() }

    var selectedGroup by remember { mutableStateOf<com.traveling.domain.model.Group?>(null) }
    var groupDropdownExpanded by remember { mutableStateOf(false) }

    var locationQuery by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<PhotonFeature>>(emptyList()) }
    var selectedLocation by remember { mutableStateOf<PhotonFeature?>(null) }

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

    LaunchedEffect(locationQuery) {
        if (locationQuery.length > 2 && selectedLocation?.properties?.name != locationQuery) {
            delay(500)
            viewModel.searchLocation(locationQuery).onSuccess { list ->
                suggestions = list.filter { !it.properties.name.isNullOrBlank() }
            }
        } else if (locationQuery.length <= 2) suggestions = emptyList()
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
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Ajouter un post", style = MaterialTheme.typography.displayMedium, color = TravelingDeepPurple, modifier = Modifier.padding(vertical = 16.dp))

        // Image selection
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = TravelingDeepPurple.copy(alpha = 0.1f),
            modifier = Modifier.size(200.dp).clickable { imageLauncher.launch("image/*") }
        ) {
            if (imageUri != null) {
                AsyncImage(model = imageUri, contentDescription = null, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(32.dp)), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
            } else {
                Icon(Icons.Default.AddPhotoAlternate, null, tint = TravelingDeepPurple, modifier = Modifier.size(60.dp).padding(60.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Audio controls
        CreateOptionBox(
            title = if (isRecording) "Enregistrement..." else if (audioFile != null) "Changer audio" else "Ajouter note audio",
            icon = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
            color = if (isRecording) Color.Red else TravelingDeepPurple,
            modifier = Modifier.fillMaxWidth().clickable {
                if (isRecording) {
                    try {
                        mediaRecorder?.apply { stop(); release() }
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
                            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()
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
            OutlinedButton(onClick = { groupDropdownExpanded = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Text(if (selectedGroup == null) "🌍 Destination : Public" else "👥 Groupe : ${selectedGroup!!.name}")
                Icon(Icons.Default.ArrowDropDown, null)
            }
            DropdownMenu(expanded = groupDropdownExpanded, onDismissRequest = { groupDropdownExpanded = false }) {
                DropdownMenuItem(text = { Text("🌍 Public") }, onClick = { selectedGroup = null; groupDropdownExpanded = false })
                userGroups.forEach { group ->
                    DropdownMenuItem(text = { Text("👥 ${group.name}") }, onClick = { selectedGroup = group; groupDropdownExpanded = false })
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

        // Tags Preview (ABOVE the input)
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
                }) { Icon(Icons.Default.Add, null) }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Description
        TextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth().height(120.dp),
            colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
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
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TravelingDeepPurple)
            ) {
                Text("Publier", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.Send, null)
            }
        }
        Spacer(Modifier.height(32.dp))
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
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.1f),
        modifier = modifier.height(80.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(8.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = title, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}
