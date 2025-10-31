
- Interaction mit NCP kann raus aus dem netzwerk, dafuer machen wir einen iframe auf.
- mitteilugnen kann raus, dafuer ein client command, das vom server gesendet wird.
- client commands sollen vom server aufgerufen werden koennen
- block changed client-> server raus, dafuer schickt der client server commands.
- command mit data : any senden koennen
- command timeout nach 30s abbruch, aber nur bei inaktivitaet, wenn zwischendurch ne message kommt, dann timeout auf 0 setzen
