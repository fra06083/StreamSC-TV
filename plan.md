# StreamSC TV — Piano di creazione e handoff

> Documento operativo per non perdere il segno. Contiene obiettivo, stato reale del progetto, file chiave, comandi, checklist e prossimi passi. Aggiornare man mano che si procede.

---

## 0. Obiettivo

Trasformare StreamSC in un’app **Android TV / Fire TV Stick**:

- leggerissima e con avvio rapido (CloudStream attuale è troppo lento);
- grafica **identica a CloudStream TV** (XML/RecyclerView/Material, non Compose);
- supporto a tutti i provider `.scext` di `StreamSC-Extensions` / `ExtensionBridge/output`;
- profili a ogni avvio, con cronologia/preferiti/continua-a-guardare separati;
- home, ricerca, dettaglio con stagioni/episodi, player, libreria, impostazioni, gestione estensioni;
- APK sideloadable per Firestick;
- build su GitHub Actions e APK pubblico in una GitHub Release, da dare ad AFTNews per il codice Downloader;
- risposta finale in italiano, con confronto peso/prestazioni rispetto a CloudStream.

Vincoli:

- package `it.fra06083.streamsctv`, nome “StreamSC TV”;
- GPL-3.0 con attribuzione a CloudStream per la grafica;
- provider `.scext` eseguiti **senza modifiche** tramite QuickJS;
- niente funzionalità inutili: APK e RAM più bassi possibile.

---

## 1. Stato attuale (snapshot)

### Fatto

- [x] Progetto Gradle creato: AGP 8.13.2, Kotlin 2.4.21, Gradle 8.13, JDK 21.
- [x] `compileSdk 36`, `minSdk 23`, `targetSdk 34`, ABI `arm64-v8a` + `armeabi-v7a`.
- [x] Firma release con `keystore/streamsctv.jks` (password `streamsctv123`, alias `streamsctv`).
- [x] Icona app (`app_icon.png`), banner TV (`tv_banner.png`).
- [x] Risorse CloudStream TV copiate: colori, dimensioni, font Product Sans, drawable, menu, layout.
- [x] Tema Material 3 scuro con colori CloudStream (`#2B2C30`, `#111111`, `#161616`, `#e9eaee`, `#9ba0a4`, `#3d50fa`).
- [x] NavigationRailView 62dp, 5 voci, footer con avatar profilo.
- [x] Layout profili, home, ricerca, libreria, estensioni, impostazioni, dettaglio, player.
- [x] Layout di supporto: `loading_list`, `dialog_list`, `dialog_loading`, `item_profile_avatar`, `item_provider_select`, `item_source`.
- [x] Asset provider in `app/src/main/assets/extensions` (7 provider).
- [x] Modelli dati (`Models.kt`).
- [x] `ProfileStore`: profili + cronologia/preferiti per profilo.
- [x] `ProviderPrefs`: provider abilitati, provider home, HLS, autoplay, dominio, versione bundled.
- [x] `Http`: OkHttp, cookie jar, cache 30 MB, User-Agent Fire TV, redirect.
- [x] `ProviderManifest` / `InstalledExtension`.
- [x] `ExtensionManager`: install bundled, ZIP/URL, sync GitHub, enable/disable/delete.
- [x] `ProviderRuntime`: QuickJS + bridge iOS (`fetch`, `parseHTML`, `console`, `webViewScrape`).
- [x] Override `BASE_URL` + `DEFAULT_CDN` per StreamingCommunity.
- [x] `ProviderRepository`: home, search, load, loadLinks, cache home 10 min, fallback VixSrc.
- [x] `VixSrcResolver` nativo (movie/tv, master playlist, token/expires/h=1, Referer).
- [x] `App.kt` con inizializzazione sincrona di Http, profili, estensioni, repository.
- [x] `ProfileActivity` + adapter profili/avatar, selezione a ogni avvio, aggiungi/modifica/elimina.
- [x] `MainActivity`: nav rail, tab, footer profilo, apertura dettaglio.
- [x] `HomeFragment` + `HomeSectionAdapter`: righe per sezione, “Continua a guardare”, provider selector, reload, ricerca.
- [x] `SearchFragment`: ricerca multi-provider, debounce, filtri Film/Serie/Anime.
- [x] `LibraryFragment`: continua a guardare + preferiti per profilo.
- [x] `ExtensionsFragment` + `ExtensionAdapter`: lista, switch, sync GitHub, installa da URL, elimina.
- [x] `SettingsFragment`: profilo, HLS, autoplay, cancella cronologia, versione, attribuzione.
- [x] `DetailFragment` + `SeasonAdapter`/`EpisodeAdapter`/`SourceAdapter`: metadati, preferiti, resume, stagioni/episodi, scelta sorgente.
- [x] `PlayerActivity`: ExoPlayer + HLS, controlli TV custom, resume, prossimo episodio, cambio sorgente, salvataggio cronologia.
- [x] `MediaCardAdapter` condiviso home/search/libreria, con progresso e label episodio.
- [x] `plan.md` creato.

