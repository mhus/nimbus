import { getLogger, ExceptionHandler } from '@nimbus/shared';
import type {
  ScrawlScript,
  ScrawlStep,
  ScrawlCondition,
  ScrawlSubject,
  ScrawlScriptLibrary,
} from '@nimbus/shared';
import type { ScrawlExecContext } from './ScrawlExecContext';
import type { ScrawlEffectFactory } from './ScrawlEffectFactory';
import type { AppContext } from '../AppContext';

const logger = getLogger('ScrawlExecutor');

/**
 * Executor for scrawl scripts.
 * Handles the execution logic for all step types.
 */
export class ScrawlExecutor {
  private cancelled = false;
  private paused = false;
  private vars = new Map<string, any>();
  private eventEmitter: EventTarget;
  private runningEffects = new Map<string, any>();
  private executorId: string = `exec_${Date.now()}_${Math.random()}`;
  private taskCompletionPromises = new Map<string, Promise<void>>();

  constructor(
    private readonly effectFactory: ScrawlEffectFactory,
    private readonly scriptLibrary: ScrawlScriptLibrary,
    private readonly appContext: AppContext,
    private readonly script: ScrawlScript,
    private readonly initialContext: Partial<ScrawlExecContext>
  ) {
    this.eventEmitter = new EventTarget();
  }

  /**
   * Start executing the script
   */
  async start(): Promise<void> {
    try {
      logger.info(`Starting script: ${this.script.id}`);

      // Determine root step
      let rootStep: ScrawlStep | undefined;

      if (this.script.root) {
        rootStep = this.script.root;
      } else if (this.script.sequences?.['main']) {
        rootStep = this.script.sequences['main'].step;
      }

      if (!rootStep) {
        throw new Error(`Script ${this.script.id} has no root step or main sequence`);
      }

      // Create execution context
      const ctx = this.createContext();

      // Execute root step
      await this.execStep(ctx, rootStep);

      logger.info(`Script completed: ${this.script.id}`);
    } catch (error) {
      throw ExceptionHandler.handleAndRethrow(
        error,
        'ScrawlExecutor.start',
        { scriptId: this.script.id }
      );
    }
  }

  /**
   * Execute a single step
   */
  async execStep(ctx: ScrawlExecContext, step: ScrawlStep): Promise<void> {
    if (this.cancelled) {
      return;
    }

    // Wait while paused
    while (this.paused && !this.cancelled) {
      await this.sleep(0.1);
    }

    try {
      switch (step.kind) {
        case 'Play':
          await this.execStepPlay(ctx, step);
          break;
        case 'Wait':
          await this.sleep(step.seconds);
          break;
        case 'Sequence':
          await this.execStepSequence(ctx, step);
          break;
        case 'Parallel':
          await this.execStepParallel(ctx, step);
          break;
        case 'Repeat':
          await this.execStepRepeat(ctx, step);
          break;
        case 'ForEach':
          await this.execStepForEach(ctx, step);
          break;
        case 'LodSwitch':
          await this.execStepLodSwitch(ctx, step);
          break;
        case 'Call':
          await this.execStepCall(ctx, step);
          break;
        case 'If':
          await this.execStepIf(ctx, step);
          break;
        case 'EmitEvent':
          this.emit(step.name, step.payload);
          break;
        case 'WaitEvent':
          await this.waitEvent(step.name, step.timeout ?? 0);
          break;
        case 'SetVar':
          this.setVar(step.name, step.value);
          break;
        case 'Cmd':
          await this.execStepCmd(ctx, step);
          break;
        case 'While':
          await this.execStepWhile(ctx, step);
          break;
        case 'Until':
          await this.execStepUntil(ctx, step);
          break;
        default:
          logger.warn(`Unknown step kind: ${(step as any).kind}`);
      }
    } catch (error) {
      throw ExceptionHandler.handleAndRethrow(
        error,
        'ScrawlExecutor.execStep',
        { scriptId: this.script.id, stepKind: step.kind }
      );
    }
  }

  private async execStepPlay(ctx: ScrawlExecContext, step: any): Promise<void> {
    // Resolve source and target from context
    const source = this.resolveSubject(ctx, step.source);
    const target = this.resolveSubject(ctx, step.target);

    // Merge context with step context
    const effectCtx: ScrawlExecContext = {
      ...ctx,
      ...(step.ctx || {}),
    };

    // Override actor/patients if source/target specified
    if (source) {
      if (Array.isArray(source)) {
        effectCtx.patients = source;
      } else {
        effectCtx.actor = source;
      }
    }
    if (target) {
      if (Array.isArray(target)) {
        effectCtx.patients = target;
      } else {
        effectCtx.patients = [target];
      }
    }

    await this.effectFactory.play(step.effectId, step.ctx || {}, effectCtx);
  }

