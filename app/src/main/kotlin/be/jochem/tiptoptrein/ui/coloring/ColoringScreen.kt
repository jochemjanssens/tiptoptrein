package be.jochem.tiptoptrein.ui.coloring

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import be.jochem.tiptoptrein.sound.Sfx
import be.jochem.tiptoptrein.sound.TrainSounds
import be.jochem.tiptoptrein.ui.common.EmojiButton
import be.jochem.tiptoptrein.ui.common.GameFrame
import kotlin.math.min

private val palette = listOf(
  Color(0xFFE53935), Color(0xFFFF9800), Color(0xFFFFEB3B), Color(0xFF8BC34A),
  Color(0xFF2E7D32), Color(0xFF4FC3F7), Color(0xFF1E40C8), Color(0xFF8E24AA),
  Color(0xFFF48FB1), Color(0xFF795548), Color(0xFF9E9E9E), Color(0xFF212121),
  Color.White,
)

@Composable
fun ColoringScreen(onHome: () -> Unit) {
  var pictureIndex by remember { mutableIntStateOf(0) }
  var brush by remember { mutableStateOf(palette[0]) }
  // Fill colors per picture, kept while switching between pictures.
  val fills = remember { coloringPictures.map { p -> mutableStateListOf(*Array(p.parts.size) { Color.White }) } }
  val praised = remember { mutableStateListOf(*Array(coloringPictures.size) { false }) }
  val picture = coloringPictures[pictureIndex]
  val colors = fills[pictureIndex]

  fun fill(part: Int) {
    colors[part] = brush
    TrainSounds.play(Sfx.POP)
    val done = picture.parts.indices.all { !picture.parts[it].fillable || colors[it] != Color.White }
    if (done && !praised[pictureIndex]) {
      praised[pictureIndex] = true
      TrainSounds.play(Sfx.CORRECT)
      TrainSounds.say("Wat mooi gekleurd!")
    }
  }

  GameFrame(
    title = "Kleuren",
    onHome = onHome,
    actions = {
      EmojiButton("🧽", onClick = {
        for (i in colors.indices) colors[i] = Color.White
        praised[pictureIndex] = false
      }, size = 56.dp)
      Spacer(Modifier.width(10.dp))
      EmojiButton("➡️", onClick = {
        pictureIndex = (pictureIndex + 1) % coloringPictures.size
        TrainSounds.say(coloringPictures[pictureIndex].name)
      }, size = 56.dp)
    },
    background = {
      Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFFFF8E1), Color(0xFFFFE0B2)))))
    },
  ) {
    BoxWithConstraints(Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, top = 84.dp, bottom = 16.dp)) {
      val landscape = maxWidth > maxHeight
      val page: @Composable (Modifier) -> Unit = { m ->
        Canvas(
          m
            .shadow(8.dp, RoundedCornerShape(20.dp))
            .background(Color.White, RoundedCornerShape(20.dp))
            .padding(10.dp)
            .pointerInput(pictureIndex) {
              detectTapGestures { tap ->
                val s = min(size.width / PICTURE_W, size.height / PICTURE_H)
                val ox = (size.width - PICTURE_W * s) / 2f
                val oy = (size.height - PICTURE_H * s) / 2f
                val p = Offset((tap.x - ox) / s, (tap.y - oy) / s)
                val hit = picture.parts.indices.reversed().firstOrNull {
                  picture.parts[it].fillable && picture.parts[it].contains(p)
                }
                if (hit != null) fill(hit)
              }
            },
        ) {
          val s = min(size.width / PICTURE_W, size.height / PICTURE_H)
          val ox = (size.width - PICTURE_W * s) / 2f
          val oy = (size.height - PICTURE_H * s) / 2f
          withTransform({
            translate(ox, oy)
            scale(s, s, Offset.Zero)
          }) {
            val outline = Stroke(width = 0.7f, join = StrokeJoin.Round)
            picture.parts.forEachIndexed { i, part ->
              if (part.fillable) drawPath(part.path, colors[i])
              drawPath(part.path, Color(0xFF212121), style = outline)
            }
          }
        }
      }
      if (landscape) {
        val swatch = min(76f, (maxHeight.value - 6 * 10f) / 7f).dp
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
          PaletteGrid(columns = 2, swatch = swatch, selected = brush) { brush = it }
          page(Modifier.weight(1f).fillMaxSize())
        }
      } else {
        val swatch = min(76f, (maxWidth.value - 6 * 10f) / 7f).dp
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
          page(Modifier.weight(1f).fillMaxSize())
          PaletteGrid(columns = 7, swatch = swatch, selected = brush) { brush = it }
        }
      }
    }
  }
}

@Composable
private fun PaletteGrid(columns: Int, swatch: Dp, selected: Color, onPick: (Color) -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    palette.chunked(columns).forEach { row ->
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        row.forEach { c ->
          val isSelected = c == selected
          Box(
            Modifier
              .size(swatch)
              .scale(if (isSelected) 1.12f else 1f)
              .shadow(if (isSelected) 10.dp else 3.dp, CircleShape)
              .background(c, CircleShape)
              .border(if (isSelected) 5.dp else 2.dp, if (isSelected) Color(0xFF263238) else Color(0xFFBDBDBD), CircleShape)
              .pointerInput(c) {
                detectTapGestures {
                  TrainSounds.play(Sfx.POP)
                  onPick(c)
                }
              },
          )
        }
      }
    }
  }
}
