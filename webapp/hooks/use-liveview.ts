import { useState, useCallback, useEffect } from "react";
import { useWebSocket } from "./use-webSocket";
import {
  isInitialMessage,
  isUpdateMessage,
  WebsocketInitialMessage,
  WebsocketUpdateMessage,
} from "@/lib/websocket-types";
import type {
  SupervisorEventLocationDto,
  SupervisorEventResponseDto,
  SupervisorReservationResponseDto,
} from "@/api";
import { getApiSupervisorCheckinEventsOptions } from "@/api/@tanstack/react-query.gen";
import { useQuery } from "@tanstack/react-query";

/**
 * Public interface for the LiveView hook
 * Provides connection status, event data, location data, and reservation statuses
 */
export interface LiveViewState {
  //events data
  events: SupervisorEventResponseDto[] | undefined;
  isLoadingEvents: boolean;
  isErrorEvents: boolean;

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
}

/**
 * Hook for managing LiveView WebSocket connection and state
 *
 * @param eventId - The event ID to connect to
 * @param enabled - Whether the connection should be active (default: true)
 * @returns LiveViewState object with connection and data information
 */
export const useLiveView = (
  eventId: bigint | null,
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

  const {
    data: events,
    isLoading: isLoadingEvents,
    isError: isErrorEvents,
  } = useQuery({
    ...getApiSupervisorCheckinEventsOptions(),
  });

  const handleMessage = useCallback((data: unknown) => {
    const dataWithType = data as
      | WebsocketInitialMessage
      | WebsocketUpdateMessage;
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

  const { isConnected, disconnect } = useWebSocket(
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
    events,
    isLoadingEvents,
    isErrorEvents,
    isConnected,
    isConnecting,
    isInitialLoading,
    event,
    location,
    reservations,
    error,
  };
};
