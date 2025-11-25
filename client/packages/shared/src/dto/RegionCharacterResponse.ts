// Auto-generated TypeScript interface mirroring Java DTO RegionCharacterResponse
// Quelle: shared/src/main/java/de/mhus/nimbus/shared/dto/region/RegionCharacterResponse.java
// Felder:
//  - id
//  - userId
//  - name
//  - display
//  - backpack: Map<String, RegionItemInfo>
//  - wearing: Map<Integer, RegionItemInfo>
//  - skills: Map<String, Integer>
//  - regionId

import { RegionItemInfo } from '../types/RegionItemInfo';

export interface RegionCharacterResponse {
  id: string;
  userId: string;
  regionId: string;
  name: string;
  display: string;
  backpack: Record<string, RegionItemInfo>;
  wearing: Record<number, RegionItemInfo>; // numerische Slots
  skills: Record<string, number>; // Skill-Name -> Level
}
