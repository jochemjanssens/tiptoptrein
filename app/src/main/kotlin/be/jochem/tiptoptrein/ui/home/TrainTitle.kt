package be.jochem.tiptoptrein.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import be.jochem.tiptoptrein.trains.TrainKind
import be.jochem.tiptoptrein.trains.drawTrain
import be.jochem.tiptoptrein.trains.locoLength
import be.jochem.tiptoptrein.ui.common.rememberClock
import kotlin.math.sin

private val wagonColors = listOf(
  Color(0xFFE53935), Color(0xFFFF9800), Color(0xFF43A047), Color(0xFF1E88E5), Color(0xFF8E24AA),
)

/** The app name as a little train: one letter per wagon for "TIPTOP", and the word "TREIN" as the last wagon. */
@Composable
fun TrainTitle(wide: Boolean, modifier: Modifier = Modifier) {
  val time by rememberClock()
  val tile = if (wide) 54.dp else 40.dp
  val gap = if (wide) 7.dp else 5.dp
  val fontSize = if (wide) 36.sp else 26.sp
  Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(gap)) {
      "TIPTOP".forEachIndexed { i, letter ->
        // Wagons rock gently, each a little later than the one before.
        val bob = sin(time * 3f + i * 0.8f) * 1.5f
        LetterWagon(letter.toString(), wagonColors[i % wagonColors.size], tile, tile, fontSize, Modifier.offset(y = bob.dp))
      }
      Spacer(Modifier.width(gap))
      LetterWagon("TREIN", Color(0xFF0D47A1), tile * 3.1f, tile, fontSize, Modifier.offset(y = (sin(time * 3f + 6f) * 1.5f).dp))
      Canvas(Modifier.size(tile * 2.1f, tile + 10.dp)) {
        val u = size.height / 11.5f
        val len = locoLength(TrainKind.HIGH_SPEED)
        drawTrain(TrainKind.HIGH_SPEED, size.width - len * u, size.height - 0.2f * u, u, cars = 0, turn = time * 4f, time = time)
      }
    }
    Canvas(Modifier.padding(top = 2.dp).size(tile * 12.2f, 6.dp)) {
      drawRect(Color(0xFF546E7A), Offset.Zero, Size(size.width, size.height))
      val step = 16.dp.toPx()
      var x = -(time * 60f) % step
      while (x < size.width) {
        drawRect(Color(0xFF6D4C41), Offset(x.coerceAtLeast(0f), 0f), Size(5.dp.toPx(), size.height))
        x += step
      }
    }
  }
}

@Composable
private fun LetterWagon(text: String, color: Color, width: Dp, height: Dp, fontSize: TextUnit, modifier: Modifier) {
  val shape = RoundedCornerShape(10.dp)
  Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
    Box(
      Modifier
        .size(width, height)
        .shadow(5.dp, shape)
        .background(color, shape)
        .border(2.5.dp, Color.White, shape),
      contentAlignment = Alignment.Center,
    ) {
      Text(text, color = Color.White, fontSize = fontSize, fontWeight = FontWeight.Black, maxLines = 1)
    }
    Canvas(Modifier.size(width, 10.dp)) {
      val r = 5.dp.toPx()
      for (cx in listOf(size.width * 0.27f, size.width * 0.73f)) {
        drawCircle(Color(0xFF263238), r, Offset(cx, size.height - r))
        drawCircle(Color(0xFFB0BEC5), r * 0.4f, Offset(cx, size.height - r))
      }
    }
  }
}
