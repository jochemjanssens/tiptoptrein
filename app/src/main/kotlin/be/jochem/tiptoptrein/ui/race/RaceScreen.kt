package be.jochem.tiptoptrein.ui.race

import be.jochem.tiptoptrein.trains.cutout
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.Image
import android.content.Context
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
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
import kotlin.math.sin

private val racers = TrainKind.entries.filter { it.isHighSpeed }

private const val MAX_KMH = 320f
private const val TURBO_KMH = 380f
private const val TURBO_SECONDS = 3f
private const val TAPS_FOR_TURBO = 14f
private const val KMH_PER_TAP = 20f
private const val DRAG = 0.28f // fraction of speed lost per second
private const val METERS_TO_UNITS = 0.55f
private const val FRONT = 24f // units from the train's screen anchor to its nose
private const val PREFS = "race"

/** A rail journey from Brussels. Each one is longer, and has a faster rival, than the one before. */
private enum class Route(
  val title: String,
  val emoji: String,
  val spoken: String,
  val meters: Float,
  val rivalKmh: Float,
  val scenery: Scenery,
  val tunnels: List<ClosedFloatingPointRange<Float>>,
  val underSea: Boolean = false,
) {
  PARIJS("Parijs", "🗼", "Naar Parijs!", 900f, 200f, Scenery.COUNTRY, listOf(380f..520f)),
  KEULEN("Keulen", "⛪", "Naar Keulen!", 1100f, 225f, Scenery.MOUNTAIN, listOf(250f..400f, 700f..820f)),
  LONDEN("Londen", "🌊", "Naar Londen, onder de zee!", 1300f, 245f, Scenery.BEACH, listOf(350f..950f), underSea = true),
}

private enum class Phase { PICK, ROUTE, COUNTDOWN, RACE, DONE }

/** Supersnel: race yourself by tapping, or watch two real trains race at their real speeds. */
@Composable
fun RaceScreen(onHome: () -> Unit) {
  var mode by remember { mutableStateOf<RaceMode?>(null) }
  when (mode) {
    null -> GameFrame(title = "Supersnel", onHome = onHome, background = {
      Canvas(Modifier.fillMaxSize()) {
        val u = min(size.height / 52f, size.width / 70f)
        drawScenery(Scenery.COUNTRY, size.height * 0.36f, u)
      }
    }) {
      ChooseRaceMode { mode = it }
    }
    RaceMode.TAP -> TapRace(onHome)
    RaceMode.REAL -> RealTrainRace(onHome)
  }
}

enum class RaceMode { TAP, REAL }

@Composable
private fun ChooseRaceMode(onPick: (RaceMode) -> Unit) {
  LaunchedEffect(Unit) { TrainSounds.say("Zelf racen, of echte treinen laten racen?") }
  BoxWithConstraints(Modifier.fillMaxSize().padding(start = 20.dp, end = 20.dp, top = 84.dp, bottom = 20.dp)) {
    val option: @Composable (RaceMode, Modifier) -> Unit = { m, mod ->
      BouncyButton(
        onClick = {
          TrainSounds.say(if (m == RaceMode.TAP) "Zelf racen!" else "Echte treinen!")
          onPick(m)
        },
        modifier = mod,
        color = Color.White.copy(alpha = 0.95f),
        shape = RoundedCornerShape(28.dp),
      ) {
        Column(Modifier.fillMaxSize().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
          Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
            if (m == RaceMode.REAL) {
              Image(
                painterResource(TrainKind.TGV.photos.first().res),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
              )
            } else {
              Text("⚡", fontSize = 90.sp)
            }
          }
          Spacer(Modifier.height(10.dp))
          Text(
            if (m == RaceMode.TAP) "⚡ Zelf racen" else "📷 Echte treinen",
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF263238),
          )
        }
      }
    }
    if (maxWidth > maxHeight) {
      Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        option(RaceMode.TAP, Modifier.weight(1f).fillMaxHeight())
        option(RaceMode.REAL, Modifier.weight(1f).fillMaxHeight())
      }
    } else {
      Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        option(RaceMode.TAP, Modifier.weight(1f).fillMaxWidth())
        option(RaceMode.REAL, Modifier.weight(1f).fillMaxWidth())
      }
    }
  }
}

