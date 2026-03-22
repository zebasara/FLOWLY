package com.flowly.move.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flowly.move.ui.theme.*

/**
 * Dropdown selector estilo FlowlyInput.
 * Muestra una lista de opciones con búsqueda nativa de Material3.
 *
 * @param selectedValue  valor actualmente seleccionado (puede ser "")
 * @param onValueChange  callback cuando el usuario elige una opción
 * @param label          texto del label superior
 * @param options        lista de opciones disponibles
 * @param enabled        false = campo deshabilitado (ciudad hasta que haya provincia)
 * @param placeholder    texto cuando no hay selección
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationDropdown(
    selectedValue: String,
    onValueChange: (String) -> Unit,
    label: String,
    options: List<String>,
    enabled: Boolean = true,
    placeholder: String = "Seleccionar…",
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    // Cerrar si la lista queda vacía (ej. cambio de provincia)
    LaunchedEffect(options) {
        if (options.isEmpty()) expanded = false
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            label,
            fontSize = 12.sp,
            color    = if (enabled) FlowlyMuted else FlowlyMuted.copy(alpha = 0.5f),
            modifier = Modifier.padding(bottom = 4.dp)
        )

        ExposedDropdownMenuBox(
            expanded         = expanded && enabled && options.isNotEmpty(),
            onExpandedChange = { if (enabled && options.isNotEmpty()) expanded = it }
        ) {
            OutlinedTextField(
                value          = selectedValue,
                onValueChange  = {},
                readOnly       = true,
                enabled        = enabled && options.isNotEmpty(),
                placeholder    = { Text(placeholder, color = FlowlyMuted, fontSize = 13.sp) },
                trailingIcon   = {
                    ExposedDropdownMenuDefaults.TrailingIcon(
                        expanded = expanded && enabled && options.isNotEmpty()
                    )
                },
                modifier       = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                shape          = RoundedCornerShape(12.dp),
                singleLine     = true,
                colors         = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor      = FlowlyAccent,
                    unfocusedBorderColor    = FlowlyBorder,
                    focusedContainerColor   = FlowlyCard2,
                    unfocusedContainerColor = FlowlyCard2,
                    disabledContainerColor  = FlowlyCard2.copy(alpha = 0.5f),
                    disabledBorderColor     = FlowlyBorder.copy(alpha = 0.4f),
                    cursorColor             = FlowlyAccent,
                    focusedTextColor        = FlowlyText,
                    unfocusedTextColor      = FlowlyText,
                    disabledTextColor       = FlowlyMuted
                )
            )

            ExposedDropdownMenu(
                expanded         = expanded && enabled && options.isNotEmpty(),
                onDismissRequest = { expanded = false },
                modifier         = Modifier
                    .background(FlowlyCard)
                    .heightIn(max = 280.dp)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text    = { Text(option, color = FlowlyText, fontSize = 13.sp) },
                        onClick = {
                            onValueChange(option)
                            expanded = false
                        },
                        colors  = MenuDefaults.itemColors(
                            textColor = FlowlyText
                        ),
                        modifier = Modifier.background(
                            if (option == selectedValue)
                                FlowlyAccent.copy(alpha = 0.12f)
                            else
                                androidx.compose.ui.graphics.Color.Transparent
                        )
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))
    }
}
