# Performanceoptimierung und Verifikation

Stand: 22. Oktober 2026. Der vorhandene Checkout wurde optimiert; öffentliche API-Signaturen, Plugin-Nachrichten und bestehende JSON-/Intervallformate bleiben erhalten. Es wurden keine Produktionsbibliotheken pauschal aktualisiert.

## Änderungen

- Gemeinsamer Render-Scheduler mit höchstens acht Threads; gleiche Startkadenz ohne Nachholbursts. Pro Display wird höchstens ein Bild gleichzeitig gezeichnet. Pausierte Displays und Durchläufe ohne berechtigte Empfänger zeichnen und flushen nicht.
- Sichtbarkeit auf dem Serverthread, eine Positionsaufnahme je Spieler und Durchlauf, Gruppierung nach Welt und quadratische Distanzen. `autoTick=false` wird berücksichtigt. MapDisplay-Ereignisse nutzen einen Index.
- Wiederverwendbarer transparenter Rasterpuffer für den Graphics2D-Adapter. Direkte `draw(IDrawingSpace)`-Implementierungen behalten ihren eigenen Zeichenpfad. Graphics2D-Objekte werden geschlossen. Tastaturen werden erst nach Änderungen oder Ablauf der Druckanzeige neu gezeichnet; Swing-Zustand bleibt auf dem EDT.
- Serialisierte Quellwechsel mit Generationsprüfung und höchstens einer wartenden, ersetzbaren Anforderung je Display. Verspätete HTTP-/EOF-Callbacks können neuere Quellen nicht überschreiben. Die globale Lade- und HTTP-Warteschlange ist jeweils auf 128 Aufgaben bei vier Arbeitsthreads begrenzt; bei Überlast erscheint ein Fehlerbild statt eines zusätzlichen Threads oder blockierender Arbeit auf dem Serverthread.
- Gemeinsame FFmpeg-Zeitachse für Video und Audio, blockierende begrenzte Queues, wiederverwendbare dekodierte Bildpuffer und native Audio-Konvertierung nach 48 kHz, mono, PCM16 little endian. Dateien werden vollständig abgespielt; Video ohne Audio und Schleifenwiedergabe werden gesondert geprüft. Schmale Videos mit aufgefüllten Bildzeilen werden korrekt behandelt.
- Primitive PCM-Ringpuffer statt einzelner `Short`-Objekte; Audioencoder entstehen bei FFmpeg nur für Medien mit Audiospur. GStreamer kopiert Pixel blockweise, vermeidet programmgesteuerte Seek-Schleifen und verwendet einen gemeinsam initialisierten Kontext. Der frühere fünfsekündige Start-Sleep samt Testplayer entfällt.
- Streamstatus-Abfragen laufen außerhalb des Serverthreads, bündeln gleichzeitig laufende Abfragen und halten Ergebnisse 250 ms vor. HTTP-Clients werden geteilt; Requests und Verbindungsressourcen werden beim Stoppen freigegeben.
- Persistenz erzeugt JSON direkt aus dem Zustand und ersetzt Dateien atomar, soweit das Dateisystem dies unterstützt. Das Laden überschreibt keine teilweise initialisierten Displays. Nicht persistente Displays haben eigene IDs und werden ebenfalls verwaltet.
- Disable stoppt Displays, native Quellen, Scheduler, HTTP und Prozesse, ohne gespeicherte Displays zu löschen. Die Laufzeit wartet insgesamt höchstens sechs Sekunden auf Hintergrundarbeit und meldet verbleibende Arbeit. Die tatsächlich verwendeten FFmpeg-Leseoptionen können diese Frist beeinflussen, wenn eigene Optionen Timeouts überschreiben.

## Live-Puffer und Kompatibilität

Bei `rtmp-server` und durch Twitch erzeugten FFmpeg-Quellen ist der Live-Modus standardmäßig aktiv. Für normale FFmpeg-/URL-/Dateiquellen bleibt er aus. Optional kann in den vorhandenen Quelldaten `"live": true` oder `false` gesetzt werden; die bisherigen positionsabhängigen Befehlsargumente bleiben gleich:

```json
{"url":"rtsp://example/live","buffer":10,"live":true}
```

