import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlin.math.max
import kotlinx.coroutines.launch
import org.example.project.Datos.*
import org.example.project.Datos.TextosTraducidos.obtenerTextosConfiguracion

@Composable
fun PantallaConfiguracion(
    coleccion: ColeccionGuardada,
    jugadores: List<String>,
    opcionesIniciales: OpcionesJuego?,
    snackbarHostState: SnackbarHostState,
    onIrAJugadores: () -> Unit,
    onIniciarJuego: (OpcionesJuego) -> Unit,
    onVolver: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    // --- LÓGICA DE IDIOMAS ---
    val idiomaActual by GestorIdiomas.idiomaActual.collectAsState()
    val textos = obtenerTextosConfiguracion(idiomaActual)
    // -------------------------

    // --- ESTADOS DE CONFIGURACIÓN ---
    var numImpostores by remember { mutableStateOf(opcionesIniciales?.numImpostores ?: 1) }
    var pistaParaImpostor by remember { mutableStateOf(opcionesIniciales?.pistaParaImpostor ?: false) }
    var limiteRondas by remember { mutableStateOf(opcionesIniciales?.limiteRondas ?: false) }
    var rondas by remember { mutableStateOf(opcionesIniciales?.rondas ?: 5) }
    var limiteTiempo by remember { mutableStateOf(opcionesIniciales?.limiteTiempo ?: false) }
    var tiempoMinutos by remember { mutableStateOf(opcionesIniciales?.tiempoMinutos ?: 10) }
    var sinRepeticiones by remember { mutableStateOf(opcionesIniciales?.sinRepeticiones ?: false) }

    // --- GESTIÓN DE DIÁLOGOS ---
    var mostrarDialogoIncompatible by remember { mutableStateOf(false) }
    var mostrarDialogoGuardarCheckpoint by remember { mutableStateOf(false) }
    var mostrarDialogoCargarCheckpoint by remember { mutableStateOf(false) }
    var mostrarRecordatorioCheckpoint by remember { mutableStateOf(false) }
    var mostrarAvisoSalirSinGuardar by remember { mutableStateOf(false) }
    var nombreNuevoCheckpoint by remember { mutableStateOf("") }

    val pasoTutorial = GestorDatos.pasoTutorialActual

    // --- LÓGICA DE DETECCIÓN DE CAMBIOS SIN GUARDAR ---
    val tieneCambiosSinGuardar = remember(
        sinRepeticiones, numImpostores, pistaParaImpostor, limiteRondas, rondas,
        limiteTiempo, tiempoMinutos, GestorDatos.palabrasUsadasSesion.size, GestorDatos.checkpointActivoId
    ) {
        if (!sinRepeticiones) return@remember false

        val cpActual = GestorDatos.checkpointsGlobales.find { it.id == GestorDatos.checkpointActivoId }

        if (cpActual == null) {
            GestorDatos.palabrasUsadasSesion.isNotEmpty()
        } else {
            val opcionesActuales = OpcionesJuego(numImpostores, pistaParaImpostor, limiteRondas, rondas, limiteTiempo, tiempoMinutos, sinRepeticiones)
            cpActual.palabrasUsadas != GestorDatos.palabrasUsadasSesion.toList() ||
                    cpActual.opciones != opcionesActuales
        }
    }

    androidx.activity.compose.BackHandler(enabled = pasoTutorial == 4 || tieneCambiosSinGuardar) {
        if (pasoTutorial == 4) {
            coroutineScope.launch { snackbarHostState.showSnackbar(textos.msgTutorialJugar) }
        } else {
            mostrarAvisoSalirSinGuardar = true
        }
    }

    val maxImpostores = max(1, jugadores.size / 3)
    LaunchedEffect(jugadores.size) {
        if (numImpostores > maxImpostores) numImpostores = maxImpostores
    }

    val totalPalabras = remember(coleccion) {
        coleccion.elementos.sumOf {
            if (it is ElementoGuardado.Individual) 1 else (it as ElementoGuardado.Conjunto).palabras.size
        }
    }
    val palabrasRestantes = if (sinRepeticiones) totalPalabras - GestorDatos.palabrasUsadasSesion.size else totalPalabras

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF9F9F9))) {
        // CABECERA
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {
                if (pasoTutorial == 4) {
                    coroutineScope.launch { snackbarHostState.showSnackbar(textos.msgTutorialJugar) }
                } else if (tieneCambiosSinGuardar) {
                    mostrarAvisoSalirSinGuardar = true
                } else {
                    onVolver()
                }
            }) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Volver")
            }
            Column {
                Text(textos.tituloConfiguracion, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(coleccion.nombre, fontSize = 14.sp, color = Color(0xFFFF6D00), fontWeight = FontWeight.Bold)
            }
        }

        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {

            if (pasoTutorial == 4) {
                Spacer(modifier = Modifier.height(200.dp))
            }

            TarjetaConfig(onClick = onIrAJugadores) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(textos.tituloJugadores, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("${jugadores.size} ${textos.descJugadores}", fontSize = 14.sp, color = if (jugadores.size < 3) Color.Red else Color.LightGray)
                    }
                    Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = Color.White)
                }
            }

            TarjetaConfig {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(textos.tituloImpostores, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(textos.descImpostores, fontSize = 12.sp, color = Color.LightGray)
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
                        Text(textos.tituloPista, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(textos.descPista, fontSize = 12.sp, color = Color.LightGray)
                    }
                    Switch(checked = pistaParaImpostor, onCheckedChange = { pistaParaImpostor = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF18C1A8)))
                }
            }

            TarjetaConfig {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(textos.tituloNoRepetir, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(textos.descNoRepetir, fontSize = 12.sp, color = Color.LightGray)
                        }
                        Switch(
                            checked = sinRepeticiones,
                            onCheckedChange = { activado ->
                                if (activado) {
                                    mostrarRecordatorioCheckpoint = true
                                } else {
                                    if (GestorDatos.checkpointActivoId != null) {
                                        mostrarDialogoIncompatible = true
                                    } else {
                                        sinRepeticiones = false
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
                                "${textos.restantes} $palabrasRestantes / $totalPalabras",
                                color = if (palabrasRestantes <= 0) Color.Red else Color(0xFFFF6D00),
                                fontWeight = FontWeight.Bold
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { mostrarDialogoCargarCheckpoint = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                    modifier = Modifier.height(32.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Text(textos.btnCargar, fontSize = 12.sp)
                                }

                                Button(
                                    onClick = { mostrarDialogoGuardarCheckpoint = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF18C1A8)),
                                    modifier = Modifier.height(32.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Text(textos.btnGuardar, fontSize = 12.sp)
                                }
                            }
                        }

                        if (GestorDatos.checkpointActivoId != null) {
                            val cp = GestorDatos.checkpointsGlobales.find { it.id == GestorDatos.checkpointActivoId }
                            if (cp != null) {
                                Text("${textos.checkpointActivo} ${cp.nombre}", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
                            }
                        }
                    }
                }
            }

            TarjetaConfig {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(textos.tituloRondas, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(if (limiteRondas) "$rondas ${textos.txtRondas}" else textos.txtIndefinido, fontSize = 12.sp, color = Color.LightGray)
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
                            Text(textos.tituloTiempo, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(if (limiteTiempo) "$tiempoMinutos ${textos.txtMinutos}" else textos.txtSinLimite, fontSize = 12.sp, color = Color.LightGray)
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
                        coroutineScope.launch { snackbarHostState.showSnackbar(textos.errorMinJugadores) }
                    } else if (sinRepeticiones && palabrasRestantes <= 0) {
                        coroutineScope.launch { snackbarHostState.showSnackbar(textos.errorNoPalabras) }
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
                Text(
                    text = if (pasoTutorial == 4) textos.btnIniciaAqui else textos.btnIniciarPartida,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(64.dp))
        }
    }

    if (mostrarAvisoSalirSinGuardar) {
        AlertDialog(
            onDismissRequest = { mostrarAvisoSalirSinGuardar = false },
            icon = { Icon(Icons.Rounded.Warning, contentDescription = null, tint = Color(0xFFFF6D00)) },
            title = { Text(textos.dialogoSalirTitulo, textAlign = TextAlign.Center) },
            text = { Text(textos.dialogoSalirDesc, textAlign = TextAlign.Center) },
            confirmButton = {
                Button(
                    onClick = {
                        mostrarAvisoSalirSinGuardar = false
                        mostrarDialogoGuardarCheckpoint = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF18C1A8)),
                    modifier = Modifier.fillMaxWidth()
                ) { Text(textos.btnGuardarPrimero) }
            },
            dismissButton = {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    TextButton(
                        onClick = {
                            mostrarAvisoSalirSinGuardar = false
                            onVolver()
                        }
                    ) { Text(textos.btnIgnorarSalir, color = Color.Red) }

                    TextButton(onClick = { mostrarAvisoSalirSinGuardar = false }) {
                        Text(textos.btnCancelar, color = Color.Gray)
                    }
                }
            }
        )
    }

    if (mostrarRecordatorioCheckpoint) {
        Dialog(onDismissRequest = { mostrarRecordatorioCheckpoint = false }) {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        IconButton(onClick = { mostrarRecordatorioCheckpoint = false }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Cerrar", tint = Color.Gray)
                        }
                    }
                    Icon(Icons.Rounded.History, contentDescription = null, tint = Color(0xFF18C1A8), modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(textos.dialogoGestionTitulo, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text(textos.dialogoGestionDesc, fontSize = 14.sp, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { mostrarRecordatorioCheckpoint = false; mostrarDialogoCargarCheckpoint = true }, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2C)), shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Rounded.CloudDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(textos.btnCargarPartida, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(onClick = { sinRepeticiones = true; GestorDatos.checkpointActivoId = null; GestorDatos.palabrasUsadasSesion.clear(); mostrarRecordatorioCheckpoint = false }, modifier = Modifier.fillMaxWidth().height(56.dp), border = BorderStroke(2.dp, Color(0xFF18C1A8)), shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Rounded.Add, contentDescription = null, tint = Color(0xFF18C1A8))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(textos.btnEmpezarCero, fontWeight = FontWeight.Bold, color = Color(0xFF18C1A8))
                    }
                }
            }
        }
    }

    if (mostrarDialogoIncompatible) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoIncompatible = false },
            title = { Text(textos.dialogoIncompatibleTitulo) },
            text = { Text(textos.dialogoIncompatibleDesc) },
            confirmButton = {
                Button(onClick = { mostrarDialogoIncompatible = false; mostrarDialogoGuardarCheckpoint = true }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF18C1A8))) {
                    Text(textos.btnGuardarProgreso)
                }
            },
            dismissButton = {
                TextButton(onClick = { sinRepeticiones = false; GestorDatos.checkpointActivoId = null; GestorDatos.palabrasUsadasSesion.clear(); mostrarDialogoIncompatible = false }) {
                    Text(textos.btnDescartarDesactivar, color = Color.Red)
                }
            }
        )
    }

    val usuarioAuth by GestorAuth.usuario.collectAsState()

    if (mostrarDialogoGuardarCheckpoint) {
        val cpsDeEstaLista = GestorDatos.checkpointsGlobales.filter { it.nombreColeccion == coleccion.nombre && it.idCreadorColeccion == coleccion.idCreador }
        AlertDialog(
            onDismissRequest = { mostrarDialogoGuardarCheckpoint = false },
            title = { Text(textos.dialogoGuardarCpTitulo) },
            text = {
                Column {
                    OutlinedTextField(value = nombreNuevoCheckpoint, onValueChange = { nombreNuevoCheckpoint = it }, label = { Text(textos.labelNombreCp) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    if (cpsDeEstaLista.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(textos.txtActualizaExistente, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        LazyColumn(modifier = Modifier.heightIn(max = 150.dp)) {
                            items(cpsDeEstaLista) { cp ->
                                Text(text = "${textos.txtActualizar} ${cp.nombre}", color = Color(0xFFFF6D00), modifier = Modifier.fillMaxWidth().clickable {
                                    val actualizado = cp.copy(
                                        fecha = System.currentTimeMillis(),
                                        palabrasUsadas = GestorDatos.palabrasUsadasSesion.toList(),
                                        opciones = OpcionesJuego(numImpostores, pistaParaImpostor, limiteRondas, rondas, limiteTiempo, tiempoMinutos, sinRepeticiones)
                                    )
                                    val index = GestorDatos.checkpointsGlobales.indexOf(cp)
                                    GestorDatos.checkpointsGlobales[index] = actualizado
                                    GestorDatos.checkpointActivoId = actualizado.id
                                    GestorDatos.guardarCambiosMemoria()

                                    usuarioAuth?.uid?.let { uid ->
                                        coroutineScope.launch { GestorDatos.subirCheckpointsNube(uid) }
                                    }

                                    mostrarDialogoGuardarCheckpoint = false
                                    coroutineScope.launch { snackbarHostState.showSnackbar(textos.msgCpActualizado) }
                                }.padding(vertical = 8.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
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

                        usuarioAuth?.uid?.let { uid ->
                            coroutineScope.launch { GestorDatos.subirCheckpointsNube(uid) }
                        }

                        mostrarDialogoGuardarCheckpoint = false
                        coroutineScope.launch { snackbarHostState.showSnackbar(textos.msgCpGuardado) }
                    }
                }, enabled = nombreNuevoCheckpoint.isNotBlank()) { Text(textos.btnCrearNuevo) }
            },
            dismissButton = { TextButton(onClick = { mostrarDialogoGuardarCheckpoint = false }) { Text(textos.btnCancelar, color = Color.Gray) } }
        )
    }

    if (mostrarDialogoCargarCheckpoint) {
        val cpsDeEstaLista = GestorDatos.checkpointsGlobales.filter { it.nombreColeccion == coleccion.nombre && it.idCreadorColeccion == coleccion.idCreador }
        AlertDialog(
            onDismissRequest = { mostrarDialogoCargarCheckpoint = false },
            title = { Text(textos.dialogoCargarCpTitulo) },
            text = {
                if (cpsDeEstaLista.isEmpty()) { Text(textos.txtNoCps) } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(cpsDeEstaLista) { cp ->
                            Surface(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                                GestorDatos.palabrasUsadasSesion.clear(); GestorDatos.palabrasUsadasSesion.addAll(cp.palabrasUsadas); GestorDatos.checkpointActivoId = cp.id
                                numImpostores = cp.opciones.numImpostores; pistaParaImpostor = cp.opciones.pistaParaImpostor; limiteRondas = cp.opciones.limiteRondas; rondas = cp.opciones.rondas; limiteTiempo = cp.opciones.limiteTiempo; tiempoMinutos = cp.opciones.tiempoMinutos; sinRepeticiones = cp.opciones.sinRepeticiones
                                mostrarDialogoCargarCheckpoint = false
                                coroutineScope.launch { snackbarHostState.showSnackbar("${textos.msgProgresoCargado} ${cp.nombre} ${textos.msgCargadoExito}".trim()) }
                            }, color = Color(0xFFE8F0FE), shape = RoundedCornerShape(8.dp)) { Text(cp.nombre, modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { mostrarDialogoCargarCheckpoint = false }) { Text(textos.btnCerrar) } }
        )
    }
}

@Composable
fun TarjetaConfig(onClick: (() -> Unit)? = null, contenido: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF181818)).let { if (onClick != null) it.clickable { onClick() } else it }.padding(20.dp)
    ) { contenido() }
}