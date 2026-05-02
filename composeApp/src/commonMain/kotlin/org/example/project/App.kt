import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Importamos todos los datos necesarios
import org.example.project.Datos.*
import org.example.project.PantallaMiPerfil

enum class PantallaNavegacion {
    INICIO, BIBLIOTECA, CREAR, EXPLORAR, PERFIL, CONFIG_JUEGO, JUGADORES, JUEGO
}

data class OpcionesJuego(
    val numImpostores: Int,
    val pistaParaImpostor: Boolean,
    val limiteRondas: Boolean,
    val rondas: Int,
    val limiteTiempo: Boolean,
    val tiempoMinutos: Int,
    val sinRepeticiones: Boolean = false
)

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

// 👇 NUEVO: Componente que simula la carga de cada pantalla
@Composable
fun PantallaConCarga(content: @Composable () -> Unit) {
    var cargando by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(350) // Tiempo para que la app respire y acomode los componentes
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

    var uidPerfilExplorar by remember { mutableStateOf<String?>(null) }

    val jugadoresGlobales = GestorDatos.jugadoresGlobales
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val usuarioAuth by GestorAuth.usuario.collectAsState()
    var invitacionPendiente by remember { mutableStateOf<InvitacionColaboracion?>(null) }
    var procesandoInvitacion by remember { mutableStateOf(false) }

    // 👇 CAMBIO: Escucha "pantallaActual" para comprobar si hay invitaciones nuevas
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (pantallaActual in listOf(PantallaNavegacion.INICIO, PantallaNavegacion.BIBLIOTECA, PantallaNavegacion.EXPLORAR, PantallaNavegacion.PERFIL)) {
                FloatingActionButton(
                    onClick = {
                        coleccionAEditar = null
                        pantallaActual = PantallaNavegacion.CREAR
                    },
                    containerColor = Color(0xFFFF6D00),
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(64.dp).offset(y = 45.dp),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Crear", modifier = Modifier.size(36.dp))
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
                        onPantallaCambiada = { nuevaPantalla -> pantallaActual = nuevaPantalla }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

            // 👇 CAMBIO: Transiciones fluidas entre pantallas
            AnimatedContent(
                targetState = pantallaActual,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.95f, animationSpec = tween(300)))
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
                                opcionesConfiguradas = null
                                pantallaActual = PantallaNavegacion.CONFIG_JUEGO
                            },
                            onGestionarJugadores = {
                                pantallaAnteriorJugadores = PantallaNavegacion.INICIO
                                pantallaActual = PantallaNavegacion.JUGADORES
                            },
                            onCrearLista = {
                                coleccionAEditar = null
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
                    PantallaNavegacion.CREAR -> PantallaConCarga {
                        PantallaCrear(
                            coleccionParaEditar = coleccionAEditar,
                            snackbarHostState = snackbarHostState,
                            onGuardadoExitoso = {
                                coleccionAEditar = null
                                pantallaActual = PantallaNavegacion.BIBLIOTECA
                            },
                            onVolver = {
                                coleccionAEditar = null
                                pantallaActual = PantallaNavegacion.INICIO
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
                    // A las pantallas de juego y configuración no les ponemos progreso por agilidad
                    PantallaNavegacion.CONFIG_JUEGO -> {
                        coleccionParaJugar?.let { coleccion ->
                            PantallaConfiguracion(
                                coleccion = coleccion,
                                jugadores = jugadoresGlobales,
                                opcionesIniciales = opcionesConfiguradas,
                                snackbarHostState = snackbarHostState,
                                onIrAJugadores = { pantallaAnteriorJugadores = PantallaNavegacion.CONFIG_JUEGO; pantallaActual = PantallaNavegacion.JUGADORES },
                                onIniciarJuego = { opciones -> opcionesConfiguradas = opciones; pantallaActual = PantallaNavegacion.JUEGO },
                                onVolver = { pantallaActual = PantallaNavegacion.INICIO }
                            )
                        }
                    }
                    PantallaNavegacion.JUGADORES -> PantallaJugadores(jugadores = jugadoresGlobales, onVolver = { pantallaActual = pantallaAnteriorJugadores })
                    PantallaNavegacion.JUEGO -> {
                        if (coleccionParaJugar != null && opcionesConfiguradas != null) {
                            PantallaJuego(
                                coleccion = coleccionParaJugar!!,
                                jugadores = jugadoresGlobales.toList(),
                                opciones = opcionesConfiguradas!!,
                                onSalir = { pantallaActual = PantallaNavegacion.CONFIG_JUEGO }
                            )
                        }
                    }
                }
            }
        }
    }

    if (invitacionPendiente != null) {
        AlertDialog(
            onDismissRequest = { },
            icon = { Icon(Icons.Rounded.Handshake, contentDescription = null, tint = Color(0xFF18C1A8)) },
            title = { Text("¡Nueva Colaboración!") },
            text = {
                if (procesandoInvitacion) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF18C1A8))
                    }
                } else {
                    Text("El investigador @${invitacionPendiente?.nombreEmisor} quiere que le ayudes a rellenar su lista '${invitacionPendiente?.nombreLista}'.\n\nSi aceptas, podrás editarla y aparecerá en tu biblioteca como propia.")
                }
            },
            confirmButton = {
                if (!procesandoInvitacion) {
                    Button(
                        onClick = {
                            scope.launch {
                                procesandoInvitacion = true
                                val inv = invitacionPendiente!!
                                val exito = GestorAuth.responderInvitacion(inv, true)
                                if (exito) {
                                    snackbarHostState.showSnackbar("¡Ahora eres colaborador de ${inv.nombreLista}!")
                                    usuarioAuth?.uid?.let { GestorDatos.descargarDatosNube(it) }
                                }
                                invitacionPendiente = null
                                procesandoInvitacion = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF18C1A8))
                    ) { Text("ACEPTAR") }
                }
            },
            dismissButton = {
                if (!procesandoInvitacion) {
                    TextButton(
                        onClick = {
                            scope.launch {
                                procesandoInvitacion = true
                                GestorAuth.responderInvitacion(invitacionPendiente!!, false)
                                invitacionPendiente = null
                                procesandoInvitacion = false
                            }
                        }
                    ) { Text("RECHAZAR", color = Color.Gray) }
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
fun MenuInferiorClavado(pantallaSeleccionada: PantallaNavegacion, onPantallaCambiada: (PantallaNavegacion) -> Unit) {
    NavigationBar(containerColor = Color(0xFF121212), contentColor = Color.White, modifier = Modifier.padding(top = 2.dp)) {
        NavigationBarItem(icon = { Icon(Icons.Rounded.Home, contentDescription = "Inicio") }, label = { Text("INICIO", fontWeight = FontWeight.Bold, fontSize = 10.sp) }, selected = pantallaSeleccionada == PantallaNavegacion.INICIO, onClick = { onPantallaCambiada(PantallaNavegacion.INICIO) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFFFF6D00), selectedTextColor = Color(0xFFFF6D00), unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray, indicatorColor = Color.Transparent))
        NavigationBarItem(icon = { Icon(Icons.Rounded.ViewList, contentDescription = "Biblioteca") }, label = { Text("BIBLIOTECA", fontSize = 10.sp) }, selected = pantallaSeleccionada == PantallaNavegacion.BIBLIOTECA, onClick = { onPantallaCambiada(PantallaNavegacion.BIBLIOTECA) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFFFF6D00), selectedTextColor = Color(0xFFFF6D00), unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray, indicatorColor = Color.Transparent))
        NavigationBarItem(icon = {}, label = { }, selected = false, onClick = { }, enabled = false)
        NavigationBarItem(icon = { Icon(Icons.Rounded.Search, contentDescription = "Explorar") }, label = { Text("EXPLORAR", fontSize = 10.sp) }, selected = pantallaSeleccionada == PantallaNavegacion.EXPLORAR, onClick = { onPantallaCambiada(PantallaNavegacion.EXPLORAR) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFFFF6D00), selectedTextColor = Color(0xFFFF6D00), unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray, indicatorColor = Color.Transparent))
        NavigationBarItem(icon = { Icon(Icons.Rounded.AccountCircle, contentDescription = "Perfil") }, label = { Text("PERFIL", fontSize = 10.sp) }, selected = pantallaSeleccionada == PantallaNavegacion.PERFIL, onClick = { onPantallaCambiada(PantallaNavegacion.PERFIL) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFFFF6D00), selectedTextColor = Color(0xFFFF6D00), unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray, indicatorColor = Color.Transparent))
    }
}

@Composable
fun PantallaEnConstruccion(nombre: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Pantalla $nombre en construcción...", color = Color.Gray) }
}