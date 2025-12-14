/**
 * Vector3 - 3D position/offset
 */
import {Vector3} from "./Vector3";

export interface Vector3Int extends Vector3 {
  x: number; // javaType: int
  y: number; // javaType: int
  z: number; // javaType: int
}
