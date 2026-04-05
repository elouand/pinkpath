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
import androidx.hilt.navigation.compose.hiltViewModel
import com.traveling.ui.common.PostCard
import com.traveling.ui.common.TravelingSearchBar
import com.traveling.ui.theme.TravelingDeepPurple
import com.traveling.ui.theme.TravelingTagBlue
import com.traveling.ui.theme.TravelingTagYellow

@Composable
fun HomeScreen(
    onPostClick: (String) -> Unit,
    viewModel: PostViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val posts by viewModel.posts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()

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

        if (isLoading && posts.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TravelingDeepPurple)
                }
            }
        } else {
            items(posts) { post ->
                PostCard(
                    title = post.content ?: post.title ?: "Sans titre",
                    tags = post.tags?.map { it to TravelingTagBlue } ?: emptyList(),
                    location = post.title ?: "Inconnu",
                    author = post.authorName,
                    authorProfileUrl = post.authorAvatar,
                    imageUrl = post.fullImageUrl,
                    likes = (post.likes ?: 0).toString(),
                    comments = (post.commentsCount ?: 0).toString(),
                    isLiked = post.isLiked,
                    onLikeClick = { 
                        currentUser?.id?.toIntOrNull()?.let { userId ->
                            viewModel.toggleLike(post.id, userId) 
                        }
                    },
                    onClick = { onPostClick(post.id) }
                )
            }
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