  private async execStepSequence(ctx: ScrawlExecContext, step: any): Promise<void> {
    for (const childStep of step.steps) {
      await this.execStep(ctx, childStep);
      if (this.cancelled) break;
    }
  }

  private async execStepParallel(ctx: ScrawlExecContext, step: any): Promise<void> {
    const steps = step.steps || [];
    const taskPromises: Promise<void>[] = [];

    for (let i = 0; i < steps.length; i++) {
      const parallelStep = steps[i];

      // Use explicit ID or generate one
      const taskId = (parallelStep as any).id || `${this.executorId}_parallel_${i}`;

      // Fork context and execute
      const forkedCtx = this.fork(ctx);
      const taskPromise = this.execStep(forkedCtx, parallelStep);
      taskPromises.push(taskPromise);

      // Register task completion promise (for While-loops)
      this.taskCompletionPromises.set(taskId, taskPromise);

      // Cleanup after completion
      taskPromise.finally(() => {
        this.taskCompletionPromises.delete(taskId);
      });
    }

    // Wait for all parallel tasks to complete
    await Promise.all(taskPromises);
  }

  private async execStepRepeat(ctx: ScrawlExecContext, step: any): Promise<void> {
    if (step.times != null) {
      // Repeat N times
      for (let i = 0; i < step.times && !this.cancelled; i++) {
        await this.execStep(ctx, step.step);
      }
    } else if (step.untilEvent) {
      // Repeat until event
      while (!this.cancelled) {
        const done = await this.waitEvent(step.untilEvent, 0);
        if (done) break;
        await this.execStep(ctx, step.step);
      }
    }
  }

  private async execStepForEach(ctx: ScrawlExecContext, step: any): Promise<void> {
    // Resolve collection (e.g., "$patients")
    const collection = this.resolveValue(ctx, step.collection);

    if (!Array.isArray(collection)) {
      logger.warn(`ForEach: collection is not an array: ${step.collection}`);
      return;
    }

    // Execute step for each item
    for (const item of collection) {
      if (this.cancelled) break;

      // Create context with item variable
      const itemCtx = { ...ctx };
      this.setContextVar(itemCtx, step.itemVar, item);

      await this.execStep(itemCtx, step.step);
    }
  }

  private async execStepLodSwitch(ctx: ScrawlExecContext, step: any): Promise<void> {
    const lodLevel = ctx.lodLevel || 'medium';
    const lodStep = step.levels[lodLevel];

    if (lodStep) {
      await this.execStep(ctx, lodStep);
    } else {
      logger.debug(`No step defined for LOD level: ${lodLevel}`);
    }
  }

  private async execStepCall(ctx: ScrawlExecContext, step: any): Promise<void> {
    // Load the called script
    const calledScript = await this.scriptLibrary.load(step.scriptId);

    if (!calledScript) {
      logger.warn(`Called script not found: ${step.scriptId}`);
      return;
    }

    // Create new executor for called script
    const subExecutor = new ScrawlExecutor(
      this.effectFactory,
      this.scriptLibrary,
      this.appContext,
      calledScript,
      {
        ...ctx,
        ...(step.args || {}),
      }
    );

    await subExecutor.start();
  }

  private async execStepIf(ctx: ScrawlExecContext, step: any): Promise<void> {
    const conditionMet = this.evalCondition(ctx, step.cond);

    if (conditionMet) {
      await this.execStep(ctx, step.then);
    } else if (step.else) {
      await this.execStep(ctx, step.else);
    }
  }

  private async execStepCmd(ctx: ScrawlExecContext, step: any): Promise<void> {
    const commandService = ctx.appContext.services.command;
    if (!commandService) {
      logger.warn('CommandService not available, skipping Cmd step');
      return;
    }

    const { cmd, parameters = [] } = step;

    if (!cmd) {
      logger.warn('Cmd step: cmd is required');
      return;
    }

    try {
      logger.debug('Executing command from Cmd step', {
        cmd,
        parameters,
        scriptId: this.script.id,
      });

      await commandService.executeCommand(cmd, parameters);
    } catch (error) {
      // Log error but continue script execution
      ExceptionHandler.handle(error, 'ScrawlExecutor.execStepCmd', {
        cmd,
        scriptId: this.script.id,
      });
      logger.warn('Command execution failed in Cmd step', {
        cmd,
        error: (error as Error).message,
      });
    }
  }

