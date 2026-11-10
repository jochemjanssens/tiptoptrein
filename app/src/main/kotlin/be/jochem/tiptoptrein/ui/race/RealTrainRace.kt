package be.jochem.tiptoptrein.ui.race

import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.sin
import be.jochem.tiptoptrein.trains.cutout
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import be.jochem.tiptoptrein.sound.Sfx
import be.jochem.tiptoptrein.sound.TrainSounds
import be.jochem.tiptoptrein.trains.Scenery
import be.jochem.tiptoptrein.trains.TrainKind
import be.jochem.tiptoptrein.trains.drawRails
import be.jochem.tiptoptrein.trains.drawScenery
import be.jochem.tiptoptrein.trains.topSpeed
import be.jochem.tiptoptrein.trains.watchableTrains
import be.jochem.tiptoptrein.ui.common.BouncyButton
import be.jochem.tiptoptrein.ui.common.GameFrame
import be.jochem.tiptoptrein.ui.common.OutlinedTitle
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min

private const val RACE_SECONDS = 7f // the faster train always needs about this long
private const val SPEED_UP_SECONDS = 2.5f
private const val LANE_TOP = 0.52f // rail heights, as a fraction of the screen
private const val LANE_BOTTOM = 0.88f
private const val PHOTO_HEIGHT = 0.17f // height of a real train on the track, as a fraction of the screen

/** Size unit for the rails and scenery. */
private fun raceUnit(width: Float, height: Float) = min(height / 64f, width / 80f)

/** Size of a cut-out train on screen: a fixed height, but never wider than most of the screen. */
internal fun photoSize(image: ImageBitmap, screenW: Float, screenH: Float): Size {
  val aspect = image.width.toFloat() / image.height
  val h = min(screenH * PHOTO_HEIGHT, screenW * 0.62f / aspect)
  return Size(h * aspect, h)
}

private enum class Stage { PICK, COUNTDOWN, RACE, DONE }

