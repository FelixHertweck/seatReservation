/**
 * WebSocket message types for supervisor live view
 */

import type {
  SupervisorEventLocationDto,
  SupervisorEventResponseDto,
  SupervisorReservationResponseDto,
  SupervisorSeatStatusDto,
} from "@/api";

/**
 * Initial message sent when a WebSocket connection is established
 * Contains the complete initial state for the event
 */
export interface WebsocketInitialMessage {
  type: "INITIAL";
  location: SupervisorEventLocationDto;
  event: SupervisorEventResponseDto;
  reservations: SupervisorReservationResponseDto[];
}

/**
 * Update message sent when a reservation status changes
 */
export interface WebsocketUpdateMessage {
  type: "UPDATE";
  seatStatus: SupervisorSeatStatusDto;
}

/**
 * Sent when a brand new reservation is created (currently only by the box office flow), carrying
 * the full reservation so clients can add it to their list instead of only patching the live
 * status of a reservation they already knew about (see {@link WebsocketUpdateMessage}).
 */
export interface WebsocketNewReservationMessage {
  type: "NEW_RESERVATION";
  reservation: SupervisorReservationResponseDto;
}

/**
 * Type guard to check if message is an initial message
 */
export function isInitialMessage(
  message: unknown,
): message is WebsocketInitialMessage {
  return (message as WebsocketInitialMessage)?.type === "INITIAL";
}

/**
 * Type guard to check if message is an update message
 */
export function isUpdateMessage(
  message: unknown,
): message is WebsocketUpdateMessage {
  return (message as WebsocketUpdateMessage)?.type === "UPDATE";
}

/**
 * Type guard to check if message is a new-reservation message
 */
export function isNewReservationMessage(
  message: unknown,
): message is WebsocketNewReservationMessage {
  return (message as WebsocketNewReservationMessage)?.type === "NEW_RESERVATION";
}
