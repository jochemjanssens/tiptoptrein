package be.jochem.tiptoptrein.ui.drive

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import be.jochem.tiptoptrein.sound.Sfx
import be.jochem.tiptoptrein.sound.TrainSounds
import be.jochem.tiptoptrein.trains.TrainKind
import be.jochem.tiptoptrein.trains.drawRails
import be.jochem.tiptoptrein.trains.drawScenery
import be.jochem.tiptoptrein.trains.drawTrain
import be.jochem.tiptoptrein.trains.trainLength
import be.jochem.tiptoptrein.ui.common.EmojiButton
import be.jochem.tiptoptrein.ui.common.GameFrame
import be.jochem.tiptoptrein.ui.common.StarCounter
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private val stationNames = listOf(
  "Mechelen", "Brussel-Zuid", "Leuven", "Antwerpen-Centraal", "Gent-Sint-Pieters",
  "Brugge", "Oostende", "Hasselt", "Namen", "Opa en Oma",
)

private const val CARS = 2
private const val MAX_SPEED = 55f // units per second
private const val TRAIN_SCREEN_LEFT = 8f // units
private const val FIRST_STATION = 150f
private const val STATION_SPACING = 300f
private const val BOARDING_SECONDS = 3f

private val personColors = listOf(
  Color(0xFFE91E63), Color(0xFF3F51B5), Color(0xFF009688), Color(0xFFFF9800), Color(0xFF9C27B0),
)

private enum class Phase { DRIVING, BOARDING }

@Composable
fun DriveScreen(onHome: () -> Unit) {
  var kindIndex by remember { mutableIntStateOf(1) }
  val kind = TrainKind.entries[kindIndex]
  var worldX by remember { mutableFloatStateOf(0f) }
  var speed by remember { mutableFloatStateOf(0f) }
  var gas by remember { mutableStateOf(false) }
  var brake by remember { mutableStateOf(false) }
  var station by remember { mutableIntStateOf(0) }
  var phase by remember { mutableStateOf(Phase.DRIVING) }
  var boardTime by remember { mutableFloatStateOf(0f) }
  var stars by remember { mutableIntStateOf(0) }
  var clock by remember { mutableFloatStateOf(0f) }
  var message by remember { mutableStateOf("") }
  val text = rememberTextMeasurer()

  val trainLen = trainLength(kind, CARS)
  val platformLen = trainLen + 16f
  // The game loop below outlives recompositions; read the latest train through these.
  val loopKind by rememberUpdatedState(kind)
  val loopTrainLen by rememberUpdatedState(trainLen)
  fun stationStart(k: Int) = FIRST_STATION + k * STATION_SPACING
  fun stationName(k: Int) = stationNames[k % stationNames.size]

  LaunchedEffect(Unit) {
    TrainSounds.say("Jij bent de machinist! Druk op de groene knop. Stop bij het station.")
    message = "Naar ${stationName(0)}"
    var last = withFrameNanos { it }
    while (true) {
      withFrameNanos { now ->
        val dt = min(0.05f, (now - last) / 1_000_000_000f)
        last = now
        clock += dt
        when (phase) {
          Phase.DRIVING -> {
            val accel = when {
              brake -> -45f
              gas -> 16f
              else -> -3f
            }
            speed = (speed + accel * dt).coerceIn(0f, MAX_SPEED)
            worldX += speed * dt

            val front = worldX + loopTrainLen
            val start = stationStart(station)
            val zoneStart = start + loopTrainLen * 0.55f
            val zoneEnd = start + loopTrainLen + 16f + 6f
            if (speed == 0f && front in zoneStart..zoneEnd) {
              phase = Phase.BOARDING
              boardTime = 0f
              stars++
              TrainSounds.play(Sfx.CORRECT)
              TrainSounds.say("Goed gestopt! Instappen maar!")
              message = "⭐ Goed gestopt!"
            } else if (front > zoneEnd + 2f) {
              TrainSounds.play(Sfx.WRONG)
              station++
              TrainSounds.say("Oei, te ver! Op naar het volgende station.")
              message = "Naar ${stationName(station)}"
            }
          }
          Phase.BOARDING -> {
            boardTime += dt
            if (boardTime > BOARDING_SECONDS) {
              phase = Phase.DRIVING
              station++
              TrainSounds.play(loopKind.sound)
              TrainSounds.sayNext("Deuren dicht. Daar gaan we!")
              message = "Naar ${stationName(station)}"
            }
          }
        }
      }
    }
  }

  GameFrame(
    title = "Machinist",
    onHome = onHome,
    actions = {
      StarCounter(stars)
      Spacer(Modifier.width(10.dp))
      EmojiButton("🔄", onClick = {
        if (speed == 0f) {
          kindIndex = (kindIndex + 1) % TrainKind.entries.size
          TrainSounds.say(TrainKind.entries[kindIndex].title)
        }
      }, size = 56.dp)
    },
    background = {
      Canvas(Modifier.fillMaxSize()) {
        val u = min(size.height / 34f, size.width / (trainLen + 40f))
        val railY = size.height * 0.7f
        val scroll = worldX * u
        drawScenery(kind.scenery, railY, u, scroll = scroll, time = clock)

        // stations: the current target and the previous one (still visible while leaving)
        for (k in max(0, station - 1)..station + 1) {
          val sx = (stationStart(k) - worldX + TRAIN_SCREEN_LEFT) * u
          if (sx > size.width || sx + platformLen * u < -40f * u) continue
          val boarding = phase == Phase.BOARDING && k == station
          val waiting = if (k < station || (boarding && boardTime > BOARDING_SECONDS * 0.8f)) 0 else 3 + k % 3
          drawStation(sx, railY, u, platformLen, stationName(k), text, waiting, if (boarding) boardTime else 0f, k)
        }

        drawRails(railY, u, scroll)
        drawTrain(kind, TRAIN_SCREEN_LEFT * u, railY, u, cars = CARS, turn = worldX, time = clock)

        drawSpeedometer(Offset(size.width / 2f, size.height - 3.5f * u), 5f * u, speed / MAX_SPEED)
      }
    },
  ) {
    Text(
      message,
      fontSize = 24.sp,
      fontWeight = FontWeight.Bold,
      color = Color(0xFF263238),
      modifier = Modifier
        .align(Alignment.TopCenter)
        .padding(top = 80.dp)
        .background(Color.White.copy(alpha = 0.85f), RoundedCornerShape(50))
        .padding(horizontal = 20.dp, vertical = 6.dp),
    )
    Row(
      Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp),
      verticalAlignment = Alignment.Bottom,
    ) {
      HoldButton("🛑", "Rem", Color(0xFFE53935)) { brake = it }
      Spacer(Modifier.weight(1f))
      EmojiButton("📯", onClick = { TrainSounds.play(kind.sound) }, size = 76.dp, color = Color(0xFFFFC107), sound = null)
      Spacer(Modifier.width(150.dp))
      EmojiButton("🔔", onClick = { TrainSounds.play(Sfx.BELL) }, size = 76.dp, color = Color(0xFFFFC107), sound = null)
      Spacer(Modifier.weight(1f))
      HoldButton("🟢", "Gas", Color(0xFF43A047)) { gas = it }
    }
  }
}

