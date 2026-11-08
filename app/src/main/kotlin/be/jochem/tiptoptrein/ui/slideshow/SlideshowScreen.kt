package be.jochem.tiptoptrein.ui.slideshow

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import be.jochem.tiptoptrein.sound.TrainSounds
import be.jochem.tiptoptrein.trains.Photo
import be.jochem.tiptoptrein.trains.TrainKind
import be.jochem.tiptoptrein.trains.Video
import be.jochem.tiptoptrein.trains.video
import be.jochem.tiptoptrein.trains.watchableTrains
import be.jochem.tiptoptrein.ui.common.EmojiButton
import be.jochem.tiptoptrein.ui.common.GameFrame
import kotlin.math.abs

/** One page of the gallery: a photo or a film of a train. */
private sealed class Page(val kind: TrainKind) {
  class Picture(kind: TrainKind, val photo: Photo) : Page(kind)
  class Film(kind: TrainKind, val video: Video) : Page(kind)
}

// Every train's photos, with its film (if it has one) right after the first photo.
private val pages: List<Page> = watchableTrains.flatMap { kind ->
  buildList {
    add(Page.Picture(kind, kind.photos.first()))
    kind.video?.let { add(Page.Film(kind, it)) }
    kind.photos.drop(1).forEach { add(Page.Picture(kind, it)) }
  }
}

/** "Treinen kijken": a gallery of real photos and films; go back and forth with the arrows or a swipe. */
@Composable
fun SlideshowScreen(onHome: () -> Unit) {
  var index by remember { mutableIntStateOf(0) }
  // +1 when moving forward, -1 when moving back, so pages slide in from the right side.
  var direction by remember { mutableIntStateOf(1) }
  val page = pages[index]
  val kind = page.kind

  // Introduce each train once, when its first page comes up.
  LaunchedEffect(kind) {
    TrainSounds.play(kind.sound)
    TrainSounds.say("De ${kind.title}! ${kind.fact}")
  }

  fun go(delta: Int) {
    direction = delta
    index = (index + delta + pages.size) % pages.size
  }

  GameFrame(
    title = kind.title,
    onHome = onHome,
    actions = {
      EmojiButton("🔊", onClick = { TrainSounds.say("De ${kind.title}! ${kind.fact}") }, size = 56.dp)
    },
    background = {
      Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF81D4FA), Color(0xFF4FC3F7)))))
    },
  ) {
    BoxWithConstraints(Modifier.fillMaxSize().padding(start = 12.dp, end = 12.dp, top = 76.dp, bottom = 12.dp)) {
      val wide = maxWidth > 700.dp
      val swipe = with(LocalDensity.current) { 60.dp.toPx() }
      val shape = RoundedCornerShape(22.dp)
      Box(
        Modifier
          .fillMaxSize()
          .shadow(10.dp, shape)
          .clip(shape)
          .background(Color.Black)
          .pointerInput(Unit) {
            var dragged = 0f
            detectHorizontalDragGestures(
              onDragStart = { dragged = 0f },
              onDragEnd = { if (abs(dragged) > swipe) go(if (dragged < 0) 1 else -1) },
            ) { _, amount -> dragged += amount }
          },
      ) {
        AnimatedContent(
          targetState = index,
          transitionSpec = {
            slideInHorizontally { it * direction } togetherWith slideOutHorizontally { -it * direction }
          },
          label = "page",
        ) { shown ->
          when (val p = pages[shown]) {
            is Page.Picture -> Image(
              painterResource(p.photo.res),
              contentDescription = p.kind.title,
              contentScale = ContentScale.Crop,
              modifier = Modifier
                .fillMaxSize()
                .clickable(remember { MutableInteractionSource() }, indication = null) { go(1) },
            )
            is Page.Film -> VideoClip(p.video, Modifier.fillMaxSize())
          }
        }
        // The fact, readable on top of the photo; films keep the whole screen.
        if (page is Page.Picture) {
          Column(
            Modifier
              .align(Alignment.BottomCenter)
              .fillMaxWidth()
              .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))))
              .padding(start = 96.dp, end = 96.dp, top = 28.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Text(
              kind.fact,
              fontSize = if (wide) 26.sp else 18.sp,
              lineHeight = if (wide) 32.sp else 23.sp,
              fontWeight = FontWeight.SemiBold,
              color = Color.White,
              textAlign = TextAlign.Center,
              maxLines = 3,
              overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            PageDots(pages.filter { it.kind == kind }, page)
          }
        } else {
          Text(
            "🎬 Filmpje",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier
              .align(Alignment.TopStart)
              .padding(12.dp)
              .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
              .padding(horizontal = 14.dp, vertical = 4.dp),
          )
        }
      }
      EmojiButton("⬅️", onClick = { go(-1) }, size = 72.dp, modifier = Modifier.align(Alignment.CenterStart).padding(start = 10.dp))
      EmojiButton("➡️", onClick = { go(1) }, size = 72.dp, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 10.dp))
    }
  }
}

/** Little dots for this train's pages; a film shows as a slightly bigger, yellow dot. */
@Composable
private fun PageDots(trainPages: List<Page>, current: Page) {
  Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
    trainPages.forEach { p ->
      val film = p is Page.Film
      Box(
        Modifier
          .size(if (p === current) 14.dp else 10.dp)
          .background(
            when {
              p === current -> Color.White
              film -> Color(0xFFFFC107)
              else -> Color.White.copy(alpha = 0.45f)
            },
            CircleShape,
          ),
      )
    }
  }
}
