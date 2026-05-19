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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image as ComposeImage
import org.example.project.decodificarBase64Imagen
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.text.style.TextOverflow
import org.example.project.Datos.*
import org.example.project.Datos.TextosTraducidos.TextosExplorar
import org.example.project.Datos.TextosTraducidos.obtenerTextosExplorar

enum class VistaExplorar {
    FEED, PERFIL_USUARIO
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaExplorar(
    uidInicial: String? = null,
    onLimpiarUidInicial: () -> Unit = {},
    onVolver: () -> Unit,
    onIrAPerfilLogin: () -> Unit,
    onJugarColeccion: (ColeccionGuardada) -> Unit,
    onEditar: (ColeccionGuardada) -> Unit
) {
    val usuario by GestorAuth.usuario.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // --- LÓGICA DE IDIOMAS ---
    val idiomaActual by GestorIdiomas.idiomaActual.collectAsState()
    val textos = obtenerTextosExplorar(idiomaActual)
    // -------------------------

    var vistaActual by remember { mutableStateOf(VistaExplorar.FEED) }

    var textoBusqueda by remember { mutableStateOf("") }
    // Mantenemos los valores internos fijos para no romper tu lógica
    var tipoBusqueda by remember { mutableStateOf("Listas") }

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
    var coleccionParaBorrarDescarga by remember { mutableStateOf<ColeccionGuardada?>(null) }

    val pasoTutorial = GestorDatos.pasoTutorialActual

    LaunchedEffect(usuario, vistaActual) {
        if (usuario != null && vistaActual == VistaExplorar.FEED && feedGlobal.isEmpty()) {
            cargandoFeed = true
            feedGlobal = GestorAuth.obtenerFeedGlobal(if (vistaLimitada100) 100 else 500, usuario?.uid)
            cargandoFeed = false
        }
    }

    LaunchedEffect(textoBusqueda, tipoBusqueda) {
        if (tipoBusqueda == "Usuarios" && textoBusqueda.length >= 2) {
            buscandoUsuarios = true
            delay(600)
            resultadosBusqueda = GestorAuth.buscarUsuariosPorNombre(textoBusqueda, usuario?.uid)
            buscandoUsuarios = false
        } else if (tipoBusqueda == "Usuarios") {
            resultadosBusqueda = emptyList()
        }
    }

    fun abrirPerfil(uidDestino: String) {
        scope.launch {
            cargandoPerfil = true
            vistaActual = VistaExplorar.PERFIL_USUARIO
            textoBusqueda = ""

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

    LaunchedEffect(uidInicial) {
        if (uidInicial != null) {
            abrirPerfil(uidInicial)
            onLimpiarUidInicial()
        }
    }

    androidx.activity.compose.BackHandler(enabled = pasoTutorial == 5) {
        scope.launch { snackbarHostState.showSnackbar(textos.msgAvisoTutorialVolver) }
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

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                // MODIFICACIÓN: La flecha solo aparece si estamos dentro del perfil de un usuario ajeno
                if (vistaActual == VistaExplorar.PERFIL_USUARIO) {
                    IconButton(onClick = {
                        if (pasoTutorial == 5) {
                            scope.launch { snackbarHostState.showSnackbar(textos.msgAvisoTutorialVolver) }
                        } else {
                            vistaActual = VistaExplorar.FEED
                        }
                    }) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Volver", tint = Color(0xFF1A1A1A))
                    }
                }
                Text(
                    text = if (vistaActual == VistaExplorar.PERFIL_USUARIO) "${textos.tituloPerfil} @${perfilSeleccionado?.username ?: ""}" else textos.tituloExplorar,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (usuario == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                            Icon(Icons.Rounded.Public, null, modifier = Modifier.size(80.dp), tint = Color.LightGray)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(textos.tituloExploraComunidad, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Text(
                                textos.descExploraComunidad,
                                textAlign = TextAlign.Center,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = {
                                    if (pasoTutorial == 5) {
                                        scope.launch { snackbarHostState.showSnackbar(textos.msgAvisoTutorialPerfil) }
                                    } else {
                                        onIrAPerfilLogin()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF18C1A8)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(56.dp)
                            ) {
                                Text(textos.btnIrPerfil, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    when (vistaActual) {
                        VistaExplorar.FEED -> {
                            Column(modifier = Modifier.fillMaxSize()) {

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                    FilterChip(
                                        selected = tipoBusqueda == "Listas",
                                        onClick = { tipoBusqueda = "Listas"; textoBusqueda = "" },
                                        label = { Text(textos.filtroListas, fontWeight = FontWeight.Bold) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF18C1A8),
                                            selectedLabelColor = Color.White
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = tipoBusqueda == "Listas", borderColor = Color.Transparent)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    FilterChip(
                                        selected = tipoBusqueda == "Usuarios",
                                        onClick = { tipoBusqueda = "Usuarios"; textoBusqueda = "" },
                                        label = { Text(textos.filtroUsuarios, fontWeight = FontWeight.Bold) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF18C1A8),
                                            selectedLabelColor = Color.White
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = tipoBusqueda == "Usuarios", borderColor = Color.Transparent)
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))

                                OutlinedTextField(
                                    value = textoBusqueda,
                                    onValueChange = { textoBusqueda = it },
                                    placeholder = { Text(if (tipoBusqueda == "Listas") textos.buscarListas else textos.buscarUsuarios) },
                                    leadingIcon = { Icon(Icons.Rounded.Search, null, tint = Color.Gray) },
                                    trailingIcon = {
                                        if (textoBusqueda.isNotEmpty()) {
                                            IconButton(onClick = { textoBusqueda = "" }) { Icon(Icons.Rounded.Clear, null) }
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

                                if (tipoBusqueda == "Usuarios") {
                                    if (textoBusqueda.isEmpty()) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                                            Text(textos.escribeNombreBuscar, color = Color.Gray, modifier = Modifier.padding(top = 32.dp))
                                        }
                                    } else {
                                        if (buscandoUsuarios) {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                CircularProgressIndicator(color = Color(0xFFFF6D00))
                                            }
                                        } else if (resultadosBusqueda.isEmpty()) {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                Text(textos.noInvestigadores, color = Color.Gray)
                                            }
                                        } else {
                                            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
                                                items(resultadosBusqueda) { perfil ->
                                                    TarjetaUsuarioBuscado(
                                                        perfil = perfil,
                                                        textos = textos,
                                                        onClick = { abrirPerfil(perfil.uid) }
                                                    )
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    if (textoBusqueda.isEmpty()) {
                                        LazyRow(
                                            contentPadding = PaddingValues(0.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        ) {
                                            val filtrosInternos = listOf("Nuevas", "Populares", "Más palabras")
                                            val filtrosUi = listOf(textos.filtroNuevas, textos.filtroPopulares, textos.filtroMasPalabras)

                                            items(filtrosInternos.size) { index ->
                                                val filtroI = filtrosInternos[index]
                                                val filtroU = filtrosUi[index]
                                                FilterChip(
                                                    selected = filtroSeleccionado == filtroI,
                                                    onClick = { filtroSeleccionado = filtroI },
                                                    label = { Text(filtroU) },
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
                                                    label = { Text(textos.verTodo) },
                                                    leadingIcon = { Icon(Icons.Rounded.AllInclusive, null, modifier = Modifier.size(16.dp)) }
                                                )
                                            }
                                        }
                                    } else {
                                        Text("${textos.resultadosPara} '$textoBusqueda'", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                                    }

                                    if (cargandoFeed) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            CircularProgressIndicator(color = Color(0xFFFF6D00))
                                        }
                                    } else if (feedGlobal.isEmpty()) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text(textos.noListasComunidad, color = Color.Gray)
                                        }
                                    } else {
                                        val feedMostrado = if (textoBusqueda.isNotEmpty()) {
                                            feedGlobal.filter {
                                                it.nombre.contains(textoBusqueda, ignoreCase = true) ||
                                                        it.categoria.contains(textoBusqueda, ignoreCase = true) ||
                                                        (it.nombreCreador?.contains(textoBusqueda, ignoreCase = true) == true)
                                            }
                                        } else {
                                            when (filtroSeleccionado) {
                                                "Populares" -> feedGlobal.sortedByDescending { it.likes }
                                                "Más palabras" -> feedGlobal.sortedByDescending { col -> col.elementos.sumOf { if (it is ElementoGuardado.Individual) 1 else (it as ElementoGuardado.Conjunto).palabras.size } }
                                                else -> feedGlobal
                                            }
                                        }

                                        if (feedMostrado.isEmpty() && textoBusqueda.isNotEmpty()) {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                Text(textos.noListasBusqueda, color = Color.Gray)
                                            }
                                        } else {
                                            LazyVerticalGrid(
                                                columns = GridCells.Fixed(2),
                                                modifier = Modifier.fillMaxSize(),
                                                contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
                                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                items(feedMostrado) { coleccion ->
                                                    TarjetaComunidad(
                                                        coleccion = coleccion,
                                                        autor = coleccion.nombreCreador ?: "Anónimo",
                                                        miUid = usuario?.uid,
                                                        textos = textos,
                                                        onAutorClick = {
                                                            coleccion.idCreador?.let { uid -> abrirPerfil(uid) }
                                                        },
                                                        onJugar = { onJugarColeccion(coleccion) },
                                                        onDescargar = {
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
                                                                    snackbarHostState.showSnackbar(textos.msgGuardada)
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
                                                        Text("${listasPerfilAjeno.size} ${textos.listasPublicas}", color = Color.Gray, fontSize = 14.sp)
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
                                                        Text(textos.seguidores, color = Color.Gray, fontSize = 12.sp)
                                                    }
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text("${perfilSeleccionado!!.seguidos.size}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                                        Text(textos.siguiendo, color = Color.Gray, fontSize = 12.sp)
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
                                                                    snackbarHostState.showSnackbar(textos.msgErrorSeguir)
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
                                                    Text(if (loSigo) textos.btnSiguiendo else textos.btnSeguir, fontWeight = FontWeight.Bold)
                                                }
                                            }

                                            if (!perfilSeleccionado!!.descripcion.isNullOrBlank()) {
                                                Spacer(modifier = Modifier.height(20.dp))
                                                Surface(
                                                    color = Color(0xFFF0F5F5),
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text(
                                                        text = perfilSeleccionado!!.descripcion!!,
                                                        modifier = Modifier.padding(16.dp),
                                                        fontSize = 14.sp,
                                                        color = Color(0xFF424242),
                                                        textAlign = TextAlign.Center,
                                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    if (listasPerfilAjeno.isEmpty()) {
                                        Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                                            Text(textos.sinListasPublicas, color = Color.Gray)
                                        }
                                    } else {
                                        LazyVerticalGrid(
                                            columns = GridCells.Fixed(2),
                                            modifier = Modifier.fillMaxSize(),
                                            contentPadding = PaddingValues(bottom = 16.dp),
                                            verticalArrangement = Arrangement.spacedBy(16.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            items(listasPerfilAjeno) { coleccion ->
                                                TarjetaComunidad(
                                                    coleccion = coleccion,
                                                    autor = perfilSeleccionado!!.username,
                                                    miUid = usuario?.uid,
                                                    textos = textos,
                                                    onAutorClick = null,
                                                    onJugar = { onJugarColeccion(coleccion) },
                                                    onDescargar = {
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
                                                                snackbarHostState.showSnackbar(textos.msgGuardada)
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
            textos = textos,
            onCerrar = { coleccionViendoInfo = null }
        )
    }

    if (coleccionParaBorrarDescarga != null) {
        AlertDialog(
            onDismissRequest = { coleccionParaBorrarDescarga = null },
            title = { Text(textos.dialogoEliminarDescargaTitulo) },
            text = { Text("${textos.dialogoEliminarDescargaDesc1}${coleccionParaBorrarDescarga?.nombre}${textos.dialogoEliminarDescargaDesc2}") },
            confirmButton = {
                TextButton(onClick = {
                    coleccionParaBorrarDescarga?.let { col ->
                        val colLocal = GestorDatos.coleccionesGlobales.find { it.nombre == col.nombre && it.idCreador == col.idCreador }
                        if (colLocal != null) {
                            GestorDatos.coleccionesGlobales.remove(colLocal)
                            GestorDatos.guardarCambiosMemoria()
                            scope.launch {
                                usuario?.uid?.let { miUid ->
                                    try {
                                        GestorAuth.firestore.collection("usuarios").document(miUid)
                                            .collection("colecciones").document(col.nombre).delete()
                                    } catch (e: Exception) {}
                                }
                            }
                            scope.launch { snackbarHostState.showSnackbar(textos.msgEliminada) }
                        }
                    }
                    coleccionParaBorrarDescarga = null
                }) { Text(textos.btnEliminar, color = Color.Red, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { coleccionParaBorrarDescarga = null }) { Text(textos.btnCancelar, color = Color.Gray) }
            }
        )
    }
}

@Composable
fun DialogoInfoExplorar(coleccion: ColeccionGuardada, textos: TextosExplorar, onCerrar: () -> Unit) {
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
                    placeholder = { Text(textos.buscarPalabraLista) },
                    leadingIcon = { Icon(Icons.Rounded.Search, null, tint = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(50),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text("${todasLasPalabras.size} ${textos.palabrasTotales}", color = Color.Gray, fontSize = 13.sp)

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
fun TarjetaUsuarioBuscado(perfil: PerfilSocial, textos: TextosExplorar, onClick: () -> Unit) {
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
                Text("${perfil.seguidores.size} ${textos.seguidores.lowercase()}", fontSize = 12.sp, color = Color.Gray)
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
    textos: TextosExplorar,
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

    val esColaborador = miUid != null && coleccion.colaboradores.contains(miUid)
    val estaDescargada = GestorDatos.coleccionesGlobales.any { it.nombre == coleccion.nombre && it.idCreador == coleccion.idCreador }

    var urlFoto by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(coleccion.idCreador) {
        if (coleccion.idCreador != null) {
            val perfil = GestorAuth.obtenerPerfilSocial(coleccion.idCreador)
            urlFoto = perfil?.fotoPerfil
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        border = BorderStroke(1.dp, Color(0xFFE0F2F1))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFE0F2F1)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!urlFoto.isNullOrEmpty()) {
                        if (urlFoto!!.startsWith("http")) {
                            KamelImage(asyncPainterResource(urlFoto!!), null, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                        } else {
                            val bitmap = decodificarBase64Imagen(urlFoto!!)
                            if (bitmap != null) {
                                ComposeImage(bitmap = bitmap, contentDescription = null, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                            } else {
                                Text(autor.take(1).uppercase(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00897B))
                            }
                        }
                    } else {
                        Text(autor.take(1).uppercase(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00897B))
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${textos.por} @$autor",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF6D00),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.let {
                            if (onAutorClick != null) it.clickable { onAutorClick() } else it
                        }
                    )
                    Text("$totalPalabras ${textos.pal}", fontSize = 10.sp, color = Color.Gray, maxLines = 1)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = {
                            likeDado = !likeDado
                            likesActuales += if (likeDado) 1 else -1
                            onLike(likeDado)
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (likeDado) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = "Like",
                            tint = Color(0xFFFF3D00),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text("$likesActuales", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(coleccion.nombre, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1A1A1A), maxLines = 2, overflow = TextOverflow.Ellipsis)

            Spacer(modifier = Modifier.height(6.dp))

            Surface(color = Color(0xFFE0F2F1), shape = RoundedCornerShape(8.dp)) {
                Text(coleccion.categoria.uppercase(), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00897B), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            Spacer(modifier = Modifier.weight(1f))
            HorizontalDivider(color = Color(0xFFE0F2F1), thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onInfo, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Rounded.Info, contentDescription = "Información", tint = Color.Gray, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(4.dp))

                    if (esColaborador) {
                        Icon(Icons.Rounded.Group, contentDescription = "Colaborador", tint = Color(0xFFFF6D00), modifier = Modifier.size(18.dp).padding(4.dp))
                    } else {
                        IconButton(onClick = onDescargar, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = if (estaDescargada) Icons.Rounded.CloudDone else Icons.Rounded.CloudDownload,
                                contentDescription = if (estaDescargada) "Eliminar descarga" else "Descargar",
                                tint = if (estaDescargada) Color(0xFF00897B) else Color(0xFF18C1A8),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Button(
                    onClick = onJugar,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6D00)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = "Jugar", modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(2.dp))
                    Text(textos.btnJugar, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}