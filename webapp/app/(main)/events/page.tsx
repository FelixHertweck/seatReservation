"use client";

import { useState, useEffect } from "react";
import { EventCard } from "@/components/events/event-card";
import { EventReservationModal } from "@/components/events/event-reservation-modal";
import { SearchAndFilter } from "@/components/common/search-and-filter";
import { useEvents } from "@/hooks/use-events";
import type { EventResponseDto } from "@/api";
import Loading from "./loading";

export default function EventsPage() {
  const { events, isLoading, createReservation } = useEvents();
  const [selectedEvent, setSelectedEvent] = useState<EventResponseDto | null>(
    null,
  );
  const [filteredEvents, setFilteredEvents] = useState<EventResponseDto[]>([]);

  useEffect(() => {
    if (events) {
      const sortedEvents = [...events].sort((a, b) => {
        const aHasSeats = (a.reservationsAllowed ?? 0) > 0;
        const bHasSeats = (b.reservationsAllowed ?? 0) > 0;

        if (aHasSeats && !bHasSeats) return -1;
        if (!aHasSeats && bHasSeats) return 1;
        return 0;
      });
      setFilteredEvents(sortedEvents);
    }
  }, [events]);

  if (isLoading) {
    return <Loading />;
  }

  const handleSearch = (query: string) => {
    if (!events) return;

    const filtered = events.filter(
      (event) =>
        event.name?.toLowerCase().includes(query.toLowerCase()) ||
        event.description?.toLowerCase().includes(query.toLowerCase()),
    );

    const sortedFiltered = filtered.sort((a, b) => {
      const aHasSeats = (a.reservationsAllowed ?? 0) > 0;
      const bHasSeats = (b.reservationsAllowed ?? 0) > 0;

      if (aHasSeats && !bHasSeats) return -1;
      if (!aHasSeats && bHasSeats) return 1;
      return 0;
    });

    setFilteredEvents(sortedFiltered);
  };

  return (
    <div className="container mx-auto p-6">
      <div className="mb-6">
        <h1 className="text-3xl font-bold mb-2">Events</h1>
        <p className="text-muted-foreground">
          Browse and reserve seats for upcoming events
        </p>
      </div>

      <SearchAndFilter
        onSearch={handleSearch}
        onFilter={() => {}}
        filterOptions={[]}
      />

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {filteredEvents.map((event) => (
          <EventCard
            key={event.id?.toString()}
            event={event}
            onReserve={() => setSelectedEvent(event)}
          />
        ))}
      </div>

      {selectedEvent && (
        <EventReservationModal
          event={selectedEvent}
          onClose={() => setSelectedEvent(null)}
          onReserve={createReservation}
        />
      )}
    </div>
  );
}
