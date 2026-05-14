package org.example.project.Datos

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.example.project.obtenerCodigoIdiomaSistema

// Definimos los idiomas que soporta tu app con su código oficial
enum class IdiomaSoportado(val codigo: String, val nombre: String) {
    INGLES("en", "English"),
    ESPANOL("es", "Español"),
    FRANCES("fr", "Français"),
    ITALIANO("it", "Italiano"),
    ALEMAN("de", "Deutsch"),
    CHINO("zh", "中文"),
    JAPONES("ja", "日本語")
}

object GestorIdiomas {
    // Inicializamos el estado determinando el idioma correcto (guardado o sistema)
    private val _idiomaActual = MutableStateFlow(determinarIdiomaInicial())
    val idiomaActual: StateFlow<IdiomaSoportado> = _idiomaActual.asStateFlow()

    fun cambiarIdioma(nuevoIdioma: IdiomaSoportado) {
        _idiomaActual.value = nuevoIdioma
        // 👇 NUEVO: Guardamos el idioma en el dispositivo
        GestorDatos.guardarIdiomaAjustes(nuevoIdioma.codigo)
    }

    private fun determinarIdiomaInicial(): IdiomaSoportado {
        // 1. Miramos si ya el usuario guardó un idioma previamente
        val idiomaGuardado = GestorDatos.cargarIdiomaGuardado()

        // 2. Si hay uno guardado lo usamos, si es null (primera vez) detectamos el del móvil
        val codigoBuscado = idiomaGuardado ?: obtenerCodigoIdiomaSistema()

        // 3. Devolvemos el idioma correspondiente o Inglés por defecto
        return IdiomaSoportado.entries.find { it.codigo == codigoBuscado } ?: IdiomaSoportado.INGLES
    }
}