// Shared grid geometry, used both for placing seats/markers/area zones on the
// read-only SeatMap and for the interactive location editor's canvas, so both
// draw from pixel-identical coordinates.

export const SEAT_SIZE = 32;
export const GAP = 4;
export const PADDING = 16;
export const CELL_TOTAL_SIZE = SEAT_SIZE + GAP;
export const ZONE_INSET = 6;

/** Pixel position of the top-left corner of a 1-based grid cell. */
export function cellToPx(coord: number): number {
  return PADDING + (coord - 1) * CELL_TOTAL_SIZE;
}

/** Pixel position of the center of a 1-based grid cell. */
export function cellCenterPx(coord: number): number {
  return cellToPx(coord) + SEAT_SIZE / 2;
}

/** Inverse of cellToPx: nearest 1-based grid cell for a pixel position. */
export function pxToCell(px: number): number {
  return Math.round((px - PADDING) / CELL_TOTAL_SIZE) + 1;
}

/** Full map container size (including outer padding) for `max` cells along an axis. */
export function mapPxSize(max: number): number {
  return max * SEAT_SIZE + (max - 1) * GAP + PADDING * 2;
}

/** Size of just the cell content (no outer padding) for `max` cells along an axis. */
export function gridContentPxSize(max: number): number {
  return max * SEAT_SIZE + (max - 1) * GAP;
}

export interface Point {
  x: number;
  y: number;
}

export interface PolygonBounds {
  left: number;
  top: number;
  width: number;
  height: number;
  /** SVG `points` attribute value, with coordinates relative to (left, top). */
  pointsAttr: string;
  /** Topmost vertex, not the bbox corner - a skewed shape's bbox corner can fall outside it. */
  labelAnchor: Point;
}

export function polygonBounds(points: Point[]): PolygonBounds {
  const left = Math.min(...points.map((p) => p.x));
  const top = Math.min(...points.map((p) => p.y));
  const width = Math.max(...points.map((p) => p.x)) - left;
  const height = Math.max(...points.map((p) => p.y)) - top;

  const topVertex = points.reduce(
    (best, p) => (p.y < best.y ? p : best),
    points[0],
  );

  return {
    left,
    top,
    width,
    height,
    pointsAttr: points.map((p) => `${p.x - left},${p.y - top}`).join(" "),
    labelAnchor: { x: topVertex.x - left, y: topVertex.y - top },
  };
}

/**
 * Converts an area's grid-coordinate boundary into a pixel-space polygon
 * ready to render (bounding box + SVG points), anchoring each point exactly
 * to the center of its referenced grid cell - matching the boundary point
 * positions shown by the editor's draggable vertex handles.
 */
export function boundaryToPixelPolygon(
  boundary: { xCoordinate: number; yCoordinate: number }[],
): PolygonBounds {
  const rawPoints = boundary.map((p) => ({
    x: cellCenterPx(p.xCoordinate),
    y: cellCenterPx(p.yCoordinate),
  }));
  return polygonBounds(rawPoints);
}
