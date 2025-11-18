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
    await Promise.all(
      step.steps.map((childStep: ScrawlStep) => this.execStep(this.fork(ctx), childStep))
    );
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
    return {
      ...this.initialContext,
      appContext: this.appContext,
      executor: this,
      scriptId: this.script.id,
      vars: this.initialContext.vars || {},
    } as ScrawlExecContext;
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
}
