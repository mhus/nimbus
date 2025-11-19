import { getLogger, ExceptionHandler, MessageType } from '@nimbus/shared';
import type { ScrawlScript, ScrawlScriptLibrary, ScriptActionDefinition } from '@nimbus/shared';
import type { AppContext } from '../AppContext';
import { ScrawlEffectRegistry, ScrawlEffectFactory } from './ScrawlEffectFactory';
import { ScrawlExecutor } from './ScrawlExecutor';
import type { EffectDeps } from './ScrawlEffectHandler';
import type { ScrawlExecContext } from './ScrawlExecContext';
import { LogEffect } from './effects/LogEffect';
import { CommandEffect } from './effects/CommandEffect';
import { CircleMarkerEffect } from './effects/CircleMarkerEffect';
import { ProjectileEffect } from './effects/ProjectileEffect';
import { ParticleBeamEffect } from './effects/ParticleBeamEffect';
import { LoopingSoundEffect } from './effects/LoopingSoundEffect';
import { BeamFollowEffect } from './effects/BeamFollowEffect';
import { ParticleExplosionEffect } from './effects/ParticleExplosionEffect';
import { ParticleFireEffect } from './effects/ParticleFireEffect';
import { ParticlePositionFlashEffect } from './effects/ParticlePositionFlashEffect';
import { ParticleWandFlashEffect } from './effects/ParticleWandFlashEffect';
import { ParticleWandFlashSteadyEffect } from './effects/ParticleWandFlashSteadyEffect';

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

  // Track sent effect IDs to prevent executing our own effects when they come back from server
  private sentEffectIds: Set<string> = new Set();

  // Map effectId → executorId for parameter updates
  private effectIdToExecutorId = new Map<string, string>();

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

    // Register CommandEffect for executing commands
    this.effectRegistry.register('command', CommandEffect);

    // Register CircleMarkerEffect for visual AOE markers
    this.effectRegistry.register('circleMarker', CircleMarkerEffect);

    // Register ProjectileEffect for flying projectiles
    this.effectRegistry.register('projectile', ProjectileEffect);

    // Register ParticleBeamEffect for magical beam effects
    this.effectRegistry.register('particleBeam', ParticleBeamEffect);

    // Register LoopingSoundEffect for looping sounds in While/Until loops
    this.effectRegistry.register('sound:loop', LoopingSoundEffect);

    // Register BeamFollowEffect for dynamic beam tracking
    this.effectRegistry.register('beam:follow', BeamFollowEffect);

    // Register ParticleExplosionEffect for radial particle explosions
    this.effectRegistry.register('particleExplosion', ParticleExplosionEffect);

    // Register ParticleFireEffect for fire simulation
    this.effectRegistry.register('particleFire', ParticleFireEffect);

    // Register ParticlePositionFlashEffect for lightning strikes
    this.effectRegistry.register('particlePositionFlash', ParticlePositionFlashEffect);

    // Register ParticleWandFlashEffect for wand beams (one-shot)
    this.effectRegistry.register('particleWandFlash', ParticleWandFlashEffect);

    // Register ParticleWandFlashSteadyEffect for continuous wand beams
    this.effectRegistry.register('particleWandFlashSteady', ParticleWandFlashSteadyEffect);

    logger.debug('Built-in effects registered', {
      effects: [
        'log',
        'command',
        'circleMarker',
        'projectile',
        'particleBeam',
        'sound:loop',
        'beam:follow',
        'particleExplosion',
        'particleFire',
        'particlePositionFlash',
        'particleWandFlash',
        'particleWandFlashSteady',
      ],
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
      // Calculate multiplayer data (effectId, chunks, sendToServer)
      const shouldSendToServer = action.sendToServer !== false;
      let effectId: string | undefined;
      let affectedChunks: Array<{ cx: number; cz: number }> | undefined;

      if (shouldSendToServer) {
        const mpData = this.sendEffectTriggerToServer(action, context);
        effectId = mpData.effectId;
        affectedChunks = mpData.chunks;
      }

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

      // Merge parameters into vars (everything goes into vars now)
      const executionContext: Partial<ScrawlExecContext> = {
        vars: {
          ...(context as any)?.vars,
          ...(action.parameters || {}),
        },
      };

      // Log all parameters for debugging
      logger.info('Executing script with parameters', {
        scriptId: script.id,
        hasSource: !!executionContext.vars?.source,
        hasTarget: !!executionContext.vars?.target,
        hasTargets: !!executionContext.vars?.targets,
        targetsLength: executionContext.vars?.targets?.length,
        sourcePos: executionContext.vars?.source?.position,
        targetPos: executionContext.vars?.target?.position,
        targetsPos: executionContext.vars?.targets?.map((t: any) => t?.position),
        allVars: executionContext.vars ? Object.keys(executionContext.vars) : [],
      });

      // Execute script locally and get executor ID
      const executorId = await this.executeScript(script, executionContext);

      // Set multiplayer data on the executor if available
      if (effectId && affectedChunks && shouldSendToServer) {
        const executor = this.runningExecutors.get(executorId);
        if (executor) {
          executor.setMultiplayerData(effectId, affectedChunks, shouldSendToServer);
          // Map effectId to executorId for parameter updates
          this.effectIdToExecutorId.set(effectId, executorId);
        }
      }

      return executorId;
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
   * Updates a parameter in a running executor.
   * Can be called from any source (e.g., InputService, NetworkService, etc.)
   *
   * @param executorIdOrEffectId ID of the executor or effectId (for remote updates)
   * @param paramName Name of the parameter (e.g. 'targetPos', 'mousePos', 'volume')
   * @param value New value (any type)
   */
  updateExecutorParameter(executorIdOrEffectId: string, paramName: string, value: any): void {
    // Try direct executor ID first
    let executor = this.runningExecutors.get(executorIdOrEffectId);

    // If not found, try mapping from effectId
    if (!executor) {
      const executorId = this.effectIdToExecutorId.get(executorIdOrEffectId);
      if (executorId) {
        executor = this.runningExecutors.get(executorId);
      }
    }

    if (executor) {
      executor.updateParameter(paramName, value);
      logger.debug('Executor parameter updated via ScrawlService', {
        id: executorIdOrEffectId,
        paramName,
      });
    } else {
      logger.debug(`Executor not found for parameter update: ${executorIdOrEffectId}`, {
        paramName,
      });
    }
  }

  /**
   * Send effect trigger to server for synchronization
   *
   * Calculates affected chunks from source/target positions and sends
   * the effect to server for broadcasting to other clients.
   *
   * @param action Script action definition
   * @param context Execution context with source/target
   * @returns Effect ID and affected chunks
   */
  private sendEffectTriggerToServer(
    action: ScriptActionDefinition,
    context?: Partial<ScrawlExecContext>
  ): { effectId: string; chunks: Array<{ cx: number; cz: number }> } {
    try {
      const networkService = this.appContext.services.network;

      // Calculate affected chunks from source and target positions
      const chunks: Array<{ cx: number; cz: number }> = [];
      const chunkSize = this.appContext.worldInfo?.chunkSize || 16;

      // Get source and targets from vars
      const vars = (context as any)?.vars || {};
      const source = vars.source;
      const targets = vars.targets;

      // Add chunk for source position
      if (source?.position) {
        const pos = source.position;
        const cx = Math.floor(pos.x / chunkSize);
        const cz = Math.floor(pos.z / chunkSize);
        chunks.push({ cx, cz });
      }

      // Add chunks for target(s)
      if (targets) {
        for (const target of targets) {
          if (target?.position) {
            const pos = target.position;
            const cx = Math.floor(pos.x / chunkSize);
            const cz = Math.floor(pos.z / chunkSize);
            // Avoid duplicates
            if (!chunks.some(c => c.cx === cx && c.cz === cz)) {
              chunks.push({ cx, cz });
            }
          }
        }
      }

      // Get entity ID (if available)
      const entityId = source?.entityId;

      // Generate unique effect ID
      const effectId = `effect_${Date.now()}_${Math.random().toString(36).substring(2, 11)}`;

      // Track this effect ID so we don't execute it again when it comes back from server
      this.sentEffectIds.add(effectId);
      // Cleanup after 10 seconds
      setTimeout(() => this.sentEffectIds.delete(effectId), 10000);

      // Build effect with source/target/targets in parameters
      // They're already in vars from the context
      const effectWithContext = {
        ...action,
        parameters: {
          ...(action.parameters || {}),
          source: vars.source,
          target: vars.target,
          targets: vars.targets,
        },
      };

      // Build effect trigger message
      const effectTriggerData = {
        effectId,
        entityId,
        chunks: chunks.length > 0 ? chunks : undefined,
        effect: effectWithContext,
      };

      // Send to server if NetworkService is available
      if (networkService) {
        logger.info('🟢 CLIENT: Sending effect trigger to server', {
          effectId,
          entityId,
          chunkCount: chunks.length,
          scriptId: action.scriptId,
          messageType: 'e.t',
        });

        networkService.send({
          t: MessageType.EFFECT_TRIGGER,
          d: effectTriggerData,
        });

        logger.info('🟢 CLIENT: Effect trigger sent to server', {
          effectId,
        });
      } else {
        logger.warn('NetworkService not available, effect not sent to server');
      }

      // Return effectId and chunks for executor
      return { effectId, chunks };
    } catch (error) {
      // Log but don't fail - local effect should still execute
      logger.warn('Failed to send effect to server (effect still executed locally)', {
        error: (error as Error).message,
      });
      // Return empty data on error
      const fallbackId = `effect_${Date.now()}_${Math.random().toString(36).substring(2, 11)}`;
      return { effectId: fallbackId, chunks: [] };
    }
  }

  /**
   * Check if an effect was sent by this client
   *
   * Used by EffectTriggerHandler to prevent executing effects that
   * this client already executed locally.
   *
   * @param effectId Effect ID to check
   * @returns True if this client sent the effect
   */
  wasEffectSentByUs(effectId: string): boolean {
    return this.sentEffectIds.has(effectId);
  }

  /**
   * Dispose the service
   */
  dispose(): void {
    logger.info('Disposing ScrawlService...');
    this.cancelAllExecutors();
    this.sentEffectIds.clear();
    logger.info('ScrawlService disposed');
  }
}
