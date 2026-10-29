package be.jochem.tiptoptrein

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import be.jochem.tiptoptrein.ui.coloring.ColoringScreen
import be.jochem.tiptoptrein.ui.count.CountWagonsScreen
import be.jochem.tiptoptrein.ui.drive.DriveScreen
import be.jochem.tiptoptrein.ui.find.FindTrainScreen
import be.jochem.tiptoptrein.ui.home.HomeScreen
import be.jochem.tiptoptrein.ui.memory.MemoryScreen
import be.jochem.tiptoptrein.ui.race.RaceScreen
import be.jochem.tiptoptrein.ui.slideshow.SlideshowScreen
import be.jochem.tiptoptrein.ui.tracks.TrackPuzzleScreen

@Composable
fun MainNavigation() {
  val backStack = rememberNavBackStack(Home)
  val goHome: () -> Unit = { backStack.removeLastOrNull() }

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<Home> { HomeScreen(onOpen = { key -> backStack.add(key) }) }
        entry<Slideshow> { SlideshowScreen(onHome = goHome) }
        entry<Drive> { DriveScreen(onHome = goHome) }
        entry<CountWagons> { CountWagonsScreen(onHome = goHome) }
        entry<FindTrain> { FindTrainScreen(onHome = goHome) }
        entry<Memory> { MemoryScreen(onHome = goHome) }
        entry<Coloring> { ColoringScreen(onHome = goHome) }
        entry<TrackPuzzle> { TrackPuzzleScreen(onHome = goHome) }
        entry<Race> { RaceScreen(onHome = goHome) }
      },
  )
}
