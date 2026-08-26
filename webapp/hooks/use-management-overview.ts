"use client";

import { useQuery } from "@tanstack/react-query";

import { getApiManagerOverviewOptions } from "@/api/@tanstack/react-query.gen";
import type { UpcomingEventDto } from "@/api";

export type UpcomingEvent = UpcomingEventDto;

export interface EventContingentUsage {
  id: string;
  name: string;
  startTime?: Date | string | null;
  locationName?: string | null;
  used: number;
  total: number;
  percent: number;
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
  contingentEvents: EventContingentUsage[];
}

export function useManagementOverview(): ManagementOverview {
  const { data: overview, isLoading } = useQuery({
    ...getApiManagerOverviewOptions(),
  });

  return {
    isLoading,
    stats: {
      eventsCount: Number(overview?.stats?.eventsCount ?? 0),
      upcomingEventsCount: Number(overview?.stats?.upcomingEventsCount ?? 0),
      bookingOpenCount: Number(overview?.stats?.bookingOpenCount ?? 0),
      reservationsCount: Number(overview?.stats?.reservationsCount ?? 0),
      reservationsReserved: Number(overview?.stats?.reservationsReserved ?? 0),
      reservationsBlocked: Number(overview?.stats?.reservationsBlocked ?? 0),
      reservationsPending: Number(overview?.stats?.reservationsPending ?? 0),
      occupancyPercent: overview?.stats?.occupancyPercent ?? 0,
      occupancyReserved: Number(overview?.stats?.occupancyReserved ?? 0),
      occupancyCapacity: Number(overview?.stats?.occupancyCapacity ?? 0),
      contingentUsagePercent: overview?.stats?.contingentUsagePercent ?? 0,
      contingentUsed: Number(overview?.stats?.contingentUsed ?? 0),
      contingentGranted: Number(overview?.stats?.contingentGranted ?? 0),
    },
    upcomingEvents: overview?.upcomingEvents ?? [],
    deadlineWarnings: overview?.deadlineWarnings ?? [],
    contingentEvents: ((
      overview as unknown as { contingentEvents?: EventContingentUsage[] }
    )?.contingentEvents ?? []) as EventContingentUsage[],
  };
}
