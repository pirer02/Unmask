package org.example.project.Datos


import androidx.compose.runtime.mutableStateListOf
import com.russhwolf.settings.Settings
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import org.example.project.Datos.ListasPredeterminadas.ColeccionPredefinida
import org.example.project.Datos.ListasPredeterminadas.ElementoPredefinido

@Serializable
data class OpcionesJuego(
    val numImpostores: Int,
    val pistaParaImpostor: Boolean,
    val limiteRondas: Boolean,
    val rondas: Int,
    val limiteTiempo: Boolean,
    val tiempoMinutos: Int,
    val sinRepeticiones: Boolean = false
)

@Serializable
data class CheckpointJuego(
    val id: String = java.util.UUID.randomUUID().toString(),
    val nombre: String,
    val fecha: Long,
    val nombreColeccion: String,
    val idCreadorColeccion: String?,
    val opciones: OpcionesJuego,
    val palabrasUsadas: List<String>
)

@Serializable
data class ColeccionGuardada(
    val nombre: String,
    val categoria: String,
    val elementos: List<ElementoGuardado>,
    val idCreador: String? = null,
    val nombreCreador: String? = null,
    val likes: Int = 0,
    val usuariosLikes: List<String> = emptyList(),
    val esPublica: Boolean = false,
    val esDescargada: Boolean = false,
    val colaboradores: List<String> = emptyList(),
    val esColaboracion: Boolean = false
)

@Serializable
sealed class ElementoGuardado {
    @Serializable
    data class Individual(val palabra: String, val pista: String, val imagenUrl: String?) : ElementoGuardado()
    @Serializable
    data class Conjunto(val nombreConjunto: String, val palabras: List<Individual>) : ElementoGuardado()
}

object GestorDatos {
    private val settings = Settings()
    private const val KEY_COLECCIONES = "mis_colecciones_v1"
    private const val KEY_JUGADORES = "mis_jugadores_v1"
    private const val KEY_CHECKPOINTS = "mis_checkpoints_v1"
    private const val KEY_TUTORIAL = "paso_tutorial_v1"
    private const val KEY_IDIOMA = "idioma_preferido_v1" // 👇 NUEVO: Clave para el idioma

    private val jsonConfig = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    val coleccionesGlobales = mutableStateListOf<ColeccionGuardada>()
    val jugadoresGlobales = mutableStateListOf<String>()

    val checkpointsGlobales = mutableStateListOf<CheckpointJuego>()
    var checkpointActivoId: String? = null

    val palabrasUsadasSesion = mutableStateListOf<String>()

    var pasoTutorialActual by mutableStateOf(0)

    // 👇 NUEVAS FUNCIONES PARA EL IDIOMA
    fun cargarIdiomaGuardado(): String? {
        val idioma = settings.getString(KEY_IDIOMA, "")
        return if (idioma.isNotEmpty()) idioma else null
    }

    fun guardarIdiomaAjustes(codigo: String) {
        settings.putString(KEY_IDIOMA, codigo)
    }
    // ---------------------------------

    fun cargarDatos() {
        val jsonColecciones = settings.getString(KEY_COLECCIONES, "")
        if (jsonColecciones.isNotEmpty()) {
            try {
                val datos = jsonConfig.decodeFromString<List<ColeccionGuardada>>(jsonColecciones)
                coleccionesGlobales.clear()
                coleccionesGlobales.addAll(datos)
            } catch (e: Exception) { println("Error cargar colecciones: ${e.message}") }
        }

        val jsonJugadores = settings.getString(KEY_JUGADORES, "")
        if (jsonJugadores.isNotEmpty()) {
            try {
                val datosJugadores = jsonConfig.decodeFromString<List<String>>(jsonJugadores)
                jugadoresGlobales.clear()
                jugadoresGlobales.addAll(datosJugadores)
            } catch (e: Exception) { println("Error cargar jugadores: ${e.message}") }
        }

        val jsonCheckpoints = settings.getString(KEY_CHECKPOINTS, "")
        if (jsonCheckpoints.isNotEmpty()) {
            try {
                val datosCheckpoints = jsonConfig.decodeFromString<List<CheckpointJuego>>(jsonCheckpoints)
                checkpointsGlobales.clear()
                checkpointsGlobales.addAll(datosCheckpoints)
            } catch (e: Exception) { println("Error cargar checkpoints: ${e.message}") }
        }

        pasoTutorialActual = settings.getInt(KEY_TUTORIAL, 0)
    }

