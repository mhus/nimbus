/**
 * Main Menu GUI
 */

import { AdvancedDynamicTexture, Rectangle, TextBlock, Button, InputText, StackPanel, Control, Image, Checkbox } from '@babylonjs/gui';
import { Scene, MeshBuilder, StandardMaterial, Texture, Vector3 } from '@babylonjs/core';

export interface ServerInfo {
  name: string;
  address: string;
  port: number;
  wireframeMode: boolean;
  renderDistance: number;
  unloadDistance: number;
  world?: string;  // Selected world name
}

export interface WorldInfo {
  name: string;
  loaded: boolean;
}

/**
 * Main Menu with server selection
 */
export class MainMenu {
  private guiTexture: AdvancedDynamicTexture;
  private mainContainer?: Rectangle;
  private onConnect?: (server: ServerInfo) => void;
  private scene: Scene;
  private testCube?: any;
  private selectedPreset: 'gaston' | 'dilbert' | 'popeye' | 'godzilla' = 'gaston';
  private availableWorlds: WorldInfo[] = [];
  private selectedWorld?: string;

  constructor(scene: Scene) {
    this.scene = scene;
    // Create fullscreen GUI
    this.guiTexture = AdvancedDynamicTexture.CreateFullscreenUI('MainMenuUI', true, scene);

    // Create test cube with grass texture
    this.createTestCube();
  }

  /**
   * Create test cube with grass texture
   */
  private createTestCube(): void {
    console.log('[MainMenu] Creating test cube with grass texture');

    // Create a simple box - positioned in camera view
    // Camera is at (0, 80, 0) looking at (10, 79, 10)
    const box = MeshBuilder.CreateBox('testCube', { size: 4 }, this.scene);
    box.position = new Vector3(5, 79, 5); // Halfway between camera position and target

    // Create material with grass texture
    const material = new StandardMaterial('testMaterial', this.scene);
    material.diffuseTexture = new Texture('http://localhost:3004/assets/textures/block/grass.png', this.scene);

    box.material = material;
    this.testCube = box;

    // Animate rotation
    this.scene.registerBeforeRender(() => {
      if (this.testCube) {
        this.testCube.rotation.y += 0.01;
        this.testCube.rotation.x += 0.005;
      }
    });

    console.log('[MainMenu] Test cube created at position:', box.position);
  }

  /**
   * Fetch available worlds from server
   */
  private async fetchWorlds(address: string, port: number): Promise<WorldInfo[]> {
    try {
      // Calculate HTTP port (WebSocket port + 1)
      const httpPort = port + 1;
      const response = await fetch(`http://${address}:${httpPort}/worlds`);

      if (!response.ok) {
        console.warn('[MainMenu] Failed to fetch worlds:', response.statusText);
        return [];
      }

      const data = await response.json();
      return data.worlds || [];
    } catch (error) {
      console.warn('[MainMenu] Error fetching worlds:', error);
      return [];
    }
  }

