
# World Generator Simple

## Einführung

Der World Generator Simple ist ein einfacher Welt-Generator, der
für die Generierung von sehr einfachen Welten verwendet wird.

## Initialisierung

Die Initialisierung des World Generator Simple erfolgt durch
die Angabe von Parametern wie Größe, Biome und Ressourcenverteilung.
Diese Parameter werden in der Datenbank gespeichert, um den
Fortschritt der Generierung zu verfolgen und sicherzustellen,
dass alle Teile der Welt erfolgreich generiert wurden.

Aktuell wird nur die epische Welt unterstützt, die eine
sehr einfache Welt den Kontinent Typen:
* Wald, 
* Wüste, 
* Ozean und 
* Berge enthält. 

## Asset-Generierung

Die Asset-Generierung im World Generator Simple umfasst die
Erstellung der Assets für die untergründe der Welt. Texturen
für die Materialien wie Gras, Sand, Wasser und Felsen
werden generiert und in der Datenbank gespeichert. Diese
Texturen werden später verwendet, um die Welt visuell darzustellen.

Es werden auch die Materialien für jedes Asset generiert.

Die Assets sind bereits im Projekt enthalten und werden
nicht dynamisch generiert. Sie werden aus den resources
geladen und in der Datenbank gespeichert.

### Assets / Materialien

* **gras**: Textur für Gras, wird auf den Kontinenten verwendet.
* **sand**: Textur für Sand, wird in Wüsten und Stränden verwendet
* **wasser**: Textur für Wasser, wird in Ozeanen und Seen verwendet
* **felsen**: Textur für Felsen, wird in Bergen und felsigen Gebieten verwendet
* **baum**: Textur für Bäume, wird in Wäldern verwendet
* **blume**: Textur für Blumen, wird in Wäldern und Wiesenverwendet
* **gras_boden**: Textur für Grasboden, wird in Wäldern und Wiesen verwendet
* **sand_boden**: Textur für Sandboden, wird in Wüsten und Stränden verwendet
* **wasser_boden**: Textur für Wasserboden, wird in Ozeanen und Seen verwendet
* **felsen_boden**: Textur für Felsenboden, wird in Bergen und felsigen Gebieten verwendet
* **baum_boden**: Textur für Baumboden, wird in Wäldern verwendet
* **blume_boden**: Textur für Blumenboden, wird in Wäldern und Wiesen
  verwendet
* **pfad**: Textur für Pfade, wird in Wäldern und Wiesen verwendet
* **stein**: Textur für Steine, wird in Bergen und felsigen Gebieten verwendet
* **wasserfall**: Textur für Wasserfälle, wird in Bergen und felsigen
  Gebieten verwendet
* **fluss**: Textur für Flüsse, wird in Bergen und felsigen Gebieten
  verwendet
* **schnee**: Textur für Schnee, wird in Bergen und kalten Regionen
  verwendet
* **lava**: Textur für Lava, wird in Vulkanen und heißen Regionen
  verwendet
* **eis**: Textur für Eis, wird in kalten Regionen und auf Gletschern
  verwendet
* **moos**: Textur für Moos, wird in Wäldern und feuchten Gebieten
  verwendet
* **pilz**: Textur für Pilze, wird in Wäldern und feuchten Gebieten
  verwendet
* **kristall**: Textur für Kristalle, wird in Bergen und Höhlen
  verwendet
* **koralle**: Textur für Korallen, wird in Ozeanen und Meeren
  verwendet
* **muschel**: Textur für Muscheln, wird in Ozeanen und Meeren
  verwendet
* **algen**: Textur für Algen, wird in Ozeanen und Meeren
  verwendet
* **schilf**: Textur für Schilf, wird in Sümpfen und an Ufern
  verwendet
* **gras_sumpf**: Textur für Gras im Sumpf, wird in Sümpfen
  verwendet
* **sand_sumpf**: Textur für Sand im Sumpf, wird in Sümpfen
  verwendet
* **wasser_sumpf**: Textur für Wasser im Sumpf, wird in Sümpfen
  verwendet
* **felsen_sumpf**: Textur für Felsen im Sumpf, wird in Sümpfen
  verwendet
* **baum_sumpf**: Textur für Bäume im Sumpf, wird in Sümpfen
  verwendet
* **blume_sumpf**: Textur für Blumen im Sumpf, wird in Sümpfen
  verwendet
* **pfad_sumpf**: Textur für Pfade im Sumpf, wird in Sümpfen
  verwendet
* **stein_sumpf**: Textur für Steine im Sumpf, wird in Sümpfen
  verwendet
* **wasserfall_sumpf**: Textur für Wasserfälle im Sumpf,
  wird in Sümpfen verwendet
* **fluss_sumpf**: Textur für Flüsse im Sumpf, wird in Sümpfen
  verwendet
* **schnee_sumpf**: Textur für Schnee im Sumpf,
  wird in Sümpfen verwendet
* **lava_sumpf**: Textur für Lava im Sumpf, wird in
  Sümpfen verwendet
* **eis_sumpf**: Textur für Eis im Sumpf, wird in
  Sümpfen verwendet
* **moos_sumpf**: Textur für Moos im Sumpf,
  wird in Sümpfen verwendet
* **pilz_sumpf**: Textur für Pilze im Sumpf,
  wird in Sümpfen verwendet
* **kristall_sumpf**: Textur für Kristalle im Sumpf
  wird in Sümpfen verwendet
* **koralle_sumpf**: Textur für Korallen im Sumpf,
  wird in Sümpfen verwendet
* **muschel_sumpf**: Textur für Muscheln im Sumpf
  wird in Sümpfen verwendet
