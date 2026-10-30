package be.jochem.tiptoptrein.ui.builder

import be.jochem.tiptoptrein.ui.common.GameMotion
import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import be.jochem.tiptoptrein.sound.Sfx
import be.jochem.tiptoptrein.sound.TrainSounds
import be.jochem.tiptoptrein.trains.CAR_GAP
import be.jochem.tiptoptrein.trains.CAR_LEN
import be.jochem.tiptoptrein.trains.TrainKind
import be.jochem.tiptoptrein.trains.drawRails
import be.jochem.tiptoptrein.trains.drawScenery
import be.jochem.tiptoptrein.trains.drawTrain
import be.jochem.tiptoptrein.trains.drawWagon
import be.jochem.tiptoptrein.trains.locoLength
import be.jochem.tiptoptrein.trains.trainLength
import be.jochem.tiptoptrein.ui.common.BouncyButton
import be.jochem.tiptoptrein.ui.common.GameFrame
import be.jochem.tiptoptrein.ui.common.rememberClock
import kotlinx.coroutines.launch
import kotlin.math.min

private const val MAX_WAGONS = 5

private val paints = listOf(
  Color(0xFFE53935), Color(0xFFFF9800), Color(0xFFFFEB3B), Color(0xFF8BC34A),
  Color(0xFF2E7D32), Color(0xFF4FC3F7), Color(0xFF1E40C8), Color(0xFF8E24AA),
  Color(0xFFF48FB1), Color(0xFF795548), Color(0xFF9E9E9E), Color(0xFF212121),
)

private enum class Tab(val emoji: String, val label: String) {
  LOCO("🚂", "Locomotief"),
  WAGONS("🚃", "Wagons"),
  PAINT("🎨", "Verf"),
}

private class Wagon(val kind: TrainKind, color: Color?) {
  var color by mutableStateOf(color)
}

