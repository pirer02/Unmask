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
import org.example.project.Datos.TextosTraducidos.TextosJuego
import org.example.project.Datos.TextosTraducidos.obtenerTextosJuego

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
    val imagenUrl: String? = null,
    val categoriaOrigen: String = "" // 👇 NUEVO
)

@Composable
fun PantallaJuego(
    coleccion: ColeccionGuardada,
    jugadores: List<String>,
    opciones: OpcionesJuego,
    onSalir: () -> Unit
) {
    // --- LÓGICA DE IDIOMAS ---
    val idiomaActual by GestorIdiomas.idiomaActual.collectAsState()
    val textos = obtenerTextosJuego(idiomaActual)
    // -------------------------

    val pasoTutorial = GestorDatos.pasoTutorialActual
    var mostrarAvisoSalirTutorial by remember { mutableStateOf(false) }

    // 👇 NUEVO: Estados para los diálogos de confirmación de salida
    var mostrarDialogoConfirmarSalir by remember { mutableStateOf(false) }
    var mostrarDialogoConfirmarAnular by remember { mutableStateOf(false) }

    // Controlamos el botón de atrás del sistema
    androidx.activity.compose.BackHandler(enabled = true) {
        if (pasoTutorial in 5..7) {
            mostrarAvisoSalirTutorial = true
        } else {
            mostrarDialogoConfirmarSalir = true
        }
    }


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
                        imagenUrl = elemento.imagenUrl,
                        categoriaOrigen = elemento.categoriaOrigen.ifBlank { coleccion.categoria } // 👇 NUEVO
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
                        imagenUrl = p.imagenUrl,
                        categoriaOrigen = p.categoriaOrigen.ifBlank { coleccion.categoria } // 👇 NUEVO
                    )
                }
            }
        }

        val palabrasDisponibles = if (opciones.sinRepeticiones) {
            val usadas = GestorDatos.palabrasUsadasSesion.map { it.lowercase() }
            todasLasPalabras.filter { it.palabra.lowercase() !in usadas }
        } else {
            todasLasPalabras
        }

        val elegida = if (palabrasDisponibles.isNotEmpty()) palabrasDisponibles.random() else todasLasPalabras.random()

        if (opciones.sinRepeticiones) {
            GestorDatos.palabrasUsadasSesion.add(elegida.palabra)

            if (GestorDatos.checkpointActivoId != null) {
                val cpActual = GestorDatos.checkpointsGlobales.find { it.id == GestorDatos.checkpointActivoId }
                if (cpActual != null) {
                    if (GestorDatos.palabrasUsadasSesion.size >= todasLasPalabras.size) {
                        GestorDatos.checkpointsGlobales.remove(cpActual)
                        GestorDatos.checkpointActivoId = null
                        GestorDatos.guardarCambiosMemoria()
                    } else {
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

    var impostorDescubierto by remember { mutableStateOf(false) }
    var adivinandoPalabra by remember { mutableStateOf(false) }
    var tiempoAdivinar by remember { mutableStateOf(15) }
    var textoBusquedaImpostor by remember { mutableStateOf("") }

    val usuarioAuth by GestorAuth.usuario.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(GestorDatos.palabrasUsadasSesion.size) {
        if (opciones.sinRepeticiones && GestorDatos.checkpointActivoId != null) {
            usuarioAuth?.uid?.let { uid ->
                GestorDatos.subirCheckpointsNube(uid)
            }
        }
    }

    val todasLasPalabrasStrings = remember(coleccion) {
        coleccion.elementos.flatMap { el ->
            when (el) {
                is ElementoGuardado.Individual -> listOf(el.palabra.capitalizarPrimera())
                is ElementoGuardado.Conjunto -> el.palabras.map { it.palabra.capitalizarPrimera() }
            }
        }.distinct().sorted()
    }

    var haMiradoActual by remember { mutableStateOf(false) }

    LaunchedEffect(fase) {
        if (fase == FaseJuego.DEBATE && opciones.limiteTiempo) {
            while (tiempoRestante > 0 && fase == FaseJuego.DEBATE) {
                delay(1000L)
                tiempoRestante--
                if (tiempoRestante <= 0) {
                    ganadores = "IMPOSTOR"
                    mensajeVictoria = textos.msgTiempoAgotadoDebate
                    fase = FaseJuego.RESULTADOS
                    if (GestorDatos.pasoTutorialActual == 6) GestorDatos.avanzarTutorial(7)
                }
            }
        }
    }

    LaunchedEffect(adivinandoPalabra) {
        if (adivinandoPalabra) {
            tiempoAdivinar = 15
            textoBusquedaImpostor = ""
            while (tiempoAdivinar > 0 && adivinandoPalabra) {
                delay(1000L)
                tiempoAdivinar--
            }
            if (tiempoAdivinar <= 0 && adivinandoPalabra) {
                ganadores = "CIVILES"
                mensajeVictoria = textos.msgTiempoAgotadoAdivinar
                impostorDescubierto = false
                adivinandoPalabra = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF9F9F9))) {

        // CABECERA
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {
                if (pasoTutorial in 5..7) mostrarAvisoSalirTutorial = true
                else mostrarDialogoConfirmarSalir = true // 👇 MODIFICADO: Ahora pregunta antes de volver
            }) {
                Icon(Icons.Rounded.Close, contentDescription = "Salir", tint = Color.Gray)
            }
            Spacer(modifier = Modifier.weight(1f))
            if (fase == FaseJuego.DEBATE) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (opciones.limiteTiempo) {
                        val min = (tiempoRestante / 60).toString().padStart(2, '0')
                        val sec = (tiempoRestante % 60).toString().padStart(2, '0')
                        Text("$min:$sec", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = if (tiempoRestante < 30) Color.Red else Color.Black)
                    }
                    Text(if (opciones.limiteRondas) "${textos.txtRonda} $rondaActual / ${opciones.rondas}" else "${textos.txtRonda} $rondaActual", fontWeight = FontWeight.Bold, color = Color(0xFFFF6D00))
                }
            }
            Spacer(modifier = Modifier.weight(1f))

            // 👇 NUEVO: Botón arriba a la derecha para anular la partida sin gastar palabra
            if (opciones.sinRepeticiones && fase != FaseJuego.RESULTADOS && pasoTutorial !in 5..7) {
                TextButton(onClick = { mostrarDialogoConfirmarAnular = true }) {
                    Icon(Icons.Rounded.DeleteForever, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(textos.btnPartidaInvalida, color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            } else {
                Spacer(modifier = Modifier.width(48.dp)) // Mantiene el centrado si el botón no está
            }
        }

        // ESPACIADOR DINÁMICO PARA EL TUTORIAL
        if (pasoTutorial in 5..7) {
            Spacer(modifier = Modifier.height(180.dp))
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
                    Text(textos.txtDesliza, color = Color.Gray)

                    Spacer(modifier = Modifier.height(32.dp))

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(24.dp)).background(Color.White).border(2.dp, if(esImpostor) Color(0xFFFF3D00) else Color(0xFF18C1A8), RoundedCornerShape(24.dp)).padding(24.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                // 👇 NUEVO: Mostramos la categoría de la que viene la palabra para dar contexto a todos (¡incluido el impostor!)
                                Text(text = "${textos.txtTema} ${palabraElegida.categoriaOrigen.uppercase()}", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color.Gray, letterSpacing = 2.sp)

                                Spacer(modifier = Modifier.height(16.dp))
                                Text(text = if(esImpostor) textos.rolImpostor else textos.rolCivil, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = if(esImpostor) Color(0xFFFF3D00) else Color(0xFF18C1A8))
                                Spacer(modifier = Modifier.height(16.dp))

                                val textoGrupo = palabraElegida.nombreGrupo?.let { " [$it]" } ?: ""

                                if (esImpostor) {
                                    if (opciones.pistaParaImpostor) {
                                        Text("${textos.txtPista} ${palabraElegida.pista}", textAlign = TextAlign.Center, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                    } else {
                                        Text(textos.txtNoTeDescubras, textAlign = TextAlign.Center, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                    }
                                } else {
                                    palabraElegida.imagenUrl?.let { url ->
                                        KamelImage(resource = asyncPainterResource(url), contentDescription = "Imagen secreta", modifier = Modifier.size(160.dp).clip(RoundedCornerShape(16.dp)).border(2.dp, Color(0xFF18C1A8), RoundedCornerShape(16.dp)), contentScale = ContentScale.Fit)
                                        Spacer(modifier = Modifier.height(16.dp))
                                    }
                                    Text("${textos.txtPalabra} ${palabraElegida.palabra}$textoGrupo", textAlign = TextAlign.Center, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Box(modifier = Modifier.offset { IntOffset(0, offsetY.value.roundToInt()) }.fillMaxSize().clip(RoundedCornerShape(24.dp)).background(Color(0xFF181818)).pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragEnd = { coroutineScope.launch { offsetY.animateTo(0f, spring()) } },
                                onVerticalDrag = { _, dragAmount ->
                                    coroutineScope.launch {
                                        val nuevoY = (offsetY.value + dragAmount).coerceIn(-1500f, 0f)
                                        offsetY.snapTo(nuevoY)
                                        if (nuevoY < -150f) haMiradoActual = true
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
                            if (esUltimo) {
                                fase = FaseJuego.DEBATE
                                if (pasoTutorial == 5) GestorDatos.avanzarTutorial(6)
                            } else {
                                indicePase++
                                haMiradoActual = false
                                coroutineScope.launch { offsetY.snapTo(0f) }
                            }
                        },
                        enabled = haMiradoActual,
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (esUltimo) Color(0xFFFF6D00) else Color.Black, disabledContainerColor = Color.LightGray),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(text = if (!haMiradoActual) textos.btnMiraRol else if (esUltimo) textos.btnComenzar else textos.btnSiguiente, fontWeight = FontWeight.Bold, color = if (haMiradoActual) Color.White else Color.DarkGray)
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            FaseJuego.DEBATE -> {
                val jugadorHablando = ordenHablar[indiceHablando]
                Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                    Column(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text(textos.txtTurnoHablar, color = Color.Gray)
                        Text(jugadorHablando, fontSize = 42.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    }
                    Button(onClick = {
                        indiceHablando = (indiceHablando + 1) % ordenHablar.size
                        if (indiceHablando == 0) {
                            rondaActual++
                            if (opciones.limiteRondas && rondaActual > opciones.rondas) {
                                ganadores = "IMPOSTOR"
                                mensajeVictoria = textos.msgLimiteRondas
                                fase = FaseJuego.RESULTADOS
                                if (GestorDatos.pasoTutorialActual == 6) GestorDatos.avanzarTutorial(7)
                            }
                        }
                    }, modifier = Modifier.fillMaxWidth().height(60.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Black), shape = RoundedCornerShape(16.dp)) {
                        Text(textos.btnSiguienteHablante, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(onClick = { mostrarDialogoVotar = true }, modifier = Modifier.fillMaxWidth().height(60.dp), border = BorderStroke(2.dp, Color(0xFFFF3D00)), shape = RoundedCornerShape(16.dp)) {
                        Text(textos.btnVotarImpostor, color = Color(0xFFFF3D00), fontWeight = FontWeight.Bold)
                    }
                }
            }

            FaseJuego.RESULTADOS -> {
                if (impostorDescubierto && !adivinandoPalabra) {
                    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                        Column(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Text(textos.tituloDescubierto, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFF6D00), textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(mensajeVictoria, textAlign = TextAlign.Center, fontSize = 18.sp)
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(textos.descDescubierto, textAlign = TextAlign.Center, fontSize = 16.sp, color = Color.Gray)
                        }
                        Button(onClick = { adivinandoPalabra = true }, modifier = Modifier.fillMaxWidth().height(80.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Black), shape = RoundedCornerShape(16.dp)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(textos.btnIntentarAdivinar, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text(textos.txtCuidado15s, color = Color(0xFFFF6D00), fontSize = 12.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(onClick = { ganadores = "CIVILES"; mensajeVictoria = textos.msgRendido; impostorDescubierto = false }, modifier = Modifier.fillMaxWidth().height(60.dp), border = BorderStroke(2.dp, Color.Gray), shape = RoundedCornerShape(16.dp)) {
                            Text(textos.btnRendirse, color = Color.Gray, fontWeight = FontWeight.Bold)
                        }
                    }
                } else if (adivinandoPalabra) {
                    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(textos.txtRapido, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Text("00:${tiempoAdivinar.toString().padStart(2, '0')}", fontSize = 64.sp, fontWeight = FontWeight.ExtraBold, color = if(tiempoAdivinar <= 5) Color.Red else Color.Black)
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(value = textoBusquedaImpostor, onValueChange = { textoBusquedaImpostor = it }, placeholder = { Text(textos.placeholderBuscar) }, leadingIcon = { Icon(Icons.Rounded.Search, null, tint = Color.Gray) }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        val palabrasFiltradasImpostor = todasLasPalabrasStrings.filter { it.contains(textoBusquedaImpostor, ignoreCase = true) }
                        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(palabrasFiltradasImpostor) { palabraOpcion ->
                                Button(onClick = {
                                    if (palabraOpcion.equals(palabraElegida.palabra, ignoreCase = true)) { ganadores = "IMPOSTOR"; mensajeVictoria = "${textos.msgImpostorAdivina1}$palabraOpcion${textos.msgImpostorAdivina2}" } else { ganadores = "CIVILES"; mensajeVictoria = "${textos.msgImpostorFalla1}$palabraOpcion${textos.msgImpostorFalla2}" }
                                    adivinandoPalabra = false; impostorDescubierto = false; textoBusquedaImpostor = ""
                                }, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.White), elevation = ButtonDefaults.buttonElevation(2.dp), shape = RoundedCornerShape(12.dp)) { Text(palabraOpcion, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 18.sp) }
                            }
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                        Column(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Text(textos.txtVictoriaPara, fontSize = 20.sp, color = Color.Gray)
                            Text(text = if(ganadores == "CIVILES") textos.txtVictoriaCiviles else textos.txtVictoriaImpostores, fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, color = if(ganadores == "CIVILES") Color(0xFF18C1A8) else Color(0xFFFF3D00))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(mensajeVictoria, textAlign = TextAlign.Center, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(24.dp))

                            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    palabraElegida.imagenUrl?.let { url ->
                                        KamelImage(resource = asyncPainterResource(url), contentDescription = "Imagen secreta revelada", modifier = Modifier.size(120.dp).clip(RoundedCornerShape(16.dp)).border(width = 3.dp, color = if(ganadores == "CIVILES") Color(0xFF18C1A8) else Color(0xFFFF3D00), shape = RoundedCornerShape(16.dp)), contentScale = ContentScale.Fit)
                                        Spacer(modifier = Modifier.height(16.dp))
                                    }
                                    val textoGrupo = palabraElegida.nombreGrupo?.let { " [$it]" } ?: ""
                                    Text(text = "${textos.txtPalabraSecreta}${palabraElegida.palabra}$textoGrupo", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, textAlign = TextAlign.Center)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(text = "${textos.txtImpostores}${impostores.joinToString(", ")}", color = Color.Gray, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onSalir, modifier = Modifier.fillMaxWidth().height(60.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Black)) {
                            Text(if (pasoTutorial == 7) textos.btnFinalizarPrueba else textos.btnVolverMenu, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (mostrarDialogoVotar) {
        DialogoVotacionFinal(
            textos = textos,
            jugadores = jugadores,
            impostores = impostores,
            onVotoConfirmado = { haGanadoCivil, nombreExpulsado ->
                mostrarDialogoVotar = false
                if (haGanadoCivil) {
                    impostorDescubierto = true
                    mensajeVictoria = "${textos.msgAtrapado}$nombreExpulsado."
                } else {
                    ganadores = "IMPOSTOR"
                    mensajeVictoria = "${textos.msgErrorInocente1}$nombreExpulsado${textos.msgErrorInocente2}"
                    impostorDescubierto = false
                }
                fase = FaseJuego.RESULTADOS
                if (GestorDatos.pasoTutorialActual == 6) GestorDatos.avanzarTutorial(7)
            },
            onVolver = { mostrarDialogoVotar = false }
        )
    }

    if (mostrarDialogoConfirmarSalir) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoConfirmarSalir = false },
            title = { Text(textos.tituloConfirmarSalir, fontWeight = FontWeight.Bold) },
            text = { Text(textos.descConfirmarSalir) },
            confirmButton = {
                TextButton(onClick = {
                    mostrarDialogoConfirmarSalir = false
                    onSalir()
                }) {
                    Text(textos.btnConfirmar, color = Color(0xFFFF6D00), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoConfirmarSalir = false }) {
                    Text(textos.btnCancelar, color = Color.Gray)
                }
            }
        )
    }

    if (mostrarDialogoConfirmarAnular) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoConfirmarAnular = false },
            title = { Text(textos.tituloConfirmarAnular, fontWeight = FontWeight.Bold) },
            text = { Text(textos.descConfirmarAnular) },
            confirmButton = {
                TextButton(onClick = {
                    mostrarDialogoConfirmarAnular = false
                    if (opciones.sinRepeticiones) {
                        // Quitamos la palabra de la lista de usadas en esta sesión
                        GestorDatos.palabrasUsadasSesion.remove(palabraElegida.palabra)

                        // Si hay un checkpoint activo, actualizamos sus datos en memoria local
                        if (GestorDatos.checkpointActivoId != null) {
                            val cpActual = GestorDatos.checkpointsGlobales.find { it.id == GestorDatos.checkpointActivoId }
                            if (cpActual != null) {
                                val cpActualizado = cpActual.copy(
                                    palabrasUsadas = GestorDatos.palabrasUsadasSesion.toList(),
                                    fecha = System.currentTimeMillis()
                                )
                                val index = GestorDatos.checkpointsGlobales.indexOf(cpActual)
                                if (index != -1) {
                                    GestorDatos.checkpointsGlobales[index] = cpActualizado
                                    GestorDatos.guardarCambiosMemoria()
                                }
                            }
                        }
                        // Sincronizamos con la nube el cambio del checkpoint
                        usuarioAuth?.uid?.let { uid ->
                            scope.launch {
                                GestorDatos.subirCheckpointsNube(uid)
                            }
                        }
                    }
                    onSalir()
                }) {
                    Text(textos.btnConfirmar, color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoConfirmarAnular = false }) {
                    Text(textos.btnCancelar, color = Color.Gray)
                }
            }
        )
    }

    if (mostrarAvisoSalirTutorial) {
        AlertDialog(
            onDismissRequest = { mostrarAvisoSalirTutorial = false },
            title = { Text(textos.dialogoSalirTutTitulo) },
            text = { Text(textos.dialogoSalirTutDesc) },
            confirmButton = {
                TextButton(onClick = { mostrarAvisoSalirTutorial = false }) {
                    Text(textos.btnEntendido, color = Color(0xFF18C1A8), fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun DialogoVotacionFinal(
    textos: TextosJuego,
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
                Text(textos.tituloVotar, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(textos.descVotar, fontSize = 14.sp, color = Color.Red)

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
                        Text(textos.btnVolver, color = Color.Gray)
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
                        Text(textos.btnRevelar, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}