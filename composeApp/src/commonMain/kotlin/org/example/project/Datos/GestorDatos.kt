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
    val elementos: List<ElementoGuardado>,
    val idCreador: String? = null,
    val nombreCreador: String? = null,
    val likes: Int = 0,
    val usuariosLikes: List<String> = emptyList(), // 👇 NUEVO: Registra quién dio Like
    val esPublica: Boolean = false,
    val esDescargada: Boolean = false,
    // 👇 NUEVOS CAMPOS PARA COLABORACIÓN
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

    // 👇 Evita que la app se rompa al leer datos antiguos a los que les faltan campos
    private val jsonConfig = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    val coleccionesGlobales = mutableStateListOf<ColeccionGuardada>()
    val jugadoresGlobales = mutableStateListOf<String>()

    val palabrasUsadasSesion = mutableStateListOf<String>()

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
    }

    fun guardarCambiosMemoria() {
        val jsonColecciones = jsonConfig.encodeToString(coleccionesGlobales.toList())
        settings.putString(KEY_COLECCIONES, jsonColecciones)

        val jsonJugadores = jsonConfig.encodeToString(jugadoresGlobales.toList())
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

    fun limpiarDatosLocales() {
        coleccionesGlobales.clear()
        jugadoresGlobales.clear()
        guardarCambiosMemoria()
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
            }
            guardarCambiosMemoria()

            // 👇 NUEVO: Después de descargar tus datos, actualiza las listas de otros autores
            sincronizarColeccionesDescargadas(uid)
        } catch (e: Exception) { println("Error descarga nube: ${e.message}") }
    }

    suspend fun subirColeccionNube(uid: String, coleccion: ColeccionGuardada) {
        try {
            // 1. Guardado normal en tu propia nube
            GestorAuth.firestore
                .collection("usuarios")
                .document(uid)
                .collection("colecciones")
                .document(coleccion.nombre)
                .set(coleccion)

            // 👇 NUEVO: SINCRONIZACIÓN COLABORATIVA
            // 2. Si tú eres el invitado, actualízale la lista al creador original para que vea tus cambios
            if (coleccion.esColaboracion && coleccion.idCreador != null && coleccion.idCreador != uid) {
                val coleccionParaCreador = coleccion.copy(esColaboracion = false)
                GestorAuth.firestore
                    .collection("usuarios")
                    .document(coleccion.idCreador)
                    .collection("colecciones")
                    .document(coleccion.nombre)
                    .set(coleccionParaCreador)
            }

            // 3. Si tú eres el creador y tienes invitados, actualízales la lista a todos ellos
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

    // 👇 NUEVO: Motor de sincronización de listas descargadas
    suspend fun sincronizarColeccionesDescargadas(miUid: String) {
        var huboCambios = false
        // Hacemos una copia de la lista para iterar sin fallos de concurrencia
        val copiasLocales = coleccionesGlobales.toList()

        for (coleccion in copiasLocales) {
            // Solo comprobamos las que son online y sabemos quién las creó
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
                            // 1. Sigue existiendo y es pública: Actualizamos las palabras
                            val actualizada = coleccion.copy(
                                elementos = coleccionOriginal.elementos,
                                likes = coleccionOriginal.likes,
                                categoria = coleccionOriginal.categoria
                            )
                            if (coleccion != actualizada) {
                                val index = coleccionesGlobales.indexOfFirst { it.nombre == coleccion.nombre }
                                if (index != -1) coleccionesGlobales[index] = actualizada
                                subirColeccionNube(miUid, actualizada) // Guardamos el cambio en TU base de datos
                                huboCambios = true
                            }
                        } else {
                            // 2. El autor la puso Privada: Se elimina de tu biblioteca
                            coleccionesGlobales.remove(coleccion)
                            try { GestorAuth.firestore.collection("usuarios").document(miUid).collection("colecciones").document(coleccion.nombre).delete() } catch(e:Exception){}
                            huboCambios = true
                        }
                    } else {
                        // 3. El autor la borró por completo: Se elimina de tu biblioteca
                        coleccionesGlobales.remove(coleccion)
                        try { GestorAuth.firestore.collection("usuarios").document(miUid).collection("colecciones").document(coleccion.nombre).delete() } catch(e:Exception){}
                        huboCambios = true
                    }
                } catch (e: Exception) {
                    // Si falla la red, la dejamos como está hasta la próxima vez
                }
            }
        }

        if (huboCambios) {
            guardarCambiosMemoria()
        }
    }
}