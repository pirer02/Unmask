import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image as ComposeImage
import org.example.project.decodificarBase64Imagen // 👈 Importa la función que creamos antes
import org.example.project.Datos.*

enum class VistaExplorar {
    FEED, PERFIL_USUARIO
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaExplorar(
    onVolver: () -> Unit,
    onIrAPerfilLogin: () -> Unit,
    onJugarColeccion: (ColeccionGuardada) -> Unit,
    onEditar: (ColeccionGuardada) -> Unit
) {
    val usuario by GestorAuth.usuario.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var vistaActual by remember { mutableStateOf(VistaExplorar.FEED) }

    var textoBusquedaUsuarios by remember { mutableStateOf("") }
    var resultadosBusqueda by remember { mutableStateOf<List<PerfilSocial>>(emptyList()) }
    var buscandoUsuarios by remember { mutableStateOf(false) }

    var perfilSeleccionado by remember { mutableStateOf<PerfilSocial?>(null) }
    var listasPerfilAjeno by remember { mutableStateOf<List<ColeccionGuardada>>(emptyList()) }
    var cargandoPerfil by remember { mutableStateOf(false) }
    var loSigo by remember { mutableStateOf(false) }
    var contadorSeguidores by remember { mutableStateOf(0) }

    var feedGlobal by remember { mutableStateOf<List<ColeccionGuardada>>(emptyList()) }
    var cargandoFeed by remember { mutableStateOf(true) }
    var filtroSeleccionado by remember { mutableStateOf("Nuevas") }
    var vistaLimitada100 by remember { mutableStateOf(true) }

    var coleccionViendoInfo by remember { mutableStateOf<ColeccionGuardada?>(null) }
    // 👇 NUEVO: Estado para el diálogo de confirmación al borrar una descarga
    var coleccionParaBorrarDescarga by remember { mutableStateOf<ColeccionGuardada?>(null) }

    LaunchedEffect(usuario, vistaActual) {
        if (usuario != null && vistaActual == VistaExplorar.FEED && feedGlobal.isEmpty()) {
            cargandoFeed = true
            feedGlobal = GestorAuth.obtenerFeedGlobal(if (vistaLimitada100) 100 else 500, usuario?.uid)
            cargandoFeed = false
        }
    }

    LaunchedEffect(textoBusquedaUsuarios) {
        if (textoBusquedaUsuarios.length >= 2) {
            buscandoUsuarios = true
            delay(600)
            resultadosBusqueda = GestorAuth.buscarUsuariosPorNombre(textoBusquedaUsuarios, usuario?.uid)
            buscandoUsuarios = false
        } else {
            resultadosBusqueda = emptyList()
        }
    }

    fun abrirPerfil(uidDestino: String) {
        scope.launch {
            cargandoPerfil = true
            vistaActual = VistaExplorar.PERFIL_USUARIO
            textoBusquedaUsuarios = ""

            val perfil = GestorAuth.obtenerPerfilSocial(uidDestino)
            if (perfil != null) {
                perfilSeleccionado = perfil
                contadorSeguidores = perfil.seguidores.size
                listasPerfilAjeno = GestorAuth.obtenerListasPublicas(uidDestino)
                loSigo = usuario?.uid?.let { miUid -> perfil.seguidores.contains(miUid) } ?: false
            }
            cargandoPerfil = false
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFF9F9F9)
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp, bottom = paddingValues.calculateBottomPadding())
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // --- ENCABEZADO GLOBAL ---
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = {
                    if (vistaActual == VistaExplorar.PERFIL_USUARIO) {
                        vistaActual = VistaExplorar.FEED
                    } else {
                        onVolver()
                    }
                }) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Volver", tint = Color(0xFF1A1A1A))
                }
                Text(
                    text = if (vistaActual == VistaExplorar.PERFIL_USUARIO) "Perfil de @${perfilSeleccionado?.username ?: ""}" else "Explorar",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            // --- CONTENIDO DINÁMICO ---
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (usuario == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                            Icon(Icons.Rounded.Public, null, modifier = Modifier.size(80.dp), tint = Color.LightGray)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Explora la comunidad", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Text(
                                "Inicia sesión para buscar amigos, descargar listas y jugar las creaciones de otros investigadores.",
                                textAlign = TextAlign.Center,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = onIrAPerfilLogin,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF18C1A8)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(56.dp)
                            ) {
                                Text("IR A MI PERFIL", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    when (vistaActual) {
                        VistaExplorar.FEED -> {
                            Column(modifier = Modifier.fillMaxSize()) {
                                OutlinedTextField(
                                    value = textoBusquedaUsuarios,
                                    onValueChange = { textoBusquedaUsuarios = it },
                                    placeholder = { Text("Buscar usuarios...") },
                                    leadingIcon = { Icon(Icons.Rounded.Search, null, tint = Color.Gray) },
                                    trailingIcon = {
                                        if (textoBusquedaUsuarios.isNotEmpty()) {
                                            IconButton(onClick = { textoBusquedaUsuarios = "" }) { Icon(Icons.Rounded.Clear, null) }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(50)),
                                    shape = RoundedCornerShape(50),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFFFF6D00),
                                        unfocusedBorderColor = Color.LightGray
                                    ),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                if (textoBusquedaUsuarios.isNotEmpty()) {
                                    if (buscandoUsuarios) {
                                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                            CircularProgressIndicator(color = Color(0xFFFF6D00))
                                        }
                                    } else if (resultadosBusqueda.isEmpty()) {
                                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                            Text("No se encontraron investigadores", color = Color.Gray)
                                        }
                                    } else {
                                        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
                                            items(resultadosBusqueda) { perfil ->
                                                TarjetaUsuarioBuscado(
                                                    perfil = perfil,
                                                    onClick = { abrirPerfil(perfil.uid) }
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                            }
                                        }
                                    }
                                } else {
                                    LazyRow(
                                        contentPadding = PaddingValues(0.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    ) {
                                        val filtros = listOf("Nuevas", "Populares", "Más palabras")
                                        items(filtros) { filtro ->
                                            FilterChip(
                                                selected = filtroSeleccionado == filtro,
                                                onClick = { filtroSeleccionado = filtro },
                                                label = { Text(filtro) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = Color(0xFFFF6D00),
                                                    selectedLabelColor = Color.White
                                                )
                                            )
                                        }
                                        item {
                                            FilterChip(
                                                selected = !vistaLimitada100,
                                                onClick = {
                                                    vistaLimitada100 = !vistaLimitada100
                                                    scope.launch {
                                                        cargandoFeed = true
                                                        feedGlobal = GestorAuth.obtenerFeedGlobal(if (vistaLimitada100) 100 else 500, usuario?.uid)
                                                        cargandoFeed = false
                                                    }
                                                },
                                                label = { Text("Ver todo") },
                                                leadingIcon = { Icon(Icons.Rounded.AllInclusive, null, modifier = Modifier.size(16.dp)) }
                                            )
                                        }
                                    }

                                    if (cargandoFeed) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            CircularProgressIndicator(color = Color(0xFFFF6D00))
                                        }
                                    } else if (feedGlobal.isEmpty()) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text("No hay listas públicas en la comunidad aún.", color = Color.Gray)
                                        }
                                    } else {
                                        val feedOrdenado = when (filtroSeleccionado) {
                                            "Populares" -> feedGlobal.sortedByDescending { it.likes }
                                            "Más palabras" -> feedGlobal.sortedByDescending { col -> col.elementos.sumOf { if (it is ElementoGuardado.Individual) 1 else (it as ElementoGuardado.Conjunto).palabras.size } }
                                            else -> feedGlobal
                                        }

                                        LazyColumn(
                                            modifier = Modifier.fillMaxSize(),
                                            contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
                                            verticalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            items(feedOrdenado) { coleccion ->
                                                TarjetaComunidad(
                                                    coleccion = coleccion,
                                                    autor = coleccion.nombreCreador ?: "Anónimo",
                                                    miUid = usuario?.uid,
                                                    onAutorClick = {
                                                        coleccion.idCreador?.let { uid -> abrirPerfil(uid) }
                                                    },
                                                    onJugar = { onJugarColeccion(coleccion) },
                                                    onDescargar = {
                                                        // 👇 NUEVO: Comprobamos si la lista ya está guardada para mostrar aviso o descargar
                                                        val yaDescargada = GestorDatos.coleccionesGlobales.any { it.nombre == coleccion.nombre && it.idCreador == coleccion.idCreador }
                                                        if (yaDescargada) {
                                                            coleccionParaBorrarDescarga = coleccion
                                                        } else {
                                                            val copiaParaMi = coleccion.copy(esDescargada = true)
                                                            GestorDatos.guardarNuevaColeccion(copiaParaMi)
                                                            scope.launch {
                                                                usuario?.uid?.let { miUid ->
                                                                    GestorDatos.subirColeccionNube(miUid, copiaParaMi)
                                                                }
                                                                snackbarHostState.showSnackbar("Guardada en tu biblioteca online")
                                                            }
                                                        }
                                                    },
                                                    onLike = { sumar ->
                                                        coleccion.idCreador?.let { uidAutor ->
                                                            usuario?.uid?.let { miUid ->
                                                                scope.launch {
                                                                    GestorAuth.darLike(uidAutor, coleccion.nombre, miUid, sumar)
                                                                }
                                                            }
                                                        }
                                                    },
                                                    onInfo = { coleccionViendoInfo = coleccion }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        VistaExplorar.PERFIL_USUARIO -> {
                            if (cargandoPerfil) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = Color(0xFFFF6D00))
                                }
                            } else if (perfilSeleccionado != null) {
                                Column(modifier = Modifier.fillMaxSize()) {

                                    Surface(
                                        color = Color.White,
                                        shadowElevation = 2.dp,
                                        shape = RoundedCornerShape(24.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(20.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(modifier = Modifier.size(72.dp).background(Color(0xFFE0F2F1), CircleShape), contentAlignment = Alignment.Center) {
                                                        val urlFoto = perfilSeleccionado!!.fotoPerfil
                                                        if (!urlFoto.isNullOrEmpty()) {
                                                            if (urlFoto.startsWith("http")) {
                                                                KamelImage(asyncPainterResource(urlFoto), null, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                                                            } else {
                                                                // 👇 NUEVO: Decodificar si es Base64
                                                                val bitmap = decodificarBase64Imagen(urlFoto)
                                                                if (bitmap != null) {
                                                                    ComposeImage(bitmap = bitmap, contentDescription = null, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                                                                } else {
                                                                    Text(perfilSeleccionado!!.username.take(1).uppercase(), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00897B))
                                                                }
                                                            }
                                                        } else {
                                                            Text(perfilSeleccionado!!.username.take(1).uppercase(), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00897B))
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.width(16.dp))
                                                    Column {
                                                        Text("@${perfilSeleccionado!!.username}", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                                                        Text("${listasPerfilAjeno.size} listas públicas", color = Color.Gray, fontSize = 14.sp)
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(24.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(horizontalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.weight(1f)) {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text("$contadorSeguidores", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                                        Text("Seguidores", color = Color.Gray, fontSize = 12.sp)
                                                    }
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text("${perfilSeleccionado!!.seguidos.size}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                                        Text("Siguiendo", color = Color.Gray, fontSize = 12.sp)
                                                    }
                                                }

                                                Button(
                                                    onClick = {
                                                        scope.launch {
                                                            usuario?.uid?.let { miUid ->
                                                                val nuevoEstado = !loSigo
                                                                loSigo = nuevoEstado
                                                                contadorSeguidores += if (nuevoEstado) 1 else -1

                                                                val exito = GestorAuth.alternarSeguimiento(miUid, perfilSeleccionado!!.uid, nuevoEstado)
                                                                if (!exito) {
                                                                    loSigo = !nuevoEstado
                                                                    contadorSeguidores += if (!nuevoEstado) 1 else -1
                                                                    snackbarHostState.showSnackbar("Error al actualizar seguimiento")
                                                                }
                                                            }
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = if (loSigo) Color.White else Color(0xFF18C1A8),
                                                        contentColor = if (loSigo) Color.Black else Color.White
                                                    ),
                                                    border = if (loSigo) BorderStroke(1.dp, Color.LightGray) else null,
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.height(40.dp)
                                                ) {
                                                    Text(if (loSigo) "Siguiendo" else "Seguir", fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(bottom = 16.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        if (listasPerfilAjeno.isEmpty()) {
                                            item {
                                                Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                                                    Text("Este usuario no tiene listas públicas.", color = Color.Gray)
                                                }
                                            }
                                        } else {
                                            items(listasPerfilAjeno) { coleccion ->
                                                TarjetaComunidad(
                                                    coleccion = coleccion,
                                                    autor = perfilSeleccionado!!.username,
                                                    miUid = usuario?.uid,
                                                    onAutorClick = null,
                                                    onJugar = { onJugarColeccion(coleccion) },
                                                    onDescargar = {
                                                        // 👇 NUEVO: Mismo control para el botón desde el Perfil de Usuario
                                                        val yaDescargada = GestorDatos.coleccionesGlobales.any { it.nombre == coleccion.nombre && it.idCreador == coleccion.idCreador }
                                                        if (yaDescargada) {
                                                            coleccionParaBorrarDescarga = coleccion
                                                        } else {
                                                            val copiaParaMi = coleccion.copy(esDescargada = true)
                                                            GestorDatos.guardarNuevaColeccion(copiaParaMi)
                                                            scope.launch {
                                                                usuario?.uid?.let { miUid ->
                                                                    GestorDatos.subirColeccionNube(miUid, copiaParaMi)
                                                                }
                                                                snackbarHostState.showSnackbar("Guardada en tu biblioteca online")
                                                            }
                                                        }
                                                    },
                                                    onLike = { sumar ->
                                                        coleccion.idCreador?.let { uidAutor ->
                                                            usuario?.uid?.let { miUid ->
                                                                scope.launch { GestorAuth.darLike(uidAutor, coleccion.nombre, miUid, sumar) }
                                                            }
                                                        }
                                                    },
                                                    onInfo = { coleccionViendoInfo = coleccion }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (coleccionViendoInfo != null) {
        DialogoInfoExplorar(
            coleccion = coleccionViendoInfo!!,
            onCerrar = { coleccionViendoInfo = null }
        )
    }

    // 👇 NUEVO: Diálogo de alerta si intentan borrar una lista desde la pantalla explorar
    if (coleccionParaBorrarDescarga != null) {
        AlertDialog(
            onDismissRequest = { coleccionParaBorrarDescarga = null },
            title = { Text("¿Eliminar descarga?") },
            text = { Text("Ya tienes la lista '${coleccionParaBorrarDescarga?.nombre}' guardada en tu biblioteca. ¿Deseas eliminarla?") },
            confirmButton = {
                TextButton(onClick = {
                    coleccionParaBorrarDescarga?.let { col ->
                        val colLocal = GestorDatos.coleccionesGlobales.find { it.nombre == col.nombre && it.idCreador == col.idCreador }
                        if (colLocal != null) {
                            // Borrado en el teléfono
                            GestorDatos.coleccionesGlobales.remove(colLocal)
                            GestorDatos.guardarCambiosMemoria()

                            // Borrado en Firebase
                            scope.launch {
                                usuario?.uid?.let { miUid ->
                                    try {
                                        GestorAuth.firestore.collection("usuarios").document(miUid)
                                            .collection("colecciones").document(col.nombre).delete()
                                    } catch (e: Exception) {}
                                }
                            }
                            scope.launch { snackbarHostState.showSnackbar("Eliminada de tu biblioteca") }
                        }
                    }
                    coleccionParaBorrarDescarga = null
                }) { Text("ELIMINAR", color = Color.Red, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { coleccionParaBorrarDescarga = null }) { Text("CANCELAR", color = Color.Gray) }
            }
        )
    }
}

@Composable
fun DialogoInfoExplorar(coleccion: ColeccionGuardada, onCerrar: () -> Unit) {
    var textoBusquedaPalabra by remember { mutableStateOf("") }
    val todasLasPalabras = remember(coleccion) {
        coleccion.elementos.flatMap { elemento ->
            when (elemento) {
                is ElementoGuardado.Individual -> listOf(elemento.palabra)
                is ElementoGuardado.Conjunto -> elemento.palabras.map { it.palabra }
            }
        }
    }
    val filtradas = todasLasPalabras.filter { it.contains(textoBusquedaPalabra, ignoreCase = true) }

    Dialog(onDismissRequest = onCerrar) {
        Card(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(coleccion.nombre, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                        Text(coleccion.categoria.uppercase(), color = Color(0xFFFF6D00), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    IconButton(onClick = onCerrar) { Icon(Icons.Rounded.Close, null) }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = textoBusquedaPalabra,
                    onValueChange = { textoBusquedaPalabra = it },
                    placeholder = { Text("Buscar palabra en esta lista...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, null, tint = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(50),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text("${todasLasPalabras.size} palabras totales", color = Color.Gray, fontSize = 13.sp)

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filtradas) { palabra ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFFF5F5F5),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(palabra, modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TarjetaUsuarioBuscado(perfil: PerfilSocial, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).background(Color(0xFFFFF4E6), CircleShape), contentAlignment = Alignment.Center) {
                val urlFoto = perfil.fotoPerfil
                if (!urlFoto.isNullOrEmpty()) {
                    if (urlFoto.startsWith("http")) {
                        KamelImage(asyncPainterResource(urlFoto), null, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                    } else {
                        // 👇 NUEVO: Decodificar si es Base64
                        val bitmap = decodificarBase64Imagen(urlFoto)
                        if (bitmap != null) {
                            ComposeImage(bitmap = bitmap, contentDescription = null, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                        } else {
                            Text(perfil.username.take(1).uppercase(), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF6D00))
                        }
                    }
                } else {
                    Text(perfil.username.take(1).uppercase(), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF6D00))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("@${perfil.username}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("${perfil.seguidores.size} seguidores", fontSize = 12.sp, color = Color.Gray)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = Color.LightGray)
        }
    }
}

@Composable
fun TarjetaComunidad(
    coleccion: ColeccionGuardada,
    autor: String,
    miUid: String?,
    onAutorClick: (() -> Unit)?,
    onJugar: () -> Unit,
    onDescargar: () -> Unit,
    onLike: (Boolean) -> Unit,
    onInfo: () -> Unit
) {
    val totalPalabras = coleccion.elementos.sumOf { elemento ->
        when (elemento) {
            is ElementoGuardado.Individual -> 1
            is ElementoGuardado.Conjunto -> elemento.palabras.size
        }
    }

    var likeDado by remember { mutableStateOf(miUid != null && coleccion.usuariosLikes.contains(miUid)) }
    var likesActuales by remember { mutableStateOf(coleccion.likes) }

    // 👇 NUEVO: Consultamos en tiempo real si esta tarjeta específica ya está descargada
    val estaDescargada = GestorDatos.coleccionesGlobales.any { it.nombre == coleccion.nombre && it.idCreador == coleccion.idCreador }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFE0F2F1))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = Color(0xFFE0F2F1), shape = RoundedCornerShape(8.dp)) {
                    Text(coleccion.categoria.uppercase(), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00897B))
                }
                Spacer(modifier = Modifier.weight(1f))

                IconButton(onClick = onInfo, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.Info, null, tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(coleccion.nombre, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)

            Text(
                text = "Por @$autor",
                fontSize = 12.sp,
                color = Color(0xFFFF6D00),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.let {
                    if (onAutorClick != null) it.clickable { onAutorClick() }.padding(vertical = 4.dp) else it
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        likeDado = !likeDado
                        likesActuales += if (likeDado) 1 else -1
                        onLike(likeDado)
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (likeDado) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Like",
                        tint = Color(0xFFFF3D00)
                    )
                }
                Spacer(Modifier.width(4.dp))
                Text("$likesActuales", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                Spacer(modifier = Modifier.weight(1f))

                // 👇 NUEVO: El icono cambia de CloudDownload a CloudDone si la tenemos
                IconButton(onClick = onDescargar, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = if (estaDescargada) Icons.Rounded.CloudDone else Icons.Rounded.CloudDownload,
                        contentDescription = if (estaDescargada) "Eliminar descarga" else "Descargar",
                        tint = if (estaDescargada) Color(0xFF00897B) else Color(0xFF18C1A8) // Un tono un poco más oscuro si ya la tienes
                    )
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = onJugar,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6D00)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("JUGAR", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}