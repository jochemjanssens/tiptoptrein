# Naar de Play Store

De GitHub Actions-workflow (`.github/workflows/release.yml`) bouwt bij elke push naar `main` een ondertekende app-bundle (`.aab`). Zodra de geheimen hieronder ingesteld zijn, uploadt hij die ook naar Google Play.

## Eenmalig: GitHub-geheimen instellen

De upload-sleutel staat in `signing/` (staat niet in git, **maak er een back-up van**). Zet de geheimen met:

```bash
gh secret set UPLOAD_KEYSTORE_BASE64 < <(base64 -i signing/upload.jks)
gh secret set UPLOAD_KEYSTORE_PASSWORD   # plak de waarde uit signing/upload-key.properties
gh secret set UPLOAD_KEY_PASSWORD        # idem
gh secret set UPLOAD_KEY_ALIAS --body upload
gh secret set PLAY_SERVICE_ACCOUNT_JSON < pad/naar/service-account.json
```

## Eenmalig: Play Console

1. **App aanmaken** in de [Play Console](https://play.google.com/console): naam *TipTop Trein*, taal Nederlands (België), app, gratis.
2. **Eerste bundle met de hand uploaden**: download de `.aab` uit de GitHub Actions-run (artifact `tiptoptrein-N`) en upload hem bij *Testen → Interne test*. Kies *Play App Signing* (Google beheert de app-sleutel, wij houden alleen de upload-sleutel).
3. **Service-account** voor automatische uploads: in Google Cloud een service-account met JSON-sleutel maken, en die in de Play Console bij *Gebruikers en rechten* uitnodigen met de rechten om releases te beheren. Zet de JSON als geheim `PLAY_SERVICE_ACCOUNT_JSON`.
4. **App-inhoud invullen** (verplicht):
   - Privacybeleid: URL naar `docs/privacy.md` (bijvoorbeeld via GitHub Pages).
   - Doelgroep: **jonger dan 13** → de app valt onder het [Families-beleid](https://support.google.com/googleplay/android-developer/answer/9893335). Geen reclame, geen gegevensverzameling: dat klopt met deze app.
   - Gegevensveiligheid: "verzamelt of deelt geen gegevens".
   - Inhoudsclassificatie-vragenlijst, advertenties: nee.
5. **Store-vermelding**: teksten hieronder, icoon 512×512, feature graphic 1024×500 en schermafbeeldingen (telefoon én 7"/10" tablet).

Daarna: elke push naar `main` → nieuwe versie als concept op de interne test. Via *Actions → Build and release → Run workflow* kies je een andere track (beta, productie) en status `completed`.

## Store-teksten

**Korte beschrijving** (max. 80 tekens):
> Treinen kijken, rijden, tellen en kleuren. Voor kleine treinfans!

**Volledige beschrijving:**
> TipTop Trein is dé app voor kleine treinfans vanaf 4 jaar.
>
> 🎞️ Treinen kijken – echte foto's van Belgische treinen, de Eurostar en de kusttram, met een stem die alles voorleest.
> 🚂 Machinist – rij zelf de trein en stop netjes aan het perron.
> 🛤️ Sporen leggen – draai de sporen en maak een weg voor de trein.
> 🎨 Kleuren – kleur een stoomtrein, een tram of een station.
> 🔢 Tellen – hoeveel wagons heeft de trein?
> 🔍 Zoeken – waar is de metro?
> 🃏 Memory – zoek de twee dezelfde treinen.
>
> ✔ Geen reclame ✔ Geen aankopen ✔ Werkt zonder internet ✔ Verzamelt geen gegevens
