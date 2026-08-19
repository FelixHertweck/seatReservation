import type { ReservationLiveStatus, ReservationStatus } from "@/api";

/**
 * The single canonical set of "what does this seat/reservation look like" states used
 * across every seatmap, legend, and status badge in the app. Adding a new visual meaning
 * anywhere should mean adding one entry here, not inventing a new color somewhere else.
 */
export type SeatVisualStatus =
  | "AVAILABLE"
  | "SELECTED"
  | "USER_RESERVED"
  | "RESERVED"
  | "BLOCKED"
  | "PENDING"
  | "CHECKED_IN"
  | "CANCELLED"
  | "NO_SHOW";

export const SEAT_STATUS_BG: Record<SeatVisualStatus, string> = {
  AVAILABLE: "bg-green-500 dark:bg-green-600",
  SELECTED: "bg-blue-500 dark:bg-blue-600",
  USER_RESERVED: "bg-yellow-500 dark:bg-yellow-600",
  RESERVED: "bg-red-500 dark:bg-red-600",
  BLOCKED: "bg-gray-500 dark:bg-gray-600",
  PENDING: "bg-purple-500 dark:bg-purple-600",
  CHECKED_IN: "bg-yellow-300 dark:bg-yellow-600",
  CANCELLED: "bg-violet-500 dark:bg-violet-500",
  NO_SHOW: "bg-orange-500 dark:bg-orange-600",
};

// Text color to pair with the (often light) backgrounds above when used on a badge.
export const SEAT_STATUS_TEXT: Partial<Record<SeatVisualStatus, string>> = {
  USER_RESERVED: "text-black dark:text-white",
  CHECKED_IN: "text-black dark:text-white",
  PENDING: "text-white",
};

export const SEAT_STATUS_LABEL_KEY: Record<SeatVisualStatus, string> = {
  AVAILABLE: "seatStatus.available",
  SELECTED: "seatStatus.selected",
  USER_RESERVED: "seatStatus.myReserved",
  RESERVED: "seatStatus.reserved",
  BLOCKED: "seatStatus.blocked",
  PENDING: "seatStatus.pending",
  CHECKED_IN: "seatStatus.checkedIn",
  CANCELLED: "seatStatus.cancelled",
  NO_SHOW: "seatStatus.noShow",
};

/**
 * Maps a reservation's booking status + day-of check-in status to the single visual status
 * that should determine its color/label everywhere. `liveStatus` is only meaningful while
 * `status` is `RESERVED`; a missing (null/undefined) `liveStatus` means "reserved, no
 * check-in decision made yet" - it must NOT be treated as `NO_SHOW`.
 */
export function getSeatVisualStatus(
  status: ReservationStatus | undefined,
  liveStatus?: ReservationLiveStatus | null,
): SeatVisualStatus {
  if (status === "BLOCKED") return "BLOCKED";
  if (status === "PENDING") return "PENDING";
  if (status === "RESERVED") {
    if (liveStatus === "CHECKED_IN") return "CHECKED_IN";
    if (liveStatus === "CANCELLED") return "CANCELLED";
    if (liveStatus === "NO_SHOW") return "NO_SHOW";
    return "RESERVED";
  }
  return "AVAILABLE";
}
