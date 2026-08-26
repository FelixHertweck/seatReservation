"use client";

import { Gauge, CalendarDays, DoorOpen, BookmarkCheck } from "lucide-react";

import { useT } from "@/lib/i18n/hooks";
import { PageHeader } from "@/components/page-header";
import { StatCard } from "@/components/management/dashboard/stat-card";
import { UpcomingEventsPanel } from "@/components/management/dashboard/upcoming-events-panel";
import { QuickActions } from "@/components/management/dashboard/quick-actions";
import { DeadlinesPanel } from "@/components/management/dashboard/deadlines-panel";
import { ContingentUsagePanel } from "@/components/management/dashboard/contingent-usage-panel";
import { useManagementOverview } from "@/hooks/use-management-overview";
import { OverviewSkeleton } from "@/components/management/dashboard/overview-skeleton";

export default function ManagementOverviewPage() {
  const t = useT();
  const {
    isLoading,
    stats,
    upcomingEvents,
    deadlineWarnings,
    contingentEvents,
  } = useManagementOverview();

  return (
    <div className="container mx-auto p-4 sm:p-6">
      <PageHeader
        title={t("management.overview.title")}
        description={t("management.overview.description")}
      />

      {isLoading ? (
        <OverviewSkeleton />
      ) : (
        <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
          <StatCard
            label={t("management.overview.stats.occupancy")}
            value={stats.occupancyPercent}
            suffix="%"
            subLabel={t("management.overview.stats.occupancySub", {
              reserved: stats.occupancyReserved,
              capacity: stats.occupancyCapacity,
            })}
            icon={Gauge}
            href="/management/events"
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
        <UpcomingEventsPanel
          className="lg:col-span-2"
          events={upcomingEvents}
          isLoading={isLoading}
        />
        <QuickActions />
      </div>

      <div className="mt-4 grid grid-cols-1 gap-4 lg:grid-cols-3">
        <DeadlinesPanel events={deadlineWarnings} isLoading={isLoading} />
        <ContingentUsagePanel
          className="lg:col-span-2"
          events={contingentEvents}
          stats={stats}
          isLoading={isLoading}
        />
      </div>
    </div>
  );
}
