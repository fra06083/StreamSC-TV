# StreamSC TV

App Android TV / Fire TV Stick per lo streaming italiano, con interfaccia **identica a CloudStream TV** e supporto ai provider `.scext` di StreamSC.

- Leggera: niente Compose, niente Chromecast, niente download manager.
- Avvio rapido e RAM contenuta.
- Profili separati a ogni avvio: cronologia, preferiti e “Continua a guardare” per ogni profilo.
- Provider `.scext` eseguiti tramite QuickJS, senza modificarli.
- Home, ricerca multi-provider, dettaglio con stagioni/episodi, player HLS, libreria, impostazioni.
- Sync estensioni da GitHub e installazione `.scext` da URL.

## Requisiti

- Fire TV Stick (Fire OS 7/8) o Android TV, Android 6.0+ (`minSdk 23`).
- Connessione Internet.
- Installazione tramite ADB o Downloader.

## Installazione

1. Abilita “App di origini sconosciute” / “Debug ADB” sul dispositivo.
2. Installa l’APK dalla release GitHub:
   `https://github.com/fra06083/StreamSC-TV/releases/latest`
3. Al primo avvio scegli o crea un profilo.
4. Apri **Estensioni** per verificare i provider abilitati o sincronizzarli da GitHub.

## Provider

I provider inclusi nell’APK sono quelli di `fra06083/StreamSC-Extensions`:

- StreamingCommunity
- AnimeUnity
- AnimeSaturn
- CalcioStreaming
- Altadefinizione, CB01, DaddyLive (disabilitati di default se `status != 1`)

Per installare un provider esterno: **Estensioni → Installa da URL** e incolla l’URL diretto di uno ZIP `.scext`.

## Build locale

```bash
export JAVA_HOME=/percorso/jdk-21
export ANDROID_HOME=/percorso/android-sdk
./gradlew :app:assembleRelease
```

APK generato in `app/build/outputs/apk/release/app-release.apk`.

La firma release usa `keystore/streamsctv.jks`. Per una distribuzione pubblica seria, sostituire il keystore e usare i GitHub Secrets.

## GitHub Actions

Il workflow `.github/workflows/build.yml` compila l’APK a ogni push su `main` e pubblica una Release quando viene creato un tag `v*`.

```bash
git tag v1.0.0
git push origin v1.0.0
```

## Struttura

- `app/src/main/java/it/fra06083/streamsctv/data` — modelli, profili, preferenze.
- `.../ext` — gestione `.scext`, runtime QuickJS, repository provider.
- `.../net` — HTTP e resolver VixSrc.
- `.../ui` — activity, fragment, adapter e player.
- `app/src/main/assets/extensions` — provider inclusi.
- `plan.md` — piano di lavoro e checklist.

## Licenza e crediti

Questo progetto è distribuito con licenza **GPL-3.0**.

La grafica TV, i layout, i colori, i font e parte delle risorse derivano da **CloudStream** (`recloudstream/cloudstream`), anch’esso GPL-3.0. Grazie agli autori originali.

I provider `.scext` appartengono ai rispettivi autori. StreamSC TV non ospita contenuti: esegue soltanto provider forniti dall’utente e mostra flussi pubblici dai siti di origine.
