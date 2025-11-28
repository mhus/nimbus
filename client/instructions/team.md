
# Teams

Ein Team ist eine Gruppe von Benutzern, die zusammenarbeiten. Auch Gruppe genannt, aber in IT ist alles eine Gruppe, 
daher hier 'Team'.

## Datenstruktur

Ein Team hat einen Name, eine ID und Members. Es soll eine TeamData.ts datei in shared/types geben, die die Struktur beschreibt.

Ein TeamMember hat:
- playerId
- name (display name)
- icon: string (optional, asset path zu einem bild)
- status: 0 - disconnected, 1 - alive, 2 - dead - default ist erstmal 0
- position: Vector3 (optional wenn im spiel)
- health: number (optional, default 100)


[?] In instructions/general/network-model-2.0.md sind die Netzwerkmodelle beschrieben unter "Team Data (Server -> Client)"
und "Team Status (Server -> Client)"
- Erstelle das Team Datenmodell
- Erstelle das Tema Netzwerkmodell

## Service

[?] Erstelle einen TeamService im Server, der Teams verwaltet und Netzwerk Handler
- Erstelle einen TeamService der im AppContet registriert wird
- Erstelle Netzwerk Handler im NetworkService der die daten an den TeamService weiterleitet
- Es gibt immer nur ein Team in dem man ist.
- Vermerke die aktuelle TeamId und TeamName im TeamService und die liste der Members mit daten (TeamMember)

## Darstellung

[?] Erstelle in NotificationService eine Darstellung der Team Mitglieder
- team mitglieder werden links oben in einer Liste untereinander angezeigt
- Die anzeige soll nicht gross sein, da sie permanent angezeigt wird
- Es soll die moeglichkeit geben die anzeige auszublenden mit showTeamTable(show) - default ist true
- Als erstes ueber der Liste ist der Team Name
- Jedes Team Mitglied wird mit 
  - Oben: Icon (wenn vorhanden, oder default icon, kommt aus WorldInfo, oder einfach leer) mit Rahmen (farbe!) + Name,
  - darunter: Health Bar (rot) dargestellt
- Der Status steuert die darstellung: 
  - 0 - Icon Ramen schwarz
  - 1 - Icon Rahmen gruen, health unter 10: rot
  - 2 - Icon Rahmen grau - name auch grau
- Team-Liste kann mit einem Mal uebergeben werden setTeam(teamName: string, members: TeamMember[])
- Team Mitglieder koennen separat aktualisiert werden updateTeamMember(member: TeamMember)

[?] Der TeamService steuert den NotificationService um die Anzeige zu aktualisieren
- Wenn sich der status aendert wird der NotificationService informiert

[?] Erstelle Commands in engine mit denen der NotificationService mit den Teams direkt angesprochen werden kann

[?] Erstelle Commands im test_server mit denen der test server die network messages an den client sendet (zum testen, einfach ein loop command)

```text
Ich habe das Team-System erfolgreich implementiert:

  ✅ Erstellt:

  Shared (Types & Messages):
  - TeamData.ts mit TeamData, TeamMember und TeamStatusUpdate Interfaces
  - MessageTypes erweitert mit TEAM_DATA und TEAM_STATUS

  Engine (Client):
  - TeamService.ts - Verwaltet Team-Daten und koordiniert mit NotificationService
  - TeamDataMessageHandler.ts - Verarbeitet komplette Team-Daten vom Server
  - TeamStatusMessageHandler.ts - Verarbeitet Team-Status-Updates
  - ShowTeamCommand.ts - Command zum Ein-/Ausblenden der Team-Tabelle
  - NotificationService erweitert mit Team-UI-Methoden:
    - showTeamTable() - Zeigt/versteckt Team-Tabelle
    - setTeam() - Setzt komplettes Team
    - updateTeamMember() - Updated einzelnen Member
    - Status-Farben: disconnected=schwarz, alive=grün, low health (<10)=rot, dead=grau

  Test Server:
  - TeamDataCommand.ts - Sendet Team-Daten an Client
  - TeamStatusCommand.ts - Sendet Team-Status-Updates

  ✅ Registriert:

  - TeamService im AppContext und NimbusClient
  - Beide Message Handler im NetworkService
  - ShowTeamCommand im CommandService
  - Beide Server Commands im NimbusServer

  ✅ Builds erfolgreich:

  - Shared package compiled ✓
  - Engine package type check erfolgreich ✓
  - Engine build:editor erfolgreich ✓
  - Test Server type check erfolgreich ✓
  - Test Server build erfolgreich ✓

  Testing Commands:

  # Server: Team erstellen
  teamData RedTeam Player1 Player2 Player3

  # Server: Status updates
  teamStatus team_<id> player_0 50 1    # Player1 health=50, alive
  teamStatus team_<id> player_1 0 2     # Player2 dead
  teamStatus team_<id> player_2 5 1     # Player3 health=5 (wird rot)

  # Client: Team anzeigen/verstecken
  showTeam true/false

  Das System ist bereit zum Testen! 🎉
```
