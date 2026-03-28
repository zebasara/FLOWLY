package com.flowly.move.ui.screens.store

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import coil.request.ImageRequest
import coil.request.CachePolicy
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.flowly.move.data.model.StoreProduct
import com.flowly.move.ui.components.*
import com.flowly.move.ui.navigation.Routes
import com.flowly.move.ui.theme.*

@Composable
fun StoreScreen(navController: NavController) {
    val vm: StoreViewModel = viewModel()
    val user        by vm.user.collectAsStateWithLifecycle()
    val storeConfig by vm.storeConfig.collectAsStateWithLifecycle()
    val userCount   by vm.userCount.collectAsStateWithLifecycle()
    val isLoading   by vm.isLoading.collectAsStateWithLifecycle()

    val tokensLibres = user?.tokensActuales ?: 0
    val umbral       = storeConfig?.umbralUsuarios ?: 500
    val storeOpen    = userCount >= umbral
    val products     = storeConfig?.productos ?: emptyList()

    FlowlyScaffold(navController = navController, currentRoute = Routes.STORE) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = FlowlyAccent)
            }
            return@FlowlyScaffold
        }

        LazyVerticalGrid(
            columns             = GridCells.Fixed(2),
            modifier            = Modifier.fillMaxSize().padding(padding),
            contentPadding      = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement   = Arrangement.spacedBy(10.dp)
        ) {

            // ── Header (ocupa las 2 columnas) ────────────────────────────────
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Tienda",
                                fontSize   = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color      = FlowlyText
                            )
                            Text(
                                "%,d MOVE disponibles".format(tokensLibres),
                                fontSize = 12.sp,
                                color    = FlowlyMuted
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Banner de estado
                    FlowlyCard2(modifier = Modifier.fillMaxWidth()) {
                        if (storeOpen) {
                            Text(
                                "✅ La tienda está abierta. ¡Canjeá tus MOVE!",
                                fontSize   = 12.sp,
                                color      = FlowlySuccess,
                                lineHeight = 18.sp
                            )
                        } else {
                            Column {
                                Text(
                                    "🏪 La tienda se activa al llegar a $umbral usuarios. Podés ver los productos pero no canjearlos todavía.",
                                    fontSize   = 12.sp,
                                    color      = FlowlyWarn,
                                    lineHeight = 18.sp
                                )
                                Spacer(Modifier.height(6.dp))
                                FlowlyProgressBar(
                                    progress = (userCount.toFloat() / umbral).coerceIn(0f, 1f),
                                    color    = FlowlyAccent
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "$userCount / $umbral usuarios",
                                    fontSize = 11.sp,
                                    color    = FlowlyMuted
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }

            // ── Productos ────────────────────────────────────────────────────
            val active = products.filter { it.activo }
            if (active.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier         = Modifier.fillMaxWidth().padding(top = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No hay productos disponibles.",
                            fontSize = 14.sp,
                            color    = FlowlyMuted
                        )
                    }
                }
            } else {
                items(active, key = { it.id.ifBlank { it.nombre } }) { product ->
                    ProductCard(
                        product      = product,
                        storeOpen    = storeOpen,
                        tokensLibres = tokensLibres,
                        onCanjear    = {
                            navController.navigate(
                                Routes.confirmCanje(
                                    product.montoLabel,
                                    product.moveRequerido.toString(),
                                    product.categoria
                                )
                            )
                        }
                    )
                }
            }

            // Espacio inferior
            item(span = { GridItemSpan(maxLineSpan) }) {
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

// ── ProductCard ────────────────────────────────────────────────────────────────

@Composable
private fun ProductCard(
    product: StoreProduct,
    storeOpen: Boolean,
    tokensLibres: Int,
    onCanjear: () -> Unit
) {
    val canAfford = tokensLibres >= product.moveRequerido

    val categoryIcon = when (product.categoria) {
        "cash"  -> "🎁"
        "gift"  -> "🎁"
        "promo" -> "🏷️"
        else    -> "📦"
    }
    val categoryLabel = when (product.categoria) {
        "cash"  -> "Beneficio"
        "gift"  -> "Gift"
        "promo" -> "Promo"
        else    -> "Otro"
    }
    val categoryBg = when (product.categoria) {
        "cash"  -> TagGreenBg
        "gift"  -> TagBlueBg
        "promo" -> TagAmberBg
        else    -> FlowlyCard
    }
    val categoryColor = when (product.categoria) {
        "cash"  -> FlowlySuccess
        "gift"  -> FlowlyAccent3
        "promo" -> FlowlyWarn
        else    -> FlowlyMuted
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (canAfford || !storeOpen) 1f else 0.55f)
            .clip(RoundedCornerShape(16.dp))   // ← clip ANTES de background: evita desborde
            .background(FlowlyCard2)
    ) {

        // ── Área de imagen ───────────────────────────────────────────
        Box(
            modifier         = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .background(FlowlyCard),
            contentAlignment = Alignment.Center
        ) {
            if (product.imagenUrl.isNotBlank()) {
                val context = LocalContext.current
                // Cache key estable por ID de producto (ignora cambios de token de Storage)
                val cacheKey = product.id.ifBlank { product.nombre }
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(product.imagenUrl)
                        .diskCacheKey(cacheKey)
                        .memoryCacheKey(cacheKey)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .build(),
                    contentDescription = product.nombre,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier.fillMaxSize()
                )
                // Gradient fade en la parte inferior para legibilidad del badge de precio
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xDD0A120A))
                            )
                        )
                )
            } else {
                // Sin imagen: solo ícono
                Text(categoryIcon, fontSize = 52.sp)
            }

            // Badge de categoría — esquina superior derecha
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(7.dp)
                    .background(categoryBg, RoundedCornerShape(8.dp))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(
                    categoryLabel,
                    fontSize   = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = categoryColor
                )
            }

            // Badge de precio — esquina inferior izquierda (siempre visible)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .background(
                        color = if (canAfford) FlowlyAccent else FlowlyDanger,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    "%,d MOVE".format(product.moveRequerido),
                    fontSize   = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color      = FlowlyBg
                )
            }
        }

        // ── Contenido de texto ───────────────────────────────────────
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                product.nombre,
                fontSize   = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color      = FlowlyText,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                product.descripcion,
                fontSize   = 11.sp,
                color      = FlowlyMuted,
                lineHeight = 15.sp,
                maxLines   = 2,
                overflow   = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(8.dp))

            // ── Botón de acción ──────────────────────────────────────
            when {
                storeOpen && canAfford -> {
                    Button(
                        onClick        = onCanjear,
                        modifier       = Modifier.fillMaxWidth().height(34.dp),
                        shape          = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors         = ButtonDefaults.buttonColors(
                            containerColor = FlowlyAccent,
                            contentColor   = FlowlyBg
                        )
                    ) {
                        Text("Canjear", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                !canAfford -> {
                    Box(
                        modifier         = Modifier
                            .fillMaxWidth()
                            .height(34.dp)
                            .background(FlowlyCard, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Saldo insuficiente",
                            fontSize = 10.sp,
                            color    = FlowlyDanger
                        )
                    }
                }

                else -> {
                    Box(
                        modifier         = Modifier
                            .fillMaxWidth()
                            .height(34.dp)
                            .background(FlowlyCard, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Próximamente", fontSize = 10.sp, color = FlowlyMuted)
                    }
                }
            }
        }
    }
}
