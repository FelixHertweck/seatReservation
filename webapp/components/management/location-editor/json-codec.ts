import type {
  EditorMarker,
  EditorSeat,
  LocalId,
  LocationEditorState,
} from "@/components/management/location-editor/types";

/** Mirrors `EventLocationRequestDto` - the same shape the JSON-import flow POSTs. */
export interface LocationJsonDoc {
  name: string;
  address: string;
  capacity: number;
  markers?: {
    label: string;
    coordinate: { xCoordinate: number; yCoordinate: number };
  }[];
  areas?: {
    name: string;
    boundary?: { xCoordinate: number; yCoordinate: number }[];
  }[];
  seats?: {
    seatNumber: string;
    coordinate: { xCoordinate: number; yCoordinate: number };
    seatRow?: string;
    entrance?: string;
    area?: string;
  }[];
}

export function stateToJson(state: LocationEditorState): LocationJsonDoc {
  const areaName = new Map(state.areas.map((a) => [a.localId, a.name]));
  const entranceName = new Map(state.entrances.map((e) => [e.localId, e.name]));

  return {
    name: state.meta.name,
    address: state.meta.address,
    capacity: state.meta.capacity,
    markers: state.markers.map((m) => ({
      label: m.label,
      coordinate: { xCoordinate: m.x, yCoordinate: m.y },
    })),
    areas: state.areas.map((a) => ({
      name: a.name,
      boundary: a.boundary.map((p) => ({
        xCoordinate: p.x,
        yCoordinate: p.y,
      })),
    })),
    seats: state.seats.map((s) => ({
      seatNumber: s.seatNumber,
      coordinate: { xCoordinate: s.x, yCoordinate: s.y },
      seatRow: s.seatRow,
      entrance: s.entranceRef ? entranceName.get(s.entranceRef) : undefined,
      area: s.areaRef ? areaName.get(s.areaRef) : undefined,
    })),
  };
}

export interface Issue {
  message: string;
}

export interface SeatDiffCreate {
  seatNumber: string;
  seatRow: string;
  x: number;
  y: number;
  entranceName?: string;
  areaName?: string;
}
export interface SeatDiffUpdate {
  localId: LocalId;
  changes: Partial<Omit<EditorSeat, "localId" | "syncState">>;
  entranceName?: string;
  areaName?: string;
}

export interface JsonDiff {
  metaChanges: Partial<{ name: string; address: string; capacity: number }>;
  entrancesToCreate: string[];
  entrancesToDelete: LocalId[];
  areasToCreate: { name: string; boundary: { x: number; y: number }[] }[];
  areasToUpdate: {
    localId: LocalId;
    boundary: { x: number; y: number }[];
  }[];
  areasToDelete: LocalId[];
  markersToCreate: Omit<EditorMarker, "localId" | "syncState">[];
  markersToDelete: LocalId[];
  seatsToCreate: SeatDiffCreate[];
  seatsToUpdate: SeatDiffUpdate[];
  seatsToDelete: LocalId[];
}

export interface JsonToDiffResult {
  errors: Issue[];
  warnings: Issue[];
  diff: JsonDiff | null;
}

function boundaryEqual(
  a: { x: number; y: number }[],
  b: { xCoordinate: number; yCoordinate: number }[],
): boolean {
  if (a.length !== b.length) return false;
  return a.every(
    (p, i) => p.x === b[i].xCoordinate && p.y === b[i].yCoordinate,
  );
}

function validateTopLevel(d: Partial<LocationJsonDoc>): Issue[] {
  const errors: Issue[] = [];
  if (!d.name || typeof d.name !== "string") {
    errors.push({ message: 'Missing or invalid "name".' });
  }
  if (!d.address || typeof d.address !== "string") {
    errors.push({ message: 'Missing or invalid "address".' });
  }
  if (typeof d.capacity !== "number" || d.capacity < 0) {
    errors.push({ message: 'Missing or invalid "capacity".' });
  }
  return errors;
}

function validateAreas(areas: NonNullable<LocationJsonDoc["areas"]>): Issue[] {
  const errors: Issue[] = [];
  const nameCounts = new Map<string, number>();
  for (const area of areas) {
    if (!area.name) {
      errors.push({ message: "An area is missing a name." });
      continue;
    }
    nameCounts.set(area.name, (nameCounts.get(area.name) ?? 0) + 1);
    if (area.boundary && area.boundary.length > 0 && area.boundary.length < 3) {
      errors.push({
        message: `Area "${area.name}" needs at least 3 boundary points.`,
      });
    }
  }
  for (const [name, count] of nameCounts) {
    if (count > 1) errors.push({ message: `Duplicate area name "${name}".` });
  }
  return errors;
}