/** A button that reports whether it is being held down. */
@Composable
private fun HoldButton(emoji: String, label: String, color: Color, onHold: (Boolean) -> Unit) {
  var held by remember { mutableStateOf(false) }
  Box(
    Modifier
      .size(110.dp)
      .scale(if (held) 0.9f else 1f)
      .shadow(8.dp, CircleShape)
      .background(if (held) color.copy(alpha = 0.75f) else color, CircleShape)
      .pointerInput(Unit) {
        detectTapGestures(onPress = {
          held = true
          onHold(true)
          tryAwaitRelease()
          held = false
          onHold(false)
        })
      },
    contentAlignment = Alignment.Center,
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(emoji, fontSize = 40.sp)
      Text(label, fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
    }
  }
}

private fun DrawScope.drawStation(
  x: Float,
  railY: Float,
  u: Float,
  platformLen: Float,
  name: String,
  text: TextMeasurer,
  waiting: Int,
  boardTime: Float,
  seed: Int,
) {
  // building
  val bx = x + platformLen * u * 0.3f
  val bw = 26f * u
  val top = railY - 19f * u
  drawRect(Color(0xFFBF5B3C), Offset(bx, top), Size(bw, 15f * u))
  val roof = Path().apply {
    moveTo(bx - 2f * u, top)
    lineTo(bx + bw / 2, top - 7f * u)
    lineTo(bx + bw + 2f * u, top)
    close()
  }
  drawPath(roof, Color(0xFF5D4037))
  // clock
  val cc = Offset(bx + bw / 2, top - 2.6f * u)
  drawCircle(Color.White, 2f * u, cc)
  drawCircle(Color(0xFF263238), 2f * u, cc, style = Stroke(0.3f * u))
  drawLine(Color(0xFF263238), cc, cc + Offset(0f, -1.4f * u), 0.3f * u, StrokeCap.Round)
  drawLine(Color(0xFF263238), cc, cc + Offset(1.1f * u, 0f), 0.3f * u, StrokeCap.Round)
  // windows and door
  for (k in 0..3) {
    if (k == 1 || k == 2) continue
    drawRoundRect(Color(0xFFFFF59D), Offset(bx + (2f + k * 6f) * u, top + 3f * u), Size(4f * u, 5f * u), CornerRadius(2f * u))
  }
  drawRoundRect(Color(0xFF4E342E), Offset(bx + 10.5f * u, top + 6f * u), Size(5f * u, 9f * u), CornerRadius(2.5f * u))

  // name board
  val style = TextStyle(fontSize = (2.4f * u / density).sp, fontWeight = FontWeight.Black, color = Color.White)
  val layout = text.measure(name, style)
  val boardW = layout.size.width + 3f * u
  val boardX = x + 4f * u
  val boardY = railY - 18f * u
  drawRect(Color(0xFF616161), Offset(boardX + 1f * u, boardY), Size(0.6f * u, 15f * u))
  drawRect(Color(0xFF616161), Offset(boardX + boardW - 1.6f * u, boardY), Size(0.6f * u, 15f * u))
  drawRoundRect(Color(0xFF00205B), Offset(boardX, boardY), Size(boardW, layout.size.height + 1.2f * u), CornerRadius(0.6f * u))
  drawText(layout, topLeft = Offset(boardX + 1.5f * u, boardY + 0.6f * u))

  // platform
  drawRect(Color(0xFFBDBDBD), Offset(x, railY - 4f * u), Size(platformLen * u, 4f * u))
  drawRect(Color(0xFFFFEB3B), Offset(x, railY - 4f * u), Size(platformLen * u, 0.5f * u))

  // stop marker at the end of the platform
  val sx = x + platformLen * u
  drawRect(Color(0xFF616161), Offset(sx, railY - 14f * u), Size(0.6f * u, 10f * u))
  drawCircle(Color(0xFFE53935), 2.6f * u, Offset(sx + 0.3f * u, railY - 15f * u))
  drawCircle(Color.White, 1.6f * u, Offset(sx + 0.3f * u, railY - 15f * u))
  drawRect(Color(0xFFE53935), Offset(sx - 1f * u, railY - 15.4f * u), Size(2.6f * u, 0.8f * u))

  // waiting passengers; while boarding they walk toward the train and shrink away
  val progress = (boardTime / (BOARDING_SECONDS * 0.8f)).coerceIn(0f, 1f)
  for (p in 0 until waiting) {
    val px = x + (platformLen * 0.25f + p * 7f) * u
    val bob = if (boardTime > 0f) sin(boardTime * 12f + p) * 0.4f * u else 0f
    val py = railY - 4f * u + progress * 2.5f * u + bob
    val s = 1f - progress * 0.7f
    drawPerson(Offset(px, py), u * s, personColors[(p + seed) % personColors.size])
  }
}