  /**
   * Show the main menu
   */
  async show(onConnect: (server: ServerInfo) => void, preselectedServerInfo?: ServerInfo): Promise<void> {
    this.onConnect = onConnect;

    // Use preselected world if provided
    if (preselectedServerInfo?.world) {
      this.selectedWorld = preselectedServerInfo.world;
    }

    // Main container
    this.mainContainer = new Rectangle('mainContainer');
    this.mainContainer.width = '400px';
    this.mainContainer.height = '650px';
    this.mainContainer.thickness = 2;
    this.mainContainer.cornerRadius = 10;
    this.mainContainer.background = '#1a1a1aee';
    this.mainContainer.color = '#ffffff';

    // Stack for vertical layout
    const stack = new StackPanel('stack');
    stack.width = '100%';
    stack.height = '100%';
    stack.paddingTop = '20px';
    stack.paddingBottom = '20px';

    // Logo
    const logo = new Image('logo', '/textures/logo.png');
    logo.width = '120px';
    logo.height = '120px';
    logo.paddingBottom = '10px';

    // Title
    const title = new TextBlock('title', 'VoxelSrv');
    title.height = '40px';
    title.fontSize = 32;
    title.color = 'white';
    title.fontFamily = 'Arial, sans-serif';
    title.fontWeight = 'bold';
    title.paddingBottom = '5px';

    // Subtitle
    const subtitle = new TextBlock('subtitle', 'TypeScript Edition');
    subtitle.height = '30px';
    subtitle.fontSize = 16;
    subtitle.color = '#aaaaaa';
    subtitle.paddingBottom = '30px';

    // Server Name Label
    const nameLabel = new TextBlock('nameLabel', 'Server Name:');
    nameLabel.height = '25px';
    nameLabel.fontSize = 14;
    nameLabel.color = 'white';
    nameLabel.textHorizontalAlignment = Control.HORIZONTAL_ALIGNMENT_LEFT;
    nameLabel.paddingLeft = '40px';
    nameLabel.paddingBottom = '5px';

    // Server Name Input
    const nameInput = new InputText('nameInput');
    nameInput.width = '320px';
    nameInput.height = '40px';
    nameInput.fontSize = 14;
    nameInput.color = 'white';
    nameInput.background = '#333333';
    nameInput.placeholderText = 'My Server';
    nameInput.text = preselectedServerInfo?.name || 'Local Server';
    nameInput.thickness = 1;
    nameInput.paddingBottom = '15px';

    // Server Address Label
    const addressLabel = new TextBlock('addressLabel', 'Server Address:');
    addressLabel.height = '25px';
    addressLabel.fontSize = 14;
    addressLabel.color = 'white';
    addressLabel.textHorizontalAlignment = Control.HORIZONTAL_ALIGNMENT_LEFT;
    addressLabel.paddingLeft = '40px';
    addressLabel.paddingBottom = '5px';

    // Server Address Input
    const addressInput = new InputText('addressInput');
    addressInput.width = '320px';
    addressInput.height = '40px';
    addressInput.fontSize = 14;
    addressInput.color = 'white';
    addressInput.background = '#333333';
    addressInput.placeholderText = 'localhost';
    addressInput.text = preselectedServerInfo?.address || 'localhost';
    addressInput.thickness = 1;
    addressInput.paddingBottom = '15px';

    // Port Label
    const portLabel = new TextBlock('portLabel', 'Port:');
    portLabel.height = '25px';
    portLabel.fontSize = 14;
    portLabel.color = 'white';
    portLabel.textHorizontalAlignment = Control.HORIZONTAL_ALIGNMENT_LEFT;
    portLabel.paddingLeft = '40px';
    portLabel.paddingBottom = '5px';

    // Port Input
    const portInput = new InputText('portInput');
    portInput.width = '320px';
    portInput.height = '40px';
    portInput.fontSize = 14;
    portInput.color = 'white';
    portInput.background = '#333333';
    portInput.placeholderText = '3003';
    portInput.text = preselectedServerInfo?.port?.toString() || '3003';
    portInput.thickness = 1;
    portInput.paddingBottom = '15px';

    // World Selection Label
    const worldLabel = new TextBlock('worldLabel', 'World:');
    worldLabel.height = '25px';
    worldLabel.fontSize = 14;
    worldLabel.color = 'white';
    worldLabel.textHorizontalAlignment = Control.HORIZONTAL_ALIGNMENT_LEFT;
    worldLabel.paddingLeft = '40px';
    worldLabel.paddingBottom = '5px';

    // World Selection Container
    const worldContainer = new StackPanel('worldContainer');
    worldContainer.isVertical = false;
    worldContainer.height = '40px';
    worldContainer.width = '320px';
    worldContainer.horizontalAlignment = Control.HORIZONTAL_ALIGNMENT_CENTER;
    worldContainer.paddingBottom = '15px';

    // World loading text
    const worldLoadingText = new TextBlock('worldLoadingText', 'Loading worlds...');
    worldLoadingText.height = '40px';
    worldLoadingText.fontSize = 14;
    worldLoadingText.color = '#aaaaaa';
    worldContainer.addControl(worldLoadingText);

    // Function to fetch and update worlds
    const updateWorlds = async () => {
      const address = addressInput.text || 'localhost';
      const port = parseInt(portInput.text) || 3003;

      worldLoadingText.text = 'Loading worlds...';
      worldContainer.clearControls();
      worldContainer.addControl(worldLoadingText);

      const worlds = await this.fetchWorlds(address, port);
      this.availableWorlds = worlds;

      worldContainer.clearControls();

      if (worlds.length === 0) {
        const noWorldsText = new TextBlock('noWorldsText', 'No worlds available');
        noWorldsText.height = '40px';
        noWorldsText.fontSize = 14;
        noWorldsText.color = '#ff6666';
        worldContainer.addControl(noWorldsText);
        this.selectedWorld = undefined;
      } else {
        // Set default selected world
        this.selectedWorld = worlds[0].name;

        // Create world buttons
        worlds.forEach((world) => {
          const worldButton = Button.CreateSimpleButton(`world_${world.name}`, world.name);
          worldButton.width = `${320 / worlds.length - 5}px`;
          worldButton.height = '40px';
          worldButton.fontSize = 12;
          worldButton.color = 'white';
          worldButton.background = this.selectedWorld === world.name ? '#4CAF50' : '#333333';
          worldButton.cornerRadius = 3;
          worldButton.thickness = 1;
          worldButton.paddingRight = '5px';

          worldButton.onPointerClickObservable.add(() => {
            this.selectedWorld = world.name;
            // Update all button colors
            worldContainer.children.forEach((child: any) => {
              if (child.name && child.name.startsWith('world_')) {
                child.background = '#333333';
              }
            });
            worldButton.background = '#4CAF50';
          });

          worldButton.onPointerEnterObservable.add(() => {
            if (this.selectedWorld !== world.name) {
              worldButton.background = '#444444';
            }
          });

          worldButton.onPointerOutObservable.add(() => {
            if (this.selectedWorld !== world.name) {
              worldButton.background = '#333333';
            }
          });

          worldContainer.addControl(worldButton);
        });
      }
    };

    // Fetch worlds on address/port change
    addressInput.onTextChangedObservable.add(() => {
      updateWorlds();
    });

    portInput.onTextChangedObservable.add(() => {
      updateWorlds();
    });

    // Initial world fetch
    updateWorlds();

    // Terrain Preset Label
    const presetLabel = new TextBlock('presetLabel', 'Terrain Detail:');
    presetLabel.height = '25px';
    presetLabel.fontSize = 14;
    presetLabel.color = 'white';
    presetLabel.textHorizontalAlignment = Control.HORIZONTAL_ALIGNMENT_LEFT;
    presetLabel.paddingLeft = '40px';
    presetLabel.paddingBottom = '5px';

    // Terrain Preset Buttons Container
    const presetContainer = new StackPanel('presetContainer');
    presetContainer.isVertical = false;
    presetContainer.height = '40px';
    presetContainer.width = '320px';
    presetContainer.horizontalAlignment = Control.HORIZONTAL_ALIGNMENT_CENTER;
    presetContainer.paddingBottom = '15px';

    // Helper function to create preset button
    const createPresetButton = (name: string, preset: 'gaston' | 'dilbert' | 'popeye' | 'godzilla') => {
      const button = Button.CreateSimpleButton(`preset_${preset}`, name);
      button.width = '75px';
      button.height = '40px';
      button.fontSize = 12;
      button.color = 'white';
      button.background = this.selectedPreset === preset ? '#4CAF50' : '#333333';
      button.cornerRadius = 3;
      button.thickness = 1;
      button.paddingRight = '5px';

      button.onPointerClickObservable.add(() => {
        this.selectedPreset = preset;
        // Update all button colors
        presetContainer.children.forEach((child: any) => {
          if (child.name && child.name.startsWith('preset_')) {
            child.background = '#333333';
          }
        });
        button.background = '#4CAF50';
      });

      button.onPointerEnterObservable.add(() => {
        if (this.selectedPreset !== preset) {
          button.background = '#444444';
        }
      });

      button.onPointerOutObservable.add(() => {
        if (this.selectedPreset !== preset) {
          button.background = '#333333';
        }
      });

      return button;
    };

    // Create preset buttons
    const gastonButton = createPresetButton('Gaston', 'gaston');
    const dilbertButton = createPresetButton('Dilbert', 'dilbert');
    const popeyeButton = createPresetButton('Popeye', 'popeye');
    const godzillaButton = createPresetButton('Godzilla', 'godzilla');

    presetContainer.addControl(gastonButton);
    presetContainer.addControl(dilbertButton);
    presetContainer.addControl(popeyeButton);
    presetContainer.addControl(godzillaButton);

    // Connect Button
    const connectButton = Button.CreateSimpleButton('connectButton', 'Connect to Server');
    connectButton.width = '320px';
    connectButton.height = '50px';
    connectButton.fontSize = 18;
    connectButton.color = 'white';
    connectButton.background = '#4CAF50';
    connectButton.cornerRadius = 5;
    connectButton.thickness = 0;
    connectButton.paddingBottom = '10px';

    connectButton.onPointerEnterObservable.add(() => {
      connectButton.background = '#45a049';
    });

    connectButton.onPointerOutObservable.add(() => {
      connectButton.background = '#4CAF50';
    });

    connectButton.onPointerClickObservable.add(() => {
      // Determine render and unload distances based on preset
      let renderDistance = 1;
      let unloadDistance = 2;

      switch (this.selectedPreset) {
        case 'gaston':
          renderDistance = 1;
          unloadDistance = 2;
          break;
        case 'dilbert':
          renderDistance = 2;
          unloadDistance = 3;
          break;
        case 'popeye':
          renderDistance = 3;
          unloadDistance = 4;
          break;
        case 'godzilla':
          renderDistance = 4;
          unloadDistance = 5;
          break;
      }

      const serverInfo: ServerInfo = {
        name: nameInput.text || 'Server',
        address: addressInput.text || 'localhost',
        port: parseInt(portInput.text) || 3001,
        wireframeMode: false, // Wireframe removed
        renderDistance,
        unloadDistance,
        world: this.selectedWorld,  // Add selected world
      };

      this.hide();
      if (this.onConnect) {
        this.onConnect(serverInfo);
      }
    });

    // Add all controls to stack
    stack.addControl(logo);
    stack.addControl(title);
    stack.addControl(subtitle);
    stack.addControl(nameLabel);
    stack.addControl(nameInput);
    stack.addControl(addressLabel);
    stack.addControl(addressInput);
    stack.addControl(portLabel);
    stack.addControl(portInput);
    stack.addControl(worldLabel);
    stack.addControl(worldContainer);
    stack.addControl(presetLabel);
    stack.addControl(presetContainer);
    stack.addControl(connectButton);

    this.mainContainer.addControl(stack);
    this.guiTexture.addControl(this.mainContainer);
  }

  /**
   * Hide the main menu
   */
  hide(): void {
    if (this.mainContainer) {
      this.guiTexture.removeControl(this.mainContainer);
      this.mainContainer.dispose();
      this.mainContainer = undefined;
    }

    // Remove test cube
    if (this.testCube) {
      this.testCube.dispose();
      this.testCube = undefined;
    }
  }

  /**
   * Check if menu is visible
   */
  isVisible(): boolean {
    return this.mainContainer !== undefined;
  }

  /**
   * Dispose the menu
   */
  dispose(): void {
    this.hide();
    this.guiTexture.dispose();

    // Dispose test cube
    if (this.testCube) {
      this.testCube.dispose();
      this.testCube = undefined;
    }
  }
}
