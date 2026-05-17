package com.traveling.ui.travelshare

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.traveling.domain.model.Post
import com.traveling.ui.common.PostCard
import com.traveling.ui.common.TravelingSearchBar
import com.traveling.ui.theme.TravelingDeepPurple
import com.traveling.ui.theme.TravelingTagBlue
import com.traveling.ui.theme.TravelingTagYellow

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FeedScreen(
    onPostClick: (String) -> Unit,
    onCreatePostClick: () -> Unit,
    initialGroupName: String? = null,
    initialSearch: String? = null,
    viewModel: PostViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val posts by viewModel.posts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    val userGroups by viewModel.groups.collectAsState()

    var searchQuery by remember { mutableStateOf(initialSearch ?: "") }
    var selectedTab by remember { mutableStateOf(if (initialGroupName != null) "Group" else "Populaires") }
    var expanded by remember { mutableStateOf(false) }
    
    var selectedGroup by remember { mutableStateOf<String?>(initialGroupName) }
    
    // Nouveau filtre : Tout, Posts, Itinéraires
    var contentTypeFilter by remember { mutableStateOf("Tout") }

    // Filtrage des posts
    val filteredPosts = remember(posts, selectedTab, selectedGroup, searchQuery, contentTypeFilter) {
        var result = if (selectedTab == "Populaires") {
            posts.filter { it.isPublic }
        } else {
            if (selectedGroup == null) posts else posts.filter { it.groupName == selectedGroup }
        }

        // Filtre par type de contenu
        result = when (contentTypeFilter) {
            "Posts" -> result.filter { it.itineraryId == null }
            "Itinéraires" -> result.filter { it.itineraryId != null }
            else -> result
        }

        if (searchQuery.isNotBlank()) {
            result = result.filter { 
                it.title?.contains(searchQuery, ignoreCase = true) == true ||
                it.content?.contains(searchQuery, ignoreCase = true) == true ||
                it.tags?.any { tag -> tag.contains(searchQuery, ignoreCase = true) } == true
            }
        }
        result
    }

    LaunchedEffect(isLoggedIn) {
        viewModel.loadPosts()
        viewModel.loadUserGroups()
    }

    Scaffold(
        floatingActionButton = {
            if (isLoggedIn) {
                FloatingActionButton(
                    onClick = onCreatePostClick,
                    containerColor = TravelingDeepPurple,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Post")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            TravelingSearchBar(
                placeholder = "Rechercher des posts",
                initialValue = searchQuery,
                onValueChange = { searchQuery = it }
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // Selection Row (Populaires / Groupes)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (selectedTab == "Populaires") Color.White else Color.Transparent,
                    modifier = Modifier.clickable { selectedTab = "Populaires" }
                ) {
                    Text(
                        text = "Populaires",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.titleLarge,
                        color = TravelingDeepPurple,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (selectedTab == "Group") TravelingDeepPurple else Color.Transparent,
                        modifier = Modifier.clickable { 
                            selectedTab = "Group"
                            expanded = true 
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedGroup ?: "Groupes",
                                style = MaterialTheme.typography.titleLarge,
                                color = if (selectedTab == "Group") Color.White else TravelingDeepPurple,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = if (selectedTab == "Group") Color.White else TravelingDeepPurple
                            )
                        }
                    }

                    if (userGroups.isNotEmpty()) {
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Tous mes groupes", color = TravelingDeepPurple) },
                                onClick = {
                                    selectedGroup = null
                                    selectedTab = "Group"
                                    expanded = false
                                }
                            )
                            userGroups.forEach { group ->
                                DropdownMenuItem(
                                    text = { Text(group.name ?: "", color = TravelingDeepPurple) },
                                    onClick = {
                                        selectedGroup = group.name
                                        selectedTab = "Group"
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Content Type Filter Chips
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                listOf("Tout", "Posts", "Itinéraires").forEach { type ->
                    FilterChip(
                        selected = contentTypeFilter == type,
                        onClick = { contentTypeFilter = type },
                        label = { Text(type) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TravelingTagYellow,
                            selectedLabelColor = TravelingDeepPurple,
                            labelColor = Color.Gray
                        ),
                        border = null,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            PullToRefreshBox(
                isRefreshing = isLoading,
                onRefresh = { 
                    viewModel.loadPosts()
                    viewModel.loadUserGroups()
                },
                modifier = Modifier.weight(1f)
            ) {
                if (filteredPosts.isEmpty() && !isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        val message = if (selectedTab == "Populaires") "Aucun post public" else "Aucun post dans ce groupe"
                        Text(message, color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(
                            items = filteredPosts,
                            key = { it.id },
                            contentType = { "post" }
                        ) { post ->
                            val tags = remember(post.tags) {
                                post.tags?.mapIndexed { index, tag -> 
                                    tag to if (index % 4 == 3) TravelingTagYellow else TravelingTagBlue 
                                } ?: emptyList()
                            }
                            
                            PostCard(
                                title = post.content ?: post.title ?: "Sans titre",
                                tags = tags,
                                location = post.title ?: "Inconnu",
                                author = post.authorName ?: "Anonyme",
                                authorProfileUrl = post.authorAvatar,
                                imageUrl = post.fullImageUrl,
                                likes = post.likes.toString(),
                                comments = post.commentsCount.toString(),
                                isLiked = post.isLiked,
                                onLikeClick = {
                                    currentUser?.id?.toIntOrNull()?.let { userId ->
                                        viewModel.toggleLike(post.id, userId)
                                    }
                                },
                                onClick = { onPostClick(post.id) },
                                sharedItinerary = post.sharedItinerary
                            )
                        }
                    }
                }
            }
        }
    }
}
