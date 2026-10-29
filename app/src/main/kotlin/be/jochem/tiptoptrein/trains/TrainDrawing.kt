package be.jochem.tiptoptrein.trains

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin

// All train geometry is expressed in "units" (u). A train is about 10u tall, cars are 15u long.
// Trains always face right: wagons on the left, the locomotive at the right end.

const val CAR_LEN = 15f
const val CAR_GAP = 0.8f

fun locoLength(kind: TrainKind): Float = when (kind) {
  TrainKind.STEAM -> 17.5f
  TrainKind.SHINKANSEN -> 27f
  TrainKind.HIGH_SPEED, TrainKind.ICE, TrainKind.TGV, TrainKind.MAGLEV -> 22f
  TrainKind.TRAM -> 14f
  else -> 16f
}

fun trainLength(kind: TrainKind, cars: Int): Float = cars * (CAR_LEN + CAR_GAP) + locoLength(kind)

/** Horizontal centre (in px, relative to the train's left edge) of car [index]. */
fun carCenter(index: Int, u: Float): Float = (index * (CAR_LEN + CAR_GAP) + CAR_LEN / 2) * u

private val Ink = Color(0xFF263238)
private val Glass = Color(0xFF37474F)
private val GlassShine = Color(0xFF90CAF9)
private val Wheel = Color(0xFF424242)
private val TramYellow = Color(0xFFFFD200)

private val containerColors = listOf(
  Color(0xFF1E88E5), Color(0xFFE53935), Color(0xFF43A047), Color(0xFFFDD835), Color(0xFF8E24AA),
)

/**
 * Draws a complete train.
 *
 * @param left x of the train's left edge in px.
 * @param railY y of the top of the rail in px.
 * @param turn wheel rotation in radians.
 * @param time running time in seconds, used for smoke.
 * @param carColors optional body colors per wagon, overriding the kind's own colors.
 * @param liftCar index of a wagon to draw raised slightly (used when counting).
 */
fun DrawScope.drawTrain(
  kind: TrainKind,
  left: Float,
  railY: Float,
  u: Float,
  cars: Int = kind.cars,
  turn: Float = 0f,
  time: Float = 0f,
  carColors: List<Color>? = null,
  liftCar: Int = -1,
) {
  for (i in 0 until cars) {
    val x = left + i * (CAR_GAP + CAR_LEN) * u
    val lift = if (i == liftCar) -1.6f * u else 0f
    // coupler to the next vehicle
    drawLine(Ink, Offset(x + CAR_LEN * u, railY - 2.4f * u), Offset(x + (CAR_LEN + CAR_GAP) * u, railY - 2.4f * u), 0.4f * u)
    drawCar(kind, i, x, railY + lift, u, turn, carColors?.getOrNull(i))
  }
  val lx = left + cars * (CAR_GAP + CAR_LEN) * u
  drawLoco(kind, lx, railY, u, turn, time)
}

// ---- wagons ------------------------------------------------------------------------------

