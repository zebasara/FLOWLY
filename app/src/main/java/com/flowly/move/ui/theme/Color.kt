package com.flowly.move.ui.theme

import androidx.compose.ui.graphics.Color

// ── Background surfaces (depth system) ──────────────────────
val FlowlyBg         = Color(0xFF080F08)  // deepest bg — near-black green
val FlowlySurface    = Color(0xFF0D160D)  // intermediate surface
val FlowlyCard       = Color(0xFF121C12)  // main card surface
val FlowlyCardBottom = Color(0xFF101A10)  // card gradient endpoint (bottom)
val FlowlyCard2      = Color(0xFF182618)  // elevated card / input bg
val FlowlyCard3      = Color(0xFF1E2E1E)  // most elevated surface

// ── Borders ──────────────────────────────────────────────────
val FlowlyBorder       = Color(0xFF1E3020)  // default border
val FlowlyBorderBright = Color(0xFF384E38)  // active / focused / hover border

// ── Accent ───────────────────────────────────────────────────
val FlowlyAccent     = Color(0xFF7EE621)  // lime green — primary brand (unchanged)
val FlowlyAccentDark = Color(0xFF4CAF10)  // darker accent for gradient endpoints
val FlowlyAccentGlow = Color(0x1A7EE621)  // ~10% opacity — ambient glow / nav tint
val FlowlyAccent2    = Color(0xFFF59E0B)  // amber — secondary
val FlowlyAccent3    = Color(0xFF38BDF8)  // sky blue — tertiary
val FlowlyPurple     = Color(0xFFA78BFA)

// ── Text ─────────────────────────────────────────────────────
val FlowlyText    = Color(0xFFF0F7F0)  // primary — faint green tint
val FlowlyTextSub = Color(0xFFAABEAA)  // secondary — muted with green warmth
val FlowlyMuted   = Color(0xFF6A7A6A)  // muted text

// ── Semantic ─────────────────────────────────────────────────
val FlowlyDanger  = Color(0xFFFF6B6B)
val FlowlySuccess = Color(0xFF34D399)
val FlowlyWarn    = Color(0xFFFBBF24)

// ── Tag backgrounds ──────────────────────────────────────────
val TagGreenBg  = Color(0xFF0D3A10)
val TagAmberBg  = Color(0xFF3A2A08)
val TagRedBg    = Color(0xFF3A0F0F)
val TagBlueBg   = Color(0xFF0A1E38)
val TagPurpleBg = Color(0xFF1A1438)
