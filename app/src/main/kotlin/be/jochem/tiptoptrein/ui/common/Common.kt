package be.jochem.tiptoptrein.ui.common

import androidx.compose.ui.MotionDurationScale
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import be.jochem.tiptoptrein.sound.Sfx
import be.jochem.tiptoptrein.sound.TrainSounds

/** Seconds since this composable entered the composition, updated every frame. */
@Composable
fun rememberClock(): State<Float> {
  val time = remember { mutableFloatStateOf(0f) }
  LaunchedEffect(Unit) {
    val start = withFrameNanos { it }
    while (true) withFrameNanos { time.floatValue = (it - start) / 1_000_000_000f }
  }
  return time
}

/** A large, bouncy, child-friendly button. */
@Composable
fun BouncyButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  color: Color = Color(0xFFFFC107),
  shape: Shape = CircleShape,
  enabled: Boolean = true,
  sound: Sfx? = Sfx.POP,
  content: @Composable BoxScope.() -> Unit,
) {
  val interaction = remember { MutableInteractionSource() }
  val pressed by interaction.collectIsPressedAsState()
  val scale by animateFloatAsState(if (pressed) 0.88f else 1f, spring(dampingRatio = 0.4f), label = "press")
  Box(
    modifier = modifier
      .scale(scale)
      .shadow(6.dp, shape)
      .background(if (enabled) color else color.copy(alpha = 0.35f), shape)
      .border(3.dp, Color.White.copy(alpha = 0.7f), shape)
      .clickable(interactionSource = interaction, indication = null, enabled = enabled) {
        sound?.let { TrainSounds.play(it) }
        onClick()
      },
    contentAlignment = Alignment.Center,
    content = content,
  )
}

@Composable
fun EmojiButton(
  emoji: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  size: Dp = 64.dp,
  color: Color = Color.White,
  sound: Sfx? = Sfx.POP,
) {
  BouncyButton(onClick = onClick, modifier = modifier.size(size), color = color, sound = sound) {
    Text(emoji, fontSize = (size.value * 0.48f).sp)
  }
}

/** Screen frame with a home button, a title and optional actions on the right. */
@Composable
fun GameFrame(
  title: String,
  onHome: () -> Unit,
  modifier: Modifier = Modifier,
  actions: @Composable () -> Unit = {},
  background: @Composable BoxScope.() -> Unit = {},
  content: @Composable BoxScope.() -> Unit,
) {
  Box(modifier.fillMaxSize()) {
    background()
    // The game uses the full width: a camera hole at the side only hides a bit of scenery, while
    // padding for it would push everything off-centre. Only the header avoids it.
    Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical))) {
      content()
      Row(
        Modifier.fillMaxWidth().safeDrawingPadding().padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        EmojiButton("🏠", onClick = {
          TrainSounds.stopSpeaking()
          onHome()
        }, size = 56.dp)
        Spacer(Modifier.width(14.dp))
        OutlinedTitle(title, Modifier.weight(1f))
        actions()
      }
    }
  }
}

@Composable
fun OutlinedTitle(text: String, modifier: Modifier = Modifier, fontSize: Int = 30, color: Color = Color.White) {
  Box(modifier) {
    val style = TextStyle(fontSize = fontSize.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Start)
    Text(
      text,
      style = style.copy(
        color = Color(0xFF263238),
        drawStyle = androidx.compose.ui.graphics.drawscope.Stroke(width = 10f),
      ),
    )
    Text(text, style = style.copy(color = color))
  }
}

@Composable
fun StarCounter(count: Int, modifier: Modifier = Modifier) {
  Box(
    modifier
      .background(Color.White.copy(alpha = 0.85f), CircleShape)
      .padding(horizontal = 16.dp, vertical = 6.dp),
  ) {
    Text("⭐ $count", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color(0xFF263238))
  }
}

/**
 * Game motion (a train driving in, the train on the finished track) is the point of the game, so
 * it runs at normal speed even when the system's animation scale is turned down or off.
 */
val GameMotion: MotionDurationScale = object : MotionDurationScale {
  override val scaleFactor = 1f
}
