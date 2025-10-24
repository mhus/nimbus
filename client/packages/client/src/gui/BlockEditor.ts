/**
 * Block Editor
 * Shows and edits properties of selected block
 * Two modes: Display (Block-Info) and Edit (Block-Editor)
 */
import {
  AdvancedDynamicTexture,
  Rectangle,
  TextBlock,
  ScrollViewer,
  Control,
  StackPanel,
  Button,
  InputText,
  Checkbox,
  ColorPicker,
} from '@babylonjs/gui';
import type { Scene } from '@babylonjs/core';
import { Color3 } from '@babylonjs/core';
import type { BlockSelector, SelectedBlock } from '../player/BlockSelector';
import type { ClientRegistry } from '../registry/ClientRegistry';
import type { ChunkManager } from '../world/ChunkManager';
import type { BlockType, BlockModifier } from '@nimbus-client/core';
import { BlockShape, getBlockEdgeOffsets, setBlockEdgeOffsets } from '@nimbus-client/core';
import {
  addLabel,
  addSectionTitle,
  addProperty,
  addText,
  addSpacer,
} from './BlockEditorHelpers';
import {
  addInputField,
  addDropdown,
  addCheckbox,
  addCollapsibleHeader,
  addNumberInput,
  addEdgeOffsetRow,
  addScaleRow,
  addRotationRow,
  addColorPickerRow,
} from './BlockEditorInputs';
import { DropdownMenu } from './DropdownMenu';

enum EditorTab {
  BlockInfo = 'info',
  BlockEditor = 'editor',
  BlockList = 'list',
  Changes = 'changes'
}

type ModificationStatus = 'new' | 'deleted' | 'modified';

interface BlockModification {
  position: { x: number; y: number; z: number };
  status: ModificationStatus;
  originalBlockId: number;  // To track what it was before first change
  currentBlockId: number;  // Current block ID after modification
  modifier?: BlockModifier;  // Optional modifier for this block instance
  metadata?: any;  // Optional metadata for this block instance
}

interface BlockCopyBuffer {
  blockId: number;
  modifier?: BlockModifier;
  metadata?: any;
  edgeOffsets?: number[];
}

export class BlockEditor {
  private scene: Scene;
  private advancedTexture: AdvancedDynamicTexture;
  private blockSelector: BlockSelector;
  private registry: ClientRegistry;
  private chunkManager: ChunkManager;
  private client: any; // VoxelClient reference

  private container!: Rectangle;
  private toolbarPanel!: StackPanel;
  private tabButtonsPanel!: StackPanel;
  private contentContainer!: Rectangle;
  private scrollViewer!: ScrollViewer;
  private contentPanel!: StackPanel;

  private isVisible = false;
  private selectedBlock: SelectedBlock | null = null;
  private currentTab: EditorTab = EditorTab.BlockInfo;
  private isEditMode = false;
  private ignoreNextPointerLock = false; // Flag to prevent pointer lock after Accept

  // Change tracking - only coordinates and status
  private modifiedBlocks: Map<string, BlockModification> = new Map();

  // Edit form fields
  private editBlockId!: InputText;
  private editBlockShape!: InputText;
  private editBlockMaterial!: InputText;

  // Modifier fields
  private editModifierShape!: InputText;
  private editModifierDisplayName!: InputText;
  private editModifierTexture!: InputText;
  private editModifierRotation!: InputText;
  private currentModifier: BlockModifier | null = null;

  // Metadata fields
  private currentMetadata: any | null = null;

  // Collapsible section states
  private edgeOffsetsExpanded = false;
  private modifierExpanded = false;
  private blockOptionsExpanded = false;
  private metadataExpanded = false;

  // Edge offset data (8 corners * 3 values = 24 values)
  private currentEdgeOffsets: number[] | null = null;

  // Copy/Paste buffer for blocks
  private copyPasteBuffer: BlockCopyBuffer | null = null;

  constructor(
    scene: Scene,
    advancedTexture: AdvancedDynamicTexture,
    blockSelector: BlockSelector,
    registry: ClientRegistry,
    chunkManager: ChunkManager,
    client: any
  ) {
    this.scene = scene;
    this.advancedTexture = advancedTexture;
    this.blockSelector = blockSelector;
    this.registry = registry;
    this.chunkManager = chunkManager;
    this.client = client;

    this.createUI();
    this.hide();
    this.setupKeyboardListener();
    this.setupServerResponseHandlers();

    console.log('[BlockEditor] Initialized with tabs and edit mode');
  }

  /**
   * Setup keyboard listener for ESC and ',' keys
   */
  private setupKeyboardListener(): void {
    this.scene.onKeyboardObservable.add((kbInfo) => {
      // Only handle keys when editor is visible
      if (!this.isVisible) return;

      if (kbInfo.type === 1 && kbInfo.event.key === 'Escape') { // KeyboardEventTypes.KEYDOWN = 1
        console.log('[BlockEditor] ESC key pressed - accepting edit mode');
        this.acceptEditMode();
      }

      // Handle ',' key for paste in new-block-select mode
      if (kbInfo.type === 1 && kbInfo.event.key === ',') { // KeyboardEventTypes.KEYDOWN = 1
        // Only trigger if blockSelector is in SelectForNew mode
        if (this.blockSelector.isSelectForNewMode() && this.copyPasteBuffer) {
          console.log('[BlockEditor] "," key pressed - pasting buffer in new-block-select mode');
          // Activate edit mode first, then paste
          this.activateEditMode();
          // Paste the buffer after a short delay to ensure edit mode is fully activated
          setTimeout(() => {
            this.pasteBlock();
          }, 50);
        }
      }
    });
  }

  /**
   * Setup server response handlers
   */
  private setupServerResponseHandlers(): void {
    const socket = (this.client as any).socket;
    if (!socket) {
      console.warn('[BlockEditor] Cannot setup server response handlers: No socket available');
      return;
    }

    console.log('[BlockEditor] Setting up server response handler for block_changes_applied...');
    console.log('[BlockEditor] Socket type:', typeof socket);
    console.log('[BlockEditor] Socket.on type:', typeof socket.on);

    // Listen for block_changes_applied response from server
    socket.on('block_changes_applied', (message: any) => {
      console.log('[BlockEditor] *** HANDLER CALLED *** Received block_changes_applied response:', message);

      if (message.success) {
        console.log(`[BlockEditor] Server successfully applied ${message.count} block changes to ${message.affectedChunks} chunks`);

        // NOW we can safely clear modifiedBlocks since server confirmed the changes
        this.modifiedBlocks.clear();
        this.refreshContent();

        console.log('[BlockEditor] Local modifications cleared after server confirmation');
      } else {
        console.error('[BlockEditor] Server failed to apply block changes:', message.error);
        // Don't clear modifiedBlocks - user can try again
      }
    });

    console.log('[BlockEditor] Server response handlers registered for block_changes_applied');
  }

  /**
   * Create editor UI with tabs and toolbar
   */
  private createUI(): void {
    // Main container (right side of screen)
    this.container = new Rectangle('blockEditorContainer');
    this.container.width = '450px';
    this.container.height = 0.85;
    this.container.thickness = 2;
    this.container.color = '#FFFFFF';
    this.container.background = '#000000DD';
    this.container.horizontalAlignment = Control.HORIZONTAL_ALIGNMENT_RIGHT;
    this.container.verticalAlignment = Control.VERTICAL_ALIGNMENT_CENTER;
    this.container.left = -10;
    this.container.isVisible = false;

    // Title
    const title = new TextBlock('editorTitle');
    title.text = 'Block Editor';
    title.color = '#FFFFFF';
    title.fontSize = 20;
    title.height = '35px';
    title.textHorizontalAlignment = Control.HORIZONTAL_ALIGNMENT_CENTER;
    title.verticalAlignment = Control.VERTICAL_ALIGNMENT_TOP;
    title.paddingTop = 5;
    this.container.addControl(title);

    // Close button (X icon in top-right corner)
    const closeButton = Button.CreateSimpleButton('closeEditor', '✕');
    closeButton.width = '30px';
    closeButton.height = '30px';
    closeButton.color = '#FFFFFF';
    closeButton.background = '#AA0000';
    closeButton.thickness = 1;
    closeButton.fontSize = 18;
    closeButton.fontWeight = 'bold';
    closeButton.horizontalAlignment = Control.HORIZONTAL_ALIGNMENT_RIGHT;
    closeButton.verticalAlignment = Control.VERTICAL_ALIGNMENT_TOP;
    closeButton.top = 5;
    closeButton.left = -5;
    closeButton.onPointerClickObservable.add(() => {
      this.closeEditor();
    });
    this.container.addControl(closeButton);

    // Toolbar
    this.createToolbar();

    // Tab buttons
    this.createTabButtons();

    // Content container (positioned directly below tab buttons)
    this.contentContainer = new Rectangle('contentContainer');
    this.contentContainer.width = 1;
    this.contentContainer.height = 0.85; // Use most of available space (leave room for header)
    this.contentContainer.thickness = 0;
    this.contentContainer.verticalAlignment = Control.VERTICAL_ALIGNMENT_TOP;
    this.contentContainer.top = 110; // Position directly below tabs (35 + 40 + 35)

    // Scroll viewer for content
    this.scrollViewer = new ScrollViewer('editorScroller');
    this.scrollViewer.width = 1;
    this.scrollViewer.height = 1;
    this.scrollViewer.thickness = 0;

    // Content panel
    this.contentPanel = new StackPanel('editorContent');
    this.contentPanel.width = 1;
    this.contentPanel.paddingLeft = 10;
    this.contentPanel.paddingRight = 10;
    this.contentPanel.paddingTop = 10;
    this.contentPanel.paddingBottom = 10;

    this.scrollViewer.addControl(this.contentPanel);
    this.contentContainer.addControl(this.scrollViewer);
    this.container.addControl(this.contentContainer);

    // Add to advanced texture
    this.advancedTexture.addControl(this.container);
  }

