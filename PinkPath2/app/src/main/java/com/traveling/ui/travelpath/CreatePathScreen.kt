package com.traveling.ui.travelpath

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.traveling.ui.common.TravelingSearchBar
import com.traveling.ui.theme.TravelingDeepPurple
import com.traveling.ui.travelshare.AuthTextField

@Composable
fun CreatePathScreen(onBack: () -> Unit) {
    var pathName by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("") }
    var budget by remember { mutableStateOf("") }
    var selectedEffort by remember { mutableStateOf("walk") }
    var selectedActivity by remember { mutableStateOf("Restauration") }
    var isWeatherSensitive by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Créer un itinéraire",
            style = MaterialTheme.typography.displayMedium,
            color = TravelingDeepPurple,
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(32.dp))

        AuthTextField(value = pathName, onValueChange = { pathName = it }, placeholder = "Nom de l’itinéraire")

        Spacer(modifier = Modifier.height(16.dp))

        TravelingSearchBar(placeholder = "Rechercher un lieu")

        Spacer(modifier = Modifier.height(16.dp))

        // Selected Location Placeholder
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Stars, contentDescription = null, tint = TravelingDeepPurple)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Faculté des sciences",
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.Bold,
                color = TravelingDeepPurple
            )
            Icon(Icons.Default.Info, contentDescription = null, tint = TravelingDeepPurple) // Placeholder for command
            Text(text = "C", color = TravelingDeepPurple, fontWeight = FontWeight.Bold)
            Icon(Icons.AutoMirrored.Filled.ArrowRight, contentDescription = null, tint = TravelingDeepPurple)
        }

        Spacer(modifier = Modifier.height(16.dp))

        AuthTextField(value = duration, onValueChange = { duration = it }, placeholder = "durée voulue")
        Spacer(modifier = Modifier.height(16.dp))
        AuthTextField(value = budget, onValueChange = { budget = it }, placeholder = "budget")

        Spacer(modifier = Modifier.height(24.dp))

        // Effort and Weather Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                EffortButton(
                    icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                    isSelected = selectedEffort == "walk",
                    onClick = { selectedEffort = "walk" }
                )
                EffortButton(
                    icon = Icons.AutoMirrored.Filled.DirectionsRun,
                    isSelected = selectedEffort == "run",
                    onClick = { selectedEffort = "run" }
                )
            }
            
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isWeatherSensitive) TravelingDeepPurple.copy(alpha = 0.2f) else Color(0xFFD1C4E9),
                border = if (isWeatherSensitive) BorderStroke(2.dp, TravelingDeepPurple) else null,
                modifier = Modifier
                    .size(60.dp)
                    .clickable { isWeatherSensitive = !isWeatherSensitive }
            ) {
                Icon(
                    imageVector = Icons.Default.WbSunny,
                    contentDescription = null,
                    modifier = Modifier.padding(8.dp),
                    tint = if (isWeatherSensitive) TravelingDeepPurple else Color.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Activités",
            style = MaterialTheme.typography.displayMedium,
            color = TravelingDeepPurple,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Activities Horizontal Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ActivityItem(
                name = "Restauration",
                icon = Icons.Default.Restaurant,
                isSelected = selectedActivity == "Restauration",
                modifier = Modifier.weight(1f),
                onClick = { selectedActivity = "Restauration" }
            )
            ActivityItem(
                name = "Culture",
                icon = Icons.AutoMirrored.Filled.MenuBook,
                isSelected = selectedActivity == "Culture",
                modifier = Modifier.weight(1f),
                onClick = { selectedActivity = "Culture" }
            )
            ActivityItem(
                name = "Loisirs",
                icon = Icons.Default.SportsTennis,
                isSelected = selectedActivity == "Loisirs",
                modifier = Modifier.weight(1f),
                onClick = { selectedActivity = "Loisirs" }
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = { /* TODO: Calculation Logic */ },
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(70.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TravelingDeepPurple),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Calculer l’itinéraire",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun EffortButton(icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) TravelingDeepPurple.copy(alpha = 0.2f) else Color(0xFFD1C4E9),
        border = if (isSelected) BorderStroke(2.dp, TravelingDeepPurple) else null,
        modifier = Modifier
            .size(60.dp)
            .clickable { onClick() }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.padding(8.dp),
            tint = if (isSelected) TravelingDeepPurple else Color.Black
        )
    }
}

@Composable
fun ActivityItem(
    name: String,
    icon: ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = if (isSelected) TravelingDeepPurple.copy(alpha = 0.4f) else Color.Transparent,
        border = BorderStroke(2.dp, Color.Black),
        modifier = modifier
            .height(100.dp)
            .clickable { onClick() }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(4.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(40.dp))
            Text(
                text = name,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
