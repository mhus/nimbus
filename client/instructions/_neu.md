
[x] Interaction mit NCP kann raus aus dem netzwerk, dafuer machen wir einen iframe auf.
- mitteilungen kann raus, dafuer ein client command, das vom server gesendet wird.
- client commands sollen vom server aufgerufen werden koennen
- block changed client-> server raus, dafuer schickt der client server commands.
- command mit data : any senden koennen
- command timeout nach 30s abbruch, aber nur bei inaktivitaet, wenn zwischendurch ne message kommt, dann timeout auf 0 setzen

- Connection Timeout im Server ist aktuell disabled, das muss noch repariert werden
- Texturen auch als base64:... angeben koennen (wird von babylon unterstuetzt!), nur kleine erlaubt
- Brauchen ein show status System: Leben, Gesundheit, Energie, Luft unter wasser... Die dann angezeigt werden koennen, hier Netzwerk Erweiterung oder Command?
- Brauchen Kompass System, in welche richtung ist was auf dem Kompass, via Command, da selten updates
- Brauchen Gruppe Info System (Gesundgeit + Name, Position), hier Netzwerk Erweiterung oder Command?, vermutlich Netzwerk

- Umstellen der '/' Taste von BlockEditor auf BlockAction, die wird als command zum server geschickt, hier ist eine Action
  hinterlegt, z.b. delet, create block xy, copy/select, paste, move to, open BlockEditor (edit), via View in nimbus_editors
  steuerbar.

- Status in BlockModifierMerge.ts richtig analysieren: WorldStatus und Season aus EnvironmentStatus holen
  - Bei season langsam durch eine raster funktion(x,y,z) von 0 - 100% auffuellen bis alle im neuen season sind.

- Neuer parameter 'wip' am Block soll bloecke im EDIT mode highlghten
- Parameter authConerHeights soll raus, dafuer immer auf shape == CUBE pruefen // kann evtl erweitert werden
- Sammeln von Bloecken in der gleichen gruppe, Selektieren von ganer gruppe - jedes mal mesh auf bauen und destroyen, aber nur wen interaktion aktiviert wird, damit wird performance gespart!
- Aktuelle selektierung schicker machen

- Reconnect nach disconnect im client

[x] Durchfallen durch welt beim start - safari

- Player daten in echt verwalten !!!

- Darstellung der shortcuts in der UI fehlt

- Teleport Screen und Musik

- Erlaubte Bewegungsmodi werden vom World-Server vorgegeben (walk, fly, sprint) - free_fly ist im EDITOR immer erlaubt. Unterscheidung free_fly und fly mit collision!

- ItemType Editor in controls
