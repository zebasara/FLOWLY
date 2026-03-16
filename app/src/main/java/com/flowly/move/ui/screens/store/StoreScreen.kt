package com.flowly.move.ui.screens.store

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Tienda", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = FlowlyText)
                    Text("%,d MOVE disponibles".format(tokensLibres), fontSize = 12.sp, color = FlowlyMuted)
                }
            }

            // Banner de estado de la tienda
            FlowlyCard2(modifier = Modifier.padding(horizontal = 16.dp)) {
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

            Spacer(Modifier.height(12.dp))

            if (products.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No hay productos disponibles.", fontSize = 14.sp, color = FlowlyMuted)
                }
            } else {
                products.filter { it.activo }.forEach { product ->
                    ProductCard(
                        product    = product,
                        storeOpen  = storeOpen,
                        tokensLibres = tokensLibres,
                        onCanjear  = {
                            navController.navigate(
                                Routes.confirmCanje(product.montoLabel, product.moveRequerido.toString(), product.categoria)
                            )
                        }
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ProductCard(
    product: StoreProduct,
    storeOpen: Boolean,
    tokensLibres: Int,
    onCanjear: () -> Unit
) {
    val canAfford = tokensLibres >= product.moveRequerido
    val categoryIcon = when (product.categoria) {
        "cash"  -> "💸"
        "gift"  -> "🎁"
        "promo" -> "🏷️"
        else    -> "📦"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .alpha(if (canAfford || !storeOpen) 1f else 0.6f)
            .background(FlowlyCard2, RoundedCornerShape(14.dp))
    ) {
        // Imagen / ícono
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                .background(FlowlyCard),
            contentAlignment = Alignment.Center
        ) {
            if (product.imagenUrl.isNotBlank()) {
                AsyncImage(
                    model             = product.imagenUrl,
                    contentDescription = product.nombre,
                    contentScale      = ContentScale.Crop,
                    modifier          = Modifier.fillMaxSize()
                )
            } else {
                Text(categoryIcon, fontSize = 36.sp)
            }
        }

        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(product.nombre, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = FlowlyText)
                when (product.categoria) {
                    "cash"  -> TagGreen("Efectivo")
                    "gift"  -> TagBlue("Gift")
                    "promo" -> TagAmber("Promo")
                }
            }

            Text(
                product.descripcion,
                fontSize   = 12.sp,
                color      = FlowlyMuted,
                lineHeight = 17.sp,
                modifier   = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "%,d MOVE".format(product.moveRequerido),
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color      = if (canAfford) FlowlyAccent else FlowlyDanger
                )
                if (storeOpen && canAfford) {
                    FlowlyOutlineButton(
                        text     = "Canjear",
                        onClick  = onCanjear,
                        modifier = Modifier.width(90.dp).height(36.dp)
                    )
                } else if (!canAfford) {
                    Text("Saldo insuficiente", fontSize = 11.sp, color = FlowlyDanger)
                } else {
                    Text("Próximamente", fontSize = 11.sp, color = FlowlyMuted)
                }
            }
        }
    }
}
