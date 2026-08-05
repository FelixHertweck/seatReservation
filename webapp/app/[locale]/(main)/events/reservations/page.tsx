"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
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
import { PageHeader } from "@/components/page-header";
import { Button } from "@/components/custom-ui/button";
import { CalendarDays } from "@/components/icons";

interface SelectedReservation {
  reservation: UserReservationResponseDto;
  event: UserEventResponseDto | null;
  location: UserEventLocationResponseDto | null;
  eventReservations?: UserReservationResponseDto[] | null;
}

export default function MyReservationsPage() {
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

  const eventIdFromUrl = searchParams.get("eventId");

  const [userSearchQuery, setUserSearchQuery] = useState<string>("");
  const [filters, setFilters] = useState<Record<string, unknown>>(() =>
    eventIdFromUrl ? { eventId: eventIdFromUrl } : {},
  );

  const eventOptions = useMemo(() => {
    const seen = new Map<string, string>();
    for (const reservation of reservations) {
      if (!reservation.eventId) continue;
      const event = events.find((e) => e.id === reservation.eventId);
      if (event?.name) seen.set(reservation.eventId, event.name);
    }
    return [...seen.entries()].map(([value, label]) => ({ value, label }));
  }, [reservations, events]);

  const locationOptions = useMemo(
    () =>
      locations
        .filter((l) => l.id && l.name)
        .map((l) => ({ value: l.id!, label: l.name! })),
    [locations],
  );

  const groupedReservations = useMemo(() => {
    if (!reservations || !events) return [];

    const query = userSearchQuery.toLowerCase();
    const eventIdFilter = filters.eventId as string | undefined;
    const locationIdFilter = filters.locationId as string | undefined;

    const filtered = reservations.filter((reservation) => {
      const event = events.find((e) => e.id === reservation.eventId);
      const matchesQuery =
        !query || (event?.name?.toLowerCase().includes(query) ?? false);
      const matchesEvent =
        !eventIdFilter || reservation.eventId === eventIdFilter;
      const matchesLocation =
        !locationIdFilter || event?.locationId === locationIdFilter;
      return matchesQuery && matchesEvent && matchesLocation;
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
  }, [reservations, userSearchQuery, filters, events]);

  const handleDeleteReservation = async (reservationIds: string[]) => {
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

  useEffect(() => {
    if (!eventIdFromUrl || eventsLoading || !isLoggedIn) return;
    if (events.some((e) => e.id === eventIdFromUrl)) return;

    router.replace("/events/reservations");
    toast.error(t("reservationsPage.noReservationsFoundTitle"), {
      description: t("reservationsPage.noReservationsFoundDescription"),
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [eventIdFromUrl, eventsLoading, isLoggedIn, events]);

  return (
    <div className="container mx-auto px-2 py-3 md:p-6">
      <PageHeader
        title={t("reservationsPage.title")}
        description={t("reservationsPage.description")}
        search={
          <SearchAndFilter
            onSearch={setUserSearchQuery}
            onFilter={setFilters}
            filterOptions={[
              ...(eventOptions.length > 0
                ? [
                    {
                      key: "eventId",
                      label: t("reservationsPage.eventFilterLabel"),
                      type: "select" as const,
                      options: eventOptions,
                    },
                  ]
                : []),
              ...(locationOptions.length > 0
                ? [
                    {
                      key: "locationId",
                      label: t("reservationsPage.locationFilterLabel"),
                      type: "select" as const,
                      options: locationOptions,
                    },
                  ]
                : []),
            ]}
            initialFilters={
              eventIdFromUrl ? { eventId: eventIdFromUrl } : undefined
            }
            initialQuery={userSearchQuery}
            className="w-full"
          />
        }
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
                viewEventHref={
                  firstReservation.eventId
                    ? `/events?eventId=${firstReservation.eventId}`
                    : undefined
                }
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
          areas={selectedReservation.location?.areas || []}
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
        <Button variant="outline" className="mt-4" asChild>
          <Link href="/events">
            <CalendarDays className="h-4 w-4" />
            {t("eventsNav.browse")}
          </Link>
        </Button>
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
