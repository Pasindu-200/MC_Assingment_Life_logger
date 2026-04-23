package com.example.lifelogger.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.lifelogger.R
import com.example.lifelogger.data.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: (String?) -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    val darkBrown = Color(0xFF3E2723)

    Box(modifier = modifier.fillMaxSize()) {
        // Background Image
        Image(
            painter = painterResource(id = R.drawable.front_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 1.0f 
        )

        // UI Container
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Transparent 
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "📔 Book of My Life",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = darkBrown,
                    fontFamily = FontFamily.Serif
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Capture your moments",
                    style = MaterialTheme.typography.bodyLarge,
                    color = darkBrown.copy(alpha = 0.8f),
                    fontFamily = FontFamily.Serif
                )

                Spacer(modifier = Modifier.height(48.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email", color = Color.Black) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedBorderColor = Color.Black,
                        unfocusedBorderColor = Color.Gray
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password", color = Color.Black) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedBorderColor = Color.Black,
                        unfocusedBorderColor = Color.Gray
                    )
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = Color.Red,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isLoading) {
                    CircularProgressIndicator(color = darkBrown)
                } else {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        // Sign In Button
                        TextButton(
                            onClick = {
                                scope.launch {
                                    isLoading = true
                                    errorMessage = null
                                    try {
                                        SupabaseClient.auth.signInWith(Email) {
                                            this.email = email
                                            this.password = password
                                        }
                                        val user = SupabaseClient.auth.currentUserOrNull()
                                        onLoginSuccess(user?.id)
                                    } catch (e: Exception) {
                                        errorMessage = "Login failed: ${e.message}"
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = email.isNotBlank() && password.isNotBlank()
                        ) {
                            Text(
                                text = "Sign In",
                                color = darkBrown,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = FontFamily.Serif
                            )
                        }

                        // Sign Up Button
                        TextButton(
                            onClick = {
                                scope.launch {
                                    isLoading = true
                                    errorMessage = null
                                    try {
                                        SupabaseClient.auth.signUpWith(Email) {
                                            this.email = email
                                            this.password = password
                                        }
                                        errorMessage = "Check your email for confirmation!"
                                    } catch (e: Exception) {
                                        errorMessage = "Sign up failed: ${e.message}"
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = email.isNotBlank() && password.isNotBlank()
                        ) {
                            Text(
                                text = "Sign Up",
                                color = darkBrown,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = FontFamily.Serif
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = { onSkip() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Continue without account", color = Color.Black)
                }

                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "• Entries stored locally\n• Cloud sync when logged in",
                    style = MaterialTheme.typography.labelSmall,
                    color = darkBrown.copy(alpha = 0.6f)
                )
            }
        }
    }
}
