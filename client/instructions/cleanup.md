
## Cleanup

[ ] Gerade ziehen von Metadaten: an Block-Typen und Instanzen
> Wie genau ist die Struktur?
> BlockMaterial genauer ansehen. Typ 'barrier' weg, soll hier 'flame' oder 'lightning' eingebaut werden oder als shape? Oder beides?
> Was macht eigentlich BlockMaterial 'gas' ???
> Es muessen nicht alle Kombinationen funktionieren, z.b. BlockMaterial gas mit Wind.
> ToolType muss weg.


[x] Custom Testures: wird in assetPath eine Textur angegeben, dann wird diese verwendet. Mehrere Texturen können mit komma getrennt eingegeben werden (Array)
- Custom Texturen wirken auf die meisten Shapes, ausser Model, Flame, Water, Lava.
