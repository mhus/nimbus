
# Rest API aller server

Die server REST API soll so afgebaut werden, dass sie sich nicht ueberschneiden und durch Filter die separaten
Akteure getrennt werden.

## Universe

Universe Server bekommt den einstiegspunkt '/uiap'

Folgende Akteure und ihre Basispfade:

- '/universe/user' - Zugriff des Users, wird durch den UserSeecurityFilter geschuetzt
- '/universe/quadrant' - Zugriff durch den Quadrant Server, wird durch den QuadrantServerSecurityFilter geschuetzt
- '/universe/world' - Zugriff durch den World Server, wird durch den WorldServerSecurityFilter geschuetzt

Alle Internen Entitäten und Server beginnen mit dem prefix U, also
- UserSeecurityFilter -> UUserSeecurityFilter
- QuadrantServerSecurityFilter -> UQuadrantServerSecurityFilter
- WorldServerSecurityFilter -> UWorldServerSecurityFilter
- User Entiry -> UUser
- UserService -> UUserService
- ...

## Quadrant

Quadrant Server bekommt den einstiegspunkt '/qapi'

Folgende Akteure und ihre Basispfade:

- '/quadrant/world' - Zugriff durch den World Server, wird durch den WorldServerSecurityFilter geschuetzt
- '/quadrant/universe' - Zugriff durch den Universe Server, wird durch den UniverseServerSecurityFilter geschuetzt
- '/quadrant/user' - Zugriff durch den User Server, wird durch den UserServerSecurityFilter geschuetzt

Alle Internen Entitäten und Server beginnen mit dem prefix Q.

## World

World Server bekommt den einstiegspunkt '/wapi'

Folgende Akteure und ihre Basispfade:

- '/world/universe' - Zugriff durch den Universe Server, wird durch den UniverseServerSecurityFilter geschuetzt
- '/world/user' - Zugriff durch den User Server, wird durch den UserServerSecurityFilter geschuetzt
- '/world/quadrant' - Zugriff durch den Quadrant Server, wird durch den QuadrantServerSecurityFilter geschuetzt

Alle Internen Entitäten und Server beginnen mit dem prefix W.