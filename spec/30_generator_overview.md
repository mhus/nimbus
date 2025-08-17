# Generator Overview

## Übersicht

Die Generierung der Welt erfolgt in mehreren Phasen, die jeweils
spezifische Aspekte der Welt behandeln. Jede Phase kann
unabhängig voneinander entwickelt und getestet werden, was
eine flexible Anpassung der Generierung ermöglicht. Die Phasen
können auch in verschiedenen Reihenfolgen ausgeführt werden,
abhängig von den spezifischen Anforderungen der Welt und den
gewünschten Ergebnissen.

## Generator-Phasen

Die Generierung der Welt erfolgt in mehreren Phasen, die jeweils
spezifische Aspekte der Welt behandeln. Jede Phase kann
unabhängig voneinander entwickelt und getestet werden, was
eine flexible Anpassung der Generierung ermöglicht. Die Phasen
können auch in verschiedenen Reihenfolgen ausgeführt werden,
abhängig von den spezifischen Anforderungen der Welt und den
gewünschten Ergebnissen.    
Die Phasen der Generierung können Folgendes umfassen:
- **Initialisierung**: Legt die grundlegenden Parameter der Welt fest, einschließlich Größe, Biome, Ressourcenverteilung und andere Einstellungen. Diese Phase kann auch die Festlegung von Regeln und Einschränkungen für die Generierung umfassen.
- **Asset/Material-Generierung**: Erstellt die grundlegenden Assets der Welt, einschließlich Texturen, Modelle und andere visuelle Elemente. Diese Phase kann auch die Erstellung von Soundeffekten und Musik umfassen, die in der Welt verwendet werden.
- **Kontinent-Generierung**: Erstellt die grundlegenden Kontinente und Ozeane der Welt, einschließlich der Platzierung von Landmassen, Inseln und anderen geografischen Merkmalen. Diese Phase kann auch die Festlegung von Klimazonen und Wetterbedingungen umfassen.
- **Terrain-Generierung**: Erzeugt die grundlegende Landschaft der Welt, einschließlich Berge, Täler, Flüsse und Seen. Diese Phase kann auch die Platzierung von Biomen und anderen natürlichen Merkmalen umfassen.
- **Historische Generierung**: Erstellt die Geschichte und Hintergrundinformationen der Welt, einschließlich wichtiger Ereignisse, Charaktere und Orte. Diese Phase kann auch die Festlegung von Regeln und Einschränkungen für die Welt umfassen, die das Verhalten von Spielern und NPCs beeinflussen.
- **Struktur-Generierung**: Platziert Gebäude, Dörfer, Ruinen und andere Strukturen in der Welt. Diese Phase kann auch die Platzierung von Ressourcen wie Erzen, Pflanzen und Tieren umfassen.
- **Item-Generierung**: Erstellt Gegenstände, die in der Welt gefunden oder 
  verwendet werden können, einschließlich Waffen, Rüstungen, Werkzeuge und andere nützliche Objekte. Diese Phase kann auch die Platzierung von Quests und anderen Aktivitäten umfassen.
- **Quest-Generierung**: Erstellt Quests und Aufgaben, die die Spieler in der Welt erfüllen können. Diese Phase kann auch die Platzierung von NPCs (Nicht-Spieler-Charakteren) umfassen, die den Spielern helfen oder sie herausfordern können.

Die Phasen sind nicht festgelegt und können je nach den spezifischen Anforderungen
der Welt angepasst werden. Die Phasen werden separat entwickelt und ausgeführt,
sodass eine flexible Anpassung der Generierung möglich ist.

## Generator-Konfiguration

Jede Phase kann unabhängig konfiguriert werden. Die Konfiguration erfolgt über
Parameter, die in der Datenbank gespeichert werden. Die Parameter können zur
Laufzeit geändert werden, ohne dass die Anwendung neu gestartet werden muss.

## Generator-Orchestrierung

Die Orchestrierung der Phasen erfolgt über einen Generator-Manager, der die
Reihenfolge der Phasen und deren Parameter verwaltet. Der Generator-Manager kann
auch parallele Ausführung von Phasen ermöglichen, sofern diese keine
Abhängigkeiten voneinander haben.
