package com.traveling.ui.travelshare

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.traveling.domain.model.Post
import com.traveling.ui.common.PostCard
import com.traveling.ui.theme.TravelingDeepPurple
import com.traveling.ui.theme.TravelingTagBlue
import com.traveling.ui.theme.TravelingTagYellow

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FeedScreen(
    onPostClick: (String) -> Unit,
    onCreatePostClick: () -> Unit,
    onUserClick: (Int) -> Unit = {},
    initialGroupName: String? = null,
    initialSearch: String? = null,
    viewModel: PostViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val posts by viewModel.posts.collectAsState()
    val followingPosts by viewModel.followingPosts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    val userGroups by viewModel.groups.collectAsState()
    val userSearchResults by viewModel.userSearchResults.collectAsState()

    var searchQuery by remember { mutableStateOf(initialSearch ?: "") }
    var selectedTab by remember { mutableStateOf(if (initialGroupName != null) "Group" else "Populaires") }
    var expanded by remember { mutableStateOf(false) }
    var showUserDropdown by remember { mutableStateOf(false) }

    var selectedGroup by remember { mutableStateOf<String?>(initialGroupName) }

    // Nouveau filtre : Tout, Posts, Itinéraires
    var contentTypeFilter by remember { mutableStateOf("Tout") }

    // Applied search for posts (only set on Enter/submit)
    var activePostSearch by remember { mutableStateOf(initialSearch ?: "") }

    // Trigger user search as user types
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank()) {
            viewModel.searchUsers(searchQuery)
            showUserDropdown = true
        } else {
            viewModel.clearUserSearch()
            showUserDropdown = false
            activePostSearch = ""
        }
    }

    val filteredPosts = remember(posts, followingPosts, selectedTab, selectedGroup, activePostSearch, contentTypeFilter) {
        var result = when (selectedTab) {
            "Abonnements" -> followingPosts
            "Group" -> if (selectedGroup == null) posts else posts.filter { it.groupName == selectedGroup }
            else -> posts.filter { it.isPublic }
        }

        result = when (contentTypeFilter) {
            "Posts" -> result.filter { it.itineraryId == null }
            "Itinéraires" -> result.filter { it.itineraryId != null }
            else -> result
        }

        if (activePostSearch.isNotBlank()) {
            result = result.filter {
                it.title?.contains(activePostSearch, ignoreCase = true) == true ||
                it.content?.contains(activePostSearch, ignoreCase = true) == true ||
                it.tags?.any { tag -> tag.contains(activePostSearch, ignoreCase = true) } == true
            }
        }
        result
    }

    LaunchedEffect(isLoggedIn) {
        viewModel.loadPosts()
        viewModel.loadUserGroups()
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab == "Abonnements") viewModel.loadFollowingPosts()
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

            // Hybrid search bar with user dropdown
            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White.copy(alpha = 0.9f),
                    shadowElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Search, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.weight(1f),
                            textStyle = TextStyle(fontSize = 15.sp, color = MaterialTheme.colorScheme.onBackground),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                activePostSearch = searchQuery
                                showUserDropdown = false
                                viewModel.clearUserSearch()
                            }),
                            decorationBox = { inner ->
                                if (searchQuery.isEmpty()) {
                                    Text("Rechercher posts ou utilisateurs", color = Color.Gray, fontSize = 15.sp)
                                }
                                inner()
                            }
                        )
                    }
                }

                // User results dropdown
                if (showUserDropdown && userSearchResults.isNotEmpty()) {
                    DropdownMenu(
                        expanded = true,
                        onDismissRequest = { showUserDropdown = false },
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .background(Color.White)
                    ) {
                        userSearchResults.forEach { user ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (user.profileUrl != null) {
                                            AsyncImage(
                                                model = user.profileUrl,
                                                contentDescription = null,
                                                modifier = Modifier.size(32.dp).clip(CircleShape)
                                            )
                                        } else {
                                            Surface(
                                                modifier = Modifier.size(32.dp),
                                                shape = CircleShape,
                                                color = TravelingDeepPurple.copy(alpha = 0.15f)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(Icons.Default.Person, null, tint = TravelingDeepPurple, modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = user.pseudo ?: user.username,
                                                fontWeight = FontWeight.Medium,
                                                color = TravelingDeepPurple,
                                                fontSize = 14.sp
                                            )
                                            if (user.pseudo != null) {
                                                Text("@${user.username}", fontSize = 12.sp, color = Color.Gray)
                                            }
                                        }
                                    }
                                },
                                onClick = {
                                    showUserDropdown = false
                                    viewModel.clearUserSearch()
                                    searchQuery = ""
                                    onUserClick(user.id)
                                }
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // Selection Row (Populaires / Abonnements / Groupes)
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
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.titleMedium,
                        color = TravelingDeepPurple,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                if (isLoggedIn) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (selectedTab == "Abonnements") TravelingDeepPurple else Color.Transparent,
                        modifier = Modifier.clickable { selectedTab = "Abonnements" }
                    ) {
                        Text(
                            text = "Abonnements",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (selectedTab == "Abonnements") Color.White else TravelingDeepPurple,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))
                }

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
                                style = MaterialTheme.typography.titleMedium,
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
                    when (selectedTab) {
                        "Abonnements" -> viewModel.loadFollowingPosts()
                        else -> { viewModel.loadPosts(); viewModel.loadUserGroups() }
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                if (filteredPosts.isEmpty() && !isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        val message = when (selectedTab) {
                            "Abonnements" -> "Aucun post de vos abonnements"
                            "Group" -> "Aucun post dans ce groupe"
                            else -> "Aucun post public"
                        }
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
                                sharedItinerary = post.sharedItinerary,
                                onReportClick = { viewModel.reportPost(post.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}
