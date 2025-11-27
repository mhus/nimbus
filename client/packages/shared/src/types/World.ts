/**
 * World - World information and configuration
 */

import type { Vector3 } from './Vector3';
import type { ScriptActionDefinition } from '../scrawl/ScriptActionDefinition';

export enum SeasonStatus {
  NONE = 0,
  WINTER = 1,
  SPRING = 2,
  SUMMER = 3,
  AUTUMN = 4,
}

/**
 * World information
 * Contains all metadata and configuration for a world
 *
 * Only worldId and name are required. All other fields are optional
 * to allow flexible usage across different contexts (server, client, editor).
 */
export interface WorldInfo {
  /** Unique world identifier (required) */
  worldId: string;

  /** World display name (required) */
  name: string;

  /** World description */
  description?: string;

  /** Start position (min coordinates) */
  start?: Vector3;

  /** Stop position (max coordinates) */
  stop?: Vector3;

  /** Chunk size (default: 16) */
  chunkSize?: number;

  /** Asset path for resources */
  assetPath?: string;

  /** Asset server port */
  assetPort?: number;

  /** World region identifier */
  worlRegion?: string;

  /** World status (0=active, 1=inactive, etc.) */
  status?: number;

  /** Season status identifier (e.g., 'spring', 'summer', 'autumn', 'winter') */
  seasonStatus: SeasonStatus;

  /** Season progress (0.0 to 1.0, representing progress through current season) */
  seasonProgress: number;

  /** Creation timestamp */
  createdAt: string;

  /** Last update timestamp */
  updatedAt: string;

  /** World owner information */
  owner: {
    user: string;
    displayName: string;
    email?: string;
  };

  /** World settings */
  settings: {
    maxPlayers: number;
    allowGuests: boolean;
    pvpEnabled: boolean;
    pingInterval: number;

    /** Audio settings */
    /** Ambient music when player dies (optional) */
    deadAmbientAudio?: string;

    /** Step sound when swimming (optional, default: 'audio/liquid/swim1.ogg') */
    swimStepAudio?: string;

    /** Environment settings */
    /** Scene clear/background color RGB (optional, default: {r: 0.5, g: 0.7, b: 1.0}) */
    clearColor?: { r: number; g: number; b: number };

    /** Camera far clipping plane (optional, default: 500) */
    cameraMaxZ?: number;

    /** Sun visualization settings */
    /** Sun texture path (optional, default: 'textures/sun/sun1.png') */
    sunTexture?: string;

    /** Sun size/radius (optional, default: 80) */
    sunSize?: number;

    /** Sun position angle Y in degrees (optional, default: 90 = East) */
    sunAngleY?: number;

    /** Sun elevation in degrees (optional, default: 45) */
    sunElevation?: number;

    /** Sun color RGB (optional, default: [1, 1, 0.9]) */
    sunColor?: { r: number; g: number; b: number };

    /** Sun enabled (optional, default: true) */
    sunEnabled?: boolean;

    /** SkyBox settings */
    /** SkyBox configuration (optional) */
    skyBox?: {
      /** SkyBox enabled (default: false) */
      enabled: boolean;
      /** SkyBox mode: 'color' or 'texture' (default: 'color') */
      mode: 'color' | 'texture';
      /** Color mode: RGB color (default: {r: 0.2, g: 0.5, b: 1.0}) */
      color?: { r: number; g: number; b: number };
      /** Texture mode: Base path for cube textures (e.g., 'textures/skybox/stars') */
      texturePath?: string;
      /** SkyBox size (default: 2000) */
      size?: number;
      /** SkyBox rotation in degrees (default: 0) */
      rotation?: number;
    };

    /** Moon configurations (up to 3 moons) */
    moons?: Array<{
      /** Moon enabled (default: false) */
      enabled: boolean;
      /** Moon size (default: 60) */
      size?: number;
      /** Moon position on circle 0-360° (0=North, 90=East, default: spread evenly) */
      positionOnCircle?: number;
      /** Moon height over camera in degrees -90 to 90 (default: 45) */
      heightOverCamera?: number;
      /** Moon distance from camera (default: 450, must be > sun distance 400, < skybox ~1000) */
      distance?: number;
      /** Moon phase 0.0 (new moon) to 1.0 (full moon), default: 0.5 (half moon) */
      phase?: number;
      /** Moon texture path (optional, e.g., 'textures/moon/moon1.png') */
      texture?: string;
    }>;

    /** Horizon gradient box settings (optional) */
    horizonGradient?: {
      /** Enabled (default: false) */
      enabled: boolean;
      /** Distance on XZ plane from camera (default: 300) */
      distance?: number;
      /** Y position of bottom edge (default: 0) */
      y?: number;
      /** Height of the vertical sides (default: 100) */
      height?: number;
      /** Bottom color RGB (default: {r: 0.7, g: 0.8, b: 0.9}) */
      color0?: { r: number; g: number; b: number };
      /** Top color RGB (default: {r: 0.3, g: 0.5, b: 0.8}) */
      color1?: { r: number; g: number; b: number };
      /** Transparency 0-1 (default: 0.5) */
      alpha?: number;
    };

    /** Environment scripts (optional) */
    environmentScripts?: Array<{
      /** Script name (unique identifier) */
      name: string;
      /** Script group (e.g., 'environment', 'weather', 'daytime') */
      group: string;
      /** Script action definition */
      script: ScriptActionDefinition;
    }>;
  };

  /** License information */
  license?: {
    type: string;
    expiresAt?: string;
  };

  /** Start area for new players */
  startArea?: {
    x: number;
    y: number;
    z: number;
    radius: number;
    rotation: number;
  };

  /** Editor URL for block editing */
  editorUrl?: string;
}
