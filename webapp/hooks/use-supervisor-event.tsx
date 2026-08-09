"use client";

import {
  createContext,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from "react";
import { usePathname, useRouter, useSearchParams } from "next/navigation";

interface SupervisorEventContextType {
  selectedEventId: string | null;
  setSelectedEventId: (eventId: string | null) => void;
}

const SupervisorEventContext = createContext<
  SupervisorEventContextType | undefined
>(undefined);

// Scoped to the /supervisor layout so the selected event is shared between
// checkin, liveview and box-office, and forgotten once the supervisor is
// no longer under /supervisor.
export const SupervisorEventProvider = ({
  children,
}: {
  children: ReactNode;
}) => {
  const [selectedEventId, setSelectedEventId] = useState<string | null>(null);

  return (
    <SupervisorEventContext.Provider
      value={{ selectedEventId, setSelectedEventId }}
    >
      {children}
    </SupervisorEventContext.Provider>
  );
};

// Keeps the shared selection and the current page's "eventId" URL param in
// sync in both directions: a deep link with ?eventId=... adopts it into the
// shared selection, and landing here without one falls back to (and restores
// into the URL) whatever event is already selected elsewhere under /supervisor.
export function useSupervisorEvent() {
  const context = useContext(SupervisorEventContext);
  if (!context) {
    throw new Error(
      "useSupervisorEvent must be used within a SupervisorEventProvider",
    );
  }
  const { selectedEventId, setSelectedEventId } = context;

  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();

  useEffect(() => {
    const urlEventId = searchParams.get("eventId");
    if (urlEventId && urlEventId !== selectedEventId) {
      setSelectedEventId(urlEventId);
    } else if (!urlEventId && selectedEventId) {
      const params = new URLSearchParams(searchParams.toString());
      params.set("eventId", selectedEventId);
      router.replace(`${pathname}?${params.toString()}`, { scroll: false });
    }
  }, [searchParams, selectedEventId, setSelectedEventId, pathname, router]);

  const selectEvent = (eventId: string) => {
    setSelectedEventId(eventId);
    const params = new URLSearchParams(searchParams.toString());
    params.set("eventId", eventId);
    router.replace(`${pathname}?${params.toString()}`, { scroll: false });
  };

  return { selectedEventId, selectEvent } as const;
}
