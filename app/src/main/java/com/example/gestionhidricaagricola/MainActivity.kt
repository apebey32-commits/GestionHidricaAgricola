
package com.example.gestionhidricaagricola

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import com.example.gestionhidricaagricola.ui.theme.GestionHidricaAgricolaTheme
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

private const val DATABASE_URL =
    "https://gestionhidricaagricola-default-rtdb.firebaseio.com/"

private const val UMBRAL_HUMEDAD = 30.0

private val Verde = Color(0xFF287D65)
private val Azul = Color(0xFF287CB7)
private val Rojo = Color(0xFFD45B48)
private val Fondo = Color(0xFFF5F6FA)
private val Morado = Color(0xFFE9E3F7)
private val TextoOscuro = Color(0xFF263238)

data class LecturaSensor(
    val id: String = "",
    val humedadSuelo: Double = 0.0,
    val temperatura: Double = 0.0,
    val humedadAmbiental: Double = 0.0,
    val fecha: String = "---"
)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            GestionHidricaAgricolaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Fondo
                ) {
                    MonitoreoFirebase()
                }
            }
        }
    }
}

@Composable
fun MonitoreoFirebase() {

    var historial by remember {
        mutableStateOf<List<LecturaSensor>>(emptyList())
    }

    var estadoConexion by remember {
        mutableStateOf("Preparando conexión...")
    }

    var cargando by remember {
        mutableStateOf(false)
    }

    fun cargarDatos() {
        cargando = true
        estadoConexion = "Consultando Firebase..."

        val referencia = FirebaseDatabase
            .getInstance(DATABASE_URL)
            .getReference("historial")

        referencia.addListenerForSingleValueEvent(
            object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {

                    val registros = mutableListOf<LecturaSensor>()

                    for (registro in snapshot.children) {

                        val lectura = LecturaSensor(
                            id = registro.key ?: "",
                            humedadSuelo = registro
                                .child("humedadSuelo")
                                .getValue(Double::class.java) ?: 0.0,
                            temperatura = registro
                                .child("temperatura")
                                .getValue(Double::class.java) ?: 0.0,
                            humedadAmbiental = registro
                                .child("humedadAmbiental")
                                .getValue(Double::class.java) ?: 0.0,
                            fecha = registro
                                .child("fecha")
                                .getValue(String::class.java) ?: "---"
                        )

                        registros.add(lectura)
                    }

                    historial = registros.reversed()
                    cargando = false

                    estadoConexion = if (registros.isNotEmpty()) {
                        "Conectado: ${registros.size} registros cargados"
                    } else {
                        "Conectado, pero no hay registros en historial"
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    cargando = false
                    estadoConexion = "Error de Firebase: ${error.message}"
                }
            }
        )
    }

    LaunchedEffect(Unit) {
        cargarDatos()
    }

    val ultimaLectura = historial.firstOrNull()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Fondo),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 24.dp,
            bottom = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        item {

            Column {

                Text(
                    text = "Gestión Hídrica Agrícola",
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextoOscuro
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .background(
                                if (estadoConexion.startsWith("Conectado")) {
                                    Verde
                                } else {
                                    Rojo
                                },
                                CircleShape
                            )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = estadoConexion,
                        fontSize = 13.sp,
                        color = Color.DarkGray
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {

                    Button(
                        onClick = { cargarDatos() },
                        enabled = !cargando,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF315D91)
                        )
                    ) {
                        Text(
                            if (cargando) "Consultando..." else "Actualizar"
                        )
                    }
                }
            }
        }

        item {

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Morado
                )
            ) {

                Column(
                    modifier = Modifier.padding(18.dp)
                ) {

                    Text(
                        text = "Último registro",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF35245C)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    if (ultimaLectura != null) {

                        Text(
                            text = "Fecha / ID: ${ultimaLectura.fecha} / ${ultimaLectura.id}",
                            fontSize = 12.sp,
                            color = Color(0xFF493B65),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {

                            Indicador(
                                titulo = "Humedad del suelo",
                                valor = "${ultimaLectura.humedadSuelo}%",
                                color = Verde
                            )

                            Indicador(
                                titulo = "Temperatura",
                                valor = "${ultimaLectura.temperatura} °C",
                                color = Rojo
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        HorizontalDivider(
                            color = Color(0xFFD5CBE8)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Indicador(
                            titulo = "Humedad ambiental",
                            valor = "${ultimaLectura.humedadAmbiental}%",
                            color = Azul
                        )

                    } else {

                        Text(
                            text = "Esperando mediciones de Firebase...",
                            color = Color.DarkGray
                        )
                    }
                }
            }
        }

        // NUEVA SECCIÓN: ALERTA DE HUMEDAD DEL SUELO

        if (ultimaLectura != null) {

            item {

                val humedadBaja =
                    ultimaLectura.humedadSuelo < UMBRAL_HUMEDAD

                val colorAlerta = if (humedadBaja) {
                    Color(0xFFFFE5E0)
                } else {
                    Color(0xFFE0F2E9)
                }

                val colorTexto = if (humedadBaja) {
                    Color(0xFFB83D2D)
                } else {
                    Verde
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colorAlerta
                    )
                ) {

                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {

                        Text(
                            text = if (humedadBaja) {
                                "⚠ Alerta de humedad del suelo"
                            } else {
                                "✓ Estado de humedad del suelo"
                            },
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorTexto
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Humedad actual: ${ultimaLectura.humedadSuelo}%",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colorTexto
                        )

                        Spacer(modifier = Modifier.height(5.dp))

                        Text(
                            text = if (humedadBaja) {
                                "Humedad baja. Se recomienda revisar las condiciones del suelo y evaluar la necesidad de riego."
                            } else {
                                "La humedad está dentro del rango establecido. No se detecta una alerta por humedad baja."
                            },
                            fontSize = 13.sp,
                            color = colorTexto
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Umbral de referencia: ${UMBRAL_HUMEDAD.toInt()}%",
                            fontSize = 12.sp,
                            color = colorTexto
                        )
                    }
                }
            }
        }

        // GRÁFICO HISTÓRICO

        item {

            if (historial.isNotEmpty()) {

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {

                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {

                        Text(
                            text = "Historial de humedad",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextoOscuro
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {

                            Leyenda(
                                color = Verde,
                                texto = "Humedad suelo"
                            )

                            Leyenda(
                                color = Azul,
                                texto = "Humedad ambiental"
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        GraficoHumedad(
                            historial = historial
                        )
                    }
                }
            }
        }

        item {

            Text(
                text = "Historial de mediciones",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextoOscuro
            )
        }

        items(historial) { lectura ->

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {

                Column(
                    modifier = Modifier.padding(14.dp)
                ) {

                    Text(
                        text = "Fecha: ${lectura.fecha}",
                        fontWeight = FontWeight.SemiBold,
                        color = TextoOscuro
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "ID: ${lectura.id}",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Humedad del suelo: ${lectura.humedadSuelo}%",
                        color = Verde
                    )

                    Text(
                        text = "Temperatura: ${lectura.temperatura} °C",
                        color = Rojo
                    )

                    Text(
                        text = "Humedad ambiental: ${lectura.humedadAmbiental}%",
                        color = Azul
                    )
                }
            }
        }
    }
}

@Composable
fun Indicador(
    titulo: String,
    valor: String,
    color: Color
) {

    Column {

        Text(
            text = titulo,
            fontSize = 13.sp,
            color = Color(0xFF554C66)
        )

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = valor,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun Leyenda(
    color: Color,
    texto: String
) {

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )

        Spacer(modifier = Modifier.width(5.dp))

        Text(
            text = texto,
            fontSize = 11.sp,
            color = Color.DarkGray
        )
    }
}

@Composable
fun GraficoHumedad(
    historial: List<LecturaSensor>
) {

    val datos = historial.reversed()

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
    ) {

        val margen = 12.dp.toPx()
        val ancho = size.width - margen * 2
        val alto = size.height - margen * 2

        // Líneas horizontales de referencia.

        for (i in 0..4) {

            val y = margen + alto * i / 4f

            drawLine(
                color = Color(0xFFE0E0E0),
                start = Offset(margen, y),
                end = Offset(size.width - margen, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        if (datos.isEmpty()) return@Canvas

        fun obtenerPunto(
            indice: Int,
            valor: Double
        ): Offset {

            val x = if (datos.size == 1) {
                size.width / 2f
            } else {
                margen + ancho * indice / (datos.size - 1).toFloat()
            }

            val valorLimitado = valor.coerceIn(0.0, 100.0)

            val y = margen + alto *
                    (1f - valorLimitado.toFloat() / 100f)

            return Offset(x, y)
        }

        fun dibujarLinea(
            valores: List<Double>,
            color: Color
        ) {

            if (valores.isEmpty()) return

            val camino = Path()

            valores.forEachIndexed { indice, valor ->

                val punto = obtenerPunto(indice, valor)

                if (indice == 0) {
                    camino.moveTo(punto.x, punto.y)
                } else {
                    camino.lineTo(punto.x, punto.y)
                }
            }

            drawPath(
                path = camino,
                color = color,
                style = Stroke(
                    width = 2.5.dp.toPx()
                )
            )

            valores.forEachIndexed { indice, valor ->

                val punto = obtenerPunto(indice, valor)

                drawCircle(
                    color = color,
                    radius = 4.dp.toPx(),
                    center = punto
                )
            }
        }

        dibujarLinea(
            datos.map { it.humedadSuelo },
            Verde
        )

        dibujarLinea(
            datos.map { it.humedadAmbiental },
            Azul
        )
    }
}
