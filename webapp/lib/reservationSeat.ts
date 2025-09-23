import { ReservationStatus, SeatStatusDto } from "@/api";

export function findSeatStatus(
  seatId: bigint | undefined,
  seatStatuses: SeatStatusDto[] | undefined,
): ReservationStatus | undefined {
  if (!seatId || !seatStatuses) return undefined;
  return seatStatuses.find((status) => status.seatId === seatId)?.status;
}