  /**
   * Create toolbar with action buttons
   */
  private createToolbar(): void {
    this.toolbarPanel = new StackPanel('toolbar');
    this.toolbarPanel.isVertical = false;
    this.toolbarPanel.height = '40px';
    this.toolbarPanel.width = 1;
    this.toolbarPanel.verticalAlignment = Control.VERTICAL_ALIGNMENT_TOP;
    this.toolbarPanel.top = 35;
    this.toolbarPanel.paddingLeft = 5;
    this.toolbarPanel.paddingRight = 5;

    // Accept Edit Mode button
    const acceptBtn = this.createToolbarButton('Accept', '#00AA00');
    acceptBtn.onPointerClickObservable.add(() => {
      // Stop event propagation to prevent canvas click handler from triggering
      this.acceptEditMode();
    });
    this.toolbarPanel.addControl(acceptBtn);

    this.addToolbarSpacer();

    // Apply All button
    const applyAllBtn = this.createToolbarButton('Apply All', '#00AA00');
    applyAllBtn.onPointerClickObservable.add(() => this.applyAll());
    this.toolbarPanel.addControl(applyAllBtn);

    // Revert All button
    const revertAllBtn = this.createToolbarButton('Revert All', '#AA0000');
    revertAllBtn.onPointerClickObservable.add(() => this.revertAll());
    this.toolbarPanel.addControl(revertAllBtn);

    this.addToolbarSpacer();

    // New Block button
    const newBlockBtn = this.createToolbarButton('New Block', '#0088FF');
    newBlockBtn.onPointerClickObservable.add(() => this.createNewBlock());
    this.toolbarPanel.addControl(newBlockBtn);

    this.addToolbarSpacer();

    // Delete Block button
    const deleteBtn = this.createToolbarButton('Delete', '#AA0000');
    deleteBtn.onPointerClickObservable.add(() => this.deleteBlock());
    this.toolbarPanel.addControl(deleteBtn);

    this.container.addControl(this.toolbarPanel);
  }

  /**
   * Create toolbar button
   */
  private createToolbarButton(text: string, color: string): Button {
    const button = Button.CreateSimpleButton('btn_' + text, text);
    button.width = '80px';
    button.height = '30px';
    button.color = '#FFFFFF';
    button.background = color;
    button.thickness = 1;
    button.fontSize = 12;
    return button;
  }

  /**
   * Add spacer to toolbar
   */
  private addToolbarSpacer(): void {
    const spacer = new Rectangle('spacer');
    spacer.width = '5px';
    spacer.height = '30px';
    spacer.thickness = 0;
    this.toolbarPanel.addControl(spacer);
  }

  /**
   * Create tab buttons
   */
  private createTabButtons(): void {
    this.tabButtonsPanel = new StackPanel('tabButtons');
    this.tabButtonsPanel.isVertical = false;
    this.tabButtonsPanel.height = '35px';
    this.tabButtonsPanel.width = 1;
    this.tabButtonsPanel.verticalAlignment = Control.VERTICAL_ALIGNMENT_TOP;
    this.tabButtonsPanel.top = 75;
    this.tabButtonsPanel.paddingLeft = 5;

    // Block Info Tab
    const infoBtn = this.createTabButton('Block Info', EditorTab.BlockInfo);
    this.tabButtonsPanel.addControl(infoBtn);

    // Block Editor Tab
    const editorBtn = this.createTabButton('Block Editor', EditorTab.BlockEditor);
    this.tabButtonsPanel.addControl(editorBtn);

    // Block Type List Tab
    const listBtn = this.createTabButton('Block-Type Liste', EditorTab.BlockList);
    this.tabButtonsPanel.addControl(listBtn);

    // Changes Tab
    const changesBtn = this.createTabButton('Änderungen', EditorTab.Changes);
    this.tabButtonsPanel.addControl(changesBtn);

    this.container.addControl(this.tabButtonsPanel);
  }

  /**
   * Create tab button
   */
  private createTabButton(text: string, tab: EditorTab): Button {
    const button = Button.CreateSimpleButton('tab_' + tab, text);
    button.width = '110px';
    button.height = '30px';
    button.color = '#FFFFFF';
    button.background = this.currentTab === tab ? '#005500' : '#003300';
    button.thickness = 1;
    button.fontSize = 12;

    button.onPointerClickObservable.add(() => {
      this.switchTab(tab);
    });

    return button;
  }

  /**
   * Switch to different tab
   */
  private switchTab(tab: EditorTab): void {
    // Set flag to ignore pointer lock from tab button click
    this.ignoreNextPointerLock = true;

    this.currentTab = tab;
    this.refreshTabButtons();
    this.refreshContent();
  }

  /**
   * Refresh tab button colors
   */
  private refreshTabButtons(): void {
    // Update all tab button backgrounds
    for (let i = 0; i < this.tabButtonsPanel.children.length; i++) {
      const child = this.tabButtonsPanel.children[i];
      if (child instanceof Button) {
        const tabName = child.name.replace('tab_', '');
        child.background = this.currentTab === tabName ? '#005500' : '#003300';
      }
    }
  }

  /**
   * Activate edit mode (switch to Block Editor tab)
   * If an Air block is selected, automatically create a new block (stone, ID 1)
   */
  activateEditMode(): void {
    if (!this.isVisible) return;

    // IMPORTANT: Get current selection from BlockSelector BEFORE switching tab
    // because update() stops running in BlockEditor tab
    const currentSelection = this.blockSelector.getSelectedBlock();
    if (!currentSelection) {
      console.log('[BlockEditor] No block selected - automatically activating "New Block" mode');
      this.createNewBlock();
      return;
    }

    // Update selectedBlock to current selection
    this.selectedBlock = currentSelection;

    // Reset edge offsets when selecting a new block
    this.currentEdgeOffsets = null;

    // If in "select for new" mode and position is occupied, prevent editing
    if (this.blockSelector.isSelectForNewMode() && this.selectedBlock.blockId !== 0) {
      console.warn(`[BlockEditor] Cannot create new block: Position is occupied (blockId: ${this.selectedBlock.blockId})`);
      return; // Don't open editor for occupied position
    }

    // If Air block is selected, automatically create a new block (stone, ID 1)
    if (this.selectedBlock.blockId === 0) {
      console.log(`[BlockEditor] Air block selected - creating new block (stone) at (${this.selectedBlock.blockX}, ${this.selectedBlock.blockY}, ${this.selectedBlock.blockZ})`);

      // Create stone block (ID 1) at this position
      this.trackBlockModification(1);

      // Update selectedBlock blockId to reflect the change
      this.selectedBlock.blockId = 1;

      // Exit "select for new" mode if active
      if (this.blockSelector.isSelectForNewMode()) {
        this.blockSelector.disableSelectForNew();
        console.log('[BlockEditor] Auto-exited "select for new" mode after creating block');
      }
    }

    console.log(`[BlockEditor] Edit mode activated for block at (${this.selectedBlock.blockX}, ${this.selectedBlock.blockY}, ${this.selectedBlock.blockZ}) - ID: ${this.selectedBlock.blockId}`);

    this.isEditMode = true;
    this.switchTab(EditorTab.BlockEditor);

    // Show red highlight for the block being edited
    this.blockSelector.setEditModeBlock(this.selectedBlock);
  }

