package be.jochem.tiptoptrein.ui.find

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import be.jochem.tiptoptrein.sound.Sfx
import be.jochem.tiptoptrein.sound.TrainSounds
import be.jochem.tiptoptrein.trains.TrainKind
import be.jochem.tiptoptrein.trains.drawRails
import be.jochem.tiptoptrein.trains.drawTrain
import be.jochem.tiptoptrein.trains.trainLength
import be.jochem.tiptoptrein.ui.common.BouncyButton
import be.jochem.tiptoptrein.ui.common.EmojiButton
import be.jochem.tiptoptrein.ui.common.GameFrame
import be.jochem.tiptoptrein.ui.common.OutlinedTitle
import be.jochem.tiptoptrein.ui.common.StarCounter
import be.jochem.tiptoptrein.ui.common.rememberClock
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.min

/** Trains that look alike share a group, so a round never asks to tell them apart. */
private val groups = listOf(
  listOf(TrainKind.STEAM),
  listOf(TrainKind.INTERCITY, TrainKind.SPRINTER, TrainKind.DOUBLE_DECKER),
  listOf(TrainKind.HIGH_SPEED),
  listOf(TrainKind.ICE),
  listOf(TrainKind.TGV),
  listOf(TrainKind.MAGLEV),
  listOf(TrainKind.FREIGHT),
  listOf(TrainKind.METRO),
  listOf(TrainKind.TRAM),
)

private data class Question(val choices: List<TrainKind>, val answer: TrainKind)

private fun newQuestion(previous: TrainKind?): Question {
  val choices = groups.shuffled().take(3).map { it.random() }
  val answer = choices.filter { it != previous }.random()
  return Question(choices, answer)
}

@Composable
fun FindTrainScreen(onHome: () -> Unit) {
  var question by remember { mutableStateOf(newQuestion(null)) }
  var solved by remember { mutableStateOf(false) }
  var stars by remember { mutableIntStateOf(0) }
  val wrong = remember { mutableStateListOf<TrainKind>() }
  val time by rememberClock()
  val scope = rememberCoroutineScope()

  LaunchedEffect(question) {
    solved = false
    wrong.clear()
    delay(300)
    TrainSounds.say("Waar is de ${question.answer.title}?")
  }

  fun pick(kind: TrainKind) {
    if (solved) return
    if (kind == question.answer) {
      solved = true
      stars++
      TrainSounds.play(Sfx.CORRECT)
      TrainSounds.say("Ja, goed gevonden! Knap van jou!")
      scope.launch {
        delay(600)
        TrainSounds.play(kind.sound)
        delay(2200)
        question = newQuestion(question.answer)
      }
    } else {
      wrong.add(kind)
      TrainSounds.play(Sfx.WRONG)
      TrainSounds.say("Bijna! Dat is de ${kind.title}. Waar is de ${question.answer.title}?")
    }
  }

  GameFrame(
    title = "Zoek de trein",
    onHome = onHome,
    actions = {
      StarCounter(stars)
      Spacer(Modifier.width(10.dp))
      EmojiButton("🔊", onClick = { TrainSounds.say("Waar is de ${question.answer.title}?") }, size = 56.dp)
    },
    background = {
      Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFCE93D8), Color(0xFF7E57C2)))))
    },
  ) {
    Column(
      Modifier.fillMaxSize().padding(start = 20.dp, end = 20.dp, top = 84.dp, bottom = 20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      OutlinedTitle("Waar is de ${question.answer.title}?", fontSize = 34)
      Spacer(Modifier.height(14.dp))
      BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
        val card: @Composable (TrainKind, Modifier) -> Unit = { kind, m -> ChoiceCard(kind, m, question, wrong, solved, time) { pick(kind) } }
        if (maxWidth > maxHeight) {
          Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            question.choices.forEach { card(it, Modifier.weight(1f).fillMaxHeight()) }
          }
        } else {
          Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            question.choices.forEach { card(it, Modifier.weight(1f).fillMaxWidth()) }
          }
        }
      }
    }
  }
}

@Composable
private fun ChoiceCard(
  kind: TrainKind,
  modifier: Modifier,
  question: Question,
  wrong: List<TrainKind>,
  solved: Boolean,
  time: Float,
  onPick: () -> Unit,
) {
  val isWrong = kind in wrong
  val highlight = when {
    solved && kind == question.answer -> Color(0xFF66BB6A)
    isWrong -> Color(0xFFBDBDBD)
    else -> Color(0xFFE1F5FE)
  }
  BouncyButton(
    onClick = onPick,
    modifier = modifier,
    color = highlight,
    shape = RoundedCornerShape(26.dp),
    sound = null,
  ) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
      Canvas(Modifier.fillMaxWidth().weight(1f)) {
        val len = trainLength(kind, 1)
        val u = min(size.height / 16f, size.width / (len + 4f))
        val railY = size.height * 0.75f
        val x = (size.width - len * u) / 2f
        drawRails(railY, u)
        drawTrain(kind, x, railY, u, cars = 1, time = time)
      }
      // After a wrong guess, show the name so the child learns it.
      Text(
        if (isWrong || (solved && kind == question.answer)) kind.title else " ",
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF263238),
        modifier = Modifier.padding(bottom = 10.dp),
      )
    }
  }
}
