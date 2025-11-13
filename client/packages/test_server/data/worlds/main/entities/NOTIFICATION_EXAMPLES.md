# Entity Notification Examples

Diese Datei beschreibt die Test-Entities für die neuen Notification-Features:
- **notifyOnCollision**: Sendet Event an Server bei Player-Kollision
- **notifyOnAttentionRange**: Sendet Event an Server wenn Player in Range kommt

## Test-Entities

### 1. guard1.json - Patrol Guard
**Position**: (30, 64, 30)
**Features**:
- `notifyOnCollision: true` - Sendet Kollisions-Event
- `notifyOnAttentionRange: 15` - Wird aufmerksam bei 15 Blöcken Distanz
- `solid: true` - Player kann nicht durchlaufen
- `speed: 1.0` - Patroulliert langsam in 20 Block Radius

**Test-Szenario**:
1. Nähern Sie sich dem Guard auf 15 Blöcke → Proximity-Event im Server-Log
2. Kollidieren Sie mit dem Guard → Collision-Event im Server-Log
3. Schleichen (Crouch) reduziert Attention Range (wenn distanceNotifyReductionCrouch gesetzt)

**Erwartete Server-Logs**:
```
[INFO] Entity interaction received: action=entityProximity, entityId=guard1
[INFO] Entity interaction received: action=entityCollision, entityId=guard1
```

---

### 2. sentinel1.json - Static Sentinel
**Position**: (-20, 64, -20)
**Features**:
- `notifyOnCollision: true` - Sendet Kollisions-Event
- `notifyOnAttentionRange: 25` - Große Attention Range (25 Blöcke)
- `solid: true` - Blockiert Player
- `speed: 0.5` - Bewegt sich sehr langsam in kleinem 5 Block Radius

**Test-Szenario**:
1. Nähern Sie sich von weiter Distanz → Proximity-Event bei 25 Blöcken
2. Betreten Sie den Bereich mehrmals → Event nur beim ersten Mal (Enter)
3. Verlassen und erneut betreten → Event erneut gesendet

**Besonderheit**: Größere Attention Range testet Distanz-Berechnungen über längere Distanzen

---

### 3. trap1.json - Proximity Trap
**Position**: (50, 64, 50)
**Features**:
- `notifyOnCollision: true` - Sendet Event bei Berührung
- `notifyOnAttentionRange: 5` - Kleine Range (5 Blöcke)
- `solid: false` - Player kann durchlaufen (!)
- `speed: 0` - Statisch, bewegt sich nicht

**Test-Szenario**:
1. Nähern Sie sich auf 5 Blöcke → Proximity-Event (Falle aktiviert sich)
2. Laufen Sie durch die Entity → Collision-Event (Falle löst aus)
3. Da `solid: false` wird Player nicht blockiert

**Use Case**: Simuliert unsichtbare Trigger-Zonen oder Fallen

---

## Player Stealth-Modifiers

Die Player-Properties können die Attention Range beeinflussen:

```typescript
// In PlayerInfo oder via Modifiers
distanceNotifyReductionWalk: 0     // Keine Reduktion beim Gehen
distanceNotifyReductionCrouch: -5  // 5 Blöcke weniger Range beim Schleichen
```

**Effektive Range Berechnung**:
```
effectiveRange = notifyOnAttentionRange + (isCrouching ? distanceNotifyReductionCrouch : distanceNotifyReductionWalk)
```

**Beispiel mit guard1**:
- Normal: 15 + 0 = 15 Blöcke
- Crouched: 15 + (-5) = 10 Blöcke

## Testing

### Server-Side Logs prüfen:
```bash
pnpm dev:test_server
```

Achten Sie auf:
```
[INFO] Entity interaction received {
  sessionId: "...",
  username: "testuser",
  entityId: "guard1",
  action: "entityProximity",
  timestamp: ...,
  params: {
    distance: 14.5,
    effectiveRange: 15,
    entered: true
  }
}
```

### Client-Side Logs prüfen:
Im Browser-Console (F12):
```
[EntityService] Entity proximity notification sent (entered range)
[EntityService] Entity collision notification sent
```

## Anpassungen für Game-Logic

Der Server-Handler in `NimbusServer.ts` ist aktuell nur ein Logger:

```typescript
private handleEntityInteraction(session, messageId, data) {
  // TODO: Implement game logic
  // - 'entityCollision': Trigger damage, bounce, etc.
  // - 'entityProximity': NPC becomes alert, initiates dialog, etc.
}
```

**Nächste Schritte**:
1. Dialog-System bei Proximity
2. Combat-System bei Collision
3. Quest-Trigger bei Proximity
4. Trap-Activation bei Collision
