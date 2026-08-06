import type {
  SeatDto,
  SupervisorEventLocationDto,
  SupervisorEventResponseDto,
  SupervisorReservationResponseDto,
  SupervisorSeatStatusDto,
} from "@/api";

export interface GuestSeatAssignmentDto {
  id?: string;
  eventId?: string;
  seat?: SeatDto;
  guestName?: string;
  assignedByUsername?: string;
  assignedAt?: string;
}

export interface GuestSeatAssignRequestDto {
  eventId: string;
  seatIds: string[];
  guestName: string;
}

/**
 * Initial message sent when a WebSocket connection is established
 * Contains the complete initial state for the event
 */
export interface WebsocketInitialMessage {
  type: "INITIAL";
  location: SupervisorEventLocationDto;
  event: SupervisorEventResponseDto;
  reservations: SupervisorReservationResponseDto[];
  guestAssignments?: GuestSeatAssignmentDto[];
}

export interface WebsocketReservationUpdateMessage {
  type: "UPDATE";
  seatStatus: SupervisorSeatStatusDto;
}

export interface WebsocketGuestAssignedMessage {
  type: "GUEST_ASSIGNED";
  guestAssignment: GuestSeatAssignmentDto;
}

export interface WebsocketGuestRemovedMessage {
  type: "GUEST_REMOVED";
  removedSeatId: string;
}

/**
 * Update message sent when a reservation status or guest assignment changes
 */
export type WebsocketUpdateMessage =
  | WebsocketReservationUpdateMessage
  | WebsocketGuestAssignedMessage
  | WebsocketGuestRemovedMessage;

/**
 * Type guard to check if message is an initial message
 */
export function isInitialMessage(
  message: unknown,
): message is WebsocketInitialMessage {
  return (message as WebsocketInitialMessage)?.type === "INITIAL";
}

/**
 * Type guard to check if message is a standard update message
 */
export function isReservationUpdateMessage(
  message: unknown,
): message is WebsocketReservationUpdateMessage {
  return (message as WebsocketReservationUpdateMessage)?.type === "UPDATE";
}

/**
 * Type guard to check if message is a guest assigned update message
 */
export function isGuestAssignedMessage(
  message: unknown,
): message is WebsocketGuestAssignedMessage {
  return (message as WebsocketGuestAssignedMessage)?.type === "GUEST_ASSIGNED";
}

/**
 * Type guard to check if message is a guest removed update message
 */
export function isGuestRemovedMessage(
  message: unknown,
): message is WebsocketGuestRemovedMessage {
  return (message as WebsocketGuestRemovedMessage)?.type === "GUEST_REMOVED";
}

/**
 * Type guard to check if message is any update message
 */
export function isUpdateMessage(
  message: unknown,
): message is WebsocketUpdateMessage {
  const type = (message as { type?: string })?.type;
  return (
    type === "UPDATE" || type === "GUEST_ASSIGNED" || type === "GUEST_REMOVED"
  );
}
