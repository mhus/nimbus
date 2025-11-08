
- Interaction mit NCP kann raus aus dem netzwerk, dafuer machen wir einen iframe auf.
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

- Brauche einen pseudo Wall am chunk der an den raundern zu macht, damit die sonne nicht in den tunnel scheint.
    oder fuer weit weit weg. Ideal mit Alpha ausblendung oben.
  - name: backdrop
  - 4 x Array of BackdropItem(s)
  - backdrop: [n : Array<BackdropItem>,e : Array<BackdropItem>, s : Array<BackdropItem>, w : Array<BackdropItem>]
- BackdropItem: {
        typeId?, // a backdrop type id, that will be overwritten with following parameters
        la?,   // local x/z coordinate a (0-16) - start, default 0
        ya?,   // world y coordinate a - start, default 0
        lb?,   // local x/z coordinate b (0-16) - end, default 16
        yb?,   // world y coordinate b - end, default ya + 60
        texture? : string,
        color? : string,
        alpha? : number,
        alphaMode? : int
     }

z.b.
[
    [
        {
            la: 0,
            ya: 40,
            lb: 16,
            yb: 60,
            texture: "textures/backdrop/hills.png"
        }
    ],
    [
        {
        la: 0,
        ya: 40,
        lb: 16,
        yb: 60,
        texture: "textures/backdrop/hills.png"
        }
    ],
    [
        {
        la: 0,
        ya: 40,
        lb: 16,
        yb: 60,
        texture: "textures/backdrop/fog.png"
        }
    ],
    [
        {
        la: 0,
        ya: 40,
        lb: 16,
        yb: 60,
        texture: "textures/backdrop/fog.png"
        }
    ]
]

- Im Server eine rest api auf der backdropType heruntergeladen werden koennen.
  GET /api/backdrop/{id}
  - die backdropTypes werden im filesystem des servers im ordner files/backdrops/ gespeichert mit ihrer id als name, z.b. 1.json

- Die client 'engine' laed die packDrops lazy wenn sie angefordert werden und cache diese im
  BackdropService

