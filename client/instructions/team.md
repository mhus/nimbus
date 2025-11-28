
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


[ ] In instructions/general/network-model-2.0.md sind die Netzwerkmodelle beschrieben unter "Team Data (Server -> Client)"
und "Team Status (Server -> Client)"
- Erstelle das Team Datenmodell
- Erstelle das Tema Netzwerkmodell

## Service

[ ] Erstelle einen TeamService im Server, der Teams verwaltet und Netzwerk Handler
- Erstelle einen TeamService der im AppContet registriert wird
- Erstelle Netzwerk Handler im NetworkService der die daten an den TeamService weiterleitet
- Es gibt immer nur ein Team in dem man ist.
- Vermerke die aktuelle TeamId und TeamName im TeamService und die liste der Members mit daten (TeamMember)

## Darstellung

[ ] Erstelle in NotificationService eine Darstellung der Team Mitglieder
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

[ ] Der TeamService steuert den NotificationService um die Anzeige zu aktualisieren
- Wenn sich der status aendert wird der NotificationService informiert

[ ] Erstelle Commands in engine mit denen der NotificationService mit den Teams direkt angesprochen werden kann

[ ] Erstelle Commands imtest_Server mit denen der test server die network messages an den client sendet (zum testen, einfach ein loop command)