function validateSeats(
  seats: NonNullable<LocationJsonDoc["seats"]>,
  capacity: number | undefined,
): { errors: Issue[]; warnings: Issue[] } {
  const errors: Issue[] = [];
  const warnings: Issue[] = [];
  const cellKeys = new Set<string>();

  for (const seat of seats) {
    if (!seat.seatNumber || !seat.coordinate) {
      errors.push({ message: "A seat is missing its number or coordinate." });
      continue;
    }
    const cellKey = `${seat.coordinate.xCoordinate}-${seat.coordinate.yCoordinate}`;
    if (cellKeys.has(cellKey)) {
      warnings.push({
        message: `More than one seat at (${seat.coordinate.xCoordinate}, ${seat.coordinate.yCoordinate}).`,
      });
    }
    cellKeys.add(cellKey);
  }

  if (typeof capacity === "number" && capacity !== seats.length) {
    warnings.push({
      message: `Capacity (${capacity}) does not match the seat count (${seats.length}).`,
    });
  }

  return { errors, warnings };
}

function validateDoc(
  d: Partial<LocationJsonDoc>,
  areas: NonNullable<LocationJsonDoc["areas"]>,
  seats: NonNullable<LocationJsonDoc["seats"]>,
): { errors: Issue[]; warnings: Issue[] } {
  const seatValidation = validateSeats(seats, d.capacity);
  return {
    errors: [
      ...validateTopLevel(d),
      ...validateAreas(areas),
      ...seatValidation.errors,
    ],
    warnings: seatValidation.warnings,
  };
}

function diffEntrances(
  seats: NonNullable<LocationJsonDoc["seats"]>,
  state: LocationEditorState,
): { toCreate: string[]; toDelete: LocalId[] } {
  const docNames = new Set(
    seats.map((s) => s.entrance).filter((n): n is string => !!n),
  );
  const existingByName = new Map(state.entrances.map((e) => [e.name, e]));
  return {
    toCreate: [...docNames].filter((name) => !existingByName.has(name)),
    toDelete: state.entrances
      .filter((e) => !docNames.has(e.name))
      .map((e) => e.localId),
  };
}

function diffAreas(
  areas: NonNullable<LocationJsonDoc["areas"]>,
  seats: NonNullable<LocationJsonDoc["seats"]>,
  state: LocationEditorState,
): {
  toCreate: JsonDiff["areasToCreate"];
  toUpdate: JsonDiff["areasToUpdate"];
  toDelete: LocalId[];
} {
  const docAreaNames = new Map<string, { x: number; y: number }[]>();
  for (const area of areas) {
    docAreaNames.set(
      area.name,
      (area.boundary ?? []).map((p) => ({
        x: p.xCoordinate,
        y: p.yCoordinate,
      })),
    );
  }
  for (const seat of seats) {
    if (seat.area && !docAreaNames.has(seat.area)) {
      docAreaNames.set(seat.area, []);
    }
  }

  const existingByName = new Map(state.areas.map((a) => [a.name, a]));
  const toCreate: JsonDiff["areasToCreate"] = [];
  const toUpdate: JsonDiff["areasToUpdate"] = [];
  for (const [name, boundary] of docAreaNames) {
    const existing = existingByName.get(name);
    if (!existing) {
      toCreate.push({ name, boundary });
    } else if (
      !boundaryEqual(
        boundary,
        existing.boundary.map((p) => ({ xCoordinate: p.x, yCoordinate: p.y })),
      )
    ) {
      toUpdate.push({ localId: existing.localId, boundary });
    }
  }
  const toDelete = state.areas
    .filter((a) => !docAreaNames.has(a.name))
    .map((a) => a.localId);

  return { toCreate, toUpdate, toDelete };
}

const markerKey = (label: string, x: number, y: number) => `${label}@${x},${y}`;

function diffMarkers(
  markers: NonNullable<LocationJsonDoc["markers"]>,
  state: LocationEditorState,
): { toCreate: JsonDiff["markersToCreate"]; toDelete: LocalId[] } {
  const docKeys = new Set(
    markers.map((m) =>
      markerKey(m.label, m.coordinate.xCoordinate, m.coordinate.yCoordinate),
    ),
  );
  const existingKeys = new Set(
    state.markers.map((m) => markerKey(m.label, m.x, m.y)),
  );

  const toCreate = markers
    .filter(
      (m) =>
        !existingKeys.has(
          markerKey(
            m.label,
            m.coordinate.xCoordinate,
            m.coordinate.yCoordinate,
          ),
        ),
    )
    .map((m) => ({
      label: m.label,
      x: m.coordinate.xCoordinate,
      y: m.coordinate.yCoordinate,
    }));
  const toDelete = state.markers
    .filter((m) => !docKeys.has(markerKey(m.label, m.x, m.y)))
    .map((m) => m.localId);

  return { toCreate, toDelete };
}

