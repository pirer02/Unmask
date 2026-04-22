import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import kotlinx.coroutines.launch
import org.example.project.Datos.*

@Composable
fun PantallaMiPerfil(
    onVolver: () -> Unit,
    onIniciarSesionGoogle: () -> Unit,
    onEditar: (ColeccionGuardada) -> Unit,
    onJugar: (ColeccionGuardada) -> Unit
) {
    val usuario by GestorAuth.usuario.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // ESTADOS CENTRALES
    var estadoPantalla by remember { mutableStateOf("CARGANDO") }
    var clicEnIniciarSesion by remember { mutableStateOf(false) }
    var nombreUsuarioReal by remember { mutableStateOf("") }

    // VARIABLES DE MIGRACIÓN Y CARGA
    var cargandoAuth by remember { mutableStateOf(false) }
    var listasLocalesTemp by remember { mutableStateOf<List<ColeccionGuardada>>(emptyList()) }
    var listasSeleccionadas by remember { mutableStateOf<Set<ColeccionGuardada>>(emptySet()) }

    // DIÁLOGOS
    var mostrarDialogoNombre by remember { mutableStateOf(false) }
    var mostrarDialogoCerrar by remember { mutableStateOf(false) }
    var mostrarDialogoBorrar by remember { mutableStateOf(false) }
    var mostrarDialogoMigracion by remember { mutableStateOf(false) }
    var coleccionParaBorrar by remember { mutableStateOf<ColeccionGuardada?>(null) }

    var expListas by remember { mutableStateOf(false) }
    var nombreUsuarioInput by remember { mutableStateOf("") }
    var errorNombre by remember { mutableStateOf("") }

    // LÓGICA DE SESIÓN Y MIGRACIÓN
    LaunchedEffect(usuario) {
        if (usuario == null) {
            estadoPantalla = "INVITADO"
        } else {
            if (clicEnIniciarSesion) {
                // 1. EL USUARIO ACABA DE PULSAR EL BOTÓN: Capturamos listas locales antes de que se mezclen
                listasLocalesTemp = GestorDatos.coleccionesGlobales.toList()
                listasSeleccionadas = listasLocalesTemp.toSet()

                val esNuevo = GestorAuth.necesitaNombreUsuario()

                if (esNuevo) {
                    estadoPantalla = "CONFIGURANDO"
                    mostrarDialogoNombre = true
                } else {
                    nombreUsuarioReal = GestorAuth.obtenerNombreUsuario(usuario!!.uid) ?: ""

                    // Si hay listas locales, preguntamos aunque el usuario no sea nuevo
                    if (listasLocalesTemp.isNotEmpty()) {
                        estadoPantalla = "CONFIGURANDO"
                        mostrarDialogoMigracion = true
                    } else {
                        GestorDatos.descargarDatosNube(usuario!!.uid)
                        snackbarHostState.showSnackbar("¡Bienvenido de nuevo, $nombreUsuarioReal!")
                        clicEnIniciarSesion = false
                        estadoPantalla = "PERFIL"
                    }
                }
            } else {
                // 2. SESIÓN YA ACTIVA (App abierta recientemente)
                nombreUsuarioReal = GestorAuth.obtenerNombreUsuario(usuario!!.uid) ?: ""
                GestorDatos.descargarDatosNube(usuario!!.uid)
                estadoPantalla = "PERFIL"
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFF9F9F9)
    ) { padding ->
        // AJUSTE DE CABECERA: Sin padding superior para igualar a Biblioteca
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp, bottom = padding.calculateBottomPadding())
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // CABECERA VOLVER
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onVolver) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = null, tint = Color(0xFF1A1A1A))
                }
                Text("Mi Perfil", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A))
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (estadoPantalla) {
                "CARGANDO" -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFFFF6D00))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Conectando con Google...", color = Color.Gray, fontSize = 14.sp)
                    }
                }

                "INVITADO" -> {
                    Spacer(modifier = Modifier.height(32.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(8.dp),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(modifier = Modifier.size(90.dp).background(Color(0xFFE0F2F1), CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.CloudSync, null, modifier = Modifier.size(48.dp), tint = Color(0xFF00897B))
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Text("¡Sincroniza tus datos!", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = Color(0xFF1A1A1A))
                            Text("Guarda tus listas y jugadores en la nube para no perderlos nunca.", color = Color.Gray, textAlign = TextAlign.Center, fontSize = 15.sp)
                            Spacer(modifier = Modifier.height(32.dp))
                            Button(
                                onClick = {
                                    clicEnIniciarSesion = true
                                    estadoPantalla = "CARGANDO" // Activa ProgressBar inmediatamente
                                    onIniciarSesionGoogle()
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF18C1A8)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.Rounded.Login, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("INICIAR SESIÓN CON GOOGLE", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                "CONFIGURANDO" -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFFFF6D00), modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Preparando tu cuenta...", fontWeight = FontWeight.Bold, color = Color.Gray)
                    }
                }

                "PERFIL" -> LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(24.dp)) {

                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Brush.verticalGradient(listOf(Color(0xFF18C1A8), Color(0xFF00897B)))).padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val fotoUrl = usuario?.photoURL ?: ""
                                Box(modifier = Modifier.size(110.dp).background(Color.White, CircleShape).padding(4.dp).clip(CircleShape).background(Color(0xFFFFD8C2)), contentAlignment = Alignment.Center) {
                                    if (fotoUrl.isNotEmpty()) KamelImage(asyncPainterResource(fotoUrl), null, modifier = Modifier.fillMaxSize())
                                    else Icon(Icons.Rounded.Person, null, modifier = Modifier.fillMaxSize(), tint = Color.White)
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("@$nombreUsuarioReal", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                                Text(usuario?.email ?: "", color = Color(0xFFE0F2F1), fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(24.dp))
                                Surface(color = Color.White.copy(alpha = 0.2f), shape = RoundedCornerShape(16.dp)) {
                                    Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.LibraryBooks, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("${GestorDatos.coleccionesGlobales.size} Listas creadas", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp), shape = RoundedCornerShape(24.dp)) {
                            Column(modifier = Modifier.fillMaxWidth().clickable { expListas = !expListas }.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(48.dp).background(Color(0xFFFFF4E6), CircleShape), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Rounded.CloudDone, null, tint = Color(0xFFFF6D00))
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text("Mis listas en la nube", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
                                    Icon(if(expListas) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = Color.Gray)
                                }
                                AnimatedVisibility(visible = expListas, enter = expandVertically(tween(300)), exit = shrinkVertically(tween(300))) {
                                    Column(modifier = Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                        if (GestorDatos.coleccionesGlobales.isEmpty()) {
                                            Text("No hay listas en la nube.", color = Color.Gray, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                                        } else {
                                            GestorDatos.coleccionesGlobales.forEach { col ->
                                                TarjetaColeccion(col, { coleccionParaBorrar = col }, { onEditar(col) }, { onJugar(col) }, true)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        OutlinedButton(onClick = { mostrarDialogoCerrar = true }, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1A1A1A)), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Color.LightGray)) {
                            Icon(Icons.Rounded.Logout, null, tint = Color.Gray)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("CERRAR SESIÓN", fontWeight = FontWeight.Bold)
                        }
                        TextButton(onClick = { mostrarDialogoBorrar = true }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            Icon(Icons.Rounded.Warning, null, tint = Color(0xFFE57373), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Eliminar mi cuenta definitivamente", color = Color(0xFFE57373), fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }

    // --- DIÁLOGOS DE CONFIRMACIÓN ---

    if (coleccionParaBorrar != null) {
        AlertDialog(
            onDismissRequest = { coleccionParaBorrar = null },
            title = { Text("¿Eliminar colección?") },
            text = { Text("¿Seguro que quieres borrar '${coleccionParaBorrar?.nombre}' de la nube? No se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    coleccionParaBorrar?.let { col ->
                        val nombreCol = col.nombre
                        GestorDatos.coleccionesGlobales.remove(col)
                        GestorDatos.guardarCambiosMemoria()
                        scope.launch {
                            usuario?.uid?.let { uid ->
                                try { GestorAuth.firestore.collection("usuarios").document(uid).collection("colecciones").document(nombreCol).delete() } catch (e: Exception) { }
                            }
                        }
                    }
                    coleccionParaBorrar = null
                }) { Text("ELIMINAR", color = Color.Red, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { coleccionParaBorrar = null }) { Text("CANCELAR", color = Color.Gray) } }
        )
    }

    if (mostrarDialogoCerrar) {
        AlertDialog(onDismissRequest = { mostrarDialogoCerrar = false }, title = { Text("¿Cerrar sesión?") }, text = { Text("Se borrarán los datos temporales del dispositivo. Tus listas seguirán en la nube.") }, confirmButton = { Button(onClick = { scope.launch { GestorAuth.cerrarSesion(); GestorDatos.limpiarDatosLocales(); clicEnIniciarSesion = false }; mostrarDialogoCerrar = false }, colors = ButtonDefaults.buttonColors(containerColor = Color.Black)) { Text("CERRAR SESIÓN") } }, dismissButton = { TextButton(onClick = { mostrarDialogoCerrar = false }) { Text("CANCELAR", color = Color.Gray) } })
    }

    if (mostrarDialogoBorrar) {
        AlertDialog(onDismissRequest = { mostrarDialogoBorrar = false }, title = { Text("⚠️ ELIMINAR CUENTA", color = Color.Red) }, text = { Text("Acción IRREVERSIBLE. Se borrarán tus listas, tu nombre y tu progreso para siempre.") }, confirmButton = { Button(onClick = { val uidFijo = usuario?.uid; if (uidFijo != null) { scope.launch { if(GestorAuth.eliminarCuentaDefinitiva(uidFijo)) { GestorDatos.limpiarDatosLocales(); clicEnIniciarSesion = false; snackbarHostState.showSnackbar("Cuenta eliminada") } else { snackbarHostState.showSnackbar("Seguridad: Reinicia sesión") } } }; mostrarDialogoBorrar = false }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("BORRAR TODO", fontWeight = FontWeight.Bold) } }, dismissButton = { TextButton(onClick = { mostrarDialogoBorrar = false }) { Text("CANCELAR", color = Color.Gray) } })
    }

    if (mostrarDialogoNombre) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Nombre de investigador") },
            text = {
                OutlinedTextField(value = nombreUsuarioInput, onValueChange = { nombreUsuarioInput = it.lowercase().replace(" ", "") }, singleLine = true, isError = errorNombre.isNotEmpty(), supportingText = { if (errorNombre.isNotEmpty()) Text(errorNombre) }, shape = RoundedCornerShape(12.dp), label = { Text("Tu apodo único") })
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        if (nombreUsuarioInput.length < 3) { errorNombre = "Mínimo 3 letras"; return@launch }
                        cargandoAuth = true
                        if (GestorAuth.registrarNombreUsuario(nombreUsuarioInput)) {
                            nombreUsuarioReal = nombreUsuarioInput
                            mostrarDialogoNombre = false
                            if (listasLocalesTemp.isNotEmpty()) {
                                mostrarDialogoMigracion = true
                            } else {
                                GestorDatos.descargarDatosNube(usuario!!.uid)
                                snackbarHostState.showSnackbar("¡Bienvenido @$nombreUsuarioReal!")
                                estadoPantalla = "PERFIL"
                                clicEnIniciarSesion = false
                            }
                        } else { errorNombre = "Nombre en uso" }
                        cargandoAuth = false
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF18C1A8))) { Text("CONFIRMAR") }
            }
        )
    }

    if (mostrarDialogoMigracion) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("¿Guardar listas locales?") },
            text = {
                Column {
                    Text("Tienes listas en este móvil. ¿Quieres subirlas a tu cuenta de Google?", color = Color.Gray, fontSize = 14.sp)
                    Spacer(Modifier.height(16.dp))
                    LazyColumn(Modifier.heightIn(max = 200.dp)) {
                        items(listasLocalesTemp) { coleccion ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = listasSeleccionadas.contains(coleccion), onCheckedChange = {
                                    listasSeleccionadas = if (it) listasSeleccionadas + coleccion else listasSeleccionadas - coleccion
                                }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFFFF6D00)))
                                Text(coleccion.nombre, fontSize = 16.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        cargandoAuth = true
                        listasSeleccionadas.forEach { GestorDatos.subirColeccionNube(usuario!!.uid, it) }
                        GestorDatos.subirJugadoresNube(usuario!!.uid)
                        GestorDatos.descargarDatosNube(usuario!!.uid)
                        mostrarDialogoMigracion = false
                        snackbarHostState.showSnackbar("¡Todo sincronizado, @$nombreUsuarioReal!")
                        estadoPantalla = "PERFIL"
                        clicEnIniciarSesion = false
                        cargandoAuth = false
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF18C1A8))) { Text("GUARDAR") }
            },
            dismissButton = {
                TextButton(onClick = {
                    scope.launch {
                        GestorDatos.descargarDatosNube(usuario!!.uid)
                        mostrarDialogoMigracion = false
                        estadoPantalla = "PERFIL"
                        clicEnIniciarSesion = false
                    }
                }) { Text("DESCARTAR LOCALES", color = Color.Gray) }
            }
        )
    }
}