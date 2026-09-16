# Kids Tracker

App Android per seguire, giorno per giorno, come vanno a scuola due bambini piccoli:
le faccine che manda la scuola (nanna, primo, secondo, dolce), com'è andata l'entrata
e l'uscita, e se stavano bene o no. Poi mostra come cambiano le cose nel tempo.

I dati restano sul telefono. L'app non chiede il permesso di internet.

## Cosa c'è dentro

| Schermata | A cosa serve |
|---|---|
| **Oggi** | Si attaccano le faccine, una per una. Ogni tocco salva subito. |
| **Calendario** | Il mese come un foglio di adesivi: colore, faccina e numero per ogni giornata. |
| **Andamento** | Media mobile a 7 giorni dei bambini sullo stesso asse, mini-serie per categoria, confronto fra i giorni della settimana. |
| **Scoperte** | Le frasi già scritte: quanto pesa il malessere sulla pappa, il giorno peggiore per l'entrata, la striscia senza rossi, dove i gemelli divergono. |
| **Impostazioni** | Nomi, promemoria giornaliero, export JSON/CSV, import, cancellazione. |

## Come si misura una giornata

Ogni faccina vale dei punti: **sì = 2**, **così così = 1**, **no = 0**.

- **Indice pappa** = media di primo, secondo e dolce, in percentuale.
- **Indice giornata** = media di tutte e sei le categorie (nanna, primo, secondo, dolce,
  entrata, uscita), pesate uguale.

Le categorie non segnate non contano come zero: vengono escluse dalla media. Un giorno
di assenza non ha indice e non spezza le strisce, ma nemmeno le allunga.

Il giudizio di una giornata segue l'indice: **buona** da 67 in su, **così così** da 34 a 66,
**difficile** sotto 34.

## Compilare

Serve JDK 17 e l'SDK Android (compileSdk 35). Poi:

```bash
./gradlew testDebugUnitTest   # la matematica degli indici e il backup
./gradlew assembleDebug       # APK in app/build/outputs/apk/debug/
```

## Installare sul telefono

Ogni push fa girare [il workflow Android](.github/workflows/android.yml), che allega
l'APK di debug agli artifact della run. Si scarica da lì e si installa (serve abilitare
l'installazione da origini sconosciute).

Per una versione stabile, basta un tag:

```bash
git tag v0.1.0 && git push origin v0.1.0
```

Il [workflow Release](.github/workflows/release.yml) allega `kids-tracker.apk` alla release
di GitHub.

### Firmare la release (facoltativo ma consigliato)

Senza firma, ogni build ha una firma diversa e per aggiornare l'app bisogna disinstallarla.
Per evitarlo, si genera una chiave una volta sola:

```bash
keytool -genkey -v -keystore kids.jks -keyalg RSA -keysize 2048 -validity 10000 -alias kids
base64 -w0 kids.jks   # il risultato va nel secret KEYSTORE_BASE64
```

Poi si aggiungono quattro secret al repository: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`,
`KEY_ALIAS`, `KEY_PASSWORD`. Il workflow li usa da solo; se non ci sono, ripiega sull'APK
di debug. **Il file `.jks` non va committato** (è già nel `.gitignore`).

## Com'è fatta

- **Kotlin + Jetpack Compose** (Material 3), niente XML per le schermate.
- **Room** per il database locale, con le enum salvate come testo così l'export resta leggibile.
- **WorkManager** per il promemoria giornaliero.
- Nessuna libreria di grafici: linee, barre, faccine e icone sono disegnate con `Canvas`.
- Niente Hilt o Dagger: le dipendenze si montano a mano in `Contenitore`.

```
app/src/main/java/com/kidstracker/
├── domain/        Modelli e statistiche — Kotlin puro, testato
├── data/          Room, repository, preferenze, backup JSON/CSV
├── notifiche/     Promemoria giornaliero
└── ui/
    ├── tema/      Colori, tipografia, misure
    ├── componenti/Faccine, schede sticker, grafici, icone
    └── schermate/ Le sei schermate
```

## Le scelte di colore

I due colori dei bambini (blu `#1B6FE3` e magenta `#C42A86`) sono stati scelti perché
restano distinguibili anche con daltonismo (ΔE 15.9 in simulazione protanopia) e perché
non si confondono con il rosso/giallo/verde delle faccine. Il verde/giallo/rosso non viene
mai usato da solo: la forma della bocca porta lo stesso significato del colore.

L'app è solo in tema chiaro: il fondo crema fa parte dell'identità e le faccine colorate
su fondo scuro perderebbero contrasto.

## Font

[Fredoka](https://fonts.google.com/specimen/Fredoka) e
[Figtree](https://fonts.google.com/specimen/Figtree), entrambi con licenza SIL Open Font
License. Le licenze sono in [`docs/`](docs/).
