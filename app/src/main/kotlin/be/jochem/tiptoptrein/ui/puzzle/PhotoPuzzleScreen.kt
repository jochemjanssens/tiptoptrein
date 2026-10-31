package be.jochem.tiptoptrein.ui.puzzle

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import be.jochem.tiptoptrein.sound.Sfx
import be.jochem.tiptoptrein.sound.TrainSounds
import be.jochem.tiptoptrein.trains.TrainKind
import be.jochem.tiptoptrein.ui.common.BouncyButton
import be.jochem.tiptoptrein.ui.common.GameFrame
import be.jochem.tiptoptrein.ui.common.StarCounter
import kotlin.math.hypot
import kotlin.math.min
import kotlin.random.Random

// Grid (columns x rows) per level: 4, 6, then 8 pieces.
private val levelGrids = listOf(2 to 2, 3 to 2, 4 to 2)

/** One piece of the photo. [x]/[y] are its top-left corner in the play area, in px. */
private class Piece(val col: Int, val row: Int, startX: Float, startY: Float) {
  var x by mutableFloatStateOf(startX)
  var y by mutableFloatStateOf(startY)
  var placed by mutableStateOf(false)
  var lift by mutableFloatStateOf(0f) // z-order: last touched on top
}

@Composable
fun PhotoPuzzleScreen(onHome: () -> Unit) {
  var level by remember { mutableIntStateOf(0) }
  var kind by remember { mutableStateOf(TrainKind.entries.random()) }
  var stars by remember { mutableIntStateOf(0) }
  var solved by remember { mutableStateOf(false) }
  var round by remember { mutableIntStateOf(0) }

  LaunchedEffect(round) {
    solved = false
    TrainSounds.say(if (round == 0) "Sleep de stukjes naar de goede plek!" else "Nog een puzzel!")
  }

  fun next() {
    level = min(level + 1, levelGrids.lastIndex)
    kind = TrainKind.entries.filter { it != kind }.random()
    round++
  }

  GameFrame(
    title = "Fotopuzzel",
    onHome = onHome,
    actions = { StarCounter(stars) },
    background = {
      Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFB2EBF2), Color(0xFF4DD0E1)))))
    },
  ) {
    BoxWithConstraints(Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, top = 84.dp, bottom = 16.dp)) {
      val density = LocalDensity.current
      val picture = remember(round) { kind.photos.random() }
      val photo = ImageBitmap.imageResource(picture.res)
      val (cols, rows) = levelGrids[level]
      val areaW = with(density) { maxWidth.toPx() }
      val areaH = with(density) { maxHeight.toPx() }
      val landscape = areaW > areaH
      val aspect = photo.width.toFloat() / photo.height

      // The board takes ~60% of the space, the pieces start in a tray beside or below it.
      val boardW: Float
      val boardH: Float
      if (landscape) {
        boardW = min(areaW * 0.5f, areaH * aspect)
        boardH = boardW / aspect
      } else {
        boardH = min(areaH * 0.5f, areaW / aspect)
        boardW = boardH * aspect
      }
      val boardX = if (landscape) 0f else (areaW - boardW) / 2f
      val boardY = if (landscape) (areaH - boardH) / 2f else 0f
      val pieceW = boardW / cols
      val pieceH = boardH / rows

      val pieces = remember(round, areaW, areaH) {
        val trayX = if (landscape) boardW + 24f else 0f
        val trayY = if (landscape) 0f else boardH + 24f
        val trayW = if (landscape) areaW - trayX else areaW
        val trayH = if (landscape) areaH else areaH - trayY
        // Lay the pieces out in a loose grid in the tray, in random order.
        val gap = 20f
        val perRow = Math.round((trayW + gap) / (pieceW + gap)).coerceAtLeast(1)
        val stepX = min(pieceW + gap, (trayW - pieceW) / (perRow - 1).coerceAtLeast(1))
        val trayRows = (cols * rows + perRow - 1) / perRow
        val stepY = min(pieceH + gap, (trayH - pieceH) / (trayRows - 1).coerceAtLeast(1))
        (0 until cols).flatMap { c -> (0 until rows).map { r -> c to r } }.shuffled().mapIndexed { i, (c, r) ->
          Piece(
            c, r,
            trayX + (i % perRow) * stepX + Random.nextFloat() * 10f,
            trayY + (i / perRow) * stepY + Random.nextFloat() * 10f,
          )
        }
      }

      // Board with a faint version of the photo as a hint.
      Box(
        Modifier
          .offset { IntOffset(boardX.toInt(), boardY.toInt()) }
          .size(with(density) { boardW.toDp() }, with(density) { boardH.toDp() })
          .background(Color.White, RoundedCornerShape(4.dp)),
      ) {
        Image(
          painterResource(picture.res), null,
          contentScale = ContentScale.FillBounds,
          modifier = Modifier.fillMaxSize().alpha(0.22f),
        )
        Canvas(Modifier.fillMaxSize()) {
          for (c in 1 until cols) drawLine(Color.White, Offset(c * pieceW, 0f), Offset(c * pieceW, size.height), 4f)
          for (r in 1 until rows) drawLine(Color.White, Offset(0f, r * pieceH), Offset(size.width, r * pieceH), 4f)
          drawRect(Color(0xFF00838F), style = Stroke(6f))
        }
      }

      var topLift by remember { mutableFloatStateOf(1f) }
      pieces.forEach { piece ->
        PieceView(
          piece, photo, cols, rows,
          widthDp = with(density) { pieceW.toDp() },
          heightDp = with(density) { pieceH.toDp() },
          enabled = !solved,
          onPickUp = {
            topLift += 1f
            piece.lift = topLift
          },
          onDrop = {
            val tx = boardX + piece.col * pieceW
            val ty = boardY + piece.row * pieceH
            if (hypot(piece.x - tx, piece.y - ty) < min(pieceW, pieceH) * 0.4f) {
              piece.x = tx
              piece.y = ty
              piece.placed = true
              piece.lift = 0f
              TrainSounds.play(Sfx.POP)
              if (pieces.all { it.placed }) {
                solved = true
                stars++
                TrainSounds.play(Sfx.CORRECT)
                TrainSounds.say("Hoera! Het is de ${kind.title}!")
              }
            }
          },
        )
      }

      if (solved) {
        Column(
          Modifier.align(if (landscape) Alignment.CenterEnd else Alignment.BottomCenter).padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Text(
            "🎉 ${kind.title}!",
            fontSize = 34.sp, fontWeight = FontWeight.Black, color = Color(0xFF263238),
            modifier = Modifier.background(Color.White, RoundedCornerShape(20.dp)).padding(horizontal = 20.dp, vertical = 8.dp),
          )
          Spacer(Modifier.height(16.dp))
          BouncyButton(onClick = ::next, modifier = Modifier.size(width = 230.dp, height = 80.dp), color = Color(0xFF43A047), shape = RoundedCornerShape(40.dp)) {
            Text("➡️ Volgende", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White)
          }
        }
      }
    }
  }
}