  /**
   * Accept edit mode (switch back to Block Info) and exit "select for new" mode if active
   */
  private acceptEditMode(): void {
    this.isEditMode = false;
    this.switchTab(EditorTab.BlockInfo);

    // Hide red highlight when exiting edit mode
    this.blockSelector.setEditModeBlock(null);

    // Exit "select for new" mode if active
    if (this.blockSelector.isSelectForNewMode()) {
      this.blockSelector.disableSelectForNew();
      console.log('[BlockEditor] Exited "select for new" mode');
    }

    // Exit pointer lock if active (user can manually click into 3D window to re-enable)
    if (document.pointerLockElement) {
      // Set flag to ignore the IMMEDIATE pointer lock request from button click
      this.ignoreNextPointerLock = true;

      document.exitPointerLock();
      console.log('[BlockEditor] Pointer lock released - click into 3D window to re-enable');
    } else {
      // No pointer lock was active, just set flag to ignore button click
      this.ignoreNextPointerLock = true;
    }

    console.log('[BlockEditor] Edit mode accepted');
  }

  /**
   * Create new block - activate "select for new" mode
   */
  createNewBlock(): void {
    console.log('[BlockEditor] Entering "select for new" mode...');

    // Set flag to ignore pointer lock from button click
    this.ignoreNextPointerLock = true;

    // Enable "select for new" mode in BlockSelector
    this.blockSelector.enableSelectForNew();

    // Switch to Block Info tab so user can see the green selection
    this.switchTab(EditorTab.BlockInfo);
  }

  /**
   * Delete current block (set to air)
   */
  private deleteBlock(): void {
    if (!this.selectedBlock) return;

    console.log(`[BlockEditor] Deleting block at (${this.selectedBlock.blockX}, ${this.selectedBlock.blockY}, ${this.selectedBlock.blockZ})`);

    // Set flag to ignore pointer lock from button click
    this.ignoreNextPointerLock = true;

    // Set block to air (ID 0) using the existing trackBlockModification system
    this.trackBlockModification(0);
  }

  /**
   * Apply all changes - send to server
   */
  private applyAll(): void {
    console.log('[BlockEditor] Applying all changes...', this.modifiedBlocks.size, 'blocks');

    // Set flag to ignore pointer lock from button click
    this.ignoreNextPointerLock = true;

    if (this.modifiedBlocks.size === 0) {
      console.log('[BlockEditor] No changes to apply');
      return;
    }

    // Get socket from client
    const socket = (this.client as any).socket;
    if (!socket || !socket.isConnected()) {
      console.error('[BlockEditor] Cannot apply changes: Not connected to server');
      return;
    }

    // Convert modifiedBlocks to array of block updates
    const blockUpdates = Array.from(this.modifiedBlocks.entries()).map(([key, modification]) => {
      // Use the stored currentBlockId instead of reading from chunk
      // This prevents issues where server chunk updates overwrite local changes before they're sent
      const currentBlockId = modification.currentBlockId;

      // DEBUG: Log block 94
      if (currentBlockId === 94) {
        console.log(`[BlockEditor] 🔵 BLOCK 94 - Preparing to send to server at (${modification.position.x}, ${modification.position.y}, ${modification.position.z})`);
      }

      // Get current metadata from chunk (not tracked in modifiedBlocks yet)
      const currentMetadata = this.chunkManager.getBlockMetadata(
        modification.position.x,
        modification.position.y,
        modification.position.z
      );

      return {
        x: modification.position.x,
        y: modification.position.y,
        z: modification.position.z,
        blockId: currentBlockId,
        modifier: modification.modifier,
        metadata: currentMetadata,
      };
    });

    console.log(`[BlockEditor] Sending ${blockUpdates.length} block updates to server...`);

    // DEBUG: Log all block 94s being sent
    const block94Updates = blockUpdates.filter(u => u.blockId === 94);
    if (block94Updates.length > 0) {
      console.log(`[BlockEditor] 🔵 BLOCK 94 - Sending ${block94Updates.length} blocks of type 94:`, block94Updates);
    }

    // Send to server (socket.send spreads data with ...data, so changes will be at top level)
    socket.send('apply_block_changes', {
      changes: blockUpdates,
    });

    // DON'T clear modifications yet - wait for server confirmation
    // The 'block_changes_applied' handler will clear them after receiving success response

    console.log('[BlockEditor] Block updates sent to server, waiting for confirmation...', this.modifiedBlocks.size, 'still tracked');
  }

  /**
   * Revert all changes
   */
  private revertAll(): void {
    console.log('[BlockEditor] Reverting all changes...');

    // Set flag to ignore pointer lock from button click
    this.ignoreNextPointerLock = true;

    // Collect all affected chunks
    const affectedChunks = new Set<string>();
    for (const modification of this.modifiedBlocks.values()) {
      const chunkX = Math.floor(modification.position.x / 32);
      const chunkZ = Math.floor(modification.position.z / 32);
      const chunkKey = `${chunkX},${chunkZ}`;
      affectedChunks.add(chunkKey);
    }

    console.log(`[BlockEditor] Reloading ${affectedChunks.size} affected chunks from server...`);

    // Reload each affected chunk from server
    for (const chunkKey of affectedChunks) {
      const [chunkX, chunkZ] = chunkKey.split(',').map(Number);
      this.chunkManager.reloadChunk(chunkX, chunkZ);
    }

    // Clear modifications list
    this.modifiedBlocks.clear();
    this.refreshContent();

    console.log('[BlockEditor] All changes reverted, chunks reloaded from server');
  }

  /**
   * Track modification for current block
   * Smart logic: deleted + new = modified
   */
  private trackBlockModification(newBlockId: number): void {
    if (!this.selectedBlock) return;

    const key = `${this.selectedBlock.blockX},${this.selectedBlock.blockY},${this.selectedBlock.blockZ}`;

    // Get current block ID from chunk data
    const currentBlockId = this.chunkManager.getBlockAt(
      this.selectedBlock.blockX,
      this.selectedBlock.blockY,
      this.selectedBlock.blockZ
    );

    if (currentBlockId === undefined) {
      console.warn(`[BlockEditor] Cannot modify block at ${key}: Chunk not loaded`);
      return;
    }

    console.log(`[BlockEditor] Modifying block at ${key}: ${currentBlockId} -> ${newBlockId}`);

    // Check if already tracked
    const existing = this.modifiedBlocks.get(key);

    if (!existing) {
      // First modification of this block
      let status: ModificationStatus;

      if (currentBlockId === 0 && newBlockId !== 0) {
        status = 'new';  // Was air, now solid
      } else if (currentBlockId !== 0 && newBlockId === 0) {
        status = 'deleted';  // Was solid, now air
      } else {
        status = 'modified';  // Changed type
      }

      this.modifiedBlocks.set(key, {
        position: {
          x: this.selectedBlock.blockX,
          y: this.selectedBlock.blockY,
          z: this.selectedBlock.blockZ,
        },
        status,
        originalBlockId: currentBlockId,
        currentBlockId: newBlockId,  // Store the new block ID here
      });

      console.log(`[BlockEditor] Tracked as ${status} (original: ${currentBlockId})`);
    } else {
      // Already tracked - apply smart logic
      const wasAir = existing.originalBlockId === 0;
      const isAir = newBlockId === 0;

      if (wasAir && !isAir) {
        // Was air originally, now solid -> 'new'
        existing.status = 'new';
      } else if (!wasAir && isAir) {
        // Was solid originally, now air -> 'deleted'
        existing.status = 'deleted';
      } else if (!wasAir && !isAir) {
        // Was solid, still solid (changed type) -> 'modified'
        existing.status = 'modified';
      } else {
        // Was air, still air (changed to different air?) -> remove from tracking
        this.modifiedBlocks.delete(key);
        console.log(`[BlockEditor] Removed tracking (air -> air)`);
      }

      // Update currentBlockId to the new value
      existing.currentBlockId = newBlockId;

      console.log(`[BlockEditor] Updated status to ${existing.status}`);
    }

    // Apply change directly to chunk data
    this.chunkManager.updateBlockAt(
      this.selectedBlock.blockX,
      this.selectedBlock.blockY,
      this.selectedBlock.blockZ,
      newBlockId
    );

    // Update selectedBlock for UI
    this.selectedBlock.blockId = newBlockId;

    // Refresh display
    this.refreshContent();
  }

  /**
   * Revert single block
   */
  private revertBlock(): void {
    if (!this.selectedBlock) return;

    const key = `${this.selectedBlock.blockX},${this.selectedBlock.blockY},${this.selectedBlock.blockZ}`;

    if (this.modifiedBlocks.has(key)) {
      this.modifiedBlocks.delete(key);
      console.log(`[BlockEditor] Reverted block at ${key}`);
      this.refreshContent();
    }
  }