Die gemeinsame Medienqueue hält im Live-Modus höchstens 500 ms Differenz zwischen den gepufferten Zeitstempeln und nie mehr Einträge als `buffer` erlaubt. Bei Überlast werden alte Audio-/Videoereignisse verworfen und der Audio-Rückstau geleert. Der primitive Live-Audiopuffer ist zusätzlich auf 24.000 Samples (500 ms) begrenzt. Das ist eine Grenze für die Boiler-Puffer, keine Garantie für die gesamte Verzögerung von Netzwerk, Decoder, MediaMTX und Client. Ohne Live-Modus blockiert ein voller Puffer den Produzenten; Audio wird dabei nicht absichtlich verworfen. Auch ohne Voicechat-Backend folgt Video seiner Medienzeitachse.

## Reproduzierbarer Build

Gradle 8.5 muss mit Java 21 gestartet werden. Das auf diesem Rechner standardmäßig ausgewählte Java 25 eignet sich nicht für diesen Wrapper. In PowerShell:

```powershell
$env:JAVA_HOME = 'C:\Pfad\zum\jdk-21'
.\gradlew.bat --no-daemon --console=plain --gradle-user-home build/gradle-user-home build
.\gradlew.bat --no-daemon --console=plain --gradle-user-home build/gradle-user-home :plugin:performanceBenchmark
```

Die nativen FFmpeg-Testbibliotheken werden passend zum Betriebssystem als reine Testabhängigkeiten aufgelöst. Sie gelangen nicht in die Release-JAR. Bei eingeschränkter Prozesskommunikation benötigt Gradle Zugriff auf seinen lokalen Daemon.

- Installierbares Plugin: `build/libs/Boiler-3.0.3-all.jar`
- JUnit-Bericht: `plugin/build/reports/tests/test/index.html`
- Rohmessungen: `build/reports/performance/raster.csv`

Die 38 automatisierten Tests prüfen Renderüberschneidungen und Intervallwechsel, Entfernen während des Ladens, verspätete Callbacks, begrenzte und zusammengefasste Ladeaufträge, Empfängerintervalle, Datei-Roundtrips und Shutdown ohne Dateilöschung. Weitere Tests prüfen PCM-Reihenfolge und Rückstau, Live-Verwerfen und Pufferrecycling, EOF, BGRx-Farben samt Zeilenpadding, Transparenz und Skalierung sowie beide Zeichenmethoden. Die nativen Tests erzeugen echte FFmpeg-Dateien, prüfen Video ohne Audio, Audio-Resampling, Schleifen und zehn Lade-/Stoppzyklen mit Freigabe beider Medienthreads. Ein separater Test prüft den unveränderten Streamstatus-Nachrichtenaufbau und die asynchrone Antwort.

## Gemessener Vergleich

Windows x64, Microsoft OpenJDK 21.0.12.1, Heap 256–512 MiB. Pro Fall 20 Aufwärmbilder und 60 Messbilder; 1, 10 und 50 Displays bei 384×128 und 1024×576 Pixeln, jeweils mit und ohne Empfänger. Die Ausführungsreihenfolge wechselt zwischen den Szenarien.

Der Benchmark isoliert den Java2D-Rasteradapter. Drei synthetische Bild-/Video-/Stream-Zeichenquellen und die reale Tastaturquelle bilden die Mischung. Er vergleicht den ursprünglichen Adapter mit neuer Bildallokation gegen den wiederverwendbaren Adapter; Grafikressourcen werden in beiden Varianten geschlossen. Die Quellenoptimierungen selbst sind auf beiden Seiten gleich. Decoder-, Minecraft-, MapEngine-Konvertierungs- und Paketkosten sind nicht enthalten. Es wird ohne Echtzeit-Pausen gezeichnet.

| Displays und Auflösung | Heap-Allokation alt, MiB | Heap-Allokation neu, MiB | Laufzeit alt / neu, ms | GC-Zyklen alt / neu |
|---|---:|---:|---:|---:|
| 1 × 384×128 | 11.35 | 0.06 | 10.5 / 7.3 | 1 / 0 |
| 10 × 384×128 | 113.40 | 0.43 | 68.4 / 53.2 | 0 / 0 |
| 50 × 384×128 | 566.73 | 2.03 | 281.8 / 272.3 | 4 / 0 |
| 1 × 1024×576 | 135.10 | 0.05 | 75.9 / 67.1 | 4 / 0 |
| 10 × 1024×576 | 1350.84 | 0.38 | 616.5 / 575.1 | 25 / 0 |
| 50 × 1024×576 | 6754.05 | 1.80 | 3614.7 / 3424.4 | 84 / 0 |

