"use client";

import {
  MapPinned,
  CalendarDays,
  DoorOpen,
  BookmarkCheck,
  Ticket,
} from "lucide-react";

import { useT } from "@/lib/i18n/hooks";
import { PageHeader } from "@/components/page-header";
import { StatCard } from "@/components/management/dashboard/stat-card";
import { UpcomingEventsPanel } from "@/components/management/dashboard/upcoming-events-panel";
import { CapacityPanel } from "@/components/management/dashboard/capacity-panel";
import { QuickActions } from "@/components/management/dashboard/quick-actions";
import { DeadlinesPanel } from "@/components/management/dashboard/deadlines-panel";
import { useManagementOverview } from "@/hooks/use-management-overview";
import { Skeleton } from "@/components/ui/skeleton";

export default function ManagementOverviewPage() {
  const t = useT();
  const {
    isLoading,
    stats,
    upcomingEvents,
    deadlineWarnings,
    locationCapacities,
  } = useManagementOverview();

  return (
    <div className="container mx-auto p-4 sm:p-6">
      <PageHeader
        title={t("management.overview.title")}
        description={t("management.overview.description")}
      />

      {isLoading ? (
        <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
          {Array.from({ length: 4 }, (_, i) => (
            <Skeleton key={i} className="h-24 rounded-lg" />
          ))}
        </div>
      ) : (
        <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
          <StatCard
            label={t("management.overview.stats.locations")}
            value={stats.locationsCount}
            subLabel={t("management.overview.stats.locationsSub", {
              count: stats.totalSeats,
            })}
            icon={MapPinned}
            href="/management/locations"
          />
          <StatCard
            label={t("management.overview.stats.events")}
            value={stats.eventsCount}
            subLabel={t("management.overview.stats.eventsSub", {
              count: stats.upcomingEventsCount,
            })}
            icon={CalendarDays}
            href="/management/events"
          />
          <StatCard
            label={t("management.overview.stats.bookingOpen")}
            value={stats.bookingOpenCount}
            subLabel={t("management.overview.stats.bookingOpenSub")}
            icon={DoorOpen}
            href="/management/events"
          />
          <StatCard
            label={t("management.overview.stats.reservations")}
            value={stats.reservationsCount}
            subLabel={t("management.overview.stats.reservationsSub", {
              reserved: stats.reservationsReserved,
              blocked: stats.reservationsBlocked,
              pending: stats.reservationsPending,
            })}
            icon={BookmarkCheck}
            href="/management/reservations"
          />
        </div>
      )}

      <div className="mt-6 grid grid-cols-1 gap-4 lg:grid-cols-3">
        <UpcomingEventsPanel events={upcomingEvents} />
        <CapacityPanel locations={locationCapacities} />
        <QuickActions />
      </div>

      <div className="mt-4 grid grid-cols-1 gap-4 lg:grid-cols-3">
        <DeadlinesPanel events={deadlineWarnings} />
        <StatCard
          className="lg:col-span-2"
          label={t("management.overview.stats.allowances")}
          value={stats.allowancesTotal}
          subLabel={t("management.overview.stats.allowancesSub", {
            count: stats.allowancesUserCount,
          })}
          icon={Ticket}
          href="/management/allowances"
        />
      </div>
    </div>
  );
}
