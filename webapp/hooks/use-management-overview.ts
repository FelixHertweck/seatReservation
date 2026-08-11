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

const UPCOMING_EVENTS_LIMIT = 5;
const DEADLINES_LIMIT = 5;

export interface UpcomingEvent {
  event: EventResponseDto;
  location: EventLocationResponseDto | undefined;
  reservedCount: number;
  capacity: number;
}

export interface ManagementOverview {
  isLoading: boolean;
  stats: {
    eventsCount: number;
    upcomingEventsCount: number;
    bookingOpenCount: number;
    reservationsCount: number;
    reservationsReserved: number;
    reservationsBlocked: number;
    reservationsPending: number;
    occupancyPercent: number;
    occupancyReserved: number;
    occupancyCapacity: number;
    contingentUsagePercent: number;
    contingentUsed: number;
    contingentGranted: number;
  };
  upcomingEvents: UpcomingEvent[];
  deadlineWarnings: UpcomingEvent[];
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

    const toUpcomingEvent = (event: EventResponseDto): UpcomingEvent => {
      const location = event.eventLocationId
        ? locationById.get(event.eventLocationId)
        : undefined;
      const reservedCount =
        event.reservedCount ??
        event.seatStatuses?.filter((s) => s.status === "RESERVED").length ??
        0;
      return {
        event,
        location,
        reservedCount,
        capacity: location?.seatCount ?? 0,
      };
    };

    const futureEvents = (events ?? []).filter(
      (e) => e.startTime && e.startTime.getTime() >= now,
    );

    const upcomingEvents = futureEvents
      .slice()
      .sort((a, b) => a.startTime!.getTime() - b.startTime!.getTime())
      .slice(0, UPCOMING_EVENTS_LIMIT)
      .map(toUpcomingEvent);

    const deadlineWarnings = (events ?? [])
      .filter((e) => e.bookingDeadline && e.bookingDeadline.getTime() >= now)
      .sort(
        (a, b) => a.bookingDeadline!.getTime() - b.bookingDeadline!.getTime(),
      )
      .slice(0, DEADLINES_LIMIT)
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

    // Occupancy across all upcoming events: reserved seats vs. total capacity.
    const occupancyReserved = futureEvents.reduce(
      (sum, e) =>
        sum +
        (e.reservedCount ??
          e.seatStatuses?.filter((s) => s.status === "RESERVED").length ??
          0),
      0,
    );
    const occupancyCapacity = futureEvents.reduce((sum, e) => {
      const location = e.eventLocationId
        ? locationById.get(e.eventLocationId)
        : undefined;
      return sum + (location?.seatCount ?? 0);
    }, 0);
    const occupancyPercent =
      occupancyCapacity > 0
        ? Math.round((occupancyReserved / occupancyCapacity) * 100)
        : 0;

    // Contingents: reservationsAllowedCount is the *remaining* balance (the
    // backend decrements it per reservation and restores it on cancellation),
    // so the originally granted amount per user/event is reconstructed as
    // remaining + already-used (active RESERVED reservations for that pair).
    const reservedCountByPair = new Map<string, number>();
    for (const r of reservations ?? []) {
      if (r.status !== "RESERVED" || !r.eventId || !r.user?.id) continue;
      const key = `${r.eventId}:${r.user.id}`;
      reservedCountByPair.set(key, (reservedCountByPair.get(key) ?? 0) + 1);
    }
    let contingentUsed = 0;
    let contingentGranted = 0;
    for (const a of allowances ?? []) {
      if (!a.eventId || !a.userId) continue;
      const used = reservedCountByPair.get(`${a.eventId}:${a.userId}`) ?? 0;
      contingentUsed += used;
      contingentGranted += (a.reservationsAllowedCount ?? 0) + used;
    }
    const contingentUsagePercent =
      contingentGranted > 0
        ? Math.round((contingentUsed / contingentGranted) * 100)
        : 0;

    return {
      isLoading:
        locationsLoading ||
        eventsLoading ||
        reservationsLoading ||
        allowancesLoading ||
        usersLoading,
      stats: {
        eventsCount: events?.length ?? 0,
        upcomingEventsCount: futureEvents.length,
        bookingOpenCount,
        reservationsCount: reservations?.length ?? 0,
        reservationsReserved,
        reservationsBlocked,
        reservationsPending,
        occupancyPercent,
        occupancyReserved,
        occupancyCapacity,
        contingentUsagePercent,
        contingentUsed,
        contingentGranted,
      },
      upcomingEvents,
      deadlineWarnings,
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