@Composable
fun TrainBuilderScreen(onHome: () -> Unit) {
  val context = LocalContext.current
  val saved = remember { load(context) }
  var loco by remember { mutableStateOf(saved.first) }
  val wagons = remember { mutableStateListOf<Wagon>().apply { addAll(saved.second) } }
  var tab by remember { mutableStateOf(Tab.LOCO) }
  var brush by remember { mutableStateOf<Color?>(paints[0]) }
  // 0 = standing still in the middle. 0..1 drives off to the right, 1..2 comes back in from the left.
  val drive = remember { Animatable(0f) }
  val time by rememberClock()
  val scope = rememberCoroutineScope()

  LaunchedEffect(Unit) { TrainSounds.say("Bouw je eigen trein!") }
  // Remember the train for next time.
  LaunchedEffect(loco, wagons.size, wagons.map { it.color }) { save(context, loco, wagons) }

  fun go() {
    if (drive.value != 0f) return
    TrainSounds.play(loco.sound)
    TrainSounds.say("Daar gaat jouw trein!")
    scope.launch(GameMotion) {
      drive.animateTo(1f, tween(3500, easing = LinearEasing))
      drive.snapTo(1f)
      drive.animateTo(2f, tween(2500))
      drive.snapTo(0f)
    }
  }

  GameFrame(
    title = "Bouw je trein",
    onHome = onHome,
    background = { Box(Modifier.fillMaxSize().background(Color(0xFFE3F2FD))) },
  ) {
    val preview: @Composable (Modifier) -> Unit = { modifier ->
      // Preview: tap a wagon to paint it, tap the locomotive to hear it.
      Canvas(
        modifier
          .shadow(6.dp, RoundedCornerShape(24.dp))
          .background(Color.White, RoundedCornerShape(24.dp))
          .pointerInput(loco, tab, brush) {
            detectTapGestures { tap ->
              val g = previewGeometry(size.width.toFloat(), size.height.toFloat(), loco, wagons.size)
              val i = ((tap.x - g.left) / ((CAR_LEN + CAR_GAP) * g.u)).toInt()
              val onTrain = tap.x >= g.left && tap.y in (g.railY - 12f * g.u)..g.railY
              when {
                !onTrain -> {}
                i < wagons.size && tab == Tab.PAINT -> {
                  wagons[i].color = brush
                  TrainSounds.play(Sfx.POP)
                }
                i >= wagons.size -> TrainSounds.play(loco.sound)
              }
            }
          },
      ) {
        val g = previewGeometry(size.width, size.height, loco, wagons.size)
        val len = trainLength(loco, wagons.size) * g.u
        val p = drive.value
        val x = when {
          p <= 1f -> g.left + (size.width - g.left) * p
          else -> -len + (g.left + len) * (p - 1f)
        }
        drawScenery(loco.scenery, g.railY, g.u, scroll = if (p > 0f) (p + time * 0.2f) * size.width else 0f, time = time)
        drawRails(g.railY, g.u)
        drawTrain(
          loco, x, g.railY, g.u, cars = wagons.size, turn = x / g.u, time = time,
          carColors = wagons.map { it.color },
          carKinds = wagons.map { it.kind },
        )
      }
    }
    val pickTab: (Tab) -> Unit = { t ->
      tab = t
      TrainSounds.say(t.label)
    }
    val options: @Composable (Boolean) -> Unit = { small ->
      when (tab) {
        Tab.LOCO -> OptionRow(TrainKind.entries, small, selected = { it == loco }) { kind ->
          loco = kind
          TrainSounds.play(kind.sound)
          TrainSounds.say(kind.title)
        }
        Tab.WAGONS -> WagonRow(wagons, small)
        Tab.PAINT -> PaintRow(brush, small) { brush = it }
      }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
      if (maxHeight < 500.dp) {
        // Phone: round icon buttons in a 2x2 block beside the preview, smaller option cards.
        Column(Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, top = 72.dp, bottom = 10.dp)) {
          Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TabCircle(Tab.LOCO, tab, pickTab)
                TabCircle(Tab.WAGONS, tab, pickTab)
              }
              Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TabCircle(Tab.PAINT, tab, pickTab)
                BouncyButton(onClick = ::go, modifier = Modifier.size(60.dp), color = Color(0xFF43A047)) {
                  Text("▶️", fontSize = 28.sp)
                }
              }
            }
            preview(Modifier.weight(1f).fillMaxHeight())
          }
          Spacer(Modifier.height(10.dp))
          Box(Modifier.fillMaxWidth().height(100.dp)) { options(true) }
        }
      } else {
        Column(Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, top = 80.dp, bottom = 16.dp)) {
          preview(Modifier.fillMaxWidth().weight(1f))
          Spacer(Modifier.height(14.dp))
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Tab.entries.forEach { t ->
              BouncyButton(
                onClick = { pickTab(t) },
                modifier = Modifier.size(width = 190.dp, height = 64.dp),
                color = if (tab == t) Color(0xFF1E88E5) else Color.White,
                shape = RoundedCornerShape(32.dp),
              ) {
                Text("${t.emoji} ${t.label}", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = if (tab == t) Color.White else Color(0xFF263238))
              }
            }
            Spacer(Modifier.weight(1f))
            BouncyButton(onClick = ::go, modifier = Modifier.size(width = 200.dp, height = 72.dp), color = Color(0xFF43A047), shape = RoundedCornerShape(36.dp)) {
              Text("▶️ Rijden!", fontSize = 26.sp, fontWeight = FontWeight.Black, color = Color.White)
            }
          }
          Spacer(Modifier.height(12.dp))
          Box(Modifier.fillMaxWidth().height(130.dp)) { options(false) }
        }
      }
    }
  }
}

@Composable
private fun TabCircle(t: Tab, current: Tab, onPick: (Tab) -> Unit) {
  BouncyButton(
    onClick = { onPick(t) },
    modifier = Modifier.size(60.dp),
    color = if (t == current) Color(0xFF1E88E5) else Color.White,
  ) {
    Text(t.emoji, fontSize = 28.sp)
  }
}

private class Geometry(val u: Float, val railY: Float, val left: Float)

/** Where the standing train is drawn; always sized for a full train so it doesn't jump around. */
private fun previewGeometry(w: Float, h: Float, loco: TrainKind, wagons: Int): Geometry {
  val maxLen = trainLength(TrainKind.HIGH_SPEED, MAX_WAGONS)
  val u = min(h / 15f, w / (maxLen + 4f))
  val len = trainLength(loco, wagons) * u
  return Geometry(u, h * 0.82f, (w - len) / 2f)
}

