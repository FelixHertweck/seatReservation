// Remembers which seats a user had selected when a reservation attempt was interrupted by the
// "add a verified email first" dialog, so they don't have to re-pick everything after coming
// back from /profile or /verify. Read-once: consumed and cleared as soon as it's restored.

const STORAGE_KEY = "pendingReservationSelection";

interface PendingReservationSelection {
  eventId: string;
  seatIds: string[];
}

export function savePendingReservationSelection(
  eventId: string,
  seatIds: string[],
): void {
  if (seatIds.length === 0) return;
  try {
    sessionStorage.setItem(
      STORAGE_KEY,
      JSON.stringify({
        eventId,
        seatIds,
      } satisfies PendingReservationSelection),
    );
  } catch {
    // Storage unavailable (private browsing, etc.) - the user just re-picks seats manually.
  }
}

export function takePendingReservationSelection(): PendingReservationSelection | null {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    sessionStorage.removeItem(STORAGE_KEY);
    const parsed = JSON.parse(raw);
    if (
      typeof parsed?.eventId !== "string" ||
      !Array.isArray(parsed?.seatIds)
    ) {
      return null;
    }
    return parsed as PendingReservationSelection;
  } catch {
    return null;
  }
}
