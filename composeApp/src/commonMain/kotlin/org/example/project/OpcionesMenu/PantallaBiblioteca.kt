import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExitToApp
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.example.project.Datos.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaBiblioteca(
    onEditar: (ColeccionGuardada) -> Unit,
    onJugar: (ColeccionGuardada) -> Unit,
    onVerPerfilAjeno: (String) -> Unit
) {
    val colecciones = GestorDatos.coleccionesGlobales
    var coleccionParaBorrar by remember { mutableStateOf<ColeccionGuardada?>(null) }

    val usuarioAuth by GestorAuth.usuario.collectAsState()
    val scope = rememberCoroutineScope()

    var textoBusqueda by remember { mutableStateOf("") }
    var filtroSeleccionado by remember { mutableStateOf("Todas") }

    // 👇 CAMBIO: Filtramos y además ORDENAMOS por prioridad
    val coleccionesFiltradas = colecciones.filter { coleccion ->
        val coincideTexto = textoBusqueda.isBlank() ||
                coleccion.nombre.contains(textoBusqueda, ignoreCase = true) ||
                coleccion.categoria.contains(textoBusqueda, ignoreCase = true)

        val coincideTipo = when (filtroSeleccionado) {
            "Mías" -> !coleccion.esDescargada && !coleccion.esColaboracion
            "Descargadas" -> coleccion.esDescargada && !coleccion.esColaboracion
            "Colaboraciones" -> coleccion.esColaboracion
            else -> true
        }

        coincideTexto && coincideTipo
    }.sortedBy { coleccion ->
        // Asignamos la prioridad de aparición
        when {
            !coleccion.esDescargada && !coleccion.esColaboracion -> 1 // 1º Las tuyas
            coleccion.esColaboracion -> 2                             // 2º Las colaborativas
            else -> 3                                                 // 3º Las descargadas online
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Mi Biblioteca", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = textoBusqueda,
            onValueChange = { textoBusqueda = it },
            placeholder = { Text("Buscar listas...") },
            leadingIcon = { Icon(Icons.Rounded.Search, null, tint = Color.Gray) },
            trailingIcon = {
                if (textoBusqueda.isNotEmpty()) {
                    IconButton(onClick = { textoBusqueda = "" }) {
                        Icon(Icons.Rounded.Clear, null)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(50),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFFF6D00),
                unfocusedBorderColor = Color.LightGray
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 👇 CAMBIO: Añadido "Colaboraciones" a la lista de opciones
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val opcionesFiltro = listOf("Todas", "Mías", "Descargadas", "Colaboraciones")
            items(opcionesFiltro) { filtro ->
                FilterChip(
                    selected = filtroSeleccionado == filtro,
                    onClick = { filtroSeleccionado = filtro },
                    label = { Text(filtro) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFF6D00),
                        selectedLabelColor = Color.White
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = filtroSeleccionado == filtro,
                        borderColor = if (filtroSeleccionado == filtro) Color.Transparent else Color.LightGray
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (coleccionesFiltradas.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                if (colecciones.isEmpty()) {
                    Text("Aún no tienes ninguna lista creada.", color = Color.Gray)
                } else {
                    Text("No se encontraron resultados.", color = Color.Gray)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.weight(1f)) {
                items(coleccionesFiltradas) { coleccion ->
                    TarjetaColeccion(
                        coleccion = coleccion,
                        onEliminarClick = { coleccionParaBorrar = coleccion },
                        onEditarClick = { onEditar(coleccion) },
                        onJugarClick = { onJugar(coleccion) },
                        onAutorClick = { coleccion.idCreador?.let { uid -> onVerPerfilAjeno(uid) } }
                    )
                }
            }
        }
    }

    if (coleccionParaBorrar != null) {
        val esColaboracion = coleccionParaBorrar!!.esColaboracion
        AlertDialog(
            onDismissRequest = { coleccionParaBorrar = null },
            title = { Text(if (esColaboracion) "¿Abandonar colaboración?" else "¿Eliminar colección?") },
            text = {
                if (esColaboracion) {
                    Text("¿Seguro que quieres dejar de colaborar en '${coleccionParaBorrar?.nombre}'? Perderás el acceso a la lista, pero el creador conservará los cambios que hayas hecho.")
                } else {
                    Text("¿Estás seguro de que quieres borrar '${coleccionParaBorrar?.nombre}'? Esta acción no se puede deshacer.")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    coleccionParaBorrar?.let { col ->
                        scope.launch {
                            usuarioAuth?.uid?.let { uid ->
                                if (col.esColaboracion) {
                                    GestorDatos.abandonarColaboracion(uid, col)
                                } else {
                                    val nombreCol = col.nombre
                                    GestorDatos.coleccionesGlobales.remove(col)
                                    GestorDatos.guardarCambiosMemoria()
                                    try {
                                        GestorAuth.firestore
                                            .collection("usuarios")
                                            .document(uid)
                                            .collection("colecciones")
                                            .document(nombreCol)
                                            .delete()
                                    } catch (e: Exception) { }
                                }
                            }
                        }
                    }
                    coleccionParaBorrar = null
                }) { Text(if (esColaboracion) "ABANDONAR" else "ELIMINAR", color = Color(0xFFFF3D00)) }
            },
            dismissButton = {
                TextButton(onClick = { coleccionParaBorrar = null }) { Text("CANCELAR", color = Color.Gray) }
            }
        )
    }
}

@Composable
fun TarjetaColeccion(
    coleccion: ColeccionGuardada,
    onEliminarClick: () -> Unit,
    onEditarClick: () -> Unit,
    onJugarClick: () -> Unit,
    mostrarAcciones: Boolean = true,
    onAutorClick: (() -> Unit)? = null
) {
    val totalPalabras = coleccion.elementos.sumOf { elemento ->
        when (elemento) {
            is ElementoGuardado.Individual -> 1
            is ElementoGuardado.Conjunto -> elemento.palabras.size
        }
    }

    // 👇 CAMBIO: Definición de colores con Modo Oscuro para tus listas
    val colorFondo: Color
    val borderStroke: BorderStroke?
    val colorTitulo: Color
    val colorAcento: Color
    val colorFondoChip: Color
    val colorSeparador: Color
    val colorIconoEdit: Color
    val colorBotonJugar: Color

    when {
        coleccion.esColaboracion -> { // Naranja Ámbar Fuerte (Colaboraciones)
            colorFondo = Color(0xFFFFF3E0)
            borderStroke = BorderStroke(1.dp, Color(0xFFFFE0B2))
            colorTitulo = Color(0xFF1A1A1A)
            colorAcento = Color(0xFFE65100)
            colorFondoChip = Color.White
            colorSeparador = Color(0xFFFFE0B2)
            colorIconoEdit = Color.Gray
            colorBotonJugar = Color(0xFF18C1A8) // Verde Azulado
        }
        coleccion.esDescargada -> { // Azul (Online)
            colorFondo = Color(0xFFE3F2FD)
            borderStroke = BorderStroke(1.dp, Color(0xFFBBDEFB))
            colorTitulo = Color(0xFF1A1A1A)
            colorAcento = Color(0xFF1976D2)
            colorFondoChip = Color.White
            colorSeparador = Color(0xFFBBDEFB)
            colorIconoEdit = Color.Gray
            colorBotonJugar = Color(0xFF18C1A8) // Verde Azulado
        }
        else -> { // Mías (Modo Oscuro)
            colorFondo = Color(0xFF181818)
            borderStroke = null
            colorTitulo = Color.White
            colorAcento = Color(0xFFFF6D00)
            colorFondoChip = Color(0xFF2C2C2C)
            colorSeparador = Color.DarkGray
            colorIconoEdit = Color.LightGray
            colorBotonJugar = Color(0xFFFF6D00) // Naranja principal
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colorFondo),
        border = borderStroke,
        elevation = CardDefaults.cardElevation(defaultElevation = if (coleccion.esColaboracion || coleccion.esDescargada) 0.dp else 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(coleccion.nombre, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colorTitulo)

                        if (coleccion.esColaboracion) {
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Rounded.Group, null, tint = colorAcento, modifier = Modifier.size(16.dp))
                        } else if (coleccion.esDescargada) {
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Rounded.CloudDownload, null, tint = colorAcento, modifier = Modifier.size(16.dp))
                        }
                    }

                    if (coleccion.esDescargada || coleccion.esColaboracion) {
                        coleccion.nombreCreador?.let { autor ->
                            Text(
                                text = "Por @$autor",
                                color = colorAcento.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.let {
                                    if (onAutorClick != null) it.clickable { onAutorClick.invoke() }.padding(vertical = 2.dp) else it
                                }
                            )
                        }
                    } else {
                        Text("Por Mí", color = colorAcento.copy(alpha = 0.9f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Text(coleccion.categoria.uppercase(), color = colorAcento, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                }
                Surface(color = colorFondoChip, border = borderStroke, shape = RoundedCornerShape(8.dp)) {
                    Text(text = "$totalPalabras pal.", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = colorAcento)
                }
            }

            if (mostrarAcciones) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = colorSeparador, thickness = 1.dp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row {
                        IconButton(onClick = onEliminarClick) {
                            Icon(
                                imageVector = if (coleccion.esColaboracion) Icons.Rounded.ExitToApp else Icons.Rounded.Delete,
                                contentDescription = null,
                                tint = Color(0xFFFF3D00)
                            )
                        }

                        // 👇 CAMBIO: Solo mostramos el lápiz de editar si NO es una lista descargada
                        if (!coleccion.esDescargada) {
                            IconButton(onClick = onEditarClick) {
                                Icon(Icons.Rounded.Edit, contentDescription = null, tint = colorIconoEdit)
                            }
                        }
                    }
                    Button(onClick = onJugarClick, colors = ButtonDefaults.buttonColors(containerColor = colorBotonJugar), contentPadding = PaddingValues(horizontal = 16.dp)) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("JUGAR", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}