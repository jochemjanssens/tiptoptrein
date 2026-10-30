package be.jochem.tiptoptrein.ui.home

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import be.jochem.tiptoptrein.Coloring
import be.jochem.tiptoptrein.CountWagons
import be.jochem.tiptoptrein.Drive
import be.jochem.tiptoptrein.FindTrain
import be.jochem.tiptoptrein.Memory
import be.jochem.tiptoptrein.PhotoPuzzle
import be.jochem.tiptoptrein.Race
import be.jochem.tiptoptrein.Slideshow
import be.jochem.tiptoptrein.TrackPuzzle
import be.jochem.tiptoptrein.TrainBuilder
import be.jochem.tiptoptrein.sound.TrainSounds
import be.jochem.tiptoptrein.trains.Scenery
import be.jochem.tiptoptrein.trains.TrainKind
import be.jochem.tiptoptrein.trains.drawRails
import be.jochem.tiptoptrein.trains.drawScenery
import be.jochem.tiptoptrein.trains.drawTrain
import be.jochem.tiptoptrein.trains.trainLength
import be.jochem.tiptoptrein.ui.common.BouncyButton
import be.jochem.tiptoptrein.ui.common.OutlinedTitle
import be.jochem.tiptoptrein.ui.common.rememberClock
import kotlin.math.min

private data class Activity(val emoji: String, val label: String, val color: Color, val key: NavKey)

private val activities = listOf(
  Activity("🎞️", "Treinen kijken", Color(0xFF42A5F5), Slideshow),
  Activity("⚡", "Supersnel", Color(0xFF26A69A), Race),
  Activity("🚂", "Machinist", Color(0xFFEF5350), Drive),
  Activity("🔧", "Bouw je trein", Color(0xFF5C6BC0), TrainBuilder),
  Activity("🛤️", "Sporen leggen", Color(0xFF8D6E63), TrackPuzzle),
  Activity("🧩", "Fotopuzzel", Color(0xFF00ACC1), PhotoPuzzle),
  Activity("🎨", "Kleuren", Color(0xFFEC407A), Coloring),
  Activity("🔢", "Tellen", Color(0xFF66BB6A), CountWagons),
  Activity("🔍", "Zoeken", Color(0xFFAB47BC), FindTrain),
  Activity("🃏", "Memory", Color(0xFFFFA726), Memory),
)

@Composable
fun HomeScreen(onOpen: (NavKey) -> Unit) {
  val time by rememberClock()
  LaunchedEffect(Unit) { TrainSounds.say("Hallo! Fijn dat je er bent. Waar gaan we mee spelen?") }

  Box(Modifier.fillMaxSize()) {
    // A parade of trains driving past along the bottom.
    // Plain holder (not state): written while drawing, read when the train is tapped.
    val currentKind = remember { arrayOf(TrainKind.STEAM) }
    Canvas(
      Modifier.fillMaxSize().pointerInput(Unit) {
        detectTapGestures { TrainSounds.play(currentKind[0].sound) }
      },
    ) {
      val railY = size.height * 0.93f
      val u = min(size.height / 66f, size.width / 60f)
      drawScenery(Scenery.COUNTRY, railY, u, scroll = time * 4f * u, time = time)
      drawRails(railY, u)
      val speed = 22f * u
      val lap = TrainKind.entries.maxOf { trainLength(it, 2) } * u + size.width
      val lapTime = lap / speed
      val index = (time / lapTime).toInt() % TrainKind.entries.size
      val kind = TrainKind.entries[index]
      currentKind[0] = kind
      val len = trainLength(kind, 2) * u
      val x = (time % lapTime) * speed - len
      drawTrain(kind, x, railY, u, cars = 2, turn = x / (1f * u), time = time)
    }

    BoxWithConstraints(
      Modifier.fillMaxSize()
        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical))
        .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
      val landscape = maxWidth > maxHeight
      val columns = when {
        landscape -> 5
        maxWidth > 600.dp -> 3
        else -> 2
      }
      // Leave the bottom part of the screen for the passing trains.
      val gridHeight = if (landscape) maxHeight * 0.66f else maxHeight * 0.74f
      val wide = maxWidth > 700.dp
      val cardSide = min(maxWidth.value / columns, gridHeight.value / ((activities.size + columns - 1) / columns))
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedTitle("TipTop Trein", fontSize = if (wide) 52 else 40)
        Spacer(Modifier.height(10.dp))
        Column(
          Modifier.fillMaxWidth().height(gridHeight),
          verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          activities.chunked(columns).forEach { row ->
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
              row.forEach { a -> ActivityCard(a, Modifier.weight(1f).fillMaxHeight(), cardSide) { onOpen(a.key) } }
              // keep cards the same width on the last, shorter row
              repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ActivityCard(a: Activity, modifier: Modifier, side: Float, onOpen: () -> Unit) {
  BouncyButton(
    onClick = {
      TrainSounds.say(a.label)
      onOpen()
    },
    modifier = modifier,
    color = a.color,
    shape = RoundedCornerShape(28.dp),
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(a.emoji, fontSize = (side * 0.28f).coerceIn(36f, 80f).sp)
      Text(
        a.label,
        fontSize = (side * 0.1f).coerceIn(17f, 30f).sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        textAlign = TextAlign.Center,
        lineHeight = (side * 0.11f).coerceIn(19f, 32f).sp,
      )
    }
  }
}
