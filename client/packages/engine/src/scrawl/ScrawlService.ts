import { getLogger, ExceptionHandler } from '@nimbus/shared';
import type { ScrawlScript, ScrawlScriptLibrary, ScriptActionDefinition } from '@nimbus/shared';
import type { AppContext } from '../AppContext';
import { ScrawlEffectRegistry, ScrawlEffectFactory } from './ScrawlEffectFactory';
import { ScrawlExecutor } from './ScrawlExecutor';
import type { EffectDeps } from './ScrawlEffectHandler';
import type { ScrawlExecContext } from './ScrawlExecContext';
import { LogEffect } from './effects/LogEffect';

const logger = getLogger('ScrawlService');

/**
 * Central service for managing scrawl scripts and effect execution.
 * Manages running executors, effect factory, and script library.
 */
export class ScrawlService {
  private effectRegistry: ScrawlEffectRegistry;
  private effectFactory: ScrawlEffectFactory;
  private scriptLibrary: ScrawlScriptLibrary;
  private runningExecutors = new Map<string, ScrawlExecutor>();
  private executorIdCounter = 0;

  constructor(private readonly appContext: AppContext) {
    // Initialize effect registry and factory
    this.effectRegistry = new ScrawlEffectRegistry();
    this.effectFactory = new ScrawlEffectFactory(this.effectRegistry, this.createEffectDeps());

    // Initialize script library (simple in-memory implementation)
    this.scriptLibrary = this.createScriptLibrary();

    logger.info('ScrawlService initialized');
  }

  /**
   * Initialize the service (called after AppContext is fully set up)
   */
  async initialize(): Promise<void> {
    try {
      logger.info('ScrawlService initializing...');

      // Register built-in effects
      this.registerBuiltInEffects();

      logger.info('ScrawlService initialized successfully');
    } catch (error) {
      throw ExceptionHandler.handleAndRethrow(error, 'ScrawlService.initialize');
    }
  }

  /**
   * Register built-in effect handlers
   */
  private registerBuiltInEffects(): void {
    // Register LogEffect for testing and debugging
    this.effectRegistry.register('log', LogEffect);
    logger.debug('Built-in effects registered', {
      effects: ['log'],
    });
  }

  /**
   * Get the effect registry for registering custom effects
   */
  getEffectRegistry(): ScrawlEffectRegistry {
    return this.effectRegistry;
  }

  /**
   * Get the effect factory
   */
  getEffectFactory(): ScrawlEffectFactory {
    return this.effectFactory;
  }

  /**
   * Get the script library
   */
  getScriptLibrary(): ScrawlScriptLibrary {
    return this.scriptLibrary;
  }

  /**
   * Execute a script action definition
   * @param action Script action definition
   * @param context Initial execution context
   * @returns Executor ID
   */
  async executeAction(
    action: ScriptActionDefinition,
    context?: Partial<ScrawlExecContext>
  ): Promise<string> {
    try {
      // Determine which script to execute
      let script: ScrawlScript | undefined;

      if (action.script) {
        // Inline script
        script = action.script;
      } else if (action.scriptId) {
        // Load script by ID
        script = await this.scriptLibrary.load(action.scriptId);
      }

      if (!script) {
        throw new Error('No script provided in action definition');
      }

      // Merge parameters into context
      const executionContext: Partial<ScrawlExecContext> = {
        ...context,
        ...(action.parameters || {}),
      };

      // Execute script
      return await this.executeScript(script, executionContext);
    } catch (error) {
      throw ExceptionHandler.handleAndRethrow(
        error,
        'ScrawlService.executeAction',
        { scriptId: action.scriptId }
      );
    }
  }

