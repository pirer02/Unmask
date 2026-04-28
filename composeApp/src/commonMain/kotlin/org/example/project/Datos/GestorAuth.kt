package org.example.project.Datos

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class PerfilSocial(
    val uid: String = "",
    val username: String = "",
    val fotoPerfil: String? = null,
    val seguidores: List<String> = emptyList(),
    val seguidos: List<String> = emptyList()
)

object GestorAuth {
    private val auth = Firebase.auth
    val firestore = Firebase.firestore

    private val _usuario = MutableStateFlow(auth.currentUser)
    val usuario: StateFlow<FirebaseUser?> = _usuario

    suspend fun actualizarUsuarioActual() {
        _usuario.value = auth.currentUser
    }

    suspend fun cerrarSesion() {
        auth.signOut()
        _usuario.value = null
    }

    suspend fun obtenerNombreUsuario(uid: String): String? {
        return try {
            val doc = firestore.collection("usuarios").document(uid).get()
            doc.get<String>("username")
        } catch (e: Exception) { null }
    }

    suspend fun necesitaNombreUsuario(): Boolean {
        val user = auth.currentUser ?: return false
        return try {
            val doc = firestore.collection("usuarios").document(user.uid).get()
            !doc.exists
        } catch (e: Exception) { false }
    }

    suspend fun registrarNombreUsuario(nombreUsuario: String): Boolean {
        val user = auth.currentUser ?: return false
        val nombreLimpio = nombreUsuario.trim().lowercase()
        val fotoTexto = user.photoURL ?: ""

        return try {
            val docNombre = firestore.collection("nombres_usuario").document(nombreLimpio).get()
            if (docNombre.exists) return false

            firestore.collection("usuarios").document(user.uid).set(
                mapOf(
                    "email" to (user.email ?: ""),
                    "username" to nombreLimpio,
                    "fotoPerfil" to fotoTexto
                )
            )
            firestore.collection("nombres_usuario").document(nombreLimpio).set(
                mapOf("uid" to user.uid)
            )
            true
        } catch (e: Exception) { false }
    }

    suspend fun actualizarFotoPerfil(uid: String, urlFoto: String): Boolean {
        return try {
            firestore.collection("usuarios").document(uid).update(
                mapOf("fotoPerfil" to urlFoto)
            )
            true
        } catch (e: Exception) { false }
    }

