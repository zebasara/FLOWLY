package com.flowly.move.ui.screens.blockchain

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.flowly.move.data.model.RetiroBlockchain
import com.flowly.move.ui.components.*
import com.flowly.move.ui.navigation.Routes
import com.flowly.move.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BlockchainScreen(navController: NavController) {

    val vm: BlockchainViewModel = viewModel()

    val isLoading      by vm.isLoading.collectAsStateWithLifecycle()
    val config         by vm.config.collectAsStateWithLifecycle()
    val user           by vm.user.collectAsStateWithLifecycle()
    val retiros        by vm.retiros.collectAsStateWithLifecycle()
    val userCount      by vm.userCount.collectAsStateWithLifecycle()
    val uiState        by vm.uiState.collectAsStateWithLifecycle()
    val walletState    by vm.walletSaveState.collectAsStateWithLifecycle()

    // ── form state ──────────────────────────────────────────────────────────
    var walletInput    by remember { mutableStateOf("") }
    var amountInput    by remember { mutableStateOf("") }
    var showConfirm    by remember { mutableStateOf(false) }

    // Prefill wallet when user loads
    LaunchedEffect(user) {
        if (walletInput.isBlank() && user?.walletAddress?.isNotBlank() == true) {
            walletInput = user!!.walletAddress
        }
    }

    // Reset form on success
    LaunchedEffect(uiState) {
        if (uiState is BlockchainUiState.Success) {
            amountInput = ""
            showConfirm = false
            vm.resetUiState()
        }
    }

    FlowlyScaffold(navController = navController, currentRoute = Routes.CANJES) { padding ->

        if (isLoading || config == null) {
            Box(
                modifier         = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = FlowlyAccent) }
            return@FlowlyScaffold
        }

        val cfg = config!!

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(8.dp))

            // ── Header ───────────────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    if (cfg.enabled) "Retiro ${cfg.red}" else "Retiro blockchain",
                    fontSize   = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color      = FlowlyText
                )
                if (cfg.enabled) TagGreen("Activo")
            }

            if (!cfg.enabled) {
                // ── LOCKED VIEW ──────────────────────────────────────────────
                LockedBlockchainView(
                    userCount    = userCount,
                    cfg          = cfg
                )
            } else {
                // ── ACTIVE VIEW ──────────────────────────────────────────────
                ActiveBlockchainView(
                    vm           = vm,
                    user         = user,
                    retiros      = retiros,
                    uiState      = uiState,
                    walletState  = walletState,
                    walletInput  = walletInput,
                    amountInput  = amountInput,
                    showConfirm  = showConfirm,
                    cfg          = cfg,
                    userCount    = userCount,
                    onWalletChange  = { walletInput = it },
                    onAmountChange  = { amountInput = it },
                    onShowConfirm   = { showConfirm = it }
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// LOCKED VIEW
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LockedBlockchainView(
    userCount : Long,
    cfg       : com.flowly.move.data.model.BlockchainConfig
) {
    val progreso = (userCount.toFloat() / cfg.metaUsuarios.toFloat()).coerceIn(0f, 1f)

    Column(
        modifier            = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier         = Modifier
                .size(80.dp)
                .background(Color(0xFF1A2A1A), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) { Text("🔒", fontSize = 36.sp) }

        Spacer(Modifier.height(16.dp))

        Text(
            "Próximamente en blockchain",
            fontSize   = 20.sp,
            fontWeight = FontWeight.Bold,
            color      = FlowlyText,
            textAlign  = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Text(
            cfg.mensajeBloqueado,
            fontSize   = 13.sp,
            color      = FlowlyMuted,
            textAlign  = TextAlign.Center,
            lineHeight = 20.sp
        )
    }

    // Progress card
    FlowlyCard(modifier = Modifier.padding(horizontal = 16.dp)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier.fillMaxWidth()
        ) {
            Text("Progreso hacia el lanzamiento", fontSize = 12.sp, color = FlowlyMuted)
            Text(
                "%,d / %,d".format(userCount, cfg.metaUsuarios),
                fontSize   = 28.sp,
                fontWeight = FontWeight.Bold,
                color      = FlowlyAccent2,
                modifier   = Modifier.padding(top = 6.dp)
            )
            Text("usuarios registrados", fontSize = 12.sp, color = FlowlyMuted)
            Spacer(Modifier.height(12.dp))
            FlowlyProgressBar(progress = progreso)
            Spacer(Modifier.height(4.dp))
            Text("%.0f%% del objetivo".format(progreso * 100), fontSize = 12.sp, color = FlowlyMuted)
        }
    }

    Spacer(Modifier.height(12.dp))

    // Features preview
    FlowlyCard(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            "CUANDO SE ACTIVE",
            fontSize      = 10.sp,
            fontWeight    = FontWeight.SemiBold,
            color         = Color(0xFF4B6B4B),
            letterSpacing = 1.sp
        )
        Spacer(Modifier.height(10.dp))
        FeatureRow("📤", "Retirar MOVE a tu wallet", "MetaMask, Trust Wallet y otras")
        FlowlySeparator()
        FeatureRow("🌐", "Red ${cfg.red}", "Ticker: ${cfg.redTicker}")
        FlowlySeparator()
        if (cfg.tasaCambioInfo.isNotBlank()) {
            FeatureRow("💱", "Tasa de cambio", cfg.tasaCambioInfo)
            FlowlySeparator()
        }
        FeatureRow(
            "💸", "Comisión de retiro",
            "${cfg.feePercent.toInt()}% del monto retirado"
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ACTIVE VIEW
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ActiveBlockchainView(
    vm          : BlockchainViewModel,
    user        : com.flowly.move.data.model.User?,
    retiros     : List<RetiroBlockchain>,
    uiState     : BlockchainUiState,
    walletState : BlockchainUiState,
    walletInput : String,
    amountInput : String,
    showConfirm : Boolean,
    cfg         : com.flowly.move.data.model.BlockchainConfig,
    userCount   : Long,
    onWalletChange  : (String) -> Unit,
    onAmountChange  : (String) -> Unit,
    onShowConfirm   : (Boolean) -> Unit
) {
    val disponible  = user?.tokensActuales ?: 0
    val amountInt   = amountInput.toIntOrNull() ?: 0
    val fee         = if (amountInt > 0) (amountInt * cfg.feePercent / 100f).toInt().coerceAtLeast(1) else 0
    val neto        = amountInt - fee

    // ── Network info card ────────────────────────────────────────────────────
    FlowlyCard(modifier = Modifier.padding(horizontal = 16.dp)) {
        FeeRow("Red",              cfg.red)
        FlowlySeparator()
        FeeRow("Ticker",           cfg.redTicker)
        if (cfg.tasaCambioInfo.isNotBlank()) {
            FlowlySeparator()
            FeeRow("Tasa aprox.", cfg.tasaCambioInfo)
        }
        FlowlySeparator()
        FeeRow("Comisión",         "${cfg.feePercent.toInt()}% del retiro")
        FlowlySeparator()
        FeeRow("Mínimo retiro",    "%,d MOVE".format(cfg.minRetiro))
        FlowlySeparator()
        FeeRow("Máximo retiro",    "%,d MOVE".format(cfg.maxRetiro))
        FlowlySeparator()
        FeeRow("Usuarios en MOVE", "%,d".format(userCount))
    }

    Spacer(Modifier.height(16.dp))

    // ── Wallet section ───────────────────────────────────────────────────────
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        SectionLabel("TU WALLET (${cfg.red.uppercase()})")
        Spacer(Modifier.height(6.dp))
        FlowlyInput(
            value         = walletInput,
            onValueChange = onWalletChange,
            placeholder   = "0x… dirección de tu wallet",
            label         = "Wallet destino"
        )
        Spacer(Modifier.height(6.dp))

        when (walletState) {
            is BlockchainUiState.Loading ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = FlowlyAccent)
                    Spacer(Modifier.width(6.dp))
                    Text("Guardando…", fontSize = 12.sp, color = FlowlyMuted)
                }
            is BlockchainUiState.Success ->
                Text("✅ Wallet guardada", fontSize = 12.sp, color = FlowlyAccent)
            is BlockchainUiState.Error ->
                Text("❌ ${walletState.msg}", fontSize = 12.sp, color = FlowlyWarn)
            else -> {}
        }

        Spacer(Modifier.height(8.dp))
        FlowlySecondaryButton(
            text    = "Guardar wallet",
            onClick = {
                vm.saveWallet(walletInput.trim())
                vm.resetWalletState()
            },
            modifier = Modifier.fillMaxWidth()
        )
    }

    Spacer(Modifier.height(16.dp))

    // ── Withdrawal form ──────────────────────────────────────────────────────
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        SectionLabel("CANTIDAD A RETIRAR")
        Spacer(Modifier.height(6.dp))
        FlowlyInput(
            value         = amountInput,
            onValueChange = { v -> if (v.length <= 9 && v.all { it.isDigit() }) onAmountChange(v) },
            placeholder   = "Mínimo %,d MOVE".format(cfg.minRetiro),
            label         = "Cantidad MOVE",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Disponible: %,d MOVE".format(disponible),
                fontSize = 12.sp,
                color    = FlowlyMuted
            )
            Text(
                "Todo",
                fontSize = 12.sp,
                color    = FlowlyAccent,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication        = null
                ) { onAmountChange(disponible.toString()) }
            )
        }

        // Fee preview
        if (amountInt >= cfg.minRetiro) {
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0E1E0E), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF2A4A2A), RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Column {
                    ConfirmRow("Monto total",   "%,d MOVE".format(amountInt),  FlowlyText)
                    Spacer(Modifier.height(4.dp))
                    ConfirmRow("Comisión (${cfg.feePercent.toInt()}%)", "− %,d MOVE".format(fee), FlowlyWarn)
                    Spacer(Modifier.height(4.dp))
                    Divider(color = Color(0xFF2A4A2A), thickness = 0.5.dp)
                    Spacer(Modifier.height(4.dp))
                    ConfirmRow("Recibirás",     "%,d MOVE".format(neto),       FlowlyAccent)
                }
            }
        }

        // Error
        if (uiState is BlockchainUiState.Error) {
            Spacer(Modifier.height(8.dp))
            Text("⚠️ ${uiState.msg}", fontSize = 12.sp, color = FlowlyWarn)
        }

        Spacer(Modifier.height(12.dp))

        FlowlyPrimaryButton(
            text    = if (uiState is BlockchainUiState.Loading) "Procesando…" else "Retirar a mi wallet",
            onClick = { onShowConfirm(true) },
            enabled = uiState !is BlockchainUiState.Loading
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Los retiros son procesados manualmente. Recibirás una notificación cuando se acrediten.",
            fontSize   = 11.sp,
            color      = FlowlyMuted,
            lineHeight = 16.sp
        )
    }

    // ── Confirmation dialog ──────────────────────────────────────────────────
    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { onShowConfirm(false) },
            containerColor   = Color(0xFF0E1A0E),
            title = {
                Text(
                    "Confirmar retiro",
                    fontWeight = FontWeight.Bold,
                    color      = FlowlyText
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ConfirmRow("Monto",       "%,d MOVE".format(amountInt),  FlowlyText)
                    ConfirmRow("Comisión",    "%,d MOVE".format(fee),        FlowlyWarn)
                    ConfirmRow("Neto",        "%,d MOVE".format(neto),       FlowlyAccent)
                    Spacer(Modifier.height(4.dp))
                    Text("Wallet destino:", fontSize = 12.sp, color = FlowlyMuted)
                    Text(
                        walletInput.take(20) + if (walletInput.length > 20) "…" else "",
                        fontSize   = 12.sp,
                        color      = FlowlyText,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Red: ${cfg.red}",
                        fontSize = 12.sp,
                        color    = FlowlyAccent2
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.solicitarRetiro(amountInt, walletInput.trim())
                }) {
                    Text("Confirmar", color = FlowlyAccent, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { onShowConfirm(false) }) {
                    Text("Cancelar", color = FlowlyMuted)
                }
            }
        )
    }

    // ── History ──────────────────────────────────────────────────────────────
    if (retiros.isNotEmpty()) {
        Spacer(Modifier.height(20.dp))
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            SectionLabel("HISTORIAL DE RETIROS")
            Spacer(Modifier.height(8.dp))
            retiros.forEach { retiro ->
                RetiroItem(retiro = retiro, red = cfg.red)
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HELPER COMPOSABLES
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FeatureRow(icon: String, title: String, subtitle: String) {
    Row(
        modifier             = Modifier.fillMaxWidth(),
        verticalAlignment    = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(icon, fontSize = 18.sp)
        Column {
            Text(title,    fontSize = 14.sp, fontWeight = FontWeight.Medium, color = FlowlyText)
            Text(subtitle, fontSize = 12.sp, color = FlowlyMuted)
        }
    }
}

@Composable
private fun FeeRow(label: String, value: String) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.sp, color = FlowlyMuted)
        Text(value, fontSize = 12.sp, color = FlowlyAccent2, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ConfirmRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = FlowlyMuted)
        Text(value, fontSize = 13.sp, color = valueColor, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        fontSize      = 10.sp,
        fontWeight    = FontWeight.SemiBold,
        color         = Color(0xFF4B6B4B),
        letterSpacing = 1.sp
    )
}

