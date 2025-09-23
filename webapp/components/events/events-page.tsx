"use client";

import { useState, useMemo, useEffect } from "react";
import { useSearchParams } from "next/navigation";
import { EventCard } from "@/components/events/event-card";
import { EventCardSkeleton } from "@/components/events/event-card-skeleton";
import { EventReservationModal } from "@/components/events/event-reservation-modal";
import { SearchAndFilter } from "@/components/common/search-and-filter";
import { useEvents } from "@/hooks/use-events";
import type { UserEventResponseDto } from "@/api";

export default function EventsSubPage() {
  const searchParams = useSearchParams();

  const {
    events,
    locations,
    isLoading: eventsLoading,
    createReservation,
  } = useEvents();
  const [selectedEvent, setSelectedEvent] =
    useState<UserEventResponseDto | null>(null);
  const [eventSearchQuery, setEventSearchQuery] = useState<string>("");

  useEffect(() => {
    if (eventsLoading) return;

    const eventIdFromUrl = searchParams.get("id");

    if (eventIdFromUrl) {
      const eventId = BigInt(eventIdFromUrl);
      const event = events?.find((e) => e.id === eventId);

      if (event) {
        setEventSearchQuery(event.name || "");
      }

      const params = new URLSearchParams(searchParams.toString());
      params.delete("id");
      const newUrl = `${window.location.pathname}${params.toString() ? `?${params.toString()}` : ""}`;
      window.history.replaceState({}, "", newUrl);
    }
  }, [events, eventsLoading, searchParams]);

  const filteredEvents = useMemo(() => {
    if (!events) return [];

    const filtered = events.filter(
      (event) =>
        event.name?.toLowerCase().includes(eventSearchQuery.toLowerCase()) ||
        event.description
          ?.toLowerCase()
          .includes(eventSearchQuery.toLowerCase()),
    );

    return [...filtered].sort((a, b) => {
      const aHasSeats = (a.reservationsAllowed ?? 0) > 0;
      const bHasSeats = (b.reservationsAllowed ?? 0) > 0;

      if (aHasSeats && !bHasSeats) return -1;
      if (!aHasSeats && bHasSeats) return 1;
      return 0;
    });
  }, [events, eventSearchQuery]);

  const handleEventSearch = (query: string) => {
    setEventSearchQuery(query);
  };

  const getLocation = (locationId: bigint | undefined) => {
    if (!locationId) return null;
    return locations?.find((l) => l.id === locationId) || null;
  };

  return (
    <div>
      <SearchAndFilter
        onSearch={handleEventSearch}
        onFilter={() => {}}
        filterOptions={[]}
        initialQuery={eventSearchQuery}
      />

      {eventsLoading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-2 md:gap-4">
          {Array.from({ length: 3 }).map((_, index) => (
            <EventCardSkeleton key={index} />
          ))}
        </div>
      ) : filteredEvents.length === 0 ? (
        <div className="text-center py-12">
          <p className="text-muted-foreground text-lg">No events available</p>
          <p className="text-muted-foreground">
            Try again or check your search
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-2 md:gap-4">
          {filteredEvents.map((event) => (
            <EventCard
              key={event.id?.toString()}
              event={event}
              location={getLocation(event.locationId)}
              onReserve={() => setSelectedEvent(event)}
            />
          ))}
        </div>
      )}

      {selectedEvent && (
        <EventReservationModal
          event={selectedEvent}
          location={getLocation(selectedEvent.locationId)}
          userReservations={[]}
          onClose={() => setSelectedEvent(null)}
          onReserve={createReservation}
        />
      )}
    </div>
  );
}