  /**
   * Copy current selected block to buffer
   */
  private copyBlock(): void {
    if (!this.selectedBlock) return;

    // Get current block data from chunk
    const blockId = this.selectedBlock.blockId;
    const modifier = this.chunkManager.getBlockModifier(
      this.selectedBlock.blockX,
      this.selectedBlock.blockY,
      this.selectedBlock.blockZ
    );
    const metadata = this.chunkManager.getBlockMetadata(
      this.selectedBlock.blockX,
      this.selectedBlock.blockY,
      this.selectedBlock.blockZ
    );
    const edgeOffsets = this.chunkManager.getBlockEdgeOffsets(
      this.selectedBlock.blockX,
      this.selectedBlock.blockY,
      this.selectedBlock.blockZ
    );

    // Store in buffer
    this.copyPasteBuffer = {
      blockId,
      modifier: modifier || undefined,
      metadata: metadata || undefined,
      edgeOffsets: edgeOffsets || undefined,
    };

    console.log(`[BlockEditor] Copied block (ID: ${blockId}) to buffer`, this.copyPasteBuffer);

    // Refresh to show buffer info
    this.refreshContent();
  }

  /**
   * Paste buffer data to currently selected block
   */
  private pasteBlock(): void {
    if (!this.selectedBlock || !this.copyPasteBuffer) return;

    console.log(`[BlockEditor] Pasting block from buffer to (${this.selectedBlock.blockX}, ${this.selectedBlock.blockY}, ${this.selectedBlock.blockZ})`);

    // Apply blockId via trackBlockModification
    this.trackBlockModification(this.copyPasteBuffer.blockId);

    // Apply modifier if exists
    if (this.copyPasteBuffer.modifier) {
      const key = `${this.selectedBlock.blockX},${this.selectedBlock.blockY},${this.selectedBlock.blockZ}`;
      let tracked = this.modifiedBlocks.get(key);

      if (!tracked) {
        tracked = {
          position: {
            x: this.selectedBlock.blockX,
            y: this.selectedBlock.blockY,
            z: this.selectedBlock.blockZ,
          },
          status: 'modified',
          originalBlockId: this.selectedBlock.blockId,
          currentBlockId: this.copyPasteBuffer.blockId,
        };
        this.modifiedBlocks.set(key, tracked);
      }

      // Copy modifier to tracked modifications
      tracked.modifier = { ...this.copyPasteBuffer.modifier };

      // Apply to chunk
      this.chunkManager.updateBlockModifier(
        this.selectedBlock.blockX,
        this.selectedBlock.blockY,
        this.selectedBlock.blockZ,
        tracked.modifier
      );
    }

    // Apply metadata if exists
    if (this.copyPasteBuffer.metadata) {
      this.chunkManager.updateBlockMetadata(
        this.selectedBlock.blockX,
        this.selectedBlock.blockY,
        this.selectedBlock.blockZ,
        { ...this.copyPasteBuffer.metadata }
      );
    }

    // Apply edge offsets if exists
    if (this.copyPasteBuffer.edgeOffsets) {
      this.currentEdgeOffsets = [...this.copyPasteBuffer.edgeOffsets];
      this.chunkManager.updateBlockEdgeOffsets(
        this.selectedBlock.blockX,
        this.selectedBlock.blockY,
        this.selectedBlock.blockZ,
        this.currentEdgeOffsets
      );
    }

    console.log(`[BlockEditor] Pasted block (ID: ${this.copyPasteBuffer.blockId})`);

    // Refresh content
    this.refreshContent();
  }

  /**
   * Apply current edge offsets to chunk
   */
  private applyEdgeOffsets(): void {
    if (!this.selectedBlock || !this.currentEdgeOffsets) {
      return;
    }

    console.log(`[BlockEditor] Applying edge offsets to block at (${this.selectedBlock.blockX}, ${this.selectedBlock.blockY}, ${this.selectedBlock.blockZ})`);

    // Apply edge offsets via ChunkManager (this will also trigger re-render)
    this.chunkManager.updateBlockEdgeOffsets(
      this.selectedBlock.blockX,
      this.selectedBlock.blockY,
      this.selectedBlock.blockZ,
      this.currentEdgeOffsets
    );
  }

  /**
   * Show editor
   */
  show(): void {
    if (this.isVisible) return;

    this.container.isVisible = true;
    this.isVisible = true;
    this.update();
  }

  /**
   * Hide editor
   */
  hide(): void {
    if (!this.isVisible) return;

    this.container.isVisible = false;
    this.isVisible = false;
  }

  /**
   * Toggle editor visibility
   */
  toggle(): void {
    if (this.isVisible) {
      this.hide();
    } else {
      this.show();
    }
  }

  /**
   * Close editor and disable select mode
   */
  private closeEditor(): void {
    console.log('[BlockEditor] Closing editor via Close button');

    // Exit edit mode if active
    if (this.isEditMode) {
      this.isEditMode = false;
      this.blockSelector.setEditModeBlock(null);
    }

    // Disable "select for new" mode if active
    if (this.blockSelector.isSelectForNewMode()) {
      this.blockSelector.disableSelectForNew();
      console.log('[BlockEditor] Disabled "select for new" mode');
    }

    // Disable selection mode
    this.blockSelector.disable();
    console.log('[BlockEditor] Disabled selection mode');

    // Hide editor
    this.hide();

    // Release pointer lock if active
    if (document.pointerLockElement) {
      this.ignoreNextPointerLock = true;
      document.exitPointerLock();
      console.log('[BlockEditor] Pointer lock released');
    }
  }

  /**
   * Check if editor is visible
   */
  getIsVisible(): boolean {
    return this.isVisible;
  }

  /**
   * Check if editor is in edit mode (Block Editor tab active)
   */
  isInEditMode(): boolean {
    return this.isVisible && this.currentTab === EditorTab.BlockEditor;
  }

  /**
   * Check if pointer lock should be ignored (e.g., right after Accept button)
   */
  shouldIgnorePointerLock(): boolean {
    if (this.ignoreNextPointerLock) {
      this.ignoreNextPointerLock = false;
      return true;
    }
    return false;
  }

  /**
   * Update editor content
   */
  update(): void {
    if (!this.isVisible) return;

    // In Block Editor tab (edit mode), keep the block locked to the one selected when '.' was pressed
    if (this.currentTab === EditorTab.BlockEditor) {
      return;
    }

    // In other tabs (Block Info), update with current selection
    const selected = this.blockSelector.getSelectedBlock();

    if (this.hasSelectionChanged(selected)) {
      this.selectedBlock = selected;
      this.refreshContent();
    }
  }

  /**
   * Check if selection has changed
   */
  private hasSelectionChanged(newSelection: SelectedBlock | null): boolean {
    if (!this.selectedBlock && !newSelection) return false;
    if (!this.selectedBlock || !newSelection) return true;

    return (
      this.selectedBlock.blockX !== newSelection.blockX ||
      this.selectedBlock.blockY !== newSelection.blockY ||
      this.selectedBlock.blockZ !== newSelection.blockZ ||
      this.selectedBlock.blockId !== newSelection.blockId
    );
  }

  /**
   * Refresh editor content based on current tab
   */
  private refreshContent(): void {
    this.contentPanel.clearControls();

    switch (this.currentTab) {
      case EditorTab.BlockInfo:
        this.renderBlockInfo();
        break;
      case EditorTab.BlockEditor:
        this.renderBlockEditor();
        break;
      case EditorTab.BlockList:
        this.renderBlockList();
        break;
      case EditorTab.Changes:
        this.renderChanges();
        break;
    }
  }

