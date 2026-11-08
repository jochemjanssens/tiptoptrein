package be.jochem.tiptoptrein.ui.memory

import be.jochem.tiptoptrein.ui.common.EmojiButton
import be.jochem.tiptoptrein.trains.watchableTrains
import be.jochem.tiptoptrein.trains.Photo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.Image
import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import be.jochem.tiptoptrein.sound.Sfx
import be.jochem.tiptoptrein.sound.TrainSounds
import be.jochem.tiptoptrein.trains.TrainKind
import be.jochem.tiptoptrein.trains.drawRails
import be.jochem.tiptoptrein.trains.drawTrain
import be.jochem.tiptoptrein.trains.locoLength
import be.jochem.tiptoptrein.ui.common.BouncyButton
import be.jochem.tiptoptrein.ui.common.GameFrame
import be.jochem.tiptoptrein.ui.common.OutlinedTitle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.min

// Trains that look clearly different, so pairs are easy to recognise. Each game uses six of them.
private val pairPool = listOf(
  TrainKind.STEAM, TrainKind.INTERCITY, TrainKind.HIGH_SPEED, TrainKind.ICE, TrainKind.TGV,
  TrainKind.MAGLEV, TrainKind.FREIGHT, TrainKind.METRO, TrainKind.TRAM,
)

/** A card shows either a drawn train or, when [photo] is set, a real photo of it. */
private class Card(val kind: TrainKind, val photo: Photo?) {
  var faceUp by mutableStateOf(false)
  var matched by mutableStateOf(false)
}

/** Six pairs. With photos, both cards of a pair show the same real photo. */
private fun newDeck(photos: Boolean): List<Card> {
  val kinds = (if (photos) watchableTrains else pairPool).shuffled().take(6)
  val faces = kinds.map { it to if (photos) it.photos.random() else null }
  return (faces + faces).shuffled().map { (kind, photo) -> Card(kind, photo) }
}

private const val PREFS = "memory"

@Composable
fun MemoryScreen(onHome: () -> Unit) {
  val prefs = LocalContext.current.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
  var photos by remember { mutableStateOf(prefs.getBoolean("photos", false)) }
  var deck by remember { mutableStateOf(newDeck(photos)) }
  val open = remember { mutableStateListOf<Card>() }
  var busy by remember { mutableStateOf(false) }
  var turns by remember { mutableIntStateOf(0) }
  val scope = rememberCoroutineScope()
  val won = deck.all { it.matched }

  fun tap(card: Card) {
    if (busy || card.faceUp || card.matched) return
    card.faceUp = true
    TrainSounds.play(Sfx.POP)
    open.add(card)
    if (open.size < 2) return
    turns++
    val (a, b) = open.toList()
    busy = true
    scope.launch {
      delay(700)
      if (a.kind == b.kind) {
        a.matched = true
        b.matched = true
        TrainSounds.play(Sfx.CORRECT)
        val plural = a.kind.plural
        if (deck.all { it.matched }) {
          TrainSounds.say("Twee $plural! Hoera! Alles gevonden!")
          delay(600)
          TrainSounds.play(Sfx.WHISTLE)
        } else {
          TrainSounds.say("Twee $plural!")
        }
      } else {
        delay(600)
        a.faceUp = false
        b.faceUp = false
      }
      open.clear()
      busy = false
    }
  }

  GameFrame(
    title = "Treinen-memory",
    onHome = onHome,
    actions = {
      // Choose drawn trains or real photos; switching starts a new game.
      for ((usePhotos, emoji) in listOf(false to "✏️", true to "📷")) {
        EmojiButton(emoji, onClick = {
          if (photos != usePhotos) {
            photos = usePhotos
            prefs.edit().putBoolean("photos", usePhotos).apply()
            deck = newDeck(usePhotos)
            open.clear()
            turns = 0
            TrainSounds.say(if (usePhotos) "Memory met echte treinen!" else "Memory met tekeningen!")
          }
        }, size = 56.dp, color = if (photos == usePhotos) Color(0xFFFFC107) else Color.White)
        Spacer(Modifier.width(10.dp))
      }
    },
    background = {
      Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFFFCC80), Color(0xFFFF8A65)))))
    },
  ) {
    BoxWithConstraints(Modifier.fillMaxSize().padding(start = 20.dp, end = 20.dp, top = 84.dp, bottom = 16.dp)) {
    val columns = if (maxWidth > maxHeight) 6 else 3
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
      deck.chunked(columns).forEach { row ->
        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
          row.forEach { card -> MemoryCard(card, Modifier.weight(1f).fillMaxSize()) { tap(card) } }
        }
      }
    }
    }
    if (won) {
      Column(
        Modifier.align(Alignment.Center)
          .shadow(12.dp, RoundedCornerShape(30.dp))
          .background(Color.White, RoundedCornerShape(30.dp))
          .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        OutlinedTitle("🎉 Hoera! 🎉", fontSize = 40, color = Color(0xFFFFC107))
        Text("Alles gevonden!", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF263238))
        BouncyButton(
          onClick = {
            deck = newDeck(photos)
            turns = 0
          },
          modifier = Modifier.padding(top = 16.dp).size(width = 240.dp, height = 72.dp),
          color = Color(0xFF43A047),
          shape = RoundedCornerShape(36.dp),
        ) {
          Text("🔁 Nog een keer", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White)
        }
      }
    }
  }
}

@Composable
private fun MemoryCard(card: Card, modifier: Modifier, onTap: () -> Unit) {
  val rotation by animateFloatAsState(if (card.faceUp || card.matched) 180f else 0f, tween(400), label = "flip")
  val shape = RoundedCornerShape(18.dp)
  Box(
    modifier
      .graphicsLayer {
        rotationY = rotation
        cameraDistance = 14f * density
      }
      .shadow(6.dp, shape)
      .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onTap),
  ) {
    if (rotation <= 90f) {
      Box(
        Modifier.fillMaxSize()
          .background(Brush.linearGradient(listOf(Color(0xFF1E88E5), Color(0xFF3949AB))), shape)
          .border(4.dp, Color.White, shape),
        contentAlignment = Alignment.Center,
      ) {
        Text("🚂", fontSize = 64.sp)
      }
    } else {
      Box(
        Modifier.fillMaxSize()
          // mirror back so the face isn't drawn backwards
          .graphicsLayer { rotationY = 180f }
          .background(if (card.matched) Color(0xFFC8E6C9) else Color(0xFFE1F5FE), shape)
          .border(4.dp, if (card.matched) Color(0xFF66BB6A) else Color.White, shape),
      ) {
        if (card.photo != null) {
          Image(
            painterResource(card.photo.res),
            contentDescription = card.kind.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().padding(4.dp).clip(RoundedCornerShape(14.dp)),
          )
        } else Canvas(Modifier.fillMaxSize().padding(6.dp)) {
          val len = locoLength(card.kind)
          val u = min(size.height / 14f, size.width / (len + 3f))
          val railY = size.height * 0.8f
          drawRails(railY, u)
          drawTrain(card.kind, (size.width - len * u) / 2f, railY, u, cars = 0, time = 0.6f)
        }
      }
    }
  }
}
