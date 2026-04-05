package com.traveling.ui.travelshare

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.util.Log
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
import java.io.FileOutputStream

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

    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var description by remember { mutableStateOf("") }
    
    // Photon Autocomplete State
    var locationQuery by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<PhotonFeature>>(emptyList()) }
    var selectedLocation by remember { mutableStateOf<PhotonFeature?>(null) }
    var isSearching by remember { mutableStateOf(false) }

    // Audio state
    var isRecording by remember { mutableStateOf(false) }
    var audioFile by remember { mutableStateOf<File?>(null) }
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }

    // Preview audio state
    var isPlaying by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(context, "Permission micro refusée", Toast.LENGTH_SHORT).show()
        }
    }

    // Debounce search
    LaunchedEffect(locationQuery) {
        if (locationQuery.length > 2 && selectedLocation?.properties?.name != locationQuery) {
            isSearching = true
            delay(500)
            viewModel.searchLocation(locationQuery).onSuccess {
                suggestions = it
            }
            isSearching = false
        } else {
            suggestions = emptyList()
        }
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

    DisposableEffect(Unit) {
        onDispose {
            try {
                mediaRecorder?.stop()
                mediaRecorder?.release()
                mediaPlayer?.stop()
                mediaPlayer?.release()
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

        // Add Image Box
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = TravelingDeepPurple,
            modifier = Modifier
                .size(200.dp)
                .padding(8.dp)
                .clickable { imageLauncher.launch("image/*") }
        ) {
            if (imageUri != null) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(32.dp)),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Ajouter une image",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(60.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Location Autocomplete Section
        Column(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = locationQuery,
                onValueChange = { 
                    locationQuery = it 
                    if (selectedLocation?.properties?.name != it) selectedLocation = null
                },
                label = { Text("Rechercher un lieu réel") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    if (isSearching) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    else if (selectedLocation != null) Icon(Icons.Default.CheckCircle, "Validé", tint = Color.Green)
                },
                shape = RoundedCornerShape(12.dp)
            )
            
            if (suggestions.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    tonalElevation = 4.dp,
                    shadowElevation = 8.dp
                ) {
                    Column {
                        suggestions.forEach { feature ->
                            ListItem(
                                headlineContent = { Text(feature.properties.name) },
                                supportingContent = { Text(feature.properties.displayName) },
                                leadingContent = { Icon(Icons.Default.Place, null) },
                                modifier = Modifier.clickable {
                                    selectedLocation = feature
                                    locationQuery = feature.properties.name
                                    suggestions = emptyList()
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Description Section
        TextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description / Note") },
            modifier = Modifier.fillMaxWidth().height(100.dp),
            colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Audio controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CreateOptionBox(
                title = if (isRecording) "Enregistrement..." else if (audioFile != null) "Changer audio" else "Ajouter note audio",
                icon = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                color = if (isRecording) Color.Red else TravelingDeepPurple,
                modifier = Modifier.weight(1f).clickable {
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
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Send Button
        if (isLoading) {
            CircularProgressIndicator(color = TravelingDeepPurple)
        } else {
            SendButton(
                text = "Envoyer en public",
                modifier = Modifier.fillMaxWidth().clickable {
                    if (imageUri == null) {
                        Toast.makeText(context, "L'image est obligatoire", Toast.LENGTH_SHORT).show()
                    } else if (selectedLocation == null) {
                        Toast.makeText(context, "Veuillez sélectionner un lieu réel", Toast.LENGTH_SHORT).show()
                    } else {
                        try {
                            val file = uriToFile(context, imageUri!!)
                            viewModel.uploadPost(
                                image = file,
                                audio = audioFile,
                                description = description,
                                typeLieu = selectedLocation!!.properties.name,
                                latitude = selectedLocation!!.geometry.latitude,
                                longitude = selectedLocation!!.geometry.longitude,
                                isPublic = true,
                                authorId = currentUser?.id
                            )
                        } catch (e: Exception) {
                            Toast.makeText(context, "Erreur fichier", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
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

@Composable
fun SendButton(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = TravelingDeepPurple,
        modifier = modifier.height(50.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(text = text, color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color.White)
        }
    }
}
