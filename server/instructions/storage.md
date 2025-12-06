
# Storage

[x] Erstelle einen MongoStorageService, der Daten in MongoDB speicert.
- Die daten muessen in chunks geteilt werden, ein chunk ist immer 512KB gross. (Konfigurierbar)?
- Es wir deine StorageData Entity genoetigt. 
  - id: ObjectId (automatisch generiert) uuid
  - path: string (eindeutiger Pfad)
  - index: int (index des chunks, beginnend bei 0)
  - data: byte[] (die daten des chunks)
  - final: boolean (gibt an, ob es der letzte chunk ist)
  - size: long (die gesamte groesse der daten, nur im letzten chunk gesetzt)
  - createdAt: Date (erstellungsdatum)
- Es wird pro chunk immer ein StorageData gespeichert. der letzte bekommt das flag final=true und size=<gesamte groesse>
- uuid + index bilden den Primärschlüssel.
- Wenn info geladen wird, dann wird uuid + index + final = true geladen, um die gesamte groesse zu bekommen.
- id wird als storage.id zurueckgegeben.
- warum id: path kann mehrfach verwendet werden, es sind quasi versionen der gleichen datei und die id ist die referenz auf die version.
  Dadurch sind zwischen updates keine luecken moeglich. Alte versionen werden spaeter automatsich geloescht.
- Es wird eine StorageDelete Entity benoetigt:
  - storageId: ObjectId (referenziert die StorageData id)
  - deletedAt: Date (loeschdatum)
- Bei delete und update wird fuer die alte version ein StorageDelete erstellt mit deletAt = jetzt + 5 Minuten.
- Es wird ein scheduler benoetigt, der alle StorageDelete eintraege durchgeht und die referenzierten StorageData loescht. Und die StorageDelete eintraege danach loescht.
- FileStorageService wird deaktiviert
- Es werden Stream implementierungen benoetigt, die die chunks automatisch laden und speichern.
- Wichtig ist, dass die daten nicht in den speicher passen, deshalb muessen die streams die chunks nacheinander laden und speichern.

> [x] Storage with worldId
