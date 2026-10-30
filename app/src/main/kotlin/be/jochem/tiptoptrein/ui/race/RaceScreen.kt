package be.jochem.tiptoptrein.ui.race

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import be.jochem.tiptoptrein.sound.Sfx
import be.jochem.tiptoptrein.sound.TrainSounds
import be.jochem.tiptoptrein.trains.Scenery
import be.jochem.tiptoptrein.trains.TrainKind
import be.jochem.tiptoptrein.trains.drawRails
import be.jochem.tiptoptrein.trains.drawScenery
import be.jochem.tiptoptrein.trains.drawTrain
import be.jochem.tiptoptrein.trains.isHighSpeed
import be.jochem.tiptoptrein.trains.locoLength
import be.jochem.tiptoptrein.trains.trainLength
import be.jochem.tiptoptrein.ui.common.BouncyButton
import be.jochem.tiptoptrein.ui.common.GameFrame
import be.jochem.tiptoptrein.ui.common.OutlinedTitle
import be.jochem.tiptoptrein.ui.common.StarCounter
import kotlinx.coroutines.delay
import kotlin.math.min

private val racers = TrainKind.entries.filter { it.isHighSpeed }

private const val RACE_METERS = 1000f
private const val MAX_KMH = 320f
private const val KMH_PER_TAP = 20f
private const val DRAG = 0.28f // fraction of speed lost per second
private const val RIVAL_KMH = 230f
private const val METERS_TO_UNITS = 0.55f
private const val FRONT = 24f // units from the train's screen anchor to its nose

private enum class Phase { PICK, COUNTDOWN, RACE, DONE }

@Composable
fun RaceScreen(onHome: () -> Unit) {
  var phase by remember { mutableStateOf(Phase.PICK) }
  var mine by remember { mutableStateOf(TrainKind.HIGH_SPEED) }
  var rival by remember { mutableStateOf(TrainKind.ICE) }
  var myPos by remember { mutableFloatStateOf(0f) }
  var rivalPos by remember { mutableFloatStateOf(0f) }
  var mySpeed by remember { mutableFloatStateOf(0f) }
  var rivalSpeed by remember { mutableFloatStateOf(0f) }
  var countdown by remember { mutableIntStateOf(3) }
  var won by remember { mutableStateOf(false) }
  var stars by remember { mutableIntStateOf(0) }
  var clock by remember { mutableFloatStateOf(0f) }
  var round by remember { mutableIntStateOf(0) }

  LaunchedEffect(Unit) { TrainSounds.say("Kies jouw supersnelle trein!") }

  LaunchedEffect(phase, round) {
    when (phase) {
      Phase.COUNTDOWN -> {
        myPos = 0f
        rivalPos = 0f
        mySpeed = 0f
        rivalSpeed = 0f
        TrainSounds.say("Tik zo snel als je kan!")
        delay(1800)
        for (n in 3 downTo 1) {
          countdown = n
          TrainSounds.play(Sfx.POP)
          TrainSounds.say(listOf("", "één", "twee", "drie")[n])
          delay(900)
        }
        countdown = 0
        TrainSounds.play(Sfx.WHISTLE)
        TrainSounds.say("Start!")
        phase = Phase.RACE
      }
      Phase.RACE -> {
        var last = withFrameNanos { it }
        while (phase == Phase.RACE) {
          withFrameNanos { now ->
            val dt = min(0.05f, (now - last) / 1_000_000_000f)
            last = now
            clock += dt
            mySpeed = (mySpeed - mySpeed * DRAG * dt).coerceAtLeast(0f)
            // The rival speeds up steadily, and eases off a little when far ahead so it stays exciting.
            val rivalTarget = if (rivalPos - myPos > 120f) RIVAL_KMH * 0.8f else RIVAL_KMH
            rivalSpeed += ((rivalTarget - rivalSpeed) * 0.6f * dt).coerceAtMost(40f * dt)
            myPos += mySpeed / 3.6f * dt
            rivalPos += rivalSpeed / 3.6f * dt
            if (myPos >= RACE_METERS || rivalPos >= RACE_METERS) {
              won = myPos >= rivalPos
              phase = Phase.DONE
            }
          }
        }
      }
      Phase.DONE -> {
        if (won) {
          stars++
          TrainSounds.play(Sfx.CORRECT)
          TrainSounds.say("Jij wint! Wat ben jij snel!")
        } else {
          TrainSounds.play(Sfx.WRONG)
          TrainSounds.say("Bijna! Nog een keer?")
        }
      }
      Phase.PICK -> {}
    }
  }

  fun start(kind: TrainKind) {
    mine = kind
    rival = racers.filter { it != kind }.random()
    phase = Phase.COUNTDOWN
    round++
  }

  GameFrame(
    title = "Supersnel",
    onHome = onHome,
    actions = { StarCounter(stars) },
    background = {
      Canvas(Modifier.fillMaxSize()) {
        val u = min(size.height / 52f, size.width / 70f)
        val scroll = myPos * METERS_TO_UNITS * u
        drawScenery(Scenery.COUNTRY, size.height * 0.36f, u, scroll = scroll * 0.6f, time = clock)
        if (phase == Phase.PICK) return@Canvas
        val laneTop = size.height * 0.56f
        val laneBottom = size.height * 0.84f
        val trainX = size.width * 0.22f
        val lanes = listOf(Triple(laneTop, rival, rivalPos), Triple(laneBottom, mine, myPos))
        for ((railY, _, _) in lanes) drawRails(railY, u, scroll)
        for ((railY, kind, pos) in lanes) {
          drawFinish(trainX + (RACE_METERS - myPos) * METERS_TO_UNITS * u + FRONT * u, railY, u)
          val len = trainLength(kind, 1) * u
          val x = trainX + (pos - myPos) * METERS_TO_UNITS * u + FRONT * u - len
          drawTrain(kind, x, railY, u, cars = 1, turn = pos * 2f, time = clock)
        }
        if (mySpeed > 180f) speedLines(laneBottom, u, clock)
      }
    },
  ) {
    when (phase) {
      Phase.PICK -> PickTrain(::start)
      else -> RaceHud(phase, countdown, mySpeed, won, onTap = {
        if (phase == Phase.RACE) mySpeed = (mySpeed + KMH_PER_TAP).coerceAtMost(MAX_KMH)
      }, onAgain = { start(mine) }, onOther = {
        phase = Phase.PICK
        TrainSounds.say("Kies jouw supersnelle trein!")
      })
    }
  }
}