private fun DrawScope.drawCar(kind: TrainKind, index: Int, x: Float, railY: Float, u: Float, turn: Float, override: Color?) {
  when (kind) {
    TrainKind.STEAM -> {
      bogies(x, railY, u, CAR_LEN, turn)
      val top = railY - 9f * u
      drawRoundRect(Color(0xFF4E342E), Offset(x - 0.3f * u, top - 0.6f * u), Size((CAR_LEN + 0.6f) * u, 1.4f * u), CornerRadius(u))
      body(x, top, CAR_LEN * u, 6.6f * u, override ?: Color(0xFF8D3B2B), 0.4f * u)
      drawRect(Color(0xFFFFE0B2), Offset(x, top + 1.2f * u), Size(CAR_LEN * u, 2.6f * u))
      windows(x + 1f * u, top + 1.5f * u, 5, 2f * u, 2f * u, 0.7f * u, u)
      drawRect(Color(0xFFFFC107), Offset(x, top + 4.6f * u), Size(CAR_LEN * u, 0.3f * u))
    }
    TrainKind.FREIGHT -> {
      bogies(x, railY, u, CAR_LEN, turn)
      drawRect(Color(0xFF5D4037), Offset(x, railY - 3.3f * u), Size(CAR_LEN * u, 1f * u))
      val color = override ?: containerColors[index % containerColors.size]
      if (index % 3 == 2 && override == null) {
        // coal hopper
        val p = Path().apply {
          moveTo(x + 0.5f * u, railY - 8.5f * u)
          lineTo(x + (CAR_LEN - 0.5f) * u, railY - 8.5f * u)
          lineTo(x + (CAR_LEN - 2f) * u, railY - 3.3f * u)
          lineTo(x + 2f * u, railY - 3.3f * u)
          close()
        }
        drawPath(p, Color(0xFF6D4C41))
        for (k in 0..5) drawCircle(Ink, 1.2f * u, Offset(x + (2.2f + k * 2.1f) * u, railY - 8.6f * u))
      } else {
        val top = railY - 9f * u
        drawRect(color, Offset(x + 0.4f * u, top), Size((CAR_LEN - 0.8f) * u, 5.7f * u))
        for (k in 1..9) {
          val rx = x + (0.4f + k * 1.42f) * u
          drawLine(color.darker(0.18f), Offset(rx, top + 0.4f * u), Offset(rx, top + 5.3f * u), 0.35f * u)
        }
        drawRect(color.darker(0.3f), Offset(x + 0.4f * u, top), Size((CAR_LEN - 0.8f) * u, 5.7f * u), style = Stroke(0.3f * u))
      }
    }
    TrainKind.TRAM -> {
      bogies(x, railY, u, CAR_LEN, turn, r = 0.8f)
      val top = railY - 8.6f * u
      val c = override ?: TramYellow
      body(x, top, CAR_LEN * u, 6.8f * u, Color.White, 1.2f * u)
      drawRect(c, Offset(x, top + 4.4f * u), Size(CAR_LEN * u, 2.4f * u))
      windows(x + 1f * u, top + 1f * u, 4, 2.7f * u, 3f * u, 0.6f * u, u)
      door(x + 6.3f * u, top + 0.8f * u, u, c)
    }
    else -> {
      val s = electricStyle(kind)
      if (kind == TrainKind.MAGLEV) hover(x, railY, u, CAR_LEN) else bogies(x, railY, u, CAR_LEN, turn)
      val h = s.height * u
      val top = railY - 1.8f * u - h
      val c = override ?: s.body
      body(x, top, CAR_LEN * u, h, c, s.corner * u)
      drawRect(s.stripe, Offset(x, top + h - 1.4f * u), Size(CAR_LEN * u, 1f * u))
      if (s.decks == 2) {
        windows(x + 1f * u, top + 0.9f * u, 5, 2f * u, 2f * u, 0.7f * u, u)
        windows(x + 1f * u, top + 4.2f * u, 5, 2f * u, 2f * u, 0.7f * u, u)
      } else {
        windows(x + 1f * u, top + 1.2f * u, 5, 2f * u, 2.4f * u, 0.7f * u, u)
      }
      s.door?.let { door(x + 0.2f * u, top + h - 6.2f * u, u, it, w = 1.6f, hgt = 4.6f) }
      s.door?.let { door(x + (CAR_LEN - 1.8f) * u, top + h - 6.2f * u, u, it, w = 1.6f, hgt = 4.6f) }
      if (override != null) drawRect(s.stripe, Offset(x, top + h - 1.4f * u), Size(CAR_LEN * u, 1f * u))
      if (kind == TrainKind.MAGLEV) drawRect(Color(0xFFFF8F00), Offset(x, top + h - 0.4f * u), Size(CAR_LEN * u, 0.3f * u))
    }
  }
}

private data class ElectricStyle(
  val body: Color,
  val stripe: Color,
  val nose: Color,
  val height: Float,
  val corner: Float,
  val decks: Int = 1,
  val door: Color? = null,
)

// Colors taken from photos: NMBS trains are white with a grey-blue band, red doors and a
// yellow nose. The Eurostar is silver-grey with a yellow nose.
private val NmbsBand = Color(0xFF4A5868)
private val NmbsYellow = Color(0xFFFFD100)
private val NmbsRed = Color(0xFFD32F2F)
private val NmbsWhite = Color(0xFFF7F7F7)

private fun electricStyle(kind: TrainKind) = when (kind) {
  TrainKind.INTERCITY -> ElectricStyle(NmbsWhite, NmbsBand, NmbsYellow, 7f, 0.6f, door = NmbsRed)
  TrainKind.SPRINTER -> ElectricStyle(Color(0xFFEDEDED), NmbsBand, NmbsYellow, 7f, 0.6f, door = NmbsRed)
  TrainKind.DOUBLE_DECKER -> ElectricStyle(NmbsWhite, NmbsBand, NmbsYellow, 8.8f, 0.8f, decks = 2, door = NmbsRed)
  TrainKind.HIGH_SPEED -> ElectricStyle(Color(0xFFCFD5DB), Color(0xFF14285A), NmbsYellow, 6.6f, 1.2f)
  // ICE: white with a red stripe. TGV: white with a dark nose and red stripe.
  // Shinkansen: white with a blue stripe. Maglev: white with a blue stripe and an orange line.
  TrainKind.ICE -> ElectricStyle(Color(0xFFF5F5F5), Color(0xFFEC0016), Color(0xFFF5F5F5), 6.6f, 1.2f)
  TrainKind.TGV -> ElectricStyle(Color(0xFFF2F2F2), Color(0xFFC8102E), Color(0xFF3A3F47), 6.8f, 1.2f)
  TrainKind.SHINKANSEN -> ElectricStyle(Color.White, Color(0xFF1A4CA0), Color.White, 6.4f, 1.4f)
  TrainKind.MAGLEV -> ElectricStyle(Color(0xFFF8F8F8), Color(0xFF1E5AA8), Color(0xFFF8F8F8), 6.6f, 1.6f)
  TrainKind.METRO -> ElectricStyle(Color(0xFFB0BEC5), Color(0xFF37474F), Color(0xFF37474F), 7.2f, 1.4f, door = Color(0xFF37474F))
  else -> ElectricStyle(Color.Gray, Color.DarkGray, Color.Gray, 7f, 0.6f)
}

