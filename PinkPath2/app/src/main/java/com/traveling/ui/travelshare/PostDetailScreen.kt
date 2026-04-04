package com.traveling.ui.travelshare

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.traveling.ui.common.Tag
import com.traveling.ui.common.TravelingSearchBar
import com.traveling.ui.theme.TravelingDeepPurple
import com.traveling.ui.theme.TravelingTagBlue
import com.traveling.ui.theme.TravelingTagYellow

@Composable
fun PostDetailScreen(postId: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        // Search Bar (Same as Feed)
        TravelingSearchBar(
            placeholder = "Rechercher des posts",
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Main Card
        Card(
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.7f)),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp)
                .padding(bottom = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Post Title
                Text(
                    text = "Superbe faculté, l’espace vert est magnifique.",
                    style = MaterialTheme.typography.displayMedium,
                    fontSize = 24.sp,
                    lineHeight = 28.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Tags
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Tag("Université", TravelingTagBlue)
                    Tag("Uni", TravelingTagBlue)
                    Tag("FDS", TravelingTagBlue)
                    Tag("Montpellier", TravelingTagYellow)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Author Info
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Gray)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(text = "Faculté des sciences", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(text = "Yanis Portes", color = Color.Gray, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Post Image Placeholder
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.LightGray)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Post Stats & Report
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Favorite, contentDescription = null, tint = TravelingDeepPurple, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "122k", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        
                        Spacer(modifier = Modifier.width(24.dp))
                        
                        Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "2", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    
                    Icon(Icons.Default.Warning, contentDescription = "Signaler", tint = TravelingDeepPurple, modifier = Modifier.size(30.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Comments Section
                CommentItem(
                    author = "Eliott Cani",
                    comment = "C’est vraiment super j’ai très envie d’y aller!",
                    likes = "100k"
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                CommentItem(
                    author = "Kylian Joigneault",
                    comment = "Non c’est nul",
                    likes = "0"
                )
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun CommentItem(author: String, comment: String, likes: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.Gray)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = author, color = Color.Gray, fontSize = 14.sp)
            Text(
                text = comment,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = TravelingDeepPurple,
                lineHeight = 22.sp
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.FavoriteBorder, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = likes, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
        
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Signaler",
            tint = TravelingDeepPurple,
            modifier = Modifier
                .size(24.dp)
                .align(Alignment.Bottom)
        )
    }
}
