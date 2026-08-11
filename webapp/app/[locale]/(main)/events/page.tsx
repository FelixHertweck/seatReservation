"use client";

import { useEffect, useMemo, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { useT } from "@/lib/i18n/hooks";
import { useEvents } from "@/hooks/use-events";
import { SearchAndFilter } from "@/components/common/search-and-filter";
import { EventCardSkeleton } from "@/components/events/event-card-skeleton";
import { EventReservationModal } from "@/components/events/event-reservation-modal";
import { EventCard } from "@/components/events/event-card";
import { useReservations } from "@/hooks/use-reservations";
import { PageHeader } from "@/components/page-header";
import {
  getApiUserEventsByIdOptions,
  getApiUserLocationsByIdOptions,
} from "@/api/@tanstack/react-query.gen";

export default function EventsPage() {
  const t = useT();
  const router = useRouter();
  const searchParams = useSearchParams();

  const {
    events,
    locations,
    isLoading: eventsLoading,
    createReservation,
  } = useEvents();
  const { isLoading: reservationsLoading, reservations } = useReservations();
  const [selectedEventId, setSelectedEventId] = useState<string | null>(null);
  const [eventSearchQuery, setEventSearchQuery] = useState<string>("");
  const [filters, setFilters] = useState<Record<string, unknown>>({
    onlyUpcoming: true,
  });
  const [now] = useState(() => Date.now());

  // Open modal if eventId is in URL query parameters on initial load
  useEffect(() => {
    if (eventsLoading) return;
    const eventId = searchParams.get("eventId");
    if (!eventId) return;

    if (events.some((event) => event.id === eventId)) {
      setSelectedEventId(eventId);
    } else {
      router.replace("/events");
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [eventsLoading]);

  const { data: selectedEventDetail } = useQuery({
    ...getApiUserEventsByIdOptions({
      path: { id: selectedEventId ?? "" },
    }),
    enabled: !!selectedEventId,
    refetchInterval: 5000,
  });

  const selectedEvent = useMemo(() => {
    if (!selectedEventId) return null;
    const base = events.find((event) => event.id === selectedEventId);
    if (!base) return null;
    return selectedEventDetail ?? base;
  }, [events, selectedEventId, selectedEventDetail]);

  const { data: selectedLocationDetail } = useQuery({
    ...getApiUserLocationsByIdOptions({
      path: { id: selectedEvent?.locationId ?? "" },
    }),
    enabled: !!selectedEvent?.locationId,
  });

  const selectedLocation = useMemo(() => {
    if (!selectedEvent?.locationId) return null;
    const base = locations.find((l) => l.id === selectedEvent.locationId);
    return selectedLocationDetail ?? base ?? null;
  }, [locations, selectedEvent?.locationId, selectedLocationDetail]);

  const locationOptions = useMemo(
    () =>
      locations
        .filter((l) => l.id && l.name)
        .map((l) => ({ value: l.id!, label: l.name! })),
    [locations],
  );

  const reservationCountByEvent = useMemo(() => {
    const map = new Map<string, number>();
    for (const reservation of reservations) {
      if (!reservation.eventId) continue;
      map.set(reservation.eventId, (map.get(reservation.eventId) ?? 0) + 1);
    }
    return map;
  }, [reservations]);

  const filteredEvents = useMemo(() => {
    if (!events) return [];

    const locationId = filters.locationId as string | undefined;
    const onlyUpcoming = filters.onlyUpcoming === true;
    const filtered = events.filter((event) => {
      const matchesQuery =
        event.name?.toLowerCase().includes(eventSearchQuery.toLowerCase()) ||
        event.description
          ?.toLowerCase()
          .includes(eventSearchQuery.toLowerCase());
      const matchesLocation = !locationId || event.locationId === locationId;
      const eventEnd = event.endTime ?? event.startTime;
      const isPast = !!eventEnd && new Date(eventEnd).getTime() < now;
      const matchesPast = !onlyUpcoming || !isPast;
      return matchesQuery && matchesLocation && matchesPast;
    });

    return [...filtered].sort((a, b) => {
      const aHasSeats = (a.reservationsAllowed ?? 0) > 0;
      const bHasSeats = (b.reservationsAllowed ?? 0) > 0;

      if (aHasSeats && !bHasSeats) return -1;
      if (!aHasSeats && bHasSeats) return 1;
      return 0;
    });
  }, [events, eventSearchQuery, filters, now]);

  const handleEventSearch = (query: string) => {
    setEventSearchQuery(query);
  };

  const getLocation = (locationId: string | undefined) => {
    if (!locationId) return null;
    return locations?.find((l) => l.id === locationId) || null;
  };

  const getReservationsForEvent = (eventId: string | undefined) => {
    if (!eventId) return [];
    return reservations.filter((r) => r.eventId === eventId);
  };

  const closeModal = () => {
    setSelectedEventId(null);
    if (searchParams.get("eventId")) {
      router.replace("/events");
    }
  };

  return (
    <div className="container mx-auto px-2 py-3 md:p-6">
      <PageHeader
        title={t("eventsPage.title")}
        description={t("eventsPage.description")}
        search={
          <SearchAndFilter
            onSearch={handleEventSearch}
            onFilter={setFilters}
            filterOptions={[
              ...(locationOptions.length > 0
                ? [
                    {
                      key: "locationId",
                      label: t("eventsPage.locationFilterLabel"),
                      type: "select" as const,
                      options: locationOptions,
                    },
                  ]
                : []),
              {
                key: "onlyUpcoming",
                label: t("eventsPage.onlyUpcomingFilterLabel"),
                type: "switch" as const,
              },
            ]}
            initialFilters={{ onlyUpcoming: true }}
            initialQuery={eventSearchQuery}
            className="w-full"
          />
        }
      />

      {eventsLoading || reservationsLoading ? (
        <LoadingAnimation />
      ) : filteredEvents.length === 0 ? (
        <NoEventsAvailable eventsLength={events.length} />
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-2 md:gap-4">
          <p className="col-span-full text-sm text-muted-foreground">
            {t("eventsPage.resultsCount", {
              shown: filteredEvents.length,
              total: events.length,
            })}
          </p>
          {filteredEvents.map((event) => (
            <EventCard
              key={event.id?.toString()}
              event={event}
              location={getLocation(event.locationId)}
              reservationCount={
                reservationCountByEvent.get(event.id ?? "") ?? 0
              }
              onReserve={() => setSelectedEventId(event.id ?? null)}
            />
          ))}
        </div>
      )}

      {selectedEvent && (
        <EventReservationModal
          event={selectedEvent}
          location={selectedLocation ?? getLocation(selectedEvent.locationId)}
          userReservations={getReservationsForEvent(selectedEvent.id)}
          onClose={closeModal}
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
