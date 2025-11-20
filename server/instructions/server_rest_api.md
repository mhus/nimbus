
# Rest API aller server

Die server REST API soll so afgebaut werden, dass sie sich nicht ueberschneiden und durch Filter die separaten
Akteure getrennt werden.

## Universe

Universe Server bekommt den einstiegspunkt '/uiap'

Folgende Akteure und ihre Basispfade:

- '/universe/user' - Zugriff des Users, wird durch den UserSeecurityFilter geschuetzt
- '/universe/Region' - Zugriff durch den Region Server, wird durch den RegionServerSecurityFilter geschuetzt
- '/universe/world' - Zugriff durch den World Server, wird durch den WorldServerSecurityFilter geschuetzt

Alle Internen Entitäten und Server beginnen mit dem prefix U, also
- UserSeecurityFilter -> UUserSeecurityFilter
- RegionServerSecurityFilter -> URegionServerSecurityFilter
- WorldServerSecurityFilter -> UWorldServerSecurityFilter
- User Entiry -> UUser
- UserService -> UUserService
- ...

## Region

Region Server bekommt den einstiegspunkt '/qapi'

Folgende Akteure und ihre Basispfade:

- '/Region/world' - Zugriff durch den World Server, wird durch den WorldServerSecurityFilter geschuetzt
- '/Region/universe' - Zugriff durch den Universe Server, wird durch den UniverseServerSecurityFilter geschuetzt
- '/Region/user' - Zugriff durch den User Server, wird durch den UserServerSecurityFilter geschuetzt

Alle Internen Entitäten und Server beginnen mit dem prefix Q.

## World

World Server bekommt den einstiegspunkt '/wapi'

Folgende Akteure und ihre Basispfade:

- '/world/universe' - Zugriff durch den Universe Server, wird durch den UniverseServerSecurityFilter geschuetzt
- '/world/user' - Zugriff durch den User Server, wird durch den UserServerSecurityFilter geschuetzt
- '/world/Region' - Zugriff durch den Region Server, wird durch den RegionServerSecurityFilter geschuetzt

Alle Internen Entitäten und Server beginnen mit dem prefix W.