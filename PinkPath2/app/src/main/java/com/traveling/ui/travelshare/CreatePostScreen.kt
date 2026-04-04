package com.traveling.ui.travelshare

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.traveling.ui.common.Tag
import com.traveling.ui.common.TravelingSearchBar
import com.traveling.ui.theme.TravelingDeepPurple
import com.traveling.ui.theme.TravelingTagBlue

@Composable
fun CreatePostScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Ajouter un post",
            style = MaterialTheme.typography.displayMedium,
            color = TravelingDeepPurple,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        // Add Image Box
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = TravelingDeepPurple,
            modifier = Modifier
                .size(200.dp)
                .padding(8.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Ajouter une image",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Icon(
                    imageVector = Icons.Default.NoteAdd,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(60.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Location Section
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Lieu:",
                style = MaterialTheme.typography.displayMedium,
                color = TravelingDeepPurple,
                fontSize = 28.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            TravelingSearchBar(placeholder = "Rechercher un lieu", modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Tags Section
        Text(
            text = "Tags",
            style = MaterialTheme.typography.displayMedium,
            color = TravelingDeepPurple
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Tag("Trampoline", TravelingTagBlue)
            Tag("sport", TravelingTagBlue)
            Tag("FUN", TravelingTagBlue)
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Add Note & Audio
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CreateOptionBox(
                title = "Ajouter note écrite",
                icon = Icons.Default.EditNote,
                modifier = Modifier.weight(1f)
            )
            CreateOptionBox(
                title = "Ajouter note audio",
                icon = Icons.Default.Mic,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Send Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SendButton(text = "Envoyer en public", modifier = Modifier.weight(1f))
            SendButton(text = "Envoyer au groupe", modifier = Modifier.weight(1f))
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun CreateOptionBox(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = TravelingDeepPurple,
        modifier = modifier.height(140.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

@Composable
fun SendButton(text: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(32.dp),
        color = Color(0xFFB39DDB), // Light purple
        modifier = modifier.height(56.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Text(
                text = text,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = null,
                tint = Color.White
            )
        }
    }
}
