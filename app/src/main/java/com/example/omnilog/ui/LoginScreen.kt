package com.example.omnilog.ui

import androidx.biometric.BiometricManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.omnilog.security.BiometricAuthManager
import com.example.omnilog.ui.theme.BrandEmerald
import com.example.omnilog.ui.theme.BrandGradientEnd
import com.example.omnilog.ui.theme.BrandGradientMid
import com.example.omnilog.ui.theme.BrandGradientStart
import com.example.omnilog.viewmodel.MainViewModel

enum class AuthMode { LOGIN, REGISTER, FORGOT_PASSWORD }

@Composable
fun LoginScreen(viewModel: MainViewModel, onLoginSuccess: () -> Unit) {
    val userAccount by viewModel.userAccount.collectAsState()
    var authMode by remember { mutableStateOf(AuthMode.LOGIN) }
    var name            by remember { mutableStateOf("") }
    var email           by remember { mutableStateOf("") }
    var mobileNumber    by remember { mutableStateOf("") }
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
        name = ""; email = ""; mobileNumber = ""; password = ""; confirmPassword = ""
        errorText = null; successText = null; isLoading = false
    }

    fun validate(): Boolean {
        if (authMode == AuthMode.REGISTER && name.isBlank()) {
            errorText = "Please enter your full name"; return false
        }
        if (authMode == AuthMode.REGISTER && mobileNumber.isBlank()) {
            errorText = "Please enter your mobile number"; return false
        }
        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            errorText = "Please enter a valid email address"; return false
        }
        if (authMode == AuthMode.FORGOT_PASSWORD) {
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
        // Gradient base & live interactive background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
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

            // Logo with premium concentric PulseRings
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                PulseRing(color = BrandGradientStart, size = 180.dp, delayMs = 0)
                PulseRing(color = BrandGradientEnd, size = 130.dp, delayMs = 800)
                val infiniteTransition = rememberInfiniteTransition(label = "logoBreath")
                val scale by infiniteTransition.animateFloat(
                    initialValue = 0.96f,
                    targetValue = 1.04f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2400, easing = EaseInOutSine),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "logoScale"
                )
                RoutineLogo(
                    size = 96.dp,
                    modifier = Modifier.graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                )
            }

            Spacer(Modifier.height(16.dp))

            GradientText(
                text = "RoutineLog",
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                "Your AI-powered daily workspace companion",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            // Auth Card
            AnimatedContent(
                targetState = authMode,
                transitionSpec = {
                    fadeIn(tween(300)) togetherWith fadeOut(tween(250))
                },
                label = "authCard"
            ) { mode ->
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    // Headline Deluxe Typography
                    GradientText(
                        text = when (mode) {
                            AuthMode.LOGIN           -> "Welcome back"
                            AuthMode.REGISTER        -> "Create account"
                            AuthMode.FORGOT_PASSWORD -> "Reset password"
                        },
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = when (mode) {
                            AuthMode.LOGIN           -> "Sign in to access your dashboard"
                            AuthMode.REGISTER        -> "Fill in details below to register"
                            AuthMode.FORGOT_PASSWORD -> "We'll send a secure reset link"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )

                    Spacer(Modifier.height(24.dp))

                    // Full Name and Mobile (Register only)
                    if (mode == AuthMode.REGISTER) {
                        PremiumTextField(
                            value = name,
                            onValueChange = { name = it; errorText = null },
                            label = "Full Name",
                            leadingIcon = { Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary) }
                        )
                        Spacer(Modifier.height(14.dp))
                        
                        PremiumTextField(
                            value = mobileNumber,
                            onValueChange = { mobileNumber = it; errorText = null },
                            label = "Mobile Number",
                            leadingIcon = { Icon(Icons.Default.Phone, null, tint = MaterialTheme.colorScheme.primary) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                        )
                        Spacer(Modifier.height(14.dp))
                    }

                    // Email Input
                    PremiumTextField(
                        value = email,
                        onValueChange = { email = it; errorText = null },
                        label = "Email Address",
                        leadingIcon = { Icon(Icons.Default.Email, null, tint = MaterialTheme.colorScheme.primary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        isError = errorText?.contains("email", ignoreCase = true) == true
                    )

                    // Password Input (Login + Register only)
                    if (mode != AuthMode.FORGOT_PASSWORD) {
                        Spacer(Modifier.height(14.dp))
                        PremiumTextField(
                            value = password,
                            onValueChange = { password = it; errorText = null },
                            label = "Password",
                            leadingIcon = { Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (passwordVisible) "Hide" else "Show",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None
                                                   else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            isError = errorText?.contains("password", ignoreCase = true) == true ||
                                      errorText?.contains("match", ignoreCase = true) == true
                        )
                        
                        if (mode == AuthMode.LOGIN) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "💡 Hint: Default password is 'admin123' if not registered.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.align(Alignment.Start).padding(start = 4.dp)
                            )
                        }
                    }

                    // Confirm Password (Register only)
                    if (mode == AuthMode.REGISTER) {
                        Spacer(Modifier.height(14.dp))
                        PremiumTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it; errorText = null },
                            label = "Confirm Password",
                            leadingIcon = { Icon(Icons.Default.LockOpen, null, tint = MaterialTheme.colorScheme.primary) },
                            trailingIcon = {
                                IconButton(onClick = { confirmVisible = !confirmVisible }) {
                                    Icon(
                                        if (confirmVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (confirmVisible) "Hide" else "Show",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            visualTransformation = if (confirmVisible) VisualTransformation.None
                                                   else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            isError = errorText?.contains("match", ignoreCase = true) == true
                        )
                    }

                    // Error Message
                    AnimatedVisibility(visible = errorText != null) {
                        errorText?.let {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 14.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f))
                                    .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ErrorOutline, null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(it,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                            }
                        }
                    }

                    // Success Message
                    AnimatedVisibility(visible = successText != null) {
                        successText?.let {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 14.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(BrandEmerald.copy(alpha = 0.15f))
                                    .border(1.dp, BrandEmerald.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircleOutline, null,
                                    tint = BrandEmerald,
                                    modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(it,
                                    color = BrandEmerald,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // Primary Interactive CTA Button with Scale Transition
                    Button(
                        onClick = {
                            if (validate()) {
                                when (mode) {
                                    AuthMode.REGISTER -> {
                                        isLoading = true
                                        viewModel.checkIfAdminRegistered(name, email) { adminExists ->
                                            if (adminExists) {
                                                isLoading = false
                                                errorText = "An Admin account is already registered. Only one Admin is allowed globally."
                                            } else {
                                                val prefs = context.getSharedPreferences("auth_prefs", android.content.Context.MODE_PRIVATE)
                                                prefs.edit().putString("saved_password", password).apply()
                                                viewModel.registerUser(name, email, mobileNumber, password) {
                                                    isLoading = false
                                                    onLoginSuccess()
                                                }
                                            }
                                        }
                                    }
                                    AuthMode.FORGOT_PASSWORD -> {
                                        successText = "Reset link sent to $email"
                                        errorText = null
                                        // Reset password back to default
                                        val prefs = context.getSharedPreferences("auth_prefs", android.content.Context.MODE_PRIVATE)
                                        prefs.edit().putString("saved_password", "admin123").apply()
                                        viewModel.sendForgotPasswordNotification(email)
                                    }
                                    AuthMode.LOGIN -> {
                                        isLoading = true
                                        viewModel.loginUser(
                                            email = email,
                                            passwordEntered = password,
                                            onSuccess = {
                                                isLoading = false
                                                onLoginSuccess()
                                            },
                                            onFailure = { cloudError ->
                                                isLoading = false
                                                errorText = cloudError
                                            }
                                        )
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
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
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    // Premium Biometric Button with animation scanner halo
                    if (mode == AuthMode.LOGIN && canUseBiometric && userAccount?.biometricEnabled == true) {
                        Spacer(Modifier.height(18.dp))
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val infiniteTransition = rememberInfiniteTransition(label = "bioGlow")
                            val bioScale by infiniteTransition.animateFloat(
                                initialValue = 1f, targetValue = 1.15f,
                                animationSpec = infiniteRepeatable(tween(1600, easing = EaseOutQuad), RepeatMode.Restart),
                                label = "bioScale"
                            )
                            val bioAlpha by infiniteTransition.animateFloat(
                                initialValue = 0.4f, targetValue = 0f,
                                animationSpec = infiniteRepeatable(tween(1600, easing = EaseOutQuad), RepeatMode.Restart),
                                label = "bioAlpha"
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.95f)
                                    .height(48.dp)
                                    .graphicsLayer { scaleX = bioScale; scaleY = bioScale; alpha = bioAlpha }
                                    .border(2.dp, BrandGradientEnd, RoundedCornerShape(14.dp))
                            )

                            OutlinedButton(
                                onClick = {
                                    biometricManager.showBiometricPrompt(
                                        activity = context as FragmentActivity,
                                        onSuccess = onLoginSuccess,
                                        onError = { errorText = it }
                                    )
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color(0x0CFFFFFF)
                                ),
                                border = BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(BrandGradientStart, BrandGradientEnd)))
                            ) {
                                Icon(
                                    Icons.Default.Fingerprint, null,
                                    modifier = Modifier.size(22.dp),
                                    tint = BrandGradientEnd
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    "Secure Biometric Sign In",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Auth mode toggles
            if (authMode != AuthMode.FORGOT_PASSWORD) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        if (authMode == AuthMode.LOGIN) "Don't have an account? "
                        else "Already have an account? ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
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
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (authMode == AuthMode.LOGIN) {
                TextButton(onClick = {
                    authMode = AuthMode.FORGOT_PASSWORD
                    resetFields()
                }) {
                    Text("Forgot your password?",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                }
            }

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
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun PremiumTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    isError: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }
    val focusGlow by animateFloatAsState(
        targetValue = if (isFocused) 0.6f else 0.15f,
        animationSpec = tween(300),
        label = "fieldGlow"
    )

    val borderBrush = if (isError) {
        Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.error))
    } else {
        Brush.linearGradient(
            colors = listOf(
                BrandGradientStart.copy(alpha = focusGlow),
                BrandGradientMid.copy(alpha = focusGlow * 0.5f),
                BrandGradientEnd.copy(alpha = focusGlow)
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x0CFFFFFF))
            .border(1.5.dp, borderBrush, RoundedCornerShape(14.dp))
    ) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)) },
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isFocused = it.isFocused },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                errorContainerColor = Color.Transparent
            ),
            singleLine = true
        )
    }
}