// ---- locomotives -------------------------------------------------------------------------

private fun DrawScope.drawLoco(kind: TrainKind, x: Float, railY: Float, u: Float, turn: Float, time: Float) {
  when (kind) {
    TrainKind.STEAM -> steamLoco(x, railY, u, turn, time)
    TrainKind.FREIGHT -> dieselLoco(x, railY, u, turn, time)
    TrainKind.TRAM -> tramFront(x, railY, u, turn)
    else -> electricLoco(kind, x, railY, u, turn)
  }
}

private fun DrawScope.steamLoco(x: Float, railY: Float, u: Float, turn: Float, time: Float) {
  val red = Color(0xFFD32F2F)
  val boiler = Color(0xFF2E7D32)
  // smoke behind everything else
  smoke(x + 14.2f * u, railY - 11f * u, u, time, Color(0xFFECEFF1), 1f)
  // frame
  drawRect(red, Offset(x, railY - 3.6f * u), Size(17f * u, 1.1f * u))
  // cowcatcher
  val catcher = Path().apply {
    moveTo(x + 16.4f * u, railY - 2.6f * u)
    lineTo(x + 17.6f * u, railY - 2.6f * u)
    lineTo(x + 18.6f * u, railY - 0.3f * u)
    lineTo(x + 16.4f * u, railY - 0.3f * u)
    close()
  }
  drawPath(catcher, red)
  // cab
  drawRect(boiler, Offset(x, railY - 10f * u), Size(5.2f * u, 6.6f * u))
  drawRoundRect(Ink, Offset(x - 0.5f * u, railY - 10.8f * u), Size(6.2f * u, 1f * u), CornerRadius(0.5f * u))
  drawRoundRect(Color(0xFFFFF59D), Offset(x + 1f * u, railY - 9.2f * u), Size(3.2f * u, 2.6f * u), CornerRadius(0.4f * u))
  drawRect(Color(0xFFFFC107), Offset(x, railY - 5.4f * u), Size(5.2f * u, 0.35f * u))
  // boiler
  drawRoundRect(boiler, Offset(x + 5f * u, railY - 8.4f * u), Size(11.4f * u, 4.9f * u), CornerRadius(1.4f * u))
  for (bx in listOf(7.5f, 10.5f, 13.5f)) {
    drawRect(Color(0xFFFFC107), Offset(x + bx * u, railY - 8.4f * u), Size(0.4f * u, 4.9f * u))
  }
  drawRoundRect(Ink, Offset(x + 15.8f * u, railY - 8.2f * u), Size(1f * u, 4.5f * u), CornerRadius(0.5f * u))
  // dome and chimney
  drawCircle(Color(0xFFFFC107), 1.1f * u, Offset(x + 9f * u, railY - 8.4f * u))
  drawRect(Ink, Offset(x + 13.6f * u, railY - 10.6f * u), Size(1.4f * u, 2.4f * u))
  drawRect(Ink, Offset(x + 13.1f * u, railY - 11.2f * u), Size(2.4f * u, 0.8f * u))
  // lamp
  drawCircle(Color(0xFFFFEB3B), 0.7f * u, Offset(x + 16.4f * u, railY - 9f * u))
  drawRect(Ink, Offset(x + 16f * u, railY - 8.4f * u), Size(0.8f * u, 0.3f * u))
  // wheels: two big driving wheels, one small leading wheel
  val big = 1.9f * u
  val w1 = Offset(x + 6.6f * u, railY - big)
  val w2 = Offset(x + 11f * u, railY - big)
  wheel(Offset(x + 2.6f * u, railY - 1.2f * u), 1.2f * u, turn, red)
  wheel(w1, big, turn, red)
  wheel(w2, big, turn, red)
  wheel(Offset(x + 14.8f * u, railY - 1.1f * u), 1.1f * u, turn, red)
  // connecting rod
  val crank = 1.1f * u
  val dx = cos(turn) * crank
  val dy = sin(turn) * crank
  drawLine(Color(0xFFB0BEC5), Offset(w1.x + dx, w1.y + dy), Offset(w2.x + dx, w2.y + dy), 0.5f * u, StrokeCap.Round)
  drawLine(Color(0xFFB0BEC5), Offset(w2.x + dx, w2.y + dy), Offset(x + 15f * u, railY - 2.8f * u), 0.4f * u, StrokeCap.Round)
}

