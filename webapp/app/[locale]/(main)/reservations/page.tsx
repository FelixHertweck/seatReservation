"use client";

import { useEffect, useMemo, useState } from "react";
import { useReservations } from "@/hooks/use-reservations";
import { useT } from "@/lib/i18n/hooks";
import { UserReservationResponseDto } from "@/api";
import { useEvents } from "@/hooks/use-events";
import { SearchAndFilter } from "@/components/common/search-and-filter";
import { ReservationCardSkeleton } from "@/components/reservations/reservation-card-skeleton";
import { SeatMapModal } from "@/components/reservations/reservation-modal";
import { ReservationCard } from "@/components/reservations/reservation-card";
import { useAuth } from "@/hooks/use-auth";
import { useRouter, useSearchParams } from "next/navigation";
import { toast } from "@/hooks/use-toast";

export default function EventsPage() {
  const t = useT();
  const router = useRouter();

  const {
    reservations,
    isLoading: reservationsLoading,
    deleteReservation,
  } = useReservations();
  const { isLoading: eventsLoading, events, locations } = useEvents();
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

  const searchParams = useSearchParams();
  const { isLoggedIn } = useAuth();

  useEffect(() => {
    if (eventsLoading || !isLoggedIn) return;

    const eventIdFromUrl = searchParams.get("id");

    if (eventIdFromUrl && reservationSearchQuery === "") {
      const eventId = BigInt(eventIdFromUrl);
      const event = events?.find((e) => e.id === eventId);

      if (event) {
        setReservationSearchQuery(event.name || "");
      } else {
        router.replace("/reservations");
        setTimeout(() => {
          toast({
            title: t("reservationsPage.noReservationsFoundTitle"),
            description: t("reservationsPage.noReservationsFoundDescription"),
            variant: "destructive",
          });
        }, 500);
      }
    }
  }, [
    eventsLoading,
    isLoggedIn,
    searchParams,
    events,
    router,
    t,
    reservationSearchQuery,
  ]);

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

  const LoadingAnimation = () => (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-2 md:gap-4">
      {Array.from({ length: 3 }).map((_, index) => (
        <ReservationCardSkeleton key={index} />
      ))}
    </div>
  );

  const NoReservationAvailable = () => {
    if (reservations.length === 0) {
      return (
        <div className="text-center py-12">
          <p className="text-muted-foreground text-lg">
            {t("reservationsPage.noReservationsYet")}
          </p>
          <p className="text-muted-foreground">
            {t("reservationsPage.switchToAvailableEvents")}
          </p>
        </div>
      );
    }
    return (
      <div className="text-center py-12">
        <p className="text-muted-foreground text-lg">
          {t("reservationsPage.noReservationsMatchSearch")}
        </p>
        <p className="text-muted-foreground">
          {t("reservationsPage.checkSearch")}
        </p>
      </div>
    );
  };

  return (
    <div className="container mx-auto px-2 py-3 md:p-6">
      <div className="mb-3 md:mb-6">
        <h1 className="text-2xl md:text-3xl font-bold mb-1 md:mb-2">
          {t("reservationsPage.title")}
        </h1>
        <p className="text-muted-foreground text-sm md:text-base">
          {t("reservationsPage.description")}
        </p>
      </div>
      <SearchAndFilter
        onSearch={handleReservationSearch}
        onFilter={() => {}}
        filterOptions={[]}
        initialQuery={reservationSearchQuery}
      />

      {reservationsLoading ? (
        <LoadingAnimation />
      ) : groupedReservations.length === 0 ? (
        <NoReservationAvailable />
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
