"use client";

import { useMemo, useState } from "react";
import { useReservations } from "@/hooks/use-reservations";
import { useT } from "@/lib/i18n/hooks";
import {
  UserEventLocationResponseDto,
  UserEventResponseDto,
  UserReservationResponseDto,
} from "@/api";
import { useEvents } from "@/hooks/use-events";
import { SearchAndFilter } from "@/components/common/search-and-filter";
import { ReservationCardSkeleton } from "@/components/reservations/reservation-card-skeleton";
import { SeatMapModal } from "@/components/reservations/reservation-modal";
import { ReservationCard } from "@/components/reservations/reservation-card";
import { useAuth } from "@/hooks/use-auth";
import { useRouter, useSearchParams } from "next/navigation";
import { toast } from "sonner";

interface SelectedReservation {
  reservation: UserReservationResponseDto;
  event: UserEventResponseDto | null;
  location: UserEventLocationResponseDto | null;
  eventReservations?: UserReservationResponseDto[] | null;
}

export default function EventsPage() {
  const t = useT();
  const router = useRouter();

  const {
    reservations,
    isLoading: reservationsLoading,
    deleteReservation,
  } = useReservations();
  const { isLoading: eventsLoading, events, locations } = useEvents();
  const searchParams = useSearchParams();
  const { isLoggedIn } = useAuth();
  const [selectedReservation, setSelectedReservation] =
    useState<SelectedReservation | null>(null);

  const reservationSearchQuery = useMemo(() => {
    const eventIdFromUrl = searchParams.get("id");
    if (eventIdFromUrl && events && !eventsLoading && isLoggedIn) {
      const eventId = BigInt(eventIdFromUrl);
      const event = events.find((e) => e.id === eventId);
      if (event) {
        return event.name || "";
      } else {
        router.replace("/reservations");
        setTimeout(() => {
          toast.error(t("reservationsPage.noReservationsFoundTitle"), {
            description: t("reservationsPage.noReservationsFoundDescription"),
          });
        }, 500);
        return "";
      }
    }
    return "";
  }, [searchParams, events, eventsLoading, isLoggedIn, router, t]);

  const [userSearchQuery, setUserSearchQuery] = useState<string>("");
  const effectiveSearchQuery =
    userSearchQuery !== "" ? userSearchQuery : reservationSearchQuery;

  const groupedReservations = useMemo(() => {
    if (!reservations || !events) return [];

    const filtered = reservations.filter((reservation) => {
      const event = events.find((e) => e.id === reservation.eventId);
      const eventName = event?.name?.toLowerCase() || "";
      return eventName.includes(effectiveSearchQuery.toLowerCase());
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
  }, [reservations, effectiveSearchQuery, events]);

  const handleReservationSearch = (query: string) => {
    setUserSearchQuery(query);
  };

  const handleDeleteReservation = async (reservationIds: bigint[]) => {
    await deleteReservation(reservationIds);
  };

  const handleViewReservationSeats = (
    reservation: UserReservationResponseDto,
  ) => {
    const event = events?.find((e) => e.id === reservation.eventId) ?? null;
    const location = locations?.find((l) => l.id === event?.locationId) ?? null;
    const eventReservations = reservations.filter(
      (reservation) => reservation.eventId === event?.id,
    );

    setSelectedReservation({
      reservation,
      event,
      location,
      eventReservations,
    });
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
        initialQuery={effectiveSearchQuery}
      />

      {reservationsLoading ? (
        <LoadingAnimation />
      ) : groupedReservations.length === 0 ? (
        <NoReservationAvailable reservationLength={reservations.length} />
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-2 lg:gap-4">
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
          seats={selectedReservation.location?.seats || []}
          seatStatuses={selectedReservation.event?.seatStatuses || []}
          markers={selectedReservation.location?.markers || []}
          reservation={selectedReservation.reservation}
          eventReservations={selectedReservation.eventReservations || []}
          onClose={() => setSelectedReservation(null)}
          isLoading={false}
        />
      )}
    </div>
  );
}

const LoadingAnimation = () => (
  <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-2 md:gap-4">
    {Array.from({ length: 3 }).map((_, index) => (
      <ReservationCardSkeleton key={index} />
    ))}
  </div>
);

const NoReservationAvailable = ({
  reservationLength,
}: {
  reservationLength: number;
}) => {
  const t = useT();

  if (reservationLength === 0) {
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
