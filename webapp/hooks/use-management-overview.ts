"use client";

import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";

import {
  getApiManagerEventlocationsOptions,
  getApiManagerEventsOptions,
  getApiManagerReservationsOptions,
  getApiManagerReservationAllowanceOptions,
  getApiUsersManagerOptions,
} from "@/api/@tanstack/react-query.gen";
import type { EventLocationResponseDto, EventResponseDto } from "@/api";

const SEVEN_DAYS_MS = 7 * 24 * 60 * 60 * 1000;
const UPCOMING_EVENTS_LIMIT = 5;

export interface UpcomingEvent {
  event: EventResponseDto;
  location: EventLocationResponseDto | undefined;
  reservedCount: number;
  capacity: number;
}

export interface LocationCapacity {
  location: EventLocationResponseDto;
  seatCount: number;
  eventCount: number;
}

export interface ManagementOverview {
  isLoading: boolean;
  stats: {
    locationsCount: number;
    totalSeats: number;
    eventsCount: number;
    upcomingEventsCount: number;
    bookingOpenCount: number;
    reservationsCount: number;
    reservationsReserved: number;
    reservationsBlocked: number;
    reservationsPending: number;
    allowancesTotal: number;
    allowancesUserCount: number;
  };
  upcomingEvents: UpcomingEvent[];
  deadlineWarnings: UpcomingEvent[];
  locationCapacities: LocationCapacity[];
}

/**
 * Derives every dashboard-overview tile/panel from the same list queries the
 * rest of the management section already uses - no dedicated aggregate
 * backend endpoint exists (or is needed) for this.
 */
export function useManagementOverview(): ManagementOverview {
  const { data: locations, isLoading: locationsLoading } = useQuery({
    ...getApiManagerEventlocationsOptions(),
  });
  const { data: events, isLoading: eventsLoading } = useQuery({
    ...getApiManagerEventsOptions(),
  });
  const { data: reservations, isLoading: reservationsLoading } = useQuery({
    ...getApiManagerReservationsOptions(),
  });
  const { data: allowances, isLoading: allowancesLoading } = useQuery({
    ...getApiManagerReservationAllowanceOptions(),
  });
  const { isLoading: usersLoading } = useQuery({
    ...getApiUsersManagerOptions(),
  });

  // Captured once per hook instance rather than read directly during render,
  // to satisfy the rule against calling impure functions (Date.now) in render.
  const [now] = useState(() => Date.now());

  return useMemo(() => {
    const locationById = new Map(
      (locations ?? []).map((location) => [location.id, location]),
    );

    const eventsByLocationId = new Map<string, number>();
    for (const event of events ?? []) {
      if (!event.eventLocationId) continue;
      eventsByLocationId.set(
        event.eventLocationId,
        (eventsByLocationId.get(event.eventLocationId) ?? 0) + 1,
      );
    }

    const toUpcomingEvent = (event: EventResponseDto): UpcomingEvent => {
      const location = event.eventLocationId
        ? locationById.get(event.eventLocationId)
        : undefined;
      const reservedCount =
        event.seatStatuses?.filter((s) => s.status === "RESERVED").length ?? 0;
      return {
        event,
        location,
        reservedCount,
        capacity: location?.seatIds?.length ?? 0,
      };
    };

    const upcomingEvents = (events ?? [])
      .filter((e) => e.startTime && e.startTime.getTime() >= now)
      .sort((a, b) => a.startTime!.getTime() - b.startTime!.getTime())
      .slice(0, UPCOMING_EVENTS_LIMIT)
      .map(toUpcomingEvent);

    const deadlineWarnings = (events ?? [])
      .filter(
        (e) =>
          e.bookingDeadline &&
          e.bookingDeadline.getTime() >= now &&
          e.bookingDeadline.getTime() - now <= SEVEN_DAYS_MS,
      )
      .sort(
        (a, b) => a.bookingDeadline!.getTime() - b.bookingDeadline!.getTime(),
      )
      .map(toUpcomingEvent);

    const bookingOpenCount = (events ?? []).filter(
      (e) =>
        e.bookingStartTime &&
        e.bookingDeadline &&
        e.bookingStartTime.getTime() <= now &&
        now <= e.bookingDeadline.getTime(),
    ).length;

    const reservationsReserved = (reservations ?? []).filter(
      (r) => r.status === "RESERVED",
    ).length;
    const reservationsBlocked = (reservations ?? []).filter(
      (r) => r.status === "BLOCKED",
    ).length;
    const reservationsPending = (reservations ?? []).filter(
      (r) => r.status === "PENDING",
    ).length;

    const allowancesTotal = (allowances ?? []).reduce(
      (sum, a) => sum + (a.reservationsAllowedCount ?? 0),
      0,
    );
    const allowancesUserCount = new Set(
      (allowances ?? []).map((a) => a.userId).filter(Boolean),
    ).size;

    const locationCapacities: LocationCapacity[] = (locations ?? []).map(
      (location) => ({
        location,
        seatCount: location.seatIds?.length ?? 0,
        eventCount: eventsByLocationId.get(location.id ?? "") ?? 0,
      }),
    );

    return {
      isLoading:
        locationsLoading ||
        eventsLoading ||
        reservationsLoading ||
        allowancesLoading ||
        usersLoading,
      stats: {
        locationsCount: locations?.length ?? 0,
        totalSeats: (locations ?? []).reduce(
          (sum, l) => sum + (l.seatIds?.length ?? 0),
          0,
        ),
        eventsCount: events?.length ?? 0,
        upcomingEventsCount: (events ?? []).filter(
          (e) => e.startTime && e.startTime.getTime() >= now,
        ).length,
        bookingOpenCount,
        reservationsCount: reservations?.length ?? 0,
        reservationsReserved,
        reservationsBlocked,
        reservationsPending,
        allowancesTotal,
        allowancesUserCount,
      },
      upcomingEvents,
      deadlineWarnings,
      locationCapacities,
    };
  }, [
    now,
    locations,
    events,
    reservations,
    allowances,
    locationsLoading,
    eventsLoading,
    reservationsLoading,
    allowancesLoading,
    usersLoading,
  ]);
}
