
# Docker

Docker Image erstellen mit Java JRE 25 image.

## JVM Images bauen und starten

```text

  Wie es funktioniert:

  Das Script baut alle 4 Services nacheinander mit --target:
  1. player (world-player)
  2. control (world-control)
  3. life (world-life)
  4. generator (world-generator)

  Der Vorteil:

  ✅ Build-Stage wird gecacht - Maven kompiliert nur beim ersten Service
  ✅ Alle nachfolgenden Builds nutzen den Cache und kopieren nur die fertigen JARs
  ✅ Viel schneller als 4x einzeln zu bauen

  Verwendung:

  # Alle Services auf einmal bauen
  ./build-all.sh

  # Mit custom Tag
  ./build-all.sh --tag v1.0.0

  # Für AMD64
  ./build-all.sh --amd64

  # Danach alle starten
  ./run-player-image.sh -d
  ./run-control-image.sh -d

  Build-Reihenfolge:

  1. Erster Service (player): Maven kompiliert alles (dauert länger)
  2. Zweiter Service (control): Nutzt gecachte Build-Stage (sehr schnell!)
  3. Dritter Service (life): Nutzt gecachte Build-Stage (sehr schnell!)
  4. Vierter Service (generator): Nutzt gecachte Build-Stage (sehr schnell!)

  Das ist jetzt optimal! 🚀
```

## TypeScript images bauen und starten

[ ] Erstelle in deployment/docker-ts Dockerfiles und script zum bauen von 
- ../client/packages/engine im modus viewer (pnpm build:viewer)
- ../client/packages/engine im modus editor (pnpm build:editor)
- ../client/packages/controls
Es soll jeweils ein Package erstellt werden. Die TypeScript packages sollen als production builds gebaut werden.
- viewer port 3000
- editor port 3001
- controls port 3002
