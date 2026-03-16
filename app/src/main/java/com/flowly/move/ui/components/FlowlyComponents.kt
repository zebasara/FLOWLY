package com.flowly.move.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.flowly.move.ui.navigation.Routes
import com.flowly.move.ui.theme.*

// ── Cards ─────────────────────────────────────────────────────

@Composable
fun FlowlyCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(FlowlyCard, RoundedCornerShape(16.dp))
            .border(1.dp, FlowlyBorder, RoundedCornerShape(16.dp))
            .padding(14.dp),
        content = content
    )
}

@Composable
fun FlowlyCard2(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(FlowlyCard2, RoundedCornerShape(12.dp))
            .padding(12.dp),
        content = content
    )
}

// ── Buttons ───────────────────────────────────────────────────

@Composable
fun FlowlyPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = FlowlyAccent,
            contentColor = FlowlyBg,
            disabledContainerColor = FlowlyCard2,
            disabledContentColor = FlowlyMuted
        )
    ) {
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

@Composable
fun FlowlySecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color = FlowlyText
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = FlowlyCard2,
            contentColor = textColor
        ),
        border = BorderStroke(1.dp, FlowlyBorder)
    ) {
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

@Composable
fun FlowlyOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = FlowlyAccent),
        border = BorderStroke(1.5.dp, FlowlyAccent)
    ) {
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

// ── Input ─────────────────────────────────────────────────────

@Composable
fun FlowlyInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    isPassword: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            label,
            fontSize = 12.sp,
            color = FlowlyMuted,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = FlowlyMuted, fontSize = 13.sp) },
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = keyboardOptions,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor      = FlowlyAccent,
                unfocusedBorderColor    = FlowlyBorder,
                focusedContainerColor   = FlowlyCard2,
                unfocusedContainerColor = FlowlyCard2,
                cursorColor             = FlowlyAccent,
                focusedTextColor        = FlowlyText,
                unfocusedTextColor      = FlowlyText
            )
        )
        Spacer(Modifier.height(10.dp))
    }
}

// ── Tags ──────────────────────────────────────────────────────

@Composable fun TagGreen(text: String)  = FlowlyTag(text, TagGreenBg,  FlowlySuccess)
@Composable fun TagAmber(text: String)  = FlowlyTag(text, TagAmberBg,  FlowlyWarn)
@Composable fun TagRed(text: String)    = FlowlyTag(text, TagRedBg,    FlowlyDanger)
@Composable fun TagBlue(text: String)   = FlowlyTag(text, TagBlueBg,   FlowlyAccent3)
@Composable fun TagPurple(text: String) = FlowlyTag(text, TagPurpleBg, FlowlyPurple)

@Composable
fun FlowlyTag(text: String, bg: Color, textColor: Color) {
    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text(text, color = textColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ── Progress bar ─────────────────────────────────────────────

@Composable
fun FlowlyProgressBar(
    progress: Float,
    color: Color = FlowlyAccent,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .background(FlowlyCard2, RoundedCornerShape(3.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(color, RoundedCornerShape(3.dp))
        )
    }
}

// ── Avatar ────────────────────────────────────────────────────

@Composable
fun FlowlyAvatar(
    initials: String,
    photoUrl: String = "",
    size: Dp = 34.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(FlowlyAccent, FlowlyAccent2))),
        contentAlignment = Alignment.Center
    ) {
        if (photoUrl.isNotBlank()) {
            AsyncImage(
                model              = photoUrl,
                contentDescription = initials,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier.fillMaxSize()
            )
        } else {
            Text(
                initials.take(2).uppercase(),
                fontSize   = (size.value * 0.38f).sp,
                fontWeight = FontWeight.Bold,
                color      = FlowlyBg
            )
        }
    }
}

// ── Section title & separator ────────────────────────────────

@Composable
fun SectionTitle(text: String) {
    Text(
        text.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF4B6B4B),
        letterSpacing = 1.sp,
        modifier = Modifier.padding(top = 14.dp, bottom = 8.dp)
    )
}

@Composable
fun FlowlySeparator(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier = modifier.padding(vertical = 10.dp), color = FlowlyBorder)
}

// ── Metric mini card ─────────────────────────────────────────

@Composable
fun MetricCard(value: String, label: String, valueColor: Color = FlowlyText, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(FlowlyCard2, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = valueColor)
        Text(label, fontSize = 11.sp, color = FlowlyMuted)
    }
}

// ── Bottom Navigation Bar ─────────────────────────────────────

enum class BottomNavItem(val label: String, val emoji: String, val route: String) {
    HOME    ("Inicio",   "🏠", Routes.HOME),
    CANJES  ("Canjes",   "💱", Routes.CANJES),
    STORE   ("Tienda",   "🛍️", Routes.STORE),
    RANKINGS("Rankings", "🏆", Routes.RANKINGS),
    PROFILE ("Perfil",   "👤", Routes.PROFILE)
}

@Composable
fun FlowlyBottomNav(navController: NavController, currentRoute: String?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(FlowlyCard)
            .border(BorderStroke(1.dp, FlowlyBorder), RoundedCornerShape(0.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 4.dp, start = 8.dp, end = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem.entries.forEach { item ->
                val isSelected = currentRoute == item.route
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            navController.navigate(item.route) {
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(item.emoji, fontSize = 20.sp)
                    Text(
                        item.label,
                        fontSize = 9.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) FlowlyAccent else FlowlyMuted
                    )
                }
            }
        }
        // Espacio para la barra de navegación del sistema (back/home/recientes)
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsBottomHeight(WindowInsets.navigationBars)
                .background(FlowlyCard)
        )
    }
}

// ── Screen scaffold with bottom nav ──────────────────────────

@Composable
fun FlowlyScaffold(
    navController: NavController,
    currentRoute: String,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        containerColor = FlowlyBg,
        // Solo aplicamos el inset del status bar al contenido.
        // La navigation bar (back/home/recientes) la maneja el Spacer dentro de FlowlyBottomNav.
        contentWindowInsets = WindowInsets.statusBars,
        bottomBar = { FlowlyBottomNav(navController, currentRoute) }
    ) { paddingValues ->
        content(paddingValues)
    }
}
