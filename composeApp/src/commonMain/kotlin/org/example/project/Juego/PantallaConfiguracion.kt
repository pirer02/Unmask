import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlinx.coroutines.launch
import org.example.project.Datos.*

@Composable
fun PantallaConfiguracion(
    coleccion: ColeccionGuardada,
    jugadores: List<String>,
    opcionesIniciales: OpcionesJuego?, // 👇 Recibe la configuración anterior si existe
    snackbarHostState: SnackbarHostState,
    onIrAJugadores: () -> Unit,
    onIniciarJuego: (OpcionesJuego) -> Unit,
    onVolver: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    // 👇 Inicializamos con la configuración pasada por parámetro (para mantenerla intacta al volver)
    var numImpostores by remember { mutableStateOf(opcionesIniciales?.numImpostores ?: 1) }
    var pistaParaImpostor by remember { mutableStateOf(opcionesIniciales?.pistaParaImpostor ?: false) }

    var limiteRondas by remember { mutableStateOf(opcionesIniciales?.limiteRondas ?: false) }
    var rondas by remember { mutableStateOf(opcionesIniciales?.rondas ?: 5) }

    var limiteTiempo by remember { mutableStateOf(opcionesIniciales?.limiteTiempo ?: false) }
    var tiempoMinutos by remember { mutableStateOf(opcionesIniciales?.tiempoMinutos ?: 10) }

    // 👇 El estado del nuevo botón
    var sinRepeticiones by remember { mutableStateOf(opcionesIniciales?.sinRepeticiones ?: false) }

    val maxImpostores = max(1, jugadores.size / 3)
    LaunchedEffect(jugadores.size) {
        if (numImpostores > maxImpostores) numImpostores = maxImpostores // Evitamos bugs sin borrar tu configuración
    }

    // 👇 CÁLCULOS DEL CONTADOR DE PALABRAS
    val totalPalabras = remember(coleccion) {
        coleccion.elementos.sumOf {
            if (it is ElementoGuardado.Individual) 1 else (it as ElementoGuardado.Conjunto).palabras.size
        }
    }
    val palabrasRestantes = if (sinRepeticiones) totalPalabras - GestorDatos.palabrasUsadasSesion.size else totalPalabras

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF9F9F9))) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onVolver) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Volver") }
            Column {
                Text("Configuración", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(coleccion.nombre, fontSize = 14.sp, color = Color(0xFFFF6D00), fontWeight = FontWeight.Bold)
            }
        }

        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            TarjetaConfig(onClick = onIrAJugadores) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Jugadores", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("${jugadores.size} añadidos (Mínimo 3)", fontSize = 14.sp, color = if (jugadores.size < 3) Color.Red else Color.LightGray)
                    }
                    Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = Color.White)
                }
            }

            TarjetaConfig {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Impostores", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Máximo 1 por cada 3 jugadores", fontSize = 12.sp, color = Color.LightGray)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.background(Color(0xFF2A2A2A), RoundedCornerShape(8.dp))) {
                        IconButton(onClick = { if (numImpostores > 1) numImpostores-- }) { Icon(Icons.Rounded.Remove, contentDescription = "-", tint = Color.White) }
                        Text("$numImpostores", color = Color(0xFFFF6D00), fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(horizontal = 8.dp))
                        IconButton(onClick = { if (numImpostores < maxImpostores) numImpostores++ }) { Icon(Icons.Rounded.Add, contentDescription = "+", tint = Color.White) }
                    }
                }
            }

            TarjetaConfig {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Pista para Impostor", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Recibirá una pista vaga de la palabra", fontSize = 12.sp, color = Color.LightGray)
                    }
                    Switch(checked = pistaParaImpostor, onCheckedChange = { pistaParaImpostor = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF18C1A8)))
                }
            }

            // 👇 NUEVA TARJETA: NO REPETIR PALABRAS
            TarjetaConfig {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("No repetir palabras", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Evita que salgan palabras de partidas anteriores", fontSize = 12.sp, color = Color.LightGray)
                        }
                        Switch(
                            checked = sinRepeticiones,
                            onCheckedChange = {
                                sinRepeticiones = it
                                // Se vacía el contador si la enciendes o apagas, contando como desde el principio
                                GestorDatos.palabrasUsadasSesion.clear()
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF18C1A8))
                        )
                    }
                    // Si está activo, mostramos el contador
                    if (sinRepeticiones) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Palabras restantes:", color = Color.White, fontSize = 14.sp)
                            Text(
                                "$palabrasRestantes / $totalPalabras",
                                color = if (palabrasRestantes <= 0) Color.Red else Color(0xFFFF6D00),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            TarjetaConfig {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Límite de Rondas", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(if (limiteRondas) "$rondas rondas" else "Indefinido", fontSize = 12.sp, color = Color.LightGray)
                        }
                        Switch(checked = limiteRondas, onCheckedChange = { limiteRondas = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFFF6D00)))
                    }
                    if (limiteRondas) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Slider(value = rondas.toFloat(), onValueChange = { rondas = it.toInt() }, valueRange = 1f..10f, steps = 8, colors = SliderDefaults.colors(thumbColor = Color(0xFFFF6D00), activeTrackColor = Color(0xFFFF6D00)))
                    }
                }
            }

            TarjetaConfig {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Límite de Tiempo", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(if (limiteTiempo) "$tiempoMinutos minutos" else "Sin límite", fontSize = 12.sp, color = Color.LightGray)
                        }
                        Switch(checked = limiteTiempo, onCheckedChange = { limiteTiempo = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFFF6D00)))
                    }
                    if (limiteTiempo) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Slider(value = tiempoMinutos.toFloat(), onValueChange = { tiempoMinutos = it.toInt() }, valueRange = 1f..20f, steps = 18, colors = SliderDefaults.colors(thumbColor = Color(0xFFFF6D00), activeTrackColor = Color(0xFFFF6D00)))
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (jugadores.size < 3) {
                        coroutineScope.launch { snackbarHostState.showSnackbar("Faltan jugadores. Necesitas mínimo 3.") }
                    } else if (sinRepeticiones && palabrasRestantes <= 0) {
                        // 👇 Bloqueamos si el contador llega a cero
                        coroutineScope.launch { snackbarHostState.showSnackbar("No quedan palabras nuevas. Apaga y enciende la opción para reiniciar.") }
                    } else {
                        val opciones = OpcionesJuego(numImpostores, pistaParaImpostor, limiteRondas, rondas, limiteTiempo, tiempoMinutos, sinRepeticiones)
                        onIniciarJuego(opciones)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF18C1A8)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("INICIAR PARTIDA", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(64.dp))
        }
    }
}

@Composable
fun TarjetaConfig(onClick: (() -> Unit)? = null, contenido: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF181818)).let { if (onClick != null) it.clickable { onClick() } else it }.padding(20.dp)
    ) { contenido() }
}