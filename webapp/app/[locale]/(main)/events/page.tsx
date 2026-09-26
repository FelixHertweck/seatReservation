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
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionTrigger,
} from "@/components/ui/accordion";
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

  const {
    data: selectedEventDetail,
    isLoading: isEventDetailLoading,
    isFetching: isEventFetching,
  } = useQuery({
    ...getApiUserEventsByIdOptions({
      path: { id: selectedEventId ?? "" },
    }),
    enabled: !!selectedEventId,
    refetchInterval: 20000,
  });

  const selectedEvent = useMemo(() => {
    if (!selectedEventId) return null;
    const base = events.find((event) => event.id === selectedEventId);
    if (!base) return null;
    return selectedEventDetail ?? base;
  }, [events, selectedEventId, selectedEventDetail]);

  const {
    data: selectedLocationDetail,
    isLoading: isLocationLoading,
    isFetching: isLocationFetching,
  } = useQuery({
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

  const { filteredEvents, hiddenPastEvents } = useMemo(() => {
    if (!events) return { filteredEvents: [], hiddenPastEvents: [] };

    const locationId = filters.locationId as string | undefined;
    const onlyUpcoming = filters.onlyUpcoming === true;
    const visible: typeof events = [];
    const hiddenPast: typeof events = [];
    for (const event of events) {
      const matchesQuery =
        event.name?.toLowerCase().includes(eventSearchQuery.toLowerCase()) ||
        event.description
          ?.toLowerCase()
          .includes(eventSearchQuery.toLowerCase());
      const matchesLocation = !locationId || event.locationId === locationId;
      if (!matchesQuery || !matchesLocation) continue;
      const eventEnd = event.endTime ?? event.startTime;
      const isPast = !!eventEnd && new Date(eventEnd).getTime() < now;
      if (onlyUpcoming && isPast) hiddenPast.push(event);
      else visible.push(event);
    }

    const sorted = [...visible].sort((a, b) => {
      const aHasSeats = (a.reservationsAllowed ?? 0) > 0;
      const bHasSeats = (b.reservationsAllowed ?? 0) > 0;

      if (aHasSeats && !bHasSeats) return -1;
      if (!aHasSeats && bHasSeats) return 1;
      return 0;
    });
    return { filteredEvents: sorted, hiddenPastEvents: hiddenPast };
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
    <div className="container mx-auto flex flex-1 flex-col px-2 pt-3 pb-0 md:px-6 md:pt-6 md:pb-0">
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
            initialFilters={filters}
            initialQuery={eventSearchQuery}
            className="w-full"
          />
        }
      />

      {eventsLoading || reservationsLoading ? (
        <LoadingAnimation />
      ) : filteredEvents.length === 0 && hiddenPastEvents.length === 0 ? (
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

      {!eventsLoading &&
        !reservationsLoading &&
        hiddenPastEvents.length > 0 && (
          <Accordion
            type="single"
            collapsible
            className="-mb-2 mt-auto pt-8 md:-mb-4"
          >
            <AccordionItem value="hidden-past" className="border-b-0 border-t">
              <AccordionTrigger className="py-2 text-xs text-muted-foreground">
                {t("eventsPage.hiddenPastEvents", {
                  count: hiddenPastEvents.length,
                })}
              </AccordionTrigger>
              <AccordionContent>
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-2 md:gap-4">
                  {hiddenPastEvents.map((event) => (
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
              </AccordionContent>
            </AccordionItem>
          </Accordion>
        )}

      {selectedEvent && (
        <EventReservationModal
          event={selectedEvent}
          location={selectedLocation ?? getLocation(selectedEvent.locationId)}
          userReservations={getReservationsForEvent(selectedEvent.id)}
          isLocationLoading={isLocationLoading}
          isEventLoading={isEventDetailLoading}
          isFetching={isEventFetching || isLocationFetching}
          onClose={closeModal}
          onReserve={async (eventId, seatIds) => {
            const res = await createReservation(eventId, seatIds);
            router.push(`/events/reservations?eventId=${eventId}&showQr=true`);
            return res;
          }}
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
