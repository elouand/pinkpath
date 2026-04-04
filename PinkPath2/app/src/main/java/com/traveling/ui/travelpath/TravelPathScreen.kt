package com.traveling.ui.travelpath

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.traveling.ui.common.TravelingSearchBar
import com.traveling.ui.theme.TravelingDeepPurple
import com.traveling.ui.theme.TravelingTagYellow

@Composable
fun TravelPathScreen(onCreatePathClick: () -> Unit, onCreateGroupClick: () -> Unit) {
    var selectedTab by remember { mutableStateOf("itinéraires") }

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

            // Search Bar
            TravelingSearchBar(
                placeholder = if (selectedTab == "itinéraires") "Rechercher un itinéraire" else "Rechercher un groupe"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Custom Tab Selector
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

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
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
                    item {
                        GroupCard(
                            name = "groupe B",
                            sharedPaths = "4 itinéraires partagés",
                            postsCount = "53 posts"
                        )
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
                // Map Preview Placeholder
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray)
                ) {
                    // Simulating a map image
                }

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
fun GroupCard(name: String, sharedPaths: String, postsCount: String) {
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

            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = TravelingDeepPurple, modifier = Modifier.size(30.dp)) // Using LocationOn as route placeholder
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = sharedPaths, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Group, contentDescription = null, tint = TravelingDeepPurple, modifier = Modifier.size(30.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = postsCount, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // User Avatars Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.Gray)
                            .padding(2.dp)
                    )
                }
                Text(text = "+1", color = Color.Gray, modifier = Modifier.padding(start = 4.dp), fontSize = 12.sp)
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