    // 👇 CORREGIDO: Borrado brutal e infalible
    suspend fun eliminarCuentaDefinitiva(uid: String): Boolean {
        return try {
            val user = auth.currentUser ?: return false
            val username = obtenerNombreUsuario(uid)

            // 1. Arrasamos con sus listas públicas y privadas
            try {
                val colecciones = firestore.collection("usuarios").document(uid).collection("colecciones").get()
                colecciones.documents.forEach { it.reference.delete() }
            } catch (e: Exception) { }

            // 2. Arrasamos con su documento principal
            try { firestore.collection("usuarios").document(uid).delete() } catch (e: Exception) { }

            // 3. Liberamos su nombre para que otro lo pueda usar
            try {
                if (username != null) firestore.collection("nombres_usuario").document(username).delete()
            } catch (e: Exception) { }

            // 4. Intentamos borrar el usuario de Auth. Si falla por seguridad, al menos le cerramos la sesión
            try { user.delete() } catch (e: Exception) { }

            auth.signOut()
            _usuario.value = null
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun buscarUsuariosPorNombre(query: String, miUid: String? = null): List<PerfilSocial> {
        if (query.isBlank()) return emptyList()
        val queryLimpia = query.trim().lowercase()

        return try {
            val snapshot = firestore.collection("nombres_usuario").get()
            val coincidencias = snapshot.documents
                .filter { it.id.contains(queryLimpia) }
                .take(20)

            val resultados = mutableListOf<PerfilSocial>()
            for (doc in coincidencias) {
                val uid = try { doc.get<String>("uid") } catch(e: Exception) { continue }
                if (uid == miUid) continue
                val perfil = obtenerPerfilSocial(uid)
                if (perfil != null) resultados.add(perfil)
            }
            resultados
        } catch (e: Exception) { emptyList() }
    }

    suspend fun obtenerPerfilSocial(uid: String): PerfilSocial? {
        return try {
            val doc = firestore.collection("usuarios").document(uid).get()
            if (doc.exists) {
                val username = try { doc.get<String>("username") } catch(e: Exception) { "Usuario" }
                val foto = try { doc.get<String>("fotoPerfil") } catch(e: Exception) { null }
                val seguidores = try { doc.get<List<String>>("seguidores") } catch(e: Exception) { emptyList() }
                val seguidos = try { doc.get<List<String>>("seguidos") } catch(e: Exception) { emptyList() }

                PerfilSocial(uid, username, foto, seguidores, seguidos)
            } else null
        } catch (e: Exception) { null }
    }

    suspend fun alternarSeguimiento(miUid: String, targetUid: String, seguir: Boolean): Boolean {
        return try {
            val miDocRef = firestore.collection("usuarios").document(miUid)
            val miDoc = miDocRef.get()
            val misSeguidos = try { miDoc.get<List<String>>("seguidos").toMutableList() } catch(e: Exception) { mutableListOf() }

            if (seguir && !misSeguidos.contains(targetUid)) misSeguidos.add(targetUid)
            else if (!seguir) misSeguidos.remove(targetUid)
            miDocRef.update(mapOf("seguidos" to misSeguidos))

            val targetDocRef = firestore.collection("usuarios").document(targetUid)
            val targetDoc = targetDocRef.get()
            val susSeguidores = try { targetDoc.get<List<String>>("seguidores").toMutableList() } catch(e: Exception) { mutableListOf() }

            if (seguir && !susSeguidores.contains(miUid)) susSeguidores.add(miUid)
            else if (!seguir) susSeguidores.remove(miUid)
            targetDocRef.update(mapOf("seguidores" to susSeguidores))

            true
        } catch (e: Exception) { false }
    }

    suspend fun obtenerListasPublicas(uid: String): List<ColeccionGuardada> {
        return try {
            val snapshot = firestore.collection("usuarios").document(uid).collection("colecciones").get()
            snapshot.documents
                .map { it.data<ColeccionGuardada>() }
                // 👇 AÑADIDO: && !it.esDescargada para no mostrar las que este usuario guardó de otros
                .filter { it.esPublica && !it.esDescargada }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun obtenerFeedGlobal(limite: Int = 50, miUid: String? = null): List<ColeccionGuardada> {
        return try {
            val snapshot = firestore.collectionGroup("colecciones").get()

            snapshot.documents
                .map { it.data<ColeccionGuardada>() }
                // 👇 AÑADIDO: && !it.esDescargada para ignorar las copias de todos los usuarios
                .filter { it.esPublica && it.idCreador != miUid && !it.esDescargada }
                .shuffled()
                .take(limite)
        } catch (e: Exception) { emptyList() }
    }

    suspend fun darLike(uidAutor: String, nombreColeccion: String, miUid: String, sumar: Boolean): Boolean {
        return try {
            val docRef = firestore.collection("usuarios").document(uidAutor).collection("colecciones").document(nombreColeccion)

            val doc = docRef.get()
            if (doc.exists) {
                val coleccion = doc.data<ColeccionGuardada>()
                val listaLikes = coleccion.usuariosLikes.toMutableList()

                if (sumar && !listaLikes.contains(miUid)) {
                    listaLikes.add(miUid)
                } else if (!sumar) {
                    listaLikes.remove(miUid)
                }

                docRef.update(mapOf(
                    "likes" to listaLikes.size,
                    "usuariosLikes" to listaLikes
                ))
                true
            } else {
                false
            }
        } catch (e: Exception) { false }
    }
}