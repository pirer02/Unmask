import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import org.example.project.Datos.*

@Composable
fun PantallaInicio(
    onJugar: (ColeccionGuardada) -> Unit,
    onGestionarJugadores: () -> Unit // 👇 Nuevo parámetro
) {
    val recientes = GestorDatos.coleccionesGlobales.takeLast(3)
    val predeterminadas = DatosPredeterminados.listasPredeterminadas

    var coleccionViendoInfo by remember { mutableStateOf<ColeccionGuardada?>(null) }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("¡Juega con Unmask!", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            IconButton(
                onClick = { /* TODO */ },
                modifier = Modifier.size(48.dp).clip(CircleShape)
            ) {
                Icon(Icons.Rounded.AccountCircle, contentDescription = "Foto de perfil", modifier = Modifier.fillMaxSize(), tint = Color(0xFFFF6D00))
            }
        }

        // 👇 BOTÓN PARA LOS JUGADORES DEL DÍA
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable { onGestionarJugadores() },
            colors = CardDefaults.cardColors(containerColor = Color(0xFF18C1A8)), // Usamos tu turquesa
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Group, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Los jugadores de hoy", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                    Text("Configura tu grupo antes de jugar", color = Color.White, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (recientes.isNotEmpty()) {
            SeccionTitulo(titulo = "Creaciones Recientes", icono = Icons.Rounded.Stars)
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp)) {
                items(recientes) { coleccion ->
                    TarjetaColeccionInicio(
                        nombre = coleccion.nombre, categoria = coleccion.categoria, esPredeterminada = false,
                        onJugarClick = { onJugar(coleccion) }, onInfoClick = { coleccionViendoInfo = coleccion }
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        SeccionTitulo(titulo = "Colecciones Predeterminadas", icono = Icons.Rounded.Info)
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp)) {
            items(predeterminadas) { predefinida ->
                TarjetaColeccionInicio(
                    nombre = predefinida.nombre, categoria = predefinida.categoria, esPredeterminada = true,
                    onJugarClick = { onJugar(GestorDatos.convertirPredefinidaAGuardada(predefinida)) },
                    onInfoClick = { coleccionViendoInfo = GestorDatos.convertirPredefinidaAGuardada(predefinida) }
                )
                Spacer(modifier = Modifier.width(16.dp))
            }
        }

        Spacer(modifier = Modifier.height(64.dp))
    }

    if (coleccionViendoInfo != null) {
        DialogoInfoColeccion(coleccion = coleccionViendoInfo!!, onCerrar = { coleccionViendoInfo = null })
    }
}

@Composable
fun SeccionTitulo(titulo: String, icono: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icono, contentDescription = null, tint = Color.Gray)
        Spacer(modifier = Modifier.width(8.dp))
        Text(titulo, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
    }
}

@Composable
fun TarjetaColeccionInicio(
    nombre: String, categoria: String, esPredeterminada: Boolean, onJugarClick: () -> Unit, onInfoClick: () -> Unit
) {
    // 👇 AQUÍ DEFINIMOS LOS 2 DISEÑOS (Premium vs Oficial)
    val containerColor = if (esPredeterminada) Color(0xFFE0F2F1) else Color(0xFF181818) // Menta vs Oscuro
    val textColor = if (esPredeterminada) Color(0xFF121212) else Color.White
    val categoryColor = if (esPredeterminada) Color(0xFF00897B) else Color(0xFFFF6D00) // Turquesa oscuro vs Naranja
    val infoColor = if (esPredeterminada) Color.Gray else Color.LightGray
    val boxColor = if (esPredeterminada) Color(0xFF18C1A8) else Color(0xFFFF6D00) // Cuadro de letra Turquesa vs Naranja

    Card(
        modifier = Modifier.width(200.dp).height(if (esPredeterminada) 240.dp else 280.dp).clip(RoundedCornerShape(16.dp)).clickable { onJugarClick() },
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (esPredeterminada) 0.dp else 4.dp), // Las predeterminadas son más planas
        border = if (esPredeterminada) BorderStroke(1.dp, Color(0xFFB2DFDB)) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(modifier = Modifier.size(64.dp).background(boxColor, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Text(nombre.take(1).uppercase(), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(nombre, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor, maxLines = 2)
            Spacer(modifier = Modifier.height(4.dp))
            Text(categoria.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = categoryColor)
            Spacer(modifier = Modifier.weight(1f))
            HorizontalDivider(color = infoColor, thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onInfoClick) { Icon(Icons.Rounded.Info, contentDescription = "Información", tint = infoColor) }
                Button(
                    onClick = onJugarClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF18C1A8)),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = "Jugar", modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("JUGAR", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun DialogoInfoColeccion(coleccion: ColeccionGuardada, onCerrar: () -> Unit) {
    var textoBusqueda by remember { mutableStateOf("") }
    val todasLasPalabras = remember(coleccion) {
        coleccion.elementos.flatMap { elemento ->
            when (elemento) {
                is ElementoGuardado.Individual -> listOf(elemento.palabra)
                is ElementoGuardado.Conjunto -> elemento.palabras.map { it.palabra }
            }
        }
    }
    val palabrasFiltradas = todasLasPalabras.filter { it.contains(textoBusqueda, ignoreCase = true) }

    Dialog(onDismissRequest = onCerrar) {
        Card(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(coleccion.nombre, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(coleccion.categoria, color = Color(0xFFFF6D00), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    IconButton(onClick = onCerrar) { Icon(Icons.Rounded.Close, contentDescription = "Cerrar") }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = textoBusqueda, onValueChange = { textoBusqueda = it }, placeholder = { Text("Buscar palabra...") }, leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = Color.Gray) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(50), singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Contiene ${todasLasPalabras.size} palabras en total", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(palabrasFiltradas) { palabra ->
                        Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFFF5F5F5), shape = RoundedCornerShape(8.dp)) {
                            Text(text = palabra, modifier = Modifier.padding(12.dp), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}