  /**
   * Execute a script
   * @param scriptIdOrObj Script ID or script object
   * @param context Initial execution context
   * @returns Executor ID
   */
  async executeScript(
    scriptIdOrObj: string | ScrawlScript,
    context?: Partial<ScrawlExecContext>
  ): Promise<string> {
    try {
      // Load script if ID provided
      const script =
        typeof scriptIdOrObj === 'string'
          ? await this.scriptLibrary.load(scriptIdOrObj)
          : scriptIdOrObj;

      if (!script) {
        throw new Error(`Script not found: ${scriptIdOrObj}`);
      }

      // Create executor
      const executor = new ScrawlExecutor(
        this.effectFactory,
        this.scriptLibrary,
        this.appContext,
        script,
        context || {}
      );

      // Generate executor ID
      const executorId = `executor_${this.executorIdCounter++}`;
      this.runningExecutors.set(executorId, executor);

      logger.info(`Starting script execution: ${script.id}`, { executorId });

      // Execute script (fire-and-forget)
      executor
        .start()
        .then(() => {
          logger.info(`Script execution completed: ${script.id}`, { executorId });
          this.runningExecutors.delete(executorId);
        })
        .catch((error) => {
          ExceptionHandler.handle(error, 'ScrawlService.executeScript.executor', {
            scriptId: script.id,
            executorId,
          });
          this.runningExecutors.delete(executorId);
        });

      return executorId;
    } catch (error) {
      throw ExceptionHandler.handleAndRethrow(
        error,
        'ScrawlService.executeScript',
        { scriptId: typeof scriptIdOrObj === 'string' ? scriptIdOrObj : scriptIdOrObj.id }
      );
    }
  }

  /**
   * Get a running executor by ID
   */
  getExecutor(executorId: string): ScrawlExecutor | undefined {
    return this.runningExecutors.get(executorId);
  }

  /**
   * Cancel a running executor
   */
  cancelExecutor(executorId: string): boolean {
    const executor = this.runningExecutors.get(executorId);
    if (executor) {
      executor.cancel();
      this.runningExecutors.delete(executorId);
      logger.info(`Executor cancelled: ${executorId}`);
      return true;
    }
    return false;
  }

  /**
   * Pause a running executor
   */
  pauseExecutor(executorId: string): boolean {
    const executor = this.runningExecutors.get(executorId);
    if (executor) {
      executor.pause();
      logger.info(`Executor paused: ${executorId}`);
      return true;
    }
    return false;
  }

  /**
   * Resume a paused executor
   */
  resumeExecutor(executorId: string): boolean {
    const executor = this.runningExecutors.get(executorId);
    if (executor) {
      executor.resume();
      logger.info(`Executor resumed: ${executorId}`);
      return true;
    }
    return false;
  }

  /**
   * Get all running executor IDs
   */
  getRunningExecutorIds(): string[] {
    return Array.from(this.runningExecutors.keys());
  }

  /**
   * Cancel all running executors
   */
  cancelAllExecutors(): void {
    const ids = this.getRunningExecutorIds();
    ids.forEach((id) => this.cancelExecutor(id));
    logger.info(`Cancelled ${ids.length} executors`);
  }

  /**
   * Create effect dependencies
   */
  private createEffectDeps(): EffectDeps {
    return {
      log: (message: string, ...args: any[]) => logger.debug(message, ...args),
      now: () => performance.now() / 1000,
      // Add more dependencies as needed (scene, audio, etc.)
    };
  }

  /**
   * Create script library (simple in-memory implementation)
   */
  private createScriptLibrary(): ScrawlScriptLibrary {
    const scripts = new Map<string, ScrawlScript>();

    return {
      get: (id: string) => scripts.get(id),

      load: async (id: string) => {
        // Check cache first
        let script = scripts.get(id);
        if (script) {
          return script;
        }

        // TODO: Load from assets (e.g., /assets/scripts/{id}.scrawl.json)
        // For now, just return undefined
        logger.warn(`Script not found in library: ${id}`);
        return undefined;
      },

      has: (id: string) => scripts.has(id),
    };
  }

  /**
   * Register a script in the library (for testing/development)
   */
  registerScript(script: ScrawlScript): void {
    const library = this.scriptLibrary as any;
    if (library.scripts) {
      library.scripts.set(script.id, script);
    }
    logger.debug(`Script registered: ${script.id}`);
  }

  /**
   * Dispose the service
   */
  dispose(): void {
    logger.info('Disposing ScrawlService...');
    this.cancelAllExecutors();
    logger.info('ScrawlService disposed');
  }
}