  /**
   * Evaluate a condition
   */
  private evalCondition(ctx: ScrawlExecContext, cond: ScrawlCondition): boolean {
    switch (cond.kind) {
      case 'VarEquals':
        return this.getVar(cond.name) === cond.value;

      case 'VarExists':
        return this.vars.has(cond.name);

      case 'Chance':
        return Math.random() < cond.p;

      case 'HasTargets': {
        const patients = ctx.patients || [];
        const min = cond.min ?? 1;
        return patients.length >= min;
      }

      case 'HasSource':
        return ctx.actor !== undefined;

      case 'IsVarTrue': {
        const value = this.vars.get(cond.name);
        if (value === undefined) {
          return cond.defaultValue ?? false;
        }
        return value === true;
      }

      case 'IsVarFalse': {
        const value = this.vars.get(cond.name);
        if (value === undefined) {
          return cond.defaultValue ?? true;
        }
        return value === false;
      }

      default:
        logger.warn(`Unknown condition kind: ${(cond as any).kind}`);
        return false;
    }
  }

  /**
   * Resolve a subject reference (e.g., "$actor", "$patient", "$patient[0]")
   */
  private resolveSubject(
    ctx: ScrawlExecContext,
    ref?: string
  ): ScrawlSubject | ScrawlSubject[] | undefined {
    if (!ref) return undefined;

    // Handle $actor
    if (ref === '$actor') {
      return ctx.actor;
    }

    // Handle $patients
    if (ref === '$patients') {
      return ctx.patients;
    }

    // Handle $patient (first patient)
    if (ref === '$patient') {
      return ctx.patients?.[0];
    }

    // Handle $patient[N]
    const match = ref.match(/^\$patient\[(\d+)\]$/);
    if (match) {
      const index = parseInt(match[1], 10);
      return ctx.patients?.[index];
    }

    // Handle context variables
    return this.resolveValue(ctx, ref);
  }

  /**
   * Resolve a value reference from context
   */
  private resolveValue(ctx: ScrawlExecContext, ref: string): any {
    // Variable reference: $varName
    if (ref.startsWith('$')) {
      const varName = ref.substring(1);

      // Check context first
      if (varName in ctx) {
        return ctx[varName];
      }

      // Check vars map
      return this.vars.get(varName);
    }

    // Literal value
    return ref;
  }

  /**
   * Set a variable in context (for ForEach)
   */
  private setContextVar(ctx: ScrawlExecContext, name: string, value: any): void {
    // Remove $ prefix if present
    const varName = name.startsWith('$') ? name.substring(1) : name;
    (ctx as any)[varName] = value;
  }

  /**
   * Create execution context
   */
  private createContext(): ScrawlExecContext {
    const ctx: ScrawlExecContext = {
      ...this.initialContext,
      appContext: this.appContext,
      executor: this,
      scriptId: this.script.id,
      vars: this.initialContext.vars || {},
    } as ScrawlExecContext;

    // Set default variables from initial context
    this.setDefaultVariables(ctx);

    return ctx;
  }

  /**
   * Sets default variables in the context.
   * These are automatically available in all scripts.
   */
  private setDefaultVariables(ctx: ScrawlExecContext): void {
    // $source - The source subject (actor)
    if (ctx.actor) {
      this.vars.set('source', ctx.actor);
    }

    // $target - The target subject (first patient)
    if (ctx.patients && ctx.patients.length > 0) {
      this.vars.set('target', ctx.patients[0]);
    }

    // $targets - Array of all targets (all patients)
    if (ctx.patients) {
      this.vars.set('targets', ctx.patients);
    }

    // $item - The item Block that triggered this effect (if available)
    if (this.initialContext.item) {
      this.vars.set('item', this.initialContext.item);
    }

    // $itemId - The item ID (if available)
    if (this.initialContext.itemId) {
      this.vars.set('itemId', this.initialContext.itemId);
    }

    // $itemName - The item name (if available)
    if (this.initialContext.itemName) {
      this.vars.set('itemName', this.initialContext.itemName);
    }

    // $itemTexture - The item texture (if available)
    if (this.initialContext.itemTexture) {
      this.vars.set('itemTexture', this.initialContext.itemTexture);
    }
  }

  /**
   * Fork context for parallel execution
   */
  private fork(ctx: ScrawlExecContext): ScrawlExecContext {
    return {
      ...ctx,
      vars: { ...ctx.vars },
    };
  }

