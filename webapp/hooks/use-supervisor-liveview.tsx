"use client";

import {
  createContext,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from "react";
import { useQuery } from "@tanstack/react-query";

import { getApiSupervisorCheckinEventsOptions } from "@/api/@tanstack/react-query.gen";
import { useLiveView, type LiveViewState } from "@/hooks/use-liveview";
import { useSelectedSupervisorEventId } from "@/hooks/use-supervisor-event";

const SupervisorLiveViewContext = createContext<LiveViewState | undefined>(
  undefined,
);

function LiveViewConnection({
  eventId,
  enabled,
  children,
}: {
  eventId: string | null;
  enabled: boolean;
  children: ReactNode;
}) {
  const liveView = useLiveView(eventId, enabled);
  return (
    <SupervisorLiveViewContext.Provider value={liveView}>
      {children}
    </SupervisorLiveViewContext.Provider>
  );
}

// One shared websocket for all /supervisor pages.
export function SupervisorLiveViewProvider({
  children,
}: {
  children: ReactNode;
}) {
  const selectedEventId = useSelectedSupervisorEventId();
  const { data: events } = useQuery({
    ...getApiSupervisorCheckinEventsOptions(),
  });

  const [now, setNow] = useState<number | null>(null);
  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setNow(Date.now());
    const interval = setInterval(() => setNow(Date.now()), 60_000);
    return () => clearInterval(interval);
  }, []);

  // The backend rejects connections before the booking deadline.
  const bookingDeadline = (events ?? []).find(
    (e) => e.id === selectedEventId,
  )?.bookingDeadline;
  const isDeadlinePassed =
    now !== null &&
    !!bookingDeadline &&
    new Date(bookingDeadline).getTime() < now;

  return (
    // Keyed by event to reset the state when another one is selected.
    <LiveViewConnection
      key={selectedEventId ?? "none"}
      eventId={selectedEventId}
      enabled={!!selectedEventId && isDeadlinePassed}
    >
      {children}
    </LiveViewConnection>
  );
}

export function useSupervisorLiveView(): LiveViewState {
  const context = useContext(SupervisorLiveViewContext);
  if (!context) {
    throw new Error(
      "useSupervisorLiveView must be used within a SupervisorLiveViewProvider",
    );
  }
  return context;
}
