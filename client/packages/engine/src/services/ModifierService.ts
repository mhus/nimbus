/**
 * ModifierService - Manages modifier stacks
 *
 * A modifier system that allows multiple values to override a base value with priorities.
 * Higher priority modifiers override lower priority ones.
 * When modifiers have the same priority, the most recently created one wins.
 */

import { getLogger, ExceptionHandler } from '@nimbus/shared';

const logger = getLogger('ModifierService');

/** Maximum priority value for default modifiers */
const MAX_PRIORITY = Number.MAX_SAFE_INTEGER;

/**
 * Enum für alle zentral verwalteten Stack-Namen
 *
 * WICHTIG: Alle neuen StackModifier müssen hier registriert werden!
 *
 * Workflow für neue Stacks:
 * 1. Stack-Namen hier als Enum-Wert hinzufügen
 * 2. Stack in StackModifierCreator.createAllStackModifiers() erstellen
 * 3. In Service nur noch per getModifierStack(StackName.XXX) holen
 *
 * Vorteile:
 * - Typsicherheit (keine Tippfehler)
 * - Zentrale Initialisierung garantiert Verfügbarkeit
 * - IDE-Autovervollständigung
 * - Alle Stacks an einem Ort dokumentiert
 *
 * @example
 * // Im Creator (StackModifierCreator.ts):
 * modifierService.createModifierStack<boolean>(
 *   StackName.PLAYER_VIEW_MODE,
 *   true,
 *   (value) => {
 *     const playerService = appContext.services.player;
 *     if (playerService) playerService.onViewModeChanged(value);
 *   }
 * );
 *
 * @example
 * // Im Service (z.B. PlayerService.ts):
 * get viewModeStack(): ModifierStack<boolean> | undefined {
 *   return this.appContext.services.modifier?.getModifierStack<boolean>(
 *     StackName.PLAYER_VIEW_MODE
 *   );
 * }
 */
export enum StackName {
  /** Player view mode: true = ego-view (first-person), false = third-person */
  PLAYER_VIEW_MODE = 'playerViewMode',

  /** Player movement state: WALK, SPRINT, JUMP, FALL, FLY, SWIM, CROUCH, RIDING */
  PLAYER_MOVEMENT_STATE = 'playerMovementState',

  // Weitere Stacks hier hinzufügen
}

/**
 * Modifier - A value that can be applied to a ModifierStack
 * @template T The type of the value
 */
export class Modifier<T> {
  private _value: T;
  private readonly _prio: number;
  private readonly _created: number;
  private readonly _stack: ModifierStack<T>;
  private _enabled: boolean = true;

  /**
   * Create a new modifier
   * @param value The initial value
   * @param prio The priority (higher values win)
   * @param stack The owning stack
   */
  constructor(value: T, prio: number, stack: ModifierStack<T>) {
    this._value = value;
    this._prio = prio;
    this._created = Date.now();
    this._stack = stack;
  }

  /**
   * Get the current value
   */
  get value(): T {
    return this._value;
  }

  /**
   * Get the current value (alias for value getter)
   */
  getValue(): T {
    return this._value;
  }

  /**
   * Set a new value
   * @param value The new value
   */
  setValue(value: T): void {
    this._value = value;
    this._stack.update(false);
  }

  /**
   * Get the priority
   */
  get prio(): number {
    return this._prio;
  }

  /**
   * Get the creation timestamp
   */
  get created(): number {
    return this._created;
  }

  /**
   * Get enabled state
   */
  get enabled(): boolean {
    return this._enabled;
  }

  /**
   * Set enabled state
   * When disabled, the modifier is ignored in stack calculations
   */
  setEnabled(enabled: boolean): void {
    if (this._enabled !== enabled) {
      this._enabled = enabled;
      this._stack.update(false);
    }
  }

  /**
   * Close this modifier (remove from stack)
   */
  close(): void {
    this._stack.removeModifier(this);
  }
}

/**
 * ModifierStack - Manages a prioritized list of modifiers
 * @template T The type of the values
 */
