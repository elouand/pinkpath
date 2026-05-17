package com.traveling.ui.travelpath

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.traveling.domain.model.Group
import com.traveling.domain.model.SavedItinerary
import com.traveling.ui.common.TravelingSearchBar
import com.traveling.ui.theme.TravelingDeepPurple
import com.traveling.ui.theme.TravelingTagYellow
import com.traveling.ui.travelshare.PostViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TravelPathScreen(
    onCreatePathClick: () -> Unit,
    onCreateGroupClick: () -> Unit,
    onGroupClick: (Int) -> Unit = {},
    onNavigateToMap: () -> Unit = {},
    onNavigateToEditItinerary: () -> Unit = {},
    viewModel: PostViewModel = hiltViewModel(),
    itineraryViewModel: ItineraryViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf("itinéraires") }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    val userGroups by viewModel.groups.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val itineraryState by itineraryViewModel.uiState.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadUserGroups()
        itineraryViewModel.loadItineraries()
    }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    LaunchedEffect(searchQuery, selectedTab) {
        if (selectedTab == "groupes" && searchQuery.isNotBlank()) {
            viewModel.searchGroups(searchQuery)
        }
    }

    val filteredItineraries = remember(searchQuery, itineraryState.savedItineraries) {
        if (searchQuery.isBlank()) itineraryState.savedItineraries
        else itineraryState.savedItineraries.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    if (!isSearchActive) {
                        Text(
                            text = if (selectedTab == "itinéraires") "Mes Itinéraires" else "Mes Groupes",
                            color = TravelingDeepPurple,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        TravelingSearchBar(
                            placeholder = if (selectedTab == "itinéraires") "Filtrer mes trajets..." else "Découvrir des groupes...",
                            initialValue = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth().padding(end = 8.dp)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        isSearchActive = !isSearchActive
                        if (!isSearchActive) searchQuery = ""
                    }) {
                        Icon(
                            imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Search",
                            tint = TravelingDeepPurple
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTab == "itinéraires") onCreatePathClick()
                    else onCreateGroupClick()
                },
                containerColor = TravelingDeepPurple,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.LightGray.copy(alpha = 0.3f),
                modifier = Modifier.height(40.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TabItem(text = "itinéraires", isSelected = selectedTab == "itinéraires", onClick = { selectedTab = "itinéraires" })
                    TabItem(text = "groupes", isSelected = selectedTab == "groupes", onClick = { selectedTab = "groupes" })
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading && searchQuery.isNotBlank() && selectedTab == "groupes") {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TravelingDeepPurple)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (selectedTab == "itinéraires") {
                        if (filteredItineraries.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = if (searchQuery.isBlank()) "Aucun itinéraire sauvegardé." else "Aucun résultat pour \"$searchQuery\"",
                                        color = Color.Gray,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            items(filteredItineraries) { itinerary ->
                                SavedItineraryCard(
                                    itinerary = itinerary,
                                    onModify = { itineraryViewModel.startEdit(itinerary); onNavigateToEditItinerary() },
                                    onStart = { itineraryViewModel.startItinerary(itinerary); onNavigateToMap() }
                                )
                            }
                        }
                    } else {
                        if (searchQuery.isBlank()) {
                            if (userGroups.isEmpty()) {
                                item { Text("Vous n'êtes membre d'aucun groupe.", color = Color.Gray, modifier = Modifier.padding(16.dp), textAlign = TextAlign.Center) }
                            } else {
                                items(userGroups) { group ->
                                    GroupCard(group = group, onClick = { onGroupClick(group.id) })
                                }
                            }
                        } else {
                            if (searchResults.isEmpty() && !isLoading) {
                                item { Text("Aucun groupe trouvé pour \"$searchQuery\".", color = Color.Gray, modifier = Modifier.padding(16.dp), textAlign = TextAlign.Center) }
                            } else {
                                items(searchResults) { group ->
                                    val isMember = userGroups.any { it.id == group.id }
                                    GroupCard(
                                        group = group, 
                                        onClick = { if (isMember) onGroupClick(group.id) },
                                        isSearchItem = true,
                                        isMember = isMember,
                                        onJoinClick = { viewModel.joinGroup(group.id) }
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

@Composable
fun TabItem(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) TravelingTagYellow else Color.Transparent,
        modifier = Modifier.clickable { onClick() }.fillMaxHeight()
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 24.dp)) {
            Text(text = text, fontWeight = FontWeight.Bold, color = if (isSelected) TravelingDeepPurple else Color.Gray, fontSize = 16.sp)
        }
    }
}

@Composable
fun SavedItineraryCard(itinerary: SavedItinerary, onModify: () -> Unit, onStart: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(itinerary.name, style = MaterialTheme.typography.titleMedium, color = TravelingDeepPurple, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, tint = TravelingDeepPurple, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("${itinerary.stepsCount} lieux", fontSize = 13.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, null, tint = TravelingDeepPurple, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("${itinerary.duration} min", fontSize = 13.sp)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onModify, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) {
                    Text("Infos", color = TravelingDeepPurple)
                }
                Button(onClick = onStart, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = TravelingDeepPurple), shape = RoundedCornerShape(10.dp)) {
                    Text("Démarrer")
                }
            }
        }
    }
}

@Composable
fun GroupCard(
    group: Group, 
    onClick: () -> Unit,
    isSearchItem: Boolean = false,
    isMember: Boolean = true,
    onJoinClick: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
        modifier = Modifier.fillMaxWidth().clickable(enabled = isMember) { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Surface(modifier = Modifier.size(60.dp).clip(RoundedCornerShape(12.dp)), color = TravelingDeepPurple.copy(alpha = 0.1f)) {
                    if (group.imageUrl != null) {
                        AsyncImage(model = group.imageUrl, contentDescription = null, contentScale = ContentScale.Crop)
                    } else {
                        Icon(Icons.Default.Group, null, tint = TravelingDeepPurple, modifier = Modifier.padding(12.dp))
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = group.name, style = MaterialTheme.typography.titleLarge, color = TravelingDeepPurple, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    group.description?.let { Text(text = it, style = MaterialTheme.typography.bodyMedium, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }

                if (isSearchItem && !isMember) {
                    Button(
                        onClick = onJoinClick,
                        enabled = !group.isPending,
                        colors = ButtonDefaults.buttonColors(containerColor = if (group.isPending) Color.Gray else TravelingDeepPurple),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = when {
                                group.isPending -> "En attente..."
                                group.isPublic -> "Rejoindre"
                                else -> "Demander"
                            },
                            fontSize = 12.sp
                        )
                    }
                }
            }
            
            if (isMember) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Badge(containerColor = TravelingTagYellow.copy(alpha = 0.2f)) {
                            Text("${group.count?.paths ?: 0} itinéraires", modifier = Modifier.padding(4.dp), color = TravelingDeepPurple, fontSize = 10.sp)
                        }
                        Badge(containerColor = TravelingDeepPurple.copy(alpha = 0.1f)) {
                            Text("${group.count?.photos ?: 0} posts", modifier = Modifier.padding(4.dp), color = TravelingDeepPurple, fontSize = 10.sp)
                        }
                    }
                    Text("${group.count?.users ?: 0} membres", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
    }
}
