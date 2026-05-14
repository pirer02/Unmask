package org.example.project

import android.os.Build
import java.util.Locale

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

actual fun obtenerCodigoIdiomaSistema(): String {
    // Esto detecta el idioma del móvil en Android (ej: "es", "en", "fr")
    return Locale.getDefault().language
}