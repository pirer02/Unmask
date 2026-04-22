import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.example.project.Datos.*

// --- PEQUEÑA HERRAMIENTA PARA MAYÚSCULAS ---
fun String.capitalizarPrimeraCrear(): String {
    if (this.isBlank()) return this
    return this.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

// --- MODELOS DE ESTADO ---
class PalabraUI(palabraIni: String = "", pistaIni: String = "") {
    var palabra by mutableStateOf(palabraIni)
    var pista by mutableStateOf(pistaIni)
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

    // --- Control del Diálogo de Borrado ---
    var indiceParaBorrar by remember { mutableStateOf<Int?>(null) }
    var tituloDialogoBorrado by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        if (coleccionParaEditar != null) {
            nombreColeccion = coleccionParaEditar.nombre
            categoriaColeccion = coleccionParaEditar.categoria

            coleccionParaEditar.elementos.forEach { el ->
                when (el) {
                    is ElementoGuardado.Individual -> elementos.add(ElementoUI.Individual(PalabraUI(el.palabra, el.pista)))
                    is ElementoGuardado.Conjunto -> {
                        val nuevoConjunto = ElementoUI.Conjunto(el.nombreConjunto)
                        el.palabras.forEach { p -> nuevoConjunto.palabras.add(PalabraUI(p.palabra, p.pista)) }
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

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFE8F0FE))) {

        // CABECERA CON BOTÓN GUARDAR
        Row(modifier = Modifier.fillMaxWidth().background(Color.White).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onVolver) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Volver") }
            Text(if (coleccionParaEditar != null) "Editar Lista" else "Nueva Lista", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = {
                    var hayError = false
                    var primerIndiceError = -1
                    var mensajeSnackbar = "Faltan campos por rellenar"

                    // 1. LIMPIEZA DE ERRORES PREVIOS
                    errorNombreCol = false
                    errorCategoriaCol = false
                    elementos.forEach { el ->
                        when(el) {
                            is ElementoUI.Individual -> { el.data.errorPalabra = false; el.data.errorPista = false }
                            is ElementoUI.Conjunto -> {
                                el.errorNombre = false
                                el.palabras.forEach { p -> p.errorPalabra = false; p.errorPista = false }
                            }
                        }
                    }

                    // 2. VALIDAR NOMBRE DE COLECCIÓN
                    val nombreLimpio = nombreColeccion.trim()
                    if (nombreLimpio.isBlank()) {
                        errorNombreCol = true; mensajeErrorNombreCol = "Obligatorio"
                        hayError = true; if (primerIndiceError == -1) primerIndiceError = 0
                    } else {
                        val existeOtra = GestorDatos.coleccionesGlobales.any {
                            it.nombre.equals(nombreLimpio, ignoreCase = true) && it.nombre != coleccionParaEditar?.nombre
                        }
                        if (existeOtra) {
                            errorNombreCol = true; mensajeErrorNombreCol = "¡Ya existe una lista con este nombre!"
                            hayError = true; mensajeSnackbar = "Corrige los errores marcados"; if (primerIndiceError == -1) primerIndiceError = 0
                        }
                    }

                    if (categoriaColeccion.isBlank()) {
                        errorCategoriaCol = true; mensajeErrorCategoriaCol = "Obligatorio"
                        hayError = true; if (primerIndiceError == -1) primerIndiceError = 0
                    }

                    // 3. VALIDAR PALABRAS Y PISTAS
                    val palabrasUnicas = mutableSetOf<String>()
                    val pistasUnicas = mutableSetOf<String>()

                    elementos.forEachIndexed { index, el ->
                        val indiceRealEnLista = index + 1

                        when (el) {
                            is ElementoUI.Individual -> {
                                val pLimpia = el.data.palabra.trim().lowercase()
                                val cLimpia = el.data.pista.trim().lowercase()

                                if (pLimpia.isBlank()) {
                                    el.data.errorPalabra = true; el.data.mensajeErrorPalabra = "Obligatorio"
                                    hayError = true; if (primerIndiceError == -1) primerIndiceError = indiceRealEnLista
                                } else if (!palabrasUnicas.add(pLimpia)) {
                                    el.data.errorPalabra = true; el.data.mensajeErrorPalabra = "¡Palabra repetida!"
                                    hayError = true; mensajeSnackbar = "Tienes palabras repetidas"; if (primerIndiceError == -1) primerIndiceError = indiceRealEnLista
                                }

                                if (cLimpia.isBlank()) {
                                    el.data.errorPista = true; el.data.mensajeErrorPista = "Obligatorio"
                                    hayError = true; if (primerIndiceError == -1) primerIndiceError = indiceRealEnLista
                                } else if (!pistasUnicas.add(cLimpia)) {
                                    el.data.errorPista = true; el.data.mensajeErrorPista = "¡Pista repetida!"
                                    hayError = true; mensajeSnackbar = "Tienes pistas repetidas"; if (primerIndiceError == -1) primerIndiceError = indiceRealEnLista
                                }
                            }
                            is ElementoUI.Conjunto -> {
                                var errorEnEsteConjunto = false

                                if (el.nombre.isBlank()) {
                                    el.errorNombre = true; el.mensajeErrorNombre = "Obligatorio"
                                    hayError = true; errorEnEsteConjunto = true
                                }

                                el.palabras.forEach { p ->
                                    val pLimpia = p.palabra.trim().lowercase()
                                    val cLimpia = p.pista.trim().lowercase()

                                    if (pLimpia.isBlank()) {
                                        p.errorPalabra = true; p.mensajeErrorPalabra = "Obligatorio"
                                        hayError = true; errorEnEsteConjunto = true
                                    } else if (!palabrasUnicas.add(pLimpia)) {
                                        p.errorPalabra = true; p.mensajeErrorPalabra = "¡Palabra repetida!"
                                        hayError = true; errorEnEsteConjunto = true; mensajeSnackbar = "Tienes palabras repetidas"
                                    }

                                    if (cLimpia.isBlank()) {
                                        p.errorPista = true; p.mensajeErrorPista = "Obligatorio"
                                        hayError = true; errorEnEsteConjunto = true
                                    } else if (!pistasUnicas.add(cLimpia)) {
                                        p.errorPista = true; p.mensajeErrorPista = "¡Pista repetida!"
                                        hayError = true; errorEnEsteConjunto = true; mensajeSnackbar = "Tienes pistas repetidas"
                                    }
                                }

                                if (errorEnEsteConjunto) {
                                    el.expandido = true
                                    if (primerIndiceError == -1) primerIndiceError = indiceRealEnLista
                                }
                            }
                        }
                    }

                    if (hayError) {
                        coroutineScope.launch {
                            listState.animateScrollToItem(primerIndiceError)
                            snackbarHostState.showSnackbar(mensajeSnackbar)
                        }
                    } else {
                        val elementosGuardables = elementos.map { ui ->
                            when (ui) {
                                is ElementoUI.Individual -> ElementoGuardado.Individual(
                                    palabra = ui.data.palabra.capitalizarPrimeraCrear(),
                                    pista = ui.data.pista.capitalizarPrimeraCrear(),
                                    imagenUrl = null
                                )
                                is ElementoUI.Conjunto -> ElementoGuardado.Conjunto(
                                    nombreConjunto = ui.nombre.capitalizarPrimeraCrear(),
                                    palabras = ui.palabras.map { p ->
                                        ElementoGuardado.Individual(
                                            palabra = p.palabra.capitalizarPrimeraCrear(),
                                            pista = p.pista.capitalizarPrimeraCrear(),
                                            imagenUrl = null
                                        )
                                    }
                                )
                            }
                        }

                        val nuevaLista = ColeccionGuardada(
                            nombre = nombreColeccion.capitalizarPrimeraCrear(),
                            categoria = categoriaColeccion.capitalizarPrimeraCrear(),
                            elementos = elementosGuardables
                        )

                        // Guardado Local
                        if (coleccionParaEditar != null) {
                            GestorDatos.actualizarColeccion(coleccionParaEditar.nombre, nuevaLista)
                        } else {
                            GestorDatos.guardarNuevaColeccion(nuevaLista)
                        }

                        // 👇 AHORA: Guardado en la Nube (Firebase)
                        val usuarioLogueado = GestorAuth.usuario.value
                        if (usuarioLogueado != null) {
                            coroutineScope.launch {
                                GestorDatos.subirColeccionNube(usuarioLogueado.uid, nuevaLista)
                            }
                        }

                        onGuardadoExitoso()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF18C1A8))
            ) { Text("GUARDAR", fontWeight = FontWeight.Bold) }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = nombreColeccion, onValueChange = { nombreColeccion = it },
                            label = { Text("Nombre de la Lista") },
                            keyboardOptions = opcionesTeclado,
                            isError = errorNombreCol,
                            supportingText = { if (errorNombreCol) Text(mensajeErrorNombreCol, color = MaterialTheme.colorScheme.error) },
                            modifier = Modifier.fillMaxWidth(), singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = categoriaColeccion, onValueChange = { categoriaColeccion = it },
                            label = { Text("Categoría (Ej: Películas)") },
                            keyboardOptions = opcionesTeclado,
                            isError = errorCategoriaCol,
                            supportingText = { if (errorCategoriaCol) Text(mensajeErrorCategoriaCol, color = MaterialTheme.colorScheme.error) },
                            modifier = Modifier.fillMaxWidth(), singleLine = true
                        )
                    }
                }
            }

            itemsIndexed(elementos) { index, elemento ->
                when (elemento) {

                    is ElementoUI.Individual -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0x3318C1A8)),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Palabra Individual", color = Color(0xFF18C1A8), fontWeight = FontWeight.Bold)
                                    IconButton(
                                        onClick = {
                                            tituloDialogoBorrado = "Palabra Individual"
                                            indiceParaBorrar = index
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Rounded.Close, contentDescription = "Borrar", tint = Color.Red)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = elemento.data.palabra, onValueChange = { elemento.data.palabra = it },
                                    label = { Text("Palabra Secreta") }, keyboardOptions = opcionesTeclado,
                                    isError = elemento.data.errorPalabra,
                                    supportingText = { if(elemento.data.errorPalabra) Text(elemento.data.mensajeErrorPalabra, color = MaterialTheme.colorScheme.error) },
                                    modifier = Modifier.fillMaxWidth(), singleLine = true
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = elemento.data.pista, onValueChange = { elemento.data.pista = it },
                                    label = { Text("Pista para el Impostor") }, keyboardOptions = opcionesTeclado,
                                    isError = elemento.data.errorPista,
                                    supportingText = { if(elemento.data.errorPista) Text(elemento.data.mensajeErrorPista, color = MaterialTheme.colorScheme.error) },
                                    modifier = Modifier.fillMaxWidth(), singleLine = true
                                )
                            }
                        }
                    }

                    is ElementoUI.Conjunto -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2C)),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        if (!elemento.expandido) {
                                            elemento.expandido = true
                                            elemento.cargando = true
                                        } else {
                                            elemento.expandido = false
                                        }
                                    }.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (elemento.expandido) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                        contentDescription = null, tint = Color(0xFFFF6D00)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (elemento.nombre.isBlank()) "Grupo de palabras..." else elemento.nombre,
                                        color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)
                                    )

                                    if (!elemento.expandido && (elemento.errorNombre || elemento.palabras.any { it.errorPalabra || it.errorPista })) {
                                        Icon(Icons.Rounded.Warning, contentDescription = "Error", tint = Color.Red)
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }

                                    IconButton(
                                        onClick = {
                                            tituloDialogoBorrado = "Grupo Completo"
                                            indiceParaBorrar = index
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Rounded.Delete, contentDescription = "Borrar conjunto", tint = Color.Red)
                                    }
                                }

                                if (elemento.expandido) {
                                    Column(modifier = Modifier.fillMaxWidth().background(Color(0xFFF0F5F5)).padding(16.dp)) {

                                        if (elemento.cargando) {
                                            var progresoAnimado by remember { mutableFloatStateOf(0f) }

                                            LaunchedEffect(Unit) {
                                                Animatable(0f).animateTo(
                                                    targetValue = 1f,
                                                    animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing)
                                                ) {
                                                    progresoAnimado = value
                                                }
                                                elemento.cargando = false
                                            }

                                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth(0.8f)) {
                                                    LinearProgressIndicator(
                                                        progress = progresoAnimado,
                                                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                                        color = Color(0xFFFF6D00),
                                                        trackColor = Color(0xFFFFD8C2)
                                                    )
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Text(
                                                        text = "Cargando... ${(progresoAnimado * 100).toInt()}%",
                                                        color = Color.Gray,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp
                                                    )
                                                }
                                            }
                                        } else {
                                            OutlinedTextField(
                                                value = elemento.nombre, onValueChange = { elemento.nombre = it },
                                                label = { Text("Nombre de este Grupo") }, keyboardOptions = opcionesTeclado,
                                                isError = elemento.errorNombre,
                                                supportingText = { if (elemento.errorNombre) Text(elemento.mensajeErrorNombre, color = MaterialTheme.colorScheme.error) },
                                                modifier = Modifier.fillMaxWidth(), singleLine = true
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))

                                            elemento.palabras.forEachIndexed { i, p ->
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text("${i + 1}.", fontWeight = FontWeight.Bold, color = Color.Gray)
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        OutlinedTextField(
                                                            value = p.palabra, onValueChange = { p.palabra = it },
                                                            placeholder = { Text("Palabra") }, keyboardOptions = opcionesTeclado,
                                                            isError = p.errorPalabra,
                                                            supportingText = { if(p.errorPalabra) Text(p.mensajeErrorPalabra, color = MaterialTheme.colorScheme.error) },
                                                            modifier = Modifier.fillMaxWidth(), singleLine = true
                                                        )
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        OutlinedTextField(
                                                            value = p.pista, onValueChange = { p.pista = it },
                                                            placeholder = { Text("Pista") }, keyboardOptions = opcionesTeclado,
                                                            isError = p.errorPista,
                                                            supportingText = { if(p.errorPista) Text(p.mensajeErrorPista, color = MaterialTheme.colorScheme.error) },
                                                            modifier = Modifier.fillMaxWidth(), singleLine = true
                                                        )
                                                    }
                                                    IconButton(onClick = { elemento.palabras.removeAt(i) }) {
                                                        Icon(Icons.Rounded.Close, contentDescription = "Borrar", tint = Color.Gray)
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(12.dp))
                                            }

                                            TextButton(onClick = { elemento.palabras.add(PalabraUI()) }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                                                Icon(Icons.Rounded.Add, contentDescription = null, tint = Color(0xFFFF6D00))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("AÑADIR PALABRA AL GRUPO", color = Color(0xFFFF6D00), fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedButton(
                        onClick = { elementos.add(ElementoUI.Individual()) },
                        modifier = Modifier.weight(1f).height(60.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF18C1A8)),
                        border = BorderStroke(2.dp, Color(0xFF18C1A8))
                    ) {
                        Text("+ PALABRA", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val nuevoConjunto = ElementoUI.Conjunto()
                            nuevoConjunto.palabras.add(PalabraUI())
                            nuevoConjunto.expandido = true
                            elementos.add(nuevoConjunto)
                        },
                        modifier = Modifier.weight(1f).height(60.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2C))
                    ) {
                        Text("+ GRUPO", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(64.dp))
            }
        }
    }

    // DIÁLOGO DE CONFIRMACIÓN PARA BORRAR ELEMENTOS
    if (indiceParaBorrar != null) {
        AlertDialog(
            onDismissRequest = { indiceParaBorrar = null },
            title = { Text("¿Eliminar $tituloDialogoBorrado?") },
            text = { Text("¿Estás seguro de que quieres borrar este elemento? Se perderán todos los datos que hayas escrito en su interior.") },
            confirmButton = {
                TextButton(onClick = {
                    indiceParaBorrar?.let { elementos.removeAt(it) }
                    indiceParaBorrar = null
                }) {
                    Text("ELIMINAR", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { indiceParaBorrar = null }) {
                    Text("CANCELAR", color = Color.Gray)
                }
            }
        )
    }
}