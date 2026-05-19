package org.example.project

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.example.project.Datos.*

import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewState
import com.multiplatform.webview.web.rememberWebViewNavigator

import com.preat.peekaboo.image.picker.rememberImagePickerLauncher
import com.preat.peekaboo.image.picker.SelectionMode
import com.preat.peekaboo.image.picker.ResizeOptions
import org.example.project.Datos.TextosTraducidos.TextosPerfil
import org.example.project.Datos.TextosTraducidos.obtenerTextosPerfil
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import androidx.compose.foundation.Image as ComposeImage

enum class SeccionPerfil { PRINCIPAL, LISTA_USUARIOS }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalEncodingApi::class)
@Composable
fun PantallaMiPerfil(
    onVolver: () -> Unit,
    onIniciarSesionGoogle: () -> Unit,
    onEditar: (ColeccionGuardada) -> Unit,
    onJugar: (ColeccionGuardada) -> Unit,
    onVerPerfilAjeno: (String) -> Unit
) {
    val usuarioAuth by GestorAuth.usuario.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // --- LÓGICA DE IDIOMAS ---
    val idiomaActual by GestorIdiomas.idiomaActual.collectAsState()
    val textos = obtenerTextosPerfil(idiomaActual)
    // -------------------------

    var seccionActual by remember { mutableStateOf(SeccionPerfil.PRINCIPAL) }
    var estadoPantalla by remember { mutableStateOf("CARGANDO") }
    var clicEnIniciarSesion by remember { mutableStateOf(false) }
    var nombreUsuarioReal by remember { mutableStateOf("") }

    var miPerfilSocial by remember { mutableStateOf<PerfilSocial?>(null) }
    var tituloListaUsuarios by remember { mutableStateOf("") }
    var usuariosAMostrar by remember { mutableStateOf<List<PerfilSocial>>(emptyList()) }

    var cargandoAuth by remember { mutableStateOf(false) }
    var listasLocalesTemp by remember { mutableStateOf<List<ColeccionGuardada>>(emptyList()) }
    var listasSeleccionadas by remember { mutableStateOf<Set<ColeccionGuardada>>(emptySet()) }

    var mostrarDialogoNombre by remember { mutableStateOf(false) }
    var mostrarDialogoCerrar by remember { mutableStateOf(false) }
    var mostrarDialogoBorrar by remember { mutableStateOf(false) }
    var mostrarDialogoMigracion by remember { mutableStateOf(false) }
    var coleccionParaBorrar by remember { mutableStateOf<ColeccionGuardada?>(null) }

    var mostrarDialogoCompartir by remember { mutableStateOf(false) }
    var coleccionACompartir by remember { mutableStateOf<ColeccionGuardada?>(null) }
    var textoBusquedaAmigo by remember { mutableStateOf("") }
    var resultadosAmigos by remember { mutableStateOf<List<PerfilSocial>>(emptyList()) }
    var buscandoAmigos by remember { mutableStateOf(false) }
    var invitacionesEnviadas by remember { mutableStateOf<List<InvitacionColaboracion>>(emptyList()) }

    var mostrarDialogoGestionColaboradores by remember { mutableStateOf(false) }
    var coleccionGestionando by remember { mutableStateOf<ColeccionGuardada?>(null) }
    var perfilesColaboradores by remember { mutableStateOf<List<PerfilSocial>>(emptyList()) }
    var perfilesPendientes by remember { mutableStateOf<List<PerfilSocial>>(emptyList()) }
    var perfilCreador by remember { mutableStateOf<PerfilSocial?>(null) }
    var cargandoColaboradores by remember { mutableStateOf(false) }
    var textoBusquedaColabs by remember { mutableStateOf("") }

    var mostrarMenuFoto by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    var mostrarBuscadorAvatar by remember { mutableStateOf(false) }

    var mostrarMenuOpciones by remember { mutableStateOf(false) }

    var nombreUsuarioInput by remember { mutableStateOf("") }
    var errorNombre by remember { mutableStateOf("") }

    var misCreacionesExpandido by remember { mutableStateOf(false) }
    var textoBusquedaCreaciones by remember { mutableStateOf("") }

    var filtroOrdenCreaciones by remember { mutableStateOf("Nuevas") }

    var mostrarDialogoDescripcion by remember { mutableStateOf(false) }
    var inputDescripcion by remember { mutableStateOf("") }

    val imagePicker = rememberImagePickerLauncher(
        selectionMode = SelectionMode.Single,
        scope = scope,
        resizeOptions = ResizeOptions(width = 400, height = 400, compressionQuality = 0.5),
        onResult = { byteArrays ->
            val bytes = byteArrays.firstOrNull()
            if (bytes != null) {
                val base64String = Base64.encode(bytes)
                scope.launch {
                    usuarioAuth?.uid?.let { uid ->
                        if (GestorAuth.actualizarFotoPerfil(uid, base64String)) {
                            miPerfilSocial = miPerfilSocial?.copy(fotoPerfil = base64String)
                            snackbarHostState.showSnackbar(textos.msgFotoExito)
                        } else {
                            snackbarHostState.showSnackbar(textos.msgFotoError)
                        }
                    }
                }
            }
        }
    )

    LaunchedEffect(mostrarDialogoCompartir, coleccionACompartir) {
        if (mostrarDialogoCompartir && coleccionACompartir != null && usuarioAuth != null) {
            textoBusquedaAmigo = ""
        }
    }

    LaunchedEffect(textoBusquedaAmigo) {
        if (textoBusquedaAmigo.length >= 2) {
            buscandoAmigos = true
            delay(600)
            val res = GestorAuth.buscarUsuariosPorNombre(textoBusquedaAmigo, usuarioAuth?.uid)

            resultadosAmigos = res.filter { perfil ->
                val yaInvitado = invitacionesEnviadas.any { it.uidDestino == perfil.uid && it.nombreLista == coleccionACompartir?.nombre }
                val yaColabora = coleccionACompartir?.colaboradores?.contains(perfil.uid) == true
                val esCreador = coleccionACompartir?.idCreador == perfil.uid
                !yaInvitado && !yaColabora && !esCreador
            }
            buscandoAmigos = false
        } else {
            resultadosAmigos = emptyList()
        }
    }

    LaunchedEffect(usuarioAuth) {
        if (usuarioAuth == null) {
            estadoPantalla = "INVITADO"
        } else {
            invitacionesEnviadas = GestorAuth.obtenerInvitacionesEnviadas(usuarioAuth!!.uid)
            val perfil = GestorAuth.obtenerPerfilSocial(usuarioAuth!!.uid)

            if (perfil != null && perfil.fotoPerfil.isNullOrEmpty() && !usuarioAuth!!.photoURL.isNullOrEmpty()) {
                val urlGoogle = usuarioAuth!!.photoURL!!
                GestorAuth.actualizarFotoPerfil(usuarioAuth!!.uid, urlGoogle)
                miPerfilSocial = perfil.copy(fotoPerfil = urlGoogle)
            } else {
                miPerfilSocial = perfil
            }

            if (clicEnIniciarSesion) {
                listasLocalesTemp = GestorDatos.coleccionesGlobales.toList()
                listasSeleccionadas = listasLocalesTemp.toSet()
                val esNuevo = GestorAuth.necesitaNombreUsuario()

                if (esNuevo) {
                    estadoPantalla = "CONFIGURANDO"
                    mostrarDialogoNombre = true
                } else {
                    nombreUsuarioReal = GestorAuth.obtenerNombreUsuario(usuarioAuth!!.uid) ?: ""
                    if (listasLocalesTemp.isNotEmpty()) {
                        estadoPantalla = "CONFIGURANDO"
                        mostrarDialogoMigracion = true
                    } else {
                        GestorDatos.descargarDatosNube(usuarioAuth!!.uid)
                        snackbarHostState.showSnackbar(textos.msgBienvenidoNuevo)
                        clicEnIniciarSesion = false
                        estadoPantalla = "PERFIL"
                    }
                }
            } else {
                nombreUsuarioReal = GestorAuth.obtenerNombreUsuario(usuarioAuth!!.uid) ?: ""
                GestorDatos.descargarDatosNube(usuarioAuth!!.uid)
                estadoPantalla = "PERFIL"
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFF9F9F9)
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(start = 16.dp, end = 16.dp, bottom = padding.calculateBottomPadding())
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // MODIFICACIÓN: La flecha de retroceso solo aparece si estamos en una subsección como la LISTA_USUARIOS
                if (seccionActual == SeccionPerfil.LISTA_USUARIOS) {
                    IconButton(onClick = { seccionActual = SeccionPerfil.PRINCIPAL }) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = null, tint = Color(0xFF1A1A1A))
                    }
                }
                Text(
                    text = if (seccionActual == SeccionPerfil.LISTA_USUARIOS) tituloListaUsuarios else textos.tituloMiPerfil,
                    fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A)
                )
                Spacer(modifier = Modifier.weight(1f))

                if (estadoPantalla == "PERFIL" && seccionActual == SeccionPerfil.PRINCIPAL) {
                    Box {
                        IconButton(onClick = { mostrarMenuOpciones = true }) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = "Opciones", tint = Color(0xFF1A1A1A))
                        }
                        DropdownMenu(
                            expanded = mostrarMenuOpciones,
                            onDismissRequest = { mostrarMenuOpciones = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            DropdownMenuItem(
                                text = { Text(textos.btnCerrarSesion) },
                                onClick = { mostrarMenuOpciones = false; mostrarDialogoCerrar = true },
                                leadingIcon = { Icon(Icons.Rounded.Logout, null, tint = Color.Gray) }
                            )
                            DropdownMenuItem(
                                text = { Text(textos.btnEliminarCuenta, color = Color.Red) },
                                onClick = { mostrarMenuOpciones = false; mostrarDialogoBorrar = true },
                                leadingIcon = { Icon(Icons.Rounded.Warning, null, tint = Color.Red) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (estadoPantalla) {
                "CARGANDO" -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFFFF6D00))
                }

                "INVITADO" -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(8.dp),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier.size(90.dp).background(Color(0xFFE0F2F1), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.CloudSync, null, modifier = Modifier.size(48.dp), tint = Color(0xFF00897B))
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(textos.tituloSincroniza, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(textos.descSincroniza, textAlign = TextAlign.Center, color = Color.Gray)
                            Spacer(modifier = Modifier.height(32.dp))
                            Button(
                                onClick = { clicEnIniciarSesion = true; estadoPantalla = "CARGANDO"; onIniciarSesionGoogle() },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF18C1A8)),
                                shape = RoundedCornerShape(16.dp)
                            ) { Text(textos.btnIniciarSesion, fontWeight = FontWeight.Bold) }
                        }
                    }
                }

                "PERFIL" -> {
                    if (seccionActual == SeccionPerfil.PRINCIPAL) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(Brush.verticalGradient(listOf(Color(0xFF18C1A8), Color(0xFF00897B))))
                                        .padding(vertical = 32.dp, horizontal = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(contentAlignment = Alignment.BottomEnd) {
                                            val urlFoto = miPerfilSocial?.fotoPerfil ?: ""
                                            val nombreMostrar = miPerfilSocial?.username?.takeIf { it.isNotBlank() } ?: nombreUsuarioReal.takeIf { it.isNotBlank() } ?: "investigador"

                                            Box(
                                                modifier = Modifier.size(120.dp).background(Color.White, CircleShape).padding(4.dp).clip(CircleShape).background(Color(0xFFFFD8C2)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (urlFoto.isNotEmpty()) {
                                                    if (urlFoto.startsWith("http")) {
                                                        KamelImage(asyncPainterResource(urlFoto), null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                                    } else {
                                                        val bitmap = decodificarBase64Imagen(urlFoto)
                                                        if (bitmap != null) {
                                                            ComposeImage(bitmap = bitmap, contentDescription = "Foto de perfil", modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                                                        } else {
                                                            Text(nombreMostrar.take(1).uppercase(), fontSize = 48.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF6D00))
                                                        }
                                                    }
                                                } else {
                                                    Text(nombreMostrar.take(1).uppercase(), fontSize = 48.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF6D00))
                                                }
                                            }

                                            SmallFloatingActionButton(
                                                onClick = { mostrarMenuFoto = true },
                                                containerColor = Color.White,
                                                contentColor = Color(0xFF00897B),
                                                shape = CircleShape,
                                                modifier = Modifier.size(36.dp).offset(x = 8.dp, y = 8.dp)
                                            ) {
                                                Icon(Icons.Rounded.PhotoCamera, null, modifier = Modifier.size(18.dp))
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))
                                        val nombreMostrar = miPerfilSocial?.username?.takeIf { it.isNotBlank() } ?: nombreUsuarioReal.takeIf { it.isNotBlank() } ?: "investigador"
                                        Text("@$nombreMostrar", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                                        Text(usuarioAuth?.email ?: "", color = Color(0xFFE0F2F1), fontSize = 14.sp)

                                        Spacer(modifier = Modifier.height(12.dp))
                                        val descText = miPerfilSocial?.descripcion
                                        if (descText.isNullOrBlank()) {
                                            TextButton(onClick = { inputDescripcion = ""; mostrarDialogoDescripcion = true }) {
                                                Text(textos.anadirDescripcion, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                                            }
                                        } else {
                                            Surface(
                                                color = Color.White.copy(alpha = 0.95f),
                                                shape = RoundedCornerShape(16.dp),
                                                shadowElevation = 4.dp,
                                                modifier = Modifier
                                                    .padding(horizontal = 24.dp)
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .clickable { inputDescripcion = descText; mostrarDialogoDescripcion = true }
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    modifier = Modifier.padding(16.dp)
                                                ) {
                                                    Text(
                                                        text = descText,
                                                        color = Color(0xFF00897B),
                                                        fontSize = 14.sp,
                                                        textAlign = TextAlign.Center,
                                                        fontWeight = FontWeight.Medium,
                                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                                    )
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Icon(
                                                        imageVector = Icons.Rounded.Edit,
                                                        contentDescription = "Editar descripción",
                                                        tint = Color.Gray.copy(alpha = 0.7f),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(24.dp))

                                        Row(horizontalArrangement = Arrangement.spacedBy(40.dp)) {
                                            ColumnaStats(miPerfilSocial?.seguidores?.size?.toString() ?: "0", textos.seguidores, blanco = true) {
                                                scope.launch {
                                                    tituloListaUsuarios = textos.seguidores
                                                    usuariosAMostrar = miPerfilSocial?.seguidores?.mapNotNull { GestorAuth.obtenerPerfilSocial(it) } ?: emptyList()
                                                    seccionActual = SeccionPerfil.LISTA_USUARIOS
                                                }
                                            }
                                            ColumnaStats(miPerfilSocial?.seguidos?.size?.toString() ?: "0", textos.siguiendo, blanco = true) {
                                                scope.launch {
                                                    tituloListaUsuarios = textos.siguiendo
                                                    usuariosAMostrar = miPerfilSocial?.seguidos?.mapNotNull { GestorAuth.obtenerPerfilSocial(it) } ?: emptyList()
                                                    seccionActual = SeccionPerfil.LISTA_USUARIOS
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(2.dp),
                                    shape = RoundedCornerShape(24.dp)
                                ) {
                                    Column(modifier = Modifier.padding(20.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { misCreacionesExpandido = !misCreacionesExpandido }.padding(vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Rounded.LibraryBooks, null, tint = Color(0xFFFF6D00))
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(textos.misCreaciones, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
                                            Icon(imageVector = if (misCreacionesExpandido) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown, contentDescription = null, tint = Color.Gray)
                                        }

                                        AnimatedVisibility(visible = misCreacionesExpandido) {
                                            Column(modifier = Modifier.fillMaxWidth()) {
                                                Spacer(modifier = Modifier.height(16.dp))

                                                if (GestorDatos.coleccionesGlobales.isEmpty()) {
                                                    Text(textos.sinListas, color = Color.Gray, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                                                } else {
                                                    OutlinedTextField(
                                                        value = textoBusquedaCreaciones,
                                                        onValueChange = { textoBusquedaCreaciones = it },
                                                        placeholder = { Text(textos.buscarListas) },
                                                        leadingIcon = { Icon(Icons.Rounded.Search, null, tint = Color.Gray) },
                                                        trailingIcon = { if (textoBusquedaCreaciones.isNotEmpty()) { IconButton(onClick = { textoBusquedaCreaciones = "" }) { Icon(Icons.Rounded.Clear, null) } } },
                                                        modifier = Modifier.fillMaxWidth(),
                                                        shape = RoundedCornerShape(50),
                                                        singleLine = true,
                                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFFF6D00), unfocusedBorderColor = Color.LightGray)
                                                    )

                                                    Spacer(modifier = Modifier.height(12.dp))

                                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        val opcionesInternas = listOf("Nuevas", "Populares", "Mayor a menor", "Menor a mayor")
                                                        val opcionesUi = listOf(textos.filtroNuevas, textos.filtroPopulares, textos.filtroMayorMenor, textos.filtroMenorMayor)

                                                        items(opcionesInternas.size) { index ->
                                                            val opcionInt = opcionesInternas[index]
                                                            val opcionUi = opcionesUi[index]

                                                            FilterChip(
                                                                selected = filtroOrdenCreaciones == opcionInt,
                                                                onClick = { filtroOrdenCreaciones = opcionInt },
                                                                label = { Text(opcionUi, fontSize = 12.sp) },
                                                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFFF6D00), selectedLabelColor = Color.White),
                                                                border = FilterChipDefaults.filterChipBorder(enabled = true, selected = filtroOrdenCreaciones == opcionInt, borderColor = if (filtroOrdenCreaciones == opcionInt) Color.Transparent else Color.LightGray)
                                                            )
                                                        }
                                                    }

                                                    Spacer(modifier = Modifier.height(16.dp))

                                                    val misListasFiltradas = GestorDatos.coleccionesGlobales.filter { col ->
                                                        !col.esDescargada && (col.nombre.contains(textoBusquedaCreaciones, ignoreCase = true) || col.categoria.contains(textoBusquedaCreaciones, ignoreCase = true))
                                                    }

                                                    val misListasOrdenadas = when (filtroOrdenCreaciones) {
                                                        "Populares" -> misListasFiltradas.sortedByDescending { it.likes }
                                                        "Mayor a menor" -> misListasFiltradas.sortedByDescending { col -> col.elementos.sumOf { if (it is ElementoGuardado.Individual) 1 else (it as ElementoGuardado.Conjunto).palabras.size } }
                                                        "Menor a mayor" -> misListasFiltradas.sortedBy { col -> col.elementos.sumOf { if (it is ElementoGuardado.Individual) 1 else (it as ElementoGuardado.Conjunto).palabras.size } }
                                                        else -> misListasFiltradas
                                                    }

                                                    if (misListasOrdenadas.isEmpty()) {
                                                        Text(textos.sinResultados, color = Color.Gray, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                                                    } else {
                                                        misListasOrdenadas.forEach { col ->
                                                            TarjetaColeccionPerfil(
                                                                coleccion = col,
                                                                tienePendientes = invitacionesEnviadas.any { it.nombreLista == col.nombre },
                                                                textos = textos,
                                                                onDelete = { coleccionParaBorrar = col },
                                                                onEdit = { onEditar(col) },
                                                                onPlay = { onJugar(col) },
                                                                onShare = {
                                                                    coleccionACompartir = col
                                                                    mostrarDialogoCompartir = true
                                                                },
                                                                onManageColabs = {
                                                                    coleccionGestionando = col
                                                                    mostrarDialogoGestionColaboradores = true
                                                                    cargandoColaboradores = true
                                                                    textoBusquedaColabs = ""
                                                                    scope.launch {
                                                                        perfilCreador = col.idCreador?.let { GestorAuth.obtenerPerfilSocial(it) }
                                                                        perfilesColaboradores = col.colaboradores.mapNotNull { GestorAuth.obtenerPerfilSocial(it) }

                                                                        val pendientesUid = invitacionesEnviadas.filter { it.nombreLista == col.nombre }.map { it.uidDestino }
                                                                        perfilesPendientes = pendientesUid.mapNotNull { GestorAuth.obtenerPerfilSocial(it) }
                                                                        cargandoColaboradores = false
                                                                    }
                                                                }
                                                            )
                                                            Spacer(Modifier.height(12.dp))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(32.dp))
                                }
                            }
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (usuariosAMostrar.isEmpty()) {
                                item { Text(textos.sinUsuariosLista, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 40.dp)) }
                            } else {
                                items(usuariosAMostrar) { perfil ->
                                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(48.dp).background(Color(0xFFE0F2F1), CircleShape), contentAlignment = Alignment.Center) {
                                                if (perfil.fotoPerfil != null && perfil.fotoPerfil.isNotEmpty()) {
                                                    if (perfil.fotoPerfil.startsWith("http")) {
                                                        KamelImage(asyncPainterResource(perfil.fotoPerfil), null, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                                                    } else {
                                                        val bitmap = decodificarBase64Imagen(perfil.fotoPerfil)
                                                        if (bitmap != null) {
                                                            ComposeImage(bitmap = bitmap, contentDescription = null, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                                                        } else {
                                                            Text(perfil.username.take(1).uppercase(), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00897B))
                                                        }
                                                    }
                                                } else {
                                                    Text(perfil.username.take(1).uppercase(), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00897B))
                                                }
                                            }
                                            Spacer(Modifier.width(16.dp))
                                            Text("@${perfil.username}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                else -> { /* CONFIGURANDO */ }
            }
        }
    }

    if (mostrarDialogoCompartir && coleccionACompartir != null) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoCompartir = false; textoBusquedaAmigo = "" },
            title = { Text(textos.tituloInvitar) },
            text = {
                Column {
                    Text("${textos.descInvitar1}${coleccionACompartir?.nombre}${textos.descInvitar2}", fontSize = 14.sp, color = Color.Gray)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = textoBusquedaAmigo,
                        onValueChange = { textoBusquedaAmigo = it },
                        placeholder = { Text(textos.buscarInvestigador) },
                        leadingIcon = { Icon(Icons.Rounded.PersonSearch, null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    Spacer(Modifier.height(12.dp))

                    if (buscandoAmigos) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally).size(24.dp), color = Color(0xFFFF6D00))
                    } else if (textoBusquedaAmigo.length >= 2 && resultadosAmigos.isEmpty()) {
                        Text(textos.sinInvestigadores, fontSize = 12.sp, color = Color.Red)
                    }

                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                        items(resultadosAmigos) { perfil ->
                            ListItem(
                                modifier = Modifier.clickable {
                                    mostrarDialogoCompartir = false
                                    onVerPerfilAjeno(perfil.uid)
                                },
                                headlineContent = { Text("@${perfil.username}", fontWeight = FontWeight.Bold) },
                                leadingContent = {
                                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFE0F2F1)), contentAlignment = Alignment.Center) {
                                        if (!perfil.fotoPerfil.isNullOrEmpty()) {
                                            if (perfil.fotoPerfil.startsWith("http")) {
                                                KamelImage(asyncPainterResource(perfil.fotoPerfil), null, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                                            } else {
                                                val bitmap = decodificarBase64Imagen(perfil.fotoPerfil)
                                                if (bitmap != null) {
                                                    ComposeImage(bitmap = bitmap, contentDescription = null, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                                                } else {
                                                    Text(perfil.username.take(1).uppercase(), color = Color(0xFF00897B), fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        } else {
                                            Text(perfil.username.take(1).uppercase(), color = Color(0xFF00897B), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                },
                                trailingContent = {
                                    TextButton(onClick = {
                                        scope.launch {
                                            val exito = GestorAuth.enviarInvitacionColaboracion(
                                                uidEmisor = usuarioAuth!!.uid,
                                                nombreEmisor = miPerfilSocial?.username ?: "investigador",
                                                uidDestino = perfil.uid,
                                                nombreLista = coleccionACompartir!!.nombre
                                            )
                                            if (exito) {
                                                val nuevaInvitacion = InvitacionColaboracion(
                                                    id = "${usuarioAuth!!.uid}_${perfil.uid}_${coleccionACompartir!!.nombre.replace(" ", "_")}",
                                                    uidEmisor = usuarioAuth!!.uid,
                                                    nombreEmisor = miPerfilSocial?.username ?: "",
                                                    uidDestino = perfil.uid,
                                                    nombreLista = coleccionACompartir!!.nombre
                                                )
                                                invitacionesEnviadas = invitacionesEnviadas + nuevaInvitacion
                                                resultadosAmigos = resultadosAmigos.filter { it.uid != perfil.uid }
                                                snackbarHostState.showSnackbar("${textos.msgInvitacionEnviada} @${perfil.username}")
                                            } else {
                                                snackbarHostState.showSnackbar(textos.msgErrorInvitacion)
                                            }
                                        }
                                    }) { Text(textos.btnInvitar) }
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { mostrarDialogoCompartir = false; textoBusquedaAmigo = "" }) { Text(textos.btnCerrar) } }
        )
    }

    if (mostrarDialogoGestionColaboradores && coleccionGestionando != null) {
        val esCreadorOriginal = !coleccionGestionando!!.esColaboracion
        val miUid = usuarioAuth?.uid

        AlertDialog(
            onDismissRequest = { mostrarDialogoGestionColaboradores = false },
            title = { Text(textos.tituloPersonas) },
            text = {
                Column {
                    OutlinedTextField(
                        value = textoBusquedaColabs,
                        onValueChange = { textoBusquedaColabs = it },
                        placeholder = { Text(textos.buscarUsuarios) },
                        leadingIcon = { Icon(Icons.Rounded.Search, null, tint = Color.Gray) },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        shape = RoundedCornerShape(50),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFF6D00),
                            unfocusedBorderColor = Color.LightGray
                        )
                    )

                    Text("${textos.descPersonas1}${coleccionGestionando?.nombre}${textos.descPersonas2}", fontSize = 14.sp, color = Color.Gray)
                    Spacer(Modifier.height(16.dp))

                    if (cargandoColaboradores) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally).size(24.dp), color = Color(0xFFFF6D00))
                    } else {
                        val creadorFiltrado = perfilCreador?.takeIf { it.uid != miUid && it.username.contains(textoBusquedaColabs, ignoreCase = true) }
                        val colabsFiltrados = perfilesColaboradores.filter { it.uid != miUid && it.username.contains(textoBusquedaColabs, ignoreCase = true) }
                        val pendFiltrados = perfilesPendientes.filter { it.uid != miUid && it.username.contains(textoBusquedaColabs, ignoreCase = true) }

                        if (creadorFiltrado == null && colabsFiltrados.isEmpty() && pendFiltrados.isEmpty()) {
                            Text(textos.sinOtrosUsuarios, fontSize = 14.sp, color = Color.Gray)
                        } else {
                            LazyColumn(modifier = Modifier.heightIn(max = 250.dp)) {

                                creadorFiltrado?.let { perfil ->
                                    item {
                                        ListItem(
                                            modifier = Modifier.clickable {
                                                mostrarDialogoGestionColaboradores = false
                                                onVerPerfilAjeno(perfil.uid)
                                            },
                                            headlineContent = { Text("@${perfil.username}", fontWeight = FontWeight.Bold, color = Color(0xFF1A73E8)) },
                                            supportingContent = { Text(textos.rolCreador, color = Color(0xFFD4AF37), fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                            leadingContent = {
                                                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFFFF8E1)), contentAlignment = Alignment.Center) {
                                                    if (!perfil.fotoPerfil.isNullOrEmpty()) {
                                                        if (perfil.fotoPerfil.startsWith("http")) {
                                                            KamelImage(asyncPainterResource(perfil.fotoPerfil), null, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                                                        } else {
                                                            val bitmap = decodificarBase64Imagen(perfil.fotoPerfil)
                                                            if (bitmap != null) {
                                                                ComposeImage(bitmap = bitmap, contentDescription = null, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                                                            } else {
                                                                Text(perfil.username.take(1).uppercase(), color = Color(0xFFD4AF37), fontWeight = FontWeight.Bold)
                                                            }
                                                        }
                                                    } else {
                                                        Text(perfil.username.take(1).uppercase(), color = Color(0xFFD4AF37), fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            },
                                            trailingContent = { Icon(Icons.Rounded.Star, contentDescription = null, tint = Color(0xFFD4AF37)) }
                                        )
                                    }
                                }

                                items(colabsFiltrados) { perfil ->
                                    ListItem(
                                        modifier = Modifier.clickable {
                                            mostrarDialogoGestionColaboradores = false
                                            onVerPerfilAjeno(perfil.uid)
                                        },
                                        headlineContent = { Text("@${perfil.username}", fontWeight = FontWeight.Bold, color = Color(0xFF1A73E8)) },
                                        supportingContent = { Text(textos.rolColaborador, color = Color(0xFF18C1A8), fontSize = 12.sp) },
                                        leadingContent = {
                                            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFE0F2F1)), contentAlignment = Alignment.Center) {
                                                if (!perfil.fotoPerfil.isNullOrEmpty()) {
                                                    if (perfil.fotoPerfil.startsWith("http")) {
                                                        KamelImage(asyncPainterResource(perfil.fotoPerfil), null, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                                                    } else {
                                                        val bitmap = decodificarBase64Imagen(perfil.fotoPerfil)
                                                        if (bitmap != null) {
                                                            ComposeImage(bitmap = bitmap, contentDescription = null, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                                                        } else {
                                                            Text(perfil.username.take(1).uppercase(), color = Color(0xFF00897B), fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                } else {
                                                    Text(perfil.username.take(1).uppercase(), color = Color(0xFF00897B), fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        },
                                        trailingContent = {
                                            if (esCreadorOriginal) {
                                                IconButton(onClick = {
                                                    scope.launch {
                                                        usuarioAuth?.uid?.let { miUid ->
                                                            GestorDatos.eliminarColaborador(miUid, coleccionGestionando!!, perfil.uid)
                                                            perfilesColaboradores = perfilesColaboradores.filter { it.uid != perfil.uid }
                                                            coleccionGestionando = coleccionGestionando!!.copy(
                                                                colaboradores = coleccionGestionando!!.colaboradores.filter { it != perfil.uid }
                                                            )
                                                            if (perfilesColaboradores.isEmpty() && perfilesPendientes.isEmpty()) mostrarDialogoGestionColaboradores = false
                                                            snackbarHostState.showSnackbar("${textos.msgExpulsado} @${perfil.username}")
                                                        }
                                                    }
                                                }) {
                                                    Icon(Icons.Rounded.PersonRemove, contentDescription = null, tint = Color.Red)
                                                }
                                            }
                                        }
                                    )
                                }

                                items(pendFiltrados) { perfil ->
                                    ListItem(
                                        modifier = Modifier.clickable {
                                            mostrarDialogoGestionColaboradores = false
                                            onVerPerfilAjeno(perfil.uid)
                                        },
                                        headlineContent = { Text("@${perfil.username}", fontWeight = FontWeight.Bold, color = Color(0xFF1A73E8).copy(alpha=0.6f)) },
                                        supportingContent = { Text(textos.rolEspera, color = Color.Gray, fontSize = 12.sp) },
                                        leadingContent = {
                                            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFE0F2F1).copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                                                if (!perfil.fotoPerfil.isNullOrEmpty()) {
                                                    if (perfil.fotoPerfil.startsWith("http")) {
                                                        KamelImage(asyncPainterResource(perfil.fotoPerfil), null, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop, alpha = 0.5f)
                                                    } else {
                                                        val bitmap = decodificarBase64Imagen(perfil.fotoPerfil)
                                                        if (bitmap != null) {
                                                            ComposeImage(bitmap = bitmap, contentDescription = null, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop, alpha = 0.5f)
                                                        } else {
                                                            Text(perfil.username.take(1).uppercase(), color = Color(0xFF00897B).copy(alpha=0.5f), fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                } else {
                                                    Text(perfil.username.take(1).uppercase(), color = Color(0xFF00897B).copy(alpha=0.5f), fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        },
                                        trailingContent = {
                                            if (esCreadorOriginal) {
                                                IconButton(onClick = {
                                                    scope.launch {
                                                        val inv = invitacionesEnviadas.find { it.nombreLista == coleccionGestionando!!.nombre && it.uidDestino == perfil.uid }
                                                        if (inv != null) {
                                                            GestorAuth.cancelarInvitacion(inv.id)
                                                            invitacionesEnviadas = invitacionesEnviadas.filter { it.id != inv.id }
                                                            perfilesPendientes = perfilesPendientes.filter { it.uid != perfil.uid }
                                                            if (perfilesColaboradores.isEmpty() && perfilesPendientes.isEmpty()) mostrarDialogoGestionColaboradores = false
                                                        }
                                                    }
                                                }) { Icon(Icons.Rounded.Cancel, tint = Color.Gray, contentDescription = null) }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { mostrarDialogoGestionColaboradores = false }) { Text(textos.btnCerrar) } }
        )
    }

    if (mostrarMenuFoto) {
        ModalBottomSheet(
            onDismissRequest = { mostrarMenuFoto = false },
            sheetState = sheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
                Text(
                    textos.tituloFoto,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )
                HorizontalDivider(color = Color(0xFFF0F0F0))

                ListItem(
                    headlineContent = { Text(textos.opcionGaleria) },
                    supportingContent = { Text(textos.descGaleria) },
                    leadingContent = { Icon(Icons.Rounded.PhotoLibrary, null, tint = Color(0xFF00897B)) },
                    modifier = Modifier.clickable { mostrarMenuFoto = false; imagePicker.launch() }
                )
                ListItem(
                    headlineContent = { Text(textos.opcionInternet) },
                    supportingContent = { Text(textos.descInternet) },
                    leadingContent = { Icon(Icons.Rounded.TravelExplore, null, tint = Color(0xFFFF6D00)) },
                    modifier = Modifier.clickable { mostrarMenuFoto = false; mostrarBuscadorAvatar = true }
                )
            }
        }
    }

    if (mostrarBuscadorAvatar && miPerfilSocial != null) {
        Dialog(
            onDismissRequest = { mostrarBuscadorAvatar = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize().padding(top = 32.dp),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = Color.White
            ) {
                val urlBusqueda = "https://www.google.com/search?tbm=isch&q=avatar+profile+picture+cool"
                val webViewState = rememberWebViewState(urlBusqueda)
                val navigator = rememberWebViewNavigator()

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().background(Color(0xFFF0F5F5)).padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { mostrarBuscadorAvatar = false }) { Text(textos.btnCerrar, color = Color.Gray) }
                        Text(textos.tituloAvatar, fontWeight = FontWeight.Bold)
                        Button(
                            onClick = {
                                scope.launch {
                                    navigator.evaluateJavaScript("(function(){var imgs=document.querySelectorAll('img');var maxA=0;var src='';imgs.forEach(function(i){var r=i.getBoundingClientRect();var a=r.width*r.height;if(a>maxA && i.src.startsWith('http')){maxA=a;src=i.src;}});return src;})();") { res ->
                                        val url = res?.trim('"', '\'')
                                        if (!url.isNullOrEmpty() && url != "null") {
                                            scope.launch {
                                                if (GestorAuth.actualizarFotoPerfil(usuarioAuth!!.uid, url)) {
                                                    miPerfilSocial = miPerfilSocial?.copy(fotoPerfil = url)
                                                    mostrarBuscadorAvatar = false
                                                    snackbarHostState.showSnackbar(textos.msgFotoExito)
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6D00))
                        ) { Text(textos.btnGuardar, fontWeight = FontWeight.Bold) }
                    }
                    WebView(state = webViewState, navigator = navigator, modifier = Modifier.fillMaxSize())
                }
            }
        }
    }

    if (coleccionParaBorrar != null) {
        val esColaboracion = coleccionParaBorrar!!.esColaboracion
        AlertDialog(
            onDismissRequest = { coleccionParaBorrar = null },
            title = { Text(if (esColaboracion) textos.tituloAbandonar else textos.tituloEliminarCol) },
            text = {
                if (esColaboracion) {
                    Text("${textos.descAbandonar1}${coleccionParaBorrar?.nombre}${textos.descAbandonar2}")
                } else {
                    Text(textos.descEliminarCol)
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
                                    try { GestorAuth.firestore.collection("usuarios").document(uid).collection("colecciones").document(nombreCol).delete() } catch (e: Exception) { }
                                }
                            }
                        }
                    }
                    coleccionParaBorrar = null
                }) { Text(if (esColaboracion) textos.btnAbandonar else textos.btnEliminar, color = Color.Red, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { coleccionParaBorrar = null }) { Text(textos.btnCancelar, color = Color.Gray) } }
        )
    }

    if (mostrarDialogoCerrar) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoCerrar = false },
            title = { Text(textos.tituloCerrar) },
            text = { Text(textos.descCerrar) },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            GestorAuth.cerrarSesion(); GestorDatos.limpiarDatosLocales(); clicEnIniciarSesion = false; mostrarDialogoCerrar = false
                            onVolver()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                ) { Text(textos.btnCerrarSesion) }
            },
            dismissButton = { TextButton(onClick = { mostrarDialogoCerrar = false }) { Text(textos.btnCancelar, color = Color.Gray) } }
        )
    }

    if (mostrarDialogoBorrar) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoBorrar = false },
            title = { Text(textos.tituloEliminar, color = Color.Red) },
            text = { Text(textos.descEliminar) },
            confirmButton = {
                Button(
                    onClick = {
                        val uidFijo = usuarioAuth?.uid;
                        if (uidFijo != null) {
                            scope.launch {
                                GestorAuth.eliminarCuentaDefinitiva(uidFijo)
                                GestorDatos.limpiarDatosLocales()
                                clicEnIniciarSesion = false
                                mostrarDialogoBorrar = false
                                onVolver()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text(textos.btnBorrarTodo, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { mostrarDialogoBorrar = false }) { Text(textos.btnCancelar, color = Color.Gray) } }
        )
    }

    if (mostrarDialogoNombre) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text(textos.tituloNombre) },
            text = {
                OutlinedTextField(
                    value = nombreUsuarioInput,
                    onValueChange = { nombreUsuarioInput = it.lowercase().replace(" ", "") },
                    singleLine = true,
                    isError = errorNombre.isNotEmpty(),
                    supportingText = { if (errorNombre.isNotEmpty()) Text(errorNombre) },
                    shape = RoundedCornerShape(12.dp),
                    label = { Text(textos.labelApodo) })
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        if (nombreUsuarioInput.length < 3) { errorNombre = textos.errorMinLetras; return@launch }
                        cargandoAuth = true
                        if (GestorAuth.registrarNombreUsuario(nombreUsuarioInput)) {
                            nombreUsuarioReal = nombreUsuarioInput
                            mostrarDialogoNombre = false
                            if (listasLocalesTemp.isNotEmpty()) {
                                mostrarDialogoMigracion = true
                            } else {
                                GestorDatos.descargarDatosNube(usuarioAuth!!.uid)
                                snackbarHostState.showSnackbar("${textos.msgBienvenido} @$nombreUsuarioReal!")
                                estadoPantalla = "PERFIL"
                                clicEnIniciarSesion = false
                            }
                        } else {
                            errorNombre = textos.errorNombreUso
                        }
                        cargandoAuth = false
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF18C1A8))) { Text(textos.btnConfirmar) }
            }
        )
    }

    if (mostrarDialogoMigracion) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text(textos.tituloMigracion) },
            text = {
                Column {
                    Text(textos.descMigracion, color = Color.Gray, fontSize = 14.sp)
                    Spacer(Modifier.height(16.dp))
                    LazyColumn(Modifier.heightIn(max = 200.dp)) {
                        items(listasLocalesTemp) { coleccion ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = listasSeleccionadas.contains(coleccion),
                                    onCheckedChange = { listasSeleccionadas = if (it) listasSeleccionadas + coleccion else listasSeleccionadas - coleccion },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFFFF6D00))
                                )
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
                        listasSeleccionadas.forEach { GestorDatos.subirColeccionNube(usuarioAuth!!.uid, it) }
                        GestorDatos.subirJugadoresNube(usuarioAuth!!.uid)
                        GestorDatos.subirCheckpointsNube(usuarioAuth!!.uid)
                        GestorDatos.descargarDatosNube(usuarioAuth!!.uid)
                        mostrarDialogoMigracion = false
                        snackbarHostState.showSnackbar(textos.msgSincronizado)
                        estadoPantalla = "PERFIL"
                        clicEnIniciarSesion = false
                        cargandoAuth = false
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF18C1A8))) { Text(textos.btnGuardar) }
            },
            dismissButton = {
                TextButton(onClick = {
                    scope.launch {
                        GestorDatos.descargarDatosNube(usuarioAuth!!.uid)
                        mostrarDialogoMigracion = false
                        estadoPantalla = "PERFIL"
                        clicEnIniciarSesion = false
                    }
                }) { Text(textos.btnDescartar, color = Color.Gray) }
            }
        )
    }

    if (mostrarDialogoDescripcion) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoDescripcion = false },
            title = { Text(textos.tituloSobreMi) },
            text = {
                OutlinedTextField(
                    value = inputDescripcion,
                    onValueChange = { if (it.length <= 300) inputDescripcion = it },
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                    placeholder = { Text(textos.placeholderSobreMi) },
                    supportingText = { Text("${inputDescripcion.length}/300", textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth()) }
                )
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        usuarioAuth?.uid?.let { uid ->
                            if (GestorAuth.actualizarDescripcionPerfil(uid, inputDescripcion)) {
                                miPerfilSocial = miPerfilSocial?.copy(descripcion = inputDescripcion)
                                mostrarDialogoDescripcion = false
                                snackbarHostState.showSnackbar(textos.msgDescExito)
                            }
                        }
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF18C1A8))) {
                    Text(textos.btnGuardar)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoDescripcion = false }) { Text(textos.btnCancelar, color = Color.Gray) }
            }
        )
    }
}

