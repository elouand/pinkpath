package com.traveling.ui.travelshare

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.traveling.ui.theme.TravelingDeepPurple

@Composable
fun LoginScreen(viewModel: AuthViewModel, onLoginSuccess: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.authEvent.collect { event ->
            when (event) {
                is AuthViewModel.AuthEvent.Success -> onLoginSuccess()
                is AuthViewModel.AuthEvent.Error -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopEnd) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = TravelingDeepPurple,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "Me connecter",
            style = MaterialTheme.typography.displayMedium,
            color = TravelingDeepPurple,
            fontSize = 42.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(60.dp))

        AuthTextField(value = username, onValueChange = { username = it }, placeholder = "Username")
        Spacer(modifier = Modifier.height(30.dp))
        AuthTextField(value = password, onValueChange = { password = it }, placeholder = "Mot-de-passe", isPassword = true)

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { viewModel.login(username, password) },
            modifier = Modifier
                .width(220.dp)
                .height(80.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TravelingDeepPurple),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Je me\nconnecte !",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 24.sp,
                textAlign = TextAlign.Center,
                color = Color.White
            )
        }
        
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isPassword: Boolean = false
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(text = placeholder, color = Color.DarkGray) },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 8.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color(0xFFD1C4E9),
            unfocusedContainerColor = Color(0xFFD1C4E9),
            disabledContainerColor = Color(0xFFD1C4E9),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = if (isPassword) KeyboardOptions(keyboardType = KeyboardType.Password) else KeyboardOptions.Default
    )
}
