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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.traveling.ui.common.PostCard
import com.traveling.ui.common.TravelingSearchBar
import com.traveling.ui.theme.TravelingDeepPurple
import com.traveling.ui.theme.TravelingTagBlue
import com.traveling.ui.theme.TravelingTagYellow

@Composable
fun FeedScreen(onPostClick: (String) -> Unit, onCreatePostClick: () -> Unit) {
    var selectedTab by remember { mutableStateOf("Populaires") }
    var expanded by remember { mutableStateOf(false) }
    val groups = listOf("étudiants", "famille", "amis")
    var selectedGroup by remember { mutableStateOf(groups[0]) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreatePostClick,
                containerColor = TravelingDeepPurple,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Post")
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
            
            TravelingSearchBar(placeholder = "Rechercher des posts")
            
            Spacer(modifier = Modifier.height(16.dp))

            // Selection Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Populaires Tab
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

                // Group Dropdown
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
                                text = selectedGroup,
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

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        groups.forEach { group ->
                            DropdownMenuItem(
                                text = { Text(group, color = TravelingDeepPurple) },
                                onClick = {
                                    selectedGroup = group
                                    selectedTab = "Group"
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                item {
                    PostCard(
                        title = "“Superbe faculté, l’espace vert est magnifique.”",
                        tags = listOf(
                            "Université" to TravelingTagBlue,
                            "Uni" to TravelingTagBlue,
                            "FDS" to TravelingTagBlue,
                            "Montpellier" to TravelingTagYellow
                        ),
                        location = "Faculté des sciences",
                        author = "Yanis Portes",
                        likes = "122k",
                        comments = "2",
                        onClick = { onPostClick("post_1") }
                    )
                }
                item {
                    PostCard(
                        title = "la pizza à 10e est une affaire!!",
                        tags = listOf(
                            "Restaurant" to TravelingTagBlue,
                            "Pizza" to TravelingTagBlue,
                            "Halal" to TravelingTagBlue,
                            "Montpellier" to TravelingTagYellow
                        ),
                        location = "Casa Pizza",
                        author = "Hugo paulin",
                        likes = "45k",
                        comments = "12",
                        onClick = { onPostClick("post_2") }
                    )
                }
            }
        }
    }
}
