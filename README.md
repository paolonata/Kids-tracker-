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
| **Impostazioni** | Nomi, tema chiaro/scuro, promemoria giornaliero, backup, export Excel/CSV. |

## Come si misura una giornata

Ogni faccina vale dei punti: **sì = 2**, **così così = 1**, **no = 0**.

- **Indice pappa** = media di primo, secondo e dolce, in percentuale.
- **Indice giornata** = media di tutte e sei le categorie (nanna, primo, secondo, dolce,
  entrata, uscita), pesate uguale.

Le categorie non segnate non contano come zero: vengono escluse dalla media. Un giorno
di assenza non ha indice e non spezza le strisce, ma nemmeno le allunga.

Il giudizio di una giornata segue l'indice: **buona** da 67 in su, **così così** da 34 a 66,
**difficile** sotto 34.

## Backup e ripristino

Da **Impostazioni → Backup** si salva un file `.json` con nomi e storico. Quel file
si ricarica in due punti: dalle impostazioni, oppure — ed è il caso che conta —
dal tasto **"Ricarica un backup"** nella schermata di benvenuto, quella che compare
al primo avvio dopo una reinstallazione. Si reinstalla l'app, si ricarica il file,
e tutto torna com'era.

Per guardare i dati altrove c'è l'esportazione in **Excel** (`.xlsx`), con tre fogli:

| Foglio | Cosa contiene |
|---|---|
| Giornate | Una riga per bambino per giorno, in parole: "sì", "così così", "no". |
| Punteggi | Gli stessi dati in numeri (0, 1, 2) più gli indici, pronti per i grafici. |
| Riepilogo | Una riga per bambino: giornate segnate, medie, giornate buone, striscia record. |

Il file `.xlsx` è scritto a mano dall'app (è uno zip con dentro degli XML), così
non serve una libreria da megabyte per produrre un foglio di calcolo vero.
C'è anche l'esportazione in CSV, se serve qualcosa di più grezzo.

## Tema chiaro e scuro

Da **Impostazioni → Aspetto**: *Sistema*, *Chiaro* o *Scuro*. Le tre faccine
(verde, giallo, rosso) hanno lo stesso colore nei due temi — sono il linguaggio
della scuola, non devono cambiare significato di sera. Cambiano invece i colori
dei due bambini, perché sul fondo scuro il blu e il magenta chiari perderebbero
contrasto: entrambe le coppie sono state verificate per restare distinguibili
anche con daltonismo.

## Compilare

Serve JDK 17 e l'SDK Android (compileSdk 35). Poi:

```bash
./gradlew testDebugUnitTest   # la matematica degli indici e il backup
./gradlew assembleDebug       # APK in app/build/outputs/apk/debug/
```

## Installare sul telefono

Ogni push fa girare [il workflow Android](.github/workflows/android.yml), che allega
l'APK agli artifact della run. Si scarica da lì e si installa (serve abilitare
l'installazione da origini sconosciute).

Per una versione stabile, basta un tag:

```bash
git tag v0.2.0 && git push origin v0.2.0
```

Il [workflow Release](.github/workflows/release.yml) allega `kids-tracker.apk` alla release
di GitHub.

### La firma: perché serve per aggiornare senza disinstallare

Android accetta di aggiornare un'app solo se la nuova versione è firmata con la
**stessa chiave** della precedente. Senza una chiave stabile, ogni build della CI
ne genera una usa e getta, e per installare la versione nuova bisogna disinstallare
quella vecchia — perdendo i dati.

Per risolverlo si genera una chiave una volta sola e la si mette nei secret del
repository. Il repository è pubblico, quindi **la chiave non va committata**: sta
nei secret, che nessun altro può leggere (il `.gitignore` copre già `*.jks`).

```bash
keytool -genkeypair -v -keystore kids.jks -storetype PKCS12 -keyalg RSA \
  -keysize 4096 -validity 10000 -alias kids
base64 -w0 kids.jks   # il risultato va nel secret KEYSTORE_BASE64
```

Poi si aggiungono quattro secret al repository (Settings → Secrets and variables →
Actions): `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.

Da quel momento in poi **anche l'APK di ogni push è firmato con quella chiave**, non
solo le release: ogni build si installa sopra la precedente. Se i secret non ci sono,
la build funziona lo stesso ma torna alla firma usa e getta.

Una nota su cosa non si può recuperare: se la chiave si perde, le installazioni
esistenti non si possono più aggiornare. Vale la pena tenerne una copia da qualche
parte al sicuro.

## Com'è fatta

- **Kotlin + Jetpack Compose** (Material 3), niente XML per le schermate.
- **Room** per il database locale, con le enum salvate come testo così l'export resta leggibile.
- **WorkManager** per il promemoria giornaliero.
- Nessuna libreria di grafici: linee, barre, faccine e icone sono disegnate con `Canvas`.
- Niente Hilt o Dagger: le dipendenze si montano a mano in `Contenitore`.

```
app/src/main/java/com/kidstracker/
├── domain/        Modelli e statistiche — Kotlin puro, testato
├── data/          Room, repository, preferenze, backup JSON/CSV/XLSX
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

Nel tema scuro i due colori diventano `#4D93F0` e `#E45BA6`, riverificati sul fondo
scuro (ΔE 12.2). Le faccine restano verdi, gialle e rosse in entrambi i temi.

## Font

[Fredoka](https://fonts.google.com/specimen/Fredoka) e
[Figtree](https://fonts.google.com/specimen/Figtree), entrambi con licenza SIL Open Font
License. Le licenze sono in [`docs/`](docs/).
