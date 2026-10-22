# Fernbedienung und SmartTV

## Einrichtung

```text
/boiler display source 1 smarttv
/boiler remote 1
```

Die Fernbedienung ist ein `CARROT_ON_A_STICK` mit einer persistent gespeicherten Display-ID. Ein umbenanntes normales Item wird nicht als Fernbedienung erkannt. Das Item kann weitergegeben werden; der Benutzer benötigt weiterhin die Berechtigungen des Displaysystems.

| Berechtigung | Zweck |
| --- | --- |
| `boiler.command.remote` | Fernbedienung per Befehl erhalten |
| `boiler.command.action` | Fernbedienung und SmartTV bedienen |
| `boiler.command.source` | Quellen aktivieren und Parameter einrichten |
| `boiler.command.settings` | Einstellungs-App öffnen und Displayoptionen ändern |

Wie bei den vorhandenen Boiler-Befehlen erhalten Operatoren standardmäßig Zugriff. Andere Spieler benötigen die entsprechenden Permission-Zuweisungen.

## Bedienung

Fernbedienung in die **Haupthand** nehmen. Laufen und Springen verändern die Spielerposition nicht; Blickbewegungen bleiben möglich. WASD wird auf die bestehenden `W/A/S/D`-Eingaben abgebildet, Springen auf `SPACE` und Sneaken auf `SHIFT`. Richtungen wiederholen sich nach acht Serverticks alle zwei Ticks; Bestätigen und Zurück werden einmal je Tastendruck ausgelöst.

Im TV navigiert WASD zwischen den Kacheln. Springen öffnet die markierte Quelle, Sneaken beendet die Wiedergabe und öffnet das Quellenmenü. Die Seitenpfeile wechseln zwischen weiteren Apps. Normale Display-Klicks öffnen Kacheln ebenfalls. Mit der Fernbedienung werden Klicks auf das gebundene Display einmal weitergeleitet; Klicks auf andere Displays lösen keine parallele Steuerung aus.

Ab **384×256 Pixeln (3×2 Karten)** beziehungsweise **256×384 Pixeln (2×3 Karten)** erscheint die von webOS inspirierte Startseite: dunkler Hintergrund, großes Banner, farbige Kategorien, App-Leiste und zuletzt verwendete Quellen. Kleinere Displays behalten die kompakte Kachelansicht. Alle Grafiken und Icons werden lokal gezeichnet; es werden keine Bilder oder Webdienste nachgeladen. Unveränderte Menüs verwenden einen zwischengespeicherten Frame.

Auf der großen Startseite wechseln A/D zwischen Apps. W führt zu den Kategorien, ein weiteres W zur Seitenleiste. In der Seitenleiste wählt W/S zwischen Home, Apps und Einstellungen; D führt zurück zu den Apps. S führt von der App-Leiste zu den zuletzt verwendeten Quellen. Springen bestätigt jeweils die Auswahl. Alle sichtbaren Kategorien, App-Kacheln, Seitenpfeile, Quellenkarten und Seitenleisten-Symbole sind auch anklickbar.

## Einstellungs-App

Die App **Einstellungen** ist in der App-Liste und auf großen Displays über das Zahnrad in der Seitenleiste erreichbar. Sie benötigt `boiler.command.settings` sowie die normale Bedienberechtigung; `boiler.command.source` ist dafür nicht erforderlich.

| Bereich | Optionen |
| --- | --- |
| Bild | Farbdithering, leuchtende Rahmen, Kartendrehung, Blickrichtung, horizontaler und vertikaler Drehwinkel |
| Leistung | Bildpuffer, Paketbündelung, Renderintervall, Sichtweite |
| Ton | Tonreichweite |

W/S wechselt durch die Optionen und die Bereiche. A/D ändert den ausgewählten Wert. Springen schaltet einen Schalter um beziehungsweise öffnet bei Zahlen eine private Chat-Abfrage für einen exakten Wert. Die Blickrichtung lässt sich mit A/D oder Springen durchschalten. Ein Klick auf einen Schalter schaltet ihn um; die Pfeile neben Zahlen ändern den Wert. Ein Klick auf den Zahlenwert öffnet die Chat-Eingabe. Ein Rechtsklick auf eine Option stellt den Standard wieder her.

