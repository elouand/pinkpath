package com.traveling.ui.travelpath

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import com.traveling.domain.model.GroupEvent
import com.traveling.domain.model.SavedItinerary
import com.traveling.ui.common.PostCard
import com.traveling.ui.theme.TravelingDeepPurple
import com.traveling.ui.theme.TravelingTagBlue
import com.traveling.ui.travelshare.AuthViewModel
import com.traveling.ui.travelshare.PostViewModel
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    groupId: Int,
    onBack: () -> Unit,
    onNavigateToGroupFeed: (String) -> Unit,
    onPostClick: (String) -> Unit,
    onUserClick: (Int) -> Unit = {},
    viewModel: PostViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    itineraryViewModel: ItineraryViewModel = hiltViewModel()
) {
    val selectedGroup by viewModel.selectedGroup.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    val joinRequests by viewModel.joinRequests.collectAsState()
    val groupEvents by viewModel.groupEvents.collectAsState()
    val eventCreateSuccess by viewModel.eventCreateSuccess.collectAsState()
    val itineraryUiState by itineraryViewModel.uiState.collectAsState()
    val itineraries = itineraryUiState.savedItineraries

    var selectedTab by remember { mutableStateOf("Flux") }
    var showCreateEventDialog by remember { mutableStateOf(false) }

    LaunchedEffect(groupId) {
        viewModel.loadGroupDetails(groupId)
        viewModel.loadGroupEvents(groupId)
    }

    LaunchedEffect(currentUser) {
        if (currentUser != null) itineraryViewModel.loadItineraries()
    }

    LaunchedEffect(eventCreateSuccess) {
        if (eventCreateSuccess) {
            showCreateEventDialog = false
            viewModel.clearEventCreateSuccess()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(selectedGroup?.name ?: "Détails du groupe") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (isLoading && selectedGroup == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = TravelingDeepPurple)
            }
        } else if (selectedGroup == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Groupe introuvable", color = Color.Gray)
            }
        } else {
            val group = selectedGroup!!
            val userInGroup = group.users?.find { it.id == currentUser?.id }
            val isAdmin = userInGroup?.role == "ADMIN" || group.userRole == "ADMIN"
            val showRequestsTab = isAdmin && !group.isPublic

            val tabs = mutableListOf("Flux", "Événements", "Membres")
            if (showRequestsTab) tabs.add("Demandes")

            Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                // Header
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier.size(100.dp).clip(RoundedCornerShape(24.dp)),
                        color = TravelingDeepPurple.copy(alpha = 0.1f)
                    ) {
                        if (group.imageUrl != null) {
                            AsyncImage(model = group.imageUrl, contentDescription = null, contentScale = ContentScale.Crop)
                        } else {
                            Icon(Icons.Default.Group, null, tint = TravelingDeepPurple, modifier = Modifier.padding(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = group.name, style = MaterialTheme.typography.headlineSmall, color = TravelingDeepPurple, fontWeight = FontWeight.Bold)
                    group.description?.let { Text(text = it, style = MaterialTheme.typography.bodyMedium, color = Color.Gray) }
                }

                TabRow(
                    selectedTabIndex = tabs.indexOf(selectedTab).coerceAtLeast(0),
                    containerColor = Color.Transparent,
                    contentColor = TravelingDeepPurple,
                    indicator = { tabPositions ->
                        val index = tabs.indexOf(selectedTab).coerceAtLeast(0)
                        if (index < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[index]),
                                color = TravelingDeepPurple
                            )
                        }
                    }
                ) {
                    tabs.forEach { tabName ->
                        Tab(
                            selected = selectedTab == tabName,
                            onClick = { selectedTab = tabName },
                            text = { Text(tabName) }
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (selectedTab) {
                        "Événements" -> {
                            if (isAdmin) {
                                item {
                                    Button(
                                        onClick = { showCreateEventDialog = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = TravelingDeepPurple),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Créer un événement")
                                    }
                                }
                            }
                            if (groupEvents.isEmpty()) {
                                item {
                                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                        Text("Aucun événement planifié", color = Color.Gray)
                                    }
                                }
                            } else {
                                items(groupEvents) { event ->
                                    EventCard(
                                        event = event,
                                        isLoggedIn = currentUser != null,
                                        currentUserId = currentUser?.id?.toIntOrNull(),
                                        onToggleInterest = { viewModel.toggleInterest(event.id, event.isInterested) },
                                        itineraryViewModel = itineraryViewModel
                                    )
                                }
                            }
                        }
                        "Flux" -> {
                            val groupPosts = group.photos ?: emptyList()
                            if (groupPosts.isEmpty()) {
                                item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Text("Aucun post dans ce groupe", color = Color.Gray) } }
                            } else {
                                items(groupPosts) { post ->
                                    PostCard(
                                        title = post.content ?: post.title ?: "Sans titre",
                                        tags = post.tags?.map { it to TravelingTagBlue } ?: emptyList(),
                                        location = post.title ?: "Inconnu",
                                        author = post.authorName ?: "Anonyme",
                                        authorProfileUrl = post.authorAvatar,
                                        imageUrl = post.fullImageUrl,
                                        likes = post.likes.toString(),
                                        comments = post.commentsCount.toString(),
                                        isLiked = post.isLiked,
                                        onLikeClick = { currentUser?.id?.toIntOrNull()?.let { viewModel.toggleLike(post.id, it) } },
                                        onClick = { onPostClick(post.id) },
                                        sharedItinerary = post.sharedItinerary,
                                        onReportClick = { viewModel.reportPost(post.id) },
                                        onAuthorClick = post.authorId?.let { { onUserClick(it) } }
                                    )
                                }
                            }
                        }
                        "Membres" -> {
                            items(group.users ?: emptyList()) { user ->
                                ListItem(
                                    headlineContent = { 
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(user.pseudo ?: user.username ?: "Utilisateur")
                                            if (user.role == "ADMIN") {
                                                Spacer(Modifier.width(8.dp))
                                                SuggestionChip(onClick = {}, label = { Text("Admin", fontSize = 10.sp) })
                                            }
                                        }
                                    },
                                    supportingContent = { Text("@${user.username}") },
                                    leadingContent = {
                                        Surface(modifier = Modifier.size(40.dp).clip(CircleShape), color = Color.LightGray) {
                                            if (user.profileUrl != null) AsyncImage(model = user.profileUrl, contentDescription = null, contentScale = ContentScale.Crop)
                                            else Icon(Icons.Default.Person, null, tint = Color.Gray, modifier = Modifier.padding(8.dp))
                                        }
                                    }
                                )
                            }
                        }
                        "Demandes" -> {
                            if (joinRequests.isEmpty()) {
                                item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Text("Aucune demande en attente", color = Color.Gray) } }
                            } else {
                                items(joinRequests) { request ->
                                    ListItem(
                                        headlineContent = { Text(request.username) },
                                        leadingContent = {
                                            Surface(modifier = Modifier.size(40.dp).clip(CircleShape), color = Color.LightGray) {
                                                if (request.userAvatar != null) AsyncImage(model = request.userAvatar, contentDescription = null, contentScale = ContentScale.Crop)
                                                else Icon(Icons.Default.Person, null, tint = Color.Gray, modifier = Modifier.padding(8.dp))
                                            }
                                        },
                                        trailingContent = {
                                            Row {
                                                IconButton(onClick = { viewModel.respondToRequest(group.id, request.id, true) }) {
                                                    Icon(Icons.Default.Check, "Accepter", tint = Color.Green)
                                                }
                                                IconButton(onClick = { viewModel.respondToRequest(group.id, request.id, false) }) {
                                                    Icon(Icons.Default.Close, "Refuser", tint = Color.Red)
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateEventDialog) {
        CreateEventDialog(
            groupId = groupId,
            itineraries = itineraries,
            onDismiss = { showCreateEventDialog = false },
            onCreate = { title, desc, startAt, itineraryId ->
                viewModel.createEvent(groupId, title, desc, startAt, itineraryId)
            }
        )
    }
}

// ── EventCard ────────────────────────────────────────────────────────────────
@Composable
private fun EventCard(
    event: GroupEvent,
    isLoggedIn: Boolean,
    currentUserId: Int?,
    onToggleInterest: () -> Unit,
    itineraryViewModel: ItineraryViewModel
) {
    var showInterestedList by remember { mutableStateOf(false) }
    var showItinerary by remember { mutableStateOf(false) }
    val uiState by itineraryViewModel.uiState.collectAsState()
    val copySuccess = uiState.copySuccess

    val dateLabel = remember(event.startAt) {
        try {
            val dt = OffsetDateTime.parse(event.startAt).atZoneSameInstant(ZoneId.systemDefault())
            dt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH'h'mm"))
        } catch (e: Exception) { event.startAt.take(16).replace("T", " ") }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Event, null, tint = TravelingDeepPurple, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    if (!event.title.isNullOrBlank()) {
                        Text(event.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TravelingDeepPurple)
                    }
                    Text(dateLabel, fontSize = 13.sp, color = Color.Gray)
                }
            }

            if (!event.description.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(event.description, fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground)
            }

            if (event.itinerary != null) {
                Spacer(Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = TravelingDeepPurple.copy(alpha = 0.06f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showItinerary = !showItinerary }
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.DirectionsWalk, null, tint = TravelingDeepPurple, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                event.itinerary.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TravelingDeepPurple,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                if (showItinerary) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                null, tint = TravelingDeepPurple, modifier = Modifier.size(18.dp)
                            )
                        }
                        if (showItinerary) {
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("${event.itinerary.duration} min", fontSize = 11.sp, color = Color.Gray)
                                Text("${String.format("%.1f", event.itinerary.distance)} km", fontSize = 11.sp, color = Color.Gray)
                                Text(event.itinerary.mode, fontSize = 11.sp, color = Color.Gray)
                            }
                            Spacer(Modifier.height(6.dp))
                            event.itinerary.steps.forEachIndexed { idx, step ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = TravelingDeepPurple,
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("${idx + 1}", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Spacer(Modifier.width(6.dp))
                                    Text(step.name, fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground)
                                }
                            }
                            if (isLoggedIn && currentUserId != null) {
                                Spacer(Modifier.height(8.dp))
                                if (copySuccess) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Check, null, tint = Color(0xFF388E3C), modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Ajouté à vos itinéraires !", color = Color(0xFF388E3C), fontSize = 12.sp)
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            itineraryViewModel.copyItinerary(event.itinerary.id, currentUserId)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = TravelingDeepPurple),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.Download, null, modifier = Modifier.size(15.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Récupérer cet itinéraire", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (event.itineraryName != null) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.DirectionsWalk, null, tint = TravelingDeepPurple, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(event.itineraryName, fontSize = 12.sp, color = TravelingDeepPurple)
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isLoggedIn) {
                    Button(
                        onClick = onToggleInterest,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (event.isInterested) TravelingDeepPurple else Color.LightGray
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            if (event.isInterested) Icons.Default.Star else Icons.Default.StarBorder,
                            null, modifier = Modifier.size(16.dp),
                            tint = if (event.isInterested) Color.White else Color.DarkGray
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (event.isInterested) "Intéressé(e)" else "Je suis intéressé(e)",
                            fontSize = 12.sp,
                            color = if (event.isInterested) Color.White else Color.DarkGray
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                if (event.interestedCount > 0) {
                    TextButton(onClick = { showInterestedList = true }) {
                        Text(
                            "${event.interestedCount} intéressé${if (event.interestedCount > 1) "s" else ""}",
                            color = TravelingDeepPurple, fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }

    if (showInterestedList && event.interestedUsers.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showInterestedList = false },
            title = { Text("Personnes intéressées") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    event.interestedUsers.forEach { user ->
                        ListItem(
                            headlineContent = { Text(user.pseudo ?: user.username) },
                            supportingContent = if (user.pseudo != null) { { Text("@${user.username}") } } else null,
                            leadingContent = {
                                Surface(modifier = Modifier.size(36.dp).clip(CircleShape), color = TravelingDeepPurple.copy(alpha = 0.15f)) {
                                    if (user.profileUrl != null) AsyncImage(model = user.profileUrl, contentDescription = null, contentScale = ContentScale.Crop)
                                    else Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Person, null, tint = TravelingDeepPurple, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showInterestedList = false }) { Text("Fermer") } }
        )
    }
}

// ── CreateEventDialog ─────────────────────────────────────────────────────────
@Composable
private fun CreateEventDialog(
    groupId: Int,
    itineraries: List<SavedItinerary>,
    onDismiss: () -> Unit,
    onCreate: (title: String?, description: String?, startAt: String, itineraryId: Int?) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedItinerary by remember { mutableStateOf<SavedItinerary?>(null) }
    var showItineraryMenu by remember { mutableStateOf(false) }

    val calendar = remember { Calendar.getInstance() }
    var dateLabel by remember { mutableStateOf("Choisir une date et heure") }
    var selectedIso by remember { mutableStateOf<String?>(null) }

    fun pickDateTime() {
        DatePickerDialog(
            context,
            { _, year, month, day ->
                TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        calendar.set(year, month, day, hour, minute, 0)
                        val iso = java.time.LocalDateTime
                            .of(year, month + 1, day, hour, minute)
                            .atZone(ZoneId.systemDefault())
                            .toOffsetDateTime()
                            .toString()
                        selectedIso = iso
                        dateLabel = "$day/${month+1}/$year à ${hour}h${minute.toString().padStart(2,'0')}"
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
                ).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Créer un événement") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titre (optionnel)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optionnel)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 3
                )

                // Date / heure
                OutlinedButton(
                    onClick = { pickDateTime() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TravelingDeepPurple)
                ) {
                    Icon(Icons.Default.CalendarMonth, null, tint = TravelingDeepPurple, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(dateLabel, color = TravelingDeepPurple, fontSize = 13.sp)
                }

                // Sélection itinéraire
                if (itineraries.isNotEmpty()) {
                    Box {
                        OutlinedButton(
                            onClick = { showItineraryMenu = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, TravelingDeepPurple)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.DirectionsWalk, null, tint = TravelingDeepPurple, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(selectedItinerary?.name ?: "Itinéraire (optionnel)", color = TravelingDeepPurple, fontSize = 13.sp)
                        }
                        DropdownMenu(
                            expanded = showItineraryMenu,
                            onDismissRequest = { showItineraryMenu = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Aucun") },
                                onClick = { selectedItinerary = null; showItineraryMenu = false }
                            )
                            itineraries.forEach { it ->
                                DropdownMenuItem(
                                    text = { Text(it.name) },
                                    onClick = { selectedItinerary = it; showItineraryMenu = false }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val iso = selectedIso ?: return@Button
                    onCreate(
                        title.ifBlank { null },
                        description.ifBlank { null },
                        iso,
                        selectedItinerary?.id
                    )
                },
                enabled = selectedIso != null,
                colors = ButtonDefaults.buttonColors(containerColor = TravelingDeepPurple)
            ) {
                Text("Créer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}