    fun guardarCambiosMemoria() {
        val jsonColecciones = jsonConfig.encodeToString(coleccionesGlobales.toList())
        settings.putString(KEY_COLECCIONES, jsonColecciones)

        val jsonJugadores = jsonConfig.encodeToString(jugadoresGlobales.toList())
        settings.putString(KEY_JUGADORES, jsonJugadores)

        val jsonCheckpoints = jsonConfig.encodeToString(checkpointsGlobales.toList())
        settings.putString(KEY_CHECKPOINTS, jsonCheckpoints)
    }

    fun avanzarTutorial(nuevoPaso: Int) {
        pasoTutorialActual = nuevoPaso
        settings.putInt(KEY_TUTORIAL, nuevoPaso)
    }

    fun terminarTutorial() {
        pasoTutorialActual = 99
        settings.putInt(KEY_TUTORIAL, 99)
    }

    fun guardarNuevaColeccion(coleccion: ColeccionGuardada) {
        coleccionesGlobales.add(coleccion)
        guardarCambiosMemoria()
    }

    fun actualizarColeccion(nombreOriginal: String, coleccionActualizada: ColeccionGuardada) {
        val index = coleccionesGlobales.indexOfFirst { it.nombre == nombreOriginal }
        if (index != -1) {
            coleccionesGlobales[index] = coleccionActualizada
            guardarCambiosMemoria()
        }
    }

    fun eliminarColeccion(coleccion: ColeccionGuardada) {
        coleccionesGlobales.remove(coleccion)
        guardarCambiosMemoria()
    }

    fun limpiarDatosLocales() {
        coleccionesGlobales.clear()
        jugadoresGlobales.clear()
        checkpointsGlobales.clear()
        checkpointActivoId = null
        guardarCambiosMemoria()
    }

    suspend fun subirCheckpointsNube(uid: String) {
        try {
            GestorAuth.firestore.collection("usuarios").document(uid)
                .update(mapOf("checkpoints" to checkpointsGlobales.toList()))
        } catch (e: Exception) {
            GestorAuth.firestore.collection("usuarios").document(uid)
                .set(mapOf("checkpoints" to checkpointsGlobales.toList()), merge = true)
        }
    }

    suspend fun descargarDatosNube(uid: String) {
        try {
            val snapshot = GestorAuth.firestore
                .collection("usuarios")
                .document(uid)
                .collection("colecciones")
                .get()

            val coleccionesNube = snapshot.documents.map { it.data<ColeccionGuardada>() }
            coleccionesGlobales.clear()
            coleccionesGlobales.addAll(coleccionesNube)

            val docUsuario = GestorAuth.firestore.collection("usuarios").document(uid).get()
            if (docUsuario.exists) {
                try {
                    val jugadoresNube = docUsuario.get<List<String>>("jugadores")
                    jugadoresGlobales.clear()
                    jugadoresGlobales.addAll(jugadoresNube)
                } catch (e: Exception) { }

                try {
                    val checkpointsNube = docUsuario.get<List<CheckpointJuego>>("checkpoints")
                    checkpointsGlobales.clear()
                    checkpointsGlobales.addAll(checkpointsNube)
                } catch (e: Exception) { }
            }
            guardarCambiosMemoria()

            sincronizarColeccionesDescargadas(uid)
        } catch (e: Exception) { println("Error descarga nube: ${e.message}") }
    }

    suspend fun subirColeccionNube(uid: String, coleccion: ColeccionGuardada) {
        try {
            GestorAuth.firestore
                .collection("usuarios")
                .document(uid)
                .collection("colecciones")
                .document(coleccion.nombre)
                .set(coleccion)

            if (coleccion.esColaboracion && coleccion.idCreador != null && coleccion.idCreador != uid) {
                val coleccionParaCreador = coleccion.copy(esColaboracion = false)
                GestorAuth.firestore
                    .collection("usuarios")
                    .document(coleccion.idCreador)
                    .collection("colecciones")
                    .document(coleccion.nombre)
                    .set(coleccionParaCreador)
            }

            if (!coleccion.esColaboracion && coleccion.colaboradores.isNotEmpty()) {
                val coleccionParaInvitado = coleccion.copy(esColaboracion = true)
                coleccion.colaboradores.forEach { colabUid ->
                    GestorAuth.firestore
                        .collection("usuarios")
                        .document(colabUid)
                        .collection("colecciones")
                        .document(coleccion.nombre)
                        .set(coleccionParaInvitado)
                }
            }
        } catch (e: Exception) { println("Error subida lista: ${e.message}") }
    }

    suspend fun subirJugadoresNube(uid: String) {
        try {
            GestorAuth.firestore.collection("usuarios").document(uid)
                .update(mapOf("jugadores" to jugadoresGlobales.toList()))
        } catch (e: Exception) {
            GestorAuth.firestore.collection("usuarios").document(uid)
                .set(mapOf("jugadores" to jugadoresGlobales.toList()), merge = true)
        }
    }