Die Änderungen werden sofort über die vorhandenen Displayeinstellungen gespeichert. Bei Änderungen an der Darstellung werden die Rahmen neu angezeigt. Die Tonreichweite gilt beim nächsten Start einer Audioquelle. Die Quellenkonfiguration bleibt erhalten. Renderintervall und Reichweiten müssen positiv sein, die Kartendrehung liegt zwischen 0 und 7, Drehwinkel müssen endlich sein. Sneaken schließt die Einstellungs-App; bei einer offenen Eingabe bricht das erste Sneaken zunächst die Eingabe ab.

Slotwechsel beendet die Fernsteuerung. Ablegen, Handwechsel, Tod oder Teleport beenden sie ebenfalls; anschließend das Item erneut auswählen, um wieder zu steuern. Ein serverseitig geöffnetes Inventar pausiert die Steuerung bis zum Schließen. Das rein clientseitige Öffnen des eigenen Inventars über E hat kein Bukkit-Öffnungsereignis; die erste Inventarinteraktion beendet die Sitzung. Logout, fehlende Berechtigungen, ein entferntes Display und die Plugin-Abschaltung räumen die Sitzung auf. Bewegungsgeschwindigkeiten und Flugberechtigungen werden nicht geändert.

Die Fernbedienung nutzt Paper 1.21.6 und benötigt keine zusätzliche Client-Mod. Die Bewegungsposition wird serverseitig gesperrt; die normale clientseitige Bewegungsvorhersage kann kurze visuelle Korrekturen verursachen. Client-Streaming-Quellen benötigen weiterhin ihre vorhandene Client-Mod und MediaMTX-Konfiguration.

## Quellen und Parameter

Das Menü übernimmt die öffentlich registrierten Quellentypen alphabetisch, einschließlich nachträglich registrierter Erweiterungen. Interne Quellen wie `default` und `error` sowie SmartTV selbst erscheinen nicht als Kacheln. Neue Registrierungen werden bei der nächsten Navigation beziehungsweise beim Öffnen des Menüs übernommen.

Fehlende Parameter werden entsprechend `@CreateCommandArguments` einzeln privat im Chat abgefragt. Nur Nachrichten des einrichtenden Spielers werden abgefangen. Ganze Zahlen, endliche Dezimalzahlen und `true`/`false` werden typgeprüft; ungültige Eingaben können korrigiert werden. `abbrechen` oder Sneaken brechen die Einrichtung ab. Nach 120 Sekunden ohne Eingabe wird die Chat-Erfassung freigegeben. Der vorhandene Befehl `/boiler display action <id> input <text>` kann ebenfalls die aktuelle Abfrage beantworten.

Bereits vollständige Parameter werden bei erneuter Auswahl wiederverwendet. **Rechtsklick auf eine Kachel** fragt alle Parameter erneut ab. Ein einzelner Punkt übernimmt den bisherigen Wert des aktuellen Feldes. Auch nach einem Serverneustart bleiben die Werte erhalten; der TV startet zunächst im Menü.

Menü und Wiedergabe werden gemeinsam pro Display angezeigt. Während einer Einrichtung besitzt der auslösende Spieler die Eingabe; andere Benutzer können diese Abfrage nicht beantworten oder abbrechen. Ein Spieler kann gleichzeitig nur eine Chat-Abfrage besitzen.

## Quellen-Lebenszyklus und Speicherung

SmartTV bleibt die äußere Display-Quelle (`sourceName() == "smarttv"`). Die Daten enthalten `selected`, ein Objekt `sources` mit den zuletzt verwendeten Parametern je Quellenname sowie die optionale Liste `recent` mit bis zu sechs zuletzt verwendeten Quellen. Bestehende Display-Dateien benötigen keine Migration. Displayoptionen bleiben im vorhandenen `settings`-Objekt des Displays gespeichert.