private fun DrawScope.dieselLoco(x: Float, railY: Float, u: Float, turn: Float, time: Float) {
  val orange = Color(0xFFF57C00)
  smoke(x + 4f * u, railY - 8f * u, u, time, Color(0xFF9E9E9E), 0.6f)
  bogies(x, railY, u, 16f, turn)
  drawRect(Ink, Offset(x, railY - 3.4f * u), Size(16f * u, 1.2f * u))
  // long hood
  drawRoundRect(orange, Offset(x + 0.8f * u, railY - 7.6f * u), Size(10f * u, 4.2f * u), CornerRadius(0.5f * u))
  for (k in 0..4) drawRect(orange.darker(0.2f), Offset(x + (1.8f + k * 1.8f) * u, railY - 6.8f * u), Size(0.8f * u, 2.6f * u))
  drawRect(Ink, Offset(x + 3.3f * u, railY - 8.2f * u), Size(1.4f * u, 0.6f * u))
  // cab
  drawRoundRect(orange, Offset(x + 10.6f * u, railY - 10f * u), Size(5.4f * u, 6.6f * u), CornerRadius(0.6f * u))
  drawRoundRect(Glass, Offset(x + 11.4f * u, railY - 9.2f * u), Size(3.8f * u, 2.4f * u), CornerRadius(0.3f * u))
  drawRect(Color.White.copy(alpha = 0.35f), Offset(x + 11.7f * u, railY - 9f * u), Size(0.8f * u, 2f * u))
  // warning stripes
  for (k in 0..3) {
    drawRect(if (k % 2 == 0) Color(0xFFFFEB3B) else Ink, Offset(x + (10.6f + k * 1.35f) * u, railY - 4.4f * u), Size(1.35f * u, 1f * u))
  }
  drawCircle(Color(0xFFFFEB3B), 0.5f * u, Offset(x + 15.6f * u, railY - 5f * u))
}

private fun DrawScope.tramFront(x: Float, railY: Float, u: Float, turn: Float) {
  val red = TramYellow
  val len = 14f
  bogies(x, railY, u, len, turn, r = 0.8f)
  val top = railY - 8.6f * u
  pantograph(x + 4f * u, top, u)
  val p = Path().apply {
    moveTo(x, top)
    lineTo(x + (len - 2.5f) * u, top)
    quadraticTo(x + len * u, top, x + len * u, top + 3f * u)
    lineTo(x + len * u, top + 6.8f * u)
    lineTo(x, top + 6.8f * u)
    close()
  }
  drawPath(p, Color.White)
  drawRect(red, Offset(x, top + 4.4f * u), Size(len * u, 2.4f * u))
  windows(x + 1f * u, top + 1f * u, 2, 2.7f * u, 3f * u, 0.6f * u, u)
  door(x + 7f * u, top + 0.8f * u, u, red)
  val windscreen = Path().apply {
    moveTo(x + 10f * u, top + 0.8f * u)
    lineTo(x + (len - 2.2f) * u, top + 0.8f * u)
    quadraticTo(x + (len - 0.4f) * u, top + 1f * u, x + (len - 0.3f) * u, top + 4f * u)
    lineTo(x + 10f * u, top + 4f * u)
    close()
  }
  drawPath(windscreen, Glass)
  drawCircle(Color(0xFFFFEB3B), 0.45f * u, Offset(x + (len - 0.7f) * u, top + 5.6f * u))
}

