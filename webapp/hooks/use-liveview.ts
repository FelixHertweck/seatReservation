import { useState, useCallback, useEffect } from "react";
import { useWebSocket } from "./use-webSocket";
import {
  isInitialMessage,
  isNewReservationMessage,
  isUpdateMessage,
  WebsocketInitialMessage,
  WebsocketNewReservationMessage,
  WebsocketUpdateMessage,
} from "@/lib/websocket-types";
import { ReservationLiveStatus } from "@/api";
import type {
  SupervisorEventLocationDto,
  SupervisorEventResponseDto,
  SupervisorReservationResponseDto,
} from "@/api";

/**
 * Public interface for the LiveView hook
 * Provides connection status, event data, location data, and reservation statuses
 */
export interface LiveViewState {
  // Connection status
  isConnected: boolean;
  isConnecting: boolean;
  isInitialLoading: boolean;

  // Event data
  event: SupervisorEventResponseDto | null;
  location: SupervisorEventLocationDto | null;

  reservations: SupervisorReservationResponseDto[];

  // Error information
  error: string | null;

  reconnect: () => void;
}

/**
 * Hook for managing LiveView WebSocket connection and state
 *
 * @param eventId - The event ID to connect to
 * @param enabled - Whether the connection should be active (default: true)
 * @returns LiveViewState object with connection and data information
 */
export const useLiveView = (
  eventId: string | null,
  enabled: boolean = true,
): LiveViewState => {
  const [isConnecting, setIsConnecting] = useState(false);
  const [isInitialLoading, setIsInitialLoading] = useState(false);
  const [event, setEvent] = useState<SupervisorEventResponseDto | null>(null);
  const [location, setLocation] = useState<SupervisorEventLocationDto | null>(
    null,
  );
  const [reservations, setReservations] = useState<
    SupervisorReservationResponseDto[]
  >([]);
  const [error, setError] = useState<string | null>(null);

  // Drop the previous event's data when another one is selected.
  const [prevEventId, setPrevEventId] = useState(eventId);
  if (prevEventId !== eventId) {
    setPrevEventId(eventId);
    setEvent(null);
    setLocation(null);
    setReservations([]);
    setError(null);
  }

  // Count as connecting from the render that enables the connection.
  const isActive = enabled && !!eventId;
  const [prevIsActive, setPrevIsActive] = useState(false);
  if (prevIsActive !== isActive) {
    setPrevIsActive(isActive);
    if (isActive) {
      setIsConnecting(true);
      setIsInitialLoading(true);
    }
  }

  const handleMessage = useCallback((data: unknown) => {
    const dataWithType = data as
      | WebsocketInitialMessage
      | WebsocketUpdateMessage
      | WebsocketNewReservationMessage;
    try {
      if (isInitialMessage(dataWithType)) {
        const initialData = data as WebsocketInitialMessage;

        setLocation(initialData.location);
        setEvent(initialData.event);
        setReservations(initialData.reservations);
        setError(null);

        setIsInitialLoading(false);
      } else if (isUpdateMessage(dataWithType)) {
        const updatedSeatStatus = data as WebsocketUpdateMessage;
        setReservations((prevReservations) => {
          return prevReservations.map((res) => {
            if (res.seat?.id === updatedSeatStatus.seatStatus.seatId) {
              return {
                ...res,
                liveStatus: updatedSeatStatus.seatStatus.liveStatus,
              };
            }
            return res;
          });
        });

        setIsInitialLoading(false);
      } else if (isNewReservationMessage(dataWithType)) {
        const newReservation = dataWithType.reservation;
        setReservations((prevReservations) => {
          if (prevReservations.some((res) => res.id === newReservation.id)) {
            return prevReservations;
          }
          // A new reservation replaces a cancelled one on the same seat.
          return [
            ...prevReservations.filter(
              (res) =>
                !newReservation.seat?.id ||
                res.seat?.id !== newReservation.seat.id ||
                res.liveStatus !== ReservationLiveStatus.CANCELLED,
            ),
            newReservation,
          ];
        });

        setIsInitialLoading(false);
      } else {
        console.warn("[useLiveView] Unknown message type:", data);
      }
    } catch (err) {
      const errorMessage =
        err instanceof Error ? err.message : "Unknown error processing message";
      console.error("[useLiveView] Error handling message:", err);
      setError(errorMessage);
    }
  }, []);

  const { isConnected, disconnect, reconnect } = useWebSocket(
    eventId
      ? `${window.location.protocol === "https:" ? "wss:" : "ws:"}//${
          window.location.host
        }/api/supervisor/liveview/${eventId}`
      : null,
    enabled,
    handleMessage,
    5,
    3000,
    (connecting: boolean) => {
      try {
        // manage local connecting state via the callback so we avoid setting state inside effects
        setIsConnecting(connecting);
        if (connecting) {
          setIsInitialLoading(true);
        } else {
          setIsInitialLoading(false);
        }
      } catch (err) {
        console.error(
          "[useLiveView] Error handling onConnecting callback:",
          err,
        );
      }
    },
  );
  useEffect(() => {
    if (!enabled || !eventId) {
      disconnect();
    }
  }, [enabled, eventId, disconnect]);

  return {
    isConnected,
    isConnecting,
    isInitialLoading,
    event,
    location,
    reservations,
    error,
    reconnect,
  };
};