@Composable
fun ColumnaStats(valor: String, etiqueta: String, blanco: Boolean = false, onClick: () -> Unit) {
    val colorTexto = if (blanco) Color.White else Color(0xFF1A1A1A)
    val colorEtiqueta = if (blanco) Color.White.copy(alpha = 0.8f) else Color.Gray

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }) {
        Text(valor, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = colorTexto)
        Text(etiqueta, fontSize = 13.sp, color = colorEtiqueta)
    }
}

@Composable
fun TarjetaColeccionPerfil(
    coleccion: ColeccionGuardada,
    tienePendientes: Boolean,
    textos: TextosPerfil,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onPlay: () -> Unit,
    onShare: () -> Unit,
    onManageColabs: () -> Unit
) {
    val totalPalabras = coleccion.elementos.sumOf { if (it is ElementoGuardado.Individual) 1 else (it as ElementoGuardado.Conjunto).palabras.size }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF5F5F5),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(coleccion.nombre, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val iconVector = if (coleccion.esColaboracion) Icons.Rounded.Group else if (coleccion.esPublica) Icons.Rounded.Public else Icons.Rounded.Lock
                        val iconColor = if (coleccion.esColaboracion) Color(0xFF18C1A8) else if (coleccion.esPublica) Color(0xFF18C1A8) else Color.Gray

                        Icon(iconVector, null, modifier = Modifier.size(12.dp), tint = iconColor)
                        Spacer(Modifier.width(4.dp))
                        Text(if (coleccion.esColaboracion) textos.etiquetaColaborativa else if (coleccion.esPublica) textos.etiquetaPublica else textos.etiquetaPrivada, fontSize = 10.sp, color = Color.Gray)

                        Spacer(Modifier.width(8.dp))
                        Text("• $totalPalabras ${textos.pal}", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    }
                }
                if (coleccion.esPublica || coleccion.esColaboracion) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Favorite, null, tint = Color(0xFFFF3D00), modifier = Modifier.size(14.dp))
                        Text(" ${coleccion.likes}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {

                if (coleccion.colaboradores.isNotEmpty() || tienePendientes) {
                    IconButton(onClick = onManageColabs) {
                        Icon(Icons.Rounded.ManageAccounts, null, tint = Color(0xFF00897B), modifier = Modifier.size(20.dp))
                    }
                }

                if (!coleccion.esColaboracion) {
                    IconButton(onClick = onShare) {
                        Icon(Icons.Rounded.PersonAdd, null, tint = Color(0xFFFF6D00), modifier = Modifier.size(20.dp))
                    }
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = if (coleccion.esColaboracion) Icons.Rounded.ExitToApp else Icons.Rounded.Delete,
                        contentDescription = null,
                        tint = Color.Red.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(onClick = onEdit) {
                    Icon(Icons.Rounded.Edit, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onPlay) {
                    Icon(Icons.Rounded.PlayArrow, null, tint = Color(0xFF18C1A8), modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

expect fun decodificarBase64Imagen(base64: String): ImageBitmap?