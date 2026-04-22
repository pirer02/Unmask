package org.example.project.Datos

import ColeccionPredefinida
import ElementoPredefinido
import androidx.compose.runtime.mutableStateListOf
import com.russhwolf.settings.Settings
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.example.project.Datos.GestorAuth

// 1. MODELOS DE DATOS
@Serializable
data class ColeccionGuardada(
    val nombre: String,
    val categoria: String,
    val elementos: List<ElementoGuardado>
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

    val coleccionesGlobales = mutableStateListOf<ColeccionGuardada>()
    val jugadoresGlobales = mutableStateListOf<String>()

    fun cargarDatos() {
        // Cargar Colecciones
        val jsonColecciones = settings.getString(KEY_COLECCIONES, "")
        if (jsonColecciones.isNotEmpty()) {
            try {
                val datos = Json.decodeFromString<List<ColeccionGuardada>>(jsonColecciones)
                coleccionesGlobales.clear()
                coleccionesGlobales.addAll(datos)
            } catch (e: Exception) { println("Error cargar colecciones: ${e.message}") }
        }

        // Cargar Jugadores
        val jsonJugadores = settings.getString(KEY_JUGADORES, "")
        if (jsonJugadores.isNotEmpty()) {
            try {
                val datosJugadores = Json.decodeFromString<List<String>>(jsonJugadores)
                jugadoresGlobales.clear()
                jugadoresGlobales.addAll(datosJugadores)
            } catch (e: Exception) { println("Error cargar jugadores: ${e.message}") }
        }
    }

    fun guardarCambiosMemoria() {
        val jsonColecciones = Json.encodeToString(coleccionesGlobales.toList())
        settings.putString(KEY_COLECCIONES, jsonColecciones)

        val jsonJugadores = Json.encodeToString(jugadoresGlobales.toList())
        settings.putString(KEY_JUGADORES, jsonJugadores)
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

    // --- FUNCIONES DE FIREBASE ---

    fun limpiarDatosLocales() {
        coleccionesGlobales.clear()
        jugadoresGlobales.clear()
        guardarCambiosMemoria()
    }

    suspend fun descargarDatosNube(uid: String) {
        try {
            // A. Descargar Colecciones
            val snapshot = GestorAuth.firestore
                .collection("usuarios")
                .document(uid)
                .collection("colecciones")
                .get()

            val coleccionesNube = snapshot.documents.map { it.data<ColeccionGuardada>() }
            coleccionesGlobales.clear()
            coleccionesGlobales.addAll(coleccionesNube)

            // B. Descargar Jugadores
            val docUsuario = GestorAuth.firestore.collection("usuarios").document(uid).get()
            if (docUsuario.exists) {
                try {
                    val jugadoresNube = docUsuario.get<List<String>>("jugadores")
                    jugadoresGlobales.clear()
                    jugadoresGlobales.addAll(jugadoresNube)
                } catch (e: Exception) { /* No hay jugadores aún */ }
            }
            guardarCambiosMemoria()
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
        } catch (e: Exception) { println("Error subida lista: ${e.message}") }
    }

    suspend fun subirJugadoresNube(uid: String) {
        try {
            GestorAuth.firestore.collection("usuarios").document(uid)
                .update(mapOf("jugadores" to jugadoresGlobales.toList()))
        } catch (e: Exception) {
            // Si el documento no existe, usamos set
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
            }
        )
    }
}