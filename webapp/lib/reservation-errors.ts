import type { ErrorWithResponse } from "@/components/init-query-client";

// Mirrors ReservationEmailRequiredException, which the backend maps to 422.
export function isReservationEmailRequiredError(error: unknown): boolean {
  return (error as ErrorWithResponse)?.response?.status === 422;
}