private fun DrawScope.electricLoco(kind: TrainKind, x: Float, railY: Float, u: Float, turn: Float) {
  val s = electricStyle(kind)
  val len = locoLength(kind)
  val h = s.height * u
  val top = railY - 1.8f * u - h
  val bottom = top + h
  if (kind == TrainKind.MAGLEV) hover(x, railY, u, len - 1f) else bogies(x, railY, u, len - 1f, turn)
  if (kind != TrainKind.METRO && kind != TrainKind.MAGLEV) pantograph(x + 4f * u, top, u)

  val noseStart = when (kind) {
    TrainKind.SHINKANSEN -> 11f
    TrainKind.HIGH_SPEED, TrainKind.ICE, TrainKind.TGV, TrainKind.MAGLEV -> 12f
    TrainKind.METRO -> 14.6f
    else -> 12.6f
  }
  val p = Path().apply {
    moveTo(x + s.corner * u, top)
    lineTo(x + noseStart * u, top)
    when (kind) {
      TrainKind.SHINKANSEN -> {
        // long flat "duck bill" nose
        cubicTo(x + 16f * u, top, x + (len - 6f) * u, bottom - 3.4f * u, x + len * u, bottom - 1.6f * u)
        lineTo(x + len * u, bottom)
      }
      TrainKind.HIGH_SPEED, TrainKind.ICE, TrainKind.TGV, TrainKind.MAGLEV -> {
        cubicTo(x + 17f * u, top, x + len * u, bottom - 2.6f * u, x + len * u, bottom - 0.8f * u)
        lineTo(x + len * u, bottom)
      }
      TrainKind.METRO -> {
        quadraticTo(x + len * u, top, x + len * u, top + 1.4f * u)
        lineTo(x + len * u, bottom)
      }
      else -> {
        quadraticTo(x + (len - 0.6f) * u, top + 0.3f * u, x + len * u, top + 4f * u)
        lineTo(x + len * u, bottom)
      }
    }
    lineTo(x, bottom)
    lineTo(x, top + s.corner * u)
    close()
  }
  drawPath(p, s.body)
  // stripe along the bottom, and a colored nose
  drawRect(s.stripe, Offset(x, bottom - 1.4f * u), Size((len - 0.1f) * u, 1f * u))
  if (kind == TrainKind.MAGLEV) drawRect(Color(0xFFFF8F00), Offset(x, bottom - 0.4f * u), Size((len - 0.4f) * u, 0.3f * u))
  if (kind == TrainKind.TGV) {
    // TGV: the whole nose is dark grey
    val nose = Path().apply {
      moveTo(x + (len - 9f) * u, top)
      cubicTo(x + 17f * u, top, x + len * u, bottom - 2.6f * u, x + len * u, bottom - 0.8f * u)
      lineTo(x + len * u, bottom)
      lineTo(x + (len - 6f) * u, bottom)
      close()
    }
    drawPath(nose, s.nose)
  } else if (kind != TrainKind.METRO) {
    val nose = Path().apply {
      moveTo(x + (len - 4.5f) * u, bottom)
      lineTo(x + len * u, bottom)
      lineTo(x + len * u, bottom - 3.4f * u)
      lineTo(x + (len - 2f) * u, bottom - 3.4f * u)
      close()
    }
    drawPath(nose, s.nose)
  }
  // side windows
  if (s.decks == 2) {
    windows(x + 1f * u, top + 0.9f * u, 4, 2f * u, 2f * u, 0.7f * u, u)
    windows(x + 1f * u, top + 4.2f * u, 4, 2f * u, 2f * u, 0.7f * u, u)
  } else {
    windows(x + 1f * u, top + 1.2f * u, if (kind == TrainKind.HIGH_SPEED) 4 else 4, 2f * u, 2.4f * u, 0.7f * u, u)
  }
  s.door?.let { door(x + 0.2f * u, bottom - 6.2f * u, u, it, w = 1.6f, hgt = 4.6f) }
  // windscreen
  val ws = Path().apply {
    when (kind) {
      TrainKind.HIGH_SPEED, TrainKind.ICE, TrainKind.TGV, TrainKind.MAGLEV, TrainKind.SHINKANSEN -> {
        moveTo(x + (len - 8.5f) * u, top + 0.5f * u)
        quadraticTo(x + (len - 4.5f) * u, top + 0.6f * u, x + (len - 2.4f) * u, top + 3f * u)
        lineTo(x + (len - 7f) * u, top + 3f * u)
        close()
      }
      TrainKind.METRO -> {
        moveTo(x + (len - 0.4f) * u, top + 1f * u)
        lineTo(x + (len - 0.4f) * u, top + 3.8f * u)
        lineTo(x + (len - 2.6f) * u, top + 3.8f * u)
        lineTo(x + (len - 2.6f) * u, top + 1f * u)
        close()
      }
      else -> {
        moveTo(x + 13f * u, top + 0.8f * u)
        quadraticTo(x + 15f * u, top + 1f * u, x + 15.5f * u, top + 3.6f * u)
        lineTo(x + 13f * u, top + 3.6f * u)
        close()
      }
    }
  }
  drawPath(ws, Glass)
  drawCircle(Color(0xFFFFF9C4), 0.45f * u, Offset(x + (len - 0.5f) * u, bottom - 2.2f * u))
}

// ---- parts -------------------------------------------------------------------------------

/** Maglev: no wheels, just a guide block and a glowing gap above the rail. */
private fun DrawScope.hover(x: Float, railY: Float, u: Float, len: Float) {
  drawRect(Color(0xFF90A4AE), Offset(x + 1f * u, railY - 1.6f * u), Size((len - 2f) * u, 0.6f * u))
  drawRect(Color(0xFF80DEEA).copy(alpha = 0.6f), Offset(x + 1f * u, railY - 1f * u), Size((len - 2f) * u, 0.7f * u))
}