  /**
   * Render Block Info tab (read-only)
   */
  private renderBlockInfo(): void {
    if (!this.selectedBlock) {
      addText(this.contentPanel, 'No block selected', '#888888');
      addText(this.contentPanel, 'Enable selection mode with "select on"', '#888888', 12);
      return;
    }

    const blockType = this.registry.getBlockByID(this.selectedBlock.blockId);

    if (!blockType) {
      addText(this.contentPanel, `Unknown block ID: ${this.selectedBlock.blockId}`, '#FF0000');
      return;
    }

    // Toolbar at the top with Copy button
    const toolbarContainer = new StackPanel('infoToolbar');
    toolbarContainer.isVertical = false;
    toolbarContainer.height = '40px';
    toolbarContainer.width = 1;
    toolbarContainer.paddingBottom = 10;

    const copyBtn = Button.CreateSimpleButton('copyBlock', 'Copy');
    copyBtn.width = '80px';
    copyBtn.height = '30px';
    copyBtn.color = '#FFFFFF';
    copyBtn.background = '#0088FF';
    copyBtn.thickness = 1;
    copyBtn.fontSize = 14;
    copyBtn.horizontalAlignment = Control.HORIZONTAL_ALIGNMENT_LEFT;
    copyBtn.onPointerClickObservable.add(() => {
      this.copyBlock();
    });
    toolbarContainer.addControl(copyBtn);

    this.contentPanel.addControl(toolbarContainer);

    // Show buffer info if exists
    if (this.copyPasteBuffer) {
      const bufferBlockType = this.registry.getBlockByID(this.copyPasteBuffer.blockId);
      const bufferName = bufferBlockType ? bufferBlockType.name : `ID ${this.copyPasteBuffer.blockId}`;
      addText(this.contentPanel, `Buffer: ${bufferName}`, '#FFFF00', 12);
      addSpacer(this.contentPanel);
    }

    // Display block information (read-only)
    addSectionTitle(this.contentPanel, 'Block Information');
    addProperty(this.contentPanel, 'Position', `(${this.selectedBlock.blockX}, ${this.selectedBlock.blockY}, ${this.selectedBlock.blockZ})`);
    addProperty(this.contentPanel, 'Distance', `${this.selectedBlock.distance.toFixed(2)} blocks`);

    // Calculate chunk coordinates
    const chunkX = Math.floor(this.selectedBlock.blockX / 32);
    const chunkZ = Math.floor(this.selectedBlock.blockZ / 32);
    const localX = this.selectedBlock.blockX - chunkX * 32;
    const localZ = this.selectedBlock.blockZ - chunkZ * 32;
    addProperty(this.contentPanel, 'Chunk', `(${chunkX}, ${chunkZ})`);
    addProperty(this.contentPanel, 'Local Pos', `(${localX}, ${this.selectedBlock.blockY}, ${localZ})`);
    addSpacer(this.contentPanel);

    // Load BlockMetadata from chunk
    const blockMetadata = this.chunkManager.getBlockMetadata(
      this.selectedBlock.blockX,
      this.selectedBlock.blockY,
      this.selectedBlock.blockZ
    );

    if (blockMetadata && Object.keys(blockMetadata).length > 0) {
      addSectionTitle(this.contentPanel, 'Block Metadata (Instance Data)');
      if (blockMetadata.displayName) {
        addProperty(this.contentPanel, 'Display Name', blockMetadata.displayName);
      }
      if (blockMetadata.groupId) {
        addProperty(this.contentPanel, 'Group ID', blockMetadata.groupId);
      }
      addSpacer(this.contentPanel);
    }

    // Load BlockModifier from chunk
    const blockModifier = this.chunkManager.getBlockModifier(
      this.selectedBlock.blockX,
      this.selectedBlock.blockY,
      this.selectedBlock.blockZ
    );

    if (blockModifier && Object.keys(blockModifier).length > 0) {
      addSectionTitle(this.contentPanel, 'Block Modifier (Visual Overrides)');
      if (blockModifier.shape !== undefined) {
        addProperty(this.contentPanel, 'Shape Override', this.getShapeName(blockModifier.shape));
      }
      if (blockModifier.texture) {
        addProperty(this.contentPanel, 'Texture Override', this.getTextureInfo(blockModifier.texture));
      }
      if (blockModifier.solid !== undefined) {
        addProperty(this.contentPanel, 'Solid Override', blockModifier.solid ? 'Yes' : 'No');
      }
      if (blockModifier.transparent !== undefined) {
        addProperty(this.contentPanel, 'Transparent Override', blockModifier.transparent ? 'Yes' : 'No');
      }
      if (blockModifier.rotationX !== undefined || blockModifier.rotationY !== undefined || blockModifier.rotation !== undefined) {
        const rotX = blockModifier.rotationX ?? 0;
        const rotY = blockModifier.rotationY ?? blockModifier.rotation ?? 0;
        addProperty(this.contentPanel, 'Rotation', `X: ${rotX}°, Y: ${rotY}°`);
      }
      if (blockModifier.scale) {
        addProperty(this.contentPanel, 'Scale', `X: ${blockModifier.scale[0]}, Y: ${blockModifier.scale[1]}, Z: ${blockModifier.scale[2]}`);
      }
      if (blockModifier.color) {
        const colorHex = `#${blockModifier.color.map(c => c.toString(16).padStart(2, '0')).join('')}`;
        addProperty(this.contentPanel, 'Color', colorHex);
      }
      if (blockModifier.facing !== undefined) {
        const facingNames = ['Down', 'Up', 'North', 'South', 'East', 'West'];
        addProperty(this.contentPanel, 'Facing', facingNames[blockModifier.facing] || `${blockModifier.facing}`);
      }
      addSpacer(this.contentPanel);
    }

    addSectionTitle(this.contentPanel, 'Block Type (Definition)');
    addProperty(this.contentPanel, 'ID', `${blockType.id}`);
    addProperty(this.contentPanel, 'Name', blockType.name);
    if (blockType.displayName) {
      addProperty(this.contentPanel, 'Display Name', blockType.displayName);
    }
    addProperty(this.contentPanel, 'Shape', this.getShapeName(blockType.shape));
    addProperty(this.contentPanel, 'Texture', this.getTextureInfo(blockType.texture));
    addProperty(this.contentPanel, 'Transparent', blockType.transparent ? 'Yes' : 'No');
    addProperty(this.contentPanel, 'Solid', blockType.solid !== false ? 'Yes' : 'No');
    if (blockType.unbreakable) {
      addProperty(this.contentPanel, 'Unbreakable', 'Yes');
    }
    addSpacer(this.contentPanel);

    if (blockType.options) {
      addSectionTitle(this.contentPanel, 'Block Options (Type Definition)');
      if (blockType.options.material) {
        addProperty(this.contentPanel, 'Material', blockType.options.material);
      }
      if (blockType.options.solid !== undefined) {
        addProperty(this.contentPanel, 'Solid (options)', blockType.options.solid ? 'Yes' : 'No');
      }
      if (blockType.options.opaque !== undefined) {
        addProperty(this.contentPanel, 'Opaque', blockType.options.opaque ? 'Yes' : 'No');
      }
      if (blockType.options.transparent !== undefined) {
        addProperty(this.contentPanel, 'Transparent (options)', blockType.options.transparent ? 'Yes' : 'No');
      }
      if (blockType.options.fluid) {
        addProperty(this.contentPanel, 'Fluid', 'Yes');
        if (blockType.options.fluidDensity !== undefined) {
          addProperty(this.contentPanel, 'Fluid Density', `${blockType.options.fluidDensity}`);
        }
        if (blockType.options.viscosity !== undefined) {
          addProperty(this.contentPanel, 'Viscosity', `${blockType.options.viscosity}`);
        }
      }
      addSpacer(this.contentPanel);
    }
  }

