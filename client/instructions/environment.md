
# Umgebung

## Übersicht

- Lichtquellen
- Sonne
- Sterne und Monde
- Wolken
- Nebel (done)
- Wind (done)
- Regen / Schnee Niederschlag
- Blitze
- Tageszeiten
- Spezielle Effekte: Dark events (Sturmwolken an einer stelle), Himmelsportale, etc.
- An speziellen stellen marker im Himmel (z.B. über einem Dungeon-Eingang)

Alles im EnvironmentService.

## Welt Lichtquellen

Es soll zwei lichtquellen geben: die Sonne und eine generelle Ambient Light Quelle. Die Sonne 
ist erstmal nicht mit der darstellung der sonne in verbindung zu bringen, sondern einfach nur eine
Lichtquelle die von einer Richtung aus scheint. Die Ambient Light Quelle sorgt dafür, dass auch die Schattenbereiche
nicht komplett schwarz sind.

Die Ambiente Lichtquelle ist bereits vorhanden, aktuell kann diese mit setLightIntensity() geregelt werden.
- Umbennenne in EnvironmentService von light in ambienteLigth
- Umbennenen von setLightIntensity in setAmbientLightIntensity
- Erstelle eine Methode setAmbienteLightSpecularColor(color: Color)
- Erstelle eine Methode setAmbientLightDiffuse(color: Color)
- Erstelle eine Methode setAmbientLightGroundColor(color: Color)

Erstelle ein sunLight in EnvironmentService für die Sonne.
- Erstelle eine Methode setSunLightIntensity(intensity: number)
- Erstelle weitere methoden fuer die Eigenschaften, wie z.b. direction, color ...

Erstelle in engine Commandos um beide lichtquellen zu steuern.
- ambientLight ....
- sunLight ....

```text
 Ambient Light Commands (packages/engine/src/commands/ambientLight/):
  - doAmbientLightIntensity [value] - Set/get intensity (0-10)
  - doAmbientLightDiffuse <r> <g> <b> - Set diffuse color (0-1)
  - doAmbientLightSpecular <r> <g> <b> - Set specular color (0-1)
  - doAmbientLightGroundColor <r> <g> <b> - Set ground color (0-1)

  Sun Light Commands (packages/engine/src/commands/sunLight/):
  - doSunLightIntensity [value] - Set/get intensity (0-10)
  - doSunLightDirection [x] [y] [z] - Set/get direction (normalized)
  - doSunLightDiffuse <r> <g> <b> - Set diffuse color (0-1)
  - doSunLightSpecular <r> <g> <b> - Set specular color (0-1)
```
