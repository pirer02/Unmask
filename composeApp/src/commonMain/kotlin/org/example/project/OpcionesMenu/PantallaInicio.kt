import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.alpha
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
import androidx.compose.ui.text.style.TextOverflow
import org.example.project.Datos.ListasPredeterminadas.DatosPredeterminados
import org.example.project.Datos.TextosTraducidos.TextosInicio
import org.example.project.Datos.TextosTraducidos.obtenerTextosInicio
import org.jetbrains.compose.resources.painterResource
import unmask.composeapp.generated.resources.Res
import unmask.composeapp.generated.resources.flag_de
import unmask.composeapp.generated.resources.flag_en
import unmask.composeapp.generated.resources.flag_es
import unmask.composeapp.generated.resources.flag_fr
import unmask.composeapp.generated.resources.flag_it
import unmask.composeapp.generated.resources.flag_ja
import unmask.composeapp.generated.resources.flag_zh

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PantallaInicio(
    onJugar: (ColeccionGuardada) -> Unit,
    onJugarCheckpoint: (ColeccionGuardada, CheckpointJuego) -> Unit = { _, _ -> },
    onGestionarJugadores: () -> Unit,
    onCrearLista: () -> Unit,
    onIrAPerfil: () -> Unit,
    onVerPerfilAjeno: (String) -> Unit
) {
    val usuarioAuth by GestorAuth.usuario.collectAsState()
    var urlFotoPerfil by remember { mutableStateOf<String?>(null) }
    var checkpointParaBorrar by remember { mutableStateOf<CheckpointJuego?>(null) }

    // --- LÓGICA DE IDIOMAS CON TEXTOS DINÁMICOS ---
    val idiomaActual by GestorIdiomas.idiomaActual.collectAsState()
    val textos = obtenerTextosInicio(idiomaActual)
    var mostrarMenuIdiomas by remember { mutableStateOf(false) }
    // ----------------------------------------------

    val pasoTutorial = GestorDatos.pasoTutorialActual
    val esTutorialActivo = pasoTutorial == 1

    LaunchedEffect(usuarioAuth) {
        if (usuarioAuth != null) {
            val perfil = GestorAuth.obtenerPerfilSocial(usuarioAuth!!.uid)
            urlFotoPerfil = perfil?.fotoPerfil
        } else {
            urlFotoPerfil = null
        }
    }

    val misCheckpoints = GestorDatos.checkpointsGlobales.sortedByDescending { it.fecha }
    val misCreaciones = GestorDatos.coleccionesGlobales.filter { !it.esDescargada && !it.esColaboracion }
    val recientes = misCreaciones.takeLast(3).reversed()
    val misDescargas = GestorDatos.coleccionesGlobales.filter { it.esDescargada && !it.esColaboracion }
    val descargasRecientes = misDescargas.takeLast(3).reversed()
    val misColaboraciones = GestorDatos.coleccionesGlobales.filter { it.esColaboracion }
    val colaboracionesRecientes = misColaboraciones.takeLast(3).reversed()

    val predeterminadas = DatosPredeterminados.obtenerListasPredeterminadas(idiomaActual.codigo)

    var coleccionViendoInfo by remember { mutableStateOf<ColeccionGuardada?>(null) }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(textos.tituloJuega, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black)

            Box {
                IconButton(
                    onClick = { mostrarMenuIdiomas = true },
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFF5F5F5))
                ) {
                    Image(
                        painter = painterResource(obtenerDibujableBandera(idiomaActual.codigo)),
                        contentDescription = idiomaActual.nombre,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }

                DropdownMenu(
                    expanded = mostrarMenuIdiomas,
                    onDismissRequest = { mostrarMenuIdiomas = false },
                    modifier = Modifier.background(Color.White)
                ) {
                    IdiomaSoportado.entries.forEach { idioma ->
                        DropdownMenuItem(
                            text = { Text(idioma.nombre) },
                            onClick = {
                                GestorIdiomas.cambiarIdioma(idioma)
                                mostrarMenuIdiomas = false
                            },
                            leadingIcon = {
                                Image(
                                    painter = painterResource(obtenerDibujableBandera(idioma.codigo)),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp).clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        )
                    }
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .alpha(if (esTutorialActivo) 0.5f else 1f)
                .clickable(enabled = !esTutorialActivo) { onGestionarJugadores() },
            colors = CardDefaults.cardColors(containerColor = Color(0xFF18C1A8))
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Group, null, tint = Color.White, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(textos.tituloJugadores, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                    Text(textos.descJugadores, color = Color.White, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (misCheckpoints.isNotEmpty()) {
            SeccionTitulo(titulo = textos.tituloPausa, icono = Icons.Rounded.SaveAs)
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), modifier = Modifier.alpha(if(esTutorialActivo) 0.5f else 1f)) {
                items(misCheckpoints) { checkpoint ->
                    val coleccionAsociada = GestorDatos.coleccionesGlobales.find { it.nombre == checkpoint.nombreColeccion && it.idCreador == checkpoint.idCreadorColeccion }
                    if (coleccionAsociada != null) {
                        TarjetaCheckpoint(
                            checkpoint = checkpoint,
                            coleccion = coleccionAsociada,
                            textos = textos,
                            onJugarClick = { if(!esTutorialActivo) onJugarCheckpoint(coleccionAsociada, checkpoint) },
                            onBorrarClick = { if(!esTutorialActivo) checkpointParaBorrar = checkpoint }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        if (recientes.isNotEmpty()) {
            SeccionTitulo(titulo = textos.tituloRecientes, icono = Icons.Rounded.Stars)
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), modifier = Modifier.alpha(if(esTutorialActivo) 0.5f else 1f)) {
                items(recientes) { coleccion ->
                    TarjetaColeccionInicio(
                        coleccion = coleccion, esPredeterminada = false, textos = textos,
                        onJugarClick = { if(!esTutorialActivo) onJugar(coleccion) }, onInfoClick = { if(!esTutorialActivo) coleccionViendoInfo = coleccion }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        SeccionTitulo(titulo = textos.tituloPredeterminadas, icono = Icons.Rounded.Info)
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), modifier = Modifier.alpha(if(esTutorialActivo) 0.5f else 1f)) {
            items(predeterminadas) { predefinida ->
                val coleccionGuardada = GestorDatos.convertirPredefinidaAGuardada(predefinida)
                TarjetaColeccionInicio(
                    coleccion = coleccionGuardada, esPredeterminada = true, textos = textos,
                    onJugarClick = { if(!esTutorialActivo) onJugar(coleccionGuardada) },
                    onInfoClick = { if(!esTutorialActivo) coleccionViendoInfo = coleccionGuardada }
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        if (colaboracionesRecientes.isNotEmpty() || descargasRecientes.isNotEmpty()) {
            if (colaboracionesRecientes.isNotEmpty()) {
                SeccionTitulo(titulo = textos.tituloColaboraciones, icono = Icons.Rounded.Handshake)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), modifier = Modifier.alpha(if(esTutorialActivo) 0.5f else 1f)) {
                    items(colaboracionesRecientes) { coleccion ->
                        TarjetaColeccionInicio(coleccion = coleccion, esPredeterminada = false, textos = textos, onJugarClick = { if(!esTutorialActivo) onJugar(coleccion) }, onInfoClick = { if(!esTutorialActivo) coleccionViendoInfo = coleccion }, onAutorClick = { if(!esTutorialActivo) coleccion.idCreador?.let { uid -> onVerPerfilAjeno(uid) } })
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
            if (descargasRecientes.isNotEmpty()) {
                SeccionTitulo(titulo = textos.tituloDescargas, icono = Icons.Rounded.CloudDownload)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), modifier = Modifier.alpha(if(esTutorialActivo) 0.5f else 1f)) {
                    items(descargasRecientes) { coleccion ->
                        TarjetaColeccionInicio(coleccion = coleccion, esPredeterminada = false, textos = textos, onJugarClick = { if(!esTutorialActivo) onJugar(coleccion) }, onInfoClick = { if(!esTutorialActivo) coleccionViendoInfo = coleccion }, onAutorClick = { if(!esTutorialActivo) coleccion.idCreador?.let { uid -> onVerPerfilAjeno(uid) } })
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clickable { onCrearLista() },
            colors = CardDefaults.cardColors(containerColor = if (esTutorialActivo) Color(0xFFE0F2F1) else Color(0xFFFFF4E6)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(2.dp, if (esTutorialActivo) Color(0xFF18C1A8) else Color(0xFFFFD8C2))
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(if (esTutorialActivo) Icons.Rounded.AdsClick else Icons.Rounded.EditNote, contentDescription = null, tint = if (esTutorialActivo) Color(0xFF18C1A8) else Color(0xFFFF6D00), modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text(if (esTutorialActivo) textos.pulsaEmpezar else textos.creaLista, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1A1A1A))
                Text(
                    textos.descCreaLista,
                    textAlign = TextAlign.Center, fontSize = 14.sp, color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )
                Button(
                    onClick = onCrearLista,
                    colors = ButtonDefaults.buttonColors(containerColor = if (esTutorialActivo) Color(0xFF18C1A8) else Color(0xFFFF6D00)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (esTutorialActivo) textos.btnCrearLista else textos.btnCrearAhora, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(100.dp))
    }

    if (coleccionViendoInfo != null) DialogoInfoColeccion(coleccion = coleccionViendoInfo!!, textos = textos, onCerrar = { coleccionViendoInfo = null })
    if (checkpointParaBorrar != null) {
        AlertDialog(
            onDismissRequest = { checkpointParaBorrar = null },
            title = { Text(textos.dialogoBorrarTitulo) },
            text = { Text("${textos.dialogoBorrarDesc1} '${checkpointParaBorrar!!.nombre}'${textos.dialogoBorrarDesc2}") },
            confirmButton = { TextButton(onClick = { GestorDatos.checkpointsGlobales.removeAll { it.id == checkpointParaBorrar!!.id }; GestorDatos.guardarCambiosMemoria(); checkpointParaBorrar = null }) { Text(textos.btnBorrar, color = Color.Red, fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(onClick = { checkpointParaBorrar = null }) { Text(textos.btnCancelar, color = Color.Gray) } }
        )
    }
}

@Composable
fun SeccionTitulo(titulo: String, icono: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icono, null, tint = Color.Gray); Spacer(modifier = Modifier.width(8.dp))
        Text(titulo, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
    }
}

@Composable
fun TarjetaColeccionInicio(coleccion: ColeccionGuardada, esPredeterminada: Boolean, textos: TextosInicio, onJugarClick: () -> Unit, onInfoClick: () -> Unit, onAutorClick: (() -> Unit)? = null) {
    val totalPalabras = coleccion.elementos.sumOf { if (it is ElementoGuardado.Individual) 1 else (it as ElementoGuardado.Conjunto).palabras.size }
    var urlFoto by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(coleccion.idCreador) { if (!esPredeterminada && coleccion.idCreador != null) { val perfil = GestorAuth.obtenerPerfilSocial(coleccion.idCreador); urlFoto = perfil?.fotoPerfil } }

    val containerColor: Color
    val textColor: Color
    val categoryColor: Color
    val infoColor: Color
    val picBorderColor: Color
    val border: BorderStroke?

    when {
        esPredeterminada -> { containerColor = Color(0xFFE0F2F1); textColor = Color(0xFF121212); categoryColor = Color(0xFF00897B); infoColor = Color.Gray; picBorderColor = Color(0xFF18C1A8); border = BorderStroke(1.dp, Color(0xFFB2DFDB)) }
        coleccion.esColaboracion -> { containerColor = Color(0xFFFFF3E0); textColor = Color(0xFFE65100); categoryColor = Color(0xFFFF6D00); infoColor = Color(0xFFF57C00); picBorderColor = Color(0xFFFF6D00); border = BorderStroke(1.dp, Color(0xFFFFE0B2)) }
        coleccion.esDescargada -> { containerColor = Color(0xFFE3F2FD); textColor = Color(0xFF0D47A1); categoryColor = Color(0xFFFF6D00); infoColor = Color(0xFF1976D2); picBorderColor = Color(0xFF1976D2); border = BorderStroke(1.dp, Color(0xFF90CAF9)) }
        else -> { containerColor = Color(0xFF181818); textColor = Color.White; categoryColor = Color(0xFFFF6D00); infoColor = Color.LightGray; picBorderColor = Color(0xFFFF6D00); border = null }
    }

    Card(modifier = Modifier.width(160.dp).height(240.dp).clip(RoundedCornerShape(16.dp)).clickable { onJugarClick() }, colors = CardDefaults.cardColors(containerColor = containerColor), elevation = CardDefaults.cardElevation(defaultElevation = if (esPredeterminada || coleccion.esDescargada || coleccion.esColaboracion) 0.dp else 4.dp), border = border) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White).padding(2.dp).clip(CircleShape).background(containerColor), contentAlignment = Alignment.Center) {
                    if (esPredeterminada) Text("U", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = picBorderColor)
                    else if (!urlFoto.isNullOrEmpty()) {
                        if (urlFoto!!.startsWith("http")) KamelImage(asyncPainterResource(urlFoto!!), null, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                        else {
                            val bitmap = decodificarBase64Imagen(urlFoto!!)
                            if (bitmap != null) ComposeImage(bitmap = bitmap, contentDescription = null, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                            else Text(coleccion.nombreCreador?.take(1)?.uppercase() ?: "I", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = picBorderColor)
                        }
                    }
                    else Text(coleccion.nombreCreador?.take(1)?.uppercase() ?: "I", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = picBorderColor)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    val textoAutor = when { esPredeterminada -> textos.unmaskOficial; coleccion.esDescargada || coleccion.esColaboracion -> "${textos.por} @${coleccion.nombreCreador ?: "investigador"}"; else -> textos.porMi }
                    Text(text = textoAutor, fontSize = 11.sp, fontWeight = if (textoAutor == textos.porMi) FontWeight.Bold else FontWeight.Medium, color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.let { if ((coleccion.esDescargada || coleccion.esColaboracion) && onAutorClick != null && !esPredeterminada) it.clickable { onAutorClick.invoke() } else it })
                    Text("$totalPalabras ${textos.pal}", fontSize = 10.sp, color = infoColor, maxLines = 1)
                }
            }
            Spacer(modifier = Modifier.height(12.dp)); Text(coleccion.nombre, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = textColor, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(6.dp)); Surface(color = if (esPredeterminada) Color(0xFFB2DFDB) else if (coleccion.esColaboracion) Color(0xFFFFE0B2) else if (coleccion.esDescargada) Color(0xFFBBDEFB) else Color(0xFF2C2C2C), shape = RoundedCornerShape(8.dp)) { Text(coleccion.categoria.uppercase(), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = categoryColor, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            Spacer(modifier = Modifier.weight(1f)); HorizontalDivider(color = infoColor, thickness = 1.dp); Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onInfoClick, modifier = Modifier.size(28.dp)) { Icon(Icons.Rounded.Info, null, tint = infoColor, modifier = Modifier.size(18.dp)) }
                Button(onClick = onJugarClick, colors = ButtonDefaults.buttonColors(containerColor = if (esPredeterminada) Color(0xFF18C1A8) else Color(0xFFFF6D00)), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp), modifier = Modifier.height(28.dp)) { Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(12.dp)); Spacer(Modifier.width(2.dp)); Text(textos.btnJugar, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
fun DialogoInfoColeccion(coleccion: ColeccionGuardada, textos: TextosInicio, onCerrar: () -> Unit) {
    var textoBusqueda by remember { mutableStateOf("") }
    val todasLasPalabras = remember(coleccion) { coleccion.elementos.flatMap { when (it) { is ElementoGuardado.Individual -> listOf(it.palabra); is ElementoGuardado.Conjunto -> it.palabras.map { p -> p.palabra } } } }
    val filtradas = todasLasPalabras.filter { it.contains(textoBusqueda, ignoreCase = true) }

    Dialog(onDismissRequest = onCerrar) {
        Card(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(coleccion.nombre, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(coleccion.categoria, color = Color(0xFFFF6D00), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    IconButton(onClick = onCerrar) { Icon(Icons.Rounded.Close, null) }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(value = textoBusqueda, onValueChange = { textoBusqueda = it }, placeholder = { Text(textos.buscarPalabra) }, leadingIcon = { Icon(Icons.Rounded.Search, null, tint = Color.Gray) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(50), singleLine = true)
                Spacer(modifier = Modifier.height(16.dp))
                Text("${textos.contiene} ${todasLasPalabras.size} ${textos.palabras}", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filtradas) { p ->
                        Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFFF5F5F5), shape = RoundedCornerShape(8.dp)) {
                            Text(p, modifier = Modifier.padding(12.dp), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TarjetaCheckpoint(checkpoint: CheckpointJuego, coleccion: ColeccionGuardada, textos: TextosInicio, onJugarClick: () -> Unit, onBorrarClick: () -> Unit) {
    val formatter = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
    val fechaTexto = formatter.format(Date(checkpoint.fecha))
    var urlFoto by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(coleccion.idCreador) {
        if (coleccion.idCreador != null) {
            val perfil = GestorAuth.obtenerPerfilSocial(coleccion.idCreador)
            urlFoto = perfil?.fotoPerfil
        }
    }

    Card(modifier = Modifier.width(160.dp).height(240.dp).clip(RoundedCornerShape(16.dp)).clickable { onJugarClick() }, colors = CardDefaults.cardColors(containerColor = Color(0xFF4A148C)), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Column(modifier = Modifier.padding(12.dp).fillMaxSize()) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(text = checkpoint.nombre, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                IconButton(onClick = onBorrarClick, modifier = Modifier.size(24.dp)) { Icon(Icons.Rounded.DeleteOutline, null, tint = Color(0xFFFF8A80)) }
            }
            Text(fechaTexto, color = Color.LightGray, fontSize = 10.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(Color.White).padding(2.dp).clip(CircleShape).background(Color(0xFF4A148C)), contentAlignment = Alignment.Center) {
                    if (!urlFoto.isNullOrEmpty()) {
                        if (urlFoto!!.startsWith("http")) KamelImage(asyncPainterResource(urlFoto!!), null, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                        else {
                            val bitmap = decodificarBase64Imagen(urlFoto!!)
                            if (bitmap != null) ComposeImage(bitmap = bitmap, contentDescription = null, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                            else Text(coleccion.nombreCreador?.take(1)?.uppercase() ?: "I", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    } else Text(coleccion.nombreCreador?.take(1)?.uppercase() ?: "I", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = coleccion.nombreCreador?.let { "${textos.por} @$it" } ?: "${textos.por} ${textos.anonimo}", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text("${textos.lista} ${coleccion.nombre}", color = Color(0xFFCE93D8), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("${checkpoint.palabrasUsadas.size} ${textos.palUsadas}", color = Color(0xFFCE93D8), fontSize = 11.sp)
            Spacer(modifier = Modifier.weight(1f))
            Button(onClick = onJugarClick, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD50000)), modifier = Modifier.fillMaxWidth().height(32.dp), contentPadding = PaddingValues(0.dp)) {
                Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(textos.btnContinuar, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)
fun obtenerDibujableBandera(codigo: String): org.jetbrains.compose.resources.DrawableResource {
    return when (codigo) {
        "es" -> Res.drawable.flag_es
        "en" -> Res.drawable.flag_en
        "fr" -> Res.drawable.flag_fr
        "it" -> Res.drawable.flag_it
        "de" -> Res.drawable.flag_de
        "zh" -> Res.drawable.flag_zh
        "ja" -> Res.drawable.flag_ja
        else -> Res.drawable.flag_en
    }
}