import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch // 👇 Añadido para poder usar la nube
import org.example.project.Datos.*

// Herramienta de capitalización para nombres
fun String.capitalizarNombre(): String {
    if (this.isBlank()) return this
    return this.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

@Composable
fun PantallaJugadores(
    jugadores: MutableList<String>,
    onVolver: () -> Unit
) {
    var nuevoJugador by remember { mutableStateOf("") }
    var errorTexto by remember { mutableStateOf("") } // Para notificar errores al usuario

    var editandoIndex by remember { mutableStateOf<Int?>(null) }
    var textoEdicion by remember { mutableStateOf("") }
    var mostrarAvisoBorrarTodos by remember { mutableStateOf(false) }

    val opcionesTeclado = KeyboardOptions(capitalization = KeyboardCapitalization.Words)

    val scope = rememberCoroutineScope()
    val usuarioLogueado = GestorAuth.usuario.collectAsState().value

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF9F9F9))) {
        // CABECERA
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onVolver) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Volver") }
            Text("Jugadores", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            Text("${jugadores.size}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF6D00))

            if (jugadores.isNotEmpty()) {
                IconButton(onClick = { mostrarAvisoBorrarTodos = true }) {
                    Icon(Icons.Rounded.DeleteSweep, contentDescription = "Borrar todos", tint = Color.Red)
                }
            }
        }

        // BLOQUE DE ENTRADA
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = nuevoJugador,
                    onValueChange = {
                        nuevoJugador = it
                        errorTexto = "" // Limpiamos el error al escribir
                    },
                    placeholder = { Text("Nombre del jugador...") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    isError = errorTexto.isNotEmpty(),
                    keyboardOptions = opcionesTeclado,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val nombreLimpio = nuevoJugador.trim()
                        if (nombreLimpio.isBlank()) {
                            errorTexto = "El nombre no puede estar vacío"
                        } else if (jugadores.any { it.equals(nombreLimpio, ignoreCase = true) }) {
                            errorTexto = "¡Este nombre ya existe!"
                        } else {
                            jugadores.add(nombreLimpio.capitalizarNombre())
                            GestorDatos.guardarCambiosMemoria()

                            // 👇 MÁGIA DE LA NUBE: Guardar al añadir
                            usuarioLogueado?.let { user -> scope.launch { GestorDatos.subirJugadoresNube(user.uid) } }

                            nuevoJugador = ""
                            errorTexto = ""
                        }
                    },
                    modifier = Modifier.size(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6D00))
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Añadir", modifier = Modifier.size(32.dp))
                }
            }
            // Mensaje de error dinámico debajo de la casilla
            if (errorTexto.isNotEmpty()) {
                Text(
                    text = errorTexto,
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // LISTA DE JUGADORES
        if (jugadores.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                Text("Añade al menos 3 jugadores", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(jugadores) { index, jugador ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        if (editandoIndex == index) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        value = textoEdicion,
                                        onValueChange = { textoEdicion = it },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        keyboardOptions = opcionesTeclado
                                    )
                                    IconButton(onClick = {
                                        val editadoLimpio = textoEdicion.trim()
                                        // Comprobamos que no se repita con otros nombres al editar
                                        val existe = jugadores.filterIndexed { i, _ -> i != index }
                                            .any { it.equals(editadoLimpio, ignoreCase = true) }

                                        if (editadoLimpio.isNotBlank() && !existe) {
                                            jugadores[index] = editadoLimpio.capitalizarNombre()
                                            GestorDatos.guardarCambiosMemoria()

                                            // 👇 MÁGIA DE LA NUBE: Guardar al editar
                                            usuarioLogueado?.let { user -> scope.launch { GestorDatos.subirJugadoresNube(user.uid) } }

                                            editandoIndex = null
                                        }
                                    }) {
                                        Icon(Icons.Rounded.Check, contentDescription = "Guardar", tint = Color(0xFF18C1A8))
                                    }
                                }
                            }
                        } else {
                            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Person, contentDescription = null, tint = Color.Gray)
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(jugador, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.weight(1f))

                                IconButton(onClick = {
                                    editandoIndex = index
                                    textoEdicion = jugador
                                }) {
                                    Icon(Icons.Rounded.Edit, contentDescription = "Editar", tint = Color.Gray)
                                }

                                IconButton(onClick = {
                                    jugadores.removeAt(index)
                                    GestorDatos.guardarCambiosMemoria()

                                    // 👇 MÁGIA DE LA NUBE: Guardar al eliminar uno
                                    usuarioLogueado?.let { user -> scope.launch { GestorDatos.subirJugadoresNube(user.uid) } }

                                    if (editandoIndex == index) editandoIndex = null
                                }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Eliminar", tint = Color.Red)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // DIÁLOGO CONFIRMAR BORRAR TODOS
    if (mostrarAvisoBorrarTodos) {
        AlertDialog(
            onDismissRequest = { mostrarAvisoBorrarTodos = false },
            title = { Text("¿Borrar todos?") },
            text = { Text("Se vaciará la lista de jugadores.") },
            confirmButton = {
                TextButton(onClick = {
                    jugadores.clear()
                    GestorDatos.guardarCambiosMemoria()

                    // 👇 MÁGIA DE LA NUBE: Guardar al vaciar la lista
                    usuarioLogueado?.let { user -> scope.launch { GestorDatos.subirJugadoresNube(user.uid) } }

                    mostrarAvisoBorrarTodos = false
                }) { Text("VACIAR", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { mostrarAvisoBorrarTodos = false }) { Text("CANCELAR", color = Color.Gray) }
            }
        )
    }
}