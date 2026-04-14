package com.traveling.ui.travelpath

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.traveling.ui.theme.TravelingDeepPurple
import com.traveling.ui.theme.TravelingTagYellow
import com.traveling.ui.travelshare.PostViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    groupId: Int,
    onBack: () -> Unit,
    viewModel: PostViewModel = hiltViewModel()
) {
    val groups by viewModel.groups.collectAsState()
    val group = groups.find { it.id == groupId }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(group?.name ?: "Détails du groupe") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { innerPadding ->
        if (group == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = TravelingDeepPurple)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    // Group Banner/Image
                    Surface(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(30.dp))
                            .background(TravelingDeepPurple.copy(alpha = 0.1f)),
                        color = Color.Transparent
                    ) {
                        if (group.imageUrl != null) {
                            AsyncImage(
                                model = group.imageUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Default.Group,
                                contentDescription = null,
                                tint = TravelingDeepPurple,
                                modifier = Modifier.padding(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = group.name,
                        style = MaterialTheme.typography.headlineMedium,
                        color = TravelingDeepPurple,
                        fontWeight = FontWeight.Bold
                    )

                    group.description?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Stats Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(label = "Membres", value = group.count?.users?.toString() ?: "0")
                        StatItem(label = "Posts", value = group.count?.photos?.toString() ?: "0")
                        StatItem(label = "Itinéraires", value = group.count?.paths?.toString() ?: "0")
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = "Membres",
                        style = MaterialTheme.typography.titleLarge,
                        color = TravelingDeepPurple,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }

                items(group.users ?: emptyList()) { user ->
                    ListItem(
                        headlineContent = { Text(user.pseudo ?: user.username ?: "Utilisateur") },
                        supportingContent = { Text("@${user.username ?: "inconnu"}") },
                        leadingContent = {
                            Surface(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.LightGray)
                            ) {
                                if (user.profileUrl != null) {
                                    AsyncImage(model = user.profileUrl, contentDescription = null, contentScale = ContentScale.Crop)
                                } else {
                                    Icon(Icons.Default.Person, null, tint = Color.Gray, modifier = Modifier.padding(8.dp))
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TravelingDeepPurple)
        Text(text = label, fontSize = 14.sp, color = Color.Gray)
    }
}
