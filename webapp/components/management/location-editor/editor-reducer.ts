import {
  emptyPendingDeletions,
  type EditorArea,
  type EditorEntrance,
  type EditorMarker,
  type EditorSeat,
  type EntityKind,
  type LocalId,
  type LocationEditorState,
  type LocationMeta,
  type SyncState,
} from "@/components/management/location-editor/types";

function withPendingDeletion<T extends { localId: string; serverId?: string }>(
  pending: LocationEditorState["pendingDeletions"],
  kind: keyof LocationEditorState["pendingDeletions"],
  removed: T[],
): LocationEditorState["pendingDeletions"] {
  const newIds = removed
    .map((item) => item.serverId)
    .filter((id): id is string => !!id);
  if (newIds.length === 0) return pending;
  return { ...pending, [kind]: [...pending[kind], ...newIds] };
}

// ADD_* actions take a caller-provided `localId` (generated via nextTmpId()
// by the autosave hook right before dispatching) rather than generating one
// inside the reducer - the autosave hook needs that id back immediately, in
// the same tick, to fire the create request and later reconcile it.
export type EditorAction =
  | { type: "HYDRATE"; state: LocationEditorState }
  | { type: "SET_META"; meta: Partial<Omit<LocationMeta, "serverId">> }
  | { type: "ADD_SEAT"; seat: Omit<EditorSeat, "syncState"> }
  | {
      type: "ADD_SEATS_BULK";
      seats: Omit<EditorSeat, "syncState">[];
    }
  | {
      type: "UPDATE_SEAT";
      localId: LocalId;
      changes: Partial<Omit<EditorSeat, "localId" | "syncState">>;
    }
  | { type: "MOVE_ENTITIES"; ids: Set<LocalId>; dx: number; dy: number }
  | { type: "DELETE_ENTITIES"; ids: Set<LocalId> }
  | { type: "ADD_MARKER"; marker: Omit<EditorMarker, "syncState"> }
  | {
      type: "UPDATE_MARKER";
      localId: LocalId;
      changes: Partial<Omit<EditorMarker, "localId" | "syncState">>;
    }
  | { type: "ADD_AREA"; area: Omit<EditorArea, "syncState"> }
  | {
      type: "UPDATE_AREA";
      localId: LocalId;
      changes: Partial<Omit<EditorArea, "localId" | "syncState">>;
    }
  | {
      type: "MOVE_AREA_POINT";
      areaLocalId: LocalId;
      index: number;
      x: number;
      y: number;
    }
  | {
      type: "INSERT_AREA_POINT";
      areaLocalId: LocalId;
      index: number;
      x: number;
      y: number;
    }
  | { type: "DELETE_AREA_POINT"; areaLocalId: LocalId; index: number }
  | { type: "DELETE_AREAS"; ids: Set<LocalId> }
  | {
      type: "ADD_ENTRANCE";
      entrance: Omit<EditorEntrance, "syncState">;
    }
  | { type: "UPDATE_ENTRANCE"; localId: LocalId; name: string }
  | { type: "DELETE_ENTRANCES"; ids: Set<LocalId> }
  | { type: "ASSIGN_AREA_TO_SEATS"; seatIds: Set<LocalId>; areaRef?: LocalId }
  | { type: "SHIFT_ALL"; dx: number; dy: number }
  | {
      type: "SET_SYNC_STATE";
      kind: EntityKind;
      localId: LocalId;
      syncState: SyncState;
    }
  | {
      type: "RECONCILE";
      kind: EntityKind;
      localId: LocalId;
      serverId: string;
    }
  | { type: "REMOVE_LOCAL"; kind: EntityKind; localId: LocalId }
  | { type: "REPLACE_STATE"; state: LocationEditorState }
  | { type: "SAVE_SUCCESS" };

function withSyncState<T extends { syncState: SyncState }>(
  items: T[],
  ids: Set<string>,
  syncState: SyncState,
): T[] {
  let changed = false;
  const next = items.map((item) => {
    if ("localId" in item && ids.has((item as { localId: string }).localId)) {
      changed = true;
      return { ...item, syncState };
    }
    return item;
  });
  return changed ? next : items;
}

