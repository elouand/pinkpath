package com.traveling.ui.travelpath

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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Group
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
import com.traveling.ui.travelshare.AuthTextField
import com.traveling.ui.travelshare.PostViewModel
import com.traveling.util.uriToFile

@Composable
fun CreateGroupScreen(
    onBack: () -> Unit,
    viewModel: PostViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var groupName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isPrivate by remember { mutableStateOf(false) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    val isLoading by viewModel.isLoading.collectAsState()
    val uploadSuccess by viewModel.uploadSuccess.collectAsState()
    val error by viewModel.error.collectAsState()

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Créer un groupe",
            style = MaterialTheme.typography.displayMedium,
            color = TravelingDeepPurple,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Image Selection
        Surface(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(60.dp))
                .clickable { imageLauncher.launch("image/*") }
                .background(TravelingDeepPurple.copy(alpha = 0.1f)),
            color = Color.Transparent
        ) {
            if (imageUri != null) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Image du groupe",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = "Ajouter une image",
                        tint = TravelingDeepPurple,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        AuthTextField(
            value = groupName,
            onValueChange = { groupName = it },
            placeholder = "Nom du groupe"
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description du groupe") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Groupe privé",
                    style = MaterialTheme.typography.titleMedium,
                    color = TravelingDeepPurple,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isPrivate) "Seuls les membres peuvent voir" else "Tout le monde peut voir",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            Switch(
                checked = isPrivate,
                onCheckedChange = { isPrivate = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = TravelingDeepPurple,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color.Gray
                )
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        if (isLoading) {
            CircularProgressIndicator(color = TravelingDeepPurple)
        } else {
            Button(
                onClick = {
                    if (groupName.isNotBlank()) {
                        val file = imageUri?.let { uriToFile(context, it) }
                        viewModel.createGroup(
                            name = groupName,
                            description = description,
                            isPrivate = isPrivate,
                            image = file
                        )
                    } else {
                        Toast.makeText(context, "Le nom du groupe est requis", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TravelingDeepPurple),
                shape = RoundedCornerShape(28.dp)
            ) {
                Icon(Icons.Default.Group, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Créer le groupe",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        TextButton(onClick = onBack) {
            Text("Annuler", color = Color.Gray)
        }
    }
}
