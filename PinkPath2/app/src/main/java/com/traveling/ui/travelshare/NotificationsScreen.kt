package com.traveling.ui.travelshare

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
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
import com.traveling.ui.theme.TravelingDeepPurple

@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onPostClick: (String) -> Unit,
    onUserClick: (Int) -> Unit,
    onGroupClick: (Int) -> Unit = {},
    viewModel: PostViewModel = hiltViewModel()
) {
    val notifications by viewModel.notifications.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadNotifications()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TravelingDeepPurple)
            }
            Text(
                "Notifications",
                style = MaterialTheme.typography.headlineSmall,
                color = TravelingDeepPurple,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (notifications.any { !it.read }) {
                TextButton(onClick = { viewModel.markAllNotificationsRead() }) {
                    Text("Tout lire", color = TravelingDeepPurple, fontSize = 13.sp)
                }
            }
        }

        if (notifications.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Notifications, null, tint = Color.LightGray, modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Aucune notification", color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(notifications, key = { it.id }) { notif ->
                    NotificationRow(
                        notif = notif,
                        onClick = {
                            when {
                                notif.type == "event_soon" && notif.groupId != null -> onGroupClick(notif.groupId)
                                notif.photoId != null -> onPostClick(notif.photoId.toString())
                                else -> onUserClick(notif.fromId)
                            }
                        },
                        onAvatarClick = { onUserClick(notif.fromId) }
                    )
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f))
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(
    notif: com.traveling.domain.model.AppNotification,
    onClick: () -> Unit,
    onAvatarClick: () -> Unit
) {
    val bg = if (notif.read) Color.Transparent else TravelingDeepPurple.copy(alpha = 0.06f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable(onClick = onAvatarClick)
        ) {
            if (notif.fromProfileUrl != null) {
                AsyncImage(
                    model = notif.fromProfileUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = CircleShape,
                    color = TravelingDeepPurple.copy(alpha = 0.15f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, null, tint = TravelingDeepPurple, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            val name = notif.fromPseudo ?: notif.fromUsername
            Text(
                text = buildString {
                    append(name)
                    when (notif.type) {
                        "new_post" -> append(" a publié un nouveau post")
                        "event_soon" -> {
                            val evTitle = notif.eventTitle
                            if (evTitle != null) append(" : « $evTitle » commence dans 1h !")
                            else append(" : un événement commence dans 1h !")
                        }
                        else -> append(" a effectué une action")
                    }
                },
                fontSize = 14.sp,
                fontWeight = if (notif.read) FontWeight.Normal else FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = formatNotifDate(notif.createdAt),
                fontSize = 12.sp,
                color = Color.Gray
            )
        }

        if (!notif.read) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(TravelingDeepPurple, CircleShape)
            )
        }
    }
}

private fun formatNotifDate(isoDate: String): String {
    return try {
        val date = java.time.OffsetDateTime.parse(isoDate)
        val now = java.time.OffsetDateTime.now()
        val minutes = java.time.Duration.between(date, now).toMinutes()
        when {
            minutes < 1 -> "À l'instant"
            minutes < 60 -> "Il y a ${minutes}min"
            minutes < 1440 -> "Il y a ${minutes / 60}h"
            else -> "Il y a ${minutes / 1440}j"
        }
    } catch (e: Exception) {
        isoDate.take(10)
    }
}