@Composable
private fun OptionRow(kinds: List<TrainKind>, small: Boolean, selected: (TrainKind) -> Boolean, onPick: (TrainKind) -> Unit) {
  val cardW = if (small) 130.dp else 170.dp
  val cardH = if (small) 96.dp else 124.dp
  LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
    items(kinds) { kind ->
      BouncyButton(
        onClick = { onPick(kind) },
        modifier = Modifier.size(width = cardW, height = cardH)
          .then(if (selected(kind)) Modifier.border(5.dp, Color(0xFF1E88E5), RoundedCornerShape(20.dp)) else Modifier),
        color = Color.White,
        shape = RoundedCornerShape(20.dp),
        sound = null,
      ) {
        Column(Modifier.fillMaxSize().padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
          Canvas(Modifier.fillMaxWidth().weight(1f)) {
            val len = locoLength(kind)
            val u = min(size.height / 12.5f, size.width / (len + 2f))
            val railY = size.height * 0.9f
            drawRails(railY, u)
            drawTrain(kind, (size.width - len * u) / 2f, railY, u, cars = 0, time = 0.6f)
          }
          Text(kind.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF263238))
        }
      }
    }
  }
}

@Composable
private fun WagonRow(wagons: SnapshotStateList<Wagon>, small: Boolean) {
  val cardW = if (small) 130.dp else 170.dp
  val cardH = if (small) 96.dp else 124.dp
  LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
    item {
      BouncyButton(
        onClick = {
          if (wagons.isNotEmpty()) wagons.removeAt(wagons.lastIndex)
        },
        modifier = Modifier.size(width = cardH, height = cardH),
        color = Color(0xFFEF5350),
        shape = RoundedCornerShape(20.dp),
      ) {
        Text("➖", fontSize = 52.sp)
      }
    }
    items(TrainKind.entries) { kind ->
      BouncyButton(
        onClick = {
          if (wagons.size < MAX_WAGONS) {
            wagons.add(Wagon(kind, null))
          } else {
            TrainSounds.say("De trein is vol!")
          }
        },
        modifier = Modifier.size(width = cardW, height = cardH),
        color = Color.White,
        shape = RoundedCornerShape(20.dp),
      ) {
        Canvas(Modifier.fillMaxSize().padding(8.dp)) {
          val u = min(size.height / 11f, size.width / (CAR_LEN + 2f))
          val railY = size.height * 0.92f
          drawRails(railY, u)
          drawWagon(kind, (size.width - CAR_LEN * u) / 2f, railY, u)
        }
      }
    }
  }
}

@Composable
private fun PaintRow(brush: Color?, small: Boolean, onPick: (Color?) -> Unit) {
  LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
    items(paints + listOf(null)) { c ->
      val selected = c == brush
      Box(
        Modifier
          .size(if (small) 68.dp else 84.dp)
          .scale(if (selected) 1.12f else 1f)
          .shadow(if (selected) 10.dp else 3.dp, CircleShape)
          .background(c ?: Color.White, CircleShape)
          .border(if (selected) 5.dp else 2.dp, if (selected) Color(0xFF263238) else Color(0xFFBDBDBD), CircleShape)
          .pointerInput(c) {
            detectTapGestures {
              TrainSounds.play(Sfx.POP)
              onPick(c)
              TrainSounds.say("Tik op een wagon.")
            }
          },
        contentAlignment = Alignment.Center,
      ) {
        if (c == null) Text("🧽", fontSize = 38.sp)
      }
    }
    item { Spacer(Modifier.width(8.dp)) }
  }
}

// ---- saving -------------------------------------------------------------------------------

private const val PREFS = "train_builder"

private fun load(context: Context): Pair<TrainKind, List<Wagon>> {
  val text = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("train", null)
    ?: return TrainKind.HIGH_SPEED to listOf(Wagon(TrainKind.HIGH_SPEED, null), Wagon(TrainKind.HIGH_SPEED, null))
  val parts = text.split("|")
  val loco = TrainKind.entries.firstOrNull { it.name == parts[0] } ?: TrainKind.HIGH_SPEED
  val wagons = parts.drop(1).mapNotNull { w ->
    val (k, c) = w.split(":").let { it[0] to it.getOrNull(1).orEmpty() }
    val kind = TrainKind.entries.firstOrNull { it.name == k } ?: return@mapNotNull null
    Wagon(kind, c.toLongOrNull()?.let { Color(it.toInt()) })
  }
  return loco to wagons.take(MAX_WAGONS)
}

private fun save(context: Context, loco: TrainKind, wagons: List<Wagon>) {
  val text = (listOf(loco.name) + wagons.map { "${it.kind.name}:${it.color?.toArgb()?.toLong()?.and(0xFFFFFFFFL) ?: ""}" })
    .joinToString("|")
  context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("train", text).apply()
}
