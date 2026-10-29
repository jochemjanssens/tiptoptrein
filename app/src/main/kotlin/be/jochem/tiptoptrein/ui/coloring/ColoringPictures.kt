package be.jochem.tiptoptrein.ui.coloring

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Path
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// Coloring pages are drawn in a fixed 100 x 70 space and scaled to the screen.
const val PICTURE_W = 100f
const val PICTURE_H = 70f

/** One area of a coloring page. Areas listed later are drawn on top and tapped first. */
sealed class Part {
  abstract val path: Path
  open val fillable: Boolean = true
  abstract fun contains(p: Offset): Boolean
}

class BoxPart(val l: Float, val t: Float, val w: Float, val h: Float, val r: Float = 0f) : Part() {
  override val path = Path().apply { addRoundRect(RoundRect(Rect(l, t, l + w, t + h), CornerRadius(r))) }
  override fun contains(p: Offset) = p.x in l..(l + w) && p.y in t..(t + h)
}

class CirclePart(val cx: Float, val cy: Float, val r: Float) : Part() {
  override val path = Path().apply { addOval(Rect(cx - r, cy - r, cx + r, cy + r)) }
  override fun contains(p: Offset) = (p.x - cx) * (p.x - cx) + (p.y - cy) * (p.y - cy) <= r * r
}

class Poly(vararg xy: Float) : Part() {
  private val pts = xy.toList().chunked(2) { Offset(it[0], it[1]) }
  override val path = Path().apply {
    moveTo(pts[0].x, pts[0].y)
    pts.drop(1).forEach { lineTo(it.x, it.y) }
    close()
  }

  // ray casting
  override fun contains(p: Offset): Boolean {
    var inside = false
    var j = pts.lastIndex
    for (i in pts.indices) {
      val a = pts[i]
      val b = pts[j]
      if ((a.y > p.y) != (b.y > p.y) && p.x < (b.x - a.x) * (p.y - a.y) / (b.y - a.y) + a.x) inside = !inside
      j = i
    }
    return inside
  }
}

/** A plain line, only outlined, never colored. */
class LinePart(x1: Float, y1: Float, x2: Float, y2: Float) : Part() {
  override val path = Path().apply {
    moveTo(x1, y1)
    lineTo(x2, y2)
  }
  override val fillable = false
  override fun contains(p: Offset) = false
}

/** Top half of a circle, e.g. a parasol. */
private fun dome(cx: Float, cy: Float, r: Float): Poly {
  val xy = mutableListOf<Float>()
  for (k in 0..12) {
    val a = PI + k * PI / 12
    xy += cx + (cos(a) * r).toFloat()
    xy += cy + (sin(a) * r * 0.6f).toFloat()
  }
  return Poly(*xy.toFloatArray())
}

class ColoringPicture(val name: String, val parts: List<Part>)