/** Race yourself: tap as fast as you can to beat the other train. */
@Composable
private fun TapRace(onHome: () -> Unit) {
  val context = LocalContext.current
  val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
  var phase by remember { mutableStateOf(Phase.PICK) }
  var mine by remember { mutableStateOf(TrainKind.HIGH_SPEED) }
  var rival by remember { mutableStateOf(TrainKind.ICE) }
  var route by remember { mutableStateOf(Route.PARIJS) }
  var wonRoutes by remember { mutableStateOf(prefs.getStringSet("won", emptySet()).orEmpty().toSet()) }
  var myPos by remember { mutableFloatStateOf(0f) }
  var rivalPos by remember { mutableFloatStateOf(0f) }
  var mySpeed by remember { mutableFloatStateOf(0f) }
  var rivalSpeed by remember { mutableFloatStateOf(0f) }
  var turboCharge by remember { mutableFloatStateOf(0f) } // 0..1, full means ready
  var turboLeft by remember { mutableFloatStateOf(0f) } // seconds of boost remaining
  var countdown by remember { mutableIntStateOf(3) }
  var won by remember { mutableStateOf(false) }
  var champion by remember { mutableStateOf(false) }
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
        turboCharge = 0f
        turboLeft = 0f
        TrainSounds.say("${route.spoken} Tik zo snel als je kan!")
        delay(2800)
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
        var nextTunnel = 0
        var last = withFrameNanos { it }
        while (phase == Phase.RACE) {
          withFrameNanos { now ->
            val dt = min(0.05f, (now - last) / 1_000_000_000f)
            last = now
            clock += dt
            if (turboLeft > 0f) {
              turboLeft = (turboLeft - dt).coerceAtLeast(0f)
              mySpeed += (TURBO_KMH - mySpeed) * 3f * dt
            } else {
              mySpeed = (mySpeed - mySpeed * DRAG * dt).coerceAtLeast(0f)
            }
            // The rival speeds up steadily, and eases off a little when far ahead so it stays exciting.
            val rivalTarget = if (rivalPos - myPos > 120f) route.rivalKmh * 0.8f else route.rivalKmh
            rivalSpeed += ((rivalTarget - rivalSpeed) * 0.6f * dt).coerceAtMost(40f * dt)
            myPos += mySpeed / 3.6f * dt
            rivalPos += rivalSpeed / 3.6f * dt
            if (nextTunnel < route.tunnels.size && myPos >= route.tunnels[nextTunnel].start - 60f) {
              TrainSounds.say(if (route.underSea) "Nu gaan we onder de zee!" else "Een tunnel!")
              nextTunnel++
            }
            if (myPos >= route.meters || rivalPos >= route.meters) {
              won = myPos >= rivalPos
              phase = Phase.DONE
            }
          }
        }
      }
      Phase.DONE -> {
        if (won) {
          stars++
          val hadCup = wonRoutes.size == Route.entries.size
          wonRoutes = wonRoutes + route.name
          prefs.edit().putStringSet("won", wonRoutes).apply()
          champion = !hadCup && wonRoutes.size == Route.entries.size
          TrainSounds.play(Sfx.CORRECT)
          TrainSounds.say(
            if (champion) "Je hebt alle ritten gewonnen! Jij bent de kampioen!"
            else "Jij wint! Je bent in ${route.title}!",
          )
        } else {
          champion = false
          TrainSounds.play(Sfx.WRONG)
          TrainSounds.say("Bijna! Nog een keer?")
        }
      }
      Phase.PICK, Phase.ROUTE -> {}
    }
  }

  fun pickTrain(kind: TrainKind) {
    mine = kind
    phase = Phase.ROUTE
    TrainSounds.sayNext("Waar rijden we naartoe?")
  }

  fun startRoute(r: Route) {
    route = r
    rival = racers.filter { it != mine }.random()
    phase = Phase.COUNTDOWN
    round++
  }

  // The real trains, cut out of their photos.
  val mineImage = mine.cutout?.let { ImageBitmap.imageResource(it) }
  val rivalImage = rival.cutout?.let { ImageBitmap.imageResource(it) }

  GameFrame(
    title = "Supersnel",
    onHome = onHome,
    actions = { StarCounter(stars) },
    background = {
      Canvas(Modifier.fillMaxSize()) {
        val u = min(size.height / 52f, size.width / 70f)
        val scroll = myPos * METERS_TO_UNITS * u
        val racing = phase != Phase.PICK && phase != Phase.ROUTE
        val scenery = if (racing) route.scenery else Scenery.COUNTRY
        drawScenery(scenery, size.height * 0.36f, u, scroll = scroll * 0.6f, time = clock)
        if (!racing) return@Canvas
        val laneTop = size.height * 0.56f
        val laneBottom = size.height * 0.84f
        val trainX = size.width * 0.22f
        fun screenX(meters: Float) = trainX + FRONT * u + (meters - myPos) * METERS_TO_UNITS * u
        route.tunnels.forEach { drawTunnel(screenX(it.start), screenX(it.endInclusive), route.underSea, u, clock) }
        val lanes = listOf(Triple(laneTop, rival, rivalPos), Triple(laneBottom, mine, myPos))
        val images = mapOf(rival to rivalImage, mine to mineImage)
        for ((railY, _, _) in lanes) drawRails(railY, u, scroll)
        for ((railY, kind, pos) in lanes) {
          drawFinish(screenX(route.meters), railY, u)
          images[kind]?.let { drawRealTrain(it, screenX(pos), railY, u, rumble = phase == Phase.RACE, phase = clock * 40f + railY) }
        }
        if (mySpeed > 180f) speedLines(laneBottom, u, clock)
      }
    },
  ) {
    when (phase) {
      Phase.PICK -> PickTrain(::pickTrain)
      Phase.ROUTE -> PickRoute(wonRoutes, ::startRoute, onOtherTrain = {
        phase = Phase.PICK
        TrainSounds.say("Kies jouw supersnelle trein!")
      })
      else -> RaceHud(
        phase, countdown, mySpeed, won, champion,
        myProgress = myPos / route.meters,
        rivalProgress = rivalPos / route.meters,
        turboCharge = turboCharge,
        turboOn = turboLeft > 0f,
        onTap = {
          if (phase == Phase.RACE) {
            mySpeed = maxOf(mySpeed, (mySpeed + KMH_PER_TAP).coerceAtMost(MAX_KMH))
            if (turboLeft == 0f && turboCharge < 1f) {
              turboCharge = (turboCharge + 1f / TAPS_FOR_TURBO).coerceAtMost(1f)
              if (turboCharge == 1f) TrainSounds.say("Turbo!")
            }
          }
        },
        onTurbo = {
          if (phase == Phase.RACE && turboCharge >= 1f) {
            turboCharge = 0f
            turboLeft = TURBO_SECONDS
            TrainSounds.play(mine.sound)
          }
        },
        onAgain = { startRoute(route) },
        onOther = {
          phase = Phase.ROUTE
          TrainSounds.say("Waar rijden we naartoe?")
        },
      )
    }
  }
}