  /**
   * Sleep for a duration
   */
  private sleep(seconds: number): Promise<void> {
    const startTime = this.now();
    const targetTime = startTime + seconds;

    return new Promise<void>((resolve) => {
      const tick = () => {
        if (this.cancelled) {
          resolve();
          return;
        }

        if (this.paused) {
          requestAnimationFrame(tick);
          return;
        }

        if (this.now() >= targetTime) {
          resolve();
          return;
        }

        requestAnimationFrame(tick);
      };

      tick();
    });
  }

  /**
   * Get current time in seconds
   */
  private now(): number {
    return performance.now() / 1000;
  }

  // Public API

  cancel(): void {
    this.cancelled = true;
    logger.debug(`Script cancelled: ${this.script.id}`);
  }

  pause(): void {
    this.paused = true;
    logger.debug(`Script paused: ${this.script.id}`);
  }

  resume(): void {
    this.paused = false;
    logger.debug(`Script resumed: ${this.script.id}`);
  }

  emit(eventName: string, payload?: any): void {
    const event = new CustomEvent(eventName, { detail: payload });
    this.eventEmitter.dispatchEvent(event);
    logger.debug(`Event emitted: ${eventName}`, { payload });
  }

  waitEvent(eventName: string, timeoutSec = 0): Promise<boolean> {
    return new Promise<boolean>((resolve) => {
      let timeoutHandle: any = null;

      const handler = (event: Event) => {
        cleanup();
        resolve(true);
      };

      const cleanup = () => {
        this.eventEmitter.removeEventListener(eventName, handler);
        if (timeoutHandle) {
          clearTimeout(timeoutHandle);
        }
      };

      this.eventEmitter.addEventListener(eventName, handler, { once: true });

      if (timeoutSec > 0) {
        timeoutHandle = setTimeout(() => {
          cleanup();
          resolve(false);
        }, timeoutSec * 1000);
      }
    });
  }

  getVar(name: string): any {
    return this.vars.get(name);
  }

  setVar(name: string, value: any): void {
    this.vars.set(name, value);
    logger.debug(`Variable set: ${name}`, { value });
  }

  isCancelled(): boolean {
    return this.cancelled;
  }

  isPaused(): boolean {
    return this.paused;
  }

  getScriptId(): string {
    return this.script.id;
  }

  /**
   * Updates a parameter in the context and notifies running effects.
   * Called externally via ScrawlService.
   *
   * Only relevant for StepUntil - StepWhile does not trigger parameter updates.
   *
   * @param paramName Name of the parameter
   * @param value New value
   */
  updateParameter(paramName: string, value: any): void {
    // Update context variable
    const ctx = this.createContext();
    ctx.vars = ctx.vars || {};
    ctx.vars[paramName] = value;

    logger.debug('Parameter updated', { paramName, value, executorId: this.executorId });

    // Notify all running effects that implement onParameterChanged
    for (const [effectId, effect] of this.runningEffects) {
      if (effect.onParameterChanged) {
        try {
          effect.onParameterChanged(paramName, value, ctx);
        } catch (error) {
          logger.error('onParameterChanged failed', { effectId, paramName, error });
        }
      }
    }
  }

  /**
   * Executes StepWhile: Loop while a parallel task is running
   */
  private async execStepWhile(ctx: ScrawlExecContext, step: any): Promise<void> {
    const timeout = step.timeout ?? 60;
    const startTime = performance.now() / 1000;

    // Get task completion promise
    const taskPromise = this.taskCompletionPromises.get(step.taskId);
    if (!taskPromise) {
      logger.warn(`Task not found: ${step.taskId}`, { scriptId: this.script.id });
      return;
    }

    let taskCompleted = false;
    taskPromise.then(() => {
      taskCompleted = true;
    });

    let effectHandler: any | null = null;

    try {
      const isSteadyEffect = await this.checkIfSteadyEffect(step.step);

      if (isSteadyEffect) {
        // Steady: Execute once, runs until task ends
        effectHandler = await this.executeAndTrackEffect(ctx, step.step);

        // Wait for task completion or timeout
        while (!this.cancelled && !taskCompleted) {
          const elapsed = performance.now() / 1000 - startTime;
          if (elapsed >= timeout) {
            logger.warn(`While loop timed out after ${timeout}s`, {
              taskId: step.taskId,
              scriptId: this.script.id,
            });
            break;
          }

          // Check if effect finished early
          if (effectHandler && !effectHandler.isRunning()) {
            logger.debug('Steady effect finished early in While loop');
            break;
          }

          await this.sleep(0.1); // 100ms polling
        }
      } else {
        // One-Shot: Execute repeatedly until task ends
        while (!this.cancelled && !taskCompleted) {
          const elapsed = performance.now() / 1000 - startTime;
          if (elapsed >= timeout) {
            logger.warn(`While loop timed out after ${timeout}s`, {
              taskId: step.taskId,
              scriptId: this.script.id,
            });
            break;
          }

          await this.execStep(ctx, step.step);
          await this.sleep(0.016); // ~60fps
        }
      }
    } finally {
      // Cleanup steady effect
      if (effectHandler) {
        this.stopAndUntrackEffect(effectHandler);
      }
    }
  }

