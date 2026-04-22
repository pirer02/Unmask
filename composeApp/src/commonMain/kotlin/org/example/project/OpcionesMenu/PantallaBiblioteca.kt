import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.Datos.*

@Composable
fun PantallaBiblioteca(
    onEditar: (ColeccionGuardada) -> Unit,
    onJugar: (ColeccionGuardada) -> Unit
) {
    val colecciones = GestorDatos.coleccionesGlobales
    var coleccionParaBorrar by remember { mutableStateOf<ColeccionGuardada?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Mi Biblioteca", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        if (colecciones.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                Text("Aún no tienes ninguna lista creada.", color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(colecciones) { coleccion ->
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
                    coleccionParaBorrar?.let {
                        GestorDatos.coleccionesGlobales.remove(it)
                        GestorDatos.guardarCambiosMemoria()
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

// 👇 ESTA ES LA FUNCIÓN QUE USAREMOS EN AMBOS SITIOS
@Composable
fun TarjetaColeccion(
    coleccion: ColeccionGuardada,
    onEliminarClick: () -> Unit,
    onEditarClick: () -> Unit,
    onJugarClick: () -> Unit,
    mostrarAcciones: Boolean = true // Por si en el perfil solo quieres verla sin botones
) {
    val totalPalabras = coleccion.elementos.sumOf { elemento ->
        when (elemento) {
            is ElementoGuardado.Individual -> 1
            is ElementoGuardado.Conjunto -> elemento.palabras.size
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF4E6)),
        border = BorderStroke(1.dp, Color(0xFFFFD8C2)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(coleccion.nombre, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1A1A1A))
                    Text(coleccion.categoria.uppercase(), color = Color(0xFFFF6D00), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                }
                Surface(color = Color.White, border = BorderStroke(1.dp, Color(0xFFFFD8C2)), shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)) {
                    Text(text = "$totalPalabras pal.", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF6D00))
                }
            }

            if (mostrarAcciones) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFFFD8C2), thickness = 1.dp)
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