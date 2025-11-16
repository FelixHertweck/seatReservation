import {
  ReservationStatus,
  SeatStatusDto,
  SupervisorSeatStatusDto,
  ReservationLiveStatus,
} from "@/api";

export function findSeatStatus(
  seatId: bigint | undefined,
  seatStatuses: SeatStatusDto[] | undefined,
): ReservationStatus | undefined {
  if (!seatId || !seatStatuses) return undefined;
  return seatStatuses.find((status) => status.seatId === seatId)?.status;
}

// Type guard to check if a status is SupervisorSeatStatusDto
export function isSupervisorSeatStatus(
  status: unknown,
): status is SupervisorSeatStatusDto {
  return (
    typeof status === "object" && status !== null && "liveStatus" in status
  );
}

// Helper to find supervisor seat status and get live status
export function findSupervisorSeatStatus(
  seatId: bigint | undefined,
  seatStatuses: SupervisorSeatStatusDto[] | undefined,
): SupervisorSeatStatusDto | undefined {
  if (!seatId || !seatStatuses) return undefined;
  return seatStatuses.find((status) => status.seatId === seatId);
}

// Helper to get live status from supervisor seat statuses
export function findLiveStatus(
  seatId: bigint | undefined,
  seatStatuses: SupervisorSeatStatusDto[] | undefined,
): ReservationLiveStatus | undefined {
  const status = findSupervisorSeatStatus(seatId, seatStatuses);
  return status?.liveStatus;
}
