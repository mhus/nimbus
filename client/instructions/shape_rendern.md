
# Shapes Rendering

## Cube

> Habe ich manuell mit hilfe con claude angepasst. Struktur ist nun sauber und
> kann erweiytert werden auf weitere renderer.

[x] Erstelle unit tests mit BabylonJS und NullEngine fuer CubeRenderer.
- Nutze nur den CubeRenderer, render einen Cube indem du ihn mit speziell gefertigten konfigurationen erstellst.
- Natuerlich wird auch der MaterialService und TextureService benoetigt.
- Teste ob der Cube korrekt gerendert wird.
- Teste ob faceVisibility korrekt funktionieren.
[x] Erstelle noch einen test fuer 'offsets'
[ ] Kannst du einen test fuer backFaceCulling erstellen (nicht sehen auf der back side) ?

## Cross

[ ] Erstelle einen CrossRenderer der ein Cross Rendert, auf dem die Texturen sind.
- Ein Cross sind zwei Flaechen, die diagonal, stehend auf einem Block angebracht sind.
- Implementiere alle eigenschften, offset, scaling, rotation wie bei einem Cube.
- Orientiere dich an CubeRenderer.

## Sphere

[ ] Erstelle den SphereRenderer. Er rendert einen Sphere.
- Orientiere dich an CubeRenderer.
- Implementiere alle eigenschaften, offset, scaling, rotation wie bei einem Cube.
- Offsets:
  - 0: Radius Offset
  - 1: Displacement

## Cylinder

[ ] Nenne shere type 'Column' um in 'Cylinder' in BlockModifier und in den Editor controls
[ ] Erstelle den CylinderRenderer. Er rendert einen Cylinder.
- Orientiere dich an CubeRenderer.
- Implementiere alle eigenschaften, offset, scaling, rotation wie bei einem Cube.
- Offsets:
  - 0: Radius Top
  - 1: Radius Bottom
  - 2: Displacement Top (incl Y)
  - 3: Displacement Bottom (incl Y)


