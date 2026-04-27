import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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

// 👇 IMPORTS DEL WEBVIEW Y KAMEL PARA IMÁGENES 👇
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewState
import com.multiplatform.webview.web.rememberWebViewNavigator
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource

// --- PEQUEÑA HERRAMIENTA PARA MAYÚSCULAS ---
fun String.capitalizarPrimeraCrear(): String {
    if (this.isBlank()) return this
    return this.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

// --- MODELOS DE ESTADO (Añadido imagenUrl) ---
class PalabraUI(palabraIni: String = "", pistaIni: String = "", imagenIni: String? = null) {
    var palabra by mutableStateOf(palabraIni)
    var pista by mutableStateOf(pistaIni)
    var imagenUrl by mutableStateOf(imagenIni) // NUEVO: Estado de la imagen

    var errorPalabra by mutableStateOf(false)
    var mensajeErrorPalabra by mutableStateOf("")
    var errorPista by mutableStateOf(false)
    var mensajeErrorPista by mutableStateOf("")
}

sealed class ElementoUI {
    val id = java.util.UUID.randomUUID().toString()

    class Individual(val data: PalabraUI = PalabraUI()) : ElementoUI()

    class Conjunto(nombreIni: String = "") : ElementoUI() {
        var nombre by mutableStateOf(nombreIni)
        var errorNombre by mutableStateOf(false)
        var mensajeErrorNombre by mutableStateOf("")
        val palabras = mutableStateListOf<PalabraUI>()
        var expandido by mutableStateOf(false)
        var cargando by mutableStateOf(false)
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

    var estaCargando by remember { mutableStateOf(true) }

    var nombreColeccion by remember { mutableStateOf("") }
    var categoriaColeccion by remember { mutableStateOf("") }

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

    var indiceParaBorrar by remember { mutableStateOf<Int?>(null) }
    var tituloDialogoBorrado by remember { mutableStateOf("") }

    // --- NUEVO: ESTADOS PARA EL BUSCADOR DE IMÁGENES ---
    var palabraBuscandoImagen by remember { mutableStateOf<PalabraUI?>(null) }
    var contextoBusquedaImagen by remember { mutableStateOf("") } // 👇 NUEVO: Guarda el nombre del grupo si lo hay
    var mostrarBuscadorImagen by remember { mutableStateOf(false) }

    // --- FUNCIONES DE UTILIDAD ---
    fun validarDuplicadosGlobales() {
        val palabrasVistas = mutableSetOf<String>()
        val pistasVistas = mutableSetOf<String>()

        elementos.forEach { el ->
            when (el) {
                is ElementoUI.Individual -> {
                    el.data.errorPalabra = false; el.data.errorPista = false
                    val pLimpia = el.data.palabra.trim().lowercase()
                    val cLimpia = el.data.pista.trim().lowercase()

                    if (pLimpia.isNotEmpty() && !palabrasVistas.add(pLimpia)) {
                        el.data.errorPalabra = true; el.data.mensajeErrorPalabra = "Repetida"
                    }
                    if (cLimpia.isNotEmpty() && !pistasVistas.add(cLimpia)) {
                        el.data.errorPista = true; el.data.mensajeErrorPista = "Repetida"
                    }
                }
                is ElementoUI.Conjunto -> {
                    el.palabras.forEach { p ->
                        p.errorPalabra = false; p.errorPista = false
                        val pLimpia = p.palabra.trim().lowercase()
                        val cLimpia = p.pista.trim().lowercase()

                        if (pLimpia.isNotEmpty() && !palabrasVistas.add(pLimpia)) {
                            p.errorPalabra = true; p.mensajeErrorPalabra = "Repetida"
                        }
                        if (cLimpia.isNotEmpty() && !pistasVistas.add(cLimpia)) {
                            p.errorPista = true; p.mensajeErrorPista = "Repetida"
                        }
                    }
                }
            }
        }
    }

    // 👇 LÓGICA DE FUSIÓN (Intacta) 👇
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
            if (linea == ".") { conjuntoActual = null; continue }

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
            coroutineScope.launch { snackbarHostState.showSnackbar("Elementos importados con éxito") }
        }
    }

    LaunchedEffect(Unit) {
        if (coleccionParaEditar != null) {
            nombreColeccion = coleccionParaEditar.nombre
            categoriaColeccion = coleccionParaEditar.categoria

            coleccionParaEditar.elementos.forEach { el ->
                when (el) {
                    is ElementoGuardado.Individual -> elementos.add(ElementoUI.Individual(PalabraUI(el.palabra, el.pista, el.imagenUrl)))
                    is ElementoGuardado.Conjunto -> {
                        val nuevoConjunto = ElementoUI.Conjunto(el.nombreConjunto)
                        el.palabras.forEach { p -> nuevoConjunto.palabras.add(PalabraUI(p.palabra, p.pista, p.imagenUrl)) }
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

    val opcionesTeclado = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)

    if (estaCargando) {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF9F9F9)), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Color(0xFFFF6D00), modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Preparando el taller...", color = Color.Gray, fontWeight = FontWeight.Bold)
            }
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFE8F0FE))) {
        Column(modifier = Modifier.fillMaxSize()) {
            // CABECERA
            Column(modifier = Modifier.fillMaxWidth().background(Color.White).padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onVolver) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Volver") }
                    Text(if (coleccionParaEditar != null) "Editar Lista" else "Nueva Lista", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.weight(1f))

                    IconButton(onClick = { mostrandoBuscador = !mostrandoBuscador }, modifier = Modifier.size(40.dp)) {
                        Icon(if (mostrandoBuscador) Icons.Rounded.SearchOff else Icons.Rounded.Search, contentDescription = "Buscar", tint = Color.Gray)
                    }

                    IconButton(onClick = { mostrarDialogoImportar = true }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Rounded.DataArray, contentDescription = "Importar Texto", tint = Color(0xFF18C1A8))
                    }

                    Button(
                        onClick = {
                            var hayError = false
                            var primerIndiceError = -1

                            if (nombreColeccion.trim().isBlank()) { errorNombreCol = true; hayError = true; primerIndiceError = 0 }
                            if (categoriaColeccion.trim().isBlank()) { errorCategoriaCol = true; hayError = true; primerIndiceError = 0 }

                            validarDuplicadosGlobales()

                            elementos.forEachIndexed { index, el ->
                                val indiceRealEnLista = index + 1
                                when (el) {
                                    is ElementoUI.Individual -> {
                                        if (el.data.palabra.isBlank() || el.data.pista.isBlank() || el.data.errorPalabra || el.data.errorPista) {
                                            if (el.data.palabra.isBlank()) el.data.errorPalabra = true
                                            if (el.data.pista.isBlank()) el.data.errorPista = true
                                            hayError = true; if (primerIndiceError == -1) primerIndiceError = indiceRealEnLista
                                        }
                                    }
                                    is ElementoUI.Conjunto -> {
                                        if (el.nombre.isBlank()) { el.errorNombre = true; hayError = true; el.expandido = true; if (primerIndiceError == -1) primerIndiceError = indiceRealEnLista }
                                        el.palabras.forEach { p ->
                                            if (p.palabra.isBlank() || p.pista.isBlank() || p.errorPalabra || p.errorPista) {
                                                if (p.palabra.isBlank()) p.errorPalabra = true
                                                if (p.pista.isBlank()) p.errorPista = true
                                                hayError = true; el.expandido = true; if (primerIndiceError == -1) primerIndiceError = indiceRealEnLista
                                            }
                                        }
                                    }
                                }
                            }

                            if (hayError) {
                                coroutineScope.launch {
                                    listState.animateScrollToItem(primerIndiceError.coerceAtLeast(0))
                                    snackbarHostState.showSnackbar("Faltan campos por rellenar o hay errores")
                                }
                            } else {
                                val elementosGuardables = elementos.map { ui ->
                                    when (ui) {
                                        is ElementoUI.Individual -> ElementoGuardado.Individual(ui.data.palabra.capitalizarPrimeraCrear(), ui.data.pista.capitalizarPrimeraCrear(), ui.data.imagenUrl)
                                        is ElementoUI.Conjunto -> ElementoGuardado.Conjunto(ui.nombre.capitalizarPrimeraCrear(), ui.palabras.map { p -> ElementoGuardado.Individual(p.palabra.capitalizarPrimeraCrear(), p.pista.capitalizarPrimeraCrear(), p.imagenUrl) })
                                    }
                                }

                                val nuevaLista = ColeccionGuardada(nombreColeccion.capitalizarPrimeraCrear(), categoriaColeccion.capitalizarPrimeraCrear(), elementosGuardables)

                                if (coleccionParaEditar != null) GestorDatos.actualizarColeccion(coleccionParaEditar.nombre, nuevaLista)
                                else GestorDatos.guardarNuevaColeccion(nuevaLista)

                                val usuarioLogueado = GestorAuth.usuario.value
                                if (usuarioLogueado != null) coroutineScope.launch { GestorDatos.subirColeccionNube(usuarioLogueado.uid, nuevaLista) }

                                onGuardadoExitoso()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF18C1A8)),
                        modifier = Modifier.padding(start = 4.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) { Text("GUARDAR", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false) }
                }

                AnimatedVisibility(visible = mostrandoBuscador) {
                    OutlinedTextField(
                        value = textoBusqueda, onValueChange = { textoBusqueda = it },
                        placeholder = { Text("Buscar palabra, pista o grupo...") },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        trailingIcon = { if(textoBusqueda.isNotEmpty()) IconButton(onClick = { textoBusqueda = "" }) { Icon(Icons.Rounded.Clear, null) } },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        singleLine = true, shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            LazyColumn(
                state = listState, modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (textoBusqueda.isEmpty()) {
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                OutlinedTextField(value = nombreColeccion, onValueChange = { nombreColeccion = it; errorNombreCol = false }, label = { Text("Nombre de la Lista") }, keyboardOptions = opcionesTeclado, isError = errorNombreCol, modifier = Modifier.fillMaxWidth(), singleLine = true)
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(value = categoriaColeccion, onValueChange = { categoriaColeccion = it; errorCategoriaCol = false }, label = { Text("Categoría (Ej: Películas)") }, keyboardOptions = opcionesTeclado, isError = errorCategoriaCol, modifier = Modifier.fillMaxWidth(), singleLine = true)
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
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text("Palabra Individual", color = Color(0xFF18C1A8), fontWeight = FontWeight.Bold)
                                        IconButton(onClick = { tituloDialogoBorrado = "Palabra Individual"; indiceParaBorrar = index }, modifier = Modifier.size(24.dp)) { Icon(Icons.Rounded.Close, contentDescription = "Borrar", tint = Color.Red) }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = elemento.data.palabra,
                                        onValueChange = { elemento.data.palabra = it; validarDuplicadosGlobales() },
                                        label = { Text("Palabra Secreta") }, keyboardOptions = opcionesTeclado,
                                        isError = elemento.data.errorPalabra,
                                        supportingText = { if(elemento.data.errorPalabra) Text(elemento.data.mensajeErrorPalabra, color = MaterialTheme.colorScheme.error) },
                                        trailingIcon = {
                                            IconButton(onClick = {
                                                palabraBuscandoImagen = elemento.data
                                                contextoBusquedaImagen = "" // 👇 No hay grupo, contexto vacío
                                                mostrarBuscadorImagen = true
                                            }) {
                                                if (elemento.data.imagenUrl != null) {
                                                    KamelImage(resource = asyncPainterResource(elemento.data.imagenUrl!!), contentDescription = "Imagen", modifier = Modifier.size(28.dp).clip(CircleShape), contentScale = ContentScale.Fit)
                                                } else {
                                                    Icon(Icons.Rounded.ImageSearch, contentDescription = "Buscar Imagen", tint = Color(0xFF18C1A8))
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(), singleLine = true
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = elemento.data.pista, onValueChange = { elemento.data.pista = it; validarDuplicadosGlobales() },
                                        label = { Text("Pista para el Impostor") }, keyboardOptions = opcionesTeclado,
                                        isError = elemento.data.errorPista, supportingText = { if(elemento.data.errorPista) Text(elemento.data.mensajeErrorPista, color = MaterialTheme.colorScheme.error) },
                                        modifier = Modifier.fillMaxWidth(), singleLine = true
                                    )
                                }
                            }
                        }

                        is ElementoUI.Conjunto -> {
                            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2C)), elevation = CardDefaults.cardElevation(4.dp)) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().clickable { elemento.expandido = !elemento.expandido; if(elemento.expandido) elemento.cargando = true }.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(imageVector = if (elemento.expandido) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, contentDescription = null, tint = Color(0xFFFF6D00))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = if (elemento.nombre.isBlank()) "Grupo de palabras..." else elemento.nombre, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                        if (!elemento.expandido && (elemento.errorNombre || elemento.palabras.any { it.errorPalabra || it.errorPista })) { Icon(Icons.Rounded.Warning, contentDescription = "Error", tint = Color.Red); Spacer(modifier = Modifier.width(8.dp)) }
                                        IconButton(onClick = { tituloDialogoBorrado = "Grupo Completo"; indiceParaBorrar = index }, modifier = Modifier.size(24.dp)) { Icon(Icons.Rounded.Delete, contentDescription = "Borrar conjunto", tint = Color.Red) }
                                    }

                                    AnimatedVisibility(visible = elemento.expandido || textoBusqueda.isNotEmpty()) {
                                        Column(modifier = Modifier.fillMaxWidth().background(Color(0xFFF0F5F5)).padding(16.dp)) {
                                            OutlinedTextField(value = elemento.nombre, onValueChange = { elemento.nombre = it; elemento.errorNombre = false }, label = { Text("Nombre de este Grupo") }, keyboardOptions = opcionesTeclado, isError = elemento.errorNombre, modifier = Modifier.fillMaxWidth(), singleLine = true)
                                            Spacer(modifier = Modifier.height(16.dp))

                                            elemento.palabras.forEachIndexed { i, p ->
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text("${i + 1}.", fontWeight = FontWeight.Bold, color = Color.Gray)
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        OutlinedTextField(
                                                            value = p.palabra, onValueChange = { p.palabra = it; validarDuplicadosGlobales() },
                                                            placeholder = { Text("Palabra") }, keyboardOptions = opcionesTeclado, isError = p.errorPalabra,
                                                            supportingText = { if(p.errorPalabra) Text(p.mensajeErrorPalabra, color = MaterialTheme.colorScheme.error) },
                                                            trailingIcon = {
                                                                IconButton(onClick = {
                                                                    palabraBuscandoImagen = p
                                                                    contextoBusquedaImagen = elemento.nombre // 👇 AQUÍ SE GUARDA EL NOMBRE DEL GRUPO (Ej: Black Clover)
                                                                    mostrarBuscadorImagen = true
                                                                }) {
                                                                    if (p.imagenUrl != null) {
                                                                        KamelImage(resource = asyncPainterResource(p.imagenUrl!!), contentDescription = "Imagen", modifier = Modifier.size(28.dp).clip(CircleShape), contentScale = ContentScale.Fit)
                                                                    } else {
                                                                        Icon(Icons.Rounded.ImageSearch, contentDescription = "Buscar Imagen", tint = Color(0xFF18C1A8))
                                                                    }
                                                                }
                                                            },
                                                            modifier = Modifier.fillMaxWidth(), singleLine = true
                                                        )
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        OutlinedTextField(value = p.pista, onValueChange = { p.pista = it; validarDuplicadosGlobales() }, placeholder = { Text("Pista") }, keyboardOptions = opcionesTeclado, isError = p.errorPista, supportingText = { if(p.errorPista) Text(p.mensajeErrorPista, color = MaterialTheme.colorScheme.error) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                                                    }
                                                    IconButton(onClick = { elemento.palabras.removeAt(i); validarDuplicadosGlobales() }) { Icon(Icons.Rounded.Close, contentDescription = "Borrar", tint = Color.Gray) }
                                                }
                                                Spacer(modifier = Modifier.height(12.dp))
                                            }

                                            TextButton(onClick = { elemento.palabras.add(PalabraUI()) }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Icon(Icons.Rounded.Add, contentDescription = null, tint = Color(0xFFFF6D00)); Spacer(modifier = Modifier.width(4.dp)); Text("AÑADIR PALABRA AL GRUPO", color = Color(0xFFFF6D00), fontWeight = FontWeight.Bold) }
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
                            OutlinedButton(onClick = { elementos.add(ElementoUI.Individual()); coroutineScope.launch { delay(100); listState.animateScrollToItem(elementos.size) } }, modifier = Modifier.weight(1f).height(60.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF18C1A8)), border = BorderStroke(2.dp, Color(0xFF18C1A8))) { Text("+ PALABRA", fontWeight = FontWeight.Bold) }
                            Button(onClick = { val nuevoConjunto = ElementoUI.Conjunto().apply { palabras.add(PalabraUI()); expandido = true }; elementos.add(nuevoConjunto); coroutineScope.launch { delay(100); listState.animateScrollToItem(elementos.size) } }, modifier = Modifier.weight(1f).height(60.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2C))) { Text("+ GRUPO", fontWeight = FontWeight.Bold, color = Color.White) }
                        }
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
            }
        }

        // --- BOTONES FLOTANTES ALINEADOS ABAJO A LA DERECHA ---
        Column(modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.End) {
            val canScrollUp = listState.canScrollBackward
            val canScrollDown = listState.canScrollForward

            AnimatedVisibility(visible = canScrollUp, enter = scaleIn(), exit = scaleOut()) { FloatingActionButton(onClick = { coroutineScope.launch { listState.animateScrollToItem(0) } }, containerColor = Color.White, contentColor = Color(0xFF18C1A8), modifier = Modifier.size(48.dp)) { Icon(Icons.Default.ArrowUpward, "Subir") } }
            AnimatedVisibility(visible = canScrollDown, enter = scaleIn(), exit = scaleOut()) { FloatingActionButton(onClick = { coroutineScope.launch { listState.animateScrollToItem(elementos.size + 1) } }, containerColor = Color(0xFF18C1A8), contentColor = Color.White, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.ArrowDownward, "Bajar") } }
        }
    }

    // --- DIÁLOGOS ---

    // 1. Buscador de Imágenes (AHORA EN DIALOG PARA SCROLL FLUIDO)
    if (mostrarBuscadorImagen && palabraBuscandoImagen != null) {
        val palabraBuscada = palabraBuscandoImagen!!.palabra.ifBlank { "paisaje" }

        // 👇 AQUÍ ESTÁ LA MEJORA FINAL: Solo se suma el nombre del grupo si la palabra está dentro de uno.
        val queryFinal = if (contextoBusquedaImagen.isNotBlank()) "$palabraBuscada $contextoBusquedaImagen" else palabraBuscada
        val urlGoogleImages = "https://www.google.com/search?tbm=isch&q=${queryFinal.replace(" ", "+")}"

        Dialog(
            onDismissRequest = { mostrarBuscadorImagen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false) // Permite ocupar el ancho completo
        ) {
            // Usamos un Surface para imitar el estilo de la cortina, pero fijo
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 32.dp), // Deja un pequeño margen arriba
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = Color.White
            ) {
                val webViewState = rememberWebViewState(urlGoogleImages)
                val navigator = rememberWebViewNavigator()

                Column(modifier = Modifier.fillMaxSize()) {
                    // Barra de control del WebView
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF0F5F5))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { mostrarBuscadorImagen = false }) {
                            Text("Cerrar", color = Color.Gray)
                        }
                        Text("Selecciona una imagen", fontWeight = FontWeight.Bold)
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
                                            coroutineScope.launch { snackbarHostState.showSnackbar("Imagen seleccionada") }
                                        } else {
                                            coroutineScope.launch { snackbarHostState.showSnackbar("Abre la imagen en grande primero") }
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6D00))
                        ) {
                            Text("SAVE", fontWeight = FontWeight.Bold)
                        }
                    }

                    // El Navegador incrustado
                    WebView(
                        state = webViewState,
                        navigator = navigator,
                        modifier = Modifier.fillMaxSize().weight(1f)
                    )
                }
            }
        }
    }

    // 2. Diálogo de Borrado Normal
    if (indiceParaBorrar != null) {
        AlertDialog(onDismissRequest = { indiceParaBorrar = null }, title = { Text("¿Eliminar $tituloDialogoBorrado?") }, text = { Text("¿Estás seguro de que quieres borrar este elemento? Se perderán todos los datos.") }, confirmButton = { TextButton(onClick = { indiceParaBorrar?.let { elementos.removeAt(it) }; indiceParaBorrar = null; validarDuplicadosGlobales() }) { Text("ELIMINAR", color = Color.Red, fontWeight = FontWeight.Bold) } }, dismissButton = { TextButton(onClick = { indiceParaBorrar = null }) { Text("CANCELAR", color = Color.Gray) } })
    }

    // 3. Diálogo Importar Bloque de Texto
    if (mostrarDialogoImportar) {
        AlertDialog(onDismissRequest = { mostrarDialogoImportar = false }, title = { Text("Importar Palabras") }, text = { Column { Text("Pega el texto con el formato:\n.NombreGrupo\nPalabra, Pista\nO palabras individuales sueltas.", fontSize = 14.sp, color = Color.Gray); Spacer(modifier = Modifier.height(8.dp)); OutlinedTextField(value = textoAImportar, onValueChange = { textoAImportar = it }, modifier = Modifier.fillMaxWidth().height(150.dp), placeholder = { Text("Pega aquí...") }) } }, confirmButton = { TextButton(onClick = { procesarTextoImportacion(textoAImportar) }) { Text("PROCESAR", color = Color(0xFF18C1A8), fontWeight = FontWeight.Bold) } }, dismissButton = { TextButton(onClick = { mostrarDialogoImportar = false }) { Text("CANCELAR", color = Color.Gray) } })
    }

    // 4. Diálogo de Resolución de Conflictos
    if (mostrarDialogoConflictos) {
        AlertDialog(onDismissRequest = { mostrarDialogoConflictos = false }, title = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.Warning, null, tint = Color(0xFFFF6D00)); Spacer(Modifier.width(8.dp)); Text("¡Palabras repetidas!") } }, text = { Text("Algunas de las palabras que intentas importar ya existen. ¿Qué deseas hacer?") }, confirmButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { val palabrasNuevasLowercase = elementosPendientesImportar.flatMap { if (it is ElementoUI.Individual) listOf(it.data.palabra.lowercase()) else (it as ElementoUI.Conjunto).palabras.map { p -> p.palabra.lowercase() } }.toSet(); elementos.forEach { el -> if (el is ElementoUI.Individual && palabrasNuevasLowercase.contains(el.data.palabra.lowercase())) { el.data.palabra = "" }; if (el is ElementoUI.Conjunto) { el.palabras.removeAll { palabrasNuevasLowercase.contains(it.palabra.lowercase()) } } }; elementos.removeAll { it is ElementoUI.Individual && it.data.palabra.isEmpty() }; aplicarYFusionarElementos(elementosPendientesImportar); validarDuplicadosGlobales(); mostrarDialogoConflictos = false; mostrarDialogoImportar = false; textoAImportar = "" }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF18C1A8))) { Text("Reemplazar antiguas") }
                Button(onClick = { val palabrasActuales = elementos.flatMap { if (it is ElementoUI.Individual) listOf(it.data.palabra.lowercase()) else (it as ElementoUI.Conjunto).palabras.map { p -> p.palabra.lowercase() } }.toSet(); val elementosLimpios = elementosPendientesImportar.mapNotNull { el -> when (el) { is ElementoUI.Individual -> if (palabrasActuales.contains(el.data.palabra.lowercase())) null else el; is ElementoUI.Conjunto -> { val palabrasFiltradas = el.palabras.filterNot { palabrasActuales.contains(it.palabra.lowercase()) }; if (palabrasFiltradas.isNotEmpty()) { val nuevoCol = ElementoUI.Conjunto(el.nombre).apply { palabras.addAll(palabrasFiltradas); expandido=true }; nuevoCol } else null } } }; aplicarYFusionarElementos(elementosLimpios); validarDuplicadosGlobales(); mostrarDialogoConflictos = false; mostrarDialogoImportar = false; textoAImportar = "" }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2C))) { Text("Omitir nuevas repetidas") }
                OutlinedButton(onClick = { aplicarYFusionarElementos(elementosPendientesImportar); validarDuplicadosGlobales(); mostrarDialogoConflictos = false; mostrarDialogoImportar = false; textoAImportar = "" }) { Text("Añadir todo y editar manual", color = Color.Gray) }
            }
        }, dismissButton = { TextButton(onClick = { mostrarDialogoConflictos = false }) { Text("CANCELAR IMPORTACIÓN") } })
    }
}