    fun convertirPredefinidaAGuardada(pred: ColeccionPredefinida): ColeccionGuardada {
        return ColeccionGuardada(
            nombre = pred.nombre,
            categoria = pred.categoria,
            elementos = pred.elementos.map { elemPre ->
                when (elemPre) {
                    is ElementoPredefinido.Individual -> ElementoGuardado.Individual(elemPre.palabra, elemPre.pista, null)
                    is ElementoPredefinido.Conjunto -> ElementoGuardado.Conjunto(
                        nombreConjunto = elemPre.nombreConjunto,
                        palabras = elemPre.palabras.map { ElementoGuardado.Individual(it.palabra, it.pista, null) }
                    )
                }
            },
            nombreCreador = "Unmask Oficial",
            esPublica = true
        )
    }

    suspend fun sincronizarColeccionesDescargadas(miUid: String) {
        var huboCambios = false
        val copiasLocales = coleccionesGlobales.toList()

        for (coleccion in copiasLocales) {
            if (coleccion.esDescargada && coleccion.idCreador != null) {
                try {
                    val doc = GestorAuth.firestore
                        .collection("usuarios")
                        .document(coleccion.idCreador)
                        .collection("colecciones")
                        .document(coleccion.nombre)
                        .get()

                    if (doc.exists) {
                        val coleccionOriginal = doc.data<ColeccionGuardada>()

                        if (coleccionOriginal.esPublica) {
                            val actualizada = coleccion.copy(
                                elementos = coleccionOriginal.elementos,
                                likes = coleccionOriginal.likes,
                                categoria = coleccionOriginal.categoria
                            )
                            if (coleccion != actualizada) {
                                val index = coleccionesGlobales.indexOfFirst { it.nombre == coleccion.nombre }
                                if (index != -1) coleccionesGlobales[index] = actualizada
                                subirColeccionNube(miUid, actualizada)
                                huboCambios = true
                            }
                        } else {
                            coleccionesGlobales.remove(coleccion)
                            try { GestorAuth.firestore.collection("usuarios").document(miUid).collection("colecciones").document(coleccion.nombre).delete() } catch(e:Exception){}
                            huboCambios = true
                        }
                    } else {
                        coleccionesGlobales.remove(coleccion)
                        try { GestorAuth.firestore.collection("usuarios").document(miUid).collection("colecciones").document(coleccion.nombre).delete() } catch(e:Exception){}
                        huboCambios = true
                    }
                } catch (e: Exception) {
                }
            }
        }

        if (huboCambios) {
            guardarCambiosMemoria()
        }
    }

    suspend fun eliminarColaborador(miUid: String, coleccion: ColeccionGuardada, colabUid: String) {
        try {
            GestorAuth.firestore
                .collection("usuarios")
                .document(colabUid)
                .collection("colecciones")
                .document(coleccion.nombre)
                .delete()
        } catch (e: Exception) { println("Error eliminando lista del colaborador: ${e.message}") }

        val nuevosColaboradores = coleccion.colaboradores.filter { it != colabUid }
        val listaActualizada = coleccion.copy(colaboradores = nuevosColaboradores)

        val index = coleccionesGlobales.indexOfFirst { it.nombre == coleccion.nombre && it.idCreador == miUid }
        if (index != -1) {
            coleccionesGlobales[index] = listaActualizada
            guardarCambiosMemoria()
        }

        subirColeccionNube(miUid, listaActualizada)
    }

    suspend fun abandonarColaboracion(miUid: String, coleccion: ColeccionGuardada) {
        val index = coleccionesGlobales.indexOfFirst { it.nombre == coleccion.nombre && it.idCreador == coleccion.idCreador }
        if (index != -1) {
            coleccionesGlobales.removeAt(index)
            guardarCambiosMemoria()
        }

        try {
            GestorAuth.firestore
                .collection("usuarios")
                .document(miUid)
                .collection("colecciones")
                .document(coleccion.nombre)
                .delete()
        } catch (e: Exception) { println("Error borrando copia local: ${e.message}") }

        coleccion.idCreador?.let { creadorUid ->
            try {
                val docRef = GestorAuth.firestore
                    .collection("usuarios")
                    .document(creadorUid)
                    .collection("colecciones")
                    .document(coleccion.nombre)

                val doc = docRef.get()
                if (doc.exists) {
                    val coleccionOriginal = doc.data<ColeccionGuardada>()
                    val nuevosColaboradores = coleccionOriginal.colaboradores.filter { it != miUid }
                    docRef.update(mapOf("colaboradores" to nuevosColaboradores))
                }
            } catch (e: Exception) { println("Error actualizando al creador: ${e.message}") }
        }
    }
}