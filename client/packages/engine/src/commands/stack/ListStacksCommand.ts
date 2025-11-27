/**
 * ListStacksCommand - List all available modifier stacks
 */

import { CommandHandler } from '../CommandHandler';
import type { AppContext } from '../../AppContext';
import { AnimationStack } from '../../services/ModifierService';
import { getModifierName } from './SetStackModifierCommand';

/**
 * ListStacks command - List all available modifier stacks with their current values
 *
 * Usage:
 *   listStacks [verbose]
 *
 * Parameters:
 *   verbose - Optional: Show detailed information including all modifiers (default: false)
 *
 * Examples:
 *   listStacks           - Show basic stack information
 *   listStacks verbose   - Show detailed information with all modifiers
 */
export class ListStacksCommand extends CommandHandler {
  private appContext: AppContext;

  constructor(appContext: AppContext) {
    super();
    this.appContext = appContext;
  }

  name(): string {
    return 'listStacks';
  }

  description(): string {
    return 'List all available modifier stacks with their current values';
  }

  execute(parameters: any[]): any {
    const modifierService = this.appContext.services.modifier;

    if (!modifierService) {
      console.error('ModifierService not available');
      return { error: 'ModifierService not available' };
    }

    const verbose = parameters.length > 0 &&
      (parameters[0] === 'verbose' || parameters[0] === 'v' || parameters[0] === 'true');

    const stackNames = modifierService.stackNames;

    if (stackNames.length === 0) {
      console.log('No modifier stacks available');
      return { stacks: [] };
    }

    console.log(`\n${'='.repeat(80)}`);
    console.log(`Available Modifier Stacks (${stackNames.length} total)`);
    console.log(`${'='.repeat(80)}\n`);

    const stacksInfo: any[] = [];

    for (const stackName of stackNames) {
      const stack = modifierService.getModifierStack(stackName);
      if (!stack) continue;

      const currentValue = stack.getValue();
      const modifierCount = stack.modifiers.length;
      const isAnimationStack = stack instanceof AnimationStack;
      const defaultValue = stack.getDefaultModifier().getValue();

      const stackInfo: any = {
        name: stackName,
        type: isAnimationStack ? 'AnimationStack' : 'ModifierStack',
        currentValue,
        defaultValue,
        modifierCount,
      };

      // Basic output
      console.log(`Stack: ${stackName}`);
      console.log(`  Type: ${isAnimationStack ? 'AnimationStack ⏱' : 'ModifierStack'}`);
      console.log(`  Current Value: ${this.formatValue(currentValue)}`);
      console.log(`  Default Value: ${this.formatValue(defaultValue)}`);
      console.log(`  Active Modifiers: ${modifierCount}`);

      if (verbose && modifierCount > 0) {
        console.log(`  Modifiers:`);
        stackInfo.modifiers = [];

        const modifiers = stack.modifiers;
        for (let i = 0; i < modifiers.length; i++) {
          const modifier = modifiers[i];
          const modifierName = getModifierName(stackName, modifier);

          const modifierInfo: any = {
            index: i,
            name: modifierName || '(unnamed)',
            value: modifier.getValue(),
            priority: modifier.prio,
            enabled: modifier.enabled,
            sequence: modifier.sequence,
          };

          console.log(`    [${i}] ${modifierName ? `Name: "${modifierName}", ` : ''}` +
            `Value: ${this.formatValue(modifier.getValue())}, ` +
            `Priority: ${modifier.prio}, ` +
            `Enabled: ${modifier.enabled}, ` +
            `Sequence: ${modifier.sequence}`);

          stackInfo.modifiers.push(modifierInfo);
        }
      }

      console.log(''); // Empty line between stacks
      stacksInfo.push(stackInfo);
    }

    console.log(`${'='.repeat(80)}\n`);

    // Summary
    const animationStackCount = stacksInfo.filter(s => s.type === 'AnimationStack').length;
    const regularStackCount = stacksInfo.filter(s => s.type === 'ModifierStack').length;
    const totalModifiers = stacksInfo.reduce((sum, s) => sum + s.modifierCount, 0);

    console.log('Summary:');
    console.log(`  Total Stacks: ${stacksInfo.length}`);
    console.log(`  - AnimationStacks: ${animationStackCount}`);
    console.log(`  - ModifierStacks: ${regularStackCount}`);
    console.log(`  Total Active Modifiers: ${totalModifiers}`);
    console.log('');

    return {
      stacks: stacksInfo,
      summary: {
        totalStacks: stacksInfo.length,
        animationStacks: animationStackCount,
        modifierStacks: regularStackCount,
        totalModifiers,
      },
    };
  }

  /**
   * Format a value for display
   */
  private formatValue(value: any): string {
    if (typeof value === 'number') {
      return value.toFixed(3);
    } else if (typeof value === 'boolean') {
      return value ? 'true' : 'false';
    } else if (typeof value === 'string') {
      return `"${value}"`;
    } else if (value === null || value === undefined) {
      return 'null';
    } else {
      return JSON.stringify(value);
    }
  }
}
