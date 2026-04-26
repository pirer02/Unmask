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

// Importamos todos los datos necesarios
import org.example.project.Datos.*

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
    val sinRepeticiones: Boolean = false // 👇 NUEVO: Parámetro para no repetir
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

@Composable
fun ContenedorPrincipal(onLoginGoogle: () -> Unit) {
    var pantallaActual by remember { mutableStateOf(PantallaNavegacion.INICIO) }
    var pantallaAnteriorJugadores by remember { mutableStateOf(PantallaNavegacion.INICIO) }

    var coleccionAEditar by remember { mutableStateOf<ColeccionGuardada?>(null) }
    var coleccionParaJugar by remember { mutableStateOf<ColeccionGuardada?>(null) }
    var opcionesConfiguradas by remember { mutableStateOf<OpcionesJuego?>(null) }

    val jugadoresGlobales = GestorDatos.jugadoresGlobales
    val snackbarHostState = remember { SnackbarHostState() }

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
            when (pantallaActual) {
                PantallaNavegacion.INICIO -> PantallaInicio(
                    onJugar = { coleccion ->
                        coleccionParaJugar = coleccion
                        // 👇 NUEVO: Limpiamos la memoria de palabras si empezamos de cero
                        GestorDatos.palabrasUsadasSesion.clear()
                        opcionesConfiguradas = null
                        pantallaActual = PantallaNavegacion.CONFIG_JUEGO
                    },
                    onGestionarJugadores = {
                        pantallaAnteriorJugadores = PantallaNavegacion.INICIO
                        pantallaActual = PantallaNavegacion.JUGADORES
                    }
                )
                PantallaNavegacion.BIBLIOTECA -> PantallaBiblioteca(
                    onEditar = { coleccion ->
                        coleccionAEditar = coleccion
                        pantallaActual = PantallaNavegacion.CREAR
                    },
                    onJugar = { coleccion ->
                        coleccionParaJugar = coleccion
                        GestorDatos.palabrasUsadasSesion.clear()
                        opcionesConfiguradas = null
                        pantallaActual = PantallaNavegacion.CONFIG_JUEGO
                    }
                )
                PantallaNavegacion.CREAR -> PantallaCrear(
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
                PantallaNavegacion.CONFIG_JUEGO -> {
                    coleccionParaJugar?.let { coleccion ->
                        PantallaConfiguracion(
                            coleccion = coleccion,
                            jugadores = jugadoresGlobales,
                            opcionesIniciales = opcionesConfiguradas, // 👇 Pasamos las opciones guardadas si venimos del juego
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
                            // 👇 CAMBIO CLAVE: Al terminar la partida, volvemos a la configuración
                            onSalir = { pantallaActual = PantallaNavegacion.CONFIG_JUEGO }
                        )
                    }
                }
                PantallaNavegacion.EXPLORAR -> PantallaEnConstruccion("Explorar")

                PantallaNavegacion.PERFIL -> {
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
                        }
                    )
                }
            }
        }
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