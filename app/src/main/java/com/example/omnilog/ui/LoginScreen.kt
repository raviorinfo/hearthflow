package com.example.omnilog.ui

import androidx.biometric.BiometricManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.omnilog.security.BiometricAuthManager
import com.example.omnilog.viewmodel.MainViewModel

enum class AuthMode { LOGIN, REGISTER, FORGOT_PASSWORD }

@Composable
fun LoginScreen(viewModel: MainViewModel, onLoginSuccess: () -> Unit) {
    val userAccount by viewModel.userAccount.collectAsState()
    var authMode by remember { mutableStateOf(AuthMode.LOGIN) }
    var name            by remember { mutableStateOf("") }
    var email           by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorText       by remember { mutableStateOf<String?>(null) }
    var successText     by remember { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmVisible  by remember { mutableStateOf(false) }
    var isLoading       by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val biometricManager = remember { BiometricAuthManager(context) }
    val canUseBiometric = remember {
        val bm = BiometricManager.from(context)
        bm.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.BIOMETRIC_WEAK
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun resetFields() {
        name = ""; email = ""; password = ""; confirmPassword = ""
        errorText = null; successText = null
    }

    fun validate(): Boolean {
        if (authMode == AuthMode.REGISTER && name.isBlank()) {
            errorText = "Please enter your full name"; return false
        }
        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            errorText = "Please enter a valid email address"; return false
        }
        if (authMode == AuthMode.FORGOT_PASSWORD) {
            // only email is needed
            errorText = null; return true
        }
        if (password.length < 6) {
            errorText = "Password must be at least 6 characters"; return false
        }
        if (authMode == AuthMode.REGISTER && password != confirmPassword) {
            errorText = "Passwords do not match"; return false
        }
        errorText = null; return true
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Gradient background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        )
        AnimatedBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.height(48.dp))

            // Logo + App name
            RoutineLogo(size = 96.dp)
            Spacer(Modifier.height(16.dp))
            Text(
                "RoutineLog",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "Your AI-powered daily companion",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(40.dp))

            // Auth Card
            AnimatedContent(
                targetState = authMode,
                transitionSpec = {
                    fadeIn(tween(250)) togetherWith fadeOut(tween(200))
                },
                label = "authCard"
            ) { mode ->
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    // Title
                    Text(
                        text = when (mode) {
                            AuthMode.LOGIN           -> "Welcome back"
                            AuthMode.REGISTER        -> "Create account"
                            AuthMode.FORGOT_PASSWORD -> "Reset password"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = when (mode) {
                            AuthMode.LOGIN           -> "Sign in to continue"
                            AuthMode.REGISTER        -> "Fill in your details below"
                            AuthMode.FORGOT_PASSWORD -> "We'll send a reset link to your email"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(24.dp))

                    // Full Name (Register only)
                    if (mode == AuthMode.REGISTER) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it; errorText = null },
                            label = { Text("Full Name") },
                            leadingIcon = { Icon(Icons.Default.Person, null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true
                        )
                        Spacer(Modifier.height(14.dp))
                    }

                    // Email
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; errorText = null },
                        label = { Text("Email address") },
                        leadingIcon = { Icon(Icons.Default.Email, null) },
                        isError = errorText?.contains("email", ignoreCase = true) == true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )

                    // Password (Login + Register only)
                    if (mode != AuthMode.FORGOT_PASSWORD) {
                        Spacer(Modifier.height(14.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it; errorText = null },
                            label = { Text("Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, null) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (passwordVisible) "Hide" else "Show"
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None
                                                   else PasswordVisualTransformation(),
                            isError = errorText?.contains("password", ignoreCase = true) == true ||
                                      errorText?.contains("match", ignoreCase = true) == true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true
                        )
                    }

                    // Confirm Password (Register only)
                    if (mode == AuthMode.REGISTER) {
                        Spacer(Modifier.height(14.dp))
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it; errorText = null },
                            label = { Text("Confirm Password") },
                            leadingIcon = { Icon(Icons.Default.LockOpen, null) },
                            trailingIcon = {
                                IconButton(onClick = { confirmVisible = !confirmVisible }) {
                                    Icon(
                                        if (confirmVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (confirmVisible) "Hide" else "Show"
                                    )
                                }
                            },
                            visualTransformation = if (confirmVisible) VisualTransformation.None
                                                   else PasswordVisualTransformation(),
                            isError = errorText?.contains("match", ignoreCase = true) == true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true
                        )
                    }

                    // Error / success messages
                    AnimatedVisibility(visible = errorText != null) {
                        errorText?.let {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ErrorOutline, null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(it,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    AnimatedVisibility(visible = successText != null) {
                        successText?.let {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF10B981).copy(alpha = 0.15f))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircleOutline, null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(it,
                                    color = Color(0xFF10B981),
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // Primary CTA
                    Button(
                        onClick = {
                            if (validate()) {
                                when (mode) {
                                    AuthMode.REGISTER -> {
                                        viewModel.registerUser(name, email)
                                        onLoginSuccess()
                                    }
                                    AuthMode.FORGOT_PASSWORD -> {
                                        // Mock: show success and go back to login
                                        successText = "Reset link sent to $email"
                                        errorText = null
                                    }
                                    AuthMode.LOGIN -> {
                                        onLoginSuccess()
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                when (mode) {
                                    AuthMode.LOGIN           -> "Sign In"
                                    AuthMode.REGISTER        -> "Create Account"
                                    AuthMode.FORGOT_PASSWORD -> "Send Reset Link"
                                },
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }

                    // Biometric login (Login mode only - if enabled by registered user)
                    if (mode == AuthMode.LOGIN && canUseBiometric && userAccount?.biometricEnabled == true) {
                        Spacer(Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = {
                                biometricManager.showBiometricPrompt(
                                    activity = context as FragmentActivity,
                                    onSuccess = onLoginSuccess,
                                    onError = { errorText = it }
                                )
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Fingerprint, null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("Sign in with Biometrics",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Toggle Login ↔ Register
            if (authMode != AuthMode.FORGOT_PASSWORD) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        if (authMode == AuthMode.LOGIN) "Don't have an account? "
                        else "Already have an account? ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = {
                            authMode = if (authMode == AuthMode.LOGIN) AuthMode.REGISTER
                                       else AuthMode.LOGIN
                            resetFields()
                        },
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text(
                            if (authMode == AuthMode.LOGIN) "Sign up" else "Sign in",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Forgot Password link (Login mode only)
            if (authMode == AuthMode.LOGIN) {
                TextButton(onClick = {
                    authMode = AuthMode.FORGOT_PASSWORD
                    resetFields()
                }) {
                    Text("Forgot your password?",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Back to Login (Forgot Password mode)
            if (authMode == AuthMode.FORGOT_PASSWORD) {
                TextButton(onClick = {
                    authMode = AuthMode.LOGIN
                    resetFields()
                }) {
                    Icon(Icons.Default.ArrowBack, null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(4.dp))
                    Text("Back to Sign In",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
