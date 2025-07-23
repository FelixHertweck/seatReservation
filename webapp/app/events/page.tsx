"use client";

import { useState } from "react";
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
  const [filteredEvents, setFilteredEvents] = useState(events);

  if (isLoading) {
    return <Loading />;
  }

  const handleSearch = (query: string) => {
    const filtered = events.filter(
      (event) =>
        event.name?.toLowerCase().includes(query.toLowerCase()) ||
        event.description?.toLowerCase().includes(query.toLowerCase()),
    );
    setFilteredEvents(filtered);
  };

  const handleFilter = (filters: Record<string, unknown>) => {
    let filtered = events;

    if (filters.hasAvailableSeats) {
      filtered = filtered.filter(
        (event) => (event.reservationsAllowed ?? 0) > 0,
      );
    }

    setFilteredEvents(filtered);
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
        onFilter={handleFilter}
        filterOptions={[
          {
            key: "hasAvailableSeats",
            label: "Available Seats",
            type: "boolean",
          },
        ]}
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