private fun DrawScope.body(x: Float, top: Float, w: Float, h: Float, color: Color, r: Float) {
  drawRoundRect(color, Offset(x, top), Size(w, h), CornerRadius(r))
  drawRoundRect(color.darker(0.25f), Offset(x, top), Size(w, h), CornerRadius(r), style = Stroke(0.25f * h / 7f))
}

private fun DrawScope.windows(x: Float, y: Float, n: Int, w: Float, h: Float, gap: Float, u: Float) {
  for (i in 0 until n) {
    val wx = x + i * (w + gap)
    drawRoundRect(Glass, Offset(wx, y), Size(w, h), CornerRadius(0.3f * u))
    drawRect(GlassShine.copy(alpha = 0.45f), Offset(wx + 0.25f * u, y + 0.25f * u), Size(0.4f * u, h - 0.5f * u))
  }
}

private fun DrawScope.door(x: Float, y: Float, u: Float, color: Color, w: Float = 2.4f, hgt: Float = 5.6f) {
  drawRoundRect(color, Offset(x, y), Size(w * u, hgt * u), CornerRadius(0.3f * u))
  drawRoundRect(Glass, Offset(x + 0.3f * u, y + 0.4f * u), Size((w - 0.6f) * u, hgt * 0.4f * u), CornerRadius(0.2f * u))
}

private fun DrawScope.pantograph(x: Float, top: Float, u: Float) {
  val c = Color(0xFF616161)
  val sw = 0.3f * u
  drawLine(c, Offset(x, top), Offset(x + 1.5f * u, top - 1.6f * u), sw)
  drawLine(c, Offset(x + 3f * u, top), Offset(x + 1.5f * u, top - 1.6f * u), sw)
  drawLine(c, Offset(x + 1.5f * u, top - 1.6f * u), Offset(x + 0.4f * u, top - 3f * u), sw)
  drawLine(c, Offset(x - 0.6f * u, top - 3f * u), Offset(x + 2.2f * u, top - 3f * u), sw * 1.4f, StrokeCap.Round)
}

private fun DrawScope.bogies(x: Float, railY: Float, u: Float, len: Float, turn: Float, r: Float = 1f) {
  val cy = railY - r * u
  for (cx in listOf(2f, 4.4f, len - 4.4f, len - 2f)) wheel(Offset(x + cx * u, cy), r * u, turn, Wheel)
  drawRect(Ink, Offset(x + 1f * u, railY - (r + 0.6f) * u), Size(4.4f * u, 0.6f * u))
  drawRect(Ink, Offset(x + (len - 5.4f) * u, railY - (r + 0.6f) * u), Size(4.4f * u, 0.6f * u))
}

private fun DrawScope.wheel(c: Offset, r: Float, turn: Float, color: Color) {
  drawCircle(Ink, r, c)
  drawCircle(color, r * 0.78f, c)
  for (k in 0 until 3) {
    val a = turn + k * (Math.PI / 3).toFloat()
    val d = Offset(cos(a) * r * 0.75f, sin(a) * r * 0.75f)
    drawLine(Ink.copy(alpha = 0.7f), c - d, c + d, r * 0.14f)
  }
  drawCircle(Color(0xFFBDBDBD), r * 0.22f, c)
}

private fun DrawScope.smoke(x: Float, y: Float, u: Float, time: Float, color: Color, scale: Float) {
  for (k in 0 until 5) {
    val phase = ((time * 0.7f + k / 5f) % 1f)
    val r = (0.9f + phase * 2.6f) * u * scale
    drawCircle(
      color.copy(alpha = (1f - phase) * 0.85f),
      r,
      Offset(x - phase * 7f * u * scale, y - phase * 7f * u * scale),
    )
  }
}

fun Color.darker(amount: Float): Color =
  Color(red * (1 - amount), green * (1 - amount), blue * (1 - amount), alpha)

// ---- scenery -----------------------------------------------------------------------------

/** Rails with sleepers. [scroll] moves the sleepers left as the train drives. */
fun DrawScope.drawRails(railY: Float, u: Float, scroll: Float = 0f) {
  drawRect(Color(0xFF9E8C7A), Offset(0f, railY), Size(size.width, 2.4f * u))
  val spacing = 3f * u
  var sx = -(scroll % spacing)
  while (sx < size.width) {
    drawRect(Color(0xFF6D4C41), Offset(sx, railY + 0.3f * u), Size(1.4f * u, 1.3f * u))
    sx += spacing
  }
  drawRect(Color(0xFF78909C), Offset(0f, railY - 0.1f * u), Size(size.width, 0.5f * u))
}