Im großen Fall sinkt die gemessene Heap-Allokation des Adapters um rund 99,97 %. Die Laufzeit verbessert sich im letzten Durchlauf moderat; frühere Durchläufe zeigten auch kleine Regressionen. Daraus lässt sich keine allgemeine CPU-, FPS- oder TPS-Steigerung ableiten. JVM-Scheduling, JIT, Auflösung und Inhalt beeinflussen die kurzen Messungen. CPU-Zeiten und kumulierte GC-Zeit stehen in der CSV; einzelne GC-Pausenquantile wurden nicht gemessen. CPU-Zeiten unter einem Windows-Zeitquantum sind wenig aussagekräftig. Ohne Empfänger wird nicht gezeichnet; die kleinen Restallokationen in der CSV stammen auch aus der Messinstrumentierung.

Der Scheduler-Test prüft 1, 10 und 50 aktive Renderaufträge auf höchstens acht Renderthreads und verhindert überlappende Aufrufe auch bei Intervalländerungen. Pufferbelegung und die 500-ms-Grenze werden mit deterministischen Queue-Tests geprüft; eine Ende-zu-Ende-Liveverzögerung wurde hier nicht gemessen.

Die Übernahme des wiederverwendeten Bildes wurde zusätzlich am Quellcode von MapEngine **v1.8.7** geprüft: [DrawingSpace.image](https://github.com/MinceraftMC/MapEngine/blob/v1.8.7/plugin/src/main/java/de/pianoman911/mapengine/core/drawing/DrawingSpace.java) kopiert die RGB-Pixel synchron über [FullSpacedColorBuffer.pixels](https://github.com/MinceraftMC/MapEngine/blob/v1.8.7/api/src/main/java/de/pianoman911/mapengine/api/util/FullSpacedColorBuffer.java) in den DrawingSpace-Puffer. Das BufferedImage selbst wird dabei nicht an die spätere Pipeline übergeben.

## Grenzen und verbleibende Prüfung auf einem Server

Im Workspace steht kein laufender Minecraft-Testserver mit Spielern und Voicechat-Clients zur Verfügung. Deshalb wurden echte MSPT/TPS, Netzwerkvolumen, Verhalten der vollständigen MapEngine-Pipeline, Client-Latenz und die Integration mit VLC, GStreamer, Simple Voice Chat beziehungsweise PlasmoVoice nicht vollständig im Serverbetrieb gemessen. Bildvergleiche und native FFmpeg-Tests ersetzen diesen Integrationstest nicht. Die in der vorhandenen README beschriebene CEF-Browserquelle ist im aktuellen Checkout nicht vorhanden und wurde nicht verändert.

Für einen belastbaren Serververgleich denselben Serverstand, dieselbe Welt, Medien, Spielerbewegungen und Einstellungen verwenden. Nach 60 Sekunden Aufwärmung jeweils 180 Sekunden mit 1, 10 und 50 gemischten Displays messen; anschließend ohne Zuschauer wiederholen. Unter Java 21 kann dazu auf dem jeweiligen Testserver beispielsweise eine JFR-Aufzeichnung erstellt werden:

```text
jcmd <Server-PID> JFR.start name=Boiler settings=profile duration=180s filename=<absoluter-Pfad-zur-JFR>
```

Vorher und nachher CPU, Allokationen, Heap nach GC, GC-Pausen, Boiler-Threads, MSPT, Bildrate, Pufferbelegung und hör-/sichtbare Liveverzögerung vergleichen. Quellen wiederholt wechseln und Displays entfernen; Threads, Handles und native Ressourcen müssen anschließend wieder zum Ausgangsniveau zurückkehren. Persistierte Displays müssen nach einem vollständigen Neustart unverändert wiederhergestellt werden. Die neue JAR wurde gebaut, aber nicht auf einen produktiven Server kopiert oder veröffentlicht.
