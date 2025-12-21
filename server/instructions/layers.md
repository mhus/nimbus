
# Layers

Es gibt schon Layers, aber die sollen nochmal neu arrangiert werden.

Beim createn von chunks sollen nur noch TerrainLayer (WLayerTerrain) beruecksichtigt werden. Hier geht es um
resourcen und performance. Da TerrainLayer chunk orientiert sind, ist es so einfacher.

TerrainLayers, die das Flag ground haben, keonnen direkt bearbeitet werden. Andere TerrainLayer
sind Model based Layers, d.h. sie werden durch ModelLayers gefüllt. d.h. ModelLayers werden einem
TerrainLayer zugeordnet. Die TerrainLayer sollten nicht direkt bearbeitet werden, da sie
jederzeit neu berechnet werden keonnen.

Lösung:

Aktuell gibt es in WLayer einen Typ und eine layerDataId die im WLayerTerrain und WLayerModel referenziert wird.
Zukünftig gibt es einen TYPE "GROUND" und einen "MODEL". Für beide gibt es genau einen WLayerTerrain Layer.
Für MODEL gibt es noch viele weitere WLayerModel Layer. Alle auf der gleichen layerDataId referenziert.

## Model Anpassen



## Aufrauemen