@Composable
private fun PickRoute(wonRoutes: Set<String>, onPick: (Route) -> Unit, onOtherTrain: () -> Unit) {
  Column(
    Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, top = 84.dp, bottom = 16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    val cup = wonRoutes.size == Route.entries.size
    OutlinedTitle(if (cup) "🏆 Jij bent de kampioen! 🏆" else "Waar rijden we naartoe?", fontSize = 30)
    Spacer(Modifier.height(12.dp))
    BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
      val card: @Composable (Route, Modifier) -> Unit = { r, m -> RouteCard(r, r.name in wonRoutes, m) { onPick(r) } }
      if (maxWidth > maxHeight) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
          Route.entries.forEach { card(it, Modifier.weight(1f).fillMaxHeight()) }
        }
      } else {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
          Route.entries.forEach { card(it, Modifier.weight(1f).fillMaxWidth()) }
        }
      }
    }
    Spacer(Modifier.height(12.dp))
    BouncyButton(onClick = onOtherTrain, modifier = Modifier.size(230.dp, 60.dp), color = Color(0xFF1E88E5), shape = RoundedCornerShape(30.dp)) {
      Text("🚄 Andere trein", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.White)
    }
  }
}

@Composable
private fun RouteCard(route: Route, won: Boolean, modifier: Modifier, onPick: () -> Unit) {
  BouncyButton(
    onClick = {
      TrainSounds.play(Sfx.POP)
      onPick()
    },
    modifier = modifier,
    color = Color.White.copy(alpha = 0.92f),
    shape = RoundedCornerShape(24.dp),
    sound = null,
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(route.emoji, fontSize = 52.sp)
      Text(route.title, fontSize = 26.sp, fontWeight = FontWeight.Black, color = Color(0xFF263238))
      Text(if (won) "⭐ Gewonnen!" else " ", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
    }
  }
}