  /**
   * Executes StepUntil: Loop until an event is emitted
   * Supports parameter updates via updateParameter()
   */
  private async execStepUntil(ctx: ScrawlExecContext, step: any): Promise<void> {
    const timeout = step.timeout ?? 60;
    const startTime = performance.now() / 1000;

    // Set up event listener
    let eventReceived = false;
    const eventPromise = this.waitEvent(step.event, 0);
    eventPromise.then(() => {
      eventReceived = true;
    });

    let effectHandler: any | null = null;

    try {
      const isSteadyEffect = await this.checkIfSteadyEffect(step.step);

      if (isSteadyEffect) {
        // Steady: Execute once, runs until event
        effectHandler = await this.executeAndTrackEffect(ctx, step.step);

        // Wait for event or timeout
        while (!this.cancelled && !eventReceived) {
          const elapsed = performance.now() / 1000 - startTime;
          if (elapsed >= timeout) {
            logger.warn(`Until loop timed out after ${timeout}s`, {
              event: step.event,
              scriptId: this.script.id,
            });
            break;
          }

          // Check if effect finished early
          if (effectHandler && !effectHandler.isRunning()) {
            logger.debug('Steady effect finished early in Until loop');
            break;
          }

          await this.sleep(0.1); // 100ms polling
        }
      } else {
        // One-Shot: Execute repeatedly until event
        while (!this.cancelled && !eventReceived) {
          const elapsed = performance.now() / 1000 - startTime;
          if (elapsed >= timeout) {
            logger.warn(`Until loop timed out after ${timeout}s`, {
              event: step.event,
              scriptId: this.script.id,
            });
            break;
          }

          await this.execStep(ctx, step.step);
          await this.sleep(0.016); // ~60fps
        }
      }
    } finally {
      // Cleanup steady effect
      if (effectHandler) {
        this.stopAndUntrackEffect(effectHandler);
      }
    }
  }

  /**
   * Checks if a step contains a steady effect
   */
  private async checkIfSteadyEffect(step: ScrawlStep): Promise<boolean> {
    if (step.kind !== 'Play') return false;

    try {
      const handler = this.effectFactory.create((step as any).effectId, (step as any).ctx || {});
      return handler.isSteadyEffect();
    } catch (error) {
      logger.error('Failed to check if steady effect', { error });
      return false;
    }
  }

  /**
   * Executes a Play step and tracks the handler
   */
  private async executeAndTrackEffect(ctx: ScrawlExecContext, step: any): Promise<any> {
    // Resolve source and target (like in execStepPlay)
    const source = this.resolveSubject(ctx, step.source);
    const target = this.resolveSubject(ctx, step.target);

    const effectCtx: ScrawlExecContext = {
      ...ctx,
      ...(step.ctx || {}),
    };

    if (source) {
      if (Array.isArray(source)) {
        effectCtx.patients = source;
      } else {
        effectCtx.actor = source;
      }
    }
    if (target) {
      if (Array.isArray(target)) {
        effectCtx.patients = target;
      } else {
        effectCtx.patients = [target];
      }
    }

    // Create and execute effect
    const handler = this.effectFactory.create(step.effectId, step.ctx || {});
    const effectId = `effect_${this.executorId}_${Date.now()}_${Math.random()}`;

    // Register in running effects
    this.runningEffects.set(effectId, handler);

    try {
      const result = handler.execute(effectCtx);
      if (result instanceof Promise) {
        await result;
      }
    } catch (error) {
      // Remove on error
      this.runningEffects.delete(effectId);
      throw error;
    }

    return handler;
  }

  /**
   * Stops an effect and removes it from tracking
   */
  private stopAndUntrackEffect(handler: any): void {
    try {
      if (handler.stop) {
        const result = handler.stop();
        if (result instanceof Promise) {
          result.catch((err: any) => logger.error('Effect stop failed', { err }));
        }
      }
    } finally {
      // Remove from running effects
      for (const [key, h] of this.runningEffects) {
        if (h === handler) {
          this.runningEffects.delete(key);
          break;
        }
      }
    }
  }
}
