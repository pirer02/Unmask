package org.example.project.Datos

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

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

        return try {
            val docNombre = firestore.collection("nombres_usuario").document(nombreLimpio).get()
            if (docNombre.exists) return false

            firestore.collection("usuarios").document(user.uid).set(
                mapOf("email" to (user.email ?: ""), "username" to nombreLimpio)
            )
            firestore.collection("nombres_usuario").document(nombreLimpio).set(
                mapOf("uid" to user.uid)
            )
            true
        } catch (e: Exception) { false }
    }

    suspend fun eliminarCuentaDefinitiva(uid: String): Boolean {
        return try {
            val user = auth.currentUser ?: return false
            val username = obtenerNombreUsuario(uid)

            // Intentamos borrar las listas (Si falla por reglas, no cerramos la app)
            try {
                val colecciones = firestore.collection("usuarios").document(uid).collection("colecciones").get()
                colecciones.documents.forEach { it.reference.delete() }
            } catch (e: Exception) { }

            // Borrar el documento principal
            try { firestore.collection("usuarios").document(uid).delete() } catch (e: Exception) { }

            // Borrar la reserva de nombre
            try {
                if (username != null) firestore.collection("nombres_usuario").document(username).delete()
            } catch (e: Exception) { }

            // Borrar usuario Auth
            user.delete()
            _usuario.value = null
            true
        } catch (e: Exception) {
            // Si salta el error "Requiere inicio de sesión reciente", lo capturamos aquí
            false
        }
    }
}