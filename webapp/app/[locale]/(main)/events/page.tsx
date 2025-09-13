"use client";

import { useState, useMemo, useEffect } from "react";
import { EventCard } from "@/components/events/event-card";
import { EventReservationModal } from "@/components/events/event-reservation-modal";
import { ReservationCard } from "@/components/reservations/reservation-card";
import { SeatMapModal } from "@/components/reservations/seat-map-modal";
import { SearchAndFilter } from "@/components/common/search-and-filter";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Badge } from "@/components/ui/badge";
import { useEvents } from "@/hooks/use-events";
import { useReservations } from "@/hooks/use-reservations";
import { useAuth } from "@/hooks/use-auth";
import type { EventResponseDto, ReservationResponseDto } from "@/api";
import Loading from "./loading";
import { useT } from "@/lib/i18n/hooks";

export default function EventsPage() {
  const t = useT();

  const { events, isLoading: eventsLoading, createReservation } = useEvents();
  const {
    reservations,
    isLoading: reservationsLoading,
    deleteReservation,
  } = useReservations();
  const { isLoggedIn } = useAuth();
  const [selectedEvent, setSelectedEvent] = useState<EventResponseDto | null>(
    null,
  );
  const [selectedReservation, setSelectedReservation] =
    useState<ReservationResponseDto | null>(null);
  const [activeTab, setActiveTab] = useState("available");
  const [eventSearchQuery, setEventSearchQuery] = useState<string>("");
  const [reservationSearchQuery, setReservationSearchQuery] =
    useState<string>("");

  useEffect(() => {
    if (eventsLoading || reservationsLoading || !isLoggedIn) return;

    const urlParams = new URLSearchParams(window.location.search);
    const eventIdFromUrl = urlParams.get("id");

    if (eventIdFromUrl) {
      const eventId = BigInt(eventIdFromUrl);
      const event = events?.find((e) => e.id === eventId);

      if (event) {
        setActiveTab("reservations");
        setReservationSearchQuery(event.name || "");
      }

      // Remove 'id' from URL
      urlParams.delete("id");
      const newUrl = `${window.location.pathname}${urlParams.toString() ? `?${urlParams.toString()}` : ""}`;
      window.history.replaceState({}, "", newUrl);
    }
  }, [
    events,
    eventsLoading,
    reservationsLoading,
    isLoggedIn,
    setActiveTab,
    setReservationSearchQuery,
  ]);

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

  const groupedReservations = useMemo(() => {
    if (!reservations || !events) return [];

    const filtered = reservations.filter((reservation) => {
      const event = events.find((e) => e.id === reservation.eventId);
      const eventName = event?.name?.toLowerCase() || "";

      return eventName.includes(reservationSearchQuery.toLowerCase());
    });

    const grouped = filtered.reduce(
      (acc, reservation) => {
        const eventId = reservation.eventId?.toString();
        if (!eventId) return acc;

        if (!acc[eventId]) {
          acc[eventId] = [];
        }
        acc[eventId].push(reservation);
        return acc;
      },
      {} as Record<string, ReservationResponseDto[]>,
    );

    return Object.values(grouped);
  }, [reservations, reservationSearchQuery, events]);

  const handleEventSearch = (query: string) => {
    setEventSearchQuery(query);
  };

  const handleReservationSearch = (query: string) => {
    setReservationSearchQuery(query);
  };

  const handleViewReservationSeats = (reservation: ReservationResponseDto) => {
    setSelectedReservation(reservation);
  };

  const handleDeleteReservation = async (reservationId: bigint) => {
    await deleteReservation(reservationId);
  };

  const getEventReservations = (eventId: bigint | undefined) => {
    if (!eventId) return [];
    return reservations.filter(
      (reservation) => reservation.eventId === eventId,
    );
  };

  if (eventsLoading || reservationsLoading) {
    return <Loading />;
  }

  return (
    <div className="container mx-auto p-6">
      <div className="mb-6">
        <h1 className="text-3xl font-bold mb-2">{t("eventsPage.title")}</h1>
        <p className="text-muted-foreground">{t("eventsPage.description")}</p>
      </div>

      <Tabs value={activeTab} onValueChange={setActiveTab} className="w-full">
        <TabsList className="grid w-full grid-cols-2">
          <TabsTrigger value="available">
            {t("eventsPage.availableEventsTab")}
          </TabsTrigger>

          <TabsTrigger value="reservations" className="flex items-center gap-2">
            {t("eventsPage.myReservationsTab")}
            {reservations.length > 0 && (
              <Badge variant="secondary" className="ml-1">
                {reservations.length}
              </Badge>
            )}
          </TabsTrigger>
        </TabsList>

        <TabsContent value="reservations" className="space-y-6">
          <SearchAndFilter
            onSearch={handleReservationSearch}
            onFilter={() => {}}
            filterOptions={[]}
            initialQuery={reservationSearchQuery}
          />

          {groupedReservations.length === 0 ? (
            <div className="text-center py-12">
              <p className="text-muted-foreground text-lg">
                {t("eventsPage.noReservationsYet")}
              </p>
              <p className="text-muted-foreground">
                {t("eventsPage.switchToAvailableEvents")}
              </p>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {groupedReservations.map((eventReservations) => {
                const firstReservation = eventReservations[0];
                const event = events?.find(
                  (e) => e.id === firstReservation.eventId,
                );
                return (
                  <ReservationCard
                    key={firstReservation.eventId?.toString()}
                    reservations={eventReservations}
                    eventName={event?.name}
                    locationName={event?.location?.name}
                    bookingDeadline={event?.bookingDeadline}
                    onViewSeats={handleViewReservationSeats}
                    onDelete={handleDeleteReservation}
                  />
                );
              })}
            </div>
          )}
        </TabsContent>

        <TabsContent value="available" className="space-y-6">
          <SearchAndFilter
            onSearch={handleEventSearch}
            onFilter={() => {}}
            filterOptions={[]}
            initialQuery={eventSearchQuery}
          />

          {filteredEvents.length === 0 ? (
            <div className="text-center py-12">
              <p className="text-muted-foreground text-lg">
                {t("eventsPage.noEventsAvailable")}
              </p>
              <p className="text-muted-foreground">
                {t("eventsPage.tryAgainOrCheckSearch")}
              </p>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {filteredEvents.map((event) => (
                <EventCard
                  key={event.id?.toString()}
                  event={event}
                  onReserve={() => setSelectedEvent(event)}
                />
              ))}
            </div>
          )}
        </TabsContent>
      </Tabs>

      {selectedReservation && (
        <SeatMapModal
          seats={
            events.find((event) => event.id === selectedReservation?.eventId)
              ?.location?.seats || []
          }
          reservation={selectedReservation}
          eventReservations={getEventReservations(selectedReservation.eventId)}
          onClose={() => setSelectedReservation(null)}
          isLoading={false}
        />
      )}

      {selectedEvent && (
        <EventReservationModal
          event={selectedEvent}
          userReservations={reservations}
          initialSeatId={selectedReservation?.seat?.id || BigInt(0)}
          onClose={() => setSelectedEvent(null)}
          onReserve={createReservation}
        />
      )}
    </div>
  );
}
