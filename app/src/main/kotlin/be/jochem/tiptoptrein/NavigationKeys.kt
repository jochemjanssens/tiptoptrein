package be.jochem.tiptoptrein

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Home : NavKey

@Serializable data object Slideshow : NavKey

@Serializable data object Drive : NavKey

@Serializable data object CountWagons : NavKey

@Serializable data object FindTrain : NavKey

@Serializable data object Memory : NavKey

@Serializable data object Coloring : NavKey

@Serializable data object TrackPuzzle : NavKey

@Serializable data object Race : NavKey