  /**
   * Render Block Editor tab (editable form)
   */
  private renderBlockEditor(): void {
    if (!this.selectedBlock) {
      addText(this.contentPanel, 'No block selected', '#888888');
      addText(this.contentPanel, 'Select a block or press "," to create new', '#888888', 12);
      return;
    }

    // Use the block ID from selectedBlock (which comes from BlockSelector)
    // This is the actual block the user selected
    const currentBlockId = this.selectedBlock.blockId;

    console.log(`[BlockEditor] Rendering editor for block ID ${currentBlockId} at (${this.selectedBlock.blockX}, ${this.selectedBlock.blockY}, ${this.selectedBlock.blockZ})`);

    const blockType = this.registry.getBlockByID(currentBlockId);

    if (!blockType) {
      addText(this.contentPanel, `Unknown block ID: ${currentBlockId}`, '#FF0000');
      return;
    }

    // Toolbar at the top with Paste button
    const toolbarContainer = new StackPanel('editorToolbar');
    toolbarContainer.isVertical = false;
    toolbarContainer.height = '40px';
    toolbarContainer.width = 1;
    toolbarContainer.paddingBottom = 10;

    const pasteBtn = Button.CreateSimpleButton('pasteBlock', this.copyPasteBuffer ? 'Paste' : '-');
    pasteBtn.width = '80px';
    pasteBtn.height = '30px';
    pasteBtn.color = '#FFFFFF';
    pasteBtn.background = this.copyPasteBuffer ? '#00AA00' : '#666666';
    pasteBtn.thickness = 1;
    pasteBtn.fontSize = 14;
    pasteBtn.horizontalAlignment = Control.HORIZONTAL_ALIGNMENT_LEFT;
    pasteBtn.isEnabled = !!this.copyPasteBuffer;
    if (this.copyPasteBuffer) {
      pasteBtn.onPointerClickObservable.add(() => {
        this.pasteBlock();
      });
    }
    toolbarContainer.addControl(pasteBtn);

    this.contentPanel.addControl(toolbarContainer);

    // Position (read-only)
    addSectionTitle(this.contentPanel, 'Block Instance Editor');
    addProperty(this.contentPanel, 'Position', `(${this.selectedBlock.blockX}, ${this.selectedBlock.blockY}, ${this.selectedBlock.blockZ})`);
    addSpacer(this.contentPanel);

    // ============================================================
    // 1. BLOCK ID / TYPE SELECTOR (Dropdown)
    // ============================================================
    addLabel(this.contentPanel, 'Block Type:');
    const allBlocks = this.registry.getAllBlocks();
    const blockOptions = allBlocks.map(b => `${b.id}: ${b.name}`);
    const currentSelection = `${blockType.id}: ${blockType.name}`;

    // Simplified dropdown (using buttons for first few options)
    addDropdown(this.contentPanel, this.advancedTexture, blockOptions, currentSelection, (newValue) => {
      const newBlockId = parseInt(newValue.split(':')[0]);
      if (!isNaN(newBlockId)) {
        this.trackBlockModification(newBlockId);
      }
    });
    addSpacer(this.contentPanel);

    // ============================================================
    // 2. EDGE OFFSETS (Collapsible)
    // ============================================================
    addCollapsibleHeader(this.contentPanel, 'Edge Offsets', this.edgeOffsetsExpanded, () => {
      this.edgeOffsetsExpanded = !this.edgeOffsetsExpanded;
      this.refreshContent();
    });

    if (this.edgeOffsetsExpanded) {
      addText(this.contentPanel, '8 corners, 3 offset values (x,y,z) each: -127 to 128', '#888888', 11);
      addSpacer(this.contentPanel);

      // Load current edge offsets from chunk
      if (!this.currentEdgeOffsets) {
        const loadedOffsets = this.chunkManager.getBlockEdgeOffsets(
          this.selectedBlock.blockX,
          this.selectedBlock.blockY,
          this.selectedBlock.blockZ
        );
        this.currentEdgeOffsets = loadedOffsets || new Array(24).fill(0);
        console.log(`[BlockEditor] Loaded edge offsets:`, this.currentEdgeOffsets);
      }

      const cornerNames = [
        'Corner 0 (left-back-bottom)',
        'Corner 1 (right-back-bottom)',
        'Corner 2 (right-front-bottom)',
        'Corner 3 (left-front-bottom)',
        'Corner 4 (left-back-top)',
        'Corner 5 (right-back-top)',
        'Corner 6 (right-front-top)',
        'Corner 7 (left-front-top)'
      ];

      for (let i = 0; i < 8; i++) {
        addLabel(this.contentPanel, cornerNames[i]);

        // X, Y, Z offsets in one row
        addEdgeOffsetRow(
          this.contentPanel,
          this.currentEdgeOffsets[i * 3],
          this.currentEdgeOffsets[i * 3 + 1],
          this.currentEdgeOffsets[i * 3 + 2],
          (x, y, z) => {
            this.currentEdgeOffsets![i * 3] = x ?? 0;
            this.currentEdgeOffsets![i * 3 + 1] = y ?? 0;
            this.currentEdgeOffsets![i * 3 + 2] = z ?? 0;

            // Apply changes immediately to chunk
            console.log(`[BlockEditor] Edge offset changed for corner ${i}: (${x}, ${y}, ${z})`);
            this.applyEdgeOffsets();
          }
        );

        addSpacer(this.contentPanel);
      }

      // Clear edge offsets button
      const clearEdgeBtn = Button.CreateSimpleButton('clearEdgeOffsets', 'Clear All Edge Offsets');
      clearEdgeBtn.width = 1;
      clearEdgeBtn.height = '30px';
      clearEdgeBtn.color = '#FFFFFF';
      clearEdgeBtn.background = '#666600';
      clearEdgeBtn.onPointerClickObservable.add(() => {
        this.currentEdgeOffsets = new Array(24).fill(0);
        this.applyEdgeOffsets();
        this.refreshContent();
      });
      this.contentPanel.addControl(clearEdgeBtn);
      addSpacer(this.contentPanel);
    }

    // ============================================================
    // 3. MODIFIER (Collapsible)
    // ============================================================
    // Load existing modifier from tracked blocks OR from chunk if not tracked yet
    const key = `${this.selectedBlock.blockX},${this.selectedBlock.blockY},${this.selectedBlock.blockZ}`;
    const tracked = this.modifiedBlocks.get(key);

    // Load modifier from chunk
    const chunkModifier = this.chunkManager.getBlockModifier(
      this.selectedBlock.blockX,
      this.selectedBlock.blockY,
      this.selectedBlock.blockZ
    );

    // If we have tracked modifications, merge them with chunk data
    // This ensures we don't lose partial edits during refreshContent()
    if (tracked?.modifier && chunkModifier) {
      // Merge: tracked modifier overrides chunk modifier
      this.currentModifier = { ...chunkModifier, ...tracked.modifier };
    } else if (tracked?.modifier) {
      this.currentModifier = tracked.modifier;
    } else {
      this.currentModifier = chunkModifier;
    }

    if (this.currentModifier) {
      console.log(`[BlockEditor] Loaded modifier:`, this.currentModifier);
    }

    addCollapsibleHeader(this.contentPanel, 'Modifier', this.modifierExpanded, () => {
      this.modifierExpanded = !this.modifierExpanded;
      this.refreshContent();
    });

    if (this.modifierExpanded) {
      addText(this.contentPanel, 'Override BlockType properties for this instance', '#888888', 11);
      addSpacer(this.contentPanel);

      // shape (dropdown)
      addLabel(this.contentPanel, 'Shape:');
      const shapeOptions = ['CUBE', 'CROSS', 'HASH', 'MODEL', 'GLASS', 'FLAT', 'SPHERE', 'COLUMN', 'ROUND_CUBE', 'STEPS', 'STAIR', 'BILLBOARD', 'SPRITE', 'FLAME'];
      const currentShape = this.currentModifier?.shape !== undefined
        ? this.getShapeName(this.currentModifier.shape)
        : this.getShapeName(blockType.shape);
      addDropdown(this.contentPanel, this.advancedTexture, shapeOptions, currentShape, (newValue) => {
        const shape = this.parseShapeName(newValue);
        this.updateModifier('shape', shape);
      });

      // texture (dropdown + free input)
      addLabel(this.contentPanel, 'Texture Path:');
      addInputField(
        this.contentPanel,
        this.currentModifier?.texture
          ? (Array.isArray(this.currentModifier.texture)
              ? this.currentModifier.texture.join(',')
              : this.currentModifier.texture)
          : '',
        (newValue) => this.updateModifier('texture', newValue || undefined)
      );

      // solid (checkbox)
      addCheckbox(
        this.contentPanel,
        'Solid',
        this.currentModifier?.solid ?? blockType.solid ?? true,
        (value) => this.updateModifier('solid', value)
      );

      // transparent (checkbox)
      addCheckbox(
        this.contentPanel,
        'Transparent',
        this.currentModifier?.transparent ?? blockType.transparent ?? false,
        (value) => this.updateModifier('transparent', value)
      );

      // unbreakable (checkbox)
      addCheckbox(
        this.contentPanel,
        'Unbreakable',
        this.currentModifier?.unbreakable ?? blockType.unbreakable ?? false,
        (value) => this.updateModifier('unbreakable', value)
      );

      // rotation (X and Y in one row)
      addLabel(this.contentPanel, 'Rotation (degrees, 0-360):');
      const rotX = this.currentModifier?.rotationX ?? blockType.rotationX ?? 0;
      const rotY = this.currentModifier?.rotationY ?? this.currentModifier?.rotation ?? blockType.rotationY ?? blockType.rotation ?? 0;
      addRotationRow(this.contentPanel, rotX, rotY, (x, y) => {
        if (x !== undefined) this.updateModifier('rotationX', x);
        if (y !== undefined) this.updateModifier('rotationY', y);
      });

      // facing (dropdown)
      addLabel(this.contentPanel, 'Facing:');
      const facingOptions = ['0: Down', '1: Up', '2: North', '3: South', '4: East', '5: West'];
      const currentFacing = this.currentModifier?.facing !== undefined
        ? `${this.currentModifier.facing}: ...`
        : '0: Down';
      addDropdown(this.contentPanel, this.advancedTexture, facingOptions, currentFacing, (newValue) => {
        const facing = parseInt(newValue.split(':')[0]);
        this.updateModifier('facing', facing);
      });

      // color (Hex input + ColorPicker button)
      addLabel(this.contentPanel, 'Color (hex, e.g. #FF0000):');
      const colorHex = this.currentModifier?.color
        ? `#${this.currentModifier.color.map(c => c.toString(16).padStart(2, '0')).join('')}`
        : '';
      addColorPickerRow(this.contentPanel, this.advancedTexture, colorHex, (newValue) => {
        if (newValue.startsWith('#') && newValue.length === 7) {
          const r = parseInt(newValue.substr(1, 2), 16);
          const g = parseInt(newValue.substr(3, 2), 16);
          const b = parseInt(newValue.substr(5, 2), 16);
          this.updateModifier('color', [r, g, b] as [number, number, number]);
        } else if (newValue === '') {
          this.updateModifier('color', undefined);
        }
      });

      // scale (x, y, z floats)
      addLabel(this.contentPanel, 'Scale:');
      const scaleX = this.currentModifier?.scale?.[0] ?? 1;
      const scaleY = this.currentModifier?.scale?.[1] ?? 1;
      const scaleZ = this.currentModifier?.scale?.[2] ?? 1;

      // X, Y, Z scale in one row
      addScaleRow(this.contentPanel, scaleX, scaleY, scaleZ, (x, y, z) => {
        this.updateModifier('scale', [x ?? 1, y ?? 1, z ?? 1] as [number, number, number]);
      });

      // windLeafiness (float 0-1)
      addNumberInput(
        this.contentPanel,
        'Wind Leafiness (0-1):',
        this.currentModifier?.windLeafiness ?? blockType.windLeafiness,
        0,
        1,
        (value) => this.updateModifier('windLeafiness', value)
      );

      // windStability (float 0-1)
      addNumberInput(
        this.contentPanel,
        'Wind Stability (0-1):',
        this.currentModifier?.windStability ?? blockType.windStability,
        0,
        1,
        (value) => this.updateModifier('windStability', value)
      );

      // windLeverUp (float 0-10)
      addNumberInput(
        this.contentPanel,
        'Wind Lever Up (0-10):',
        this.currentModifier?.windLeverUp ?? blockType.windLeverUp,
        0,
        10,
        (value) => this.updateModifier('windLeverUp', value)
      );

      // windLeverDown (float 0-10)
      addNumberInput(
        this.contentPanel,
        'Wind Lever Down (0-10):',
        this.currentModifier?.windLeverDown ?? blockType.windLeverDown,
        0,
        10,
        (value) => this.updateModifier('windLeverDown', value)
      );

      // spriteCount (integer 1-1000, only for SPRITE shape)
      addNumberInput(
        this.contentPanel,
        'Sprite Count (1-1000):',
        this.currentModifier?.spriteCount ?? blockType.spriteCount,
        1,
        1000,
        (value) => this.updateModifier('spriteCount', value)
      );

      addSpacer(this.contentPanel);

      // ============================================================
      // 3.1 BLOCK OPTIONS (Nested Collapsible within Modifier)
      // ============================================================
      addCollapsibleHeader(this.contentPanel, '  Block Options', this.blockOptionsExpanded, () => {
        this.blockOptionsExpanded = !this.blockOptionsExpanded;
        this.refreshContent();
      });

      if (this.blockOptionsExpanded) {
        addText(this.contentPanel, 'Override BlockOptions for this instance', '#888888', 11);
        addSpacer(this.contentPanel);

        const currentOptions = this.currentModifier?.options || {};

        // solid (checkbox)
        addCheckbox(
          this.contentPanel,
          '  Solid (options)',
          currentOptions.solid ?? blockType.options?.solid ?? true,
          (value) => {
            const newOptions = { ...currentOptions, solid: value };
            this.updateModifier('options', newOptions);
          }
        );

        // opaque (checkbox)
        addCheckbox(
          this.contentPanel,
          '  Opaque',
          currentOptions.opaque ?? blockType.options?.opaque ?? true,
          (value) => {
            const newOptions = { ...currentOptions, opaque: value };
            this.updateModifier('options', newOptions);
          }
        );

        // transparent (checkbox)
        addCheckbox(
          this.contentPanel,
          '  Transparent (options)',
          currentOptions.transparent ?? blockType.options?.transparent ?? false,
          (value) => {
            const newOptions = { ...currentOptions, transparent: value };
            this.updateModifier('options', newOptions);
          }
        );

        // material (dropdown)
        addLabel(this.contentPanel, '  Material:');
        const materialOptions = ['solid', 'water', 'lava', 'barrier', 'gas'];
        const currentMaterial = currentOptions.material || blockType.options?.material || 'solid';
        addDropdown(this.contentPanel, this.advancedTexture, materialOptions, currentMaterial, (newValue) => {
          const newOptions = { ...currentOptions, material: newValue as any };
          this.updateModifier('options', newOptions);
        });

        // fluid (checkbox)
        addCheckbox(
          this.contentPanel,
          '  Fluid',
          currentOptions.fluid ?? blockType.options?.fluid ?? false,
          (value) => {
            const newOptions = { ...currentOptions, fluid: value };
            this.updateModifier('options', newOptions);
          }
        );

        // fluidDensity (float)
        addNumberInput(
          this.contentPanel,
          '  Fluid Density:',
          currentOptions.fluidDensity ?? blockType.options?.fluidDensity,
          0,
          100,
          (value) => {
            const newOptions = { ...currentOptions, fluidDensity: value };
            this.updateModifier('options', newOptions);
          }
        );

        // viscosity (float)
        addNumberInput(
          this.contentPanel,
          '  Viscosity:',
          currentOptions.viscosity ?? blockType.options?.viscosity,
          0,
          1000,
          (value) => {
            const newOptions = { ...currentOptions, viscosity: value };
            this.updateModifier('options', newOptions);
          }
        );

        addSpacer(this.contentPanel);
      }

      // Clear modifier button
      const clearModifierBtn = Button.CreateSimpleButton('clearModifier', 'Clear All Modifiers');
      clearModifierBtn.width = 1;
      clearModifierBtn.height = '30px';
      clearModifierBtn.color = '#FFFFFF';
      clearModifierBtn.background = '#886600';
      clearModifierBtn.onPointerClickObservable.add(() => this.clearModifier());
      this.contentPanel.addControl(clearModifierBtn);
      addSpacer(this.contentPanel);
    }

    // ============================================================
    // 4. METADATA (Collapsible)
    // ============================================================
    // Always load metadata from chunk (it's the single source of truth)
    const chunkMetadata = this.chunkManager.getBlockMetadata(
      this.selectedBlock.blockX,
      this.selectedBlock.blockY,
      this.selectedBlock.blockZ
    );
    this.currentMetadata = chunkMetadata;

    if (this.currentMetadata) {
      console.log(`[BlockEditor] Loaded metadata:`, this.currentMetadata);
    }

    addCollapsibleHeader(this.contentPanel, 'Metadata', this.metadataExpanded, () => {
      this.metadataExpanded = !this.metadataExpanded;
      this.refreshContent();
    });

    if (this.metadataExpanded) {
      addText(this.contentPanel, 'Persistent per-instance block data', '#888888', 11);
      addSpacer(this.contentPanel);

      // displayName
      addLabel(this.contentPanel, 'Display Name:');
      addInputField(
        this.contentPanel,
        this.currentMetadata?.displayName || '',
        (newValue) => this.updateMetadata('displayName', newValue || undefined)
      );

      // groupId
      addLabel(this.contentPanel, 'Group ID:');
      addInputField(
        this.contentPanel,
        this.currentMetadata?.groupId || '',
        (newValue) => this.updateMetadata('groupId', newValue || undefined)
      );

      addSpacer(this.contentPanel);

      // Clear metadata button
      const clearMetadataBtn = Button.CreateSimpleButton('clearMetadata', 'Clear All Metadata');
      clearMetadataBtn.width = 1;
      clearMetadataBtn.height = '30px';
      clearMetadataBtn.color = '#FFFFFF';
      clearMetadataBtn.background = '#886600';
      clearMetadataBtn.onPointerClickObservable.add(() => this.clearMetadata());
      this.contentPanel.addControl(clearMetadataBtn);
      addSpacer(this.contentPanel);
    }

    // ============================================================
    // ACTIONS
    // ============================================================
    // Revert Block button
    const revertBtn = Button.CreateSimpleButton('revertBlock', 'Revert This Block');
    revertBtn.width = 1;
    revertBtn.height = '30px';
    revertBtn.color = '#FFFFFF';
    revertBtn.background = '#AA0000';
    revertBtn.onPointerClickObservable.add(() => this.revertBlock());
    this.contentPanel.addControl(revertBtn);

    addSpacer(this.contentPanel);
    addText(this.contentPanel, 'Changes are tracked live', '#00FF00', 11);
    addText(this.contentPanel, `Modified blocks: ${this.modifiedBlocks.size}`, '#FFFF00', 11);
  }