@Composable
private fun PickTrain(onPick: (TrainKind) -> Unit) {
  BoxWithConstraints(Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, top = 84.dp, bottom = 16.dp)) {
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
                // The real train, cut out of its photo.
                Image(
                  painterResource(kind.cutout ?: kind.photos.first().res),
                  contentDescription = kind.title,
                  contentScale = ContentScale.Fit,
                  modifier = Modifier.fillMaxWidth().weight(1f),
                )
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
  champion: Boolean,
  myProgress: Float,
  rivalProgress: Float,
  turboCharge: Float,
  turboOn: Boolean,
  onTap: () -> Unit,
  onTurbo: () -> Unit,
  onAgain: () -> Unit,
  onOther: () -> Unit,
) {
  BoxWithConstraints(Modifier.fillMaxSize()) {
    // Speed and how far both trains are; in the header row when there is room for it.
    Row(
      Modifier
        .align(Alignment.TopCenter)
        .offset(x = if (maxWidth > 860.dp) 40.dp else 0.dp)
        .padding(top = if (maxWidth > 860.dp) 14.dp else 78.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        "${speed.toInt()} km/u",
        fontSize = 28.sp,
        fontWeight = FontWeight.Black,
        color = if (turboOn) Color(0xFFD84315) else Color(0xFF263238),
        modifier = Modifier
          .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(50))
          .padding(horizontal = 18.dp, vertical = 4.dp),
      )
      Canvas(Modifier.size(200.dp, 34.dp)) {
        val y = size.height / 2f
        drawRoundRect(Color.White.copy(alpha = 0.9f), Offset(0f, y - 8.dp.toPx()), Size(size.width, 16.dp.toPx()), CornerRadius(8.dp.toPx()))
        val inset = 12.dp.toPx()
        val span = size.width - 2 * inset
        drawCircle(Color(0xFF90A4AE), 8.dp.toPx(), Offset(inset + span * rivalProgress.coerceIn(0f, 1f), y))
        drawCircle(Color(0xFFFFC107), 11.dp.toPx(), Offset(inset + span * myProgress.coerceIn(0f, 1f), y))
        drawCircle(Color(0xFF263238), 11.dp.toPx(), Offset(inset + span * myProgress.coerceIn(0f, 1f), y), style = Stroke(2.dp.toPx()))
      }
    }
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
        TurboButton(turboCharge, turboOn, onTurbo, Modifier.align(Alignment.BottomStart).padding(24.dp))
        PressButton("⚡", Color(0xFFFFC107), 170.dp, onTap, Modifier.align(Alignment.BottomEnd).padding(24.dp))
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
          val title = when {
            champion -> "🏆 Kampioen! 🏆"
            won -> "⭐ Jij wint! ⭐"
            else -> "Bijna!"
          }
          OutlinedTitle(title, fontSize = 44, color = Color(0xFFFFC107))
          Spacer(Modifier.height(16.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            BouncyButton(onClick = onAgain, modifier = Modifier.size(220.dp, 76.dp), color = Color(0xFF43A047), shape = RoundedCornerShape(38.dp)) {
              Text("🔁 Nog eens", fontSize = 26.sp, fontWeight = FontWeight.Black, color = Color.White, textAlign = TextAlign.Center)
            }
            BouncyButton(onClick = onOther, modifier = Modifier.size(220.dp, 76.dp), color = Color(0xFF1E88E5), shape = RoundedCornerShape(38.dp)) {
              Text("🗺️ Andere rit", fontSize = 26.sp, fontWeight = FontWeight.Black, color = Color.White, textAlign = TextAlign.Center)
            }
          }
        }
      }
      Phase.PICK, Phase.ROUTE -> {}
    }
  }
}

