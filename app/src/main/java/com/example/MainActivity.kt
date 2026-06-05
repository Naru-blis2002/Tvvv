package com.example

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.ui.theme.MyApplicationTheme

/**
 * Data Model for television signals with Geo-Blocking metadata included.
 */
data class Canal(
    val id: String,
    val nombre: String,
    val descripcion: String,
    val streamUrl: String,
    val streamUrlAlternativo: String? = null,
    val logoUrl: String,
    val categoria: String,
    val pais: String,
    val requiereVpn: Boolean = false,
    val notasGeo: String = "Puede requerir VPN de este país si te encuentras en el extranjero."
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = true) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    TvStreamingScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

/**
 * Serializes and saves custom user-added signals to Android SharedPreferences.
 */
fun saveCustomCanales(context: Context, canales: List<Canal>) {
    val prefs = context.getSharedPreferences("latam_tv_prefs", Context.MODE_PRIVATE)
    val serialized = canales.joinToString(";;;") { 
        "${it.id}:::${it.nombre}:::${it.descripcion}:::${it.streamUrl}:::${it.streamUrlAlternativo ?: ""}:::${it.logoUrl}:::${it.categoria}:::${it.pais}:::${it.requiereVpn}:::${it.notasGeo}"
    }
    prefs.edit().putString("custom_channels", serialized).apply()
}

/**
 * Loads custom user-added signals from Android SharedPreferences.
 */
fun loadCustomCanales(context: Context): List<Canal> {
    val prefs = context.getSharedPreferences("latam_tv_prefs", Context.MODE_PRIVATE)
    val serialized = prefs.getString("custom_channels", "") ?: ""
    if (serialized.isEmpty()) return emptyList()
    return serialized.split(";;;").mapNotNull { line ->
        val parts = line.split(":::")
        if (parts.size >= 9) {
            Canal(
                id = parts[0],
                nombre = parts[1],
                descripcion = parts[2],
                streamUrl = parts[3],
                streamUrlAlternativo = parts[4].takeIf { it.isNotEmpty() },
                logoUrl = parts[5],
                categoria = parts[6],
                pais = parts[7],
                requiereVpn = parts[8].toBoolean(),
                notasGeo = if (parts.size >= 10) parts[9] else "Conexión local recomendada para esta señal."
            )
        } else null
    }
}