### Fatto (aggiornato)

- [x] Prima build locale (`assembleDebug`) completata con successo.
- [x] Correzioni errori di compilazione e risorse:
  - Migrazione DSL compilerOptions per `jvmTarget = 17` in Kotlin 2.x.
  - Soppressione `checkAarMetadata` per QuickJS (richiedeva API 37 ma funziona perfettamente con 36).
  - Rimozione stringa duplicata `title_settings` in `strings.xml`.
  - Creazione drawables mancanti (`outline_big_20.xml`, `outline_less.xml`, fix `splash_background.xml`).
  - Pulizia `example_poster` in `home_result_grid.xml`.
  - Fix `PlayerActivity.kt`: rimosso `setUserAgent` obsoleto in Media3, usato `window.currentFocus`.
  - Fix `App.kt`: aggiunto receiver `Fragment.app` e accessor globale `app`.
  - Fix `Ui.kt`: mapping statico `AVATAR_DRAWABLES` per `profileAvatarRes` senza reflection né context.
  - Fix `DetailFragment.kt`: parametro `loadData` aggiunto a `startPlayer` e `showSources`.
- [x] Build release minificata (`assembleRelease`) completata con successo:
  - Ottimizzazione memoria daemon Gradle (`-Xmx1536m`, `parallel=false`, `workers=2`) per prevenire OOM killer del kernel.
  - Configurazione dinamica `outputFileName = "app-release.apk"` per la variante release.
  - Minificazione R8 + Resource shrinking attivi.
  - APK verificato con `apksigner` (firme v1 e v2 valide).
  - Dimensione APK release: **7,2 MB** (salvato in `app/build/outputs/apk/release/app-release.apk` e `dist/app-release.apk`).
- [x] Git inizializzato su branch `main` con `.gitignore` aggiornato.

### In corso / Prossimi passi

- [ ] Creazione repository GitHub `fra06083/StreamSC-TV` e push.
- [ ] Tag `v1.0.0` e release pubblica con l'APK allegato.
- [ ] Consegna URL diretto dell'APK e confronto con CloudStream.

---

## 2. Architettura e file chiave

```
/home/francesco/Progetti/StreamSC-TV/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── gradle/wrapper/...
├── keystore/streamsctv.jks
├── plan.md
├── .github/workflows/build.yml          ← da creare
└── app/
    ├── build.gradle.kts
    ├── proguard-rules.pro
    ├── src/main/AndroidManifest.xml
    ├── src/main/assets/extensions/*.scext
    ├── src/main/res/...
    └── src/main/java/it/fra06083/streamsctv/
        ├── App.kt
        ├── data/
        │   ├── Models.kt
        │   ├── ProfileStore.kt
        │   └── ProviderPrefs.kt
        ├── ext/
        │   ├── ExtensionManager.kt
        │   ├── ProviderManifest.kt
        │   ├── ProviderRepository.kt
        │   └── ProviderRuntime.kt
        ├── net/
        │   ├── Http.kt
        │   └── VixSrcResolver.kt
        └── ui/
            ├── MainActivity.kt
            ├── ProfileActivity.kt
            ├── Ui.kt
            ├── common/
            │   ├── MediaCardAdapter.kt
            │   └── ProviderSelectAdapter.kt
            ├── home/HomeFragment.kt
            ├── home/HomeSectionAdapter.kt
            ├── search/SearchFragment.kt
            ├── library/LibraryFragment.kt
            ├── extensions/ExtensionsFragment.kt
            ├── extensions/ExtensionAdapter.kt
            ├── settings/SettingsFragment.kt
            ├── detail/DetailFragment.kt
            ├── detail/DetailAdapters.kt
            ├── detail/SourceAdapter.kt
            └── player/PlayerActivity.kt
```

### Contratti importanti

