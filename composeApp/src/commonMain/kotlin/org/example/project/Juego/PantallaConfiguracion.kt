import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlinx.coroutines.launch
import org.example.project.Datos.*

@Composable
fun PantallaConfiguracion(
    coleccion: ColeccionGuardada,
    jugadores: List<String>,
    opcionesIniciales: OpcionesJuego?, // Recibe la configuración anterior si existe
    snackbarHostState: SnackbarHostState,
    onIrAJugadores: () -> Unit,
    onIniciarJuego: (OpcionesJuego) -> Unit,
    onVolver: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    // Inicializamos con la configuración pasada por parámetro
    var numImpostores by remember { mutableStateOf(opcionesIniciales?.numImpostores ?: 1) }
    var pistaParaImpostor by remember { mutableStateOf(opcionesIniciales?.pistaParaImpostor ?: false) }
    var limiteRondas by remember { mutableStateOf(opcionesIniciales?.limiteRondas ?: false) }
    var rondas by remember { mutableStateOf(opcionesIniciales?.rondas ?: 5) }
    var limiteTiempo by remember { mutableStateOf(opcionesIniciales?.limiteTiempo ?: false) }
    var tiempoMinutos by remember { mutableStateOf(opcionesIniciales?.tiempoMinutos ?: 10) }
    var sinRepeticiones by remember { mutableStateOf(opcionesIniciales?.sinRepeticiones ?: false) }

    // 👇 NUEVOS ESTADOS PARA GESTIÓN DE CHECKPOINTS
    var mostrarDialogoIncompatible by remember { mutableStateOf(false) }
    var mostrarDialogoGuardarCheckpoint by remember { mutableStateOf(false) }
    var mostrarDialogoCargarCheckpoint by remember { mutableStateOf(false) }
    var nombreNuevoCheckpoint by remember { mutableStateOf("") }

    val maxImpostores = max(1, jugadores.size / 3)
    LaunchedEffect(jugadores.size) {
        if (numImpostores > maxImpostores) numImpostores = maxImpostores
    }

    // CÁLCULOS DEL CONTADOR DE PALABRAS
    val totalPalabras = remember(coleccion) {
        coleccion.elementos.sumOf {
            if (it is ElementoGuardado.Individual) 1 else (it as ElementoGuardado.Conjunto).palabras.size
        }
    }
    val palabrasRestantes = if (sinRepeticiones) totalPalabras - GestorDatos.palabrasUsadasSesion.size else totalPalabras

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF9F9F9))) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onVolver) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Volver") }
            Column {
                Text("Configuración", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(coleccion.nombre, fontSize = 14.sp, color = Color(0xFFFF6D00), fontWeight = FontWeight.Bold)
            }
        }

        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            TarjetaConfig(onClick = onIrAJugadores) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Jugadores", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("${jugadores.size} añadidos (Mínimo 3)", fontSize = 14.sp, color = if (jugadores.size < 3) Color.Red else Color.LightGray)
                    }
                    Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = Color.White)
                }
            }

            TarjetaConfig {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Impostores", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Máximo 1 por cada 3 jugadores", fontSize = 12.sp, color = Color.LightGray)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.background(Color(0xFF2A2A2A), RoundedCornerShape(8.dp))) {
                        IconButton(onClick = { if (numImpostores > 1) numImpostores-- }) { Icon(Icons.Rounded.Remove, contentDescription = "-", tint = Color.White) }
                        Text("$numImpostores", color = Color(0xFFFF6D00), fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(horizontal = 8.dp))
                        IconButton(onClick = { if (numImpostores < maxImpostores) numImpostores++ }) { Icon(Icons.Rounded.Add, contentDescription = "+", tint = Color.White) }
                    }
                }
            }

            TarjetaConfig {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Pista para Impostor", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Recibirá una pista vaga de la palabra", fontSize = 12.sp, color = Color.LightGray)
                    }
                    Switch(checked = pistaParaImpostor, onCheckedChange = { pistaParaImpostor = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF18C1A8)))
                }
            }

            // 👇 TARJETA NO REPETIR PALABRAS (CON NUEVAS FUNCIONES)
            TarjetaConfig {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("No repetir palabras", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Evita que salgan palabras de partidas anteriores", fontSize = 12.sp, color = Color.LightGray)
                        }
                        Switch(
                            checked = sinRepeticiones,
                            onCheckedChange = { activado ->
                                if (!activado && GestorDatos.checkpointActivoId != null) {
                                    // Si intenta desactivar con un checkpoint cargado, avisamos
                                    mostrarDialogoIncompatible = true
                                } else {
                                    sinRepeticiones = activado
                                    // Si lo activa de cero (sin checkpoint), limpiamos la sesión
                                    if (activado && GestorDatos.checkpointActivoId == null) {
                                        GestorDatos.palabrasUsadasSesion.clear()
                                    }
                                }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF18C1A8))
                        )
                    }

                    if (sinRepeticiones) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Restantes: $palabrasRestantes / $totalPalabras",
                                color = if (palabrasRestantes <= 0) Color.Red else Color(0xFFFF6D00),
                                fontWeight = FontWeight.Bold
                            )

                            // 👇 BOTONES PARA GESTIONAR CHECKPOINT
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { mostrarDialogoCargarCheckpoint = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                    modifier = Modifier.height(32.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Text("Cargar", fontSize = 12.sp)
                                }

                                Button(
                                    onClick = { mostrarDialogoGuardarCheckpoint = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF18C1A8)),
                                    modifier = Modifier.height(32.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Text("Guardar", fontSize = 12.sp)
                                }
                            }
                        }

                        if (GestorDatos.checkpointActivoId != null) {
                            val cp = GestorDatos.checkpointsGlobales.find { it.id == GestorDatos.checkpointActivoId }
                            if (cp != null) {
                                Text("Checkpoint activo: ${cp.nombre}", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
                            }
                        }
                    }
                }
            }

            TarjetaConfig {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Límite de Rondas", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(if (limiteRondas) "$rondas rondas" else "Indefinido", fontSize = 12.sp, color = Color.LightGray)
                        }
                        Switch(checked = limiteRondas, onCheckedChange = { limiteRondas = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFFF6D00)))
                    }
                    if (limiteRondas) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Slider(value = rondas.toFloat(), onValueChange = { rondas = it.toInt() }, valueRange = 1f..10f, steps = 8, colors = SliderDefaults.colors(thumbColor = Color(0xFFFF6D00), activeTrackColor = Color(0xFFFF6D00)))
                    }
                }
            }

            TarjetaConfig {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Límite de Tiempo", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(if (limiteTiempo) "$tiempoMinutos minutos" else "Sin límite", fontSize = 12.sp, color = Color.LightGray)
                        }
                        Switch(checked = limiteTiempo, onCheckedChange = { limiteTiempo = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFFF6D00)))
                    }
                    if (limiteTiempo) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Slider(value = tiempoMinutos.toFloat(), onValueChange = { tiempoMinutos = it.toInt() }, valueRange = 1f..20f, steps = 18, colors = SliderDefaults.colors(thumbColor = Color(0xFFFF6D00), activeTrackColor = Color(0xFFFF6D00)))
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (jugadores.size < 3) {
                        coroutineScope.launch { snackbarHostState.showSnackbar("Faltan jugadores. Necesitas mínimo 3.") }
                    } else if (sinRepeticiones && palabrasRestantes <= 0) {
                        coroutineScope.launch { snackbarHostState.showSnackbar("No quedan palabras nuevas. Apaga y enciende la opción o carga otro checkpoint.") }
                    } else {
                        val opciones = OpcionesJuego(numImpostores, pistaParaImpostor, limiteRondas, rondas, limiteTiempo, tiempoMinutos, sinRepeticiones)
                        onIniciarJuego(opciones)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF18C1A8)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("INICIAR PARTIDA", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(64.dp))
        }
    }

    // 👇 DIÁLOGOS DE GESTIÓN DE CHECKPOINTS 👇

    if (mostrarDialogoIncompatible) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoIncompatible = false },
            title = { Text("Checkpoint Incompatible") },
            text = { Text("No puedes desactivar esta opción mientras usas un checkpoint. ¿Quieres guardar el progreso actual primero?") },
            confirmButton = {
                Button(onClick = {
                    mostrarDialogoIncompatible = false
                    mostrarDialogoGuardarCheckpoint = true
                }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF18C1A8))) {
                    Text("GUARDAR PROGRESO")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    sinRepeticiones = false
                    GestorDatos.checkpointActivoId = null
                    GestorDatos.palabrasUsadasSesion.clear()
                    mostrarDialogoIncompatible = false
                }) {
                    Text("DESCARTAR Y DESACTIVAR", color = Color.Red)
                }
            }
        )
    }

    if (mostrarDialogoGuardarCheckpoint) {
        val cpsDeEstaLista = GestorDatos.checkpointsGlobales.filter { it.nombreColeccion == coleccion.nombre && it.idCreadorColeccion == coleccion.idCreador }

        AlertDialog(
            onDismissRequest = { mostrarDialogoGuardarCheckpoint = false },
            title = { Text("Guardar Checkpoint") },
            text = {
                Column {
                    OutlinedTextField(
                        value = nombreNuevoCheckpoint,
                        onValueChange = { nombreNuevoCheckpoint = it },
                        label = { Text("Nombre para este Checkpoint") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    if (cpsDeEstaLista.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("O actualiza uno existente:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        LazyColumn(modifier = Modifier.heightIn(max = 150.dp)) {
                            items(cpsDeEstaLista) { cp ->
                                Text(
                                    text = "Actualizar: ${cp.nombre}",
                                    color = Color(0xFFFF6D00),
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        val actualizado = cp.copy(
                                            fecha = System.currentTimeMillis(),
                                            palabrasUsadas = GestorDatos.palabrasUsadasSesion.toList(),
                                            opciones = OpcionesJuego(numImpostores, pistaParaImpostor, limiteRondas, rondas, limiteTiempo, tiempoMinutos, sinRepeticiones)
                                        )
                                        val index = GestorDatos.checkpointsGlobales.indexOf(cp)
                                        GestorDatos.checkpointsGlobales[index] = actualizado
                                        GestorDatos.checkpointActivoId = actualizado.id
                                        GestorDatos.guardarCambiosMemoria()
                                        mostrarDialogoGuardarCheckpoint = false
                                        coroutineScope.launch { snackbarHostState.showSnackbar("Checkpoint actualizado") }
                                    }.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (nombreNuevoCheckpoint.isNotBlank()) {
                            val nuevoCp = CheckpointJuego(
                                nombre = nombreNuevoCheckpoint.trim(),
                                fecha = System.currentTimeMillis(),
                                nombreColeccion = coleccion.nombre,
                                idCreadorColeccion = coleccion.idCreador,
                                palabrasUsadas = GestorDatos.palabrasUsadasSesion.toList(),
                                opciones = OpcionesJuego(numImpostores, pistaParaImpostor, limiteRondas, rondas, limiteTiempo, tiempoMinutos, sinRepeticiones)
                            )
                            GestorDatos.checkpointsGlobales.add(nuevoCp)
                            GestorDatos.checkpointActivoId = nuevoCp.id
                            GestorDatos.guardarCambiosMemoria()
                            mostrarDialogoGuardarCheckpoint = false
                            coroutineScope.launch { snackbarHostState.showSnackbar("Nuevo checkpoint creado") }
                        }
                    },
                    enabled = nombreNuevoCheckpoint.isNotBlank()
                ) {
                    Text("CREAR NUEVO")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoGuardarCheckpoint = false }) { Text("CANCELAR", color = Color.Gray) }
            }
        )
    }

    if (mostrarDialogoCargarCheckpoint) {
        val cpsDeEstaLista = GestorDatos.checkpointsGlobales.filter { it.nombreColeccion == coleccion.nombre && it.idCreadorColeccion == coleccion.idCreador }

        AlertDialog(
            onDismissRequest = { mostrarDialogoCargarCheckpoint = false },
            title = { Text("Cargar Checkpoint") },
            text = {
                if (cpsDeEstaLista.isEmpty()) {
                    Text("No tienes checkpoints guardados para esta lista.")
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(cpsDeEstaLista) { cp ->
                            Surface(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                                    // Cargamos todos los datos del checkpoint
                                    GestorDatos.palabrasUsadasSesion.clear()
                                    GestorDatos.palabrasUsadasSesion.addAll(cp.palabrasUsadas)
                                    GestorDatos.checkpointActivoId = cp.id

                                    numImpostores = cp.opciones.numImpostores
                                    pistaParaImpostor = cp.opciones.pistaParaImpostor
                                    limiteRondas = cp.opciones.limiteRondas
                                    rondas = cp.opciones.rondas
                                    limiteTiempo = cp.opciones.limiteTiempo
                                    tiempoMinutos = cp.opciones.tiempoMinutos
                                    sinRepeticiones = cp.opciones.sinRepeticiones

                                    mostrarDialogoCargarCheckpoint = false
                                    coroutineScope.launch { snackbarHostState.showSnackbar("Progreso de ${cp.nombre} cargado") }
                                },
                                color = Color(0xFFE8F0FE),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(cp.nombre, modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { mostrarDialogoCargarCheckpoint = false }) { Text("CERRAR") }
            }
        )
    }
}

@Composable
fun TarjetaConfig(onClick: (() -> Unit)? = null, contenido: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF181818)).let { if (onClick != null) it.clickable { onClick() } else it }.padding(20.dp)
    ) { contenido() }
}