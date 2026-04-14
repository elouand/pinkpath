package com.traveling.ui.travelpath

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.traveling.domain.model.Group
import com.traveling.ui.common.TravelingSearchBar
import com.traveling.ui.theme.TravelingDeepPurple
import com.traveling.ui.theme.TravelingTagYellow
import com.traveling.ui.travelshare.PostViewModel

@Composable
fun TravelPathScreen(
    onCreatePathClick: () -> Unit,
    onCreateGroupClick: () -> Unit,
    onGroupClick: (Int) -> Unit = {},
    viewModel: PostViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableStateOf("itinéraires") }
    val groups by viewModel.groups.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadUserGroups()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTab == "itinéraires") {
                        onCreatePathClick()
                    } else {
                        onCreateGroupClick()
                    }
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
            Spacer(modifier = Modifier.height(16.dp))

            TravelingSearchBar(
                placeholder = if (selectedTab == "itinéraires") "Rechercher un itinéraire" else "Rechercher un groupe"
            )

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.LightGray.copy(alpha = 0.3f),
                modifier = Modifier.height(40.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TabItem(
                        text = "itinéraires",
                        isSelected = selectedTab == "itinéraires",
                        onClick = { selectedTab = "itinéraires" }
                    )
                    TabItem(
                        text = "groupes",
                        isSelected = selectedTab == "groupes",
                        onClick = { selectedTab = "groupes" }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading && selectedTab == "groupes") {
                CircularProgressIndicator(color = TravelingDeepPurple)
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (selectedTab == "itinéraires") {
                        item {
                            PathCard(
                                name = "promenade",
                                locationsCount = "2 lieu",
                                duration = "1h30"
                            )
                        }
                    } else {
                        if (groups.isEmpty()) {
                            item {
                                Text(
                                    "Aucun groupe trouvé",
                                    modifier = Modifier.padding(16.dp),
                                    color = Color.Gray
                                )
                            }
                        } else {
                            items(groups) { group ->
                                GroupCard(
                                    group = group,
                                    onClick = { onGroupClick(group.id) }
                                )
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
        modifier = Modifier
            .clickable { onClick() }
            .fillMaxHeight()
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Text(
                text = text,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) TravelingDeepPurple else Color.Gray,
                fontSize = 18.sp
            )
        }
    }
}

@Composable
fun PathCard(name: String, locationsCount: String, duration: String) {
    Card(
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.7f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleLarge,
                color = TravelingDeepPurple,
                fontSize = 24.sp
            )
            
            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = TravelingDeepPurple)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = locationsCount, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = TravelingDeepPurple)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = duration, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { /* TODO */ },
                colors = ButtonDefaults.buttonColors(containerColor = TravelingDeepPurple),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.width(150.dp)
            ) {
                Text("Voir plus", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun GroupCard(group: Group, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Group Image Preview
                Surface(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    color = TravelingDeepPurple.copy(alpha = 0.1f)
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
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = TravelingDeepPurple,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    group.description?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val sharedPaths = group.count?.paths ?: 0
                    val postsCount = group.count?.photos ?: 0
                    
                    Badge(containerColor = TravelingTagYellow.copy(alpha = 0.2f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
                            Icon(Icons.AutoMirrored.Filled.DirectionsWalk, null, modifier = Modifier.size(14.dp), tint = TravelingDeepPurple)
                            Text(" $sharedPaths", color = TravelingDeepPurple)
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Badge(containerColor = TravelingDeepPurple.copy(alpha = 0.1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
                            Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(14.dp), tint = TravelingDeepPurple)
                            Text(" $postsCount", color = TravelingDeepPurple)
                        }
                    }
                }

                // Member previews
                Row {
                    val members = group.users ?: emptyList()
                    members.take(3).forEachIndexed { index, user ->
                        Surface(
                            modifier = Modifier
                                .size(28.dp)
                                .offset(x = (index * -8).dp)
                                .clip(CircleShape)
                                .background(Color.LightGray),
                            border = androidx.compose.foundation.BorderStroke(2.dp, Color.White)
                        ) {
                            if (user.profileUrl != null) {
                                AsyncImage(
                                    model = user.profileUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(Icons.Default.Person, null, modifier = Modifier.padding(4.dp), tint = Color.Gray)
                            }
                        }
                    }
                    val totalUsers = group.count?.users ?: members.size
                    if (totalUsers > 3) {
                        Text(
                            text = "+${totalUsers - 3}",
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .align(Alignment.CenterVertically),
                            fontSize = 12.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
