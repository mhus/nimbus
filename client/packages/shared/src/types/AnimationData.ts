/**
 * AnimationData - Animation sequence definition
 *
 * An animation is a sequence of effects to be played.
 * Location is defined outside the animation, or may not be needed.
 *
 * Note: Not yet finalized!
 */

/**
 * Animation effect types
 */
export enum AnimationEffectType {
  SCALE = 'scale',
  ROTATE = 'rotate',
  TRANSLATE = 'translate',
  COLOR_CHANGE = 'colorChange',
}

/**
 * Easing function types
 */
export enum EasingType {
  LINEAR = 'linear',
  EASE_IN = 'easeIn',
  EASE_OUT = 'easeOut',
  EASE_IN_OUT = 'easeInOut',
  ELASTIC = 'elastic',
  BOUNCE = 'bounce',
}

/**
 * Animation effect definition
 */
export interface AnimationEffect {
  /** Effect type */
  type: AnimationEffectType;

  /** Effect parameters */
  params: {
    from?: any;
    to?: any;
    easing?: EasingType;
    [key: string]: any;
  };

  /** Start time relative to animation (ms) */
  startTime: number;

  /** End time relative to animation (ms) */
  endTime: number;
}

/**
 * Animation definition
 */
export interface AnimationData {
  /** Animation name */
  name: string;

  /** Total duration in milliseconds */
  duration: number;

  /** List of effects */
  effects: AnimationEffect[];

  /** Whether animation loops */
  loop?: boolean;

  /** Number of times to repeat (if not looping) */
  repeat?: number;
}