const seatKey = (seatNumber: string, seatRow: string | undefined) =>
  `${seatNumber}@${seatRow ?? ""}`;

function diffSeats(
  seats: NonNullable<LocationJsonDoc["seats"]>,
  state: LocationEditorState,
): {
  toCreate: SeatDiffCreate[];
  toUpdate: SeatDiffUpdate[];
  toDelete: LocalId[];
} {
  const existingByKey = new Map(
    state.seats.map((s) => [seatKey(s.seatNumber, s.seatRow), s]),
  );
  const docKeys = new Set(seats.map((s) => seatKey(s.seatNumber, s.seatRow)));

  const toCreate: SeatDiffCreate[] = [];
  const toUpdate: SeatDiffUpdate[] = [];
  for (const seat of seats) {
    const existing = existingByKey.get(seatKey(seat.seatNumber, seat.seatRow));
    if (!existing) {
      toCreate.push({
        seatNumber: seat.seatNumber,
        seatRow: seat.seatRow ?? "",
        x: seat.coordinate.xCoordinate,
        y: seat.coordinate.yCoordinate,
        entranceName: seat.entrance,
        areaName: seat.area,
      });
      continue;
    }
    const currentEntranceName = existing.entranceRef
      ? state.entrances.find((e) => e.localId === existing.entranceRef)?.name
      : undefined;
    const currentAreaName = existing.areaRef
      ? state.areas.find((a) => a.localId === existing.areaRef)?.name
      : undefined;
    const changed =
      existing.x !== seat.coordinate.xCoordinate ||
      existing.y !== seat.coordinate.yCoordinate ||
      (currentEntranceName ?? undefined) !== (seat.entrance ?? undefined) ||
      (currentAreaName ?? undefined) !== (seat.area ?? undefined);
    if (changed) {
      toUpdate.push({
        localId: existing.localId,
        changes: {
          x: seat.coordinate.xCoordinate,
          y: seat.coordinate.yCoordinate,
        },
        entranceName: seat.entrance,
        areaName: seat.area,
      });
    }
  }
  const toDelete = state.seats
    .filter((s) => !docKeys.has(seatKey(s.seatNumber, s.seatRow)))
    .map((s) => s.localId);

  return { toCreate, toUpdate, toDelete };
}

/**
 * Diffs a parsed JSON document against the current editor state, matching
 * entities by stable business keys (not local ids) so a JSON edit can't
 * accidentally destroy server ids.
 */
export function jsonToDiff(
  doc: unknown,
  state: LocationEditorState,
): JsonToDiffResult {
  if (typeof doc !== "object" || doc === null) {
    return {
      errors: [{ message: "Invalid JSON document." }],
      warnings: [],
      diff: null,
    };
  }
  const d = doc as Partial<LocationJsonDoc>;
  const markers = d.markers ?? [];
  const areas = d.areas ?? [];
  const seats = d.seats ?? [];

  const { errors, warnings } = validateDoc(d, areas, seats);
  if (errors.length > 0) {
    return { errors, warnings, diff: null };
  }

  const entrances = diffEntrances(seats, state);
  const areasDiff = diffAreas(areas, seats, state);
  const markersDiff = diffMarkers(markers, state);
  const seatsDiff = diffSeats(seats, state);

  const metaChanges: JsonDiff["metaChanges"] = {};
  if (d.name !== state.meta.name) metaChanges.name = d.name;
  if (d.address !== state.meta.address) metaChanges.address = d.address;
  if (d.capacity !== state.meta.capacity) metaChanges.capacity = d.capacity;

  return {
    errors,
    warnings,
    diff: {
      metaChanges,
      entrancesToCreate: entrances.toCreate,
      entrancesToDelete: entrances.toDelete,
      areasToCreate: areasDiff.toCreate,
      areasToUpdate: areasDiff.toUpdate,
      areasToDelete: areasDiff.toDelete,
      markersToCreate: markersDiff.toCreate,
      markersToDelete: markersDiff.toDelete,
      seatsToCreate: seatsDiff.toCreate,
      seatsToUpdate: seatsDiff.toUpdate,
      seatsToDelete: seatsDiff.toDelete,
    },
  };
}

export function isDiffEmpty(diff: JsonDiff): boolean {
  return (
    Object.keys(diff.metaChanges).length === 0 &&
    diff.entrancesToCreate.length === 0 &&
    diff.entrancesToDelete.length === 0 &&
    diff.areasToCreate.length === 0 &&
    diff.areasToUpdate.length === 0 &&
    diff.areasToDelete.length === 0 &&
    diff.markersToCreate.length === 0 &&
    diff.markersToDelete.length === 0 &&
    diff.seatsToCreate.length === 0 &&
    diff.seatsToUpdate.length === 0 &&
    diff.seatsToDelete.length === 0
  );
}
