import { useState, useCallback, useEffect } from "react";
import { useWebSocket } from "./use-webSocket";
import {
  isGuestAssignedMessage,
  isGuestRemovedMessage,
  isInitialMessage,
  isReservationUpdateMessage,
  isUpdateMessage,
  type GuestSeatAssignmentDto,
  type GuestSeatAssignRequestDto,
  type WebsocketInitialMessage,
  type WebsocketUpdateMessage,
} from "@/lib/websocket-types";
import type {
  ReservationRequestDto,
  ReservationResponseDto,
  SupervisorEventLocationDto,
  SupervisorEventResponseDto,
  SupervisorReservationResponseDto,
  UserDto,
} from "@/api";
import {
  getApiSupervisorCheckinEventsOptions,
  getApiUsersManagerOptions,
  postApiManagerReservationsMutation,
} from "@/api/@tanstack/react-query.gen";
import { useMutation, useQuery } from "@tanstack/react-query";
import { toast } from "sonner";
import { useT } from "@/lib/i18n/hooks";
import type { ErrorWithResponse } from "@/components/init-query-client";

/**
 * Public interface for the LiveView hook
 * Provides connection status, event data, location data, and reservation statuses
 */
export interface LiveViewState {
  //events data
  events: SupervisorEventResponseDto[] | undefined;
  isLoadingEvents: boolean;
  isErrorEvents: boolean;

  // Users data
  users: UserDto[];
  isLoadingUsers: boolean;

  // Connection status
  isConnected: boolean;
  isConnecting: boolean;
  isInitialLoading: boolean;

  // Event data
  event: SupervisorEventResponseDto | null;
  location: SupervisorEventLocationDto | null;

  reservations: SupervisorReservationResponseDto[];
  guestAssignments: GuestSeatAssignmentDto[];

  // Reservation creation
  createReservation: (
    data: ReservationRequestDto,
  ) => Promise<ReservationResponseDto[]>;
  isSubmittingReservation: boolean;

  // Guest seat assignment
  assignGuestSeats: (data: GuestSeatAssignRequestDto) => Promise<GuestSeatAssignmentDto[]>;
  removeGuestAssignment: (id: string) => Promise<void>;

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
  eventId: string | null,
  enabled: boolean = true,
): LiveViewState => {
  const t = useT();
  const [isConnecting, setIsConnecting] = useState(false);
  const [isInitialLoading, setIsInitialLoading] = useState(false);
  const [event, setEvent] = useState<SupervisorEventResponseDto | null>(null);
  const [location, setLocation] = useState<SupervisorEventLocationDto | null>(
    null,
  );
  const [reservations, setReservations] = useState<
    SupervisorReservationResponseDto[]
  >([]);
  const [guestAssignments, setGuestAssignments] = useState<
    GuestSeatAssignmentDto[]
  >([]);
  const [error, setError] = useState<string | null>(null);

  const {
    data: events,
    isLoading: isLoadingEvents,
    isError: isErrorEvents,
  } = useQuery({
    ...getApiSupervisorCheckinEventsOptions(),
  });

  const { data: users, isLoading: isLoadingUsers } = useQuery({
    ...getApiUsersManagerOptions(),
    enabled: enabled,
  });

  const createMutation = useMutation({
    ...postApiManagerReservationsMutation(),
  });

  const createReservation = async (data: ReservationRequestDto) => {
    const request = createMutation.mutateAsync({ body: data });
    toast.promise(request, {
      loading: t("common.loading"),
      success: t("management.reservations.createSuccess"),
      error: (error: ErrorWithResponse) => ({
        message: t("management.reservations.createError"),
        description: error.response?.description ?? t("common.error.default"),
      }),
    });
    return request;
  };

  const assignGuestSeats = async (data: GuestSeatAssignRequestDto): Promise<GuestSeatAssignmentDto[]> => {
    const response = await fetch("/api/supervisor/liveview/guest-assignments", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(data),
    });
    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(
        errorData.message ||
          t("liveview.guestAssignError") ||
          "Fehler bei der Platzvergabe",
      );
    }
    return response.json();
  };

  const removeGuestAssignment = async (id: string): Promise<void> => {
    const response = await fetch(
      `/api/supervisor/liveview/guest-assignments/${id}`,
      { method: "DELETE" },
    );
    if (!response.ok) {
      throw new Error(t("common.error.default"));
    }
  };

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
        setGuestAssignments(initialData.guestAssignments ?? []);
        setError(null);

        setIsInitialLoading(false);
      } else if (isUpdateMessage(dataWithType)) {
        if (isReservationUpdateMessage(dataWithType)) {
          const updatedSeatStatus = dataWithType.seatStatus;
          setReservations((prevReservations) => {
            return prevReservations.map((res) => {
              if (res.seat?.id === updatedSeatStatus.seatId) {
                return {
                  ...res,
                  liveStatus: updatedSeatStatus.liveStatus,
                };
              }
              return res;
            });
          });
        } else if (isGuestAssignedMessage(dataWithType)) {
          const newAssignment = dataWithType.guestAssignment;
          setGuestAssignments((prev) => {
            const exists = prev.some((a) => a.id === newAssignment.id);
            if (exists) return prev;
            return [...prev, newAssignment];
          });
        } else if (isGuestRemovedMessage(dataWithType)) {
          const removedSeatId = dataWithType.removedSeatId;
          setGuestAssignments((prev) =>
            prev.filter((a) => a.seat?.id !== removedSeatId),
          );
        }

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
    users: users ?? [],
    isLoadingUsers,
    isConnected,
    isConnecting,
    isInitialLoading,
    event,
    location,
    reservations,
    guestAssignments,
    createReservation,
    isSubmittingReservation: createMutation.isPending,
    assignGuestSeats,
    removeGuestAssignment,
    error,
  };
};
