
> Noch nicht final oder beschlossen!

# Umstellen des Edit Modus auf Common Editing

## Aktueller Stand

- Im player kann die session auf 'edit' gesetzt werden. 
- Dann prueft sie beim laden von chunks aus redis overlay daten zu jedem chunk
- Sind overlay daten vorhanden, werden diese mitgeladen und ueberlagern den chunk
- Overlaydaten sind an der session im redis gespeichert
- Der EditService schreibt die overlaydaten und sendet ein command an den player mit dem neuen Block, dieser wird an den client gesendet
- Beim 'speichern' werden die overlaydaten in den entsprechenden Layer geschrieben und DirtyChunk ausgeloest

- Ein client startet as editing indem es ein Layer blockt, wird (in redis?) vermerkt an der session
- beim speichern/disconnect werden die daten in den Layer geschrieben

## Common editing

- Alle sessions mit actor 'editor' laden immer overlaydaten aus dem redis - kein umschalten mehr noetig (prüfen)
- Overlay chunks werden direkt an der welt im redis gespeichert, nicht mehr an der session
- Beim laden eines chunks werden die overlaydaten aus dem redis geladen und ueberlagert
- Der EditService sendet die overlay block vie broadcast über redis an alle player, die diese welt aboniert haben,
- Die player pods senden den block an die client mit sessions, die den chunk registriert haben.

> Wie werden die daten in den Layer geschrieben? Zuordnung wenn ggf mehrere gleichzeitig editieren.