export class ModifierStack<T> {
  private readonly _stackName: string;
  private readonly _defaultModifier: Modifier<T>;
  private readonly _action: (value: T) => void;
  private readonly _modifiers: Modifier<T>[] = [];
  private _currentValue: T;
  private readonly _service: ModifierService;

  /**
   * Create a new modifier stack
   * @param stackName The name of this stack
   * @param defaultValue The default value (fallback)
   * @param action The action to execute when the value changes
   * @param service The ModifierService that owns this stack
   */
  constructor(stackName: string, defaultValue: T, action: (value: T) => void, service: ModifierService) {
    this._stackName = stackName;
    this._action = action;
    this._service = service;
    this._defaultModifier = new Modifier(defaultValue, MAX_PRIORITY, this);
    this._currentValue = defaultValue;
  }

  /**
   * Get the stack name
   */
  get stackName(): string {
    return this._stackName;
  }

  /**
   * Get the default modifier
   */
  getDefaultModifier(): Modifier<T> {
    return this._defaultModifier;
  }

  /**
   * Add a modifier to the stack
   * @param value The value
   * @param prio The priority
   * @returns The created modifier
   */
  addModifier(value: T, prio: number): Modifier<T> {
    const modifier = new Modifier(value, prio, this);
    this._modifiers.push(modifier);
    this.update(false);
    return modifier;
  }

  /**
   * Remove a modifier from the stack
   * @param modifier The modifier to remove
   */
  removeModifier(modifier: Modifier<T>): void {
    const index = this._modifiers.indexOf(modifier);
    if (index !== -1) {
      this._modifiers.splice(index, 1);
      this.update(false);
    }
  }

  /**
   * Update the current value
   * @param force Force the action to execute even if the value hasn't changed
   */
  update(force: boolean): void {
    try {
      const newValue = this._calculateValue();
      const valueChanged = newValue !== this._currentValue;

      if (valueChanged || force) {
        this._currentValue = newValue;
        this._action(newValue);

        logger.debug('ModifierStack updated', {
          stackName: this._stackName,
          newValue,
          forced: force,
          modifierCount: this._modifiers.length,
        });
      }
    } catch (error) {
      throw ExceptionHandler.handleAndRethrow(
        error,
        'ModifierStack.update',
        { stackName: this._stackName, force }
      );
    }
  }

  /**
   * Calculate the current value by selecting the highest priority modifier
   * @returns The current value
   */
  private _calculateValue(): T {
    // Filter out disabled modifiers
    const enabledModifiers = this._modifiers.filter(m => m.enabled);

    // If no enabled modifiers, use default
    if (enabledModifiers.length === 0) {
      return this._defaultModifier.value;
    }

    // Sort by priority (descending), then by created (descending for newest first)
    // Lower priority value = higher priority, so we sort ascending by prio
    const sorted = [...enabledModifiers, this._defaultModifier].sort((a, b) => {
      if (a.prio !== b.prio) {
        return a.prio - b.prio; // Lower prio value = higher priority
      }
      return b.created - a.created; // Newer wins
    });

    // The first one wins
    return sorted[0].value;
  }

  /**
   * Get the current value
   */
  get currentValue(): T {
    return this._currentValue;
  }

  /**
   * Get the current value (alias for currentValue getter)
   */
  getValue(): T {
    return this._currentValue;
  }

  /**
   * Get all modifiers (excluding default)
   */
  get modifiers(): readonly Modifier<T>[] {
    return this._modifiers;
  }

  /**
   * Dispose the stack
   * Clears all modifiers and removes itself from the ModifierService
   */
  dispose(): void {
    this._modifiers.length = 0;
    this._service._removeStackFromMap(this._stackName);
  }
}

/**
 * Modifier configuration for creating a modifier
 */
export interface ModifierConfig<T> {
  /** The value */
  value: T;
  /** The priority (lower values = higher priority) */
  prio: number;
}

/**
 * ModifierService - Central service for managing modifier stacks
 */
