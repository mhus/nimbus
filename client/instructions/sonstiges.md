
## Anderes

[ ] Flag an Block ob man da durchlaufen kann
[x] Im login den Knopf 'Raster' entfernen, dafuer eien Reihe von Buttons: Gaston, Dilbert, Popeye, Godzilla nebeneinander,
- eine kann ausewählt werden.
- 'Gaston' ist default
- 'Gaston' ist terrain load 1, unload 2
- 'Dilbert' ist terrain load 2, unload 3
- 'Popeye' ist terrain load 3, unload 4
- 'Godzilla' ist terrain load 4, unload 5

[x] SkyBox
Implementiere in einer Datei SkyManager.ts eien SkyBox mit Babylon.
Beispiel:
```typescript

    //SKY BOX
    var skybox = BABYLON.MeshBuilder.CreateBox("skyBox", { size: 1000.0 }, scene);
    var skyboxMaterial = new BABYLON.StandardMaterial("skyBox", scene);
    skyboxMaterial.backFaceCulling = false;
    skyboxMaterial.reflectionTexture = new BABYLON.CubeTexture("textures/skybox", scene);
    skyboxMaterial.reflectionTexture.coordinatesMode = BABYLON.Texture.SKYBOX_MODE;
    skyboxMaterial.diffuseColor = skyboxMaterial.specularColor = new BABYLON.Color3(0, 0, 0);
    skybox.material = skyboxMaterial;
```
Und nutze.
