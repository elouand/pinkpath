package com.traveling.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.traveling.domain.model.SharedItineraryData
import com.traveling.ui.theme.TravelingDeepPurple
import com.traveling.ui.theme.TravelingTagBlue
import com.traveling.ui.theme.TravelingTagYellow

@Composable
fun TravelingSearchBar(
    placeholder: String,
    initialValue: String = "",
    onValueChange: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.9f),
        shadowElevation = 4.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color.Gray)
            Spacer(Modifier.width(8.dp))
            BasicTextField(
                value = initialValue,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(fontSize = 16.sp, color = Color.Black),
                singleLine = true,
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (initialValue.isEmpty()) {
                            Text(text = placeholder, color = Color.Gray, fontSize = 16.sp)
                        }
                        innerTextField()
                    }
                }
            )
            if (initialValue.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun PostCard(
    title: String,
    tags: List<Pair<String, Color>>,
    location: String,
    author: String,
    likes: String,
    comments: String,
    onClick: () -> Unit,
    imageUrl: String? = null,
    authorProfileUrl: String? = null,
    isLiked: Boolean = false,
    onLikeClick: (() -> Unit)? = null,
    sharedItinerary: SharedItineraryData? = null,
    onReportClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showReportDialog by remember { mutableStateOf(false) }

    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            icon = { Icon(Icons.Default.Flag, null, tint = Color(0xFFE53935)) },
            title = { Text("Signaler ce post") },
            text = { Text("Voulez-vous signaler ce post comme inapproprié ?") },
            confirmButton = {
                TextButton(onClick = {
                    onReportClick?.invoke()
                    showReportDialog = false
                }) { Text("Signaler", color = Color(0xFFE53935)) }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) { Text("Annuler") }
            }
        )
    }
    Card(
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = TravelingDeepPurple,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                tags.forEach { (text, color) ->
                    Tag(text, color)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (authorProfileUrl != null) {
                    AsyncImage(
                        model = authorProfileUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Gray)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(text = location, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(text = author, color = Color.Gray, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(24.dp)),
                    contentScale = ContentScale.Crop
                )
            } else if (sharedItinerary != null) {
                ItineraryPreviewBox(sharedItinerary)
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.LightGray)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onLikeClick?.invoke() }
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = null,
                            tint = if (isLiked) TravelingDeepPurple else Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = likes, fontWeight = FontWeight.Bold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = comments, fontWeight = FontWeight.Bold)
                    }
                }
                if (onReportClick != null) {
                    IconButton(
                        onClick = { showReportDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReportProblem,
                            contentDescription = "Signaler",
                            tint = Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ItineraryPreviewBox(itinerary: SharedItineraryData) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(24.dp)),
        color = TravelingDeepPurple.copy(alpha = 0.05f),
        border = androidx.compose.foundation.BorderStroke(1.dp, TravelingDeepPurple.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = itinerary.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = TravelingDeepPurple,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${itinerary.steps.size} étapes",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.DirectionsWalk,
                    null,
                    tint = TravelingDeepPurple,
                    modifier = Modifier.size(24.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itinerary.steps.take(3).forEachIndexed { index, step ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = TravelingDeepPurple,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text((index + 1).toString(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text(
                            text = step.name,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = TravelingDeepPurple
                        )
                    }
                    if (index < itinerary.steps.take(3).size - 1 || itinerary.steps.size > 3) {
                        Icon(
                            Icons.Default.ArrowForward,
                            null,
                            tint = Color.LightGray,
                            modifier = Modifier.size(16.dp).align(Alignment.CenterVertically)
                        )
                    }
                }
                if (itinerary.steps.size > 3) {
                    Text(
                        text = "+${itinerary.steps.size - 3}",
                        modifier = Modifier.align(Alignment.CenterVertically),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, null, tint = TravelingDeepPurple, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("${itinerary.duration} min", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Straighten, null, tint = TravelingDeepPurple, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("${String.format("%.1f", itinerary.distance / 1000.0)} km", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun Tag(text: String?, color: Color) {
    if (text == null) return
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            fontSize = 12.sp
        )
    }
}