/** Pick two real trains and watch them race, each at its real top speed. */
@Composable
fun RealTrainRace(onHome: () -> Unit) {
  var stage by remember { mutableStateOf(Stage.PICK) }
  var first by remember { mutableStateOf<TrainKind?>(null) }
  var second by remember { mutableStateOf<TrainKind?>(null) }
  // 0 = start, 1 = finish
  var firstPos by remember { mutableFloatStateOf(0f) }
  var secondPos by remember { mutableFloatStateOf(0f) }
  var elapsed by remember { mutableFloatStateOf(0f) }
  var scroll by remember { mutableFloatStateOf(0f) }
  var countdown by remember { mutableIntStateOf(0) }
  var round by remember { mutableIntStateOf(0) }

  fun speedAt(kind: TrainKind) = kind.topSpeed * min(1f, elapsed / SPEED_UP_SECONDS)

  LaunchedEffect(stage, round) {
    when (stage) {
      Stage.PICK -> {
        first = null
        second = null
        TrainSounds.say("Kies twee treinen. Welke is het snelst?")
      }
      Stage.COUNTDOWN -> {
        firstPos = 0f
        secondPos = 0f
        elapsed = 0f
        TrainSounds.say("De ${first!!.title} tegen de ${second!!.title}!")
        delay(2600)
        for (n in 3 downTo 1) {
          countdown = n
          TrainSounds.play(Sfx.POP)
          TrainSounds.say(listOf("", "één", "twee", "drie")[n])
          delay(900)
        }
        countdown = 0
        TrainSounds.play(Sfx.WHISTLE)
        stage = Stage.RACE
      }
      Stage.RACE -> {
        val a = first!!
        val b = second!!
        val fastest = max(a.topSpeed, b.topSpeed).toFloat()
        // Scale time so the faster train finishes in RACE_SECONDS; the slower one keeps its real ratio.
        val perKmh = 1f / fastest / (RACE_SECONDS - SPEED_UP_SECONDS / 2f)
        var last = withFrameNanos { it }
        while (stage == Stage.RACE) {
          withFrameNanos { now ->
            val dt = min(0.05f, (now - last) / 1_000_000_000f)
            last = now
            elapsed += dt
            firstPos += speedAt(a) * perKmh * dt
            secondPos += speedAt(b) * perKmh * dt
            scroll += max(speedAt(a), speedAt(b)) * dt
            if (firstPos >= 1f || secondPos >= 1f) stage = Stage.DONE
          }
        }
      }
      Stage.DONE -> {
        val a = first!!
        val b = second!!
        val winner = if (a.topSpeed >= b.topSpeed) a else b
        TrainSounds.play(Sfx.CORRECT)
        TrainSounds.say(
          if (a.topSpeed == b.topSpeed) "Even snel! Allebei ${a.topSpeed} kilometer per uur!"
          else "De ${winner.title} wint! Hij rijdt wel ${winner.topSpeed} kilometer per uur!",
        )
      }
    }
  }

  // The real trains, cut out of their photos.
  val firstImage = first?.cutout?.let { ImageBitmap.imageResource(it) }
  val secondImage = second?.cutout?.let { ImageBitmap.imageResource(it) }

  GameFrame(
    title = "Echte treinen racen",
    onHome = onHome,
    background = {
      Canvas(Modifier.fillMaxSize()) {
        val u = raceUnit(size.width, size.height)
        drawScenery(Scenery.COUNTRY, size.height * 0.36f, u, scroll = scroll * u * 0.15f, time = elapsed)
        val a = first
        val b = second
        if (stage == Stage.PICK || a == null || b == null) return@Canvas
        val startNose = size.width * 0.3f
        val finish = size.width * 0.9f
        val lanes = listOf(
          Triple(size.height * LANE_TOP, secondImage, secondPos),
          Triple(size.height * LANE_BOTTOM, firstImage, firstPos),
        )
        for ((laneIndex, lane) in lanes.withIndex()) {
          val (railY, image, pos) = lane
          drawRails(railY, u, scroll * u * 0.5f)
          drawFinish(finish, railY, u)
          if (image == null) continue
          val nose = startNose + (finish - startNose) * pos.coerceAtMost(1f)
          drawRealTrain(image, nose, railY, u, rumble = stage == Stage.RACE, phase = elapsed * 40f + laneIndex * 2f)
        }
        if (stage == Stage.RACE && elapsed > SPEED_UP_SECONDS) speedLines(size.height * LANE_BOTTOM, u, elapsed)
      }
    },
  ) {
    when (stage) {
      Stage.PICK -> PickTwo(first) { kind ->
        TrainSounds.say(kind.title)
        if (first == null) {
          first = kind
        } else if (kind != first) {
          second = kind
          stage = Stage.COUNTDOWN
          round++
        }
      }
      else -> BoxWithConstraints(Modifier.fillMaxSize()) {
        val a = first ?: return@BoxWithConstraints
        val b = second ?: return@BoxWithConstraints
        // Name, photo and live speed just above each train.
        val trainHeight = (maxHeight.value * PHOTO_HEIGHT).dp
        LaneLabel(b, speedAt(b), Modifier.offset(x = 16.dp, y = maxHeight * LANE_TOP - trainHeight - 60.dp))
        LaneLabel(a, speedAt(a), Modifier.offset(x = 16.dp, y = maxHeight * LANE_BOTTOM - trainHeight - 60.dp))
        if (stage == Stage.COUNTDOWN && countdown > 0) {
          Text("$countdown", fontSize = 150.sp, fontWeight = FontWeight.Black, color = Color(0xFFFFC107), modifier = Modifier.align(Alignment.Center))
        }
        if (stage == Stage.DONE) {
          Result(a, b, maxHeight, onAgain = {
            stage = Stage.COUNTDOWN
            round++
          }, onOther = { stage = Stage.PICK })
        }
      }
    }
  }
}

@Composable
private fun LaneLabel(kind: TrainKind, kmh: Float, modifier: Modifier) {
  Row(
    modifier
      .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(50))
      .padding(end = 16.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Image(
      painterResource(kind.photos.first().res),
      contentDescription = kind.title,
      contentScale = ContentScale.Crop,
      modifier = Modifier.size(52.dp).clip(CircleShape).border(3.dp, Color.White, CircleShape),
    )
    Spacer(Modifier.width(10.dp))
    Text(kind.title, fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color(0xFF263238))
    Spacer(Modifier.width(12.dp))
    Text("${kmh.toInt()} km/u", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD84315))
  }
}

