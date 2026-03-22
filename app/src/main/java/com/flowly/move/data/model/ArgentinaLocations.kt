package com.flowly.move.data.model

/** Lista oficial de 24 provincias de Argentina (orden alfabético). */
val PROVINCIAS_ARGENTINA = listOf(
    "Buenos Aires",
    "CABA",
    "Catamarca",
    "Chaco",
    "Chubut",
    "Córdoba",
    "Corrientes",
    "Entre Ríos",
    "Formosa",
    "Jujuy",
    "La Pampa",
    "La Rioja",
    "Mendoza",
    "Misiones",
    "Neuquén",
    "Río Negro",
    "Salta",
    "San Juan",
    "San Luis",
    "Santa Cruz",
    "Santa Fe",
    "Santiago del Estero",
    "Tierra del Fuego",
    "Tucumán"
)

/** Ciudades principales por provincia. */
val CIUDADES_POR_PROVINCIA: Map<String, List<String>> = mapOf(

    "Buenos Aires" to listOf(
        "La Plata", "Mar del Plata", "Bahía Blanca", "Quilmes", "Lanús",
        "Lomas de Zamora", "Almirante Brown", "Florencio Varela", "Moreno",
        "Tigre", "Merlo", "Tres de Febrero", "San Isidro", "Malvinas Argentinas",
        "Tandil", "Junín", "San Nicolás de los Arroyos", "Pergamino", "Zárate",
        "Olavarría", "Azul", "Luján", "Pilar", "Berisso", "Ensenada",
        "Necochea", "Campana", "Escobar", "Morón", "Ezeiza",
        "Quilmes Centro", "Avellaneda", "Berazategui", "San Martín",
        "Bernal", "Adrogué", "Temperley", "Mar del Plata Centro"
    ),

    "CABA" to listOf(
        "Palermo", "Recoleta", "Belgrano", "Caballito", "Villa Crespo",
        "San Telmo", "La Boca", "Almagro", "Flores", "Barracas",
        "Núñez", "Saavedra", "Villa del Parque", "Villa Devoto",
        "Mataderos", "Villa Lugano", "Balvanera", "Montserrat",
        "Puerto Madero", "Coghlan", "Villa Urquiza", "Parque Chacabuco",
        "Nueva Pompeya", "Villa Pueyrredón", "Vélez Sársfield"
    ),

    "Catamarca" to listOf(
        "San Fernando del Valle de Catamarca", "Belén", "Tinogasta",
        "Andalgalá", "Santa María", "Recreo", "Fray Mamerto Esquiú"
    ),

    "Chaco" to listOf(
        "Resistencia", "Presidencia Roque Sáenz Peña", "Villa Ángela",
        "Charata", "Quitilipi", "Las Breñas", "General Pinedo",
        "Barranqueras", "Fontana", "Machagai", "Napenay"
    ),

    "Chubut" to listOf(
        "Rawson", "Comodoro Rivadavia", "Trelew", "Puerto Madryn",
        "Esquel", "Rada Tilly", "Gaiman", "Dolavon", "Sarmiento",
        "Río Mayo", "Perito Moreno"
    ),

    "Córdoba" to listOf(
        "Córdoba", "Villa María", "Río Cuarto", "San Francisco",
        "Bell Ville", "Alta Gracia", "Cosquín", "Villa Carlos Paz",
        "Jesús María", "Río Segundo", "Oncativo", "Laboulaye",
        "Morteros", "Leones", "La Carlota", "Deán Funes",
        "Villa Allende", "Río Ceballos", "Unquillo", "Malagueño",
        "Arroyito", "Marcos Juárez", "Villa Nueva"
    ),

    "Corrientes" to listOf(
        "Corrientes", "Goya", "Paso de los Libres", "Mercedes",
        "Curuzú Cuatiá", "Santo Tomé", "Bella Vista",
        "Ituzaingó", "Monte Caseros", "Saladas", "Esquina"
    ),

    "Entre Ríos" to listOf(
        "Paraná", "Concordia", "Gualeguaychú", "Concepción del Uruguay",
        "Gualeguay", "Villaguay", "Victoria", "Colón",
        "San José", "La Paz", "Crespo", "Chajarí", "Diamante"
    ),

    "Formosa" to listOf(
        "Formosa", "Clorinda", "Pirané", "El Colorado",
        "Laguna Blanca", "Ingeniero Juárez", "General Lucio Victorio Mansilla"
    ),

    "Jujuy" to listOf(
        "San Salvador de Jujuy", "Palpalá", "San Pedro de Jujuy",
        "Libertador General San Martín", "Perico", "Humahuaca",
        "Tilcara", "Purmamarca", "Abra Pampa", "La Quiaca"
    ),

    "La Pampa" to listOf(
        "Santa Rosa", "General Pico", "Toay",
        "Victorica", "General Acha", "Realicó", "Guatraché"
    ),

    "La Rioja" to listOf(
        "La Rioja", "Chilecito", "Aimogasta", "Chamical",
        "Chepes", "Vinchina", "Villa Unión", "Patquía"
    ),

    "Mendoza" to listOf(
        "Mendoza", "Godoy Cruz", "Las Heras", "Guaymallén",
        "Maipú", "Luján de Cuyo", "San Rafael", "Malargüe",
        "General Alvear", "Rivadavia", "San Martín",
        "Junín", "Tunuyán", "Tupungato"
    ),

    "Misiones" to listOf(
        "Posadas", "Oberá", "Eldorado", "Puerto Iguazú",
        "Apóstoles", "Montecarlo", "Jardín América",
        "Leandro N. Alem", "Aristóbulo del Valle", "Campo Grande",
        "San Vicente", "Bernardo de Irigoyen"
    ),

    "Neuquén" to listOf(
        "Neuquén", "Cutral-Có", "Plaza Huincul",
        "San Martín de los Andes", "Zapala", "Junín de los Andes",
        "Centenario", "Villa La Angostura", "Chos Malal",
        "Plottier", "Cipolletti" // límite Río Negro pero área metropolitana
    ),

    "Río Negro" to listOf(
        "Viedma", "San Carlos de Bariloche", "Cipolletti",
        "General Roca", "Allen", "Catriel", "El Bolsón",
        "Choele Choel", "Río Colorado", "Ingeniero Jacobacci",
        "San Antonio Oeste"
    ),

    "Salta" to listOf(
        "Salta", "San Ramón de la Nueva Orán", "Tartagal",
        "General José de San Martín", "Rosario de la Frontera",
        "Cafayate", "Güemes", "Metán", "Embarcación",
        "Joaquín V. González", "Cachi"
    ),

    "San Juan" to listOf(
        "San Juan", "Rivadavia", "Rawson", "Chimbas",
        "Santa Lucía", "San Martín", "Caucete", "Pocito",
        "Zonda", "Albardón", "Angaco", "Calingasta"
    ),

    "San Luis" to listOf(
        "San Luis", "Villa Mercedes", "Merlo",
        "Justo Daract", "La Punta", "Quines",
        "Tilisarao", "Arizona"
    ),

    "Santa Cruz" to listOf(
        "Río Gallegos", "Caleta Olivia", "El Calafate",
        "Pico Truncado", "Las Heras", "Puerto Deseado",
        "San Julián", "Puerto Santa Cruz", "Comandante Luis Piedra Buena"
    ),

    "Santa Fe" to listOf(
        "Rosario", "Santa Fe", "Rafaela", "Venado Tuerto",
        "Reconquista", "Santo Tomé", "Villa Gobernador Gálvez",
        "San Lorenzo", "Cañada de Gómez", "Esperanza",
        "Casilda", "Rufino", "Firmat", "Tostado",
        "Villa Constitución", "Capitán Bermúdez"
    ),

    "Santiago del Estero" to listOf(
        "Santiago del Estero", "La Banda", "Termas de Río Hondo",
        "Frías", "Añatuya", "Loreto", "Fernández",
        "Monte Quemado", "Quimilí"
    ),

    "Tierra del Fuego" to listOf(
        "Ushuaia", "Río Grande", "Tolhuin"
    ),

    "Tucumán" to listOf(
        "San Miguel de Tucumán", "Tafí Viejo", "Concepción",
        "Aguilares", "Famaillá", "Monteros", "Yerba Buena",
        "Banda del Río Salí", "Alderetes", "Lules",
        "Simoca", "Bella Vista"
    )
)

/** Devuelve las ciudades de una provincia dada (lista vacía si no existe). */
fun ciudadesDe(provincia: String): List<String> =
    CIUDADES_POR_PROVINCIA[provincia] ?: emptyList()