export function editorReducer(
  state: LocationEditorState,
  action: EditorAction,
): LocationEditorState {
  switch (action.type) {
    case "HYDRATE":
    case "REPLACE_STATE":
      return action.state;

    case "SET_META":
      return {
        ...state,
        meta: { ...state.meta, ...action.meta },
        metaDirty: true,
      };

    case "ADD_SEAT":
      return {
        ...state,
        seats: [...state.seats, { ...action.seat, syncState: "dirty" }],
      };

    case "ADD_SEATS_BULK":
      return {
        ...state,
        seats: [
          ...state.seats,
          ...action.seats.map((seat) => ({
            ...seat,
            syncState: "dirty" as const,
          })),
        ],
      };

    case "UPDATE_SEAT":
      return {
        ...state,
        seats: state.seats.map((seat) =>
          seat.localId === action.localId
            ? { ...seat, ...action.changes, syncState: "dirty" }
            : seat,
        ),
      };

    case "MOVE_ENTITIES":
      return {
        ...state,
        seats: state.seats.map((seat) =>
          action.ids.has(seat.localId)
            ? {
                ...seat,
                x: seat.x + action.dx,
                y: seat.y + action.dy,
                syncState: "dirty",
              }
            : seat,
        ),
        markers: state.markers.map((marker) =>
          action.ids.has(marker.localId)
            ? {
                ...marker,
                x: marker.x + action.dx,
                y: marker.y + action.dy,
                syncState: "dirty",
              }
            : marker,
        ),
      };

    case "DELETE_ENTITIES": {
      const removedSeats = state.seats.filter((seat) =>
        action.ids.has(seat.localId),
      );
      const removedMarkers = state.markers.filter((marker) =>
        action.ids.has(marker.localId),
      );
      const afterSeats = withPendingDeletion(
        state.pendingDeletions,
        "seat",
        removedSeats,
      );
      return {
        ...state,
        seats: state.seats.filter((seat) => !action.ids.has(seat.localId)),
        markers: state.markers.filter(
          (marker) => !action.ids.has(marker.localId),
        ),
        pendingDeletions: withPendingDeletion(
          afterSeats,
          "marker",
          removedMarkers,
        ),
      };
    }

    case "ADD_MARKER":
      return {
        ...state,
        markers: [...state.markers, { ...action.marker, syncState: "dirty" }],
      };

    case "UPDATE_MARKER":
      return {
        ...state,
        markers: state.markers.map((marker) =>
          marker.localId === action.localId
            ? { ...marker, ...action.changes, syncState: "dirty" }
            : marker,
        ),
      };

    case "ADD_AREA":
      return {
        ...state,
        areas: [...state.areas, { ...action.area, syncState: "dirty" }],
      };

    case "UPDATE_AREA":
      return {
        ...state,
        areas: state.areas.map((area) =>
          area.localId === action.localId
            ? { ...area, ...action.changes, syncState: "dirty" }
            : area,
        ),
      };

    case "MOVE_AREA_POINT":
      return {
        ...state,
        areas: state.areas.map((area) =>
          area.localId === action.areaLocalId
            ? {
                ...area,
                boundary: area.boundary.map((p, i) =>
                  i === action.index ? { x: action.x, y: action.y } : p,
                ),
                syncState: "dirty",
              }
            : area,
        ),
      };

    case "INSERT_AREA_POINT":
      return {
        ...state,
        areas: state.areas.map((area) =>
          area.localId === action.areaLocalId
            ? {
                ...area,
                boundary: [
                  ...area.boundary.slice(0, action.index),
                  { x: action.x, y: action.y },
                  ...area.boundary.slice(action.index),
                ],
                syncState: "dirty",
              }
            : area,
        ),
      };

    case "DELETE_AREA_POINT":
      return {
        ...state,
        areas: state.areas.map((area) =>
          area.localId === action.areaLocalId
            ? {
                ...area,
                boundary: area.boundary.filter((_, i) => i !== action.index),
                syncState: "dirty",
              }
            : area,
        ),
      };

    case "DELETE_AREAS": {
      const removedAreas = state.areas.filter((area) =>
        action.ids.has(area.localId),
      );
      return {
        ...state,
        areas: state.areas.filter((area) => !action.ids.has(area.localId)),
        seats: state.seats.map((seat) =>
          seat.areaRef && action.ids.has(seat.areaRef)
            ? { ...seat, areaRef: undefined, syncState: "dirty" }
            : seat,
        ),
        pendingDeletions: withPendingDeletion(
          state.pendingDeletions,
          "area",
          removedAreas,
        ),
      };
    }

    case "ADD_ENTRANCE":
      return {
        ...state,
        entrances: [
          ...state.entrances,
          { ...action.entrance, syncState: "dirty" },
        ],
      };

    case "UPDATE_ENTRANCE":
      return {
        ...state,
        entrances: state.entrances.map((entrance) =>
          entrance.localId === action.localId
            ? { ...entrance, name: action.name, syncState: "dirty" }
            : entrance,
        ),
      };

    case "DELETE_ENTRANCES": {
      const removedEntrances = state.entrances.filter((entrance) =>
        action.ids.has(entrance.localId),
      );
      return {
        ...state,
        entrances: state.entrances.filter(
          (entrance) => !action.ids.has(entrance.localId),
        ),
        seats: state.seats.map((seat) =>
          seat.entranceRef && action.ids.has(seat.entranceRef)
            ? { ...seat, entranceRef: undefined, syncState: "dirty" }
            : seat,
        ),
        pendingDeletions: withPendingDeletion(
          state.pendingDeletions,
          "entrance",
          removedEntrances,
        ),
      };
    }

    case "ASSIGN_AREA_TO_SEATS":
      return {
        ...state,
        seats: state.seats.map((seat) =>
          action.seatIds.has(seat.localId)
            ? { ...seat, areaRef: action.areaRef, syncState: "dirty" }
            : seat,
        ),
      };

    case "SHIFT_ALL":
      return {
        ...state,
        seats: state.seats.map((seat) => ({
          ...seat,
          x: seat.x + action.dx,
          y: seat.y + action.dy,
          syncState: "dirty",
        })),
        markers: state.markers.map((marker) => ({
          ...marker,
          x: marker.x + action.dx,
          y: marker.y + action.dy,
          syncState: "dirty",
        })),
        areas: state.areas.map((area) => ({
          ...area,
          boundary: area.boundary.map((p) => ({
            x: p.x + action.dx,
            y: p.y + action.dy,
          })),
          syncState: "dirty",
        })),
      };

    case "SET_SYNC_STATE": {
      const ids = new Set([action.localId]);
      switch (action.kind) {
        case "seat":
          return {
            ...state,
            seats: withSyncState(state.seats, ids, action.syncState),
          };
        case "marker":
          return {
            ...state,
            markers: withSyncState(state.markers, ids, action.syncState),
          };
        case "area":
          return {
            ...state,
            areas: withSyncState(state.areas, ids, action.syncState),
          };
        case "entrance":
          return {
            ...state,
            entrances: withSyncState(state.entrances, ids, action.syncState),
          };
      }
      break;
    }

    case "RECONCILE": {
      const apply = <T extends { localId: string; serverId?: string }>(
        items: T[],
      ): T[] =>
        items.map((item) =>
          item.localId === action.localId
            ? {
                ...item,
                serverId: action.serverId,
                syncState: "synced" as const,
              }
            : item,
        );
      switch (action.kind) {
        case "seat":
          return { ...state, seats: apply(state.seats) };
        case "marker":
          return { ...state, markers: apply(state.markers) };
        case "area":
          return { ...state, areas: apply(state.areas) };
        case "entrance":
          return { ...state, entrances: apply(state.entrances) };
      }
      break;
    }

    case "REMOVE_LOCAL":
      switch (action.kind) {
        case "seat":
          return {
            ...state,
            seats: state.seats.filter((s) => s.localId !== action.localId),
          };
        case "marker":
          return {
            ...state,
            markers: state.markers.filter((m) => m.localId !== action.localId),
          };
        case "area":
          return {
            ...state,
            areas: state.areas.filter((a) => a.localId !== action.localId),
          };
        case "entrance":
          return {
            ...state,
            entrances: state.entrances.filter(
              (e) => e.localId !== action.localId,
            ),
          };
      }
      break;

    case "SAVE_SUCCESS":
      return {
        ...state,
        metaDirty: false,
        pendingDeletions: emptyPendingDeletions(),
      };

    default:
      return state;
  }
  return state;
}