@Composable
fun TvStreamingScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    
    // Default TV Channel list
    val defaultCanales = remember {
        listOf(
            // Costa Rica
            Canal(
                id = "cr_sinart", 
                nombre = "Canal 13 (SINART)", 
                descripcion = "Televisión nacional de Costa Rica con educación, deportes y cultura.", 
                streamUrl = "https://sinart-live.stmv.live/sinart/live.m3u8", 
                streamUrlAlternativo = "http://sinart.stmv.live/sinart/playlist.m3u8",
                logoUrl = "https://i.ibb.co/yn5M8dw/telefides-costa-rica.png", 
                categoria = "Publico", 
                pais = "Costa Rica",
                requiereVpn = true,
                notasGeo = "SINART restringe transmisiones externas para eventos con derechos deportivos nacionales."
            ),
            Canal(
                id = "cr_vmlatino", 
                nombre = "VM Latino CR", 
                descripcion = "El canal de la música y entretenimiento juvenil líder en Costa Rica, éxitos pop y reggaeton.", 
                streamUrl = "https://vmlatino.stmv.live/vmlatino/live.m3u8", 
                streamUrlAlternativo = "https://vmlatino.stmv.live/vmlatino/playlist.m3u8",
                logoUrl = "https://i.imgur.com/Dvo1b82.png", 
                categoria = "Musica", 
                pais = "Costa Rica",
                requiereVpn = false,
                notasGeo = "Emisión musical libre sin limitación de IP."
            ),
            Canal(
                id = "cr_telefides", 
                nombre = "Telefides", 
                descripcion = "Canal católico costarricense con programación familiar y orientativa.", 
                streamUrl = "https://telefides.stmv.live/telefides/live.m3u8", 
                streamUrlAlternativo = "http://telefides.stmv.live/telefides/playlist.m3u8",
                logoUrl = "https://i.ibb.co/yn5M8dw/telefides-costa-rica.png", 
                categoria = "Religion", 
                pais = "Costa Rica",
                requiereVpn = false,
                notasGeo = "Distribución familiar abierta internacionalmente."
            ),
            Canal(
                id = "cr_colosal", 
                nombre = "Colosal TV Pacífico", 
                descripcion = "Señal comunitaria regional transmitiendo desde el Sur de Costa Rica.", 
                streamUrl = "https://colosal.stmv.live/colosal/live.m3u8", 
                streamUrlAlternativo = "http://colosal.stmv.live/colosal/playlist.m3u8",
                logoUrl = "https://i.imgur.com/0DZB3eB.png", 
                categoria = "Regional", 
                pais = "Costa Rica",
                requiereVpn = false,
                notasGeo = "Televisión comunitaria abierta."
            ),
            // Perú
            Canal(
                id = "pe_nativa", 
                nombre = "Nativa TV Perú", 
                descripcion = "Señal periodística independiente, debates analíticos y transmisiones deportivas de ligas peruanas.", 
                streamUrl = "https://stream.nativa.pe/hls/nativatv.m3u8", 
                streamUrlAlternativo = "https://directo.nativa.pe/hls/nativatv.m3u8",
                logoUrl = "https://i.imgur.com/Y9k9XSO.png", 
                categoria = "Noticias", 
                pais = "Perú",
                requiereVpn = false,
                notasGeo = "Emisión nacional abierta. Derechos deportivos del torneo de reserva de Perú disponibles."
            ),
            Canal(
                id = "pe_rpp", 
                nombre = "RPP TV Noticias", 
                descripcion = "La cadena radial líder informativa en el Perú emitiendo en directo para pantallas de televisión.", 
                streamUrl = "https://rpp.stmv.live/rpp/live.m3u8", 
                streamUrlAlternativo = "https://rpp-live.stmv.live/rpp/live.m3u8",
                logoUrl = "https://i.imgur.com/oBz3CgE.png", 
                categoria = "Noticias", 
                pais = "Perú",
                requiereVpn = true,
                notasGeo = "IPTV de Radio Programas del Perú. Posee bloqueos geográficos intermitentes fuera de Perú."
            ),
            Canal(
                id = "pe_unife", 
                nombre = "Unife TV Perú", 
                descripcion = "Canal de televisión abierta con entretenimiento positivo, informes y reportajes.", 
                streamUrl = "https://unife.stmv.live/unife/live.m3u8", 
                streamUrlAlternativo = "http://unife-live.stmv.live/unife/live.m3u8",
                logoUrl = "https://i.imgur.com/lVpDLVU.png", 
                categoria = "General", 
                pais = "Perú",
                requiereVpn = false,
                notasGeo = "Señal abierta generalista."
            ),
            Canal(
                id = "pe_bethel", 
                nombre = "Bethel TV Perú", 
                descripcion = "Valores educativos, éticos, científicos y culturales desde Lima.", 
                streamUrl = "https://vcp.betheltv.tv/live/bethel/playlist.m3u8", 
                logoUrl = "https://i.imgur.com/zCVh765.png", 
                categoria = "Religion", 
                pais = "Perú",
                requiereVpn = false,
                notasGeo = "Señal satelital de libre disponibilidad global."
            ),
            // Argentina
            Canal(
                id = "ar_tn", 
                nombre = "TN (Todo Noticias)", 
                descripcion = "Líder indiscutido en transmisiones informativas, móviles en vivo y debates de Argentina.", 
                streamUrl = "https://live-edge.tn.com.ar/live/tn/tn_720.m3u8", 
                streamUrlAlternativo = "https://tn-live.stmv.live/tn/live.m3u8",
                logoUrl = "https://i.imgur.com/vFwPhPS.png", 
                categoria = "Noticias", 
                pais = "Argentina",
                requiereVpn = true,
                notasGeo = "Todo Noticias de Artear. Se geo-bloquea fuera de Latinoamérica/Argentina o bajo proxies."
            ),
            Canal(
                id = "ar_canal26", 
                nombre = "Canal 26 Argentina", 
                descripcion = "Cobertura internacional integral de noticias globales en tiempo real.", 
                streamUrl = "https://live-edge.canal26.com/live/canal26/playlist_720.m3u8", 
                streamUrlAlternativo = "https://canal26-live.stmv.live/canal26/live.m3u8",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/c/c5/Canal_26_logo_%282022%29.svg/500px-Canal_26_logo_%282022%29.svg.png", 
                categoria = "Noticias", 
                pais = "Argentina",
                requiereVpn = false,
                notasGeo = "Canal informativo global abierto."
            ),
            Canal(
                id = "ar_tvpublica", 
                nombre = "TV Pública Argentina", 
                descripcion = "Transmisiones oficiales del estado nacional argentino, espectáculos federales e históricos.", 
                streamUrl = "https://tvp.stmv.live/tvp/live.m3u8", 
                streamUrlAlternativo = "https://tvp-live.stmv.live/tvp/playlist.m3u8",
                logoUrl = "https://i.imgur.com/iJMH0UK.png", 
                categoria = "Publico", 
                pais = "Argentina",
                requiereVpn = true,
                notasGeo = "Estrictamente bloqueado por IP fuera del territorio argentino en eventos de liga nacional."
            ),
            Canal(
                id = "ar_c5n", 
                nombre = "C5N Noticias", 
                descripcion = "Canal informativo de actualidad con debates, política y panoramas de economía.", 
                streamUrl = "https://live-edge.c5n.com/live/c5n/playlist.m3u8", 
                logoUrl = "https://i.imgur.com/v1uB8F5.png", 
                categoria = "Noticias", 
                pais = "Argentina",
                requiereVpn = false,
                notasGeo = "Banda informativa libre."
            ),
            // Soporte / Pruebas
            Canal(
                id = "test_bunny", 
                nombre = "Canal de Soporte (Mux)", 
                descripcion = "Flujo de prueba oficial de Mux para validar codecs receptores.", 
                streamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8", 
                logoUrl = "https://images.mux.com/lXfXF9jEicNfUeWvLAtSizg70267C7Ym00/thumbnail.jpg", 
                categoria = "Prueba", 
                pais = "Soporte",
                requiereVpn = false,
                notasGeo = "Señal libre de Google/Mux para chequeo técnico."
            ),
            Canal(
                id = "test_bbb_mp4", 
                nombre = "Canal de Soporte (MP4)", 
                descripcion = "Film de prueba Big Buck Bunny en alta resolución para calibración de búfer.", 
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4", 
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/c/c5/Big_Buck_Bunny_Logo.svg/1200px-Big_Buck_Bunny_Logo.svg.png", 
                categoria = "Prueba", 
                pais = "Soporte",
                requiereVpn = false,
                notasGeo = "Enlace puro multirregional de testeo."
            )
        )
    }

    // Load custom channels populated by the user
    val customCanalesList = remember { mutableStateListOf<Canal>() }
    LaunchedEffect(Unit) {
        customCanalesList.addAll(loadCustomCanales(context))
    }

    // Unified list combines default and custom channels
    val canalesList = remember(customCanalesList) {
        defaultCanales + customCanalesList
    }

    // Active selection & filtering states
    var selectedCanal by remember { mutableStateOf(canalesList.first { it.id == "ar_tn" }) }
    var selectedCategory by remember { mutableStateOf("Todos") }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var favoritosIds by remember { mutableStateOf(setOf("ar_tn", "cr_vmlatino", "pe_nativa")) }

    // Navigation and Collapsible section parameters
    var showNetworkTools by remember { mutableStateOf(false) }
    var showAddChannelDialog by remember { mutableStateOf(false) }

    // Playback settings for bypass filters
    val userAgents = remember {
        listOf(
            "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36" to "Chrome Android 📱 (Recomendado)",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36" to "Chrome Windows 💻",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 16_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.5 Mobile/15E148 Safari/604.1" to "Safari iOS 🍎",
            "Mozilla/5.0 (SmartTV; Realtek; LG NetCast) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/53.0.2785.143 Safari/537.36" to "Smart TV TV 📺",
            "ExoPlayerDemo/3.13" to "Default ExoPlayer ⚙️"
        )
    }
    var selectedUserAgentIndex by remember { mutableStateOf(0) }
    var useAlternativeLink by remember { mutableStateOf(false) }
    var showGeoDiagnoseResult by remember { mutableStateOf(false) }
    var enableIpBypassHeader by remember { mutableStateOf(true) }
    var customIpBypassAddress by remember { mutableStateOf("") }

    // Read values dynamically based on settings
    val currentUrl = if (useAlternativeLink && selectedCanal.streamUrlAlternativo != null) {
        selectedCanal.streamUrlAlternativo!!
    } else {
        selectedCanal.streamUrl
    }
    val currentUserAgent = userAgents[selectedUserAgentIndex].first

    // Category calculation lists
    val categoriasUnicas = remember(customCanalesList) {
        listOf("Todos", "Costa Rica", "Perú", "Argentina", "Favoritos ❤️", "Personalizados 🔗", "Soporte")
    }

    // Filter signals dynamically
    val canalesFiltrados = canalesList.filter { canal ->
        val matchCategory = when (selectedCategory) {
            "Todos" -> true
            "Favoritos ❤️" -> favoritosIds.contains(canal.id)
            "Personalizados 🔗" -> canal.id.startsWith("custom_")
            "Soporte" -> canal.pais.equals("Soporte", ignoreCase = true)
            else -> canal.pais.equals(selectedCategory, ignoreCase = true)
        }
        val matchSearch = canal.nombre.contains(searchQuery, ignoreCase = true) ||
                canal.descripcion.contains(searchQuery, ignoreCase = true)

        matchCategory && matchSearch
    }

    // If active selected channel was deleted, reset selection
    LaunchedEffect(canalesList) {
        if (!canalesList.contains(selectedCanal)) {
            selectedCanal = canalesList.firstOrNull() ?: defaultCanales.first()
        }
    }

    // Reset alternative link state when changing channel
    LaunchedEffect(selectedCanal) {
        useAlternativeLink = false
        showGeoDiagnoseResult = false
    }

    Column(modifier = modifier.fillMaxSize().background(Color(0xFF07090E))) {
        
        // --- HEADER BAR ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F121C))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFE2A612)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "📺", fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(text = "Latam TV", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    Text(text = "Control de Red & Geo-desbloqueo", color = Color(0xFF8C93A8), fontSize = 10.sp)
                }
            }
            
            // Add custom channel button (Material 3 small floating style)
            Button(
                onClick = { showAddChannelDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1B1F32),
                    contentColor = Color(0xFFE2A612)
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text(text = "＋ Añadir m3u8", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // --- 1. VIDEO PLAYER OVERLAY ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
                .aspectRatio(1.77f)
                .border(width = 0.5.dp, color = Color(0xFF1D2235))
        ) {
            VideoPlayerContainer(
                streamUrl = currentUrl,
                canalNombre = selectedCanal.nombre,
                selectedUserAgent = currentUserAgent,
                customReferer = if (selectedCanal.id == "cr_sinart") "https://sinart-live.stmv.live/" else null,
                paisCanal = selectedCanal.pais,
                requiereVpn = selectedCanal.requiereVpn,
                enableIpBypassHeader = enableIpBypassHeader,
                customIpBypassAddress = customIpBypassAddress,
                modifier = Modifier.fillMaxSize()
            )
        }

        // --- DESCRIPTION & COLLAPSIBLE CONTROLS ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF0E111B), Color(0xFF07090E))
                    )
                )
        ) {
            // Channel Info Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = selectedCanal.nombre,
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF1D2235))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = selectedCanal.pais.uppercase(),
                                color = Color(0xFFE2A612),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = selectedCanal.descripcion,
                        color = Color(0xFFA5ABC4),
                        fontSize = 11.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 14.sp
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Favorite Toggle
                val isFavorite = favoritosIds.contains(selectedCanal.id)
                IconButton(
                    onClick = {
                        favoritosIds = if (isFavorite) favoritosIds - selectedCanal.id else favoritosIds + selectedCanal.id
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isFavorite) Color(0xFFFF1744).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.04f))
                ) {
                    Text(
                        text = if (isFavorite) "❤️" else "🖤", 
                        fontSize = 16.sp,
                        modifier = Modifier.testTag("fav_btn_${selectedCanal.id}")
                    )
                }
            }

            // Quick bypass helpers badge bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp).padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (selectedCanal.requiereVpn) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFE53935).copy(alpha = 0.15f))
                                .border(1.dp, Color(0xFFEF5350).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFE53935)))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "VPN Aconsejable: " + selectedCanal.pais, color = Color(0xFFFF8A80), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF2E7D32).copy(alpha = 0.15f))
                                .border(1.dp, Color(0xFF4CAF50).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(text = "🌐 Libre (Sin Bloqueo Geográfico Conocido)", color = Color(0xFFA5D6A7), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (selectedCanal.id.startsWith("custom_")) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFE2A612).copy(alpha = 0.15f))
                                .border(1.dp, Color(0xFFE2A612).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(text = "🔗 Personalizado", color = Color(0xFFFFD54F), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Collapsible network override panel trigger
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (showNetworkTools) Color(0xFFE2A612) else Color(0xFF1D2235))
                        .clickable { showNetworkTools = !showNetworkTools }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = if (showNetworkTools) "▲ Filtros Activos" else "🛠️ Unblocker",
                        color = if (showNetworkTools) Color.Black else Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Collapsible Geo-Bypass & Network Controls Menu
            AnimatedVisibility(
                visible = showNetworkTools,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp).padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF121626))
                        .border(1.dp, Color(0xFF1F243A), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "🛠️ BYPASS DE GEO-BLOQUEO & RED (AJUSTES)",
                        color = Color(0xFFE2A612),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Modifica los agentes de envío o alterna con espejos para burlar bloqueos geográficos locales o corporativos.",
                        color = Color(0xFF8A91A5),
                        fontSize = 10.sp,
                        lineHeight = 13.sp,
                        modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                    )

                    // 1. Alternate link switch
                    if (selectedCanal.streamUrlAlternativo != null) {
                        Text(
                            text = "Seleccionar Enlace de Señal:",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (!useAlternativeLink) Color(0xFFE2A612).copy(alpha = 0.2f) else Color(0xFF1C2035))
                                    .border(width = 1.dp, color = if (!useAlternativeLink) Color(0xFFE2A612) else Color.Transparent, shape = RoundedCornerShape(6.dp))
                                    .clickable { useAlternativeLink = false }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "Enlace Principal", color = if (!useAlternativeLink) Color(0xFFFFD54F) else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(text = "OFICIAL CDN", color = Color(0xFF8C93A8), fontSize = 8.sp)
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (useAlternativeLink) Color(0xFFE2A612).copy(alpha = 0.2f) else Color(0xFF1C2035))
                                    .border(width = 1.dp, color = if (useAlternativeLink) Color(0xFFE2A612) else Color.Transparent, shape = RoundedCornerShape(6.dp))
                                    .clickable { useAlternativeLink = true }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "Enlace Espejo", color = if (useAlternativeLink) Color(0xFFFFD54F) else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(text = "CÓDIGO DE RESPALDO", color = Color(0xFF8C93A8), fontSize = 8.sp)
                                }
                            }
                        }
                    }

                    // 2. User Agent Switcher
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Emulación del Agente de Usuario (User-Agent HTTP Bypass):",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        userAgents.forEachIndexed { idx, (ua, label) ->
                            val isSelected = selectedUserAgentIndex == idx
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) Color(0xFF1E243A) else Color(0xFF171B2D))
                                    .clickable { selectedUserAgentIndex = idx }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedUserAgentIndex = idx },
                                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFE2A612), unselectedColor = Color(0xFF5E657A)),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(text = label, color = if (isSelected) Color(0xFFFFD54F) else Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    Text(text = ua, color = Color(0xFF8A91A5), fontSize = 8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }

                    // 3. HTTP Header Forgery (IP Simulation Bypass)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Suplantación de Cabeceras IP (Simulación HTTP):",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E243A).copy(alpha = 0.5f))
                            .clickable { enableIpBypassHeader = !enableIpBypassHeader }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Enviar cabeceras X-Forwarded-For",
                                color = if (enableIpBypassHeader) Color(0xFFFFD54F) else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            val currentSimulatedIp = customIpBypassAddress.ifBlank { getSimulatedIpForCountry(selectedCanal.pais) }
                            Text(
                                text = if (enableIpBypassHeader) "Inyectando IP local: $currentSimulatedIp" else "IP Bypass desactivado (Usando IP del sistema)",
                                color = Color(0xFF8A91A5),
                                fontSize = 8.sp
                            )
                        }
                        Switch(
                            checked = enableIpBypassHeader,
                            onCheckedChange = { enableIpBypassHeader = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFFE2A612),
                                checkedTrackColor = Color(0xFFE2A612).copy(alpha = 0.4f)
                            )
                        )
                    }

                    if (enableIpBypassHeader) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = customIpBypassAddress,
                                onValueChange = { customIpBypassAddress = it },
                                label = { Text("IP de Bypass Personalizada", fontSize = 9.sp) },
                                placeholder = { Text("Ej: 201.191.135.45", fontSize = 10.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f).height(46.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFE2A612),
                                    focusedLabelColor = Color(0xFFE2A612),
                                    unfocusedLabelColor = Color(0xFF808595),
                                    unfocusedTextColor = Color.White,
                                    focusedTextColor = Color.White
                                )
                            )
                            Button(
                                onClick = { 
                                    customIpBypassAddress = getSimulatedIpForCountry(selectedCanal.pais) 
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF252D4A)),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("🔄 Aleatoria", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }

                    // 4. Dynamic diagnostics for regional blocking
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0F121E))
                            .padding(10.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📊 Diagnóstico Regional de Conexión",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Diagnosticar",
                                    color = Color(0xFFE2A612),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier
                                        .clickable { showGeoDiagnoseResult = true }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Ubicación sugerida de servidor actual: EE.UU. u Europa (Servidor de Streaming AI Studio).",
                                color = Color(0xFF888F9F),
                                fontSize = 9.sp
                            )

                            if (showGeoDiagnoseResult) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Divider(color = Color(0xFF1F243E), thickness = 0.5.dp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Resultado del Análisis:",
                                    color = Color(0xFFFFD54F),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "1. Canal seleccionado: [${selectedCanal.nombre}] - Región original: [${selectedCanal.pais}].\n" +
                                            "2. Geo-bloqueo sospechado: ${if (selectedCanal.requiereVpn) "Sí (Alto Impacto)" else "Bajo (Generalmente abierto)"}.\n" +
                                            "3. Simulación de IP Activa: ${if (enableIpBypassHeader) "Sí (Cabeceras HTTP de ${selectedCanal.pais} inyectadas)" else "No (Usando su IP directa)"}.\n" +
                                            "4. Solución física o alternativa: Si la simulación de IP automática falla por restricciones de la CDN, inicie un túnel VPN real con salida en ${selectedCanal.pais} en su dispositivo, luego pulse REINTENTAR.",
                                    color = Color(0xFFA5D6A7),
                                    fontSize = 9.sp,
                                    lineHeight = 12.sp
                                )
                            }
                        }
                    }

                    // 4. If custom channel is selected, allow deletion
                    if (selectedCanal.id.startsWith("custom_")) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val remaining = customCanalesList.filter { it.id != selectedCanal.id }
                                customCanalesList.clear()
                                customCanalesList.addAll(remaining)
                                saveCustomCanales(context, remaining)
                                selectedCanal = defaultCanales.first { it.id == "ar_tn" }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth().height(36.dp)
                        ) {
                            Text(text = "🗑️ Eliminar este Canal Personalizado", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }

        // --- 2. CATEGORIES FILTER SELECTOR (M3 Dynamic Styled) ---
        Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Text(
                text = "📁 PAÍS Y CATEGORÍAS DISPONIBLES", 
                color = Color(0xFF7E849B), 
                fontSize = 10.sp, 
                fontWeight = FontWeight.Bold, 
                modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categoriasUnicas.forEach { categ ->
                    val isActive = selectedCategory == categ
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isActive) Color(0xFFE2A612) else Color(0xFF141824))
                            .border(
                                width = 1.dp, 
                                color = if (isActive) Color(0xFFFFD54F) else Color(0xFF1F243A), 
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedCategory = categ }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("category_pill_$categ")
                    ) {
                        Text(
                            text = categ, 
                            color = if (isActive) Color.Black else Color.White, 
                            fontSize = 11.sp, 
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // --- 3. SEARCH FILTER INPUT ---
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar por nombre o descripción de canal...", color = Color(0xFF5E657A), fontSize = 12.sp) },
                singleLine = true,
                maxLines = 1,
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        Text(
                            text = "✕", 
                            color = Color(0xFF8A93A8), 
                            fontSize = 14.sp, 
                            modifier = Modifier
                                .clickable { searchQuery = "" }
                                .padding(8.dp)
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .testTag("search_field"),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF0F121C),
                    unfocusedContainerColor = Color(0xFF0F121C),
                    focusedIndicatorColor = Color(0xFF3B435E),
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
        }

        // --- 4. CHANNEL SELECTION LIST ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (canalesFiltrados.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = "🔍", fontSize = 36.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No se encontraron canales",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Intenta buscando otra palabra o selecciona una categoría diferente.",
                        color = Color(0xFF7D8399),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(canalesFiltrados) { canal ->
                        val isPlaying = selectedCanal.id == canal.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .testTag("channel_card_${canal.id}"),
                            shape = RoundedCornerShape(10.dp),
                            border = if (isPlaying) BorderStroke(1.dp, Color(0xFFE2A612)) else BorderStroke(0.5.dp, Color(0xFF1D2235)),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isPlaying) Color(0xFF141826) else Color(0xFF0C0F17)
                            ),
                            onClick = { selectedCanal = canal }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Channel Logo Holder
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF1E2235)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = canal.logoUrl, 
                                        contentDescription = canal.nombre, 
                                        modifier = Modifier.size(36.dp), 
                                        contentScale = ContentScale.Fit
                                    )
                                    // Overlay fall-back acronym
                                    Text(
                                        text = canal.nombre.take(2).uppercase(), 
                                        color = Color(0xFFE2A612), 
                                        fontSize = 11.sp, 
                                        fontWeight = FontWeight.Black
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(12.dp))
                                
                                // Text fields
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = canal.nombre, 
                                            color = Color.White, 
                                            fontSize = 14.sp, 
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        // Colored badge indicating country
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFF191D30))
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = canal.pais, 
                                                color = Color(0xFFA5ABC4), 
                                                fontSize = 8.sp, 
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        
                                        if (canal.requiereVpn) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(text = "🔒", fontSize = 10.sp)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = canal.descripcion, 
                                        color = Color(0xFF8A91A5), 
                                        fontSize = 11.sp, 
                                        maxLines = 1, 
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                // Secondary play button or indicator
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(if (isPlaying) Color(0xFFE2A612) else Color(0xFF1B1E2E)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isPlaying) "◀" else "▶", 
                                        color = if (isPlaying) Color.Black else Color.White, 
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- DIALOG: ADD CUSTOM CHANNEL ---
    if (showAddChannelDialog) {
        var newNombre by remember { mutableStateOf("") }
        var newStreamUrl by remember { mutableStateOf("") }
        var newStreamUrlAlt by remember { mutableStateOf("") }
        var newLogoUrl by remember { mutableStateOf("") }
        var newPais by remember { mutableStateOf("Costa Rica") }
        var newRequiereVpn by remember { mutableStateOf(false) }
        var hasError by remember { mutableStateOf(false) }

        val paisesOpciones = listOf("Costa Rica", "Perú", "Argentina", "Otro")

        AlertDialog(
            onDismissRequest = { showAddChannelDialog = false },
            title = {
                Text(
                    text = "🔗 Añadir Señal m3u8 Personalizada",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            containerColor = Color(0xFF121524),
            confirmButton = {
                Button(
                    onClick = {
                        if (newNombre.trim().isEmpty() || newStreamUrl.trim().isEmpty()) {
                            hasError = true
                        } else {
                            val nextId = "custom_${System.currentTimeMillis()}"
                            val logoToSet = newLogoUrl.trim().ifEmpty { "https://i.imgur.com/8F9pQfF.png" }
                            val nuevoCanal = Canal(
                                id = nextId,
                                nombre = newNombre.trim(),
                                descripcion = "Señal personalizada añadida por el usuario. Enlace manual m3u8/mp4.",
                                streamUrl = newStreamUrl.trim(),
                                streamUrlAlternativo = newStreamUrlAlt.trim().takeIf { it.isNotEmpty() },
                                logoUrl = logoToSet,
                                categoria = "Personalizados 🔗",
                                pais = newPais,
                                requiereVpn = newRequiereVpn,
                                notasGeo = "Enlace manual del receptor."
                            )
                            
                            customCanalesList.add(nuevoCanal)
                            saveCustomCanales(context, customCanalesList)
                            selectedCanal = nuevoCanal
                            selectedCategory = "Personalizados 🔗"
                            showAddChannelDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE2A612), contentColor = Color.Black)
                ) {
                    Text(text = "Guardar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddChannelDialog = false }) {
                    Text(text = "Cancelar", color = Color(0xFF8C93A8))
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Introduce un enlace .m3u8 o .mp4 para reproducirlo directamente en las pantallas del reproductor. Se guardará de forma persistente.",
                        color = Color(0xFF8C93A8),
                        fontSize = 11.sp,
                        lineHeight = 14.sp
                    )

                    if (hasError) {
                        Text(
                            text = "⚠ Nombre y URL de transmisión son requisitos obligatorios.",
                            color = Color(0xFFEF5350),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Fields
                    OutlinedTextField(
                        value = newNombre,
                        onValueChange = { newNombre = it },
                        label = { Text("Nombre del Canal") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE2A612),
                            focusedLabelColor = Color(0xFFE2A612),
                            unfocusedLabelColor = Color(0xFF808595),
                            unfocusedTextColor = Color.White,
                            focusedTextColor = Color.White
                        )
                    )

                    OutlinedTextField(
                        value = newStreamUrl,
                        onValueChange = { newStreamUrl = it },
                        label = { Text("URL de Transmisión (.m3u8 / .mp4)") },
                        placeholder = { Text("https://servidor.com/live.m3u8") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE2A612),
                            focusedLabelColor = Color(0xFFE2A612),
                            unfocusedLabelColor = Color(0xFF808595),
                            unfocusedTextColor = Color.White,
                            focusedTextColor = Color.White
                        )
                    )

                    OutlinedTextField(
                        value = newStreamUrlAlt,
                        onValueChange = { newStreamUrlAlt = it },
                        label = { Text("URL Espejo / Backup (Opcional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE2A612),
                            focusedLabelColor = Color(0xFFE2A612),
                            unfocusedLabelColor = Color(0xFF808595),
                            unfocusedTextColor = Color.White,
                            focusedTextColor = Color.White
                        )
                    )

                    OutlinedTextField(
                        value = newLogoUrl,
                        onValueChange = { newLogoUrl = it },
                        label = { Text("URL del Logotipo (Opcional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE2A612),
                            focusedLabelColor = Color(0xFFE2A612),
                            unfocusedLabelColor = Color(0xFF808595),
                            unfocusedTextColor = Color.White,
                            focusedTextColor = Color.White
                        )
                    )

                    // Country Selection Row
                    Text(text = "País de Origen:", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        paisesOpciones.forEach { p ->
                            val isSel = newPais == p
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) Color(0xFFE2A612) else Color(0xFF1E2135))
                                    .clickable { newPais = p }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = p, 
                                    color = if (isSel) Color.Black else Color.White, 
                                    fontSize = 11.sp, 
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Switch for VPN dependency
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { newRequiereVpn = !newRequiereVpn }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Sujeto a Bloqueo Geográfico Estricto", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(text = "Marca esta casilla si el canal solo responde a IPs domésticas de su país original.", color = Color(0xFF8C93A8), fontSize = 10.sp, lineHeight = 12.sp)
                        }
                        Switch(
                            checked = newRequiereVpn,
                            onCheckedChange = { newRequiereVpn = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFFE2A612),
                                checkedTrackColor = Color(0xFFE2A612).copy(alpha = 0.4f)
                            )
                        )
                    }
                }
            }
        )
    }
}