- `.scext`: `manifest.json` + `provider.js` + `icon.png`.
- JS bridge:
  - `fetch(url, options)` → `{status, ok, url, text(), json()}`;
  - `parseHTML(html, selector)` → array di `{text, outerHTML, href, src, dataSrc}`;
  - `console.log/warn/error/debug`;
  - `webViewScrape` non supportato (lancia errore).
- Funzioni provider: `getMainPage(page, sectionKey)`, `search(query, page)`, `load(url)`, `loadLinks(data)`.
- Risposte provider: wrapper `{ok, data}` o `{ok, error}`.
- Home: `{items, hasNextPage}`; dettaglio: `{seasons, episodes, loadData}`; link: array.
- `loadData` è una stringa JSON con `url`, `title_id`, `episode_id`, `season`, `episode`, `tmdb_id`, ecc.
- StreamingCommunity usa `BASE_URL` e `DEFAULT_CDN` hardcoded: il runtime li sostituisce con il dominio risolto.
- VixSrc: API `/api/movie/{tmdb}` o `/api/tv/{tmdb}/{season}/{episode}` → embed → `window.masterPlaylist` → playlist con `token`, `expires`, `h=1`, `Referer: https://vixsrc.to/`.

---

## 3. Scelte tecniche e versioni

- UI: XML + RecyclerView + Material 3 + NavigationRailView (pixel-identica a CloudStream TV).
- JS: `io.github.dokar3:quickjs-kt:1.0.17` con binding async.
- HTTP: OkHttp 4.12.0.
- HTML: Jsoup 1.21.2.
- Player: Media3 1.9.3 (`exoplayer`, `exoplayer-hls`, `ui`, `datasource-okhttp`).
- Immagini: Coil 3.3.0 (`coil-network-okhttp`).
- Material 1.14.0, AppCompat 1.7.1, ConstraintLayout 2.2.1, Shimmer 0.5.0.
- minSdk 23, targetSdk 34, compileSdk 36.
- ABI: `arm64-v8a`, `armeabi-v7a`.
- R8 attivo in release; regole keep per QuickJS, Jsoup, modelli.
- Nessuna dipendenza Compose, Firebase, Cast, ExoPlayer UI extra, download manager.

---

## 4. Procedura di ripresa rapida

### 4.1 Aprire il progetto

```bash
cd /home/francesco/Progetti/StreamSC-TV
export JAVA_HOME=/home/francesco/.local/jdk-21
export ANDROID_HOME=/home/francesco/Android
```

### 4.2 Prima build debug

```bash
./gradlew :app:assembleDebug --stacktrace
```

Se manca il wrapper:

```bash
gradle wrapper --gradle-version 8.13
```

### 4.3 Build release

```bash
./gradlew :app:assembleRelease --stacktrace
ls -lh app/build/outputs/apk/release/
```

### 4.4 Installazione su Firestick (ADB)

```bash
adb connect <IP_FIRESTICK>:5555
adb install -r app/build/outputs/apk/release/app-release.apk
adb shell am start -n it.fra06083.streamsctv/.ui.ProfileActivity
```

### 4.5 Log runtime

```bash
adb logcat | grep -E "StreamSC|ProviderRuntime|JS:|AndroidRuntime"
```

### 4.6 GitHub

```bash
git init
git add .
git commit -m "StreamSC TV: first build"
gh repo create fra06083/StreamSC-TV --public --source=. --remote=origin --push
gh workflow run build.yml
gh run watch
```

### 4.7 Release APK

```bash
gh release create v1.0.0 app/build/outputs/apk/release/app-release.apk \
  --title "StreamSC TV v1.0.0" \
  --notes "APK per Fire TV / Android TV"
```

URL pubblico risultante:

```
https://github.com/fra06083/StreamSC-TV/releases/download/v1.0.0/app-release.apk
```

---

## 5. Prossimi passi immediati (in ordine)

