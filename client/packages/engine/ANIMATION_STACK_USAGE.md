# AnimationStack Usage Examples

## Overview

AnimationStacks wurden für folgende Environment-Eigenschaften erstellt:
- `ambientLightIntensity` - Ambient light intensity (EnvironmentService)
- `sunPosition` - Sun horizontal position in degrees (SunService)
- `sunElevation` - Sun vertical elevation in degrees (SunService)
- `horizonGradientAlpha` - Horizon gradient transparency (HorizonGradientService)

## Command Usage

### setStackModifier

Setzt oder aktualisiert einen Modifier in einem Stack mit optionaler Animation.

**Syntax:**
```
setStackModifier <stackName> <modifierName> <value> [prio] [waitTime]
```

**Parameter:**
- `stackName`: Name des Stacks (z.B. 'ambientLightIntensity', 'sunPosition')
- `modifierName`: Name/ID für den Modifier (zum Update oder Erstellen)
- `value`: Der zu setzende Wert (Typ abhängig vom Stack)
- `prio`: Optional - Priorität (Standard: 50, niedriger = höhere Priorität)
- `waitTime`: Optional - Wartezeit in Millisekunden für AnimationStack (Standard: 100ms)

**Beispiele:**

```bash
# Ambient light intensity animiert von aktuell auf 0.5 setzen
setStackModifier ambientLightIntensity weather 0.5 10

# Mit custom wait time (langsamere Animation)
setStackModifier ambientLightIntensity weather 0.8 10 500

# Sonne auf Sonnenuntergang-Position animieren (West = 270°)
setStackModifier sunPosition sunset 270 20 200

# Sonne zum Horizont animieren
setStackModifier sunElevation sunset 0 20 300

# Horizont-Gradient ausblenden (transparent)
setStackModifier horizonGradientAlpha night 0.0 10 200

# Horizont-Gradient einblenden
setStackModifier horizonGradientAlpha day 0.7 10 200
```

### getStackModifierCurrentValue

Holt den aktuellen effektiven Wert eines Stacks.

**Syntax:**
```
getStackModifierCurrentValue <stackName>
```

**Beispiele:**

```bash
# Aktuellen Wert der ambient light intensity abrufen
getStackModifierCurrentValue ambientLightIntensity

# Aktuelle Sonnenposition abrufen
getStackModifierCurrentValue sunPosition

# Aktuelle Sonnenelevation abrufen
getStackModifierCurrentValue sunElevation

# Aktuelle Horizont-Gradient Alpha abrufen
getStackModifierCurrentValue horizonGradientAlpha
```

## Scenario Examples

### Tag/Nacht-Übergang

```bash
# Tagesanbruch (Dawn)
setStackModifier ambientLightIntensity time_of_day 0.6 5 1000
setStackModifier sunPosition time_of_day 90 5 2000    # Osten
setStackModifier sunElevation time_of_day 15 5 2000   # Niedrig über Horizont
setStackModifier horizonGradientAlpha time_of_day 0.8 5 1000

# Mittag (Noon)
setStackModifier ambientLightIntensity time_of_day 1.0 5 1000
setStackModifier sunPosition time_of_day 180 5 2000   # Süden
setStackModifier sunElevation time_of_day 80 5 2000   # Hoch am Himmel
setStackModifier horizonGradientAlpha time_of_day 0.3 5 1000

# Sonnenuntergang (Sunset)
setStackModifier ambientLightIntensity time_of_day 0.4 5 1000
setStackModifier sunPosition time_of_day 270 5 2000   # Westen
setStackModifier sunElevation time_of_day 5 5 2000    # Knapp über Horizont
setStackModifier horizonGradientAlpha time_of_day 0.9 5 1000

# Nacht (Night)
setStackModifier ambientLightIntensity time_of_day 0.1 5 1000
setStackModifier sunElevation time_of_day -20 5 2000  # Unter Horizont
setStackModifier horizonGradientAlpha time_of_day 0.2 5 1000
```

### Wetter-Effekte

```bash
# Sturm (dunkler, dramatisch)
setStackModifier ambientLightIntensity weather 0.3 10 500
setStackModifier horizonGradientAlpha weather 0.9 10 500

# Klarer Tag (hell, klar)
setStackModifier ambientLightIntensity weather 1.2 10 500
setStackModifier horizonGradientAlpha weather 0.4 10 500

# Nebel (gedämpft)
setStackModifier ambientLightIntensity weather 0.6 10 500
setStackModifier horizonGradientAlpha weather 0.95 10 500
```

### Dynamische Anpassungen

```bash
# Schnelle Anpassung (kurze waitTime)
setStackModifier ambientLightIntensity emergency 0.2 1 50

# Langsame, sanfte Anpassung (lange waitTime)
setStackModifier sunElevation cinematic 45 5 2000

# Sofortige Änderung (waitTime 0 oder sehr klein)
setStackModifier ambientLightIntensity instant 1.0 1 1
```

## Animation Characteristics

Die AnimationStacks verwenden **exponential easing** für smooth Übergänge:

- **Ambient Light Intensity**: Faktor 0.1 (schnellere Anpassung)
- **Sun Position**: Faktor 0.05 (langsamere, sanfte Bewegung)
- **Sun Elevation**: Faktor 0.05 (langsamere, sanfte Bewegung)
- **Horizon Gradient Alpha**: Faktor 0.1 (schnellere Anpassung)

**Exponential Easing bedeutet:**
- Anfangs schnellere Bewegung
- Verlangsamung beim Annähern an den Zielwert
- Sehr sanfte, natürliche Übergänge

## Priority System

Niedrigere Prioritätswerte = höhere Priorität:

- **0-10**: System-kritische Modifiers (z.B. Cutscenes, Story-Events)
- **11-30**: Spezielle Events (z.B. Zauber, Effekte)
- **31-50**: Normale Modifiers (z.B. Zeit des Tages, Wetter)
- **51-100**: Niedrige Priorität (z.B. Ambient-Effekte)

Wenn mehrere Modifier dieselbe Priorität haben, gewinnt der neueste.

## Integration mit Scrawl Scripts

AnimationStacks können perfekt in Scrawl Scripts integriert werden:

```json
{
  "actions": [
    {
      "command": "setStackModifier",
      "params": ["ambientLightIntensity", "script_dawn", "0.6", "5", "1000"]
    },
    {
      "command": "wait",
      "params": ["2000"]
    },
    {
      "command": "setStackModifier",
      "params": ["sunElevation", "script_dawn", "15", "5", "2000"]
    }
  ]
}
```
