"use client";

import { useState, useMemo } from "react";
import { ReservationCard } from "@/components/reservations/reservation-card";
import { ReservationCardSkeleton } from "@/components/reservations/reservation-card-skeleton";
import { SeatMapModal } from "@/components/reservations/seat-map-modal";
import { SearchAndFilter } from "@/components/common/search-and-filter";
import { useReservations } from "@/hooks/use-reservations";
import { useEvents } from "@/hooks/use-events";
import type { UserReservationResponseDto } from "@/api";
import { useT } from "@/lib/i18n/hooks";

export default function ReservationsSubPage() {
  const t = useT();

  const {
    reservations,
    isLoading: reservationsLoading,
    deleteReservation,
  } = useReservations();
  const { events, locations } = useEvents();
  const [reservationSearchQuery, setReservationSearchQuery] =
    useState<string>("");
  const [selectedReservation, setSelectedReservation] =
    useState<UserReservationResponseDto | null>(null);

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
      {} as Record<string, UserReservationResponseDto[]>,
    );

    return Object.values(grouped);
  }, [reservations, reservationSearchQuery, events]);

  const handleReservationSearch = (query: string) => {
    setReservationSearchQuery(query);
  };

  const handleViewReservationSeats = (
    reservation: UserReservationResponseDto,
  ) => {
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

  const getEvent = (eventId: bigint | undefined) => {
    if (!eventId) return null;
    return events?.find((e) => e.id === eventId) || null;
  };

  const getLocation = (locationId: bigint | undefined) => {
    if (!locationId) return null;
    return locations?.find((l) => l.id === locationId) || null;
  };

  return (
    <div>
      <SearchAndFilter
        onSearch={handleReservationSearch}
        onFilter={() => {}}
        filterOptions={[]}
        initialQuery={reservationSearchQuery}
      />

      {reservationsLoading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-2 md:gap-4">
          {Array.from({ length: 3 }).map((_, index) => (
            <ReservationCardSkeleton key={index} />
          ))}
        </div>
      ) : groupedReservations.length === 0 ? (
        <div className="text-center py-12">
          <p className="text-muted-foreground text-lg">
            {t("eventsPage.noReservationsYet")}
          </p>
          <p className="text-muted-foreground">
            {t("eventsPage.switchToAvailableEvents")}
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-2 md:gap-4">
          {groupedReservations.map((eventReservations) => {
            const firstReservation = eventReservations[0];
            const event = events?.find(
              (e) => e.id === firstReservation.eventId,
            );
            const location = locations?.find((l) => l.id === event?.locationId);
            return (
              <ReservationCard
                key={firstReservation.eventId?.toString()}
                reservations={eventReservations}
                eventName={event?.name}
                locationName={location?.name}
                bookingDeadline={event?.bookingDeadline}
                onViewSeats={handleViewReservationSeats}
                onDelete={handleDeleteReservation}
              />
            );
          })}
        </div>
      )}

      {selectedReservation && (
        <SeatMapModal
          seats={getLocation(selectedReservation.eventId)?.seats || []}
          seatStatuses={
            getEvent(selectedReservation.eventId)?.seatStatuses || []
          }
          markers={getLocation(selectedReservation.eventId)?.markers || []}
          reservation={selectedReservation}
          eventReservations={getEventReservations(selectedReservation.eventId)}
          onClose={() => setSelectedReservation(null)}
          isLoading={false}
        />
      )}
    </div>
  );
}
