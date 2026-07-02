package com.zinema.app.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zinema.app.core.ui.theme.ZinemaColors

/**
 * Login screen (blueprint T-038) with the real MovieBox branching flow: email →
 * password OR verification code (existing passwordless / new account). Guest login
 * and an error snackbar throughout.
 */
@Composable
fun LoginScreen(
    onAuthenticated: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.authenticated) {
        if (state.authenticated) onAuthenticated()
    }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeError()
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = ZinemaColors.Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.auth_wordmark),
                color = ZinemaColors.Primary,
                fontSize = 40.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
            )
            Text(
                text = stringResource(R.string.auth_tagline),
                color = ZinemaColors.TextSecondary,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 32.dp),
            )

            when (state.step) {
                AuthStep.EMAIL -> EmailStep(
                    initialEmail = state.email,
                    enabled = !state.isLoading,
                    onSubmit = viewModel::onEmailSubmit,
                    onGuest = viewModel::loginAsGuest,
                )
                AuthStep.PASSWORD -> PasswordStep(
                    email = state.email,
                    enabled = !state.isLoading,
                    onSubmit = viewModel::onPasswordSubmit,
                    onBack = viewModel::onBackToEmail,
                )
                AuthStep.CODE -> CodeStep(
                    email = state.email,
                    isNewAccount = state.isNewAccount,
                    enabled = !state.isLoading,
                    onSubmit = viewModel::onCodeSubmit,
                    onResend = viewModel::onResendCode,
                    onBack = viewModel::onBackToEmail,
                )
            }

            if (state.isLoading) {
                CircularProgressIndicator(
                    color = ZinemaColors.Primary,
                    modifier = Modifier.padding(top = 24.dp),
                )
            }
        }
    }
}

@Composable
private fun EmailStep(
    initialEmail: String,
    enabled: Boolean,
    onSubmit: (String) -> Unit,
    onGuest: () -> Unit,
) {
    var email by rememberSaveable { mutableStateOf(initialEmail) }
    OutlinedTextField(
        value = email,
        onValueChange = { email = it },
        label = { Text(stringResource(R.string.auth_email)) },
        singleLine = true,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        colors = darkFieldColors(),
        modifier = Modifier.fillMaxWidth(),
    )
    PrimaryButton(
        text = stringResource(R.string.auth_continue),
        enabled = enabled && email.isNotBlank(),
        onClick = { onSubmit(email) },
    )
    TextButton(onClick = onGuest, enabled = enabled, modifier = Modifier.padding(top = 8.dp)) {
        Text(text = stringResource(R.string.auth_continue_as_guest), color = ZinemaColors.OnSurface)
    }
}

@Composable
private fun PasswordStep(
    email: String,
    enabled: Boolean,
    onSubmit: (String) -> Unit,
    onBack: () -> Unit,
) {
    var password by rememberSaveable { mutableStateOf("") }
    Text(text = email, color = ZinemaColors.TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
    OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        label = { Text(stringResource(R.string.auth_password)) },
        singleLine = true,
        enabled = enabled,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        colors = darkFieldColors(),
        modifier = Modifier.fillMaxWidth(),
    )
    PrimaryButton(
        text = stringResource(R.string.auth_sign_in),
        enabled = enabled && password.isNotBlank(),
        onClick = { onSubmit(password) },
    )
    TextButton(onClick = onBack, enabled = enabled, modifier = Modifier.padding(top = 8.dp)) {
        Text(text = stringResource(R.string.auth_use_different_email), color = ZinemaColors.OnSurface)
    }
}

@Composable
private fun CodeStep(
    email: String,
    isNewAccount: Boolean,
    enabled: Boolean,
    onSubmit: (String) -> Unit,
    onResend: () -> Unit,
    onBack: () -> Unit,
) {
    var code by rememberSaveable { mutableStateOf("") }
    val prompt = if (isNewAccount) R.string.auth_create_account else R.string.auth_code_sent
    Text(
        text = stringResource(prompt, email),
        color = ZinemaColors.TextSecondary,
        fontSize = 13.sp,
        modifier = Modifier.padding(bottom = 8.dp),
    )
    OutlinedTextField(
        value = code,
        onValueChange = { code = it.filter(Char::isDigit).take(6) },
        label = { Text(stringResource(R.string.auth_code)) },
        singleLine = true,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        colors = darkFieldColors(),
        modifier = Modifier.fillMaxWidth(),
    )
    PrimaryButton(
        text = stringResource(R.string.auth_verify),
        enabled = enabled && code.isNotBlank(),
        onClick = { onSubmit(code) },
    )
    Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = onResend, enabled = enabled) {
            Text(text = stringResource(R.string.auth_resend_code), color = ZinemaColors.OnSurface)
        }
        TextButton(onClick = onBack, enabled = enabled) {
            Text(text = stringResource(R.string.auth_use_different_email), color = ZinemaColors.TextSecondary)
        }
    }
}

@Composable
private fun PrimaryButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = ZinemaColors.Primary),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
    ) {
        Text(text = text, color = ZinemaColors.OnBackground)
    }
}

@Composable
private fun darkFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = ZinemaColors.OnBackground,
    unfocusedTextColor = ZinemaColors.OnBackground,
    focusedBorderColor = ZinemaColors.Primary,
    unfocusedBorderColor = ZinemaColors.TextTertiary,
    focusedLabelColor = ZinemaColors.Primary,
    unfocusedLabelColor = ZinemaColors.TextSecondary,
    cursorColor = ZinemaColors.Primary,
)
