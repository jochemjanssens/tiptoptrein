package be.jochem.tiptoptrein.ui.tracks

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import be.jochem.tiptoptrein.sound.Sfx
import be.jochem.tiptoptrein.sound.TrainSounds
import be.jochem.tiptoptrein.trains.TrainKind
import be.jochem.tiptoptrein.trains.drawTrain
import be.jochem.tiptoptrein.trains.locoLength
import be.jochem.tiptoptrein.ui.common.GameFrame
import be.jochem.tiptoptrein.ui.common.StarCounter
import be.jochem.tiptoptrein.ui.common.rememberClock
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

// Sides of a tile, clockwise.
private const val N = 0
private const val E = 1
private const val S = 2
private const val W = 3
private val DX = intArrayOf(0, 1, 0, -1)
private val DY = intArrayOf(-1, 0, 1, 0)

private fun opposite(side: Int) = (side + 2) % 4

private enum class Decor { GRASS, TREE, FLOWERS, HOUSE, POND }

/** A board cell. Track cells can be turned; a straight piece is W-E and a curve N-E before turning. */
private class Cell(val col: Int, val row: Int, val curve: Boolean, val track: Boolean, val decor: Decor, startTurns: Int) {
  var turns by mutableIntStateOf(startTurns)

  fun sides(): Pair<Int, Int> {
    val r = turns % 4
    return if (curve) (N + r) % 4 to (E + r) % 4 else (W + r) % 4 to (E + r) % 4
  }

  fun connects(side: Int): Boolean = sides().let { it.first == side || it.second == side }

  fun exitFor(entry: Int): Int = sides().let { if (it.first == entry) it.second else it.first }
}

private class Board(val cols: Int, val rows: Int, val startRow: Int, val goalRow: Int, val cells: List<Cell>) {
  fun at(c: Int, r: Int) = cells.firstOrNull { it.col == c && it.row == r }
}

private class Step(val cell: Cell, val entry: Int, val exit: Int)

/** Follows the rails from the start station; returns the connected steps and whether they reach the goal. */
private fun Board.follow(): Pair<List<Step>, Boolean> {
  val steps = mutableListOf<Step>()
  var c = 0
  var r = startRow
  var entry = W
  while (true) {
    val cell = at(c, r) ?: return steps to false
    if (!cell.track || !cell.connects(entry) || steps.any { it.cell === cell }) return steps to false
    val exit = cell.exitFor(entry)
    val nc = c + DX[exit]
    val nr = r + DY[exit]
    if (nc !in 0 until cols || nr !in 0 until rows) {
      // Leaving the board only counts when it leads into the goal station.
      val reachesGoal = exit == E && r == goalRow && c == cols - 1
      if (reachesGoal) steps += Step(cell, entry, exit)
      return steps to reachesGoal
    }
    // Only light up a piece once it points at a piece that fits.
    val next = at(nc, nr)
    if (next == null || !next.track || !next.connects(opposite(exit))) return steps to false
    steps += Step(cell, entry, exit)
    c = nc
    r = nr
    entry = opposite(exit)
  }
}

private val levelSizes = listOf(3 to 2, 4 to 2, 4 to 3, 5 to 3, 5 to 4, 6 to 4)

private fun newBoard(level: Int): Board {
  val (cols, rows) = levelSizes[min(level, levelSizes.lastIndex)]
  val startRow = Random.nextInt(rows)
  // Walk from left to right, sometimes going up or down inside a column.
  val needed = mutableMapOf<Pair<Int, Int>, Pair<Int, Int>>()
  var r = startRow
  for (c in 0 until cols) {
    val target = if (Random.nextFloat() < 0.6f) Random.nextInt(rows) else r
    var entry = W
    while (r != target) {
      val dir = if (target > r) S else N
      needed[c to r] = entry to dir
      entry = opposite(dir)
      r += DY[dir]
    }
    needed[c to r] = entry to E
  }
  val goalRow = r
  val easy = level == 0
  val cells = mutableListOf<Cell>()
  for (c in 0 until cols) for (row in 0 until rows) {
    val sides = needed[c to row]
    if (sides == null) {
      cells += Cell(c, row, curve = false, track = false, decor = Decor.entries.random(), startTurns = 0)
      continue
    }
    val curve = sides.first != opposite(sides.second)
    val solution = (0..3).first { t ->
      val probe = Cell(c, row, curve, true, Decor.GRASS, t)
      probe.connects(sides.first) && probe.connects(sides.second)
    }
    // Start every piece turned the wrong way (on the first level only about half of them).
    val wrong = (0..3).filter { t ->
      val probe = Cell(c, row, curve, true, Decor.GRASS, t)
      !(probe.connects(sides.first) && probe.connects(sides.second))
    }
    val start = if (easy && Random.nextBoolean()) solution else wrong.random()
    cells += Cell(c, row, curve, true, Decor.GRASS, start)
  }
  val board = Board(cols, rows, startRow, goalRow, cells)
  if (board.follow().second) {
    // Already solved by chance: turn the first piece.
    board.cells.first { it.track }.turns++
  }
  return board
}

