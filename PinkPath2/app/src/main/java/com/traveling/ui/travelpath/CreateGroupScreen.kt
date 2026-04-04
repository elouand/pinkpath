package com.traveling.ui.travelpath

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.traveling.ui.travelshare.AuthTextField

@Composable
fun CreateGroupScreen(onBack: () -> Unit) {
    var groupName by remember { mutableStateOf("") }
    var isPrivate by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Créer un groupe",
            style = MaterialTheme.typography.displayMedium,
            color = TravelingDeepPurple,
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(32.dp))

        AuthTextField(
            value = groupName,
            onValueChange = { groupName = it },
            placeholder = "Nom du groupe"
        )

        Spacer(modifier = Modifier.height(40.dp))

        TravelingSearchBar(placeholder = "Rechercher un utilisateur")

        Spacer(modifier = Modifier.height(24.dp))

        // Avatars Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .padding(end = 8.dp)
                        .clip(CircleShape)
                        .background(Color.Gray)
                )
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.White.copy(alpha = 0.5f)
            ) {
                Text(
                    text = "+1",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
            Text(
                text = "groupe privé",
                style = MaterialTheme.typography.titleLarge,
                color = TravelingDeepPurple,
                fontWeight = FontWeight.Bold
            )
            Switch(
                checked = isPrivate,
                onCheckedChange = { isPrivate = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF9C27B0), // Purple switch
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color.Gray
                )
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { /* TODO: Create Group Logic */ },
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(100.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TravelingDeepPurple),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Créer le groupe",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}
