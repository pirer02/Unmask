package org.example.project // Asegúrate de que este package coincida con el de arriba de tu PantallaMiPerfil

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
actual fun decodificarBase64Imagen(base64: String): ImageBitmap? {
    return try {
        // Transformamos el texto Base64 de nuevo a una imagen que Android entienda
        val bytes = Base64.decode(base64)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    } catch (e: Exception) {
        null
    }
}