val coloringPictures = listOf(
  ColoringPicture(
    "Stoomtrein",
    listOf(
      BoxPart(0f, 0f, 100f, 52f),
      CirclePart(88f, 12f, 7f),
      CirclePart(20f, 13f, 5f), CirclePart(27f, 10f, 6f), CirclePart(34f, 13f, 5f),
      BoxPart(0f, 52f, 100f, 18f),
      BoxPart(4f, 56f, 92f, 2.5f),
      CirclePart(54f, 9f, 5f), CirclePart(62f, 15f, 4f),
      BoxPart(12f, 22f, 16f, 26f),
      BoxPart(10f, 19f, 20f, 4f, 1f),
      BoxPart(15f, 26f, 9f, 8f, 1f),
      CirclePart(44f, 30f, 4f),
      BoxPart(56f, 17f, 10f, 3f),
      BoxPart(58f, 20f, 6f, 10f),
      BoxPart(28f, 30f, 40f, 16f, 4f),
      BoxPart(37f, 30f, 2f, 16f), BoxPart(50f, 30f, 2f, 16f),
      CirclePart(68f, 29f, 2.2f),
      BoxPart(10f, 46f, 62f, 4f),
      Poly(70f, 46f, 74f, 46f, 78f, 56f, 70f, 56f),
      CirclePart(22f, 51f, 6f), CirclePart(38f, 51f, 6f), CirclePart(54f, 51f, 6f), CirclePart(66f, 53f, 3.5f),
      CirclePart(22f, 51f, 2f), CirclePart(38f, 51f, 2f), CirclePart(54f, 51f, 2f),
      LinePart(22f, 51f, 54f, 51f),
    ),
  ),
  ColoringPicture(
    "IC-trein",
    listOf(
      BoxPart(0f, 0f, 100f, 50f),
      BoxPart(0f, 50f, 100f, 20f),
      CirclePart(12f, 10f, 6f),
      BoxPart(86f, 30f, 4f, 20f), CirclePart(88f, 25f, 8f),
      BoxPart(0f, 53f, 100f, 2.5f),
      LinePart(0f, 7f, 100f, 7f),
      LinePart(40f, 18f, 46f, 7f), LinePart(52f, 18f, 46f, 7f),
      Poly(6f, 18f, 66f, 18f, 75f, 23f, 80f, 33f, 80f, 46f, 6f, 46f),
      Poly(66f, 20f, 74f, 25f, 77f, 32f, 66f, 32f),
      BoxPart(10f, 23f, 8f, 8f, 1f), BoxPart(21f, 23f, 8f, 8f, 1f), BoxPart(32f, 23f, 8f, 8f, 1f), BoxPart(55f, 23f, 8f, 8f, 1f),
      BoxPart(6f, 37f, 74f, 4f),
      BoxPart(43f, 21f, 9f, 23f, 1f),
      BoxPart(45f, 24f, 5f, 8f, 1f),
      Poly(70f, 37f, 80f, 37f, 80f, 46f, 66f, 46f),
      CirclePart(76f, 42f, 1.8f),
      CirclePart(14f, 48f, 4f), CirclePart(24f, 48f, 4f), CirclePart(58f, 48f, 4f), CirclePart(68f, 48f, 4f),
    ),
  ),
  ColoringPicture(
    "Kusttram",
    listOf(
      BoxPart(0f, 0f, 100f, 30f),
      BoxPart(0f, 30f, 100f, 12f),
      BoxPart(0f, 42f, 100f, 28f),
      CirclePart(85f, 9f, 6f),
      Poly(8f, 31f, 24f, 31f, 21f, 35f, 11f, 35f),
      Poly(16f, 30f, 16f, 19f, 23f, 30f),
      BoxPart(85f, 44f, 1f, 10f),
      dome(85.5f, 45f, 9f),
      LinePart(0f, 26f, 100f, 26f),
      LinePart(36f, 40f, 42f, 30f), LinePart(48f, 40f, 42f, 30f),
      BoxPart(10f, 40f, 62f, 18f, 4f),
      BoxPart(14f, 43f, 8f, 7f, 1f), BoxPart(25f, 43f, 8f, 7f, 1f), BoxPart(50f, 43f, 8f, 7f, 1f),
      Poly(61f, 43f, 68f, 43f, 71f, 50f, 61f, 50f),
      BoxPart(37f, 42f, 9f, 15f, 1f),
      BoxPart(10f, 52f, 27f, 3f), BoxPart(46f, 52f, 26f, 3f),
      CirclePart(20f, 59f, 3.5f), CirclePart(29f, 59f, 3.5f), CirclePart(53f, 59f, 3.5f), CirclePart(62f, 59f, 3.5f),
      BoxPart(0f, 62f, 100f, 2.5f),
      CirclePart(90f, 64f, 3f),
    ),
  ),
  ColoringPicture(
    "Station",
    listOf(
      BoxPart(0f, 0f, 100f, 50f),
      BoxPart(0f, 50f, 100f, 20f),
      CirclePart(90f, 9f, 5f),
      BoxPart(8f, 32f, 4f, 18f), CirclePart(10f, 25f, 8f),
      BoxPart(25f, 22f, 50f, 28f),
      Poly(20f, 22f, 50f, 5f, 80f, 22f),
      CirclePart(50f, 15f, 4f),
      LinePart(50f, 15f, 50f, 12.5f), LinePart(50f, 15f, 52f, 15f),
      BoxPart(30f, 28f, 9f, 10f, 1f), BoxPart(61f, 28f, 9f, 10f, 1f),
      BoxPart(44f, 33f, 12f, 17f, 5f),
      LinePart(83f, 36f, 83f, 50f), LinePart(93f, 36f, 93f, 50f),
      BoxPart(80f, 30f, 16f, 7f, 1f),
      BoxPart(0f, 53f, 100f, 5f),
      BoxPart(0f, 62f, 100f, 2.5f),
    ),
  ),
)