* **algen_sumpf**: Textur für Algen im Sumpf,
  wird in Sümpfen verwendet
* **schilf_sumpf**: Textur für Schilf im Sumpf,
  wird in Sümpfen verwendet
* **gras_wüste**: Textur für Gras in der Wüste,
  wird in Wüsten verwendet
* **sand_wüste**: Textur für Sand in der Wüste,
  wird in Wüsten verwendet
* **wasser_wüste**: Textur für Wasser in der Wüste,
  wird in Wüsten verwendet
* **felsen_wüste**: Textur für Felsen in der Wüste,
  wird in Wüsten verwendet
* **baum_wüste**: Textur für Bäume in der Wüste,
  wird in Wüsten verwendet
* **blume_wüste**: Textur für Blumen in der Wüste,
  wird in Wüsten verwendet
* **pfad_wüste**: Textur für Pfade in der Wüste,
  wird in Wüsten verwendet
* **stein_wüste**: Textur für Steine in der Wüste,
  wird in Wüsten verwendet
* **wasserfall_wüste**: Textur für Wasserfälle in der Wüste,
  wird in Wüsten verwendet
* **fluss_wüste**: Textur für Flüsse in der Wüste,
  wird in Wüsten verwendet
* **schnee_wüste**: Textur für Schnee in der Wüste,
  wird in Wüsten verwendet
* **lava_wüste**: Textur für Lava in der Wüste,
  wird in Wüsten verwendet
* **eis_wüste**: Textur für Eis in der Wüste,
  wird in Wüsten verwendet
* **moos_wüste**: Textur für Moos in der Wüste,
  wird in Wüsten verwendet
* **pilz_wüste**: Textur für Pilze in der Wüste,
  wird in Wüsten verwendet
* **kristall_wüste**: Textur für Kristalle in der Wüste,
  wird in Wüsten verwendet
* **koralle_wüste**: Textur für Korallen in der Wüste,
  wird in Wüsten verwendet
* **muschel_wüste**: Textur für Muscheln in der Wüste,
  wird in Wüsten verwendet
* **algen_wüste**: Textur für Algen in der Wüste,
  wird in Wüsten verwendet
* **schilf_wüste**: Textur für Schilf in der Wüste,
  wird in Wüsten verwendet
* **gras_ozean**: Textur für Gras im Ozean,
  wird in Ozeanen verwendet
* **sand_ozean**: Textur für Sand im Ozean,
  wird in Ozeanen verwendet
* **wasser_ozean**: Textur für Wasser im Ozean,
  wird in Ozeanen verwendet
* **felsen_ozean**: Textur für Felsen im Ozean,
  wird in Ozeanen verwendet
* **baum_ozean**: Textur für Bäume im Ozean,
  wird in Ozeanen verwendet
* **blume_ozean**: Textur für Blumen im Ozean,
  wird in Ozeanen verwendet
* **pfad_ozean**: Textur für Pfade im Ozean,
  wird in Ozeanen verwendet
* **stein_ozean**: Textur für Steine im Ozean,
  wird in Ozeanen verwendet
* **wasserfall_ozean**: Textur für Wasserfälle im Ozean,
  wird in Ozeanen verwendet
* **fluss_ozean**: Textur für Flüsse im Ozean,
  wird in Ozeanen verwendet
* **schnee_ozean**: Textur für Schnee im Ozean,
  wird in Ozeanen verwendet
* **lava_ozean**: Textur für Lava im Ozean,
  wird in Ozeanen verwendet
* **eis_ozean**: Textur für Eis im Ozean,
  wird in Ozeanen verwendet
* **moos_ozean**: Textur für Moos im Ozean,
  wird in Ozeanen verwendet
* **pilz_ozean**: Textur für Pilze im Ozean,
  wird in Ozeanen verwendet
* **kristall_ozean**: Textur für Kristalle im Ozean,
  wird in Ozeanen verwendet
* **koralle_ozean**: Textur für Korallen im Ozean,
  wird in Ozeanen verwendet
* **muschel_ozean**: Textur für Muscheln im Ozean,
  wird in Ozeanen verwendet
* **algen_ozean**: Textur für Algen im Ozean,
  wird in Ozeanen verwendet
* **schilf_ozean**: Textur für Schilf im Ozean,
  wird in Ozeanen verwendet
* **gras_berge**: Textur für Gras in den Bergen,
  wird in Bergen verwendet
* **sand_berge**: Textur für Sand in den Bergen,
  wird in Bergen verwendet
* **wasser_berge**: Textur für Wasser in den Bergen,
  wird in Bergen verwendet
* **felsen_berge**: Textur für Felsen in den Bergen,
  wird in Bergen verwendet
* **baum_berge**: Textur für Bäume in den Bergen,
  wird in Bergen verwendet
* **blume_berge**: Textur für Blumen in den Bergen,
  wird in Bergen verwendet
* **pfad_berge**: Textur für Pfade in den Bergen,
  wird in Bergen verwendet
* **stein_berge**: Textur für Steine in den Bergen,
  wird in Bergen verwendet

...

## Kontinent-Generierung

Es wird eine zufällige Anzahl von Kontinenten generiert,
die jeweils einen Kontinent-Typ haben. Die Kontinente
werden in der Datenbank gespeichert und können später
abgerufen werden. Jeder Kontinent hat eine zufällige Größe
und Form, die auf den Parametern der Initialisierung basiert.

Kontinente können sich nicht überlappen.

## Terrain-Generierung

Die Terrain-Generierung im World Generator Simple erfolgt
durch die Erstellung von einfachen Landschaften für jeden
Kontinent.