@Composable
private fun PickTrain(onPick: (TrainKind) -> Unit) {
  BoxWithConstraints(Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, top = 84.dp, bottom = 16.dp)) {
    val landscape = maxWidth > maxHeight
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
      OutlinedTitle("Kies jouw trein", fontSize = 34)
      Spacer(Modifier.height(12.dp))
      val rows = racers.chunked(2)
      rows.forEach { row ->
        Row(Modifier.fillMaxWidth().weight(1f).padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
          row.forEach { kind ->
            BouncyButton(
              onClick = {
                TrainSounds.say(kind.title)
                onPick(kind)
              },
              modifier = Modifier.weight(1f).fillMaxHeight(),
              color = Color.White.copy(alpha = 0.92f),
              shape = RoundedCornerShape(24.dp),
            ) {
              Column(Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Canvas(Modifier.fillMaxWidth().weight(1f)) {
                  val len = locoLength(kind)
                  val u = min(size.height / 13f, size.width / (len + 3f))
                  val railY = size.height * 0.85f
                  drawRails(railY, u)
                  drawTrain(kind, (size.width - len * u) / 2f, railY, u, cars = 0)
                }
                Text(kind.title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF263238))
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun RaceHud(
  phase: Phase,
  countdown: Int,
  speed: Float,
  won: Boolean,
  onTap: () -> Unit,
  onAgain: () -> Unit,
  onOther: () -> Unit,
) {
  Box(Modifier.fillMaxSize()) {
    // speed in big numbers
    Text(
      "${speed.toInt()} km/u",
      fontSize = 44.sp,
      fontWeight = FontWeight.Black,
      color = Color(0xFF263238),
      modifier = Modifier
        .align(Alignment.TopCenter)
        .padding(top = 84.dp)
        .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(50))
        .padding(horizontal = 26.dp, vertical = 6.dp),
    )
    when (phase) {
      Phase.COUNTDOWN -> if (countdown > 0) {
        Text(
          "$countdown",
          fontSize = 160.sp,
          fontWeight = FontWeight.Black,
          color = Color(0xFFFFC107),
          modifier = Modifier.align(Alignment.Center),
        )
      }
      Phase.RACE -> {
        var pressed by remember { mutableStateOf(false) }
        Box(
          Modifier
            .align(Alignment.BottomEnd)
            .padding(24.dp)
            .size(170.dp)
            .scale(if (pressed) 0.88f else 1f)
            .shadow(12.dp, CircleShape)
            .background(Color(0xFFFFC107), CircleShape)
            .pointerInput(Unit) {
              detectTapGestures(onPress = {
                pressed = true
                onTap()
                tryAwaitRelease()
                pressed = false
              })
            },
          contentAlignment = Alignment.Center,
        ) {
          Text("⚡", fontSize = 90.sp)
        }
      }
      Phase.DONE -> {
        Column(
          Modifier
            .align(Alignment.Center)
            .shadow(12.dp, RoundedCornerShape(30.dp))
            .background(Color.White, RoundedCornerShape(30.dp))
            .padding(28.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          OutlinedTitle(if (won) "🏆 Jij wint! 🏆" else "Bijna!", fontSize = 44, color = Color(0xFFFFC107))
          Spacer(Modifier.height(16.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            BouncyButton(onClick = onAgain, modifier = Modifier.size(220.dp, 76.dp), color = Color(0xFF43A047), shape = RoundedCornerShape(38.dp)) {
              Text("🔁 Nog eens", fontSize = 26.sp, fontWeight = FontWeight.Black, color = Color.White, textAlign = TextAlign.Center)
            }
            BouncyButton(onClick = onOther, modifier = Modifier.size(220.dp, 76.dp), color = Color(0xFF1E88E5), shape = RoundedCornerShape(38.dp)) {
              Text("🚄 Andere", fontSize = 26.sp, fontWeight = FontWeight.Black, color = Color.White, textAlign = TextAlign.Center)
            }
          }
        }
      }
      Phase.PICK -> {}
    }
  }
}

private fun DrawScope.drawFinish(x: Float, railY: Float, u: Float) {
  if (x < -5f * u || x > size.width + 5f * u) return
  val sq = 1.2f * u
  for (i in 0..1) for (j in 0..9) {
    drawRect(
      if ((i + j) % 2 == 0) Color.Black else Color.White,
      Offset(x + i * sq, railY - 11f * u + j * sq),
      Size(sq, sq),
    )
  }
}

private fun DrawScope.speedLines(railY: Float, u: Float, time: Float) {
  for (k in 0 until 8) {
    val y = railY - (2f + k * 1.4f) * u
    val x = ((k * 173f - time * 2600f) % size.width + size.width) % size.width
    drawLine(Color.White.copy(alpha = 0.7f), Offset(x, y), Offset(x + 14f * u, y), 0.25f * u)
  }
}