@Composable
private fun RetiroItem(retiro: RetiroBlockchain, red: String) {
    val dateStr = remember(retiro.createdAt) {
        if (retiro.createdAt > 0)
            SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault()).format(Date(retiro.createdAt))
        else "—"
    }
    val (estadoLabel, estadoColor) = when (retiro.estado) {
        "procesado" -> "✅ Procesado" to Color(0xFF7EE621)
        "rechazado" -> "❌ Rechazado" to Color(0xFFFF6B6B)
        else        -> "⏳ Pendiente" to Color(0xFFFFB347)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0E1A0E), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFF1A3A1A), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "%,d MOVE".format(retiro.moveNeto),
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color      = FlowlyText
                )
                Text(estadoLabel, fontSize = 12.sp, color = estadoColor)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                retiro.walletDestino.let { if (it.length > 22) it.take(10) + "…" + it.takeLast(6) else it },
                fontSize   = 11.sp,
                color      = FlowlyMuted,
                fontFamily = FontFamily.Monospace
            )
            Row(
                modifier              = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(red, fontSize = 11.sp, color = FlowlyAccent2)
                Text(dateStr, fontSize = 11.sp, color = FlowlyMuted)
            }
            if (retiro.txHash.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "TX: ${retiro.txHash.take(16)}…",
                    fontSize   = 10.sp,
                    color      = Color(0xFF7EE621),
                    fontFamily = FontFamily.Monospace
                )
            }
            if (retiro.notaAdmin.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Nota: ${retiro.notaAdmin}",
                    fontSize = 11.sp,
                    color    = Color(0xFFFFB347),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