  /**
   * Render Block Type List tab
   */
  private renderBlockList(): void {
    addSectionTitle(this.contentPanel, 'All Registered Block-Types');

    const allBlocks = this.registry.getAllBlocks();

    if (allBlocks.length === 0) {
      addText(this.contentPanel, 'No block types registered', '#888888');
      return;
    }

    // Header
    addText(this.contentPanel, 'ID | Name | Shape | Material', '#00FF00', 12);
    addSpacer(this.contentPanel);

    // List all blocks
    for (const block of allBlocks) {
      const shape = this.getShapeName(block.shape);
      const material = block.options?.material || '-';
      addText(this.contentPanel, `${block.id} | ${block.name} | ${shape} | ${material}`, '#FFFFFF', 11);
    }
  }

  /**
   * Render Changes tab (modified block instances)
   */
  private renderChanges(): void {
    addSectionTitle(this.contentPanel, 'Modified Block-Instances');

    if (this.modifiedBlocks.size === 0) {
      addText(this.contentPanel, 'No changes pending', '#888888');
      addText(this.contentPanel, 'Edit blocks in the Block Editor tab', '#888888', 12);
      return;
    }

    addText(this.contentPanel, `Total changes: ${this.modifiedBlocks.size}`, '#FFFF00', 14);
    addSpacer(this.contentPanel);

    // Header
    addText(this.contentPanel, 'Position | Original → New | Status', '#00FF00', 12);
    addSpacer(this.contentPanel);

    // List all modified blocks
    for (const [key, modification] of this.modifiedBlocks.entries()) {
      const pos = `(${modification.position.x}, ${modification.position.y}, ${modification.position.z})`;

      // Use stored currentBlockId instead of reading from chunk
      const currentBlockId = modification.currentBlockId;

      // Get block type names
      const originalType = this.registry.getBlockByID(modification.originalBlockId);
      const currentType = this.registry.getBlockByID(currentBlockId);

      const originalName = originalType ? originalType.name : `ID ${modification.originalBlockId}`;
      const currentName = currentType ? currentType.name : `ID ${currentBlockId}`;

      // Status and color
      let statusText = '';
      let color = '#FFAA00';
      if (modification.status === 'new') {
        statusText = 'New';
        color = '#00FF00';
      } else if (modification.status === 'deleted') {
        statusText = 'Deleted';
        color = '#FF0000';
      } else {
        statusText = 'Modified';
        color = '#FFAA00';
      }

      // Display line
      addText(this.contentPanel, `${pos}`, '#AAAAAA', 11);
      addText(this.contentPanel, `  ${originalName} → ${currentName}`, '#FFFFFF', 11);
      addText(this.contentPanel, `  Status: ${statusText}`, color, 11);
      addSpacer(this.contentPanel);
    }

    // Action buttons
    const applyBtn = Button.CreateSimpleButton('applyChanges', 'Apply All Changes');
    applyBtn.width = 1;
    applyBtn.height = '35px';
    applyBtn.color = '#FFFFFF';
    applyBtn.background = '#00AA00';
    applyBtn.thickness = 1;
    applyBtn.fontSize = 14;
    applyBtn.onPointerClickObservable.add(() => this.applyAll());
    this.contentPanel.addControl(applyBtn);

    addSpacer(this.contentPanel);

    const revertBtn = Button.CreateSimpleButton('revertChanges', 'Revert All Changes');
    revertBtn.width = 1;
    revertBtn.height = '35px';
    revertBtn.color = '#FFFFFF';
    revertBtn.background = '#AA0000';
    revertBtn.thickness = 1;
    revertBtn.fontSize = 14;
    revertBtn.onPointerClickObservable.add(() => this.revertAll());
    this.contentPanel.addControl(revertBtn);
  }

