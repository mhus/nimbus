// This is a temporary file for the new renderBlockEditor implementation
// Will be integrated into BlockEditor.ts

  /**
   * Render Block Editor tab (editable form) - COMPLETE NEW VERSION
   */
  private renderBlockEditor(): void {
    if (!this.selectedBlock) {
      this.addText('No block selected', '#888888');
      this.addText('Select a block or press "," to create new', '#888888', 12);
      return;
    }

    const blockType = this.registry.getBlockByID(this.selectedBlock.blockId);
    if (!blockType) {
      this.addText(`Unknown block ID: ${this.selectedBlock.blockId}`, '#FF0000');
      return;
    }

    // Position (read-only)
    this.addSectionTitle('Block Instance Editor');
    this.addProperty('Position', `(${this.selectedBlock.blockX}, ${this.selectedBlock.blockY}, ${this.selectedBlock.blockZ})`);
    this.addSpacer();

    // ============================================================
    // 1. BLOCK ID / TYPE SELECTOR (Dropdown)
    // ============================================================
    this.addLabel('Block Type:');
    const allBlocks = this.registry.getAllBlocks();
    const blockOptions = allBlocks.map(b => `${b.id}: ${b.name}`);
    const currentSelection = `${blockType.id}: ${blockType.name}`;

    // Simplified dropdown (using buttons for first few options)
    this.addDropdown(blockOptions, currentSelection, (newValue) => {
      const newBlockId = parseInt(newValue.split(':')[0]);
      if (!isNaN(newBlockId)) {
        this.trackBlockModification(newBlockId);
      }
    });
    this.addSpacer();

    // ============================================================
    // 2. EDGE OFFSETS (Collapsible)
    // ============================================================
    this.addCollapsibleHeader('Edge Offsets', this.edgeOffsetsExpanded, () => {
      this.edgeOffsetsExpanded = !this.edgeOffsetsExpanded;
      this.refreshContent();
    });

    if (this.edgeOffsetsExpanded) {
      this.addText('8 corners, 3 offset values (x,y,z) each: -127 to 128', '#888888', 11);
      this.addSpacer();

      // Load current edge offsets
      // Note: We need chunk reference to get edge offsets - simplified for now
      if (!this.currentEdgeOffsets) {
        this.currentEdgeOffsets = new Array(24).fill(0);
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
        this.addLabel(cornerNames[i]);

        // X offset
        this.addNumberInput(
          `  X offset:`,
          this.currentEdgeOffsets[i * 3],
          -127,
          128,
          (value) => {
            this.currentEdgeOffsets![i * 3] = value ?? 0;
          }
        );

        // Y offset
        this.addNumberInput(
          `  Y offset:`,
          this.currentEdgeOffsets[i * 3 + 1],
          -127,
          128,
          (value) => {
            this.currentEdgeOffsets![i * 3 + 1] = value ?? 0;
          }
        );

        // Z offset
        this.addNumberInput(
          `  Z offset:`,
          this.currentEdgeOffsets[i * 3 + 2],
          -127,
          128,
          (value) => {
            this.currentEdgeOffsets![i * 3 + 2] = value ?? 0;
          }
        );

        this.addSpacer();
      }

      // Clear edge offsets button
      const clearEdgeBtn = Button.CreateSimpleButton('clearEdgeOffsets', 'Clear All Edge Offsets');
      clearEdgeBtn.width = 1;
      clearEdgeBtn.height = '30px';
      clearEdgeBtn.color = '#FFFFFF';
      clearEdgeBtn.background = '#666600';
      clearEdgeBtn.onPointerClickObservable.add(() => {
        this.currentEdgeOffsets = null;
        this.refreshContent();
      });
      this.contentPanel.addControl(clearEdgeBtn);
      this.addSpacer();
    }

    // ============================================================
    // 3. MODIFIER (Collapsible)
    // ============================================================
    const key = `${this.selectedBlock.blockX},${this.selectedBlock.blockY},${this.selectedBlock.blockZ}`;
    const tracked = this.modifiedBlocks.get(key);
    this.currentModifier = tracked?.modifier || null;

    this.addCollapsibleHeader('Modifier', this.modifierExpanded, () => {
      this.modifierExpanded = !this.modifierExpanded;
      this.refreshContent();
    });

    if (this.modifierExpanded) {
      this.addText('Override BlockType properties for this instance', '#888888', 11);
      this.addSpacer();

      // displayName
      this.addLabel('Display Name:');
      this.addInputField(
        this.currentModifier?.displayName || '',
        (newValue) => this.updateModifier('displayName', newValue || undefined)
      );

      // shape (dropdown)
      this.addLabel('Shape:');
      const shapeOptions = ['CUBE', 'CROSS', 'HASH', 'MODEL', 'GLASS', 'FLAT', 'SPHERE', 'COLUMN', 'ROUND_CUBE'];
      const currentShape = this.currentModifier?.shape !== undefined
        ? this.getShapeName(this.currentModifier.shape)
        : this.getShapeName(blockType.shape);
      this.addDropdown(shapeOptions, currentShape, (newValue) => {
        const shape = this.parseShapeName(newValue);
        this.updateModifier('shape', shape);
      });

      // texture (dropdown + free input)
      this.addLabel('Texture Path:');
      this.addInputField(
        this.currentModifier?.texture
          ? (Array.isArray(this.currentModifier.texture)
              ? this.currentModifier.texture.join(',')
              : this.currentModifier.texture)
          : '',
        (newValue) => this.updateModifier('texture', newValue || undefined)
      );

      // solid (checkbox)
      this.addCheckbox(
        'Solid',
        this.currentModifier?.solid ?? blockType.solid ?? true,
        (value) => this.updateModifier('solid', value)
      );

      // transparent (checkbox)
      this.addCheckbox(
        'Transparent',
        this.currentModifier?.transparent ?? blockType.transparent ?? false,
        (value) => this.updateModifier('transparent', value)
      );

      // unbreakable (checkbox)
      this.addCheckbox(
        'Unbreakable',
        this.currentModifier?.unbreakable ?? blockType.unbreakable ?? false,
        (value) => this.updateModifier('unbreakable', value)
      );

      // rotation (dropdown)
      this.addLabel('Rotation (degrees):');
      const rotationOptions = ['0', '45', '90', '135', '180', '225', '270', '315'];
      const currentRotation = this.currentModifier?.rotation?.toString() || '0';
      this.addDropdown(rotationOptions, currentRotation, (newValue) => {
        this.updateModifier('rotation', parseFloat(newValue));
      });

      // facing (dropdown)
      this.addLabel('Facing:');
      const facingOptions = ['0: Down', '1: Up', '2: North', '3: South', '4: East', '5: West'];
      const currentFacing = this.currentModifier?.facing !== undefined
        ? `${this.currentModifier.facing}: ...`
        : '0: Down';
      this.addDropdown(facingOptions, currentFacing, (newValue) => {
        const facing = parseInt(newValue.split(':')[0]);
        this.updateModifier('facing', facing);
      });

      // color (hex input)
      this.addLabel('Color (hex, e.g. #FF0000):');
      const colorHex = this.currentModifier?.color
        ? `#${this.currentModifier.color.map(c => c.toString(16).padStart(2, '0')).join('')}`
        : '';
      this.addInputField(colorHex, (newValue) => {
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
      this.addLabel('Scale:');
      const scaleX = this.currentModifier?.scale?.[0] ?? 1;
      const scaleY = this.currentModifier?.scale?.[1] ?? 1;
      const scaleZ = this.currentModifier?.scale?.[2] ?? 1;

      this.addNumberInput('  Scale X:', scaleX, 0.1, 10, (value) => {
        const current = this.currentModifier?.scale || [1, 1, 1];
        this.updateModifier('scale', [value ?? 1, current[1], current[2]] as [number, number, number]);
      });

      this.addNumberInput('  Scale Y:', scaleY, 0.1, 10, (value) => {
        const current = this.currentModifier?.scale || [1, 1, 1];
        this.updateModifier('scale', [current[0], value ?? 1, current[2]] as [number, number, number]);
      });

      this.addNumberInput('  Scale Z:', scaleZ, 0.1, 10, (value) => {
        const current = this.currentModifier?.scale || [1, 1, 1];
        this.updateModifier('scale', [current[0], current[1], value ?? 1] as [number, number, number]);
      });

      this.addSpacer();

      // ============================================================
      // 3.1 BLOCK OPTIONS (Nested Collapsible within Modifier)
      // ============================================================
      this.addCollapsibleHeader('  Block Options', this.blockOptionsExpanded, () => {
        this.blockOptionsExpanded = !this.blockOptionsExpanded;
        this.refreshContent();
      });

      if (this.blockOptionsExpanded) {
        this.addText('Override BlockOptions for this instance', '#888888', 11);
        this.addSpacer();

        const currentOptions = this.currentModifier?.options || {};

        // solid (checkbox)
        this.addCheckbox(
          '  Solid (options)',
          currentOptions.solid ?? blockType.options?.solid ?? true,
          (value) => {
            const newOptions = { ...currentOptions, solid: value };
            this.updateModifier('options', newOptions);
          }
        );

        // opaque (checkbox)
        this.addCheckbox(
          '  Opaque',
          currentOptions.opaque ?? blockType.options?.opaque ?? true,
          (value) => {
            const newOptions = { ...currentOptions, opaque: value };
            this.updateModifier('options', newOptions);
          }
        );

        // transparent (checkbox)
        this.addCheckbox(
          '  Transparent (options)',
          currentOptions.transparent ?? blockType.options?.transparent ?? false,
          (value) => {
            const newOptions = { ...currentOptions, transparent: value };
            this.updateModifier('options', newOptions);
          }
        );

        // material (dropdown)
        this.addLabel('  Material:');
        const materialOptions = ['solid', 'water', 'lava', 'barrier', 'gas'];
        const currentMaterial = currentOptions.material || blockType.options?.material || 'solid';
        this.addDropdown(materialOptions, currentMaterial, (newValue) => {
          const newOptions = { ...currentOptions, material: newValue as any };
          this.updateModifier('options', newOptions);
        });

        // fluid (checkbox)
        this.addCheckbox(
          '  Fluid',
          currentOptions.fluid ?? blockType.options?.fluid ?? false,
          (value) => {
            const newOptions = { ...currentOptions, fluid: value };
            this.updateModifier('options', newOptions);
          }
        );

        // fluidDensity (float)
        this.addNumberInput(
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
        this.addNumberInput(
          '  Viscosity:',
          currentOptions.viscosity ?? blockType.options?.viscosity,
          0,
          1000,
          (value) => {
            const newOptions = { ...currentOptions, viscosity: value };
            this.updateModifier('options', newOptions);
          }
        );

        this.addSpacer();
      }

      // Clear modifier button
      const clearModifierBtn = Button.CreateSimpleButton('clearModifier', 'Clear All Modifiers');
      clearModifierBtn.width = 1;
      clearModifierBtn.height = '30px';
      clearModifierBtn.color = '#FFFFFF';
      clearModifierBtn.background = '#886600';
      clearModifierBtn.onPointerClickObservable.add(() => this.clearModifier());
      this.contentPanel.addControl(clearModifierBtn);
      this.addSpacer();
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

    this.addSpacer();
    this.addText('Changes are tracked live', '#00FF00', 11);
    this.addText(`Modified blocks: ${this.modifiedBlocks.size}`, '#FFFF00', 11);
  }
