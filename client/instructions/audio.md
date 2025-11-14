
# Audio

## Model und Service

[?] Im client in der date BlockModifier.ts ist ein 'soundModifier', nenne ihn um in 'audioModifier'.
- audioModifier hat eine liste von audio dateien die erweitert werden kann. (Array) Als Typ wird ein AudioDefinition interface verwendet.
- Das feld im BlockModifier ist nun 'audio' nicht mehr 'sound'.
- AudioDefinition interface hat folgende Attribute:
  - type: string (typ der audio datei: 'steps' - kommen spaeter mehr dazu, kann mehrfach der gleiche type vorkommen)
  - path: string (pfad zur audio datei)
  - volume: number (lautstärke der audio datei, standardwert 1.0)
  - loop: boolean (ob die audio datei in einer schleife abgespielt (optional wenn sinnvoll, standardwert false)
  - enabled: boolean (ob die audio datei aktiviert ist, standardwert true)
- Passe den SoundEditor im package controls an. Der soll nun AudioEditor heissen.
  - Es soll ein 'Add Audio' button geben, der eine neue audio datei hinzufuegt.
  - Siehe dazu auch 'Add Texture' button im TextureEditor als vorlage.

[?] Asset Editor: Wenn im Asset Editor eine sound datei (ogg, mp3) im Preview angezeigt wird, soll dort ein kleines audio abspiel symbol angezeigt werden.
- Beim klicken auf das symbol soll die audio datei abgespielt werden.

[?] Erweitere die Selektion von Assets so, dass nur audio dateien (ogg, mp3) ausgewaehlt werden koennen, wenn im AudioEditor eine neue audio datei hinzugefuegt wird.
- Erweitere das auch fuer TextureEditor (bisher nur png, jpg).
- Erweitere das auch fuer ModelEditor (bisher nur glb, gltf, babylon).

[?] Erstelle einen AudioService der im AppContext registriert wird. Er ist dafuer zuständig AudioDateien zu laden, cachen und teilweise zum abzuspielen.
- Er benutzt den NetworkService um audio dateien zu laden (getAssetUrl()).
- Die Dateien werden als BABYLON.Sound Objekte geladen und gecached und zurueckgegeben.abgespielt werden.

## Integration in die App

[?] Wenn an einem Block audio dateien an einem Block sind, sollen diese beim erstellen des Chunks im ChunkService vom AudioService geladen und
an den Block vermerkt werden. In ClientBlock wird ein neues feld 'auioSteps' mit allen Audio Objekten vom typ 'steps' hinzugefuegt. Dabei wird in einer 
Struktur gespeichert referenz auf AudioDefinition und auf den BABYLON.Sound.

[ ] Im PhysicService wird beim bewegen des Spielers ein event 'onStepOver' ausgelöst, AUF welchem block (ClientBlock)) sich 
der Spieler bewegt (Falls es nicht AIR ist) und mit welchem movementType (laufen, springen, etc) und der id der entity.
- Das event wird nur ausgeloest wenn sich der spieler bewegt (also nicht steht) und nur alle 300ms (damit nicht zu viele events kommen beim laufen)
- Im SoundService wird das event gefangen und geprueft ob der ClientBlock Audio dateien in audioSteps hat.
- Falls ja, wird eine zufaellige audio datei aus der liste der 'audioSteps' audio dateien abgespielt. - Am punkt des Blocks, 
  spacial und mit dem volumen das in der AudioDefinition definiert ist.
- Im event wird auch die entityId mitgegeben, damit kann verhindert werden, das fuer den gleichen player zwei steps gleichzeitig gespielt werden.
  Vermerke dir, solange der step sound läuft die entityId, und spiele keinen neuen step sound fuer die gleiche entityId ab.
