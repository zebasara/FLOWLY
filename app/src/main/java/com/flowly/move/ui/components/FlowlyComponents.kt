package com.flowly.move.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Redeem
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.*
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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

// ── Gradients (shared) ────────────────────────────────────────

private val ButtonGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF8AF030), FlowlyAccentDark)
)
private val ProgressGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF8AF030), FlowlyAccentDark)
)
private val CardBorderGradient: Brush
    get() = Brush.verticalGradient(
        colors = listOf(FlowlyBorderBright, FlowlyBorder)
    )
private val CardBgGradient: Brush
    get() = Brush.verticalGradient(
        colors = listOf(FlowlyCard, FlowlyCardBottom)
    )

// ── Cards ─────────────────────────────────────────────────────

@Composable
fun FlowlyCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CardBgGradient, RoundedCornerShape(20.dp))
            .border(1.dp, CardBorderGradient, RoundedCornerShape(20.dp))
            .padding(18.dp),
        content = content
    )
}

@Composable
fun FlowlyCard2(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(FlowlyCard2, RoundedCornerShape(14.dp))
            .padding(14.dp),
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
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (enabled) ButtonGradient
                else Brush.linearGradient(listOf(FlowlyCard2, FlowlyCard2))
            )
            .clickable(
                enabled           = enabled,
                interactionSource = interactionSource,
                indication        = ripple(color = Color(0x40000000)),
                onClick           = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            fontWeight    = FontWeight.Bold,
            fontSize      = 15.sp,
            color         = if (enabled) FlowlyBg else FlowlyMuted,
            letterSpacing = 0.2.sp
        )
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
        onClick   = onClick,
        modifier  = modifier.fillMaxWidth().height(52.dp),
        shape     = RoundedCornerShape(16.dp),
        colors    = ButtonDefaults.outlinedButtonColors(
            containerColor = FlowlyCard2,
            contentColor   = textColor
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
        onClick  = onClick,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape    = RoundedCornerShape(16.dp),
        colors   = ButtonDefaults.outlinedButtonColors(contentColor = FlowlyAccent),
        border   = BorderStroke(1.5.dp, FlowlyAccent)
    ) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
            fontSize      = 11.sp,
            fontWeight    = FontWeight.SemiBold,
            color         = FlowlyTextSub,
            letterSpacing = 0.5.sp,
            modifier      = Modifier.padding(bottom = 6.dp)
        )
        OutlinedTextField(
            value                  = value,
            onValueChange          = onValueChange,
            placeholder            = {
                Text(placeholder, color = FlowlyMuted.copy(alpha = 0.7f), fontSize = 14.sp)
            },
            visualTransformation   = if (isPassword) PasswordVisualTransformation()
                                     else VisualTransformation.None,
            keyboardOptions        = keyboardOptions,
            modifier               = Modifier.fillMaxWidth(),
            shape                  = RoundedCornerShape(14.dp),
            singleLine             = true,
            colors                 = OutlinedTextFieldDefaults.colors(
                focusedBorderColor      = FlowlyAccent,
                unfocusedBorderColor    = FlowlyBorder,
                focusedContainerColor   = FlowlyCard2,
                unfocusedContainerColor = FlowlyCard2,
                cursorColor             = FlowlyAccent,
                focusedTextColor        = FlowlyText,
                unfocusedTextColor      = FlowlyText
            )
        )
        Spacer(Modifier.height(12.dp))
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
            .background(bg, RoundedCornerShape(100.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text,
            color         = textColor,
            fontSize      = 11.sp,
            fontWeight    = FontWeight.SemiBold,
            letterSpacing = 0.2.sp
        )
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
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(FlowlyCard3)
    ) {
        val p = progress.coerceIn(0f, 1f)
        if (p > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(p)
                    .fillMaxHeight()
                    .background(ProgressGradient, RoundedCornerShape(4.dp))
            )
        }
    }
}

// ── Avatar ────────────────────────────────────────────────────

@Composable
fun FlowlyAvatar(
    initials: String,
    photoUrl: String = "",
    size: Dp = 36.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(listOf(FlowlyAccent, FlowlyAccentDark))
            ),
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
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        fontSize   = 13.sp,
        fontWeight = FontWeight.Bold,
        color      = FlowlyTextSub,
        modifier   = modifier.padding(top = 20.dp, bottom = 10.dp)
    )
}

@Composable
fun FlowlySeparator(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier = modifier.padding(vertical = 10.dp), color = FlowlyBorder)
}

// ── Metric mini card ─────────────────────────────────────────

@Composable
fun MetricCard(
    value: String,
    label: String,
    valueColor: Color = FlowlyText,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(FlowlyCard2, RoundedCornerShape(14.dp))
            .border(1.dp, FlowlyBorder, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = valueColor)
        Text(label, fontSize = 11.sp, color = FlowlyMuted, letterSpacing = 0.2.sp)
    }
}

// ── Bottom Navigation Bar ─────────────────────────────────────

enum class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
) {
    HOME    ("Inicio",   Icons.Rounded.Home,        Routes.HOME),
    CANJES  ("Canjes",   Icons.Rounded.Redeem,       Routes.CANJES),
    STORE   ("Tienda",   Icons.Rounded.Storefront,   Routes.STORE),
    RANKINGS("Ranking",  Icons.Rounded.EmojiEvents,  Routes.RANKINGS),
    PROFILE ("Perfil",   Icons.Rounded.Person,       Routes.PROFILE)
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
            modifier                = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement   = Arrangement.SpaceAround,
            verticalAlignment       = Alignment.CenterVertically
        ) {
            BottomNavItem.entries.forEach { item ->
                val isSelected = currentRoute == item.route
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) FlowlyAccentGlow else Color.Transparent
                        )
                        .clickable {
                            navController.navigate(item.route) {
                                launchSingleTop = true
                                restoreState    = true
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Icon(
                        imageVector        = item.icon,
                        contentDescription = item.label,
                        tint               = if (isSelected) FlowlyAccent else FlowlyMuted,
                        modifier           = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        item.label,
                        fontSize   = 9.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color      = if (isSelected) FlowlyAccent else FlowlyMuted
                    )
                }
            }
        }
        // Espacio para la barra de navegación del sistema
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
    showBanner: Boolean = true,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        containerColor      = FlowlyBg,
        contentWindowInsets = WindowInsets.statusBars,
        bottomBar           = { FlowlyBottomNav(navController, currentRoute) }
    ) { paddingValues ->
        content(paddingValues)
    }
}