@Composable
fun TrackPuzzleScreen(onHome: () -> Unit) {
  var level by remember { mutableIntStateOf(0) }
  var board by remember { mutableStateOf(newBoard(0)) }
  var kind by remember { mutableStateOf(TrainKind.entries.random()) }
  var stars by remember { mutableIntStateOf(0) }
  val progress = remember { Animatable(0f) }
  val time by rememberClock()

  val (steps, solved) = board.follow()

  LaunchedEffect(board) {
    progress.snapTo(0f)
    delay(300)
    TrainSounds.say(if (level == 0) "Tik op de sporen. Dan draaien ze. Maak een weg voor de trein!" else "Maak een weg voor de trein!")
  }

  LaunchedEffect(solved, board) {
    if (!solved) return@LaunchedEffect
    TrainSounds.play(kind.sound)
    val total = steps.size + 2f
    progress.animateTo(total, tween((total * 550).toInt(), easing = LinearEasing))
    stars++
    TrainSounds.play(Sfx.CORRECT)
    TrainSounds.say("Goed zo! De trein is er!")
    delay(2500)
    level++
    kind = TrainKind.entries.random()
    board = newBoard(level)
  }

  GameFrame(
    title = "Sporen leggen",
    onHome = onHome,
    actions = { StarCounter(stars) },
    background = { Box(Modifier.fillMaxSize().background(Color(0xFF7CB342))) },
  ) {
    BoxWithConstraints(
      Modifier.fillMaxSize().padding(start = 12.dp, end = 12.dp, top = 84.dp, bottom = 12.dp),
      contentAlignment = Alignment.Center,
    ) {
      val tile = min(maxWidth.value / (board.cols + 2), maxHeight.value / board.rows).dp
      val tilePx = with(LocalDensity.current) { tile.toPx() }
      Box(Modifier.size(tile * (board.cols + 2), tile * board.rows)) {
        board.cells.forEach { cell ->
          val inChain = steps.any { it.cell === cell }
          TileView(
            cell,
            highlight = inChain,
            modifier = Modifier.offset(tile * (cell.col + 1), tile * cell.row).size(tile),
            enabled = !solved,
          )
        }
        Canvas(Modifier.fillMaxSize()) {
          drawStation(Offset(0f, board.startRow * tilePx), tilePx, goal = false)
          drawStation(Offset((board.cols + 1) * tilePx, board.goalRow * tilePx), tilePx, goal = true)
          val (pos, heading) = trainPosition(board, steps, progress.value, tilePx)
          val u = tilePx * 0.8f / locoLength(kind)
          val len = locoLength(kind) * u
          rotate(heading, pivot = pos) {
            drawTrain(kind, pos.x - len / 2f, pos.y + 5.2f * u, u, cars = 0, turn = progress.value * 6f, time = time)
          }
        }
      }
    }
  }
}

