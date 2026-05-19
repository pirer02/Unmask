import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Importamos todos los datos necesarios
import org.example.project.Datos.*
import org.example.project.Datos.TextosTraducidos.TextosApp
import org.example.project.Datos.TextosTraducidos.obtenerTextosApp
import org.example.project.PantallaMiPerfil

enum class PantallaNavegacion {
    INICIO, BIBLIOTECA, CREAR, EXPLORAR, PERFIL, CONFIG_JUEGO, JUGADORES, JUEGO
}

@Composable
fun App(onLoginGoogle: () -> Unit = {}) {
    MaterialTheme {
        var isLoading by remember { mutableStateOf(true) }

        LaunchedEffect(Unit) {
            GestorDatos.cargarDatos()
            delay(3000)
            isLoading = false
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFFF6D00))
            }
        } else {
            ContenedorPrincipal(onLoginGoogle = onLoginGoogle)
        }
    }
}

@Composable
fun PantallaConCarga(content: @Composable () -> Unit) {
    var cargando by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(350)
        cargando = false
    }

    if (cargando) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFFFF6D00))
        }
    } else {
        content()
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ContenedorPrincipal(onLoginGoogle: () -> Unit) {
    var pantallaActual by remember { mutableStateOf(PantallaNavegacion.INICIO) }
    var pantallaAnteriorJugadores by remember { mutableStateOf(PantallaNavegacion.INICIO) }

    var coleccionAEditar by remember { mutableStateOf<ColeccionGuardada?>(null) }
    var coleccionParaJugar by remember { mutableStateOf<ColeccionGuardada?>(null) }
    var opcionesConfiguradas by remember { mutableStateOf<OpcionesJuego?>(null) }

    var pantallaOrigenEdicion by remember { mutableStateOf<PantallaNavegacion>(PantallaNavegacion.INICIO) }

    var uidPerfilExplorar by remember { mutableStateOf<String?>(null) }

    val jugadoresGlobales = GestorDatos.jugadoresGlobales
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val usuarioAuth by GestorAuth.usuario.collectAsState()
    var invitacionPendiente by remember { mutableStateOf<InvitacionColaboracion?>(null) }
    var procesandoInvitacion by remember { mutableStateOf(false) }

    val pasoTutorial = GestorDatos.pasoTutorialActual

    // --- LÓGICA DE IDIOMAS ---
    val idiomaActual by GestorIdiomas.idiomaActual.collectAsState()
    val textosApp = obtenerTextosApp(idiomaActual)
    // -------------------------

    LaunchedEffect(usuarioAuth, pantallaActual) {
        if (usuarioAuth != null) {
            val pendientes = GestorAuth.obtenerInvitacionesPendientes(usuarioAuth!!.uid)
            if (pendientes.isNotEmpty()) {
                invitacionPendiente = pendientes.first()
            } else {
                invitacionPendiente = null
            }
        }
    }

    LaunchedEffect(pasoTutorial) {
        when(pasoTutorial) {
            1 -> pantallaActual = PantallaNavegacion.INICIO
            2 -> pantallaActual = PantallaNavegacion.CREAR
            3 -> pantallaActual = PantallaNavegacion.BIBLIOTECA
            4 -> {
                if (pantallaActual != PantallaNavegacion.CONFIG_JUEGO && pantallaActual != PantallaNavegacion.JUGADORES) {
                    pantallaActual = PantallaNavegacion.CONFIG_JUEGO
                }
            }
            8 -> pantallaActual = PantallaNavegacion.EXPLORAR
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = {
                if (pantallaActual in listOf(PantallaNavegacion.INICIO, PantallaNavegacion.BIBLIOTECA, PantallaNavegacion.EXPLORAR, PantallaNavegacion.PERFIL)) {
                    FloatingActionButton(
                        onClick = {
                            if (pasoTutorial == 1) GestorDatos.avanzarTutorial(2)
                            coleccionAEditar = null
                            pantallaActual = PantallaNavegacion.CREAR
                        },
                        containerColor = if (pasoTutorial == 1) Color(0xFF18C1A8) else Color(0xFFFF6D00),
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(64.dp).offset(y = 45.dp),
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
                    ) {
                        Icon(if (pasoTutorial == 1) Icons.Rounded.AdsClick else Icons.Rounded.Add, contentDescription = "Crear", modifier = Modifier.size(36.dp))
                    }
                }
            },
            floatingActionButtonPosition = FabPosition.Center,
            bottomBar = {
                if (pantallaActual !in listOf(PantallaNavegacion.CONFIG_JUEGO, PantallaNavegacion.JUGADORES, PantallaNavegacion.JUEGO)) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        ColorAcentuationLine()
                        MenuInferiorClavado(
                            pantallaSeleccionada = pantallaActual,
                            textos = textosApp,
                            onPantallaCambiada = { nuevaPantalla ->
                                if (pasoTutorial == 99 || pasoTutorial == 0 || pasoTutorial == 8) {
                                    pantallaActual = nuevaPantalla
                                }
                            }
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                AnimatedContent(
                    targetState = pantallaActual,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(300)) + scaleIn(
                            initialScale = 0.95f,
                            animationSpec = tween(300)
                        ))
                            .togetherWith(fadeOut(animationSpec = tween(300)))
                    },
                    label = "TransicionNavegacion"
                ) { pantalla ->
                    when (pantalla) {
                        PantallaNavegacion.INICIO -> PantallaConCarga {
                            PantallaInicio(
                                onJugar = { coleccion ->
                                    coleccionParaJugar = coleccion
                                    GestorDatos.palabrasUsadasSesion.clear()
                                    GestorDatos.checkpointActivoId = null
                                    opcionesConfiguradas = null
                                    pantallaActual = PantallaNavegacion.CONFIG_JUEGO
                                },
                                onJugarCheckpoint = { coleccion, checkpoint ->
                                    coleccionParaJugar = coleccion
                                    GestorDatos.palabrasUsadasSesion.clear()
                                    GestorDatos.palabrasUsadasSesion.addAll(checkpoint.palabrasUsadas)
                                    GestorDatos.checkpointActivoId = checkpoint.id
                                    opcionesConfiguradas = checkpoint.opciones
                                    pantallaActual = PantallaNavegacion.CONFIG_JUEGO
                                },
                                onGestionarJugadores = {
                                    pantallaAnteriorJugadores = PantallaNavegacion.INICIO
                                    pantallaActual = PantallaNavegacion.JUGADORES
                                },
                                onCrearLista = {
                                    if (pasoTutorial == 1) GestorDatos.avanzarTutorial(2)
                                    coleccionAEditar = null
                                    pantallaOrigenEdicion = PantallaNavegacion.INICIO
                                    pantallaActual = PantallaNavegacion.CREAR
                                },
                                onIrAPerfil = { pantallaActual = PantallaNavegacion.PERFIL },
                                onVerPerfilAjeno = { uid ->
                                    uidPerfilExplorar = uid
                                    pantallaActual = PantallaNavegacion.EXPLORAR
                                }
                            )
                        }

                        PantallaNavegacion.BIBLIOTECA -> PantallaConCarga {
                            PantallaBiblioteca(
                                onEditar = { coleccion ->
                                    coleccionAEditar = coleccion
                                    pantallaOrigenEdicion = PantallaNavegacion.BIBLIOTECA
                                    pantallaActual = PantallaNavegacion.CREAR
                                },
                                onJugar = { coleccion ->
                                    if (pasoTutorial == 3) GestorDatos.avanzarTutorial(4)
                                    coleccionParaJugar = coleccion
                                    GestorDatos.palabrasUsadasSesion.clear()
                                    opcionesConfiguradas = null
                                    pantallaActual = PantallaNavegacion.CONFIG_JUEGO
                                },
                                onVerPerfilAjeno = { uid ->
                                    uidPerfilExplorar = uid
                                    pantallaActual = PantallaNavegacion.EXPLORAR
                                }
                            )
                        }

                        PantallaNavegacion.CREAR -> PantallaConCarga {
                            PantallaCrear(
                                coleccionParaEditar = coleccionAEditar,
                                snackbarHostState = snackbarHostState,
                                onGuardadoExitoso = {
                                    if (pasoTutorial == 2) GestorDatos.avanzarTutorial(3)
                                    coleccionAEditar = null
                                    pantallaActual = PantallaNavegacion.BIBLIOTECA
                                },
                                onVolver = {
                                    coleccionAEditar = null
                                    pantallaActual = pantallaOrigenEdicion
                                }
                            )
                        }

                        PantallaNavegacion.EXPLORAR -> PantallaConCarga {
                            PantallaExplorar(
                                uidInicial = uidPerfilExplorar,
                                onLimpiarUidInicial = { uidPerfilExplorar = null },
                                onVolver = { pantallaActual = PantallaNavegacion.INICIO },
                                onIrAPerfilLogin = { pantallaActual = PantallaNavegacion.PERFIL },
                                onJugarColeccion = { coleccion ->
                                    coleccionParaJugar = coleccion
                                    GestorDatos.palabrasUsadasSesion.clear()
                                    opcionesConfiguradas = null
                                    pantallaActual = PantallaNavegacion.CONFIG_JUEGO
                                },
                                onEditar = { coleccion ->
                                    coleccionAEditar = coleccion
                                    pantallaOrigenEdicion = PantallaNavegacion.EXPLORAR
                                    pantallaActual = PantallaNavegacion.CREAR
                                }
                            )
                        }

                        PantallaNavegacion.PERFIL -> PantallaConCarga {
                            PantallaMiPerfil(
                                onVolver = { pantallaActual = PantallaNavegacion.INICIO },
                                onIniciarSesionGoogle = onLoginGoogle,
                                onEditar = { coleccion ->
                                    coleccionAEditar = coleccion
                                    pantallaOrigenEdicion = PantallaNavegacion.PERFIL
                                    pantallaActual = PantallaNavegacion.CREAR
                                },
                                onJugar = { coleccion ->
                                    coleccionParaJugar = coleccion
                                    GestorDatos.palabrasUsadasSesion.clear()
                                    opcionesConfiguradas = null
                                    pantallaActual = PantallaNavegacion.CONFIG_JUEGO
                                },
                                onVerPerfilAjeno = { uid ->
                                    uidPerfilExplorar = uid
                                    pantallaActual = PantallaNavegacion.EXPLORAR
                                }
                            )
                        }

                        PantallaNavegacion.CONFIG_JUEGO -> {
                            coleccionParaJugar?.let { coleccion ->
                                PantallaConfiguracion(
                                    coleccion = coleccion,
                                    jugadores = jugadoresGlobales,
                                    opcionesIniciales = opcionesConfiguradas,
                                    snackbarHostState = snackbarHostState,
                                    onIrAJugadores = {
                                        pantallaAnteriorJugadores =
                                            PantallaNavegacion.CONFIG_JUEGO; pantallaActual =
                                        PantallaNavegacion.JUGADORES
                                    },
                                    onIniciarJuego = { opciones ->
                                        if (pasoTutorial == 4) GestorDatos.avanzarTutorial(5)
                                        opcionesConfiguradas = opciones; pantallaActual =
                                        PantallaNavegacion.JUEGO
                                    },
                                    onVolver = { pantallaActual = PantallaNavegacion.INICIO }
                                )
                            }
                        }

                        PantallaNavegacion.JUGADORES -> PantallaJugadores(
                            jugadores = jugadoresGlobales,
                            onVolver = { pantallaActual = pantallaAnteriorJugadores })

                        PantallaNavegacion.JUEGO -> {
                            if (coleccionParaJugar != null && opcionesConfiguradas != null) {
                                PantallaJuego(
                                    coleccion = coleccionParaJugar!!,
                                    jugadores = jugadoresGlobales.toList(),
                                    opciones = opcionesConfiguradas!!,
                                    onSalir = {
                                        if (pasoTutorial == 7) {
                                            GestorDatos.avanzarTutorial(8)
                                            pantallaActual = PantallaNavegacion.EXPLORAR
                                        } else {
                                            pantallaActual = PantallaNavegacion.CONFIG_JUEGO
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (pasoTutorial != 99) {
            TutorialOverlay(
                paso = pasoTutorial,
                pantallaActual = pantallaActual,
                textos = textosApp,
                onOmitir = { GestorDatos.terminarTutorial() },
                onSiguiente = { GestorDatos.avanzarTutorial(pasoTutorial + 1) }
            )
        }
    }

    if (invitacionPendiente != null) {
        AlertDialog(
            onDismissRequest = { },
            icon = { Icon(Icons.Rounded.Handshake, contentDescription = null, tint = Color(0xFF18C1A8)) },
            title = { Text(textosApp.colabTitulo) },
            text = {
                if (procesandoInvitacion) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF18C1A8))
                    }
                } else {
                    Text("${textosApp.colabDesc1}${invitacionPendiente?.nombreEmisor}${textosApp.colabDesc2}${invitacionPendiente?.nombreLista}${textosApp.colabDesc3}")
                }
            },
            confirmButton = {
                if (!procesandoInvitacion) {
                    Button(
                        onClick = {
                            scope.launch {
                                procesandoInvitacion = true
                                val exito = GestorAuth.responderInvitacion(invitacionPendiente!!, true)
                                if (exito) {
                                    snackbarHostState.showSnackbar(textosApp.colabExito)
                                    usuarioAuth?.uid?.let { GestorDatos.descargarDatosNube(it) }
                                }
                                invitacionPendiente = null
                                procesandoInvitacion = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF18C1A8))
                    ) { Text(textosApp.colabAceptar) }
                }
            },
            dismissButton = {
                if (!procesandoInvitacion) {
                    TextButton(onClick = { scope.launch { procesandoInvitacion = true; GestorAuth.responderInvitacion(invitacionPendiente!!, false); invitacionPendiente = null; procesandoInvitacion = false } }) { Text(textosApp.colabRechazar, color = Color.Gray) }
                }
            }
        )
    }
}

@Composable
fun ColorAcentuationLine() {
    Row(modifier = Modifier.fillMaxWidth().height(2.dp)) {
        Box(modifier = Modifier.width(50.dp).fillMaxHeight().background(Color(0xFFD4AF37)))
        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFF18C1A8)))
        Box(modifier = Modifier.weight(2f).fillMaxHeight().background(Color(0xFFFF3D00)))
    }
}

@Composable
fun MenuInferiorClavado(pantallaSeleccionada: PantallaNavegacion, textos: TextosApp, onPantallaCambiada: (PantallaNavegacion) -> Unit) {
    NavigationBar(containerColor = Color(0xFF121212), contentColor = Color.White, modifier = Modifier.padding(top = 2.dp)) {
        NavigationBarItem(icon = { Icon(Icons.Rounded.Home, contentDescription = null) }, label = { Text(textos.menuInicio, fontWeight = FontWeight.Bold, fontSize = 10.sp) }, selected = pantallaSeleccionada == PantallaNavegacion.INICIO, onClick = { onPantallaCambiada(PantallaNavegacion.INICIO) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFFFF6D00), selectedTextColor = Color(0xFFFF6D00), unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray, indicatorColor = Color.Transparent))
        NavigationBarItem(icon = { Icon(Icons.Rounded.ViewList, contentDescription = null) }, label = { Text(textos.menuBiblioteca, fontSize = 10.sp) }, selected = pantallaSeleccionada == PantallaNavegacion.BIBLIOTECA, onClick = { onPantallaCambiada(PantallaNavegacion.BIBLIOTECA) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFFFF6D00), selectedTextColor = Color(0xFFFF6D00), unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray, indicatorColor = Color.Transparent))
        NavigationBarItem(icon = {}, label = { }, selected = false, onClick = { }, enabled = false)
        NavigationBarItem(icon = { Icon(Icons.Rounded.Search, contentDescription = null) }, label = { Text(textos.menuExplorar, fontSize = 10.sp) }, selected = pantallaSeleccionada == PantallaNavegacion.EXPLORAR, onClick = { onPantallaCambiada(PantallaNavegacion.EXPLORAR) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFFFF6D00), selectedTextColor = Color(0xFFFF6D00), unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray, indicatorColor = Color.Transparent))
        NavigationBarItem(icon = { Icon(Icons.Rounded.AccountCircle, contentDescription = null) }, label = { Text(textos.menuPerfil, fontSize = 10.sp) }, selected = pantallaSeleccionada == PantallaNavegacion.PERFIL, onClick = { onPantallaCambiada(PantallaNavegacion.PERFIL) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFFFF6D00), selectedTextColor = Color(0xFFFF6D00), unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray, indicatorColor = Color.Transparent))
    }
}

