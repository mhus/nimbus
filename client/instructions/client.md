
# Client

Der Client macht alles was Darstellung und Bewegung des
user Entity erfordert. Der Server vertraut auf Bewegung und
Geschwindigkeit des Entities.

Der Server prüft nur relevante Bewegungsdaten, z.b. verbotene Zohnen,
Türen die offen oder zu sind (?).

Bei Kampfen prüft der Server. Der Client stellt dar.

Der Client sendet die Position udn Ausrichtung des Entities
bei jeder Änderung zum Server, aber maximal 4 mal pro Sekunde.

Der Server sendet Entity Bewegungen zum Client, aber nur max 
4mal pro Sekunde, cumulativ und bereinigt. (Oder direkt, dann
ergibt sich ein flüssigeres Bild?)

Die Anzahl kann je nach Qualität der Verbindung variieren.
