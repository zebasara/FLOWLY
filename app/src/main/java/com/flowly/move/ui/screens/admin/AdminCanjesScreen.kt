package com.flowly.move.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.flowly.move.data.model.CanjeOferta
import com.flowly.move.ui.components.FlowlyPrimaryButton
import com.flowly.move.ui.components.FlowlySecondaryButton
import com.flowly.move.ui.theme.*

@Composable
fun AdminCanjesScreen(navController: NavController) {
    val vm: AdminCanjesViewModel = viewModel()
    val isLoading    by vm.isLoading.collectAsStateWithLifecycle()
    val uiState      by vm.uiState.collectAsStateWithLifecycle()
    val nivelMinimo  by vm.nivelMinimo.collectAsStateWithLifecycle()
    val notaMensaje  by vm.notaMensaje.collectAsStateWithLifecycle()
    val opciones     by vm.opciones.collectAsStateWithLifecycle()

    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(uiState) {
        when (uiState) {
            is AdminUiState.Success -> {
                snackbar.showSnackbar("✅ Guardado en Firestore correctamente")
                vm.clearState()
            }
            is AdminUiState.Error -> {
                snackbar.showSnackbar((uiState as AdminUiState.Error).msg)
                vm.clearState()
            }
            else -> {}
        }
    }

    Scaffold(
        containerColor = FlowlyBg,
        snackbarHost   = { SnackbarHost(snackbar) }
    ) { padding ->

        if (isLoading) {
            Box(
                modifier         = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = FlowlyAccent) }
            return@Scaffold
        }

        if (uiState is AdminUiState.Loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(FlowlyBg.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = FlowlyAccent) }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            // ── Header ────────────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "←",
                    fontSize = 20.sp,
                    color    = FlowlyMuted,
                    modifier = Modifier
                        .clickable { navController.popBackStack() }
                        .padding(end = 12.dp, top = 4.dp, bottom = 4.dp)
                )
                Column {
                    Text(
                        "⚙️ Admin · Canjes",
                        fontSize   = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color      = FlowlyText
                    )
                    Text(
                        "Los cambios se aplican al instante en la app",
                        fontSize = 11.sp,
                        color    = FlowlyMuted
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Nivel mínimo ──────────────────────────────────────────────────
            AdminSection(title = "NIVEL MÍNIMO PARA CANJEAR") {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "Nivel $nivelMinimo",
                            fontSize   = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color      = FlowlyAccent
                        )
                        Text(
                            "Los usuarios con nivel menor no ven el botón Canjear",
                            fontSize   = 11.sp,
                            color      = FlowlyMuted,
                            lineHeight = 15.sp,
                            modifier   = Modifier.padding(top = 2.dp)
                        )
                    }

                    // Controles + / -
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StepButton("-") { vm.setNivelMinimo(nivelMinimo - 1) }
                        Text(
                            nivelMinimo.toString(),
                            fontSize   = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color      = FlowlyText,
                            modifier   = Modifier.widthIn(min = 28.dp),
                        )
                        StepButton("+") { vm.setNivelMinimo(nivelMinimo + 1) }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Nota inferior ────────────────────────────────────────────────
            AdminSection(title = "NOTA INFORMATIVA (texto bajo los canjes)") {
                AdminTextField(
                    value       = notaMensaje,
                    onValueChange = vm::setNotaMensaje,
                    label       = "Mensaje",
                    singleLine  = false
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── Opciones de canje ─────────────────────────────────────────────
            AdminSection(title = "OPCIONES DE CANJE (${opciones.size})") {
                if (opciones.isEmpty()) {
                    Text(
                        "No hay opciones. Agregá una abajo.",
                        fontSize = 13.sp,
                        color    = FlowlyMuted
                    )
                } else {
                    opciones.forEachIndexed { index, oferta ->
                        CanjeOfertaRow(
                            index    = index,
                            oferta   = oferta,
                            onLabelChange  = { vm.updateLabel(index, it) },
                            onMoveChange   = { vm.updateMove(index, it) },
                            onToggleActivo = { vm.toggleActivo(index) },
                            onDelete       = { vm.removeOferta(index) }
                        )
                        if (index < opciones.lastIndex) {
                            HorizontalDivider(
                                color    = FlowlyBorder,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // ── Agregar opción ────────────────────────────────────────────────
            FlowlySecondaryButton(
                text    = "+ Agregar opción",
                onClick = { vm.addOferta() }
            )

            Spacer(Modifier.height(20.dp))

            // ── Guardar ───────────────────────────────────────────────────────
            FlowlyPrimaryButton(
                text    = "💾 Guardar en Firestore",
                enabled = uiState !is AdminUiState.Loading,
                onClick = { vm.guardar() }
            )

            // Resumen antes de guardar
            Spacer(Modifier.height(8.dp))
            ResumenGuardado(opciones = opciones)

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ── Fila editable de una opción ───────────────────────────────────────────────

@Composable
private fun CanjeOfertaRow(
    index: Int,
    oferta: CanjeOferta,
    onLabelChange: (String) -> Unit,
    onMoveChange: (String) -> Unit,
    onToggleActivo: () -> Unit,
    onDelete: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {

        // Cabecera de la fila: número + estado + botón eliminar
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(FlowlyCard2, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "#${index + 1}",
                        fontSize   = 11.sp,
                        color      = FlowlyMuted,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                // Toggle activo / inactivo
                Box(
                    modifier = Modifier
                        .background(
                            if (oferta.activo) FlowlyAccent.copy(alpha = 0.15f)
                            else FlowlyCard2,
                            RoundedCornerShape(8.dp)
                        )
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onToggleActivo() }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        if (oferta.activo) "● Activo" else "○ Inactivo",
                        fontSize   = 11.sp,
                        color      = if (oferta.activo) FlowlyAccent else FlowlyMuted,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Botón eliminar
            Box(
                modifier = Modifier
                    .background(FlowlyDanger.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onDelete() }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text("🗑 Eliminar", fontSize = 11.sp, color = FlowlyDanger)
            }
        }

        Spacer(Modifier.height(10.dp))

        // Campo Label — lo que ve el usuario ("$2.000 ARS")
        AdminTextField(
            value         = oferta.label,
            onValueChange = onLabelChange,
            label         = "Label (ej: Recompensa Nivel 1)",
            singleLine    = true
        )

        Spacer(Modifier.height(6.dp))

        // Campo MOVE — cantidad de tokens necesarios
        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AdminTextField(
                value         = if (oferta.move == 0) "" else oferta.move.toString(),
                onValueChange = onMoveChange,
                label         = "MOVE requeridos",
                singleLine    = true,
                keyboardType  = KeyboardType.Number,
                modifier      = Modifier.weight(1f)
            )
            // Preview de la tasa
            Column(horizontalAlignment = Alignment.End) {
                if (oferta.label.isNotBlank() && oferta.move > 0) {
                    Text(
                        "= ${oferta.label}",
                        fontSize   = 12.sp,
                        color      = FlowlyAccent,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "%,d MOVE".format(oferta.move),
                        fontSize = 11.sp,
                        color    = FlowlyMuted
                    )
                }
            }
        }
    }
}

// ── Resumen previo al guardado ────────────────────────────────────────────────

@Composable
private fun ResumenGuardado(opciones: List<CanjeOferta>) {
    val activas = opciones.count { it.activo }
    val invalidas = opciones.count { it.label.isBlank() || it.move <= 0 }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(FlowlyCard2, RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            "Resumen",
            fontSize   = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color      = FlowlyMuted
        )
        Text("• ${opciones.size} opciones en total · $activas activas", fontSize = 12.sp, color = FlowlyText)
        if (invalidas > 0) {
            Text(
                "⚠️ $invalidas opción(es) con datos incompletos — no se puede guardar",
                fontSize = 12.sp,
                color = FlowlyWarn
            )
        } else {
            Text("✓ Datos válidos, listo para guardar", fontSize = 12.sp, color = FlowlySuccess)
        }
    }
}

// ── Componentes de UI ─────────────────────────────────────────────────────────

@Composable
private fun AdminSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(FlowlyCard2, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            title,
            fontSize      = 10.sp,
            fontWeight    = FontWeight.SemiBold,
            color         = Color(0xFF4B6B4B),
            letterSpacing = 0.8.sp
        )
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        label         = { Text(label, fontSize = 12.sp) },
        singleLine    = singleLine,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier      = modifier,
        shape         = RoundedCornerShape(10.dp),
        colors        = OutlinedTextFieldDefaults.colors(
            focusedBorderColor      = FlowlyAccent,
            unfocusedBorderColor    = FlowlyBorder,
            focusedContainerColor   = FlowlyCard,
            unfocusedContainerColor = FlowlyCard,
            cursorColor             = FlowlyAccent,
            focusedTextColor        = FlowlyText,
            unfocusedTextColor      = FlowlyText,
            focusedLabelColor       = FlowlyAccent,
            unfocusedLabelColor     = FlowlyMuted
        )
    )
}

@Composable
private fun StepButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(FlowlyCard2, RoundedCornerShape(8.dp))
            .border(1.dp, FlowlyBorder, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = FlowlyText)
    }
}
