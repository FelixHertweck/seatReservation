"use client";

import { useEffect, useState } from "react";
import { ReservationCard } from "@/components/reservations/reservation-card";
import { SeatMapModal } from "@/components/reservations/seat-map-modal";
import { SearchAndFilter } from "@/components/common/search-and-filter";
import { useReservations } from "@/hooks/use-reservations";
import type { EventResponseDto, ReservationResponseDto } from "@/api";
import Loading from "./loading";
import { useEvents } from "@/hooks/use-events";

export default function ReservationsPage() {
  const { getEventById } = useEvents();
  const { reservations, isLoading, deleteReservation } = useReservations();
  const [selectedReservation, setSelectedReservation] =
    useState<ReservationResponseDto | null>(null);
  const [filteredReservations, setFilteredReservations] =
    useState(reservations);

  const [eventSeats, setEventSeats] = useState<EventResponseDto | null>(null);
  const [isSeatsLoading, setIsSeatsLoading] = useState(false);

  useEffect(() => {
    const fetchSeats = async () => {
      if (selectedReservation?.eventId) {
        setIsSeatsLoading(true);
        try {
          const eventData = await getEventById(selectedReservation.eventId);
          setEventSeats(eventData);
        } catch (error) {
          console.error("Fehler beim Laden der Sitzplätze:", error);
          setEventSeats(null);
        } finally {
          setIsSeatsLoading(false);
        }
      } else {
        setEventSeats(null);
      }
    };

    fetchSeats();
  }, [selectedReservation?.eventId, getEventById]);

  if (isLoading) {
    return <Loading />;
  }

  const handleSearch = (query: string) => {
    const filtered = reservations.filter((reservation) =>
      reservation.seat?.seatNumber?.toLowerCase().includes(query.toLowerCase()),
    );
    setFilteredReservations(filtered);
  };

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const handleFilter = (filters: Record<string, unknown>) => {
    setFilteredReservations(reservations);
  };

  return (
    <div className="container mx-auto p-6">
      <div className="mb-6">
        <h1 className="text-3xl font-bold mb-2">My Reservations</h1>
        <p className="text-muted-foreground">
          View and manage your event reservations
        </p>
      </div>

      <SearchAndFilter
        onSearch={handleSearch}
        onFilter={handleFilter}
        filterOptions={[]}
      />

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {filteredReservations.map((reservation) => (
          <ReservationCard
            key={reservation.id?.toString()}
            reservation={reservation}
            onViewSeats={() => setSelectedReservation(reservation)}
            onDelete={() => reservation.id && deleteReservation(reservation.id)}
          />
        ))}
      </div>

      {selectedReservation && (
        <SeatMapModal
          seats={eventSeats?.location?.seats || []}
          reservation={selectedReservation}
          onClose={() => setSelectedReservation(null)}
          isLoading={isSeatsLoading}
        />
      )}
    </div>
  );
}