Die untergeordnete Quelle sieht über ihren Display-Adapter ihren eigenen Namen, ihre Daten und ihre aktive Quelleninstanz. Interne Weiterleitungen, etwa YouTube zu GStreamer oder Twitch zu FFmpeg, und interne Fehlerquellen bleiben im TV. Veraltete Callbacks können nach einem Wechsel keine Quellen oder Daten mehr überschreiben. Zeichnung, Audio, Klicks, Scrollen, Text, Tasten und Cursorereignisse werden an die aktive Quelle weitergereicht; `SHIFT` beziehungsweise `ESCAPE` öffnen das Menü.

Zurück beendet die untergeordnete Quelle. Ein erneuter Start öffnet sie mit ihren gespeicherten Parametern neu; die Wiedergabeposition wird nicht gespeichert. Clone-Verbindungen, Renderpause, Viewport und der Client-Darstellungsmodus werden beim Wechsel zurückgesetzt. Beim Öffnen einer Client-Quelle wird der bestehende Streamingpfad aktiviert, beim Zurückkehren wieder die normale MapEngine-Darstellung.

## Verifikation

Die automatisierten Tests prüfen Item-Markierungen und Befehlspfade, Eingabemapping und Wiederholung, Positionssperre mit freier Blickrichtung, Sitzungsenden, private Chat-Abfragen, Typprüfung, Berechtigungen, Speicherung, Menü-Hitboxen, sechs Rendergrößen, Kategorien und Seitenleiste, alle Displayoptionen, Einstellungsgrenzen und Standardwerte sowie deren Übernahme in die bestehende Renderpipeline. Dazu kommen interne Weiterleitungen, verspätete Ladeergebnisse, Fehler, Client-Moduswechsel und die Freigabe einer echten `CloneSource`. Die vorhandenen Lifecycle-, Rendering-, Persistenz- und nativen FFmpeg-Tests bleiben Bestandteil der Testsuite.

Build mit Java 21:

```powershell
$env:JAVA_HOME = 'C:\Pfad\zum\jdk-21'
.\gradlew.bat --no-daemon --console=plain --gradle-user-home build/gradle-user-home build
```

Die JAR liegt unter `build/libs/Boiler-3.0.3-all.jar`, der Testbericht unter `plugin/build/reports/tests/test/index.html`. Menüvorschauen werden beim Test unter `plugin/build/reports/smarttv-preview/` erzeugt.

Für die Live-Abnahme auf Paper 1.21.6 mit MapEngine und MapMediaExt:

1. SmartTV und Fernbedienung für ein Display erzeugen; alle sechs Eingaben sowie Slotwechsel und freie Blickrichtung prüfen.
2. Bild, YouTube/Twitch, Clone und eine konfigurierte Client-Streaming-Quelle jeweils starten und mit Sneaken zurückkehren. Bild, Ton, Prozesse und Clone-Verbindungen müssen dabei sauber enden.
3. Mit zwei Spielern unterschiedliche Chat-Abfragen sowie eine belegte Abfrage am gleichen TV prüfen; Parameter dürfen nicht im öffentlichen Chat erscheinen.
4. Während Einrichtung und Wiedergabe Item ablegen, Hand wechseln, Inventar öffnen, teleportieren, sterben und ausloggen. Außerdem Display entfernen und den Server vollständig neu starten.
5. Auf Displays mit 3×2 und 2×3 Karten alle Kategorien, App-Seiten, zuletzt verwendeten Quellen und Einstellungsbereiche mit Fernbedienung und Maus bedienen. Dithering, Pufferung, Renderintervall, Ausrichtung und Reichweiten ändern und nach dem Neustart erneut prüfen.

Ein laufender Minecraft-Testserver mit Spielern und Streaming-Clients steht im Workspace nicht zur Verfügung. Die Live-Abnahme ist deshalb zusätzlich zur automatisierten Prüfung erforderlich.
