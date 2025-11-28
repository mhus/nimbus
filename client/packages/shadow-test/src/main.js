import * as BABYLON from '@babylonjs/core';

// Get canvas
const canvas = document.getElementById('renderCanvas');

// Create engine
const engine = new BABYLON.Engine(canvas, true, {
    preserveDrawingBuffer: true,
    stencil: true,
    antialias: false,
    powerPreference: 'high-performance',
}, true);

console.log('BabylonJS Version:', BABYLON.Engine.Version);
console.log('WebGL Version:', engine.webGLVersion);
console.log('Is WebGL2:', engine.isWebGL2);

// Create scene - EXACT copy from working playground example
function createScene() {
    const scene = new BABYLON.Scene(engine);

    // Light direction (same as working example)
    const dir = new BABYLON.Vector3(1, -2, 1);
    const light = new BABYLON.DirectionalLight("DirectionalLight", dir, scene);

    // Create CascadedShadowGenerator (same as working example)
    const shadow = new BABYLON.CascadedShadowGenerator(512, light);

    light.intensity = 1;

    // Shadow settings (same as working example)
    shadow.lambda = 0.2;
    shadow.filter = 0;
    shadow.numCascades = 2;

    // Create ground (same as working example)
    const ground = BABYLON.Mesh.CreateGround("ground", 8192*2, 8192*2, 10, scene);
    ground.position.y = -400;
    ground.position.z = 4096;
    ground.receiveShadows = true;

    const matGround = new BABYLON.StandardMaterial("matGround", scene);
    matGround.diffuseColor = new BABYLON.Color3(0.8, 0.8, 0.8); // Light gray
    ground.material = matGround;

    // Create boxes (same as working example)
    const matBox = new BABYLON.StandardMaterial("matBox", scene);
    matBox.diffuseColor = new BABYLON.Color3(0.6, 0.4, 0.2); // Brown

    for (let i = 0; i < 2; i++) {
        const box = BABYLON.MeshBuilder.CreateBox("box" + i, {
            width: 40,
            depth: 40,
            height: 800,
        }, scene);

        // IMPORTANT: Use push to renderList (not addShadowCaster)
        shadow.getShadowMap().renderList.push(box);

        box.material = matBox;
        box.receiveShadows = false;
        box.position.z = i * 512 * 14;
    }

    // Create camera (same as working example)
    scene.createDefaultCamera(false, true, true);
    scene.activeCamera.maxZ = shadow.shadowMaxZ = 15000;
    scene.activeCamera.angularSensibility *= 0.75;
    scene.activeCamera.speed = 30;
    scene.activeCamera.position = new BABYLON.Vector3(300, 1000, -1000);
    scene.activeCamera.setTarget(new BABYLON.Vector3(0, 0, 1000));

    // Call splitFrustum (same as working example)
    shadow.splitFrustum();

    console.log('Scene created');
    console.log('Shadow generator:', shadow);
    console.log('Shadow map size:', shadow.getShadowMap().getSize());
    console.log('Shadow casters:', shadow.getShadowMap().renderList.length);
    console.log('Ground receiveShadows:', ground.receiveShadows);

    return scene;
}

// Create the scene
const scene = createScene();

// Render loop
engine.runRenderLoop(() => {
    scene.render();
});

// Handle window resize
window.addEventListener('resize', () => {
    engine.resize();
});
