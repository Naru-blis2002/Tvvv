package com.example.ui.components

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.model.Channel

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerView(
    channel: Channel?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isBuffering by remember { mutableStateOf(false) }
    var playbackError by remember { mutableStateOf<String?>(null) }

    // Init ExoPlayer
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    // Listener for state and errors
    val playerListener = remember {
        object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    playbackError = null
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                playbackError = "Error al reproducir señal: " + (error.localizedMessage ?: "Formato incompatible o stream caído")
            }
        }
    }

    DisposableEffect(exoPlayer) {
        exoPlayer.addListener(playerListener)
        onDispose {
            exoPlayer.removeListener(playerListener)
            exoPlayer.release()
        }
    }

    // Monitor channel selection changes
    LaunchedEffect(channel) {
        playbackError = null
        if (channel != null) {
            val mediaItem = MediaItem.fromUri(channel.streamUrl)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.play()
        } else {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
        }
    }

    Box(
        modifier = modifier
            .aspectRatio(16f / 9f)
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (channel != null && playbackError == null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = true
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Overlay states: Empty stream
        if (channel == null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "No hay canal seleccionado",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Elige un canal de la parrilla inferior para iniciar el streaming",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Loading spinner
        if (isBuffering && channel != null && playbackError == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Conectando con la señal...",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Error message overlay
        playbackError?.let { error ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Error",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Error de Streaming",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = error,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}
