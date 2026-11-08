package be.jochem.tiptoptrein.ui.slideshow

import android.graphics.Color as AndroidColor
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.RawResourceDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import be.jochem.tiptoptrein.trains.Video
import be.jochem.tiptoptrein.ui.common.EmojiButton

/** Plays a train film full screen. It stops on its own at the end; tap 🔁 to watch it again. */
@OptIn(UnstableApi::class)
@Composable
fun VideoOverlay(video: Video, onClose: () -> Unit) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  var ended by remember { mutableStateOf(false) }
  val player = remember(video) {
    ExoPlayer.Builder(context).build().apply {
      setAudioAttributes(
        AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(),
        true,
      )
      setMediaItem(MediaItem.fromUri(RawResourceDataSource.buildRawResourceUri(video.res)))
      prepare()
      playWhenReady = true
    }
  }
  DisposableEffect(player, lifecycleOwner) {
    val listener = object : Player.Listener {
      override fun onPlaybackStateChanged(state: Int) {
        ended = state == Player.STATE_ENDED
      }
    }
    // Don't keep playing in the background when the app is left.
    val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_PAUSE) player.pause() }
    player.addListener(listener)
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
      player.removeListener(listener)
      player.release()
    }
  }

  Box(
    Modifier
      .fillMaxSize()
      .background(Color.Black)
      // Swallow taps so they don't reach the slideshow underneath.
      .clickable(remember { MutableInteractionSource() }, indication = null) {},
  ) {
    AndroidView(
      factory = { ctx ->
        PlayerView(ctx).apply {
          this.player = player
          useController = false
          resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
          setShutterBackgroundColor(AndroidColor.BLACK)
        }
      },
      modifier = Modifier.fillMaxSize(),
    )
    Box(Modifier.fillMaxSize().safeDrawingPadding()) {
      EmojiButton("✖️", onClick = onClose, size = 64.dp, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp))
      if (ended) {
        EmojiButton("🔁", onClick = {
          player.seekTo(0)
          player.play()
        }, size = 110.dp, color = Color(0xFFFFC107), modifier = Modifier.align(Alignment.Center))
      }
      Credit(video.credit, Modifier.align(Alignment.BottomEnd).padding(16.dp))
    }
  }
}
