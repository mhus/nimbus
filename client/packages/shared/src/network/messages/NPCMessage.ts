/**
 * NPC Dialog messages
 */

import type { BaseMessage } from '../BaseMessage';
import type { DialogSeverity } from '../MessageTypes';

/**
 * Dialog option
 */
export interface DialogOption {
  /** Option ID */
  id: string;

  /** Option text */
  text: string;

  /** Option severity/type */
  severity: DialogSeverity;
}

/**
 * Dialog data
 */
export interface DialogData {
  /** Dialog title */
  title: string;

  /** Dialog text content */
  text: string;

  /** Available options */
  options: DialogOption[];
}

/**
 * NPC open dialog data
 */
export interface NPCOpenData {
  /** Unique chat session ID */
  chatId: string;

  /** Dialog ID */
  dialogId: string;

  /** NPC ID */
  npcId: string;

  /** Picture path (optional) */
  picturePath?: string;

  /** Dialog data */
  dialogData: DialogData;
}

/**
 * NPC open dialog (Server -> Client)
 * Server sends instruction to open NPC dialog
 */
export type NPCOpenMessage = BaseMessage<NPCOpenData>;

/**
 * NPC select option data
 */
export interface NPCSelectData {
  /** NPC ID */
  npcId: string;

  /** Dialog ID */
  dialogId: string;

  /** Chat session ID */
  chatId: string;

  /** Selected option ID */
  selectedOptionId: string;
}

/**
 * NPC select option (Client -> Server)
 * Client sends player's selection in NPC dialog
 */
export type NPCSelectMessage = BaseMessage<NPCSelectData>;

/**
 * NPC update dialog data
 */
export interface NPCUpdateData {
  /** Chat session ID */
  chatId: string;

  /** Dialog ID */
  dialogId: string;

  /** NPC ID */
  npcId: string;

  /** Updated dialog data */
  dialogData: DialogData;
}

/**
 * NPC update dialog (Server -> Client)
 * Server sends instruction to update NPC dialog
 */
export type NPCUpdateMessage = BaseMessage<NPCUpdateData>;

/**
 * NPC close dialog data
 */
export interface NPCCloseData {
  /** Chat session ID */
  chatId: string;

  /** NPC ID */
  npcId: string;
}

/**
 * NPC close dialog (Server -> Client or Client -> Server)
 * Either party can close the dialog
 */
export type NPCCloseMessage = BaseMessage<NPCCloseData>;
