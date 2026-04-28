import androidx.compose.foundation.BorderStroke
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
    onJugar: (ColeccionGuardada) -> Unit
) {
    val colecciones = GestorDatos.coleccionesGlobales
    var coleccionParaBorrar by remember { mutableStateOf<ColeccionGuardada?>(null) }

    val usuarioAuth by GestorAuth.usuario.collectAsState()
    val scope = rememberCoroutineScope()

    // 👇 NUEVO: Estados para el buscador y los filtros
    var textoBusqueda by remember { mutableStateOf("") }
    var filtroSeleccionado by remember { mutableStateOf("Todas") } // Opciones: Todas, Mías, Descargadas

    // 👇 NUEVO: Lógica de filtrado en tiempo real
    val coleccionesFiltradas = colecciones.filter { coleccion ->
        // Filtro por texto (busca en nombre o categoría)
        val coincideTexto = textoBusqueda.isBlank() ||
                coleccion.nombre.contains(textoBusqueda, ignoreCase = true) ||
                coleccion.categoria.contains(textoBusqueda, ignoreCase = true)

        // Filtro por tipo (mías vs online)
        val coincideTipo = when (filtroSeleccionado) {
            "Mías" -> !coleccion.esDescargada
            "Descargadas" -> coleccion.esDescargada
            else -> true // "Todas"
        }

        coincideTexto && coincideTipo
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Mi Biblioteca", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        // 👇 NUEVO: Buscador visualmente idéntico al de Explorar
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

        // 👇 NUEVO: Chips de filtrado
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val opcionesFiltro = listOf("Todas", "Mías", "Descargadas")
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

        // MOSTRAR RESULTADOS
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
                        onJugarClick = { onJugar(coleccion) }
                    )
                }
            }
        }
    }

    if (coleccionParaBorrar != null) {
        AlertDialog(
            onDismissRequest = { coleccionParaBorrar = null },
            title = { Text("¿Eliminar colección?") },
            text = { Text("¿Estás seguro de que quieres borrar '${coleccionParaBorrar?.nombre}'? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    coleccionParaBorrar?.let { col ->
                        val nombreCol = col.nombre

                        GestorDatos.coleccionesGlobales.remove(col)
                        GestorDatos.guardarCambiosMemoria()

                        scope.launch {
                            usuarioAuth?.uid?.let { uid ->
                                try {
                                    GestorAuth.firestore
                                        .collection("usuarios")
                                        .document(uid)
                                        .collection("colecciones")
                                        .document(nombreCol)
                                        .delete()
                                } catch (e: Exception) {
                                    // Silencioso
                                }
                            }
                        }
                    }
                    coleccionParaBorrar = null
                }) { Text("ELIMINAR", color = Color(0xFFFF3D00)) }
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
    mostrarAcciones: Boolean = true
) {
    val totalPalabras = coleccion.elementos.sumOf { elemento ->
        when (elemento) {
            is ElementoGuardado.Individual -> 1
            is ElementoGuardado.Conjunto -> elemento.palabras.size
        }
    }

    val esOnline = coleccion.esDescargada
    val colorFondo = if (esOnline) Color(0xFFE3F2FD) else Color(0xFFFFF4E6)
    val colorBorde = if (esOnline) Color(0xFFBBDEFB) else Color(0xFFFFD8C2)
    val colorAcento = if (esOnline) Color(0xFF1976D2) else Color(0xFFFF6D00)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colorFondo),
        border = BorderStroke(1.dp, colorBorde),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(coleccion.nombre, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1A1A1A))
                        if (esOnline) {
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Rounded.CloudDownload, null, tint = colorAcento, modifier = Modifier.size(16.dp))
                        }
                    }

                    // 👇 NUEVO: Distinción de autoría mucho más clara
                    if (esOnline) {
                        coleccion.nombreCreador?.let { autor ->
                            Text("Por @$autor", color = colorAcento.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    } else {
                        // Resalta con FontWeight.Bold que es del usuario
                        Text("Por Mí", color = colorAcento.copy(alpha = 0.9f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Text(coleccion.categoria.uppercase(), color = colorAcento, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                }
                Surface(color = Color.White, border = BorderStroke(1.dp, colorBorde), shape = RoundedCornerShape(8.dp)) {
                    Text(text = "$totalPalabras pal.", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = colorAcento)
                }
            }

            if (mostrarAcciones) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = colorBorde, thickness = 1.dp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row {
                        IconButton(onClick = onEliminarClick) { Icon(Icons.Rounded.Delete, contentDescription = null, tint = Color(0xFFFF3D00)) }
                        IconButton(onClick = onEditarClick) { Icon(Icons.Rounded.Edit, contentDescription = null, tint = Color.Gray) }
                    }
                    Button(onClick = onJugarClick, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF18C1A8)), contentPadding = PaddingValues(horizontal = 16.dp)) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("JUGAR", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}