  /**
   * Get shape name from shape enum value
   */
  private getShapeName(shape: number): string {
    const shapes = ['CUBE', 'CROSS', 'HASH', 'MODEL', 'GLASS', 'FLAT', 'SPHERE', 'COLUMN', 'ROUND_CUBE', 'STEPS', 'STAIR', 'BILLBOARD', 'SPRITE', 'FLAME'];
    return shapes[shape] || `Unknown (${shape})`;
  }

  /**
   * Get texture info string
   */
  private getTextureInfo(texture: string | string[]): string {
    if (Array.isArray(texture)) {
      return texture.length === 1 ? texture[0] : `Array (${texture.length} textures)`;
    }
    return texture;
  }

  /**
   * Update modifier property
   */
  private updateModifier(property: keyof BlockModifier, value: any): void {
    if (!this.selectedBlock) return;

    const key = `${this.selectedBlock.blockX},${this.selectedBlock.blockY},${this.selectedBlock.blockZ}`;
    let tracked = this.modifiedBlocks.get(key);

    // Initialize tracked block if not exists
    if (!tracked) {
      tracked = {
        position: {
          x: this.selectedBlock.blockX,
          y: this.selectedBlock.blockY,
          z: this.selectedBlock.blockZ,
        },
        status: 'modified',  // Mark as modified when adding modifier
        originalBlockId: this.selectedBlock.blockId,
        currentBlockId: this.selectedBlock.blockId,  // Initialize with current block ID
      };
      this.modifiedBlocks.set(key, tracked);
    }

    // Initialize modifier if not exists - copy existing chunk modifier first!
    if (!tracked.modifier) {
      // Load existing modifier from chunk to preserve all existing properties
      const chunkModifier = this.chunkManager.getBlockModifier(
        this.selectedBlock.blockX,
        this.selectedBlock.blockY,
        this.selectedBlock.blockZ
      );
      // Copy chunk modifier to tracked.modifier (or start with empty object)
      tracked.modifier = chunkModifier ? { ...chunkModifier } : {};
    }

    // Update modifier property
    if (value === undefined) {
      delete tracked.modifier[property];
    } else {
      (tracked.modifier as any)[property] = value;
    }

    // Update current modifier reference
    this.currentModifier = tracked.modifier;

    console.log(`[BlockEditor] Updated modifier.${property} = ${value} for block at ${key}`);

    // Apply modifier to chunk immediately (live preview)
    this.applyModifier();

    this.refreshContent();
  }

  /**
   * Apply current modifier to chunk
   */
  private applyModifier(): void {
    if (!this.selectedBlock) {
      return;
    }

    console.log(`[BlockEditor] Applying modifier to block at (${this.selectedBlock.blockX}, ${this.selectedBlock.blockY}, ${this.selectedBlock.blockZ})`);

    // Apply modifier via ChunkManager (this will also trigger re-render)
    this.chunkManager.updateBlockModifier(
      this.selectedBlock.blockX,
      this.selectedBlock.blockY,
      this.selectedBlock.blockZ,
      this.currentModifier
    );
  }

  /**
   * Clear all modifiers for current block
   */
  private clearModifier(): void {
    if (!this.selectedBlock) return;

    const key = `${this.selectedBlock.blockX},${this.selectedBlock.blockY},${this.selectedBlock.blockZ}`;
    const tracked = this.modifiedBlocks.get(key);

    if (tracked) {
      tracked.modifier = undefined;
      this.currentModifier = null;
      console.log(`[BlockEditor] Cleared modifiers for block at ${key}`);

      // Apply null modifier to chunk to clear it
      this.chunkManager.updateBlockModifier(
        this.selectedBlock.blockX,
        this.selectedBlock.blockY,
        this.selectedBlock.blockZ,
        null
      );

      this.refreshContent();
    }
  }

  /**
   * Update metadata property
   */
  private updateMetadata(property: string, value: any): void {
    if (!this.selectedBlock) return;

    const key = `${this.selectedBlock.blockX},${this.selectedBlock.blockY},${this.selectedBlock.blockZ}`;

    // Initialize metadata if not exists
    if (!this.currentMetadata) {
      this.currentMetadata = {};
    }

    // Update metadata property
    if (value === undefined) {
      delete this.currentMetadata[property];
    } else {
      this.currentMetadata[property] = value;
    }

    console.log(`[BlockEditor] Updated metadata.${property} = ${value} for block at ${key}`);

    // Apply metadata to chunk immediately (live preview)
    this.applyMetadata();

    this.refreshContent();
  }

  /**
   * Apply current metadata to chunk
   */
  private applyMetadata(): void {
    if (!this.selectedBlock) {
      return;
    }

    console.log(`[BlockEditor] Applying metadata to block at (${this.selectedBlock.blockX}, ${this.selectedBlock.blockY}, ${this.selectedBlock.blockZ})`);

    // Apply metadata via ChunkManager
    this.chunkManager.updateBlockMetadata(
      this.selectedBlock.blockX,
      this.selectedBlock.blockY,
      this.selectedBlock.blockZ,
      this.currentMetadata
    );
  }

  /**
   * Clear all metadata for current block
   */
  private clearMetadata(): void {
    if (!this.selectedBlock) return;

    const key = `${this.selectedBlock.blockX},${this.selectedBlock.blockY},${this.selectedBlock.blockZ}`;

    this.currentMetadata = null;
    console.log(`[BlockEditor] Cleared metadata for block at ${key}`);

    // Apply null metadata to chunk to clear it
    this.chunkManager.updateBlockMetadata(
      this.selectedBlock.blockX,
      this.selectedBlock.blockY,
      this.selectedBlock.blockZ,
      null
    );

    this.refreshContent();
  }

  /**
   * Parse shape name to shape enum value
   */
  private parseShapeName(shapeName: string): number | undefined {
    if (!shapeName) return undefined;

    const shapeMap: { [key: string]: number } = {
      'CUBE': 0,
      'CROSS': 1,
      'HASH': 2,
      'MODEL': 3,
      'GLASS': 4,
      'FLAT': 5,
      'SPHERE': 6,
      'COLUMN': 7,
      'ROUND_CUBE': 8,
      'STEPS': 9,
      'STAIR': 10,
      'BILLBOARD': 11,
      'SPRITE': 12,
      'FLAME': 13,
    };

    const normalized = shapeName.toUpperCase().trim();
    return shapeMap[normalized];
  }

  /**
   * Dispose editor
   */
  dispose(): void {
    this.container.dispose();
  }
}
