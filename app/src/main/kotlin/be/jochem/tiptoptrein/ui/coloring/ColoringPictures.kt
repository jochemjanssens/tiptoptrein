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
  ColoringPicture(
    "TGV in de bergen",
    listOf(
      BoxPart(0f, 0f, 100f, 48f),
      CirclePart(86f, 10f, 6f),
      CirclePart(12f, 12f, 4f), CirclePart(18f, 10f, 5f), CirclePart(24f, 12f, 4f),
      Poly(18f, 48f, 50f, 9f, 82f, 48f),
      Poly(41f, 20f, 50f, 9f, 59f, 20f, 55f, 23f, 50f, 20f, 45f, 23f),
      BoxPart(0f, 48f, 100f, 22f),
      BoxPart(0f, 61f, 100f, 2.5f),
      Poly(4f, 40f, 58f, 40f, 70f, 42f, 84f, 49f, 92f, 55f, 92f, 58f, 4f, 58f),
      BoxPart(8f, 44f, 6f, 5f, 1f), BoxPart(17f, 44f, 6f, 5f, 1f), BoxPart(26f, 44f, 6f, 5f, 1f), BoxPart(35f, 44f, 6f, 5f, 1f),
      BoxPart(45f, 43f, 5f, 13f, 1f),
      Poly(60f, 42f, 70f, 43.5f, 77f, 48f, 62f, 48f),
      Poly(4f, 52f, 84f, 52f, 88f, 54.5f, 4f, 54.5f),
      CirclePart(89f, 56f, 1.2f),
      CirclePart(12f, 59f, 2.2f), CirclePart(20f, 59f, 2.2f), CirclePart(62f, 59f, 2.2f), CirclePart(70f, 59f, 2.2f),
    ),
  ),
  ColoringPicture(
    "Eurostar",
    listOf(
      BoxPart(0f, 0f, 100f, 56f),
      CirclePart(80f, 11f, 6f),
      CirclePart(50f, 12f, 4f), CirclePart(56f, 10f, 5f), CirclePart(62f, 12f, 4f),
      CirclePart(14f, 58f, 34f),
      BoxPart(0f, 56f, 100f, 14f),
      BoxPart(0f, 62f, 100f, 2.5f),
      BoxPart(2f, 36f, 22f, 20f),
      dome(13f, 36f, 11f),
      Poly(6f, 38f, 55f, 38f, 67f, 40f, 79f, 48f, 84f, 55f, 6f, 55f),
      BoxPart(10f, 42f, 6f, 5f, 1f), BoxPart(19f, 42f, 6f, 5f, 1f), BoxPart(28f, 42f, 6f, 5f, 1f), BoxPart(37f, 42f, 6f, 5f, 1f),
      Poly(57f, 40f, 67f, 41.5f, 74f, 46f, 59f, 46f),
      Poly(66f, 48f, 80f, 48f, 84f, 55f, 62f, 55f),
      BoxPart(6f, 50f, 56f, 2f),
      CirclePart(81f, 52f, 1.2f),
      CirclePart(14f, 57.5f, 2.2f), CirclePart(22f, 57.5f, 2.2f), CirclePart(64f, 57.5f, 2.2f), CirclePart(72f, 57.5f, 2.2f),
      CirclePart(90f, 63f, 2.5f), CirclePart(95f, 64f, 2f),
    ),
  ),
  ColoringPicture(
    "Zweeftrein",
    listOf(
      BoxPart(0f, 0f, 100f, 70f),
      CirclePart(12f, 10f, 6f),
      BoxPart(30f, 8f, 12f, 30f), BoxPart(33f, 12f, 3f, 4f), BoxPart(37f, 12f, 3f, 4f), BoxPart(33f, 20f, 3f, 4f), BoxPart(37f, 20f, 3f, 4f),
      BoxPart(46f, 16f, 10f, 22f), BoxPart(49f, 20f, 4f, 4f),
      Poly(62f, 38f, 62f, 14f, 68f, 6f, 74f, 14f, 74f, 38f),
      BoxPart(78f, 20f, 14f, 18f), BoxPart(81f, 24f, 3f, 3f), BoxPart(86f, 24f, 3f, 3f),
      BoxPart(0f, 38f, 100f, 3f),
      BoxPart(0f, 55f, 100f, 5f),
      BoxPart(14f, 60f, 6f, 10f), BoxPart(54f, 60f, 6f, 10f), BoxPart(88f, 60f, 6f, 10f),
      Poly(2f, 36f, 62f, 36f, 75f, 38f, 87f, 45f, 91f, 50f, 2f, 50f),
      BoxPart(7f, 40f, 6f, 5f, 1f), BoxPart(16f, 40f, 6f, 5f, 1f), BoxPart(25f, 40f, 6f, 5f, 1f), BoxPart(34f, 40f, 6f, 5f, 1f), BoxPart(43f, 40f, 6f, 5f, 1f),
      Poly(64f, 38f, 75f, 39.5f, 82f, 44f, 66f, 44f),
      Poly(2f, 46f, 88f, 46f, 89.5f, 47.5f, 2f, 47.5f),
      LinePart(6f, 52f, 86f, 52f), LinePart(6f, 53.5f, 86f, 53.5f),
      CirclePart(88f, 48.5f, 1f),
    ),
  ),
  ColoringPicture(
    "ICE op de brug",
    listOf(
      BoxPart(0f, 0f, 100f, 50f),
      CirclePart(88f, 10f, 6f),
      CirclePart(20f, 10f, 4f), CirclePart(26f, 8f, 5f), CirclePart(32f, 10f, 4f),
      BoxPart(0f, 50f, 100f, 20f),
      BoxPart(0f, 46f, 8f, 24f),
      BoxPart(92f, 46f, 8f, 24f),
      BoxPart(0f, 44f, 100f, 3f),
      BoxPart(26f, 47f, 5f, 23f), BoxPart(69f, 47f, 5f, 23f),
      Poly(40f, 63f, 54f, 63f, 51f, 67f, 43f, 67f),
      Poly(47f, 62f, 47f, 54f, 52f, 62f),
      Poly(14f, 60f, 18f, 58f, 20f, 60f, 18f, 62f),
      Poly(80f, 64f, 84f, 62f, 86f, 64f, 84f, 66f),
      Poly(4f, 28f, 62f, 28f, 75f, 30f, 87f, 37f, 91f, 42f, 91f, 43f, 4f, 43f),
      BoxPart(8f, 31f, 6f, 5f, 1f), BoxPart(17f, 31f, 6f, 5f, 1f), BoxPart(26f, 31f, 6f, 5f, 1f), BoxPart(35f, 31f, 6f, 5f, 1f), BoxPart(44f, 31f, 6f, 5f, 1f),
      Poly(64f, 30f, 75f, 31.5f, 82f, 36f, 66f, 36f),
      Poly(4f, 38f, 87f, 38f, 89f, 40f, 4f, 40f),
      CirclePart(89f, 41f, 1f),
      CirclePart(12f, 43.5f, 2f), CirclePart(20f, 43.5f, 2f), CirclePart(66f, 43.5f, 2f), CirclePart(74f, 43.5f, 2f),
    ),
  ),
)