/** Photo cards of every train; the first pick gets a yellow border and a "1". */
@Composable
private fun PickTwo(first: TrainKind?, onPick: (TrainKind) -> Unit) {
  BoxWithConstraints(Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, top = 80.dp, bottom = 16.dp)) {
    val columns = if (maxWidth > maxHeight) 4 else 2
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
      OutlinedTitle(if (first == null) "Kies twee treinen" else "En de andere trein?", fontSize = 28)
      Spacer(Modifier.height(10.dp))
      watchableTrains.chunked(columns).forEach { row ->
        Row(Modifier.fillMaxWidth().weight(1f).padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
          row.forEach { kind ->
            val picked = kind == first
            BouncyButton(
              onClick = { onPick(kind) },
              modifier = Modifier.weight(1f).fillMaxHeight()
                .then(if (picked) Modifier.border(6.dp, Color(0xFFFFC107), RoundedCornerShape(20.dp)) else Modifier),
              color = Color.White,
              shape = RoundedCornerShape(20.dp),
            ) {
              Box(Modifier.fillMaxSize().padding(5.dp).clip(RoundedCornerShape(16.dp))) {
                Image(
                  painterResource(kind.photos.first().res),
                  contentDescription = kind.title,
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.fillMaxSize(),
                )
                Text(
                  kind.title,
                  fontSize = 20.sp,
                  fontWeight = FontWeight.Black,
                  color = Color.White,
                  textAlign = TextAlign.Center,
                  modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.55f)).padding(vertical = 4.dp),
                )
                if (picked) {
                  Text(
                    "1",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF263238),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.TopStart).padding(6.dp).size(40.dp)
                      .background(Color(0xFFFFC107), CircleShape).padding(top = 2.dp),
                  )
                }
              }
            }
          }
          // keep cards the same width on the last, shorter row
          repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
        }
      }
    }
  }
}

@Composable
private fun Result(a: TrainKind, b: TrainKind, maxHeight: Dp, onAgain: () -> Unit, onOther: () -> Unit) {
  val tie = a.topSpeed == b.topSpeed
  val winner = if (a.topSpeed >= b.topSpeed) a else b
  val loser = if (winner == a) b else a
  val photoH = min(240f, maxHeight.value * 0.36f).dp
  Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Column(
      Modifier
        .padding(top = 64.dp)
        .shadow(12.dp, RoundedCornerShape(30.dp))
        .background(Color.White, RoundedCornerShape(30.dp))
        .padding(20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      if (!tie) {
        Image(
          painterResource(winner.photos.first().res),
          contentDescription = winner.title,
          contentScale = ContentScale.Crop,
          modifier = Modifier.size(width = photoH * 1.5f, height = photoH).clip(RoundedCornerShape(18.dp)),
        )
        Spacer(Modifier.height(10.dp))
      }
      OutlinedTitle(if (tie) "Even snel!" else "🏆 ${winner.title} wint!", fontSize = 34, color = Color(0xFFFFC107))
      Text(
        if (tie) "Allebei ${a.topSpeed} km/u" else "${winner.topSpeed} km/u tegen ${loser.topSpeed} km/u",
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF263238),
      )
      Spacer(Modifier.height(14.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        BouncyButton(onClick = onAgain, modifier = Modifier.size(220.dp, 68.dp), color = Color(0xFF43A047), shape = RoundedCornerShape(34.dp)) {
          Text("🔁 Nog eens", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White)
        }
        BouncyButton(onClick = onOther, modifier = Modifier.size(220.dp, 68.dp), color = Color(0xFF1E88E5), shape = RoundedCornerShape(34.dp)) {
          Text("🔀 Andere", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White)
        }
      }
    }
  }
}

/** Draws a real, cut-out train with its nose at x [nose] and its wheels on the rail at [railY]. */
internal fun DrawScope.drawRealTrain(image: ImageBitmap, nose: Float, railY: Float, u: Float, rumble: Boolean, phase: Float) {
  val sz = photoSize(image, size.width, size.height)
  // A little rumble while it drives, so the photo feels like it is moving.
  val shake = if (rumble) sin(phase) * 0.15f * u else 0f
  drawImage(
    image,
    dstOffset = IntOffset((nose - sz.width).toInt(), (railY + 0.4f * u - sz.height + shake).toInt()),
    dstSize = IntSize(sz.width.toInt(), sz.height.toInt()),
  )
}
