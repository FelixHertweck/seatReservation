"use client";

import { useState, useEffect, useMemo } from "react";
import { EventCard } from "@/components/events/event-card";
import { EventReservationModal } from "@/components/events/event-reservation-modal";
import { ReservationCard } from "@/components/reservations/reservation-card";
import { SeatMapModal } from "@/components/reservations/seat-map-modal";
import { SearchAndFilter } from "@/components/common/search-and-filter";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Badge } from "@/components/ui/badge";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useEvents } from "@/hooks/use-events";
import { useReservations } from "@/hooks/use-reservations";
import type { EventResponseDto, ReservationResponseDto } from "@/api";
import Loading from "./loading";

export default function EventsPage() {
  const { events, isLoading: eventsLoading, createReservation } = useEvents();
  const {
    reservations,
    isLoading: reservationsLoading,
    deleteReservation,
  } = useReservations();
  const [selectedEvent, setSelectedEvent] = useState<EventResponseDto | null>(
    null,
  );
  const [selectedReservation, setSelectedReservation] =
    useState<ReservationResponseDto | null>(null);
  const [activeTab, setActiveTab] = useState("available");
  const [selectedEventFilter, setSelectedEventFilter] = useState<string>("all");
  const [eventSearchQuery, setEventSearchQuery] = useState<string>("");
  const [reservationSearchQuery, setReservationSearchQuery] =
    useState<string>("");

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

  const filteredReservations = useMemo(() => {
    if (!reservations) return [];

    let filtered = [...reservations];

    // Filter by selected event if not "all"
    if (selectedEventFilter !== "all") {
      const eventId = BigInt(selectedEventFilter);
      filtered = filtered.filter(
        (reservation) => reservation.eventId === eventId,
      );
    }

    // Then apply search filter
    return filtered.filter((reservation) =>
      reservation.seat?.seatNumber
        ?.toLowerCase()
        .includes(reservationSearchQuery.toLowerCase()),
    );
  }, [reservations, selectedEventFilter, reservationSearchQuery]);

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

  const eventsWithReservations =
    events?.filter((event) =>
      reservations.some((reservation) => reservation.eventId === event.id),
    ) || [];

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
        <h1 className="text-3xl font-bold mb-2">Events & Reservations</h1>
        <p className="text-muted-foreground">
          Browse events and manage your reservations
        </p>
      </div>

      <Tabs value={activeTab} onValueChange={setActiveTab} className="w-full">
        <TabsList className="grid w-full grid-cols-2">
          <TabsTrigger value="available">Available Events</TabsTrigger>

          <TabsTrigger value="reservations" className="flex items-center gap-2">
            My Reservations
            {reservations.length > 0 && (
              <Badge variant="secondary" className="ml-1">
                {reservations.length}
              </Badge>
            )}
          </TabsTrigger>
        </TabsList>

        <TabsContent value="reservations" className="space-y-6">
          <div className="flex flex-col sm:flex-row gap-4">
            <div className="flex-1">
              <SearchAndFilter
                onSearch={handleReservationSearch}
                onFilter={() => {}}
                filterOptions={[]}
              />
            </div>
            <div className="w-full sm:w-64">
              <Select
                value={selectedEventFilter}
                onValueChange={setSelectedEventFilter}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Filter by Event" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">All Events</SelectItem>
                  {eventsWithReservations.map((event) => (
                    <SelectItem
                      key={event.id?.toString()}
                      value={event.id?.toString() || ""}
                    >
                      {event.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>

          {filteredReservations.length === 0 ? (
            <div className="text-center py-12">
              <p className="text-muted-foreground text-lg">
                {selectedEventFilter === "all"
                  ? "You don't have any reservations yet."
                  : "No reservations found for the selected event."}
              </p>
              <p className="text-muted-foreground">
                Switch to "Available Events" to make your first reservation!
              </p>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {filteredReservations.map((reservation) => (
                <ReservationCard
                  key={reservation.id?.toString()}
                  reservation={reservation}
                  onViewSeats={() => handleViewReservationSeats(reservation)}
                  onDelete={() =>
                    reservation.id && handleDeleteReservation(reservation.id)
                  }
                />
              ))}
            </div>
          )}
        </TabsContent>

        <TabsContent value="available" className="space-y-6">
          <SearchAndFilter
            onSearch={handleEventSearch}
            onFilter={() => {}}
            filterOptions={[]}
          />

          {filteredEvents.length === 0 ? (
            <div className="text-center py-12">
              <p className="text-muted-foreground text-lg">
                Derzeit sind keine Events verfügbar.
              </p>
              <p className="text-muted-foreground">
                Bitte versuchen Sie es später noch einmal oder überprüfen Sie
                Ihre Suchkriterien.
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
          onClose={() => setSelectedEvent(null)}
          onReserve={createReservation}
        />
      )}
    </div>
  );
}
