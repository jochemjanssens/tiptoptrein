package be.jochem.tiptoptrein.ui.slideshow

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import be.jochem.tiptoptrein.sound.TrainSounds
import be.jochem.tiptoptrein.trains.TrainKind
import be.jochem.tiptoptrein.trains.video
import be.jochem.tiptoptrein.trains.watchableTrains
import be.jochem.tiptoptrein.ui.common.EmojiButton
import be.jochem.tiptoptrein.ui.common.GameFrame
import be.jochem.tiptoptrein.ui.common.GameMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.max

private val kinds = watchableTrains
private const val PHOTO_SECONDS = 5

@Composable
fun SlideshowScreen(onHome: () -> Unit) {
  var index by remember { mutableIntStateOf(0) }
  var playing by remember { mutableStateOf(true) }
  var showVideo by remember { mutableStateOf(false) }
  var photoStep by remember { mutableIntStateOf(0) }
  // Horizontal slide of the photo card: 1 = off to the right, 0 = in place, -1 = off to the left.
  val slide = remember { Animatable(1f) }
  val kind = kinds[index]
  val photo = kind.photos[photoStep % kind.photos.size]

  // The slide whose entrance has finished, so pausing doesn't replay it.
  var shownIndex by remember { mutableIntStateOf(-1) }

  LaunchedEffect(index, playing, showVideo) {
    withContext(GameMotion) {
      if (shownIndex != index) {
        photoStep = 0
        slide.snapTo(1f)
        TrainSounds.play(kind.sound)
        slide.animateTo(0f, tween(700, easing = LinearOutSlowInEasing))
        TrainSounds.say(intro(index, kind) + kind.fact)
        shownIndex = index
      } else if (slide.value != 0f) {
        slide.animateTo(0f, tween(400))
      }
      if (!playing || showVideo) return@withContext
      // Show every photo of this train, and stay at least long enough to hear the fact.
      val total = max(kind.photos.size * PHOTO_SECONDS * 1000L, 6000L + kind.fact.length * 100L)
      var elapsed = 0L
      while (elapsed + PHOTO_SECONDS * 1000L <= total) {
        delay(PHOTO_SECONDS * 1000L)
        elapsed += PHOTO_SECONDS * 1000L
        if (elapsed < total) photoStep++
      }
      delay(total - elapsed)
      slide.animateTo(-1f, tween(500, easing = FastOutLinearInEasing))
      index = (index + 1) % kinds.size
    }
  }

  fun jump(delta: Int) {
    TrainSounds.stopSpeaking()
    index = (index + delta + kinds.size) % kinds.size
  }

  GameFrame(
    title = kind.title,
    onHome = onHome,
    actions = {
      kind.video?.let {
        EmojiButton("🎬", onClick = {
          TrainSounds.stopSpeaking()
          showVideo = true
        }, size = 56.dp, color = Color(0xFFFFC107))
        Spacer(Modifier.width(10.dp))
      }
      EmojiButton("🔊", onClick = { TrainSounds.say(intro(index, kind) + kind.fact) }, size = 56.dp)
      Spacer(Modifier.width(10.dp))
      EmojiButton(if (playing) "⏸️" else "▶️", onClick = { playing = !playing }, size = 56.dp)
    },
    background = {
      Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF81D4FA), Color(0xFF4FC3F7)))))
    },
  ) {
    BoxWithConstraints(Modifier.fillMaxSize().padding(start = 12.dp, end = 12.dp, top = 76.dp, bottom = 12.dp)) {
      val wide = maxWidth > 700.dp
      val shape = RoundedCornerShape(22.dp)
      // The real train fills the screen. Tap it for the next photo.
      Box(
        Modifier
          .fillMaxSize()
          .offset(x = maxWidth * slide.value)
          .shadow(10.dp, shape)
          .clip(shape)
          .background(Color.Black)
          .clickable(remember { MutableInteractionSource() }, indication = null) { photoStep++ },
      ) {
        Crossfade(photo, label = "photo") { shown ->
          Box(Modifier.fillMaxSize()) {
            Image(
              painterResource(shown.res),
              contentDescription = kind.title,
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize(),
            )
            Credit(shown.credit, Modifier.align(Alignment.TopEnd))
          }
        }
        // The fact, readable on top of the photo.
        Column(
          Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))))
            .padding(start = 96.dp, end = 96.dp, top = 28.dp, bottom = 12.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Text(
            kind.fact,
            fontSize = if (wide) 26.sp else 18.sp,
            lineHeight = if (wide) 32.sp else 23.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
          )
          Spacer(Modifier.height(8.dp))
          PhotoDots(kind.photos.size, photoStep % kind.photos.size)
        }
      }
      EmojiButton("⬅️", onClick = { jump(-1) }, size = 72.dp, modifier = Modifier.align(Alignment.CenterStart).padding(start = 10.dp))
      EmojiButton("➡️", onClick = { jump(1) }, size = 72.dp, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 10.dp))
    }
  }

  val video = kind.video
  if (showVideo && video != null) {
    VideoOverlay(video, onClose = { showVideo = false })
  }
}

/** Little dots showing which photo of the train is on screen. */
@Composable
private fun PhotoDots(count: Int, current: Int) {
  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    repeat(count) { i ->
      Box(
        Modifier
          .size(if (i == current) 14.dp else 10.dp)
          .background(if (i == current) Color.White else Color.White.copy(alpha = 0.45f), CircleShape),
      )
    }
  }
}

private val intros = listOf("Kijk eens! ", "Oh, wat mooi! ", "Goed kijken! ", "Daar is ")

/** A cheerful opening line before the fact, e.g. "Kijk eens! De Eurostar!". */
private fun intro(index: Int, kind: TrainKind): String {
  val start = intros[index % intros.size]
  return if (start == "Daar is ") "Daar is de ${kind.title}! " else "${start}De ${kind.title}! "
}

/** Photographer credit, required by the photos' Creative Commons licenses. */
@Composable
fun Credit(text: String, modifier: Modifier = Modifier) {
  Text(
    "$text · Wikimedia Commons",
    fontSize = 9.sp,
    lineHeight = 11.sp,
    maxLines = 1,
    overflow = TextOverflow.Ellipsis,
    color = Color.White,
    modifier = modifier
      .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(bottomStart = 6.dp))
      .padding(horizontal = 6.dp, vertical = 2.dp),
  )
}