fun DrawScope.drawScenery(scenery: Scenery, railY: Float, u: Float, scroll: Float = 0f, time: Float = 0f) {
  val w = size.width
  when (scenery) {
    Scenery.TUNNEL -> {
      drawRect(Color(0xFF263238), Offset.Zero, size)
      // tunnel lights rushing by
      val spacing = 30f * u
      var lx = -((scroll * 0.9f) % spacing)
      while (lx < w) {
        drawCircle(Color(0xFFFFE082).copy(alpha = 0.25f), 3f * u, Offset(lx, railY - 15f * u))
        drawCircle(Color(0xFFFFE082), 1f * u, Offset(lx, railY - 15f * u))
        lx += spacing
      }
      drawRect(Color(0xFF37474F), Offset(0f, railY), Size(w, size.height - railY))
      drawRect(Color(0xFF455A64), Offset(0f, railY - 20f * u), Size(w, 1.4f * u))
    }
    else -> {
      val sky = when (scenery) {
        Scenery.CITY -> Color(0xFFB3E5FC)
        Scenery.BEACH -> Color(0xFF81D4FA)
        Scenery.PORT -> Color(0xFFBBDEFB)
        Scenery.MOUNTAIN -> Color(0xFFE1F5FE)
        else -> Color(0xFF81D4FA)
      }
      drawRect(sky, Offset.Zero, size)
      drawCircle(Color(0xFFFFEB3B), 5f * u, Offset(w * 0.85f, railY * 0.22f))
      clouds(railY, u, scroll * 0.1f + time * 6f * u)
      when (scenery) {
        Scenery.MOUNTAIN -> {
          mountains(railY, u, scroll * 0.15f)
          hills(railY, u, scroll * 0.4f, Color(0xFF7CB342))
        }
        Scenery.CITY -> city(railY, u, scroll * 0.4f)
        Scenery.BEACH -> beach(railY, u, scroll * 0.3f, time)
        Scenery.PORT -> port(railY, u, scroll * 0.3f)
        else -> {
          hills(railY, u, scroll * 0.25f, Color(0xFF9CCC65))
          trees(railY, u, scroll * 0.6f)
        }
      }
      drawRect(
        when (scenery) {
          Scenery.CITY -> Color(0xFF9E9E9E)
          Scenery.BEACH -> Color(0xFFF3D9A4)
          else -> Color(0xFF8BC34A)
        },
        Offset(0f, railY),
        Size(w, size.height - railY),
      )
    }
  }
}

private fun DrawScope.clouds(railY: Float, u: Float, scroll: Float) {
  val spacing = 45f * u
  val span = size.width + spacing * 2
  for (k in 0 until (size.width / spacing).toInt() + 3) {
    val cx = ((k * spacing - scroll) % span + span) % span - spacing
    val cy = railY * (0.15f + 0.12f * ((k * 7) % 3))
    for ((ox, r) in listOf(0f to 3f, 3f to 4f, 6.5f to 3f)) {
      drawCircle(Color.White.copy(alpha = 0.9f), r * u, Offset(cx + ox * u, cy))
    }
  }
}

private fun DrawScope.hills(railY: Float, u: Float, scroll: Float, color: Color) {
  val spacing = 40f * u
  val span = size.width + spacing * 2
  for (k in 0 until (size.width / spacing).toInt() + 3) {
    val cx = ((k * spacing - scroll) % span + span) % span - spacing
    drawCircle(color, 22f * u, Offset(cx, railY + 14f * u))
  }
}

private fun DrawScope.mountains(railY: Float, u: Float, scroll: Float) {
  val spacing = 50f * u
  val span = size.width + spacing * 2
  for (k in 0 until (size.width / spacing).toInt() + 3) {
    val cx = ((k * spacing - scroll) % span + span) % span - spacing
    val h = (26f + (k % 3) * 6f) * u
    val p = Path().apply {
      moveTo(cx - 30f * u, railY)
      lineTo(cx, railY - h)
      lineTo(cx + 30f * u, railY)
      close()
    }
    drawPath(p, Color(0xFF90A4AE))
    val snow = Path().apply {
      moveTo(cx - 7f * u, railY - h + 7f * u)
      lineTo(cx, railY - h)
      lineTo(cx + 7f * u, railY - h + 7f * u)
      close()
    }
    drawPath(snow, Color.White)
  }
}

private fun DrawScope.trees(railY: Float, u: Float, scroll: Float) {
  val spacing = 23f * u
  val span = size.width + spacing * 2
  for (k in 0 until (size.width / spacing).toInt() + 3) {
    val cx = ((k * spacing - scroll) % span + span) % span - spacing
    val base = railY - 0.5f * u
    drawRect(Color(0xFF795548), Offset(cx - 0.6f * u, base - 5f * u), Size(1.2f * u, 5f * u))
    drawCircle(Color(0xFF388E3C), 3.2f * u, Offset(cx, base - 7f * u))
  }
}

