import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.example.project.Datos.*
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign


// 👇 IMPORTS DEL WEBVIEW Y KAMEL PARA IMÁGENES 👇
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewState
import com.multiplatform.webview.web.rememberWebViewNavigator
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import org.example.project.Datos.TextosTraducidos.obtenerTextosCrear

// --- PEQUEÑA HERRAMIENTA PARA MAYÚSCULAS ---
fun String.capitalizarPrimeraCrear(): String {
    if (this.isBlank()) return this
    return this.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

// --- MODELOS DE ESTADO ---
class PalabraUI(palabraIni: String = "", pistaIni: String = "", imagenIni: String? = null, categoriaOrigenIni: String = "") {
    var palabra by mutableStateOf(palabraIni)
    var pista by mutableStateOf(pistaIni)
    var imagenUrl by mutableStateOf(imagenIni)
    var categoriaOrigen by mutableStateOf(categoriaOrigenIni) // 👇 NUEVO

    var errorPalabra by mutableStateOf(false)
    var mensajeErrorPalabra by mutableStateOf("")
    var errorPista by mutableStateOf(false)
    var mensajeErrorPista by mutableStateOf("")
}

sealed class ElementoUI {
    val id = java.util.UUID.randomUUID().toString()

    class Individual(val data: PalabraUI = PalabraUI()) : ElementoUI()

    class Conjunto(nombreIni: String = "", categoriaOrigenIni: String = "") : ElementoUI() {
        var nombre by mutableStateOf(nombreIni)
        var errorNombre by mutableStateOf(false)
        var mensajeErrorNombre by mutableStateOf("")
        val palabras = mutableStateListOf<PalabraUI>()
        var expandido by mutableStateOf(false)
        var cargando by mutableStateOf(false)
        var categoriaOrigen by mutableStateOf(categoriaOrigenIni) // 👇 NUEVO
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaCrear(
    coleccionParaEditar: ColeccionGuardada?,
    snackbarHostState: SnackbarHostState,
    onGuardadoExitoso: () -> Unit,
    onVolver: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // --- LÓGICA DE IDIOMAS ---
    val idiomaActual by GestorIdiomas.idiomaActual.collectAsState()
    val textos = obtenerTextosCrear(idiomaActual)
    // -------------------------

    val usuarioActual by GestorAuth.usuario.collectAsState()

    var estaCargando by remember { mutableStateOf(true) }

    var nombreColeccion by remember { mutableStateOf("") }
    var categoriaColeccion by remember { mutableStateOf("") }

    var esPublica by remember { mutableStateOf(coleccionParaEditar?.esPublica ?: false) }
    var mostrarDialogoPrivacidad by remember { mutableStateOf(false) }
    var estadoDeseadoPrivacidad by remember { mutableStateOf(false) }

    var errorNombreCol by remember { mutableStateOf(false) }
    var mensajeErrorNombreCol by remember { mutableStateOf("") }
    var errorCategoriaCol by remember { mutableStateOf(false) }
    var mensajeErrorCategoriaCol by remember { mutableStateOf("") }

    val elementos = remember { mutableStateListOf<ElementoUI>() }

    var textoBusqueda by remember { mutableStateOf("") }
    var mostrandoBuscador by remember { mutableStateOf(false) }

    var mostrarDialogoImportar by remember { mutableStateOf(false) }
    var textoAImportar by remember { mutableStateOf("") }
    var elementosPendientesImportar by remember { mutableStateOf<List<ElementoUI>>(emptyList()) }
    var mostrarDialogoConflictos by remember { mutableStateOf(false) }


    var mostrarDialogoOpcionesTexto by remember { mutableStateOf(false) }
    var mostrarDialogoExportar by remember { mutableStateOf(false) }
    var textoExportado by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current

    // 👇 NUEVO: Variables para controlar la mezcla
    var mostrarDialogoMezclar by remember { mutableStateOf(false) }
    var listasAMezclar by remember { mutableStateOf<Set<ColeccionGuardada>>(emptySet()) }

    var indiceParaBorrar by remember { mutableStateOf<Int?>(null) }
    var tituloDialogoBorrado by remember { mutableStateOf("") }

    var palabraBuscandoImagen by remember { mutableStateOf<PalabraUI?>(null) }
    var contextoBusquedaImagen by remember { mutableStateOf("") }
    var mostrarBuscadorImagen by remember { mutableStateOf(false) }

    var mostrarDialogoSalirSinGuardar by remember { mutableStateOf(false) }

    // FUNCIÓN AUXILIAR: Detectar si el usuario modificó algo respecto al estado inicial
    fun tieneCambiosSinGuardar(): Boolean {
        if (coleccionParaEditar == null) {
            // Si es nueva, hay cambios si escribió nombre, categoría o alteró el elemento vacío por defecto
            if (nombreColeccion.isNotBlank() || categoriaColeccion.isNotBlank()) return true
            if (elementos.size > 1) return true
            val primer = elementos.firstOrNull() as? ElementoUI.Individual
            if (primer != null && (primer.data.palabra.isNotBlank() || primer.data.pista.isNotBlank())) return true
            return false
        }

        // Si estamos editando, comparamos campos base
        if (nombreColeccion != coleccionParaEditar.nombre) return true
        if (categoriaColeccion != coleccionParaEditar.categoria) return true
        if (esPublica != coleccionParaEditar.esPublica) return true

        // Mapeamos los elementos UI actuales a una estructura comparable simplificada
        val listaActualGuardable = elementos.map { ui ->
            when (ui) {
                is ElementoUI.Individual -> ElementoGuardado.Individual(ui.data.palabra.trim(), ui.data.pista.trim(), ui.data.imagenUrl)
                is ElementoUI.Conjunto -> ElementoGuardado.Conjunto(ui.nombre.trim(), ui.palabras.map { ElementoGuardado.Individual(it.palabra.trim(), it.pista.trim(), it.imagenUrl) })
            }
        }

        // Comparamos tamaños y contenidos limpiando espacios
        if (listaActualGuardable.size != coleccionParaEditar.elementos.size) return true

        for (i in listaActualGuardable.indices) {
            val elAct = listaActualGuardable[i]
            val elOrig = coleccionParaEditar.elementos[i]

            if (elAct::class != elOrig::class) return true

            if (elAct is ElementoGuardado.Individual && elOrig is ElementoGuardado.Individual) {
                if (elAct.palabra != elOrig.palabra.trim() || elAct.pista != elOrig.pista.trim() || elAct.imagenUrl != elOrig.imagenUrl) return true
            } else if (elAct is ElementoGuardado.Conjunto && elOrig is ElementoGuardado.Conjunto) {
                if (elAct.nombreConjunto != elOrig.nombreConjunto.trim()) return true
                if (elAct.palabras.size != elOrig.palabras.size) return true
                for (j in elAct.palabras.indices) {
                    val pAct = elAct.palabras[j]
                    val pOrig = elOrig.palabras[j]
                    if (pAct.palabra != pOrig.palabra.trim() || pAct.pista != pOrig.pista.trim() || pAct.imagenUrl != pOrig.imagenUrl) return true
                }
            }
        }
        return false
    }

    // ACCIÓN DE GUARDADO CENTRALIZADA
    fun ejecutarGuardadoCompleto() {
        var hayError = false
        var primerIndiceError = -1
        var totalPalabrasRellenas = 0

        if (nombreColeccion.trim().isBlank()) {
            errorNombreCol = true; hayError = true; primerIndiceError = 0
        }
        if (categoriaColeccion.trim().isBlank()) {
            errorCategoriaCol = true; hayError = true; primerIndiceError = 0
        }

        elementos.forEachIndexed { index, el ->
            val indiceRealEnLista = index + 1
            when (el) {
                is ElementoUI.Individual -> {
                    if (el.data.palabra.isNotBlank() && el.data.pista.isNotBlank()) totalPalabrasRellenas++

                    if (el.data.palabra.isBlank() || el.data.pista.isBlank() || el.data.errorPalabra || el.data.errorPista) {
                        if (el.data.palabra.isBlank()) el.data.errorPalabra = true
                        if (el.data.pista.isBlank()) el.data.errorPista = true
                        hayError = true; if (primerIndiceError == -1) primerIndiceError = indiceRealEnLista
                    }
                }

                is ElementoUI.Conjunto -> {
                    if (el.nombre.isBlank()) {
                        el.errorNombre = true; hayError = true; el.expandido = true; if (primerIndiceError == -1) primerIndiceError = indiceRealEnLista
                    }
                    el.palabras.forEach { p ->
                        if (p.palabra.isNotBlank() && p.pista.isNotBlank()) totalPalabrasRellenas++

                        if (p.palabra.isBlank() || p.pista.isBlank() || p.errorPalabra || p.errorPista) {
                            if (p.palabra.isBlank()) p.errorPalabra = true
                            if (p.pista.isBlank()) p.errorPista = true
                            hayError = true; el.expandido = true; if (primerIndiceError == -1) primerIndiceError = indiceRealEnLista
                        }
                    }
                }
            }
        }

        if (GestorDatos.pasoTutorialActual == 2 && totalPalabrasRellenas < 3) {
            coroutineScope.launch { snackbarHostState.showSnackbar(textos.msgMinimoPalabras) }
            return
        }

        if (hayError) {
            coroutineScope.launch {
                listState.animateScrollToItem(primerIndiceError.coerceAtLeast(0))
                snackbarHostState.showSnackbar(textos.msgFaltanCampos)
            }
        } else {
            coroutineScope.launch {
                val categoriaPrincipal = categoriaColeccion.capitalizarPrimeraCrear()

                val elementosGuardables = elementos.map { ui ->
                    when (ui) {
                        is ElementoUI.Individual -> {
                            val catOrigen = if (ui.data.categoriaOrigen.isNotBlank()) ui.data.categoriaOrigen else categoriaPrincipal
                            ElementoGuardado.Individual(
                                ui.data.palabra.capitalizarPrimeraCrear(),
                                ui.data.pista.capitalizarPrimeraCrear(),
                                ui.data.imagenUrl,
                                catOrigen
                            )
                        }
                        is ElementoUI.Conjunto -> {
                            val catOrigenConjunto = if (ui.categoriaOrigen.isNotBlank()) ui.categoriaOrigen else categoriaPrincipal
                            ElementoGuardado.Conjunto(
                                ui.nombre.capitalizarPrimeraCrear(),
                                ui.palabras.map { p ->
                                    // 👇 CLAVE AQUÍ: Hereda del conjunto si la palabra no tiene categoría propia
                                    val catOrigenPalabra = if (p.categoriaOrigen.isNotBlank()) p.categoriaOrigen else catOrigenConjunto
                                    ElementoGuardado.Individual(
                                        p.palabra.capitalizarPrimeraCrear(),
                                        p.pista.capitalizarPrimeraCrear(),
                                        p.imagenUrl,
                                        catOrigenPalabra
                                    )
                                },
                                catOrigenConjunto
                            )
                        }
                    }
                }

                val categoriasMezcladas = elementosGuardables.flatMap {
                    if (it is ElementoGuardado.Individual) listOf(it.categoriaOrigen)
                    else listOf((it as ElementoGuardado.Conjunto).categoriaOrigen)
                }.filter { it.isNotBlank() }.distinct()

                val nombreAutor = if (usuarioActual != null) GestorAuth.obtenerNombreUsuario(usuarioActual!!.uid) else null

                val nuevaLista = ColeccionGuardada(
                    nombre = nombreColeccion.capitalizarPrimeraCrear(),
                    categoria = categoriaPrincipal,
                    elementos = elementosGuardables,
                    esPublica = esPublica,
                    idCreador = usuarioActual?.uid,
                    nombreCreador = nombreAutor,
                    likes = coleccionParaEditar?.likes ?: 0,
                    usuariosLikes = coleccionParaEditar?.usuariosLikes ?: emptyList(),
                    categoriasMezcladas = categoriasMezcladas
                )

                if (coleccionParaEditar != null) GestorDatos.actualizarColeccion(coleccionParaEditar.nombre, nuevaLista)
                else GestorDatos.guardarNuevaColeccion(nuevaLista)

                if (usuarioActual != null) GestorDatos.subirColeccionNube(usuarioActual!!.uid, nuevaLista)

                onGuardadoExitoso()
            }
        }
    }

    fun intentarSalirOVolver() {
        if (GestorDatos.pasoTutorialActual == 2) {
            coroutineScope.launch { snackbarHostState.showSnackbar(textos.msgSalirTutorial) }
        } else if (tieneCambiosSinGuardar()) {
            mostrarDialogoSalirSinGuardar = true
        } else {
            onVolver()
        }
    }


    fun generarTextoExportacion(): String {
        val sb = StringBuilder()
        elementos.forEach { el ->
            when (el) {
                is ElementoUI.Individual -> {
                    if (el.data.palabra.isNotBlank() && el.data.pista.isNotBlank()) {
                        sb.append("${el.data.palabra}, ${el.data.pista}\n\n")
                    }
                }
                is ElementoUI.Conjunto -> {
                    val palabrasValidas = el.palabras.filter { it.palabra.isNotBlank() && it.pista.isNotBlank() }
                    if (el.nombre.isNotBlank() && palabrasValidas.isNotEmpty()) {
                        sb.append(".${el.nombre}\n")
                        palabrasValidas.forEachIndexed { index, p ->
                            if (index == palabrasValidas.size - 1) {
                                sb.append("${p.palabra}, ${p.pista}.\n\n")
                            } else {
                                sb.append("${p.palabra}, ${p.pista}\n")
                            }
                        }
                    }
                }
            }
        }
        return sb.toString().trim()
    }

    fun validarDuplicadosGlobales() {
        val palabrasVistas = mutableSetOf<String>()

        elementos.forEach { el ->
            when (el) {
                is ElementoUI.Individual -> {
                    el.data.errorPalabra = false; el.data.errorPista = false
                    val pLimpia = el.data.palabra.trim().lowercase()

                    if (pLimpia.isNotEmpty() && !palabrasVistas.add(pLimpia)) {
                        el.data.errorPalabra = true; el.data.mensajeErrorPalabra = textos.errorRepetida
                    }
                }
                is ElementoUI.Conjunto -> {
                    el.palabras.forEach { p ->
                        p.errorPalabra = false; p.errorPista = false
                        val pLimpia = p.palabra.trim().lowercase()

                        if (pLimpia.isNotEmpty() && !palabrasVistas.add(pLimpia)) {
                            p.errorPalabra = true; p.mensajeErrorPalabra = textos.errorRepetida
                        }
                    }
                }
            }
        }
    }

    fun aplicarYFusionarElementos(nuevos: List<ElementoUI>) {
        nuevos.forEach { nuevo ->
            when (nuevo) {
                is ElementoUI.Individual -> elementos.add(nuevo)
                is ElementoUI.Conjunto -> {
                    val grupoExistente = elementos.filterIsInstance<ElementoUI.Conjunto>()
                        .find { it.nombre.equals(nuevo.nombre, ignoreCase = true) }

                    if (grupoExistente != null) {
                        val palabrasEnExistente = grupoExistente.palabras.map { it.palabra.lowercase() }.toSet()
                        val palabrasAAñadir = nuevo.palabras.filter { it.palabra.lowercase() !in palabrasEnExistente }
                        grupoExistente.palabras.addAll(palabrasAAñadir)
                        grupoExistente.expandido = true
                    } else {
                        elementos.add(nuevo)
                    }
                }
            }
        }
    }

    fun procesarTextoImportacion(texto: String) {
        val nuevosElementos = mutableListOf<ElementoUI>()
        var conjuntoActual: ElementoUI.Conjunto? = null

        val lineas = texto.lines()
        for (lineaOriginal in lineas) {
            val linea = lineaOriginal.trim()
            if (linea.isBlank()) continue
            if (linea == ".") {
                conjuntoActual = null; continue
            }

            if (linea.startsWith(".") && !linea.contains(",") && linea.length > 1) {
                conjuntoActual = ElementoUI.Conjunto(linea.substring(1).trim()).apply { expandido = true }
                nuevosElementos.add(conjuntoActual)
                continue
            }

            if (linea.contains(",")) {
                val partes = linea.split(",", limit = 2)
                val palabra = partes[0].trim()
                var pista = partes[1].trim()
                val terminaColeccion = pista.endsWith(".")
                if (terminaColeccion) pista = pista.dropLast(1).trim()

                if (conjuntoActual != null) {
                    conjuntoActual.palabras.add(PalabraUI(palabra, pista))
                    if (terminaColeccion) conjuntoActual = null
                } else {
                    nuevosElementos.add(ElementoUI.Individual(PalabraUI(palabra, pista)))
                }
            }
        }

        val palabrasActuales = elementos.flatMap { if (it is ElementoUI.Individual) listOf(it.data.palabra.lowercase()) else (it as ElementoUI.Conjunto).palabras.map { p -> p.palabra.lowercase() } }.toSet()
        val nuevasPalabras = nuevosElementos.flatMap { if (it is ElementoUI.Individual) listOf(it.data.palabra.lowercase()) else (it as ElementoUI.Conjunto).palabras.map { p -> p.palabra.lowercase() } }

        if (nuevasPalabras.any { it in palabrasActuales }) {
            elementosPendientesImportar = nuevosElementos
            mostrarDialogoConflictos = true
        } else {
            aplicarYFusionarElementos(nuevosElementos)
            validarDuplicadosGlobales()
            mostrarDialogoImportar = false; textoAImportar = ""
            coroutineScope.launch { snackbarHostState.showSnackbar(textos.msgImportExito) }
        }
    }

    LaunchedEffect(Unit) {
        if (coleccionParaEditar != null) {
            nombreColeccion = coleccionParaEditar.nombre
            categoriaColeccion = coleccionParaEditar.categoria

            coleccionParaEditar.elementos.forEach { el ->
                when (el) {
                    is ElementoGuardado.Individual -> elementos.add(ElementoUI.Individual(PalabraUI(el.palabra, el.pista, el.imagenUrl, el.categoriaOrigen)))
                    is ElementoGuardado.Conjunto -> {
                        val nuevoConjunto = ElementoUI.Conjunto(el.nombreConjunto, el.categoriaOrigen)
                        el.palabras.forEach { p ->
                            nuevoConjunto.palabras.add(PalabraUI(p.palabra, p.pista, p.imagenUrl, p.categoriaOrigen))
                        }
                        elementos.add(nuevoConjunto)
                    }
                }
            }
        } else {
            elementos.add(ElementoUI.Individual())
        }
        delay(600)
        estaCargando = false
    }

    androidx.activity.compose.BackHandler(enabled = true) {
        intentarSalirOVolver()
    }

    val opcionesTeclado = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)

    if (estaCargando) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0xFFF9F9F9)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Color(0xFFFF6D00), modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text(textos.cargandoTaller, color = Color.Gray, fontWeight = FontWeight.Bold)
            }
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFE8F0FE))) {
        Column(modifier = Modifier.fillMaxSize()) {
            // CABECERA
            Column(modifier = Modifier.fillMaxWidth().background(Color.White).padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { intentarSalirOVolver() }) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Volver")
                    }
                    Text(
                        if (coleccionParaEditar != null) textos.tituloEditar else textos.tituloNueva,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.weight(1f))

                    IconButton(onClick = { mostrandoBuscador = !mostrandoBuscador }, modifier = Modifier.size(40.dp)) {
                        Icon(if (mostrandoBuscador) Icons.Rounded.SearchOff else Icons.Rounded.Search, contentDescription = "Buscar", tint = Color.Gray)
                    }

                    IconButton(onClick = { mostrarDialogoOpcionesTexto = true }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Rounded.DataArray, contentDescription = "Opciones Texto", tint = Color(0xFF18C1A8))
                    }

                    Button(
                        onClick = { ejecutarGuardadoCompleto() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF18C1A8)),
                        modifier = Modifier.padding(start = 4.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(textos.btnGuardar, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }
                }

                AnimatedVisibility(visible = mostrandoBuscador) {
                    OutlinedTextField(
                        value = textoBusqueda, onValueChange = { textoBusqueda = it },
                        placeholder = { Text(textos.placeholderBuscar) },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        trailingIcon = { if (textoBusqueda.isNotEmpty()) IconButton(onClick = { textoBusqueda = "" }) { Icon(Icons.Rounded.Clear, null) } },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        singleLine = true, shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (textoBusqueda.isEmpty()) {
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                OutlinedTextField(
                                    value = nombreColeccion,
                                    onValueChange = { nombreColeccion = it; errorNombreCol = false },
                                    label = { Text(textos.labelNombreLista) },
                                    keyboardOptions = opcionesTeclado,
                                    isError = errorNombreCol,
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = categoriaColeccion,
                                    onValueChange = { categoriaColeccion = it; errorCategoriaCol = false },
                                    label = { Text(textos.labelCategoria) },
                                    keyboardOptions = opcionesTeclado,
                                    isError = errorCategoriaCol,
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        if (usuarioActual == null) {
                                            coroutineScope.launch { snackbarHostState.showSnackbar(textos.msgSesionPublicar) }
                                        } else {
                                            estadoDeseadoPrivacidad = !esPublica
                                            mostrarDialogoPrivacidad = true
                                        }
                                    },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(if (esPublica) Icons.Rounded.Public else Icons.Rounded.Lock, contentDescription = null, tint = if (esPublica) Color(0xFF18C1A8) else Color.Gray)
                                        Spacer(Modifier.width(12.dp))
                                        Column {
                                            Text(if (esPublica) textos.tituloPublica else textos.tituloPrivada, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text(if (esPublica) textos.descPublica else textos.descPrivada, fontSize = 11.sp, color = Color.Gray)
                                        }
                                    }
                                    Switch(
                                        checked = esPublica,
                                        onCheckedChange = {
                                            if (usuarioActual == null) {
                                                coroutineScope.launch { snackbarHostState.showSnackbar(textos.msgSesionPrimero) }
                                            } else {
                                                estadoDeseadoPrivacidad = it
                                                mostrarDialogoPrivacidad = true
                                            }
                                        },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF18C1A8))
                                    )
                                }
                            }
                        }
                    }
                }

                itemsIndexed(elementos) { index, elemento ->
                    val coincideBusqueda = textoBusqueda.isBlank() || when (elemento) {
                        is ElementoUI.Individual -> elemento.data.palabra.contains(textoBusqueda, true) || elemento.data.pista.contains(textoBusqueda, true)
                        is ElementoUI.Conjunto -> elemento.nombre.contains(textoBusqueda, true) || elemento.palabras.any { it.palabra.contains(textoBusqueda, true) || it.pista.contains(textoBusqueda, true) }
                    }

                    if (!coincideBusqueda) return@itemsIndexed

                    when (elemento) {
                        is ElementoUI.Individual -> {
                            Card(colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0x3318C1A8)), elevation = CardDefaults.cardElevation(2.dp)) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(textos.etiquetaPalabraIndividual, color = Color(0xFF18C1A8), fontWeight = FontWeight.Bold)
                                            // 👇 CHAPA VISUAL CON EL NOMBRE DE LA CATEGORÍA
                                            if (elemento.data.categoriaOrigen.isNotBlank()) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Surface(color = Color(0xFFE0F2F1), shape = RoundedCornerShape(4.dp)) {
                                                    Text(elemento.data.categoriaOrigen.uppercase(), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00897B))
                                                }
                                            }
                                        }
                                        IconButton(onClick = { tituloDialogoBorrado = textos.tituloBorrarPalabra; indiceParaBorrar = index }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Rounded.Close, contentDescription = "Borrar", tint = Color.Red)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = elemento.data.palabra,
                                        onValueChange = { elemento.data.palabra = it; validarDuplicadosGlobales() },
                                        label = { Text(textos.labelPalabra) },
                                        keyboardOptions = opcionesTeclado,
                                        isError = elemento.data.errorPalabra,
                                        supportingText = { if (elemento.data.errorPalabra) Text(elemento.data.mensajeErrorPalabra, color = MaterialTheme.colorScheme.error) },
                                        trailingIcon = {
                                            IconButton(onClick = { palabraBuscandoImagen = elemento.data; contextoBusquedaImagen = ""; mostrarBuscadorImagen = true }) {
                                                if (elemento.data.imagenUrl != null) {
                                                    KamelImage(resource = asyncPainterResource(elemento.data.imagenUrl!!), contentDescription = "Imagen", modifier = Modifier.size(28.dp).clip(CircleShape), contentScale = ContentScale.Fit)
                                                } else {
                                                    Icon(Icons.Rounded.ImageSearch, contentDescription = "Buscar Imagen", tint = Color(0xFF18C1A8))
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = elemento.data.pista,
                                        onValueChange = { elemento.data.pista = it; validarDuplicadosGlobales() },
                                        label = { Text(textos.labelPistas) },
                                        keyboardOptions = opcionesTeclado,
                                        isError = elemento.data.errorPista,
                                        supportingText = { if (elemento.data.errorPista) Text(elemento.data.mensajeErrorPista, color = MaterialTheme.colorScheme.error) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                    // 👇 CAMPO PARA EDITAR LA CATEGORÍA
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = elemento.data.categoriaOrigen,
                                        onValueChange = { elemento.data.categoriaOrigen = it },
                                        label = { Text(textos.labelCategoriaElemento) },
                                        keyboardOptions = opcionesTeclado,
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                }
                            }
                        }

                        is ElementoUI.Conjunto -> {
                            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2C)), elevation = CardDefaults.cardElevation(4.dp)) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().clickable { elemento.expandido = !elemento.expandido; if (elemento.expandido) elemento.cargando = true }.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(imageVector = if (elemento.expandido) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, contentDescription = null, tint = Color(0xFFFF6D00))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        // 👇 CHAPA VISUAL DEL GRUPO EN LA CABECERA
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = if (elemento.nombre.isBlank()) textos.placeholderGrupo else elemento.nombre, color = Color.White, fontWeight = FontWeight.Bold)
                                            if (elemento.categoriaOrigen.isNotBlank()) {
                                                Text(elemento.categoriaOrigen.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF6D00))
                                            }
                                        }
                                        if (!elemento.expandido && (elemento.errorNombre || elemento.palabras.any { it.errorPalabra || it.errorPista })) {
                                            Icon(Icons.Rounded.Warning, contentDescription = "Error", tint = Color.Red)
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                        IconButton(onClick = { tituloDialogoBorrado = textos.tituloBorrarGrupo; indiceParaBorrar = index }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Rounded.Delete, contentDescription = "Borrar conjunto", tint = Color.Red)
                                        }
                                    }

                                    AnimatedVisibility(visible = elemento.expandido || textoBusqueda.isNotEmpty()) {
                                        Column(modifier = Modifier.fillMaxWidth().background(Color(0xFFF0F5F5)).padding(16.dp)) {
                                            OutlinedTextField(
                                                value = elemento.nombre,
                                                onValueChange = { elemento.nombre = it; elemento.errorNombre = false },
                                                label = { Text(textos.labelNombreGrupo) },
                                                keyboardOptions = opcionesTeclado,
                                                isError = elemento.errorNombre,
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            // 👇 CAMPO PARA EDITAR CATEGORÍA DEL GRUPO
                                            OutlinedTextField(
                                                value = elemento.categoriaOrigen,
                                                onValueChange = { elemento.categoriaOrigen = it },
                                                label = { Text(textos.labelCategoriaElemento) },
                                                keyboardOptions = opcionesTeclado,
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))

                                            elemento.palabras.forEachIndexed { i, p ->
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text("${i + 1}.", fontWeight = FontWeight.Bold, color = Color.Gray)
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        OutlinedTextField(
                                                            value = p.palabra,
                                                            onValueChange = { p.palabra = it; validarDuplicadosGlobales() },
                                                            placeholder = { Text(textos.placeholderPalabra) },
                                                            keyboardOptions = opcionesTeclado,
                                                            isError = p.errorPalabra,
                                                            supportingText = { if (p.errorPalabra) Text(p.mensajeErrorPalabra, color = MaterialTheme.colorScheme.error) },
                                                            trailingIcon = {
                                                                IconButton(onClick = { palabraBuscandoImagen = p; contextoBusquedaImagen = elemento.nombre; mostrarBuscadorImagen = true }) {
                                                                    if (p.imagenUrl != null) {
                                                                        KamelImage(resource = asyncPainterResource(p.imagenUrl!!), contentDescription = "Imagen", modifier = Modifier.size(28.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                                                                    } else {
                                                                        Icon(Icons.Rounded.ImageSearch, contentDescription = "Buscar Imagen", tint = Color(0xFF18C1A8))
                                                                    }
                                                                }
                                                            },
                                                            modifier = Modifier.fillMaxWidth(),
                                                            singleLine = true
                                                        )
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        OutlinedTextField(
                                                            value = p.pista,
                                                            onValueChange = { p.pista = it; validarDuplicadosGlobales() },
                                                            placeholder = { Text(textos.labelPistas) },
                                                            keyboardOptions = opcionesTeclado,
                                                            isError = p.errorPista,
                                                            supportingText = { if (p.errorPista) Text(p.mensajeErrorPista, color = MaterialTheme.colorScheme.error) },
                                                            modifier = Modifier.fillMaxWidth(),
                                                            singleLine = true
                                                        )
                                                    }
                                                    IconButton(onClick = { elemento.palabras.removeAt(i); validarDuplicadosGlobales() }) {
                                                        Icon(Icons.Rounded.Close, contentDescription = "Borrar", tint = Color.Gray)
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(12.dp))
                                            }

                                            TextButton(onClick = { elemento.palabras.add(PalabraUI()) }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                                                Icon(Icons.Rounded.Add, contentDescription = null, tint = Color(0xFFFF6D00))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(textos.btnAnadirPalabraGrupo, color = Color(0xFFFF6D00), fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (textoBusqueda.isEmpty()) {
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            OutlinedButton(
                                onClick = {
                                    elementos.add(ElementoUI.Individual())
                                    coroutineScope.launch { delay(100); listState.animateScrollToItem(elementos.size) }
                                },
                                modifier = Modifier.weight(1f).height(60.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF18C1A8)),
                                border = BorderStroke(2.dp, Color(0xFF18C1A8))
                            ) { Text(textos.btnAnadirPalabra, fontWeight = FontWeight.Bold) }
                            Button(
                                onClick = {
                                    val nuevoConjunto = ElementoUI.Conjunto().apply { palabras.add(PalabraUI()); expandido = true }
                                    elementos.add(nuevoConjunto)
                                    coroutineScope.launch { delay(100); listState.animateScrollToItem(elementos.size) }
                                },
                                modifier = Modifier.weight(1f).height(60.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2C))
                            ) { Text(textos.btnAnadirGrupo, fontWeight = FontWeight.Bold, color = Color.White) }
                        }
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
            }
        }

        // FLECHAS RÁPIDAS
        Column(
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.End
        ) {
            val canScrollUp = listState.canScrollBackward
            val canScrollDown = listState.canScrollForward

            AnimatedVisibility(visible = canScrollUp, enter = scaleIn(), exit = scaleOut()) {
                FloatingActionButton(onClick = { coroutineScope.launch { listState.animateScrollToItem(0) } }, containerColor = Color.White, contentColor = Color(0xFF18C1A8), modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.ArrowUpward, "Subir")
                }
            }
            AnimatedVisibility(visible = canScrollDown, enter = scaleIn(), exit = scaleOut()) {
                FloatingActionButton(onClick = { coroutineScope.launch { listState.animateScrollToItem(elementos.size + 1) } }, containerColor = Color(0xFF18C1A8), contentColor = Color.White, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.ArrowDownward, "Bajar")
                }
            }
        }
    }

    // --- NUEVO DIÁLOGO: SALIR SIN GUARDAR ---
    if (mostrarDialogoSalirSinGuardar) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoSalirSinGuardar = false },
            title = { Text("Cambios sin guardar", fontWeight = FontWeight.Bold) },
            text = { Text("Has realizado cambios en la lista. ¿Qué deseas hacer antes de salir?") },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            mostrarDialogoSalirSinGuardar = false
                            ejecutarGuardadoCompleto()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF18C1A8)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Guardar y salir", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            mostrarDialogoSalirSinGuardar = false
                            onVolver()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Salir sin guardar", color = Color.White)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { mostrarDialogoSalirSinGuardar = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancelar", color = Color.Gray, textAlign = TextAlign.Center)
                }
            }
        )
    }

    if (mostrarDialogoPrivacidad) {
        val hacerPublica = estadoDeseadoPrivacidad
        AlertDialog(
            onDismissRequest = { mostrarDialogoPrivacidad = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = if (hacerPublica) Icons.Rounded.Public else Icons.Rounded.Lock, contentDescription = null, tint = if (hacerPublica) Color(0xFF18C1A8) else Color(0xFFFF6D00))
                    Spacer(Modifier.width(8.dp))
                    Text(if (hacerPublica) textos.tituloDialogoPublicar else textos.tituloDialogoPrivar)
                }
            },
            text = { Text(text = if (hacerPublica) textos.descDialogoPublicar else textos.descDialogoPrivar, fontSize = 15.sp) },
            confirmButton = {
                Button(onClick = { esPublica = estadoDeseadoPrivacidad; mostrarDialogoPrivacidad = false }, colors = ButtonDefaults.buttonColors(containerColor = if (hacerPublica) Color(0xFF18C1A8) else Color(0xFFFF6D00))) {
                    Text(textos.btnConfirmar)
                }
            },
            dismissButton = { TextButton(onClick = { mostrarDialogoPrivacidad = false }) { Text(textos.btnCancelar, color = Color.Gray) } }
        )
    }

    if (mostrarBuscadorImagen && palabraBuscandoImagen != null) {
        val palabraBuscada = palabraBuscandoImagen!!.palabra.ifBlank { "paisaje" }
        val queryFinal = if (contextoBusquedaImagen.isNotBlank()) "$palabraBuscada $contextoBusquedaImagen" else palabraBuscada
        val urlGoogleImages = "https://www.google.com/search?tbm=isch&q=${queryFinal.replace(" ", "+")}"

        Dialog(onDismissRequest = { mostrarBuscadorImagen = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(modifier = Modifier.fillMaxSize().padding(top = 32.dp), shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp), color = Color.White) {
                val webViewState = rememberWebViewState(urlGoogleImages)
                val navigator = rememberWebViewNavigator()

                Column(modifier = Modifier.fillMaxSize()) {
                    Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFF0F5F5)).padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { mostrarBuscadorImagen = false }) { Text(textos.btnCerrar, color = Color.Gray) }
                        Text(textos.tituloSeleccionarImagen, fontWeight = FontWeight.Bold)
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    navigator.evaluateJavaScript(
                                        """
                                        (function() {
                                            var imgs = document.querySelectorAll('img');
                                            var maxArea = 0;
                                            var bestSrc = '';
                                            imgs.forEach(function(img) {
                                                var rect = img.getBoundingClientRect();
                                                var area = rect.width * rect.height;
                                                if(area > maxArea && img.src && img.src.startsWith('http') && !img.src.includes('gstatic')) {
                                                    maxArea = area;
                                                    bestSrc = img.src;
                                                }
                                            });
                                            return bestSrc;
                                        })();
                                        """.trimIndent()
                                    ) { result: String? ->
                                        val urlLimpia = result?.trim('"', '\'')
                                        if (!urlLimpia.isNullOrEmpty() && urlLimpia != "null") {
                                            palabraBuscandoImagen!!.imagenUrl = urlLimpia
                                            mostrarBuscadorImagen = false
                                            coroutineScope.launch { snackbarHostState.showSnackbar(textos.msgImagenSelec) }
                                        } else {
                                            coroutineScope.launch { snackbarHostState.showSnackbar(textos.msgAbrirImagen) }
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6D00))
                        ) { Text(textos.btnSaveImagen, fontWeight = FontWeight.Bold) }
                    }
                    WebView(state = webViewState, navigator = navigator, modifier = Modifier.fillMaxSize().weight(1f))
                }
            }
        }
    }

    if (indiceParaBorrar != null) {
        AlertDialog(
            onDismissRequest = { indiceParaBorrar = null },
            title = { Text(textos.tituloEliminarElemento + tituloDialogoBorrado + "?") },
            text = { Text(textos.descEliminarElemento) },
            confirmButton = {
                TextButton(onClick = { indiceParaBorrar?.let { elementos.removeAt(it) }; indiceParaBorrar = null; validarDuplicadosGlobales() }) {
                    Text(textos.btnEliminar, color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { indiceParaBorrar = null }) { Text(textos.btnCancelar, color = Color.Gray) } }
        )
    }

    if (mostrarDialogoImportar) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoImportar = false },
            title = { Text(textos.tituloImportar) },
            text = {
                Column {
                    Text(textos.descImportar, fontSize = 14.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = textoAImportar, onValueChange = { textoAImportar = it }, modifier = Modifier.fillMaxWidth().height(150.dp), placeholder = { Text(textos.placeholderImportar) })
                }
            },
            confirmButton = {
                TextButton(onClick = { procesarTextoImportacion(textoAImportar) }) { Text(textos.btnProcesar, color = Color(0xFF18C1A8), fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { mostrarDialogoImportar = false }) { Text(textos.btnCancelar, color = Color.Gray) } }
        )
    }

    if (mostrarDialogoConflictos) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoConflictos = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Warning, null, tint = Color(0xFFFF6D00)); Spacer(Modifier.width(8.dp)); Text(textos.tituloConflictos)
                }
            },
            text = { Text(textos.descConflictos) },
            confirmButton = {
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val palabrasNuevasLowercase = elementosPendientesImportar.flatMap { if (it is ElementoUI.Individual) listOf(it.data.palabra.lowercase()) else (it as ElementoUI.Conjunto).palabras.map { p -> p.palabra.lowercase() } }.toSet()
                            elementos.forEach { el ->
                                if (el is ElementoUI.Individual && palabrasNuevasLowercase.contains(el.data.palabra.lowercase())) { el.data.palabra = "" }
                                if (el is ElementoUI.Conjunto) { el.palabras.removeAll { palabrasNuevasLowercase.contains(it.palabra.lowercase()) } }
                            }
                            elementos.removeAll { it is ElementoUI.Individual && it.data.palabra.isEmpty() }
                            aplicarYFusionarElementos(elementosPendientesImportar); validarDuplicadosGlobales(); mostrarDialogoConflictos = false; mostrarDialogoImportar = false; textoAImportar = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF18C1A8))
                    ) { Text(textos.btnReemplazar) }
                    Button(
                        onClick = {
                            val palabrasActuales = elementos.flatMap { if (it is ElementoUI.Individual) listOf(it.data.palabra.lowercase()) else (it as ElementoUI.Conjunto).palabras.map { p -> p.palabra.lowercase() } }.toSet()
                            val elementosLimpios = elementosPendientesImportar.mapNotNull { el ->
                                when (el) {
                                    is ElementoUI.Individual -> if (palabrasActuales.contains(el.data.palabra.lowercase())) null else el
                                    is ElementoUI.Conjunto -> {
                                        val palabrasFiltradas = el.palabras.filterNot { palabrasActuales.contains(it.palabra.lowercase()) }
                                        if (palabrasFiltradas.isNotEmpty()) {
                                            ElementoUI.Conjunto(el.nombre).apply { palabras.addAll(palabrasFiltradas); expandido = true }
                                        } else null
                                    }
                                }
                            }
                            aplicarYFusionarElementos(elementosLimpios); validarDuplicadosGlobales(); mostrarDialogoConflictos = false; mostrarDialogoImportar = false; textoAImportar = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2C))
                    ) { Text(textos.btnOmitirNuevas) }
                    OutlinedButton(onClick = { aplicarYFusionarElementos(elementosPendientesImportar); validarDuplicadosGlobales(); mostrarDialogoConflictos = false; mostrarDialogoImportar = false; textoAImportar = "" }) {
                        Text(textos.btnAnadirTodo, color = Color.Gray)
                    }
                }
            },
            dismissButton = { TextButton(onClick = { mostrarDialogoConflictos = false }) { Text(textos.btnCancelarImportacion) } }
        )
    }

    // 👇 NUEVOS DIÁLOGOS DE OPCIONES Y EXPORTACIÓN 👇

    // 👇 NUEVOS DIÁLOGOS DE OPCIONES Y EXPORTACIÓN 👇

    if (mostrarDialogoOpcionesTexto) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoOpcionesTexto = false },
            title = { Text(textos.tituloOpcionesTexto, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { mostrarDialogoOpcionesTexto = false; mostrarDialogoMezclar = true }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A148C))) {
                        Icon(Icons.Rounded.LibraryAdd, contentDescription = null); Spacer(Modifier.width(8.dp)); Text(textos.btnOpcionMezclar, fontWeight = FontWeight.Bold)
                    }
                    Button(onClick = { mostrarDialogoOpcionesTexto = false; mostrarDialogoImportar = true }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF18C1A8))) {
                        Icon(Icons.Rounded.Download, contentDescription = null); Spacer(Modifier.width(8.dp)); Text(textos.btnOpcionImportar, fontWeight = FontWeight.Bold)
                    }
                    Button(onClick = { mostrarDialogoOpcionesTexto = false; textoExportado = generarTextoExportacion(); mostrarDialogoExportar = true }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6D00))) {
                        Icon(Icons.Rounded.Upload, contentDescription = null); Spacer(Modifier.width(8.dp)); Text(textos.btnOpcionExportar, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { mostrarDialogoOpcionesTexto = false }) { Text(textos.btnCancelar, color = Color.Gray) } }
        )
    }

    if (mostrarDialogoMezclar) {
        val listasDisponibles = GestorDatos.coleccionesGlobales.filter { it.nombre != coleccionParaEditar?.nombre }
        AlertDialog(
            onDismissRequest = { mostrarDialogoMezclar = false },
            title = { Text(textos.tituloMezclar) },
            text = {
                Column {
                    Text(textos.descMezclar, color = Color.Gray, fontSize = 14.sp)
                    Spacer(Modifier.height(16.dp))
                    LazyColumn(Modifier.heightIn(max = 300.dp)) {
                        items(listasDisponibles) { col ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable {
                                listasAMezclar = if (listasAMezclar.contains(col)) listasAMezclar - col else listasAMezclar + col
                            }) {
                                Checkbox(
                                    checked = listasAMezclar.contains(col),
                                    onCheckedChange = null,
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF18C1A8))
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("${col.nombre} (${col.categoria})", fontSize = 16.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        listasAMezclar.forEach { col ->
                            col.elementos.forEach { el ->
                                when (el) {
                                    is ElementoGuardado.Individual -> {
                                        elementos.add(ElementoUI.Individual(PalabraUI(el.palabra, el.pista, el.imagenUrl, el.categoriaOrigen.ifBlank { col.categoria })))
                                    }
                                    is ElementoGuardado.Conjunto -> {
                                        val nuevoConjunto = ElementoUI.Conjunto(el.nombreConjunto, el.categoriaOrigen.ifBlank { col.categoria })
                                        el.palabras.forEach { p ->
                                            nuevoConjunto.palabras.add(PalabraUI(p.palabra, p.pista, p.imagenUrl, p.categoriaOrigen.ifBlank { col.categoria }))
                                        }
                                        elementos.add(nuevoConjunto)
                                    }
                                }
                            }
                        }
                        if (nombreColeccion.isBlank() && listasAMezclar.isNotEmpty()) {
                            nombreColeccion = (textos.tituloMezclar + " " + listasAMezclar.joinToString(", ") { it.nombre }).take(30)
                        }
                        if (categoriaColeccion.isBlank() && listasAMezclar.isNotEmpty()) {
                            categoriaColeccion = textos.tituloMezclar
                        }

                        validarDuplicadosGlobales()
                        mostrarDialogoMezclar = false
                        listasAMezclar = emptySet()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A148C))
                ) { Text(textos.btnConfirmarMezcla, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { mostrarDialogoMezclar = false; listasAMezclar = emptySet() }) { Text(textos.btnCancelar, color = Color.Gray) } }
        )
    }

    if (mostrarDialogoExportar) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoExportar = false },
            title = { Text(textos.tituloExportar) },
            text = { OutlinedTextField(value = textoExportado, onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth().height(300.dp), shape = RoundedCornerShape(12.dp)) },
            confirmButton = {
                TextButton(onClick = {
                    clipboardManager.setText(AnnotatedString(textoExportado))
                    coroutineScope.launch { snackbarHostState.showSnackbar(textos.msgCopiado) }
                    mostrarDialogoExportar = false
                }) { Text(textos.btnCopiar, color = Color(0xFF18C1A8), fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { mostrarDialogoExportar = false }) { Text(textos.btnCerrar, color = Color.Gray) } }
        )
    }
}