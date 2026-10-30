package be.jochem.tiptoptrein.ui.count

import kotlinx.coroutines.withContext
import be.jochem.tiptoptrein.ui.common.GameMotion
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import be.jochem.tiptoptrein.sound.Sfx
import be.jochem.tiptoptrein.sound.TrainSounds
import be.jochem.tiptoptrein.trains.TrainKind
import be.jochem.tiptoptrein.trains.carCenter
import be.jochem.tiptoptrein.trains.drawRails
import be.jochem.tiptoptrein.trains.drawScenery
import be.jochem.tiptoptrein.trains.drawTrain
import be.jochem.tiptoptrein.trains.trainLength
import be.jochem.tiptoptrein.ui.common.BouncyButton
import be.jochem.tiptoptrein.ui.common.GameFrame
import be.jochem.tiptoptrein.ui.common.StarCounter
import be.jochem.tiptoptrein.ui.common.rememberClock
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.min

private const val MAX_CARS = 7

private val wagonColors = listOf(
  Color(0xFFE53935), Color(0xFF1E88E5), Color(0xFF43A047), Color(0xFFFDD835),
  Color(0xFF8E24AA), Color(0xFFFF7043), Color(0xFF26C6DA), Color(0xFFEC407A),
)

private val numberWords = listOf("nul", "één", "twee", "drie", "vier", "vijf", "zes", "zeven", "acht", "negen")

private val buttonColors = listOf(Color(0xFF42A5F5), Color(0xFFAB47BC), Color(0xFFFF7043))

private enum class Step { ENTERING, ASKING, COUNTING, LEAVING }

private data class Round(val kind: TrainKind, val cars: Int, val colors: List<Color>, val options: List<Int>)

private fun newRound(previous: Round?): Round {
  val kinds = TrainKind.entries.filter { it != TrainKind.TRAM }
  var cars: Int
  do cars = (1..MAX_CARS).random() while (cars == previous?.cars)
  val options = buildSet {
    add(cars)
    while (size < 3) add((maxOf(1, cars - 3)..min(9, cars + 3)).random())
  }.shuffled()
  return Round(kinds.random(), cars, wagonColors.shuffled().take(cars), options)
}

@Composable
fun CountWagonsScreen(onHome: () -> Unit) {
  var round by remember { mutableStateOf(newRound(null)) }
  var step by remember { mutableStateOf(Step.ENTERING) }
  var counted by remember { mutableIntStateOf(0) }
  var stars by remember { mutableIntStateOf(0) }
  val wrong = remember { mutableStateListOf<Int>() }
  val position = remember { Animatable(0f) }
  val time by rememberClock()
  val scope = rememberCoroutineScope()
  val text = rememberTextMeasurer()

  LaunchedEffect(round) {
    step = Step.ENTERING
    counted = 0
    wrong.clear()
    position.snapTo(0f)
    withContext(GameMotion) { position.animateTo(1f, tween(2800, easing = LinearOutSlowInEasing)) }
    step = Step.ASKING
    TrainSounds.say("Hoeveel wagons zie je?")
  }

  fun answer(n: Int) {
    if (step != Step.ASKING) return
    if (n != round.cars) {
      TrainSounds.play(Sfx.WRONG)
      TrainSounds.say("Bijna! Tel maar rustig nog eens.")
      wrong.add(n)
      return
    }
    step = Step.COUNTING
    stars++
    TrainSounds.play(Sfx.CORRECT)
    scope.launch(GameMotion) {
      delay(700)
      for (i in 1..round.cars) {
        counted = i
        TrainSounds.play(Sfx.POP)
        TrainSounds.say(numberWords[i])
        delay(750)
      }
      val word = if (round.cars == 1) "wagon" else "wagons"
      TrainSounds.say("Goed geteld! ${numberWords[round.cars].replaceFirstChar { it.uppercase() }} $word!")
      delay(1800)
      step = Step.LEAVING
      TrainSounds.play(round.kind.sound)
      position.animateTo(2f, tween(2000, easing = FastOutLinearInEasing))
      round = newRound(round)
    }
  }

  GameFrame(
    title = "Tel de wagons",
    onHome = onHome,
    actions = { StarCounter(stars) },
    background = {
      Canvas(Modifier.fillMaxSize()) {
        val railY = size.height * 0.62f
        val len = trainLength(round.kind, MAX_CARS)
        val u = min(size.height / 30f, size.width / (len + 6f))
        drawScenery(round.kind.scenery, railY, u, time = time)
        drawRails(railY, u)

        val trainPx = trainLength(round.kind, round.cars) * u
        val centreX = (size.width - trainPx) / 2f
        val p = position.value
        val x = if (p <= 1f) -trainPx + (centreX + trainPx) * p else centreX + (size.width - centreX) * (p - 1f)
        val lifted = if (step == Step.COUNTING && counted > 0) counted - 1 else -1
        drawTrain(
          round.kind, x, railY, u, cars = round.cars, turn = x / u, time = time,
          carColors = round.colors, liftCar = lifted,
        )

        // numbers above the wagons that have been counted
        if (step == Step.COUNTING || step == Step.LEAVING) {
          val style = TextStyle(fontSize = (5f * u / density).sp, fontWeight = FontWeight.Black, color = Color.White)
          for (i in 0 until counted) {
            val label = text.measure("${i + 1}", style)
            val cx = x + carCenter(i, u)
            val cy = railY - 14f * u
            drawCircle(round.colors[i], 3.6f * u, Offset(cx, cy))
            drawCircle(Color.White, 3.6f * u, Offset(cx, cy), style = androidx.compose.ui.graphics.drawscope.Stroke(0.5f * u))
            drawText(label, topLeft = Offset(cx - label.size.width / 2f, cy - label.size.height / 2f))
          }
        }
      }
    },
  ) {
    Row(
      Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp),
      horizontalArrangement = Arrangement.spacedBy(30.dp),
    ) {
      round.options.forEachIndexed { i, n ->
        BouncyButton(
          onClick = { answer(n) },
          modifier = Modifier.size(96.dp),
          color = buttonColors[i],
          enabled = step == Step.ASKING && n !in wrong,
          sound = null,
        ) {
          Text("$n", fontSize = 52.sp, fontWeight = FontWeight.Black, color = Color.White)
        }
      }
    }
  }
}
