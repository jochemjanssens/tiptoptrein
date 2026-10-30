package be.jochem.tiptoptrein.ui.slideshow

import kotlinx.coroutines.withContext
import be.jochem.tiptoptrein.ui.common.GameMotion
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import be.jochem.tiptoptrein.sound.TrainSounds
import be.jochem.tiptoptrein.trains.TrainKind
import be.jochem.tiptoptrein.trains.drawRails
import be.jochem.tiptoptrein.trains.drawScenery
import be.jochem.tiptoptrein.trains.drawTrain
import be.jochem.tiptoptrein.trains.trainLength
import be.jochem.tiptoptrein.ui.common.EmojiButton
import be.jochem.tiptoptrein.ui.common.GameFrame
import be.jochem.tiptoptrein.ui.common.rememberClock
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.min

private val kinds = TrainKind.entries

@Composable
fun SlideshowScreen(onHome: () -> Unit) {
  var index by remember { mutableIntStateOf(0) }
  var playing by remember { mutableStateOf(true) }
  var bigPhoto by remember { mutableStateOf(false) }
  // Train position: 0 = fully off-screen left, 1 = centred, 2 = fully off-screen right.
  val position = remember { Animatable(0f) }
  val photoScale = remember { Animatable(0f) }
  val time by rememberClock()
  val kind = kinds[index]

  // The slide whose entrance animation has finished, so pausing doesn't replay it.
  var shownIndex by remember { mutableIntStateOf(-1) }

  LaunchedEffect(index, playing, bigPhoto) {
    withContext(GameMotion) {
    if (shownIndex != index) {
      photoScale.snapTo(0f)
      position.snapTo(0f)
      position.animateTo(1f, tween(2600, easing = LinearOutSlowInEasing))
      TrainSounds.play(kind.sound)
      launch { photoScale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 200f)) }
      delay(500)
      TrainSounds.say(intro(index, kind) + kind.fact)
      shownIndex = index
    }
    if (!playing || bigPhoto) {
      if (position.value != 1f) position.animateTo(1f, tween(800))
      return@withContext
    }
    delay((6000 + kind.fact.length * 100).toLong())
    launch { photoScale.animateTo(0f, tween(400)) }
    position.animateTo(2f, tween(1800, easing = FastOutLinearInEasing))
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
      EmojiButton("🔊", onClick = { TrainSounds.say(intro(index, kind) + kind.fact) }, size = 56.dp)
      Spacer(Modifier.width(10.dp))
      EmojiButton(if (playing) "⏸️" else "▶️", onClick = { playing = !playing }, size = 56.dp)
    },
    background = {
      Canvas(Modifier.fillMaxSize()) { drawScenery(kind.scenery, size.height, size.height / 30f, time = time) }
    },
  ) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
      val density = LocalDensity.current
      val landscape = maxWidth > maxHeight
      val wide = maxWidth > 700.dp
      // Phones in landscape are short: put the text beside the photo instead of below it.
      val compact = landscape && maxHeight < 500.dp
      val textHeight = when {
        compact -> 0.dp
        landscape -> 100.dp
        else -> 150.dp
      }
      val railY = maxHeight - textHeight - if (compact) 10.dp else 20.dp
      // The photo is the star of the slide; the drawn train gets a small strip above the text.
      val trainBand = maxHeight * if (landscape) 0.13f else 0.09f
      val trainLenUnits = trainLength(kind, kind.cars)
      val u = min(trainBand.value / 11f, (maxWidth.value - 32f) / (trainLenUnits + 4f))
      val trainTop = railY - (u * 11.5f).dp
      val photoTop = if (compact) 68.dp else 76.dp
      val photoBottom = trainTop - 4.dp
      // Fit a 3:2 photo frame into the space above the train (beside the text on phones).
      val photoMaxW = if (compact) maxWidth.value * 0.5f else maxWidth.value - 190f
      val frameH = min((photoBottom - photoTop).value, photoMaxW / 1.5f).coerceAtLeast(60f)
      val frameW = frameH * 1.5f
      val photoX = if (compact) 96.dp else (maxWidth - frameW.dp) / 2

      Canvas(
        Modifier.fillMaxSize().pointerInput(kind) {
          detectTapGestures { TrainSounds.play(kind.sound) }
        },
      ) {
        val uPx = with(density) { u.dp.toPx() }
        val railPx = with(density) { railY.toPx() }
        drawScenery(kind.scenery, railPx, uPx, time = time)
        drawRails(railPx, uPx)
        val len = trainLenUnits * uPx
        val p = position.value
        val centreX = (size.width - len) / 2f
        val x = if (p <= 1f) -len + (centreX + len) * p else centreX + (size.width - centreX) * (p - 1f)
        drawTrain(kind, x, railPx, uPx, cars = kind.cars, turn = x / uPx, time = time)
      }

      // The real train, as a photo card.
      Box(
        Modifier
          .offset(x = photoX, y = photoTop + ((photoBottom - photoTop).value - frameH).coerceAtLeast(0f).dp / 2)
          .size(frameW.dp, frameH.dp)
          .scale(photoScale.value)
          .rotate(-2f)
          .shadow(12.dp, RoundedCornerShape(10.dp))
          .background(Color.White, RoundedCornerShape(10.dp))
          .padding(8.dp)
          .clickable(remember { MutableInteractionSource() }, indication = null) { bigPhoto = true },
      ) {
        Image(
          painterResource(kind.photo.res),
          contentDescription = kind.title,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(6.dp)),
        )
        Credit(kind.photo.credit, Modifier.align(Alignment.BottomEnd))
      }

      val factModifier = if (compact) {
        Modifier
          .offset(x = photoX + frameW.dp + 16.dp, y = photoTop)
          .size(width = maxWidth - photoX - frameW.dp - 16.dp - 96.dp, height = photoBottom - photoTop)
      } else {
        Modifier
          .align(Alignment.BottomCenter)
          .fillMaxWidth()
          .padding(horizontal = if (landscape) 110.dp else 20.dp, vertical = 12.dp)
      }
      Box(
        factModifier
          .background(Color.White.copy(alpha = 0.92f), RoundedCornerShape(22.dp))
          .padding(horizontal = if (compact) 14.dp else 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          kind.fact,
          fontSize = if (wide && !compact) 26.sp else 19.sp,
          lineHeight = if (wide && !compact) 32.sp else 24.sp,
          fontWeight = FontWeight.Medium,
          color = Color(0xFF263238),
          textAlign = TextAlign.Center,
          modifier = Modifier.fillMaxWidth(),
        )
      }

      val arrowY = photoTop + (photoBottom - photoTop) / 2 - 36.dp
      EmojiButton("⬅️", onClick = { jump(-1) }, size = 72.dp, modifier = Modifier.offset(x = 12.dp, y = arrowY))
      EmojiButton(
        "➡️", onClick = { jump(1) }, size = 72.dp,
        modifier = Modifier.align(Alignment.TopEnd).offset(x = (-12).dp, y = arrowY),
      )
    }
  }

  if (bigPhoto) {
    Box(
      Modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = 0.92f))
        .clickable(remember { MutableInteractionSource() }, indication = null) { bigPhoto = false },
    ) {
      Image(
        painterResource(kind.photo.res),
        contentDescription = kind.title,
        contentScale = ContentScale.Fit,
        modifier = Modifier.fillMaxSize().padding(24.dp),
      )
      Credit(kind.photo.credit, Modifier.align(Alignment.BottomEnd).padding(24.dp))
      EmojiButton("✖️", onClick = { bigPhoto = false }, size = 64.dp, modifier = Modifier.align(Alignment.TopEnd).padding(20.dp))
    }
  }
}

/** Photographer credit, required by the photos' Creative Commons licenses. */
@Composable
private fun Credit(text: String, modifier: Modifier = Modifier) {
  Text(
    "$text",
    fontSize = 6.sp,
    lineHeight = 8.sp,
    maxLines = 1,
    overflow = TextOverflow.Ellipsis,
    color = Color.White,
    modifier = modifier
      .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(topStart = 6.dp))
      .padding(horizontal = 6.dp, vertical = 2.dp),
  )
}

private val intros = listOf("Kijk eens! ", "Oh, wat mooi! ", "Goed kijken! ", "Daar is ")

/** A cheerful opening line before the fact, e.g. "Wauw! De Eurostar!". */
private fun intro(index: Int, kind: TrainKind): String {
  val start = intros[index % intros.size]
  return if (start == "Daar is ") "Daar is de ${kind.title}! " else "${start}De ${kind.title}! "
}