private val buildingColors = listOf(
  Color(0xFFFFAB91), Color(0xFFFFE082), Color(0xFFA5D6A7), Color(0xFF90CAF9), Color(0xFFCE93D8),
)

private fun DrawScope.city(railY: Float, u: Float, scroll: Float) {
  val spacing = 13f * u
  val span = size.width + spacing * 2
  for (k in 0 until (size.width / spacing).toInt() + 3) {
    val bx = ((k * spacing - scroll) % span + span) % span - spacing
    val h = (16f + (k * 5 % 4) * 4f) * u
    val c = buildingColors[k % buildingColors.size]
    drawRect(c, Offset(bx, railY - h), Size(12f * u, h))
    // stepped gable like a Dutch canal house
    drawRect(c, Offset(bx + 3f * u, railY - h - 2f * u), Size(6f * u, 2f * u))
    drawRect(c, Offset(bx + 4.5f * u, railY - h - 3.5f * u), Size(3f * u, 1.5f * u))
    for (row in 0 until (h / (5f * u)).toInt()) {
      for (col in 0..1) {
        drawRect(Color.White, Offset(bx + (2f + col * 5f) * u, railY - h + (2f + row * 5f) * u), Size(3f * u, 3f * u))
      }
    }
  }
  // overhead wire for the tram
  drawLine(Color(0xFF424242), Offset(0f, railY - 13.5f * u), Offset(size.width, railY - 13.5f * u), 0.25f * u)
}

private fun DrawScope.port(railY: Float, u: Float, scroll: Float) {
  drawRect(Color(0xFF4FC3F7), Offset(0f, railY - 8f * u), Size(size.width, 8f * u))
  val spacing = 40f * u
  val span = size.width + spacing * 2
  for (k in 0 until (size.width / spacing).toInt() + 3) {
    val cx = ((k * spacing - scroll) % span + span) % span - spacing
    // crane
    val crane = Color(0xFFE53935)
    drawRect(crane, Offset(cx, railY - 30f * u), Size(1.4f * u, 22f * u))
    drawRect(crane, Offset(cx + 10f * u, railY - 30f * u), Size(1.4f * u, 22f * u))
    drawRect(crane, Offset(cx - 6f * u, railY - 31f * u), Size(24f * u, 1.6f * u))
    // stacked containers
    for (s in 0..2) {
      drawRect(containerColors[(k + s) % containerColors.size], Offset(cx + 20f * u, railY - (6f + s * 4f) * u), Size(12f * u, 3.6f * u))
    }
  }
}

private fun DrawScope.beach(railY: Float, u: Float, scroll: Float, time: Float) {
  // sea with rolling waves, a strip of sand, then dunes with beach grass by the track
  drawRect(Color(0xFF29B6F6), Offset(0f, railY - 16f * u), Size(size.width, 8f * u))
  val waveSpacing = 8f * u
  var wx = -((scroll + time * 3f * u) % waveSpacing)
  while (wx < size.width) {
    drawCircle(Color.White.copy(alpha = 0.7f), 1.2f * u, Offset(wx, railY - 9f * u))
    wx += waveSpacing
  }
  drawRect(Color(0xFFFFE0A3), Offset(0f, railY - 8.5f * u), Size(size.width, 8.5f * u))
  val spacing = 36f * u
  val span = size.width + spacing * 2
  for (k in 0 until (size.width / spacing).toInt() + 3) {
    val cx = ((k * spacing - scroll) % span + span) % span - spacing
    // beach cabin and parasol
    drawRect(listOf(Color(0xFFE53935), Color(0xFF1E88E5), Color(0xFF43A047))[k % 3], Offset(cx, railY - 14f * u), Size(5f * u, 6f * u))
    drawRect(Color.White, Offset(cx + 1.6f * u, railY - 12f * u), Size(1.8f * u, 4f * u))
    val roof = Path().apply {
      moveTo(cx - 0.8f * u, railY - 14f * u)
      lineTo(cx + 2.5f * u, railY - 16.5f * u)
      lineTo(cx + 5.8f * u, railY - 14f * u)
      close()
    }
    drawPath(roof, Color(0xFF795548))
    drawLine(Color(0xFF5D4037), Offset(cx + 16f * u, railY - 8f * u), Offset(cx + 16f * u, railY - 14f * u), 0.3f * u)
    drawArc(Color(0xFFFF7043), 180f, 180f, true, Offset(cx + 12f * u, railY - 16f * u), Size(8f * u, 4f * u))
    // dune with grass
    drawCircle(Color(0xFFEBCB8B), 9f * u, Offset(cx + 26f * u, railY + 3f * u))
    for (g in -2..2) {
      drawLine(Color(0xFF7CB342), Offset(cx + (26f + g * 2f) * u, railY - 5f * u), Offset(cx + (25.5f + g * 2.4f) * u, railY - 8f * u), 0.3f * u)
    }
  }
}
