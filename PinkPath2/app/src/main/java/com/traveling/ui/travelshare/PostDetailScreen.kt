package com.traveling.ui.travelshare

import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.traveling.ui.theme.TravelingDeepPurple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    postId: String, 
    onBack: () -> Unit,
    viewModel: PostViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val posts by viewModel.posts.collectAsState()
    val post = remember(posts, postId) { posts.find { it.id == postId } }
    
    val commentsMap by viewModel.comments.collectAsState()
    val comments = commentsMap[postId] ?: emptyList()
    val currentUser by authViewModel.currentUser.collectAsState()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
    val error by viewModel.error.collectAsState()

    var commentText by remember { mutableStateOf("") }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }

    // Observation des erreurs (important pour debug les commentaires)
    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    LaunchedEffect(isLoggedIn) {
        if (posts.isEmpty()) {
            viewModel.loadPosts()
        }
    }

    LaunchedEffect(postId) {
        viewModel.loadComments(postId)
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Détails du post") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (post == null) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = TravelingDeepPurple)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Image du Post
                if (post.fullImageUrl != null) {
                    AsyncImage(
                        model = post.fullImageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .clip(RoundedCornerShape(24.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Titre (Description)
                Text(
                    text = post.content ?: "Sans description",
                    style = MaterialTheme.typography.displayMedium,
                    fontSize = 24.sp,
                    lineHeight = 28.sp,
                    color = TravelingDeepPurple
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Localisation et Auteur
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (post.authorAvatar != null) {
                        AsyncImage(
                            model = post.authorAvatar,
                            contentDescription = null,
                            modifier = Modifier.size(45.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(modifier = Modifier.size(45.dp).clip(CircleShape).background(Color.Gray))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = post.title ?: "Lieu inconnu", 
                            fontWeight = FontWeight.Bold, 
                            fontSize = 16.sp,
                            modifier = Modifier.clickable {
                                val label = post.title ?: "Lieu"
                                val uri = if (post.latitude != null && post.longitude != null) {
                                    Uri.parse("geo:${post.latitude},${post.longitude}?q=${post.latitude},${post.longitude}($label)")
                                } else {
                                    Uri.parse("geo:0,0?q=$label")
                                }
                                val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                                mapIntent.setPackage("com.google.android.apps.maps")
                                try {
                                    context.startActivity(mapIntent)
                                } catch (e: Exception) {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                }
                            },
                            color = TravelingDeepPurple
                        )
                        Text(text = "Par ${post.authorName ?: "Anonyme"}", color = Color.Gray, fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Audio Player Section
                if (post.audioUrl != null) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = TravelingDeepPurple.copy(alpha = 0.1f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    if (isPlaying) {
                                        mediaPlayer?.pause()
                                        isPlaying = false
                                    } else {
                                        if (mediaPlayer == null) {
                                            mediaPlayer = MediaPlayer().apply {
                                                setAudioAttributes(
                                                    AudioAttributes.Builder()
                                                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                                        .setUsage(AudioAttributes.USAGE_MEDIA)
                                                        .build()
                                                )
                                                try {
                                                    setDataSource(post.audioUrl)
                                                    prepareAsync()
                                                    setOnPreparedListener { 
                                                        start()
                                                        isPlaying = true
                                                    }
                                                    setOnCompletionListener { 
                                                        isPlaying = false 
                                                    }
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }
                                            }
                                        } else {
                                            mediaPlayer?.start()
                                            isPlaying = true
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause Audio",
                                    tint = TravelingDeepPurple,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Écouter la note audio",
                                color = TravelingDeepPurple,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Stats
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { 
                        currentUser?.id?.toIntOrNull()?.let { userId ->
                            viewModel.toggleLike(post.id, userId) 
                        }
                    }) {
                        Icon(
                            imageVector = if (post.isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (post.isLiked) TravelingDeepPurple else Color.Gray
                        )
                    }
                    Text(text = "${post.displayLikes}", fontWeight = FontWeight.Bold)
                    
                    Spacer(modifier = Modifier.width(24.dp))

                    Icon(Icons.Default.ChatBubbleOutline, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "${post.commentsCount}", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Section Commentaires
                Text(
                    text = "Commentaires",
                    style = MaterialTheme.typography.titleLarge,
                    color = TravelingDeepPurple,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))

                if (comments.isEmpty()) {
                    Text(text = "Aucun commentaire pour le moment", color = Color.Gray, fontSize = 14.sp)
                } else {
                    comments.forEach { comment ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            if (comment.authorAvatarUrl != null) {
                                AsyncImage(
                                    model = comment.authorAvatarUrl,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp).clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.LightGray))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = comment.authorName ?: "Anonyme", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(text = comment.text ?: "", fontSize = 14.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Ajouter un commentaire
                if (isLoggedIn) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = commentText,
                            onValueChange = { commentText = it },
                            placeholder = { Text("Ajouter un commentaire...") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (commentText.isNotBlank()) {
                                    currentUser?.id?.toIntOrNull()?.let { userId ->
                                        viewModel.addComment(postId, commentText, userId)
                                        commentText = ""
                                    }
                                }
                            },
                            enabled = commentText.isNotBlank()
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Envoyer", tint = TravelingDeepPurple)
                        }
                    }
                } else {
                    Text(
                        text = "Connectez-vous pour commenter",
                        color = Color.Gray,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
