import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import org.example.project.Datos.*

// 👇 IMPORTS PARA LA IMAGEN 👇
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource

enum class FaseJuego {
    REVELAR_ROLES, DEBATE, RESULTADOS
}

fun String.capitalizarPrimera(): String {
    if (this.isBlank()) return this
    return this.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

data class PalabraJuego(
    val palabra: String,
    val pista: String,
    val nombreGrupo: String? = null,
    val imagenUrl: String? = null
)

@Composable
fun PantallaJuego(
    coleccion: ColeccionGuardada,
    jugadores: List<String>,
    opciones: OpcionesJuego,
    onSalir: () -> Unit
) {
    // --- PREPARACIÓN ---
    val palabraElegida = remember {
        val todasLasPalabras = coleccion.elementos.flatMap { elemento ->
            when (elemento) {
                is ElementoGuardado.Individual -> listOf(
                    PalabraJuego(
                        palabra = elemento.palabra.capitalizarPrimera(),
                        pista = elemento.pista.split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                            .randomOrNull()?.capitalizarPrimera() ?: "",
                        nombreGrupo = null,
                        imagenUrl = elemento.imagenUrl
                    )
                )
                is ElementoGuardado.Conjunto -> elemento.palabras.map { p ->
                    PalabraJuego(
                        palabra = p.palabra.capitalizarPrimera(),
                        pista = p.pista.split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                            .randomOrNull()?.capitalizarPrimera() ?: "",
                        nombreGrupo = elemento.nombreConjunto.capitalizarPrimera(),
                        imagenUrl = p.imagenUrl
                    )
                }
            }
        }

        // FILTRAMOS LAS PALABRAS USADAS
        val palabrasDisponibles = if (opciones.sinRepeticiones) {
            val usadas = GestorDatos.palabrasUsadasSesion.map { it.lowercase() }
            todasLasPalabras.filter { it.palabra.lowercase() !in usadas }
        } else {
            todasLasPalabras
        }

        // Elegimos una aleatoria
        val elegida = if (palabrasDisponibles.isNotEmpty()) palabrasDisponibles.random() else todasLasPalabras.random()

        // LA REGISTRAMOS COMO USADA Y ACTUALIZAMOS CHECKPOINT
        if (opciones.sinRepeticiones) {
            GestorDatos.palabrasUsadasSesion.add(elegida.palabra)

            // AUTO-GUARDADO Y BORRADO DE CHECKPOINT
            if (GestorDatos.checkpointActivoId != null) {
                val cpActual = GestorDatos.checkpointsGlobales.find { it.id == GestorDatos.checkpointActivoId }
                if (cpActual != null) {
                    // Si con esta palabra se agotaron todas las palabras de la colección, destruimos el checkpoint
                    if (GestorDatos.palabrasUsadasSesion.size >= todasLasPalabras.size) {
                        GestorDatos.checkpointsGlobales.remove(cpActual)
                        GestorDatos.checkpointActivoId = null
                        GestorDatos.guardarCambiosMemoria()
                    } else {
                        // Si aún quedan, actualizamos las palabras usadas y la fecha
                        val cpActualizado = cpActual.copy(
                            palabrasUsadas = GestorDatos.palabrasUsadasSesion.toList(),
                            fecha = System.currentTimeMillis()
                        )
                        val index = GestorDatos.checkpointsGlobales.indexOf(cpActual)
                        GestorDatos.checkpointsGlobales[index] = cpActualizado
                        GestorDatos.guardarCambiosMemoria()
                    }
                }
            }
        }

        elegida
    }

    val ordenPase = remember { jugadores.shuffled() }
    val impostores = remember { jugadores.shuffled().take(opciones.numImpostores).toSet() }
    val ordenHablar = remember { jugadores.shuffled() }

    var fase by remember { mutableStateOf(FaseJuego.REVELAR_ROLES) }
    var indicePase by remember { mutableStateOf(0) }
    var indiceHablando by remember { mutableStateOf(0) }
    var rondaActual by remember { mutableStateOf(1) }
    var tiempoRestante by remember { mutableStateOf(opciones.tiempoMinutos * 60) }

    var ganadores by remember { mutableStateOf("") }
    var mensajeVictoria by remember { mutableStateOf("") }
    var mostrarDialogoVotar by remember { mutableStateOf(false) }

    // NUEVAS VARIABLES PARA LA ÚLTIMA OPORTUNIDAD
    var impostorDescubierto by remember { mutableStateOf(false) }
    var adivinandoPalabra by remember { mutableStateOf(false) }
    var tiempoAdivinar by remember { mutableStateOf(15) }
    var textoBusquedaImpostor by remember { mutableStateOf("") } // Buscador del impostor

    // LISTA PLANA DE TODAS LAS PALABRAS PARA QUE EL IMPOSTOR ELIJA
    val todasLasPalabrasStrings = remember(coleccion) {
        coleccion.elementos.flatMap { el ->
            when (el) {
                is ElementoGuardado.Individual -> listOf(el.palabra.capitalizarPrimera())
                is ElementoGuardado.Conjunto -> el.palabras.map { it.palabra.capitalizarPrimera() }
            }
        }.distinct().sorted()
    }

    var haMiradoActual by remember { mutableStateOf(false) }

    // Temporizador de Debate
    LaunchedEffect(fase) {
        if (fase == FaseJuego.DEBATE && opciones.limiteTiempo) {
            while (tiempoRestante > 0 && fase == FaseJuego.DEBATE) {
                delay(1000L)
                tiempoRestante--
                if (tiempoRestante <= 0) {
                    ganadores = "IMPOSTOR"
                    mensajeVictoria = "¡Se agotó el tiempo! Los impostores han ganado por sigilo."
                    fase = FaseJuego.RESULTADOS
                }
            }
        }
    }

    // Temporizador para la última oportunidad del impostor
    LaunchedEffect(adivinandoPalabra) {
        if (adivinandoPalabra) {
            tiempoAdivinar = 15
            textoBusquedaImpostor = "" // Reseteamos el buscador al empezar
            while (tiempoAdivinar > 0 && adivinandoPalabra) {
                delay(1000L)
                tiempoAdivinar--
            }
            if (tiempoAdivinar <= 0 && adivinandoPalabra) {
                ganadores = "CIVILES"
                mensajeVictoria = "¡Se agotó el tiempo! El impostor no logró adivinar la palabra."
                impostorDescubierto = false
                adivinandoPalabra = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF9F9F9))) {

        // CABECERA
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onSalir) { Icon(Icons.Rounded.Close, contentDescription = "Salir", tint = Color.Gray) }
            Spacer(modifier = Modifier.weight(1f))
            if (fase == FaseJuego.DEBATE) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (opciones.limiteTiempo) {
                        val min = (tiempoRestante / 60).toString().padStart(2, '0')
                        val sec = (tiempoRestante % 60).toString().padStart(2, '0')
                        Text("$min:$sec", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = if (tiempoRestante < 30) Color.Red else Color.Black)
                    }
                    Text(if (opciones.limiteRondas) "Ronda $rondaActual / ${opciones.rondas}" else "Ronda $rondaActual", fontWeight = FontWeight.Bold, color = Color(0xFFFF6D00))
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(48.dp))
        }

        when (fase) {
            FaseJuego.REVELAR_ROLES -> {
                val jugadorActual = ordenPase[indicePase]
                val esImpostor = impostores.contains(jugadorActual)
                val esUltimo = indicePase == ordenPase.size - 1
                val offsetY = remember { Animatable(0f) }
                val coroutineScope = rememberCoroutineScope()

                Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(jugadorActual, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Text("Desliza para ver tu rol", color = Color.Gray)

                    Spacer(modifier = Modifier.height(32.dp))

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        // REVELACIÓN (Fondo)
                        Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(24.dp)).background(Color.White).border(2.dp, if(esImpostor) Color(0xFFFF3D00) else Color(0xFF18C1A8), RoundedCornerShape(24.dp)).padding(24.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                                // TEMA GENERAL
                                Text(
                                    text = "TEMA: ${coleccion.categoria.uppercase()}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.Gray,
                                    letterSpacing = 2.sp
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                // TÍTULO DEL ROL
                                Text(
                                    text = if(esImpostor) "IMPOSTOR" else "CIVIL",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if(esImpostor) Color(0xFFFF3D00) else Color(0xFF18C1A8)
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                val textoGrupo = palabraElegida.nombreGrupo?.let { " [$it]" } ?: ""

                                if (esImpostor) {
                                    // EL IMPOSTOR NUNCA VE LA FOTO. Solo la pista (si está activada).
                                    if (opciones.pistaParaImpostor) {
                                        Text("Pista: ${palabraElegida.pista}", textAlign = TextAlign.Center, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                    } else {
                                        Text("¡No te descubras!", textAlign = TextAlign.Center, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                    }
                                } else {
                                    // LOS CIVILES SÍ VEN LA FOTO (si existe)
                                    palabraElegida.imagenUrl?.let { url ->
                                        KamelImage(
                                            resource = asyncPainterResource(url),
                                            contentDescription = "Imagen secreta",
                                            modifier = Modifier
                                                .size(160.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .border(2.dp, Color(0xFF18C1A8), RoundedCornerShape(16.dp)),
                                            contentScale = ContentScale.Fit
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                    }

                                    Text("Palabra: ${palabraElegida.palabra}$textoGrupo", textAlign = TextAlign.Center, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        // TAPA (Frente)
                        Box(modifier = Modifier.offset { IntOffset(0, offsetY.value.roundToInt()) }.fillMaxSize().clip(RoundedCornerShape(24.dp)).background(Color(0xFF181818)).pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragEnd = { coroutineScope.launch { offsetY.animateTo(0f, spring()) } },
                                onVerticalDrag = { _, dragAmount ->
                                    coroutineScope.launch {
                                        val nuevoY = (offsetY.value + dragAmount).coerceIn(-1500f, 0f)
                                        offsetY.snapTo(nuevoY)

                                        if (nuevoY < -150f) {
                                            haMiradoActual = true
                                        }
                                    }
                                }
                            )
                        }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.SwipeUp, contentDescription = null, tint = Color.White, modifier = Modifier.size(64.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = {
                            if (esUltimo) fase = FaseJuego.DEBATE
                            else {
                                indicePase++
                                haMiradoActual = false
                                coroutineScope.launch { offsetY.snapTo(0f) }
                            }
                        },
                        enabled = haMiradoActual,
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (esUltimo) Color(0xFFFF6D00) else Color.Black,
                            disabledContainerColor = Color.LightGray
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = if (!haMiradoActual) "MIRA TU ROL" else if (esUltimo) "COMENZAR" else "SIGUIENTE",
                            fontWeight = FontWeight.Bold,
                            color = if (haMiradoActual) Color.White else Color.DarkGray
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp)) // Reducido para dar más aire en móviles pequeños
                }
            }

            FaseJuego.DEBATE -> {
                val jugadorHablando = ordenHablar[indiceHablando]
                Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                    // Contenido central flexible
                    Column(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Turno de hablar:", color = Color.Gray)
                        Text(jugadorHablando, fontSize = 42.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    }

                    // Botones anclados abajo
                    Button(onClick = {
                        indiceHablando = (indiceHablando + 1) % ordenHablar.size
                        if (indiceHablando == 0) {
                            rondaActual++
                            if (opciones.limiteRondas && rondaActual > opciones.rondas) {
                                ganadores = "IMPOSTOR"
                                mensajeVictoria = "¡Límite de rondas alcanzado! Los impostores ganan."
                                fase = FaseJuego.RESULTADOS
                            }
                        }
                    }, modifier = Modifier.fillMaxWidth().height(60.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Black), shape = RoundedCornerShape(16.dp)) {
                        Text("SIGUIENTE HABLANTE", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(onClick = { mostrarDialogoVotar = true }, modifier = Modifier.fillMaxWidth().height(60.dp), border = BorderStroke(2.dp, Color(0xFFFF3D00)), shape = RoundedCornerShape(16.dp)) {
                        Text("VOTAR IMPOSTOR", color = Color(0xFFFF3D00), fontWeight = FontWeight.Bold)
                    }
                }
            }

            FaseJuego.RESULTADOS -> {
                if (impostorDescubierto && !adivinandoPalabra) {
                    // --- PANTALLA 1: IMPOSTOR DESCUBIERTO (Aviso previo) ---
                    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                        // Contenido central flexible
                        Column(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("¡IMPOSTOR DESCUBIERTO!", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFF6D00), textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(mensajeVictoria, textAlign = TextAlign.Center, fontSize = 18.sp)
                            Spacer(modifier = Modifier.height(24.dp))
                            Text("Pero espera... El impostor tiene una última oportunidad para robar la victoria si adivina la palabra secreta.", textAlign = TextAlign.Center, fontSize = 16.sp, color = Color.Gray)
                        }

                        // Botones anclados abajo
                        Button(
                            onClick = { adivinandoPalabra = true },
                            modifier = Modifier.fillMaxWidth().height(80.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("INTENTAR ADIVINAR", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text("¡Cuidado! Solo tendrás 15 segundos", color = Color(0xFFFF6D00), fontSize = 12.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = {
                                ganadores = "CIVILES"
                                mensajeVictoria = "El impostor se ha rendido. ¡Victoria total para los civiles!"
                                impostorDescubierto = false
                            },
                            modifier = Modifier.fillMaxWidth().height(60.dp),
                            border = BorderStroke(2.dp, Color.Gray),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("RENDIRSE", color = Color.Gray, fontWeight = FontWeight.Bold)
                        }
                    }
                } else if (adivinandoPalabra) {
                    // --- PANTALLA 2: EL RELOJ ESTÁ CORRIENDO ---
                    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("¡RÁPIDO!", color = Color.Gray, fontWeight = FontWeight.Bold)
                        Text("00:${tiempoAdivinar.toString().padStart(2, '0')}", fontSize = 64.sp, fontWeight = FontWeight.ExtraBold, color = if(tiempoAdivinar <= 5) Color.Red else Color.Black)
                        Spacer(modifier = Modifier.height(16.dp))

                        // BUSCADOR IMPLEMENTADO AQUÍ
                        OutlinedTextField(
                            value = textoBusquedaImpostor,
                            onValueChange = { textoBusquedaImpostor = it },
                            placeholder = { Text("Buscar palabra...") },
                            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = Color.Gray) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        val palabrasFiltradasImpostor = todasLasPalabrasStrings.filter {
                            it.contains(textoBusquedaImpostor, ignoreCase = true)
                        }

                        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(palabrasFiltradasImpostor) { palabraOpcion ->
                                Button(
                                    onClick = {
                                        if (palabraOpcion.equals(palabraElegida.palabra, ignoreCase = true)) {
                                            ganadores = "IMPOSTOR"
                                            mensajeVictoria = "¡Increíble! El impostor adivinó la palabra ($palabraOpcion) y ha robado la victoria."
                                        } else {
                                            ganadores = "CIVILES"
                                            mensajeVictoria = "El impostor pensó que era '$palabraOpcion' y falló. ¡Los civiles ganan!"
                                        }
                                        adivinandoPalabra = false
                                        impostorDescubierto = false
                                        textoBusquedaImpostor = "" // Reseteamos por si acaso
                                    },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                    elevation = ButtonDefaults.buttonElevation(2.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(palabraOpcion, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                }
                            }
                        }
                    }
                } else {
                    // --- PANTALLA 3: RESULTADOS FINALES ---
                    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {

                        // Contenido central flexible
                        Column(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("VICTORIA PARA", fontSize = 20.sp, color = Color.Gray)
                            Text(
                                text = ganadores,
                                fontSize = 48.sp, // Tamaño un pelín reducido para evitar cortes
                                fontWeight = FontWeight.ExtraBold,
                                color = if(ganadores == "CIVILES") Color(0xFF18C1A8) else Color(0xFFFF3D00)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(mensajeVictoria, textAlign = TextAlign.Center, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(24.dp)) // Espaciador reducido de 40 a 24

                            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {

                                    palabraElegida.imagenUrl?.let { url ->
                                        KamelImage(
                                            resource = asyncPainterResource(url),
                                            contentDescription = "Imagen secreta revelada",
                                            modifier = Modifier
                                                .size(120.dp) // Reducido de 140 a 120 para asegurar que quepa
                                                .clip(RoundedCornerShape(16.dp))
                                                .border(
                                                    width = 3.dp,
                                                    color = if(ganadores == "CIVILES") Color(0xFF18C1A8) else Color(0xFFFF3D00),
                                                    shape = RoundedCornerShape(16.dp)
                                                ),
                                            contentScale = ContentScale.Fit
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                    }

                                    val textoGrupo = palabraElegida.nombreGrupo?.let { " [$it]" } ?: ""
                                    Text(
                                        text = "Palabra secreta:\n${palabraElegida.palabra}$textoGrupo",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 20.sp, // Reducido ligeramente
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Impostores:\n${impostores.joinToString(", ")}",
                                        color = Color.Gray,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Botón fijado completamente al fondo
                        Button(onClick = onSalir, modifier = Modifier.fillMaxWidth().height(60.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Black)) {
                            Text("VOLVER AL MENÚ", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (mostrarDialogoVotar) {
        DialogoVotacionFinal(
            jugadores = jugadores,
            impostores = impostores,
            onVotoConfirmado = { haGanadoCivil, nombreExpulsado ->
                mostrarDialogoVotar = false
                if (haGanadoCivil) {
                    impostorDescubierto = true
                    mensajeVictoria = "Habéis atrapado a $nombreExpulsado."
                } else {
                    ganadores = "IMPOSTOR"
                    mensajeVictoria = "¡ERROR! $nombreExpulsado era inocente. Los impostores ganan por votación errónea."
                    impostorDescubierto = false
                }
                fase = FaseJuego.RESULTADOS
            },
            onVolver = { mostrarDialogoVotar = false }
        )
    }
}

@Composable
fun DialogoVotacionFinal(
    jugadores: List<String>,
    impostores: Set<String>,
    onVotoConfirmado: (Boolean, String) -> Unit,
    onVolver: () -> Unit
) {
    var seleccionado by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onVolver) {
        Card(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("¿Quién es el impostor?", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Si falláis, perderéis la partida.", fontSize = 14.sp, color = Color.Red)

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(jugadores) { jugador ->
                        val esSeleccionado = seleccionado == jugador
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable { seleccionado = jugador },
                            shape = RoundedCornerShape(12.dp),
                            color = if (esSeleccionado) Color(0xFFFF6D00) else Color(0xFFF0F0F0)
                        ) {
                            Text(
                                text = jugador,
                                modifier = Modifier.padding(16.dp),
                                fontWeight = FontWeight.Bold,
                                color = if (esSeleccionado) Color.White else Color.Black
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onVolver,
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("VOLVER", color = Color.Gray)
                    }

                    Button(
                        onClick = {
                            seleccionado?.let {
                                val esImpostor = impostores.contains(it)
                                onVotoConfirmado(esImpostor, it)
                            }
                        },
                        enabled = seleccionado != null,
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF18C1A8))
                    ) {
                        Text("REVELAR", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}