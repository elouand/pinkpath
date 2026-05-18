package com.traveling.ui.travelshare

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.traveling.ui.common.PostCard
import com.traveling.ui.theme.TravelingDeepPurple
import com.traveling.ui.theme.TravelingTagBlue
import com.traveling.ui.theme.TravelingTagYellow

@Composable
fun PublicProfileScreen(
    userId: Int,
    onBack: () -> Unit,
    onPostClick: (String) -> Unit,
    viewModel: PostViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val profile by viewModel.publicProfile.collectAsState()
    val isLoading by viewModel.isLoadingProfile.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
    val myId = currentUser?.id?.toIntOrNull()

    LaunchedEffect(userId) {
        viewModel.loadPublicProfile(userId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TravelingDeepPurple)
            }
            Text(
                text = profile?.pseudo ?: profile?.username ?: "Profil",
                style = MaterialTheme.typography.headlineSmall,
                color = TravelingDeepPurple,
                fontWeight = FontWeight.Bold
            )
        }

        if (isLoading && profile == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = TravelingDeepPurple)
            }
            return@Column
        }

        profile?.let { p ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (p.profileUrl != null) {
                            AsyncImage(
                                model = p.profileUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Surface(
                                modifier = Modifier.size(80.dp),
                                shape = CircleShape,
                                color = TravelingDeepPurple.copy(alpha = 0.15f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        (p.pseudo ?: p.username).take(1).uppercase(),
                                        fontSize = 32.sp,
                                        color = TravelingDeepPurple,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = p.pseudo ?: p.username,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (p.pseudo != null) {
                            Text("@${p.username}", fontSize = 13.sp, color = Color.Gray)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatItem(label = "Abonnés", value = p.followersCount)
                            StatItem(label = "Abonnements", value = p.followingCount)
                            StatItem(label = "Posts", value = p.posts.size)
                        }

                        // Boutons Follow + Cloche (masqués si c'est son propre profil)
                        if (isLoggedIn && myId != null && myId != p.id) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        if (p.isFollowing) viewModel.unfollowUser(p.id)
                                        else viewModel.followUser(p.id)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (p.isFollowing) Color.LightGray else TravelingDeepPurple
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = if (p.isFollowing) "Se désabonner" else "Suivre",
                                        color = if (p.isFollowing) Color.DarkGray else Color.White,
                                        fontSize = 13.sp
                                    )
                                }

                                if (p.isFollowing) {
                                    IconButton(
                                        onClick = { viewModel.toggleNotify(p.id, !p.notifyEnabled) }
                                    ) {
                                        Icon(
                                            imageVector = if (p.notifyEnabled)
                                                Icons.Default.NotificationsActive
                                            else
                                                Icons.Default.NotificationsNone,
                                            contentDescription = "Notifications",
                                            tint = if (p.notifyEnabled) TravelingDeepPurple else Color.Gray,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                if (p.posts.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Aucun post public", color = Color.Gray)
                        }
                    }
                } else {
                    items(items = p.posts, key = { it.id }) { post ->
                        val tags = remember(post.tags) {
                            post.tags?.mapIndexed { index, tag ->
                                tag to if (index % 4 == 3) TravelingTagYellow else TravelingTagBlue
                            } ?: emptyList()
                        }
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
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

@Composable
private fun StatItem(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value.toString(),
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = TravelingDeepPurple
        )
        Text(text = label, fontSize = 12.sp, color = Color.Gray)
    }
}