@Composable
private fun PieceView(
  piece: Piece,
  photo: ImageBitmap,
  cols: Int,
  rows: Int,
  widthDp: Dp,
  heightDp: Dp,
  enabled: Boolean,
  onPickUp: () -> Unit,
  onDrop: () -> Unit,
) {
  val shape = RoundedCornerShape(if (piece.placed) 0.dp else 8.dp)
  Box(
    Modifier
      .zIndex(piece.lift)
      .offset { IntOffset(piece.x.toInt(), piece.y.toInt()) }
      .size(widthDp, heightDp)
      .then(if (piece.placed) Modifier else Modifier.shadow(8.dp, shape).border(3.dp, Color.White, shape))
      .pointerInput(piece, enabled) {
        if (!enabled) return@pointerInput
        detectDragGestures(
          onDragStart = { if (!piece.placed) onPickUp() },
          onDragEnd = { if (!piece.placed) onDrop() },
        ) { change, drag ->
          if (piece.placed) return@detectDragGestures
          change.consume()
          piece.x += drag.x
          piece.y += drag.y
        }
      },
  ) {
    Canvas(Modifier.fillMaxSize()) {
      val sw = photo.width / cols
      val sh = photo.height / rows
      drawImage(
        photo,
        srcOffset = IntOffset(piece.col * sw, piece.row * sh),
        srcSize = IntSize(sw, sh),
        dstSize = IntSize(size.width.toInt(), size.height.toInt()),
      )
    }
  }
}