private fun DrawScope.drawPerson(feet: Offset, u: Float, color: Color) {
  drawLine(Color(0xFF37474F), feet + Offset(-0.6f * u, 0f), feet + Offset(-0.4f * u, -2.4f * u), 0.5f * u, StrokeCap.Round)
  drawLine(Color(0xFF37474F), feet + Offset(0.6f * u, 0f), feet + Offset(0.4f * u, -2.4f * u), 0.5f * u, StrokeCap.Round)
  drawRoundRect(color, feet + Offset(-1.2f * u, -6f * u), Size(2.4f * u, 3.8f * u), CornerRadius(1f * u))
  drawCircle(Color(0xFFFFCC80), 1.2f * u, feet + Offset(0f, -7.3f * u))
}

private fun DrawScope.drawSpeedometer(centre: Offset, r: Float, fraction: Float) {
  drawCircle(Color.White.copy(alpha = 0.9f), r * 1.15f, centre)
  val colors = listOf(Color(0xFF43A047), Color(0xFFFDD835), Color(0xFFE53935))
  colors.forEachIndexed { i, c ->
    drawArc(
      c, startAngle = 180f + i * 60f, sweepAngle = 60f, useCenter = false,
      topLeft = centre - Offset(r * 0.85f, r * 0.85f), size = Size(r * 1.7f, r * 1.7f),
      style = Stroke(r * 0.22f),
    )
  }
  val a = PI + fraction * PI
  val tip = centre + Offset((cos(a) * r * 0.8f).toFloat(), (sin(a) * r * 0.8f).toFloat())
  drawLine(Color(0xFF263238), centre, tip, r * 0.1f, StrokeCap.Round)
  drawCircle(Color(0xFF263238), r * 0.14f, centre)
}
