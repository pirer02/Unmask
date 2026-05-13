package org.example.project.Datos

import ColeccionPredefinida
import ElementoPredefinido
import androidx.compose.runtime.mutableStateListOf
import com.russhwolf.settings.Settings
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.example.project.Datos.GestorAuth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf

// 👇 NUEVO: Movido aquí desde App.kt para poder guardarlo
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

// 👇 NUEVO: Modelo del Checkpoint
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

// 1. MODELOS DE DATOS
@Serializable
data class ColeccionGuardada(
    val nombre: String,
    val categoria: String,
    val elementos: List<ElementoGuardado>,
    val idCreador: String? = null,
    val nombreCreador: String? = null,
    val likes: Int = 0,
    val usuariosLikes: List<String> = emptyList(), // Registra quién dio Like
    val esPublica: Boolean = false,
    val esDescargada: Boolean = false,
    // CAMPOS PARA COLABORACIÓN
    val colaboradores: List<String> = emptyList(), // UIDs de los usuarios invitados
    val esColaboracion: Boolean = false // True si esta lista es de un amigo que te invitó
)

@Serializable
sealed class ElementoGuardado {
    @Serializable
    data class Individual(val palabra: String, val pista: String, val imagenUrl: String?) : ElementoGuardado()
    @Serializable
    data class Conjunto(val nombreConjunto: String, val palabras: List<Individual>) : ElementoGuardado()
}

// 2. EL GESTOR DE DATOS
object GestorDatos {
    private val settings = Settings()
    private const val KEY_COLECCIONES = "mis_colecciones_v1"
    private const val KEY_JUGADORES = "mis_jugadores_v1"
    private const val KEY_CHECKPOINTS = "mis_checkpoints_v1"
    private const val KEY_TUTORIAL = "paso_tutorial_v1" // 👇 NUEVO

    // Evita que la app se rompa al leer datos antiguos a los que les faltan campos
    private val jsonConfig = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    val coleccionesGlobales = mutableStateListOf<ColeccionGuardada>()
    val jugadoresGlobales = mutableStateListOf<String>()

    val checkpointsGlobales = mutableStateListOf<CheckpointJuego>()
    var checkpointActivoId: String? = null

    val palabrasUsadasSesion = mutableStateListOf<String>()

    // 👇 NUEVO: Estado global del tutorial interactivo (0 = inicio, 99 = terminado)
    var pasoTutorialActual by mutableStateOf(0)

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

        // 👇 NUEVO: Cargar en qué paso se quedó el usuario
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

    // 👇 NUEVO: Funciones para controlar el avance del tutorial
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
        checkpointsGlobales.clear() // 👇 AÑADIDO
        checkpointActivoId = null     // 👇 AÑADIDO
        guardarCambiosMemoria()
    }

    suspend fun subirCheckpointsNube(uid: String) {
        try {
            GestorAuth.firestore.collection("usuarios").document(uid)
                .update(mapOf("checkpoints" to checkpointsGlobales.toList()))
        } catch (e: Exception) {
            // Si el campo no existe todavía, usamos set con merge
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

                // 👇 NUEVO: Descargar checkpoints
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