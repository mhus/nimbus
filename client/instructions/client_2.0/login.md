
# Login Seite

Auf der Login Seite loggt sich der User ein.

- Login ist nicht trivial, da 2FA oder SSO verwendet werden kann.
- Ausserdem werden Mechanismen für Code-Login und DeepLink angeboten.

"Passwort vergessen" und "Account erstellen" werden auf einer anderen Seite angeboten,
sind nicht teil von Login.

Der User bekommt ein Session-Cookie.
Er kann sich ein JWT-Token erteilen lassen, das token ist nur kurzlebig. ca. 15 Minuten.
Es kann erneuert werden.

Mit dem Token kann sich der User am 3D Server anmelden. Der Server ist auf einer anderen Domain 
als die Login Seite. Der Server prüft des Token bei jeder Anfrage.

# Start Seite

Nach dem Login kommt der User auf die Start oder Absprung Seite.
Es wird via REST eine Liste von Systemen abgefragt auf die der User Zugriff hat.
Der user kann neue Systemen anfragen, dabei wird der System-Server gefragt ob der User Zugriff hat.

- FORBIDDEN - Kein Zugriff
- NOT FOUND - Unbekannter User, wird eine URL zum Anmelden geliefert.
- OK - Zugriff erlaubt

Manche Systemen, die in enem Verbund sind, legen den User automatisch an.

Der User hat also die Auswahl von Systemen. Er wählt einen aus.
Nun wird der Server des Systemen gefragt welche Welten fuer den User bereit stehen.

- Evtl macht man hier eine Map...
- Es gibt auch Welt-Branches auf die der User zugriff hat
- Rolle mitgeben: Owner, Editor, Player (keine -> wird nicht mitgegeben)

Welt
- weltId
- name
- beschreibung
- weltBild ? Oder gesamtBild mit Map?
- Branch: main, branchname
- Rolle: Owner, Editor, Player
- groupId: Zu welcher gruppe gehoert die welt (string)
- acceptItemsgroups: Liste von group Ids von denen die Welt Items erlaubt (array of string) oder '*'
- worldSignPublicKey
- groupSignPublicKey
- signedGroupSign
- signedWorldSign


Und dann wird der NimbusViewer oder NimbusEditor gestartet (Option kann gewählt werden wenn Player Rolle Editor oder Owner hat).
Oder Asset Editor ...

## Rollen

An einer Welt haengt fuer jede rolle eine Liste von Usern die zugriff haben, oder "*" Dann ist jeder User zugriff der 
im Systemen registriert ist.

Owner (und Editor ?) kann nicht '*' sein.
Wenn der letzte Owner geloescht wurde, wird die Welt automatisch geloescht. (?)
Diese Logik kann jeder System selbst definieren.

## Branch

Das kann jeder System selbst definieren. Grundlegend sollte es eine WriteOnUpdate Branch vom 'main'. Es sit aber ein
eindeutige, andere WeltId, meist mit branch in der Id, z.B. 'kakatanien' (main), 'kakatanien-dev' (dev branch).

