/**
 * Local, arithmetic-friendly mirror of a location's seat-map entities.
 * Nested `coordinate: {xCoordinate,yCoordinate}` only appears at the API/JSON
 * boundary (see json-codec.ts) - everything here uses flat x/y.
 */

/** The server UUID once persisted, else a locally-generated "tmp-N" id. */
export type LocalId = string;

export type SyncState = "synced" | "saving" | "error";

export interface EditorSeat {
  localId: LocalId;
  serverId?: string;
  seatNumber: string;
  seatRow: string;
  x: number;
  y: number;
  entranceRef?: LocalId;
  areaRef?: LocalId;
  syncState: SyncState;
}

export interface EditorMarker {
  localId: LocalId;
  serverId?: string;
  label: string;
  x: number;
  y: number;
  syncState: SyncState;
}

export interface EditorPoint {
  x: number;
  y: number;
}

export interface EditorArea {
  localId: LocalId;
  serverId?: string;
  name: string;
  boundary: EditorPoint[];
  syncState: SyncState;
}

export interface EditorEntrance {
  localId: LocalId;
  serverId?: string;
  name: string;
  syncState: SyncState;
}

export interface LocationMeta {
  serverId: string;
  name: string;
  address: string;
  capacity: number;
}

export interface LocationEditorState {
  meta: LocationMeta;
  seats: EditorSeat[];
  markers: EditorMarker[];
  areas: EditorArea[];
  entrances: EditorEntrance[];
}

export type EditorTab = "map" | "preview" | "json";

export type EntityKind = "seat" | "marker" | "area" | "entrance";

export interface SelectionRef {
  kind: EntityKind;
  localId: LocalId;
}

let tmpIdCounter = 0;
export function nextTmpId(): LocalId {
  tmpIdCounter += 1;
  return `tmp-${tmpIdCounter}`;
}

export function emptyLocationEditorState(
  meta: LocationMeta,
): LocationEditorState {
  return { meta, seats: [], markers: [], areas: [], entrances: [] };
}

/**
 * Seats and markers must not share a grid cell - the map renders seats above
 * markers, so an overlapping marker becomes permanently unclickable. Pass
 * `excludeLocalId` when checking a move so the entity doesn't collide with
 * its own current position.
 */
export function isCellOccupied(
  state: LocationEditorState,
  x: number,
  y: number,
  excludeLocalId?: LocalId,
): boolean {
  return (
    state.seats.some(
      (s) => s.localId !== excludeLocalId && s.x === x && s.y === y,
    ) ||
    state.markers.some(
      (m) => m.localId !== excludeLocalId && m.x === x && m.y === y,
    )
  );
}
