
# Aufsetzen eines neuen Systems

## Anlegen der Daten

- Beim Starten des Universe wird ein neue admin user angelegt, password im log file
- Anlegen eines neuen users 'wadewatts' - 'Wade Watts'
- Anlegen einer neuen Region 'oasis'
- Anlegen einer neuen Welt 'middletown' in der region oasis
- Anlegen des Characters 'parzival' in der Region oasis fuer den user wadewatts

User:
- Samantha Evelyn Cook - 'samanthaevelyncook' - password: 'password1'
- Wade Watts - 'wadewatts' - password: 'password1'
- Helen Harris - 'helenharris' - password: 'password1'
- Toshiro Yoshiaki - 'toshiroyoshiaki' - password: 'password1'
- Akihide Karatsu - 'akihidekaratsu' - password: 'password1'

Charactere in oasis:
- samanthaevelyncook: art3mis
- wadewatts: parzival
- helenharris: aech
- toshiroyoshiaki: daito
- akihidekaratsu: shoto

Welten in oasis:
- middletown
- planet-doom
- distracted-globe
- racetrack
- ludus


## Wie das geht

- Frischer Start universum Server: 
  - erzeugt univrsum KeyPair und speichert den publik key in confidential/universePublicKey.txt
  - erzeugt admin user und speichert das passwort in confidential/universum.txt 
- Frischer Start Region Server: 
  - erzeugt ein Region Server public KeyPair und speichert den publik key in confidential/regionPublicKey.txt
- Login mit admin am universe server -> universe action token
- Mit action token den region server public key im universum importieren
- Den universum public key im region server importieren (geht ohne token solange der key noch nicht importiert ist)
- Mit universe aktion token am region server eine neue region anlegen
  - region server legt region intern an
  - region server erzugt region KeyPair
  - region server sendet request an universum incl. publikKey
  - universum legt region und key als region key an
