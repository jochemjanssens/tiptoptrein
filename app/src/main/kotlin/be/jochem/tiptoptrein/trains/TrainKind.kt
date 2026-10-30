package be.jochem.tiptoptrein.trains

import androidx.annotation.DrawableRes
import be.jochem.tiptoptrein.R
import be.jochem.tiptoptrein.sound.Sfx

enum class Scenery { COUNTRY, CITY, TUNNEL, PORT, MOUNTAIN, BEACH }

/** A real photo from Wikimedia Commons, with the credit its license asks for. */
class Photo(@param:DrawableRes val res: Int, val credit: String)

/**
 * Belgian trains. The [fact] is read aloud to a six-year-old, so it uses short sentences and
 * simple words.
 */
enum class TrainKind(
  val title: String,
  val plural: String,
  val fact: String,
  val sound: Sfx,
  val scenery: Scenery,
  val cars: Int,
  val photo: Photo,
) {
  STEAM(
    "Stoomtrein", "stoomtreinen",
    "Dit is een stoomtrein. Hij is heel oud. In de trein brandt een vuur. Dat maakt stoom. Tsjoek tsjoek!",
    Sfx.WHISTLE, Scenery.MOUNTAIN, 2,
    Photo(R.drawable.photo_steam, "Foto: Trougnouf (Benoit Brummer), CC BY-SA 4.0"),
  ),
  INTERCITY(
    "IC-trein", "IC-treinen",
    "Dit is de IC-trein. Hij rijdt van stad naar stad. Hij stopt niet overal. Zo gaat hij snel!",
    Sfx.HORN, Scenery.COUNTRY, 2,
    Photo(R.drawable.photo_intercity, "Foto: Stratoswift, CC BY-SA 4.0"),
  ),
  SPRINTER(
    "Desiro", "desiro's",
    "Dit is de Desiro. Hij stopt bij veel kleine stations. Deuren open. Instappen maar!",
    Sfx.HORN, Scenery.COUNTRY, 2,
    Photo(R.drawable.photo_desiro, "Foto: Mini1300, CC0"),
  ),
  DOUBLE_DECKER(
    "Dubbeldekker", "dubbeldekkers",
    "Deze trein heeft een boven en een beneden. Net als een huis met een trap! Wil jij boven zitten?",
    Sfx.HORN, Scenery.COUNTRY, 2,
    Photo(R.drawable.photo_m7, "Foto: Traveller-chewy93, CC0"),
  ),
  HIGH_SPEED(
    "Eurostar", "eurostars",
    "De Eurostar is super snel. Zoef! Hij rijdt zelfs onder de zee door.",
    Sfx.HORN, Scenery.COUNTRY, 2,
    Photo(R.drawable.photo_eurostar, "Foto: Smiley.toerist, CC BY-SA 4.0"),
  ),
  ICE(
    "ICE", "ICE's",
    "De ICE komt uit Duitsland. Hij is wit met een rode streep. Hij rijdt van Brussel naar Duitsland. Zoef!",
    Sfx.HORN, Scenery.COUNTRY, 2,
    Photo(R.drawable.photo_ice, "Foto: Joshua Brown, CC BY-SA 2.0"),
  ),
  TGV(
    "TGV", "TGV's",
    "De TGV komt uit Frankrijk. Een TGV reed ooit bijna zeshonderd kilometer per uur. Dat is een wereldrecord!",
    Sfx.HORN, Scenery.COUNTRY, 2,
    Photo(R.drawable.photo_tgv, "Foto: Florian Pépellin, CC BY-SA 4.0"),
  ),
  MAGLEV(
    "Zweeftrein", "zweeftreinen",
    "De zweeftrein heeft geen wielen! Magneten laten hem zweven. Hij rijdt in China. Heel, heel snel!",
    Sfx.HORN, Scenery.CITY, 2,
    Photo(R.drawable.photo_maglev, "Foto: Alex Needham, publiek domein"),
  ),
  FREIGHT(
    "Goederentrein", "goederentreinen",
    "Deze trein neemt geen mensen mee. Hij neemt grote dozen mee. Die heten containers.",
    Sfx.HORN, Scenery.PORT, 3,
    Photo(R.drawable.photo_freight, "Foto: Smiley.toerist, CC BY-SA 4.0"),
  ),
  METRO(
    "Metro", "metro's",
    "De metro rijdt onder de grond. In een lange tunnel. Zie je de lampjes?",
    Sfx.BELL, Scenery.TUNNEL, 2,
    Photo(R.drawable.photo_metro, "Foto: Tohaomg, CC0"),
  ),
  TRAM(
    "Kusttram", "kusttrams",
    "De kusttram rijdt langs de zee. Op naar het strand! Ding ding!",
    Sfx.BELL, Scenery.BEACH, 2,
    Photo(R.drawable.photo_tram, "Foto: Snoopy 31, CC BY-SA 4.0"),
  ),
}

/** Trains that go faster than 250 km/h. */
val TrainKind.isHighSpeed: Boolean
  get() = this == TrainKind.HIGH_SPEED || this == TrainKind.ICE || this == TrainKind.TGV || this == TrainKind.MAGLEV