/** Reacts on touch-down instead of release, so fast tapping with two hands always counts. */
@Composable
private fun PressButton(emoji: String, color: Color, size: Dp, onPress: () -> Unit, modifier: Modifier) {
  var pressed by remember { mutableStateOf(false) }
  val latestOnPress by rememberUpdatedState(onPress)
  Box(
    modifier
      .size(size)
      .scale(if (pressed) 0.88f else 1f)
      .shadow(12.dp, CircleShape)
      .background(color, CircleShape)
      .pointerInput(Unit) {
        detectTapGestures(onPress = {
          pressed = true
          latestOnPress()
          tryAwaitRelease()
          pressed = false
        })
      },
    contentAlignment = Alignment.Center,
  ) {
    Text(emoji, fontSize = (size.value * 0.53f).sp)
  }
}

/** Fills up while tapping; a ring shows how full it is, and it lights up when ready. */
@Composable
private fun TurboButton(charge: Float, active: Boolean, onPress: () -> Unit, modifier: Modifier) {
  val ready = charge >= 1f
  Box(modifier.size(130.dp), contentAlignment = Alignment.Center) {
    PressButton("🚀", if (ready || active) Color(0xFFFF7043) else Color(0xFFB0BEC5), 112.dp, onPress, Modifier)
    Canvas(Modifier.fillMaxSize()) {
      val stroke = 7.dp.toPx()
      drawArc(
        Color(0xFFFF7043), startAngle = -90f, sweepAngle = 360f * charge, useCenter = false,
        topLeft = Offset(stroke / 2, stroke / 2), size = Size(size.width - stroke, size.height - stroke),
        style = Stroke(stroke),
      )
    }
  }
}

internal fun DrawScope.drawFinish(x: Float, railY: Float, u: Float) {
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

internal fun DrawScope.speedLines(railY: Float, u: Float, time: Float) {
  for (k in 0 until 8) {
    val y = railY - (2f + k * 1.4f) * u
    val x = ((k * 173f - time * 2600f) % size.width + size.width) % size.width
    drawLine(Color.White.copy(alpha = 0.7f), Offset(x, y), Offset(x + 14f * u, y), 0.25f * u)
  }
}

/** A tunnel from x [left] to [right]. The Channel Tunnel gets the sea above it, with fish. */
private fun DrawScope.drawTunnel(left: Float, right: Float, underSea: Boolean, u: Float, time: Float) {
  val l = left.coerceAtLeast(0f)
  val r = right.coerceAtMost(size.width)
  if (l >= r) return
  drawRect(Color(0xFF37474F), Offset(l, 0f), Size(r - l, size.height))
  val lampY = size.height * 0.42f
  if (underSea) {
    val seaH = size.height * 0.34f
    drawRect(Color(0xFF0288D1), Offset(l, 0f), Size(r - l, seaH))
    var fx = left + 12f * u
    var k = 0
    while (fx < right) {
      val y = seaH * (0.3f + 0.2f * (k % 3)) + sin(time * 2f + k) * u
      if (fx in l..r) {
        drawOval(Color(0xFFFFB74D), Offset(fx, y), Size(4f * u, 2f * u))
        val tail = Path().apply {
          moveTo(fx + 3.6f * u, y + u)
          lineTo(fx + 5.2f * u, y)
          lineTo(fx + 5.2f * u, y + 2f * u)
          close()
        }
        drawPath(tail, Color(0xFFFFB74D))
      }
      fx += 22f * u
      k++
    }
  }
  var lx = left + 8f * u
  while (lx < right) {
    if (lx in l..r) drawCircle(Color(0xFFFFE082), 0.9f * u, Offset(lx, lampY))
    lx += 16f * u
  }
  for (edge in listOf(left, right)) {
    if (edge in 0f..size.width) drawRect(Color(0xFF90A4AE), Offset(edge - 1f * u, 0f), Size(2f * u, size.height))
  }
}