export class ModifierService {
  private readonly stackModifiers = new Map<string, ModifierStack<any>>();

  /**
   * Create a new modifier stack
   * @param stackName The name of the stack
   * @param defaultValue The default value
   * @param action The action to execute when the value changes
   * @returns The created modifier stack
   */
  createModifierStack<T>(
    stackName: string,
    defaultValue: T,
    action: (value: T) => void
  ): ModifierStack<T> {
    try {
      if (this.stackModifiers.has(stackName)) {
        throw new Error(`ModifierStack '${stackName}' already exists`);
      }

      const stack = new ModifierStack(stackName, defaultValue, action, this);
      this.stackModifiers.set(stackName, stack);

      // Execute action immediately with default value
      action(defaultValue);

      logger.info('ModifierStack created', {
        stackName,
        defaultValue,
      });

      return stack;
    } catch (error) {
      throw ExceptionHandler.handleAndRethrow(
        error,
        'ModifierService.createModifierStack',
        { stackName, defaultValue }
      );
    }
  }

  /**
   * Get or create a modifier stack
   * @param stackName The name of the stack
   * @param defaultValue The default value (only used if stack doesn't exist)
   * @param action The action to execute when the value changes (only used if stack doesn't exist)
   * @returns The existing or newly created modifier stack
   */
  getOrCreateModifierStack<T>(
    stackName: string,
    defaultValue: T,
    action: (value: T) => void
  ): ModifierStack<T> {
    const existing = this.stackModifiers.get(stackName);
    if (existing) {
      return existing as ModifierStack<T>;
    }

    return this.createModifierStack(stackName, defaultValue, action);
  }

  /**
   * Add a modifier to a stack
   * @param stackName The name of the stack
   * @param config The modifier configuration
   * @returns The created modifier
   */
  addModifier<T>(stackName: string, config: ModifierConfig<T>): Modifier<T> {
    try {
      const stack = this.stackModifiers.get(stackName);
      if (!stack) {
        throw new Error(`ModifierStack '${stackName}' does not exist`);
      }

      return stack.addModifier(config.value, config.prio);
    } catch (error) {
      throw ExceptionHandler.handleAndRethrow(
        error,
        'ModifierService.addModifier',
        { stackName, config }
      );
    }
  }

  /**
   * Remove a modifier (called by Modifier.close())
   * @param modifier The modifier to remove
   */
  removeModifier<T>(modifier: Modifier<T>): void {
    // Modifier.close() calls stack.removeModifier() directly
    // This method is here for API completeness but not strictly needed
  }

  /**
   * Get a modifier stack
   * @param stackName The name of the stack
   * @returns The modifier stack or undefined
   */
  getModifierStack<T>(stackName: string): ModifierStack<T> | undefined {
    return this.stackModifiers.get(stackName);
  }

  /**
   * Remove a stack
   * @param stackName The name of the stack
   */
  removeStack(stackName: string): void {
    try {
      const stack = this.stackModifiers.get(stackName);
      if (stack) {
        stack.dispose();
        this.stackModifiers.delete(stackName);

        logger.info('ModifierStack removed', { stackName });
      }
    } catch (error) {
      ExceptionHandler.handle(error, 'ModifierService.removeStack', { stackName });
    }
  }

  /**
   * Check if a stack exists
   * @param stackName The name of the stack
   * @returns True if the stack exists
   */
  hasStack(stackName: string): boolean {
    return this.stackModifiers.has(stackName);
  }

  /**
   * Get all stack names
   */
  get stackNames(): string[] {
    return Array.from(this.stackModifiers.keys());
  }

  /**
   * Dispose all stacks
   */
  dispose(): void {
    for (const stack of this.stackModifiers.values()) {
      stack.dispose();
    }
    this.stackModifiers.clear();
    logger.info('ModifierService disposed');
  }

  /**
   * Internal method to remove a stack from the map
   * Called by ModifierStack.dispose()
   * @internal
   */
  _removeStackFromMap(stackName: string): void {
    this.stackModifiers.delete(stackName);
  }
}