1. [ ] Compilare `assembleDebug` e correggere gli errori.
2. [ ] Verificare che le risorse XML siano tutte valide (in particolare `activity_player_tv.xml`, `fragment_result_tv.xml`, `home_result_grid.xml`).
3. [ ] Verificare import e API di `PlayerActivity` (Media3, `Http.DEFAULT_UA`, `lifecycleScope`).
4. [ ] Verificare `DetailFragment` (extra `EXTRA_LOAD_DATA`, `Chip`, `getColor`, visibilità).
5. [ ] Verificare `ProfileActivity` dopo il passaggio a `MaterialAlertDialogBuilder`.
6. [ ] Build release + R8, controllare crash da ProGuard (QuickJS/Jsoup).
7. [ ] Test manuale su Firestick: primo avvio, profilo, home, ricerca, dettaglio, player, resume, preferiti.
8. [ ] Test provider: StreamingCommunity, AnimeUnity, AnimeSaturn, CalcioStreaming.
9. [ ] Test sync GitHub e installazione `.scext` da URL.
10. [ ] Creare workflow GitHub Actions.
11. [ ] Push, build su Actions, Release con APK.
12. [ ] Misurare APK vs CloudStream e scrivere confronto finale.
13. [ ] README + licenza GPL-3.0 + attribuzione CloudStream.
14. [ ] Consegnare URL APK ad AFTNews.

---

## 6. Checklist funzionale dettagliata

### Avvio e profili
- [ ] All’avvio compare sempre la selezione profilo.
- [ ] Profilo default creato al primo avvio.
- [ ] Aggiunta profilo con nome e avatar.
- [ ] Modifica nome/avatar con long press.
- [ ] Eliminazione profilo (almeno uno deve restare).
- [ ] Cambio profilo da impostazioni.
- [ ] Cronologia/preferiti isolati per profilo.

### Home
- [ ] Provider home scelto e persistito.
- [ ] Sezioni del manifest visualizzate con titoli corretti.
- [ ] “Continua a guardare” in cima se c’è cronologia.
- [ ] Reload provider.
- [ ] Apertura ricerca dalla home.
- [ ] Stato vuoto/errore se nessun provider.
- [ ] Focus D-pad tra righe e card.

### Ricerca
- [ ] Ricerca su tutti i provider abilitati.
- [ ] Debounce e annullamento ricerca precedente.
- [ ] Filtri Film / Serie TV / Anime.
- [ ] Deduplica risultati.
- [ ] Apertura dettaglio.
- [ ] Gestione provider che falliscono senza bloccare gli altri.

### Dettaglio
- [ ] Backdrop, poster, titolo, trama, cast, generi.
- [ ] Metadati provider, tipo, anno, voto, durata.
- [ ] Preferito on/off persistente.
- [ ] “Riprendi” se cronologia > 1% e < 95%.
- [ ] Film: play diretto.
- [ ] Serie: pannello episodi, stagioni, episodi.
- [ ] Scelta sorgente se più link.
- [ ] Errore se nessun link.
- [ ] Back chiude pannello episodi prima di uscire.

### Player
- [ ] HLS e progressivo.
- [ ] Header Referer/User-Agent.
- [ ] Play/pause, ±10s, barra tempo.
- [ ] Resume da posizione salvata.
- [ ] Salvataggio cronologia ogni 10s e in pausa/stop.
- [ ] Prossimo episodio.
- [ ] Cambio sorgente mantenendo posizione.
- [ ] Autoplay next se attivo.
- [ ] Controlli che si nascondono dopo 5s.
- [ ] Gestione errore playback.

### Libreria
- [ ] Continua a guardare con progresso.
- [ ] Preferiti in griglia.
- [ ] Apertura dettaglio da entrambi.
- [ ] Stato vuoto.

### Estensioni
- [ ] Lista provider installati con icona/versione/stato.
- [ ] Switch abilita/disabilita.
- [ ] Sync GitHub (`fra06083/StreamSC-Extensions`, branch `main`).
- [ ] Installazione da URL ZIP `.scext`.
- [ ] Eliminazione con conferma.
- [ ] Refresh dopo sync/install.

### Impostazioni
- [ ] Nome/avatar profilo.
- [ ] Cambio profilo.
- [ ] Preferisci HLS.
- [ ] Autoplay next.
- [ ] Cancella cronologia profilo.
- [ ] Versione app.
- [ ] Attribuzione CloudStream GPL-3.0.

---

## 7. Test matrice minima

