package com.traveling.ui.travelshare

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.traveling.ui.common.PostCard
import com.traveling.ui.common.TravelingSearchBar
import com.traveling.ui.theme.TravelingDeepPurple
import com.traveling.ui.theme.TravelingTagBlue
import com.traveling.ui.theme.TravelingTagYellow

@Composable
fun HomeScreen(onPostClick: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        // Search Bar
        item {
            TravelingSearchBar(placeholder = "Rechercher un lieu")
        }

        // Around Me Section
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Autour de moi",
                    style = MaterialTheme.typography.displayMedium,
                    color = TravelingDeepPurple
                )
                Spacer(modifier = Modifier.height(16.dp))
                AroundMeRow()
            }
        }

        // Popular Posts Section
        item {
            Text(
                text = "Posts populaires",
                style = MaterialTheme.typography.displayMedium,
                color = TravelingDeepPurple,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

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
                comments = "5",
                onClick = { onPostClick("post_123") }
            )
        }
        
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
fun AroundMeRow() {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(5) {
            Box(
                modifier = Modifier
                    .size(width = 140.dp, height = 100.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.LightGray)
            ) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.Center),
                    tint = Color.White
                )
            }
        }
    }
}