/** Where the train is after driving [progress] tiles from the start station, and which way it faces (degrees). */
private fun trainPosition(board: Board, steps: List<Step>, progress: Float, t: Float): Pair<Offset, Float> {
  fun centre(c: Int, r: Int) = Offset((c + 1.5f) * t, (r + 0.5f) * t)
  fun mid(c: Int, r: Int, side: Int) = centre(c, r) + Offset(DX[side] * t / 2f, DY[side] * t / 2f)

  val startCentre = Offset(0.5f * t, (board.startRow + 0.5f) * t)
  if (progress <= 1f) {
    return Offset(startCentre.x + progress * t * 0.5f + 0f, startCentre.y) to 0f
  }
  val i = (progress - 1f).toInt()
  val f = (progress - 1f) - i
  if (i >= steps.size) {
    val goal = Offset((board.cols + 1.5f) * t, (board.goalRow + 0.5f) * t)
    val from = goal - Offset(t, 0f)
    return Offset(from.x + t * 0.5f + min(f, 1f) * t * 0.5f, goal.y) to 0f
  }
  val s = steps[i]
  val a = mid(s.cell.col, s.cell.row, s.entry)
  val b = mid(s.cell.col, s.cell.row, s.exit)
  if (s.entry == opposite(s.exit)) {
    val dir = s.exit
    return (a + (b - a) * f) to (atan2(DY[dir].toFloat(), DX[dir].toFloat()) * 180f / PI.toFloat())
  }
  val corner = centre(s.cell.col, s.cell.row) +
    Offset((DX[s.entry] + DX[s.exit]) * t / 2f, (DY[s.entry] + DY[s.exit]) * t / 2f)
  val a0 = atan2(a.y - corner.y, a.x - corner.x)
  val a1 = atan2(b.y - corner.y, b.x - corner.x)
  var delta = a1 - a0
  if (delta > PI) delta -= (2 * PI).toFloat()
  if (delta < -PI) delta += (2 * PI).toFloat()
  val ang = a0 + delta * f
  val pos = corner + Offset(cos(ang) * t / 2f, sin(ang) * t / 2f)
  val heading = ang + (if (delta > 0) PI.toFloat() / 2f else -PI.toFloat() / 2f)
  return pos to heading * 180f / PI.toFloat()
}

@Composable
private fun TileView(cell: Cell, highlight: Boolean, modifier: Modifier, enabled: Boolean) {
  val angle by animateFloatAsState(cell.turns * 90f, spring(dampingRatio = 0.55f, stiffness = 500f), label = "turn")
  Box(
    modifier
      .graphicsLayer { rotationZ = if (cell.track) angle else 0f }
      .clickable(remember { MutableInteractionSource() }, indication = null, enabled = enabled && cell.track) {
        cell.turns++
        TrainSounds.play(Sfx.POP)
      },
  ) {
    Canvas(Modifier.fillMaxSize()) {
      if (cell.track) drawTrack(cell.curve, highlight) else drawDecor(cell.decor, cell.col * 7 + cell.row)
    }
  }
}

private fun DrawScope.drawTrack(curve: Boolean, highlight: Boolean) {
  val t = size.width
  val pad = t * 0.04f
  drawRoundRect(
    if (highlight) Color(0xFFFFF59D) else Color(0xFFD7CCC8),
    Offset(pad, pad), Size(t - 2 * pad, t - 2 * pad),
    androidx.compose.ui.geometry.CornerRadius(t * 0.12f),
  )
  val rail = if (highlight) Color(0xFFFF8F00) else Color(0xFF607D8B)
  val sleeper = Color(0xFF6D4C41)
  val gauge = t * 0.22f
  val railW = t * 0.05f
  if (!curve) {
    // W-E straight
    var x = t * 0.06f
    while (x < t) {
      drawRect(sleeper, Offset(x, t / 2 - gauge * 0.85f), Size(t * 0.07f, gauge * 1.7f))
      x += t * 0.16f
    }
    drawLine(rail, Offset(0f, t / 2 - gauge / 2), Offset(t, t / 2 - gauge / 2), railW)
    drawLine(rail, Offset(0f, t / 2 + gauge / 2), Offset(t, t / 2 + gauge / 2), railW)
  } else {
    // N-E curve around the top-right corner
    val corner = Offset(t, 0f)
    for (k in 0..5) {
      val a = (PI / 2 + (k + 0.5) * (PI / 2) / 6).toFloat()
      val inner = corner + Offset(cos(a) * (t / 2 - gauge * 0.85f), sin(a) * (t / 2 - gauge * 0.85f))
      val outer = corner + Offset(cos(a) * (t / 2 + gauge * 0.85f), sin(a) * (t / 2 + gauge * 0.85f))
      drawLine(sleeper, inner, outer, t * 0.07f)
    }
    for (radius in listOf(t / 2 - gauge / 2, t / 2 + gauge / 2)) {
      drawArc(
        rail, startAngle = 90f, sweepAngle = 90f, useCenter = false,
        topLeft = corner - Offset(radius, radius), size = Size(radius * 2, radius * 2),
        style = Stroke(railW),
      )
    }
  }
}

