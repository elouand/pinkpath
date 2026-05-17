package com.traveling.ui.travelpath

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import com.traveling.ui.common.PostCard
import com.traveling.ui.theme.TravelingDeepPurple
import com.traveling.ui.theme.TravelingTagBlue
import com.traveling.ui.travelshare.AuthViewModel
import com.traveling.ui.travelshare.PostViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    groupId: Int,
    onBack: () -> Unit,
    onNavigateToGroupFeed: (String) -> Unit,
    onPostClick: (String) -> Unit,
    viewModel: PostViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val selectedGroup by viewModel.selectedGroup.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    val joinRequests by viewModel.joinRequests.collectAsState()
    
    var selectedTab by remember { mutableStateOf("Flux") }

    LaunchedEffect(groupId) {
        viewModel.loadGroupDetails(groupId)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(selectedGroup?.name ?: "Détails du groupe") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (isLoading && selectedGroup == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = TravelingDeepPurple)
            }
        } else if (selectedGroup == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Groupe introuvable", color = Color.Gray)
            }
        } else {
            val group = selectedGroup!!
            val userInGroup = group.users?.find { it.id == currentUser?.id }
            val isAdmin = userInGroup?.role == "ADMIN" || group.userRole == "ADMIN"
            val showRequestsTab = isAdmin && !group.isPublic

            val tabs = mutableListOf("Flux", "Membres")
            if (showRequestsTab) tabs.add("Demandes")

            Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                // Header
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier.size(100.dp).clip(RoundedCornerShape(24.dp)),
                        color = TravelingDeepPurple.copy(alpha = 0.1f)
                    ) {
                        if (group.imageUrl != null) {
                            AsyncImage(model = group.imageUrl, contentDescription = null, contentScale = ContentScale.Crop)
                        } else {
                            Icon(Icons.Default.Group, null, tint = TravelingDeepPurple, modifier = Modifier.padding(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = group.name, style = MaterialTheme.typography.headlineSmall, color = TravelingDeepPurple, fontWeight = FontWeight.Bold)
                    group.description?.let { Text(text = it, style = MaterialTheme.typography.bodyMedium, color = Color.Gray) }
                }

                TabRow(
                    selectedTabIndex = tabs.indexOf(selectedTab).coerceAtLeast(0),
                    containerColor = Color.Transparent,
                    contentColor = TravelingDeepPurple,
                    indicator = { tabPositions ->
                        val index = tabs.indexOf(selectedTab).coerceAtLeast(0)
                        if (index < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[index]),
                                color = TravelingDeepPurple
                            )
                        }
                    }
                ) {
                    tabs.forEach { tabName ->
                        Tab(
                            selected = selectedTab == tabName,
                            onClick = { selectedTab = tabName },
                            text = { Text(tabName) }
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (selectedTab) {
                        "Flux" -> {
                            val groupPosts = group.photos ?: emptyList()
                            if (groupPosts.isEmpty()) {
                                item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Text("Aucun post dans ce groupe", color = Color.Gray) } }
                            } else {
                                items(groupPosts) { post ->
                                    PostCard(
                                        title = post.content ?: post.title ?: "Sans titre",
                                        tags = post.tags?.map { it to TravelingTagBlue } ?: emptyList(),
                                        location = post.title ?: "Inconnu",
                                        author = post.authorName ?: "Anonyme",
                                        authorProfileUrl = post.authorAvatar,
                                        imageUrl = post.fullImageUrl,
                                        likes = post.likes.toString(),
                                        comments = post.commentsCount.toString(),
                                        isLiked = post.isLiked,
                                        onLikeClick = { currentUser?.id?.toIntOrNull()?.let { viewModel.toggleLike(post.id, it) } },
                                        onClick = { onPostClick(post.id) },
                                        sharedItinerary = post.sharedItinerary
                                    )
                                }
                            }
                        }
                        "Membres" -> {
                            items(group.users ?: emptyList()) { user ->
                                ListItem(
                                    headlineContent = { 
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(user.pseudo ?: user.username ?: "Utilisateur")
                                            if (user.role == "ADMIN") {
                                                Spacer(Modifier.width(8.dp))
                                                SuggestionChip(onClick = {}, label = { Text("Admin", fontSize = 10.sp) })
                                            }
                                        }
                                    },
                                    supportingContent = { Text("@${user.username}") },
                                    leadingContent = {
                                        Surface(modifier = Modifier.size(40.dp).clip(CircleShape), color = Color.LightGray) {
                                            if (user.profileUrl != null) AsyncImage(model = user.profileUrl, contentDescription = null, contentScale = ContentScale.Crop)
                                            else Icon(Icons.Default.Person, null, tint = Color.Gray, modifier = Modifier.padding(8.dp))
                                        }
                                    }
                                )
                            }
                        }
                        "Demandes" -> {
                            if (joinRequests.isEmpty()) {
                                item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Text("Aucune demande en attente", color = Color.Gray) } }
                            } else {
                                items(joinRequests) { request ->
                                    ListItem(
                                        headlineContent = { Text(request.username) },
                                        leadingContent = {
                                            Surface(modifier = Modifier.size(40.dp).clip(CircleShape), color = Color.LightGray) {
                                                if (request.userAvatar != null) AsyncImage(model = request.userAvatar, contentDescription = null, contentScale = ContentScale.Crop)
                                                else Icon(Icons.Default.Person, null, tint = Color.Gray, modifier = Modifier.padding(8.dp))
                                            }
                                        },
                                        trailingContent = {
                                            Row {
                                                IconButton(onClick = { viewModel.respondToRequest(group.id, request.id, true) }) {
                                                    Icon(Icons.Default.Check, "Accepter", tint = Color.Green)
                                                }
                                                IconButton(onClick = { viewModel.respondToRequest(group.id, request.id, false) }) {
                                                    Icon(Icons.Default.Close, "Refuser", tint = Color.Red)
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
