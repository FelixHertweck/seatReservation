"use client";

import { useMemo, useState } from "react";
import { useT } from "@/lib/i18n/hooks";
import { useEvents } from "@/hooks/use-events";
import { UserEventResponseDto } from "@/api";
import { SearchAndFilter } from "@/components/common/search-and-filter";
import { EventCardSkeleton } from "@/components/events/event-card-skeleton";
import { EventReservationModal } from "@/components/events/event-reservation-modal";
import { EventCard } from "@/components/events/event-card";
import { useReservations } from "@/hooks/use-reservations";

export default function EventsPage() {
  const t = useT();

  const {
    events,
    locations,
    isLoading: eventsLoading,
    createReservation,
  } = useEvents();
  const { isLoading: reservationsLoading, reservations } = useReservations();
  const [selectedEvent, setSelectedEvent] =
    useState<UserEventResponseDto | null>(null);
  const [eventSearchQuery, setEventSearchQuery] = useState<string>("");

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

  const getReservationsForEvent = (eventId: bigint | undefined) => {
    if (!eventId) return [];
    return reservations.filter((r) => r.eventId === eventId);
  };

  return (
    <div className="container mx-auto px-2 py-3 md:p-6">
      <div className="mb-3 md:mb-6">
        <h1 className="text-2xl md:text-3xl font-bold mb-1 md:mb-2">
          {t("eventsPage.title")}
        </h1>
        <p className="text-muted-foreground text-sm md:text-base">
          {t("eventsPage.description")}
        </p>
      </div>

      <SearchAndFilter
        onSearch={handleEventSearch}
        onFilter={() => {}}
        filterOptions={[]}
        initialQuery={eventSearchQuery}
      />

      {eventsLoading || reservationsLoading ? (
        <LoadingAnimation />
      ) : filteredEvents.length === 0 ? (
        <NoEventsAvailable eventsLength={events.length} />
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
          userReservations={getReservationsForEvent(selectedEvent.id)}
          onClose={() => setSelectedEvent(null)}
          onReserve={createReservation}
        />
      )}
    </div>
  );
}

const LoadingAnimation = () => (
  <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-2 md:gap-4">
    {Array.from({ length: 3 }).map((_, index) => (
      <EventCardSkeleton key={index} />
    ))}
  </div>
);

const NoEventsAvailable = ({ eventsLength }: { eventsLength: number }) => {
  const t = useT();
  if (eventsLength === 0) {
    return (
      <div className="text-center py-12">
        <p className="text-muted-foreground text-lg">
          {t("eventsPage.noEventsAvailable")}
        </p>
        <p className="text-muted-foreground">{t("eventsPage.tryAgain")}</p>
      </div>
    );
  }
  return (
    <div className="text-center py-12">
      <p className="text-muted-foreground text-lg">
        {t("eventsPage.noEventsMatchSearch")}
      </p>
      <p className="text-muted-foreground">{t("eventsPage.checkSearch")}</p>
    </div>
  );
};
