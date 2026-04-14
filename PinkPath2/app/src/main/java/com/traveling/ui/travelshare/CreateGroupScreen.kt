package com.traveling.ui.travelshare

import android.net.Uri
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.traveling.ui.theme.TravelingDeepPurple
import com.traveling.util.uriToFile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(
    onBack: () -> Unit,
    viewModel: PostViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uploadSuccess by viewModel.uploadSuccess.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var groupName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isPrivate by remember { mutableStateOf(false) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> imageUri = uri }

    LaunchedEffect(uploadSuccess) {
        if (uploadSuccess) {
            Toast.makeText(context, "Groupe créé avec succès !", Toast.LENGTH_SHORT).show()
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Créer un groupe") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Sélection d'Image
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = TravelingDeepPurple.copy(alpha = 0.1f),
                modifier = Modifier
                    .size(150.dp)
                    .clickable { imageLauncher.launch("image/*") }
            ) {
                if (imageUri != null) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(24.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = TravelingDeepPurple, modifier = Modifier.size(40.dp))
                        Text("Photo du groupe", fontSize = 12.sp, color = TravelingDeepPurple)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Nom du groupe
            OutlinedTextField(
                value = groupName,
                onValueChange = { groupName = it },
                label = { Text("Nom du groupe") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Slider / Switch pour Privé
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (isPrivate) "Groupe Privé" else "Groupe Public",
                        fontWeight = FontWeight.Bold,
                        color = TravelingDeepPurple
                    )
                    Text(
                        text = if (isPrivate) "Uniquement sur invitation" else "Visible par tous",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                Switch(
                    checked = isPrivate,
                    onCheckedChange = { isPrivate = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = TravelingDeepPurple)
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            if (isLoading) {
                CircularProgressIndicator(color = TravelingDeepPurple)
            } else {
                Button(
                    onClick = {
                        if (groupName.isBlank()) {
                            Toast.makeText(context, "Le nom du groupe est obligatoire", Toast.LENGTH_SHORT).show()
                        } else {
                            val imageFile = imageUri?.let { uriToFile(context, it) }
                            viewModel.createGroup(
                                name = groupName,
                                description = description,
                                isPrivate = isPrivate,
                                image = imageFile
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TravelingDeepPurple)
                ) {
                    Text("Créer le groupe", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
