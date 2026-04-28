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
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import org.example.project.Datos.*
import androidx.compose.foundation.Image as ComposeImage
import org.example.project.decodificarBase64Imagen

@Composable
fun PantallaInicio(
    onJugar: (ColeccionGuardada) -> Unit,
    onGestionarJugadores: () -> Unit,
    onCrearLista: () -> Unit
) {
    // Filtramos para separar las creaciones propias de las descargas online
    val misCreaciones = GestorDatos.coleccionesGlobales.filter { !it.esDescargada }
    val recientes = misCreaciones.takeLast(3).reversed()

    val misDescargas = GestorDatos.coleccionesGlobales.filter { it.esDescargada }
    val descargasRecientes = misDescargas.takeLast(3).reversed()

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
                onClick = { /* Navegación al perfil manejada por App.kt */ },
                modifier = Modifier.size(48.dp).clip(CircleShape)
            ) {
                Icon(Icons.Rounded.AccountCircle, contentDescription = "Foto de perfil", modifier = Modifier.fillMaxSize(), tint = Color(0xFFFF6D00))
            }
        }

        // Botón para gestionar el grupo de jugadores
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable { onGestionarJugadores() },
            colors = CardDefaults.cardColors(containerColor = Color(0xFF18C1A8)),
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

        // 1. CREACIONES RECIENTES
        if (recientes.isNotEmpty()) {
            SeccionTitulo(titulo = "Creaciones Recientes", icono = Icons.Rounded.Stars)
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp)) {
                items(recientes) { coleccion ->
                    TarjetaColeccionInicio(
                        coleccion = coleccion, esPredeterminada = false,
                        onJugarClick = { onJugar(coleccion) }, onInfoClick = { coleccionViendoInfo = coleccion }
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 2. LISTAS DESCARGADAS (Toque azul de Internet)
        if (descargasRecientes.isNotEmpty()) {
            SeccionTitulo(titulo = "Listas Descargadas", icono = Icons.Rounded.CloudDownload)
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp)) {
                items(descargasRecientes) { coleccion ->
                    TarjetaColeccionInicio(
                        coleccion = coleccion, esPredeterminada = false,
                        onJugarClick = { onJugar(coleccion) }, onInfoClick = { coleccionViendoInfo = coleccion }
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 3. COLECCIONES PREDETERMINADAS
        SeccionTitulo(titulo = "Colecciones Predeterminadas", icono = Icons.Rounded.Info)
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp)) {
            items(predeterminadas) { predefinida ->
                val coleccionGuardada = GestorDatos.convertirPredefinidaAGuardada(predefinida)
                TarjetaColeccionInicio(
                    coleccion = coleccionGuardada, esPredeterminada = true,
                    onJugarClick = { onJugar(coleccionGuardada) },
                    onInfoClick = { coleccionViendoInfo = coleccionGuardada }
                )
                Spacer(modifier = Modifier.width(16.dp))
            }
        }

        // 4. SUGERENCIA PARA CREAR LISTA (Solo si no tiene listas propias)
        if (misCreaciones.isEmpty()) {
            Spacer(modifier = Modifier.height(32.dp))
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF4E6)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFFFD8C2))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Rounded.EditNote, contentDescription = null, tint = Color(0xFFFF6D00), modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Crea tu propia lista", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1A1A1A))
                    Text(
                        "Añade tus palabras favoritas y compártelas con la comunidad.",
                        textAlign = TextAlign.Center, fontSize = 14.sp, color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                    )
                    Button(
                        onClick = onCrearLista,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6D00)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("CREAR AHORA", fontWeight = FontWeight.Bold)
                    }
                }
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
    coleccion: ColeccionGuardada,
    esPredeterminada: Boolean,
    onJugarClick: () -> Unit,
    onInfoClick: () -> Unit
) {
    val totalPalabras = coleccion.elementos.sumOf { elemento ->
        when (elemento) {
            is ElementoGuardado.Individual -> 1
            is ElementoGuardado.Conjunto -> elemento.palabras.size
        }
    }

    // 👇 NUEVO: Estado para guardar la foto que descargaremos
    var urlFoto by remember { mutableStateOf<String?>(null) }

    // 👇 NUEVO: Efecto que busca la foto del usuario automáticamente si no es predeterminada
    LaunchedEffect(coleccion.idCreador) {
        if (!esPredeterminada && coleccion.idCreador != null) {
            val perfil = GestorAuth.obtenerPerfilSocial(coleccion.idCreador)
            urlFoto = perfil?.fotoPerfil
        }
    }

    // Configuración de colores según el tipo de lista
    val containerColor: Color
    val textColor: Color
    val categoryColor: Color
    val infoColor: Color
    val picBorderColor: Color
    val border: BorderStroke?

    when {
        esPredeterminada -> {
            containerColor = Color(0xFFE0F2F1)
            textColor = Color(0xFF121212)
            categoryColor = Color(0xFF00897B)
            infoColor = Color.Gray
            picBorderColor = Color(0xFF18C1A8)
            border = BorderStroke(1.dp, Color(0xFFB2DFDB))
        }
        coleccion.esDescargada -> {
            containerColor = Color(0xFFE3F2FD) // Azul internet claro
            textColor = Color(0xFF0D47A1)
            categoryColor = Color(0xFFFF6D00)
            infoColor = Color(0xFF1976D2)
            picBorderColor = Color(0xFF1976D2)
            border = BorderStroke(1.dp, Color(0xFF90CAF9))
        }
        else -> {
            containerColor = Color(0xFF181818)
            textColor = Color.White
            categoryColor = Color(0xFFFF6D00)
            infoColor = Color.LightGray
            picBorderColor = Color(0xFFFF6D00)
            border = null
        }
    }

    Card(
        modifier = Modifier.width(220.dp).height(280.dp).clip(RoundedCornerShape(16.dp)).clickable { onJugarClick() },
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (esPredeterminada || coleccion.esDescargada) 0.dp else 4.dp),
        border = border
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.White).padding(2.dp).clip(CircleShape).background(containerColor),
                    contentAlignment = Alignment.Center
                ) {
                    if (esPredeterminada) {
                        Text("U", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = picBorderColor)
                    } else if (!urlFoto.isNullOrEmpty()) {
                        if (urlFoto!!.startsWith("http")) {
                            KamelImage(asyncPainterResource(urlFoto!!), null, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                        } else {
                            val bitmap = decodificarBase64Imagen(urlFoto!!)
                            if (bitmap != null) {
                                ComposeImage(bitmap = bitmap, contentDescription = null, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                            } else {
                                Text(coleccion.nombreCreador?.take(1)?.uppercase() ?: "I", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = picBorderColor)
                            }
                        }
                    } else {
                        Text(coleccion.nombreCreador?.take(1)?.uppercase() ?: "I", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = picBorderColor)
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (esPredeterminada) "Unmask Oficial" else "Por @${coleccion.nombreCreador ?: "investigador"}",
                        fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor, maxLines = 1
                    )
                    Text("$totalPalabras palabras", fontSize = 10.sp, color = infoColor)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(coleccion.nombre, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = textColor, maxLines = 2, minLines = 2)

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (coleccion.esDescargada && !esPredeterminada) {
                    Icon(Icons.Rounded.Cloud, null, modifier = Modifier.size(14.dp), tint = infoColor)
                    Spacer(Modifier.width(4.dp))
                }
                Text(coleccion.categoria.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = categoryColor)
            }

            Spacer(modifier = Modifier.weight(1f))
            HorizontalDivider(color = infoColor, thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onInfoClick) { Icon(Icons.Rounded.Info, contentDescription = "Información", tint = infoColor) }
                Button(
                    onClick = onJugarClick,
                    colors = ButtonDefaults.buttonColors(containerColor = if (esPredeterminada) Color(0xFF18C1A8) else Color(0xFFFF6D00)),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = "Jugar", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("JUGAR", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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