@Composable
fun PantallaEnConstruccion(nombre: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Pantalla $nombre en construcción...", color = Color.Gray) }
}

@Composable
fun TutorialOverlay(
    paso: Int,
    pantallaActual: PantallaNavegacion,
    textos: TextosApp,
    onOmitir: () -> Unit,
    onSiguiente: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (paso == 0 || paso == 8) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)))
        }

        val alineacion: Alignment
        var offsetY = 0.dp

        when (paso) {
            0, 8 -> { alineacion = Alignment.Center }
            1 -> { alineacion = Alignment.Center; offsetY = (-40).dp }
            2 -> { alineacion = Alignment.BottomCenter; offsetY = 16.dp }
            3 -> { alineacion = Alignment.BottomCenter; offsetY = (-80).dp }
            4 -> {
                if (pantallaActual == PantallaNavegacion.JUGADORES) {
                    alineacion = Alignment.BottomCenter
                    offsetY = 0.dp
                } else {
                    alineacion = Alignment.TopCenter
                    offsetY = 50.dp
                }
            }
            5, 6, 7 -> { alineacion = Alignment.TopCenter; offsetY = 50.dp }
            else -> { alineacion = Alignment.Center }
        }

        val titulo: String
        val mensaje: String
        val textoBoton: String?

        when (paso) {
            0 -> { titulo = textos.tut0Titulo; mensaje = textos.tut0Desc; textoBoton = textos.tut0Btn }
            1 -> { titulo = textos.tut1Titulo; mensaje = textos.tut1Desc; textoBoton = null }
            2 -> { titulo = textos.tut2Titulo; mensaje = textos.tut2Desc; textoBoton = null }
            3 -> { titulo = textos.tut3Titulo; mensaje = textos.tut3Desc; textoBoton = null }
            4 -> {
                if (pantallaActual == PantallaNavegacion.JUGADORES) {
                    titulo = textos.tut4JugTitulo
                    mensaje = textos.tut4JugDesc
                    textoBoton = null
                } else {
                    titulo = textos.tut4ConfTitulo
                    mensaje = textos.tut4ConfDesc
                    textoBoton = null
                }
            }
            5 -> { titulo = textos.tut5Titulo; mensaje = textos.tut5Desc; textoBoton = null }
            6 -> { titulo = textos.tut6Titulo; mensaje = textos.tut6Desc; textoBoton = null }
            7 -> { titulo = textos.tut7Titulo; mensaje = textos.tut7Desc; textoBoton = null }
            8 -> { titulo = textos.tut8Titulo; mensaje = textos.tut8Desc; textoBoton = textos.tut8Btn }
            else -> { titulo = ""; mensaje = ""; textoBoton = null }
        }

        if (titulo.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                contentAlignment = alineacion
            ) {
                Card(
                    modifier = Modifier.offset(y = offsetY),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(12.dp),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(2.dp, Color(0xFF18C1A8))
                ) {
                    Column(
                        modifier = Modifier.padding(top = 16.dp, start = 20.dp, end = 20.dp, bottom = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(titulo, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFFFF6D00))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(mensaje, textAlign = TextAlign.Center, fontSize = 14.sp, color = Color.DarkGray)

                        if (textoBoton != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { if (paso == 8) onOmitir() else onSiguiente() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF18C1A8)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(textoBoton, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (paso != 8) {
                            Spacer(modifier = Modifier.height(if (textoBoton != null) 6.dp else 10.dp))
                            HorizontalDivider(color = Color(0xFFE0F2F1), thickness = 1.dp)
                            TextButton(
                                onClick = onOmitir,
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.FastForward, contentDescription = null, tint = Color(0xFFFF3D00), modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(textos.tutOmitir, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = Color(0xFFFF3D00))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}