"use client";

import {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { useQuery } from "@tanstack/react-query";

import type { SupervisorEventResponseDto } from "@/api";
import { getApiSupervisorCheckinEventsOptions } from "@/api/@tanstack/react-query.gen";
import { useLiveView, type LiveViewState } from "@/hooks/use-liveview";
import { useSelectedSupervisorEventId } from "@/hooks/use-supervisor-event";

export interface SupervisorLiveViewState extends LiveViewState {
  // Events whose booking deadline has passed.
  availableEvents: SupervisorEventResponseDto[];
  selectedEvent: SupervisorEventResponseDto | undefined;
  // False until the events and the current time are loaded.
  isDeadlineKnown: boolean;
  isDeadlinePassed: boolean;
}

const SupervisorLiveViewContext = createContext<
  SupervisorLiveViewState | undefined
>(undefined);

// One shared websocket for all /supervisor pages.
export function SupervisorLiveViewProvider({
  children,
}: {
  children: ReactNode;
}) {
  const selectedEventId = useSelectedSupervisorEventId();
  const { data: events, isLoading: isLoadingEvents } = useQuery({
    ...getApiSupervisorCheckinEventsOptions(),
  });

  // Refreshed periodically so an event crossing its deadline becomes available.
  const [now, setNow] = useState<number | null>(null);
  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setNow(Date.now());
    const interval = setInterval(() => setNow(Date.now()), 60_000);
    return () => clearInterval(interval);
  }, []);

  const availableEvents = useMemo(() => {
    if (now === null) return [];
    return (events ?? []).filter(
      (e) => !!e.bookingDeadline && new Date(e.bookingDeadline).getTime() < now,
    );
  }, [events, now]);
  const selectedEvent = useMemo(
    () => (events ?? []).find((e) => e.id === selectedEventId),
    [events, selectedEventId],
  );
  const isDeadlinePassed =
    now !== null &&
    !!selectedEvent?.bookingDeadline &&
    new Date(selectedEvent.bookingDeadline).getTime() < now;

  // The backend rejects connections before the booking deadline.
  const liveView = useLiveView(
    selectedEventId,
    !!selectedEventId && isDeadlinePassed,
  );

  const value = {
    ...liveView,
    availableEvents,
    selectedEvent,
    isDeadlineKnown: !isLoadingEvents && now !== null,
    isDeadlinePassed,
  };

  return (
    <SupervisorLiveViewContext.Provider value={value}>
      {children}
    </SupervisorLiveViewContext.Provider>
  );
}

export function useSupervisorLiveView(): SupervisorLiveViewState {
  const context = useContext(SupervisorLiveViewContext);
  if (!context) {
    throw new Error(
      "useSupervisorLiveView must be used within a SupervisorLiveViewProvider",
    );
  }
  return context;
}
