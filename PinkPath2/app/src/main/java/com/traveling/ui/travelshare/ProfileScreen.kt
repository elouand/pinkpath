package com.traveling.ui.travelshare

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.traveling.ui.theme.TravelingDeepPurple
import com.traveling.util.uriToFile

@Composable
fun ProfileScreen(
    viewModel: AuthViewModel,
    postViewModel: PostViewModel,
    onLoginClick: () -> Unit,
    onSignupClick: () -> Unit,
    onNotificationsClick: () -> Unit = {},
    onAdminClick: () -> Unit = {}
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val unreadCount by postViewModel.unreadNotificationCount.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(currentUser) {
        if (currentUser != null) postViewModel.loadNotifications()
    }

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val file = uriToFile(context, it)
            viewModel.updateProfilePicture(file)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Icons row: Notifications + Settings
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            if (currentUser != null) {
                BadgedBox(
                    badge = {
                        if (unreadCount > 0) {
                            Badge { Text(if (unreadCount > 9) "9+" else unreadCount.toString()) }
                        }
                    }
                ) {
                    IconButton(onClick = onNotificationsClick) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = TravelingDeepPurple,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
            IconButton(onClick = { /* TODO */ }) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = TravelingDeepPurple,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        if (currentUser == null) {
            // Not logged in view
            Text(
                text = "Vous n’êtes pas connecté",
                style = MaterialTheme.typography.displayMedium,
                color = TravelingDeepPurple,
                textAlign = TextAlign.Center,
                fontSize = 40.sp,
                lineHeight = 48.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(80.dp))

            Button(
                onClick = onLoginClick,
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(80.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TravelingDeepPurple),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "Se connecter", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = onSignupClick,
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(80.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TravelingDeepPurple),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "Créer un compte", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            // Logged in view
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color.Gray)
                    .clickable { imageLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (currentUser?.profileUrl != null) {
                    AsyncImage(
                        model = currentUser?.profileUrl,
                        contentDescription = "Profile Picture",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(60.dp),
                        tint = Color.White
                    )
                }
                
                // Overlay "Edit" hint
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Changer la photo",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(24.dp)
                    )
                }

                if (isLoading) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val displayName = currentUser?.pseudo ?: currentUser?.username ?: "Utilisateur"
            Text(
                text = "Bonjour, $displayName !",
                style = MaterialTheme.typography.displayMedium,
                color = TravelingDeepPurple,
                fontSize = 32.sp,
                textAlign = TextAlign.Center
            )

            if (currentUser?.email != null) {
                Text(
                    text = currentUser?.email!!,
                    color = Color.Gray,
                    fontSize = 16.sp
                )
            }
            
            Spacer(modifier = Modifier.height(40.dp))

            if (currentUser?.isAdmin == true) {
                Button(
                    onClick = onAdminClick,
                    colors = ButtonDefaults.buttonColors(containerColor = TravelingDeepPurple),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(0.6f)
                ) {
                    Icon(Icons.Default.AdminPanelSettings, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Admin", color = Color.White)
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Button(
                onClick = { viewModel.logout() },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.7f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Se déconnecter", color = Color.White)
            }
        }
    }
}
