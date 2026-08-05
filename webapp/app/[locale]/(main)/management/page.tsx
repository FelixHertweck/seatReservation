"use client";

import { GaugeIcon } from "@/components/ui/gauge";
import { BookmarkCheckIcon } from "@/components/ui/bookmark-check";
import { TicketIcon } from "@/components/ui/ticket";
import { CalendarDaysIcon } from "@/components/ui/calendar-days";
import { DoorOpen } from "lucide-react";

import { useT } from "@/lib/i18n/hooks";
import { PageHeader } from "@/components/page-header";
import { StatCard } from "@/components/management/dashboard/stat-card";
import { UpcomingEventsPanel } from "@/components/management/dashboard/upcoming-events-panel";
import { QuickActions } from "@/components/management/dashboard/quick-actions";
import { DeadlinesPanel } from "@/components/management/dashboard/deadlines-panel";
import { useManagementOverview } from "@/hooks/use-management-overview";
import { Skeleton } from "@/components/custom-ui/skeleton";

export default function ManagementOverviewPage() {
  const t = useT();
  const { isLoading, stats, upcomingEvents, deadlineWarnings } =
    useManagementOverview();

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
            label={t("management.overview.stats.occupancy")}
            value={stats.occupancyPercent}
            suffix="%"
            subLabel={t("management.overview.stats.occupancySub", {
              reserved: stats.occupancyReserved,
              capacity: stats.occupancyCapacity,
            })}
            icon={GaugeIcon}
            href="/management/events"
          />
          <StatCard
            label={t("management.overview.stats.events")}
            value={stats.eventsCount}
            subLabel={t("management.overview.stats.eventsSub", {
              count: stats.upcomingEventsCount,
            })}
            icon={CalendarDaysIcon}
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
            icon={BookmarkCheckIcon}
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
        <StatCard
          className="lg:col-span-2"
          label={t("management.overview.stats.contingentUsage")}
          value={stats.contingentUsagePercent}
          suffix="%"
          subLabel={t("management.overview.stats.contingentUsageSub", {
            used: stats.contingentUsed,
            total: stats.contingentGranted,
          })}
          icon={TicketIcon}
          href="/management/allowances"
        />
      </div>
    </div>
  );
}