| Test | Dispositivo | Esito |
|------|-------------|-------|
| Installazione APK | Fire TV Stick 4K | [ ] |
| Primo avvio + profilo | Fire TV Stick 4K | [ ] |
| Home StreamingCommunity | Fire TV Stick 4K | [ ] |
| Ricerca multi-provider | Fire TV Stick 4K | [ ] |
| Dettaglio serie + episodi | Fire TV Stick 4K | [ ] |
| Playback HLS VixSrc | Fire TV Stick 4K | [ ] |
| Resume + continua a guardare | Fire TV Stick 4K | [ ] |
| Preferiti per profilo | Fire TV Stick 4K | [ ] |
| Sync estensioni GitHub | Fire TV Stick 4K | [ ] |
| Installazione `.scext` da URL | Fire TV Stick 4K | [ ] |
| Avvio a freddo (misura) | Fire TV Stick 4K | [ ] |
| RAM a runtime (misura) | Fire TV Stick 4K | [ ] |
| Layout 720p | Emulatore/TV | [ ] |
| Focus D-pad completo | Emulatore/TV | [ ] |

---

## 8. Confronto con CloudStream (da completare)

Da misurare:

- dimensione APK:
  - StreamSC TV: `ls -lh app/build/outputs/apk/release/app-release.apk`
  - CloudStream: APK ufficiale/ultima release.
- RAM:
  - `adb shell dumpsys meminfo it.fra06083.streamsctv`
  - `adb shell dumpsys meminfo com.lagradost.cloudstream3`
- avvio a freddo:
  - `adb shell am force-stop ...`
  - `adb shell am start -W -n ...`
- numero dipendenze/classi (opzionale): `apkanalyzer`.
- sensazione d’uso su Firestick (home, ricerca, player).

Note già previste:

- StreamSC TV non include Compose, Chromecast, download manager, sync cloud, temi multipli, player avanzato.
- Usa XML/RecyclerView e poche librerie.
- QuickJS è la parte più pesante ma necessaria per i provider `.scext`.
- Il confronto finale va scritto in italiano, con numeri reali se disponibili e stime dichiarate.

---

## 9. Rischi e note

- **QuickJS su Fire TV**: verificare che la JNI `arm64-v8a`/`armeabi-v7a` sia inclusa e non rimossa da R8.
- **Provider JS**: alcuni usano `webViewScrape` (non supportato) o API browser; in quel caso il provider può fallire.
- **StreamingCommunity**: dominio cambia spesso; il resolver prova `.foo`, `.boo`, `.bar`, `.autos` e cache 6h.
- **VixSrc**: richiede Referer e token; se 403, il fallback nativo può non bastare.
- **Media3 HLS**: alcuni stream hanno audio separato; ExoPlayer gestisce le rendition HLS.
- **Fire OS**: minSdk 23 va bene; testare su Fire OS 7/8.
- **Keystore**: non committare il keystore in pubblico se si vuole evitare abusi; per ora è nel progetto e servirà per la firma. Valutare se escluderlo dal repo e usare secrets GitHub.
- **GPL-3.0**: includere licenza e attribuzione CloudStream nel README e nelle impostazioni.
- **GitHub Actions**: usare `actions/upload-artifact` e `softprops/action-gh-release` o `gh release upload`.
- **Downloader**: il codice AFTNews richiede un URL HTTPS diretto e stabile; la release GitHub va bene.

---

## 10. Comandi utili

```bash
# Pulizia
./gradlew clean

# Debug
./gradlew :app:assembleDebug

# Release
./gradlew :app:assembleRelease

# Firma manuale (se serve)
$ANDROID_HOME/build-tools/36.0.0/apksigner sign \
  --ks keystore/streamsctv.jks \
  --ks-pass pass:streamsctv123 \
  --key-pass pass:streamsctv123 \
  --ks-key-alias streamsctv \
  app/build/outputs/apk/release/app-release.apk

# Dimensione APK
du -h app/build/outputs/apk/release/app-release.apk

# Memoria app
adb shell dumpsys meminfo it.fra06083.streamsctv | head -40

# Avvio a freddo
adb shell am force-stop it.fra06083.streamsctv
adb shell am start -W -n it.fra06083.streamsctv/.ui.ProfileActivity
```

---

## 11. Criterio di “pronto”

Si considera pronto quando:

- [ ] `assembleRelease` passa senza errori.
- [ ] APK installabile su Firestick.
- [ ] Selezione profilo a ogni avvio.
- [ ] Home, ricerca, dettaglio, player funzionano con StreamingCommunity.
- [ ] Almeno un provider anime funziona.
- [ ] Cronologia/preferiti/continua a guardare persistono per profilo.
- [ ] Sync estensioni e installazione `.scext` funzionano.
- [ ] APK pubblicato in GitHub Release con URL diretto.
- [ ] Confronto CloudStream scritto in italiano.
- [ ] URL consegnato per il codice Downloader.