fun getSimulatedIpForCountry(country: String): String {
    return when (country.lowercase().trim()) {
        "costa rica" -> {
            val options = listOf("201.191.135.45", "196.40.48.5", "186.15.54.10", "190.113.64.12")
            options.random()
        }
        "perú", "peru" -> {
            val options = listOf("190.119.24.110", "179.7.0.12", "200.121.134.19", "190.187.52.22")
            options.random()
        }
        "argentina" -> {
            val options = listOf("200.45.191.12", "186.108.35.44", "190.2.16.89", "190.224.160.100")
            options.random()
        }
        else -> {
            "190.119.24.110" // Default Latam
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerContainer(
    streamUrl: String,
    canalNombre: String,
    selectedUserAgent: String,
    customReferer: String?,
    paisCanal: String,
    requiereVpn: Boolean,
    enableIpBypassHeader: Boolean,
    customIpBypassAddress: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isBuffering by remember { mutableStateOf(true) }
    var streamError by remember { mutableStateOf<String?>(null) }
    var reloadTrigger by remember { mutableStateOf(0) }

    // Instantiate ExoPlayer
    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    // Attach Player Change Listeners
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_READY) {
                    streamError = null
                }
            }
            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                val codeName = when (error.errorCode) {
                    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> "Código de estado HTTP incorrecto (403 Forbidden / 404 No Encontrado)."
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> "Fallo en conexión de red con el host remoto."
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> "Tiempo de espera de conexión de red excedido."
                    PlaybackException.ERROR_CODE_IO_UNSPECIFIED -> "El canal no responde bajo este agente."
                    else -> error.localizedMessage ?: "Fallo de decodificación o red."
                }
                streamError = codeName
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    // Dynamic stream loading configuration when parameters change
    LaunchedEffect(streamUrl, selectedUserAgent, customReferer, reloadTrigger, enableIpBypassHeader, customIpBypassAddress) {
        isBuffering = true
        streamError = null
        
        try {
            // Build custom HTTP-source factories dynamically
            val httpDataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
                .setUserAgent(selectedUserAgent)
                .setAllowCrossProtocolRedirects(true)
            
            // Populate referer header if needed
            val customHeaders = mutableMapOf<String, String>()
            if (!customReferer.isNullOrBlank()) {
                customHeaders["Referer"] = customReferer
            }
            if (enableIpBypassHeader) {
                val ipToUse = customIpBypassAddress.ifBlank { getSimulatedIpForCountry(paisCanal) }
                customHeaders["X-Forwarded-For"] = ipToUse
                customHeaders["X-Real-IP"] = ipToUse
                customHeaders["Client-IP"] = ipToUse
                customHeaders["True-Client-IP"] = ipToUse
                customHeaders["CF-Connecting-IP"] = ipToUse
                customHeaders["Via"] = "1.1 vegur"
            }
            if (customHeaders.isNotEmpty()) {
                httpDataSourceFactory.setDefaultRequestProperties(customHeaders)
            }
            
            val dataSourceFactory = androidx.media3.datasource.DefaultDataSource.Factory(context, httpDataSourceFactory)
            val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dataSourceFactory)
            
            // Configure media sources
            val mediaItem = MediaItem.fromUri(Uri.parse(streamUrl))
            val mediaSource = mediaSourceFactory.createMediaSource(mediaItem)
            
            exoPlayer.setMediaSource(mediaSource)
            exoPlayer.prepare()
            exoPlayer.play()
        } catch (e: Exception) {
            isBuffering = false
            streamError = "Fallo al inicializar parámetros de transmisión: ${e.localizedMessage}"
        }
    }

    // Stop and Release Player cleanly on disposal
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    Box(modifier = modifier.background(Color.Black), contentAlignment = Alignment.Center) {
        // Core native player view surface
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, 
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay status header bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(vertical = 6.dp, horizontal = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00E676))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "REPRODUCIENDO EN VIVO: $canalNombre", 
                    color = Color(0xFF00E676), 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 9.sp
                )
            }
        }

        // Buffer Overlay indicator
        if (isBuffering) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = Color(0xFFE2A612), 
                        strokeWidth = 2.dp, 
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Conectando al servidor multimedia...", 
                        color = Color.White, 
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Señal original: $paisCanal", 
                        color = Color(0xFF8A91A5), 
                        fontSize = 9.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        // Beautiful Error panel handling Geo-blocking advice
        if (streamError != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = "👁️‍🌫️", fontSize = 32.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Text(
                        text = "Señal no disponible o bloqueada", 
                        color = Color.White, 
                        fontSize = 13.sp, 
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = streamError ?: "", 
                        color = Color(0xFFEF9A9A), 
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))

                    // Explicit Geo-block solution instructions
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF1E1719))
                            .border(0.5.dp, Color(0xFFEF5350).copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Column {
                            Text(
                                text = "💡 POSIBLE GEO-BLOQUEO DETECTADO",
                                color = Color(0xFFFF8A80),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Este canal proviene de $paisCanal${if (requiereVpn) " y tiene protecciones de IP estrictas" else ""}. Dado que juegas desde un emulador remoto, la CDN rechaza peticiones extranjeras.\n" +
                                        "👉 SOLUCIÓN: Abra su cliente VPN en su celular/PC físico, configure el túnel en el país de origen ($paisCanal) para otorgar IP local doméstica, y luego presione el botón REINTENTAR aquí abajo.",
                                color = Color(0xFFECEFF1),
                                fontSize = 9.sp,
                                lineHeight = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Action Buttons Row
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { reloadTrigger++ },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE2A612), contentColor = Color.Black),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(text = "REINTENTAR", fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}
