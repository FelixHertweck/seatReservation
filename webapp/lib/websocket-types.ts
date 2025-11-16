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
 * Union type for all WebSocket messages
 */
export type WebsocketMessage = WebsocketInitialMessage | WebsocketUpdateMessage;

/**
 * Type guard to check if message is an initial message
 */
export function isInitialMessage(
  message: WebsocketMessage,
): message is WebsocketInitialMessage {
  return message.type === "INITIAL";
}

/**
 * Type guard to check if message is an update message
 */
export function isUpdateMessage(
  message: WebsocketMessage,
): message is WebsocketUpdateMessage {
  return message.type === "UPDATE";
}