private fun DrawScope.drawDecor(decor: Decor, seed: Int) {
  val t = size.width
  val c = Offset(t / 2, t / 2)
  for (k in 0..3) {
    val p = Offset(t * (0.15f + 0.23f * ((seed + k * 3) % 4)), t * (0.2f + 0.2f * k))
    drawLine(Color(0xFF558B2F), p, p + Offset(t * 0.02f, -t * 0.06f), t * 0.02f, StrokeCap.Round)
  }
  when (decor) {
    Decor.GRASS -> {}
    Decor.TREE -> {
      drawRect(Color(0xFF795548), Offset(c.x - t * 0.05f, c.y), Size(t * 0.1f, t * 0.3f))
      drawCircle(Color(0xFF2E7D32), t * 0.24f, c - Offset(0f, t * 0.05f))
      drawCircle(Color(0xFF43A047), t * 0.14f, c - Offset(t * 0.06f, t * 0.12f))
    }
    Decor.FLOWERS -> {
      val colors = listOf(Color(0xFFE53935), Color(0xFFFFEB3B), Color(0xFFAB47BC), Color.White)
      for (k in 0..4) {
        val p = Offset(t * (0.2f + 0.15f * k), t * (0.4f + 0.12f * ((k + seed) % 3)))
        for (petal in 0..4) {
          val a = petal * 2 * PI.toFloat() / 5
          drawCircle(colors[(k + seed) % colors.size], t * 0.035f, p + Offset(cos(a) * t * 0.04f, sin(a) * t * 0.04f))
        }
        drawCircle(Color(0xFFFFA000), t * 0.03f, p)
      }
    }
    Decor.HOUSE -> {
      drawRect(Color(0xFFFFCC80), Offset(t * 0.25f, t * 0.42f), Size(t * 0.5f, t * 0.38f))
      val roof = Path().apply {
        moveTo(t * 0.18f, t * 0.44f)
        lineTo(t * 0.5f, t * 0.15f)
        lineTo(t * 0.82f, t * 0.44f)
        close()
      }
      drawPath(roof, Color(0xFFD84315))
      drawRect(Color(0xFF6D4C41), Offset(t * 0.44f, t * 0.6f), Size(t * 0.13f, t * 0.2f))
      drawRect(Color(0xFF81D4FA), Offset(t * 0.3f, t * 0.5f), Size(t * 0.1f, t * 0.1f))
    }
    Decor.POND -> {
      drawOval(Color(0xFF29B6F6), Offset(t * 0.15f, t * 0.3f), Size(t * 0.7f, t * 0.42f))
      drawOval(Color(0xFFB3E5FC), Offset(t * 0.3f, t * 0.4f), Size(t * 0.2f, t * 0.08f))
    }
  }
}

private fun DrawScope.drawStation(topLeft: Offset, t: Float, goal: Boolean) {
  val x = topLeft.x
  val y = topLeft.y
  // short piece of straight track through the station
  drawRect(Color(0xFF6D4C41), Offset(x, y + t * 0.5f - t * 0.19f), Size(t, t * 0.38f))
  drawLine(Color(0xFF607D8B), Offset(x, y + t * 0.39f), Offset(x + t, y + t * 0.39f), t * 0.05f)
  drawLine(Color(0xFF607D8B), Offset(x, y + t * 0.61f), Offset(x + t, y + t * 0.61f), t * 0.05f)
  // building above the track
  val bx = x + t * 0.15f
  drawRect(Color(0xFFBF5B3C), Offset(bx, y + t * 0.02f), Size(t * 0.7f, t * 0.24f))
  drawRect(Color(0xFF5D4037), Offset(bx - t * 0.05f, y), Size(t * 0.8f, t * 0.06f))
  drawRect(Color(0xFFFFF59D), Offset(bx + t * 0.1f, y + t * 0.09f), Size(t * 0.12f, t * 0.1f))
  drawRect(Color(0xFFFFF59D), Offset(bx + t * 0.48f, y + t * 0.09f), Size(t * 0.12f, t * 0.1f))
  // platform below the track
  drawRect(Color(0xFFBDBDBD), Offset(x, y + t * 0.74f), Size(t, t * 0.2f))
  drawRect(Color(0xFFFFEB3B), Offset(x, y + t * 0.74f), Size(t, t * 0.03f))
  if (goal) {
    // chequered finish flag
    val px = x + t * 0.85f
    drawLine(Color(0xFF424242), Offset(px, y + t * 0.95f), Offset(px, y + t * 0.62f), t * 0.03f)
    val sq = t * 0.06f
    for (i in 0..3) for (j in 0..2) {
      drawRect(if ((i + j) % 2 == 0) Color.Black else Color.White, Offset(px + i * sq, y + t * 0.62f + j * sq), Size(sq, sq))
    }
  }
}
