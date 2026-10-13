"use client";

import Link from "next/link";
import { useMemo } from "react";
import {
  Armchair,
  ArrowRight,
  ArrowUp,
  Clock,
  Eye,
  LogIn as CheckInIcon,
  RefreshCw,
  Store,
  UserCheck,
  WifiOff,
} from "lucide-react";
import type { LucideIcon } from "lucide-react";

import { useT } from "@/lib/i18n/hooks";
import { cn, formatDateTime } from "@/lib/utils";
import { SEAT_STATUS_BG } from "@/lib/seatStatusStyles";
import { PageHeader } from "@/components/page-header";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/custom-ui/skeleton";
import EventSelector from "@/components/common/supervisor/event-selector";
import { LiveviewConnectionBadge } from "@/components/liveview/liveview-status";
import { useSupervisorLiveView } from "@/hooks/use-supervisor-liveview";
import { useCheckin } from "@/hooks/use-checkin";
import { useSupervisorEvent } from "@/hooks/use-supervisor-event";
import { ReservationLiveStatus, ReservationStatus } from "@/api";
import type {
  SupervisorEventLocationDto,
  SupervisorReservationResponseDto,
} from "@/api";

function ToolCard({
  href,
  icon: Icon,
  title,
  description,
}: {
  href: string;
  icon: LucideIcon;
  title: string;
  description: string;
}) {
  return (
    <Card className="transition-colors hover:bg-accent/40 hover:border-accent-foreground/20">
      <Link href={href}>
        <CardContent className="flex items-start gap-4 py-6">
          <div className="rounded-lg bg-primary/10 p-2.5 text-primary">
            <Icon className="h-5 w-5" />
          </div>
          <div className="min-w-0 flex-1">
            <div className="flex items-center gap-2">
              <h3 className="font-semibold">{title}</h3>
              <ArrowRight className="h-4 w-4 shrink-0 text-muted-foreground" />
            </div>
            <p className="mt-1 text-sm text-muted-foreground">{description}</p>
          </div>
        </CardContent>
      </Link>
    </Card>
  );
}

interface EventStats {
  totalSeats: number;
  reservedCount: number;
  blockedCount: number;
  freeCount: number;
  bookedCount: number;
  checkedInCount: number;
  awaitingCount: number;
  cancelledCount: number;
  boxOfficeGuestCount: number;
}

function computeEventStats(
  reservations: SupervisorReservationResponseDto[],
  location: SupervisorEventLocationDto | null,
): EventStats {
  const booked = reservations.filter(
    (r) => r.status === ReservationStatus.RESERVED,
  );
  const checkedInCount = booked.filter(
    (r) => r.liveStatus === ReservationLiveStatus.CHECKED_IN,
  ).length;
  // Only cancelled seats are free again; NO_SHOW stays reserved.
  const cancelledCount = booked.filter(
    (r) => r.liveStatus === ReservationLiveStatus.CANCELLED,
  ).length;
  const awaitingCount = booked.length - checkedInCount - cancelledCount;
  const blockedCount = reservations.filter(
    (r) => r.status === ReservationStatus.BLOCKED,
  ).length;
  const totalSeats = location?.seats?.length ?? 0;
  const reservedCount = checkedInCount + awaitingCount;
  return {
    totalSeats,
    reservedCount,
    blockedCount,
    freeCount: Math.max(0, totalSeats - reservedCount - blockedCount),
    bookedCount: booked.length,
    checkedInCount,
    awaitingCount,
    cancelledCount,
    boxOfficeGuestCount: booked.filter(
      (r) => !!r.guestName && r.liveStatus !== ReservationLiveStatus.CANCELLED,
    ).length,
  };
}

function SelectEventNotice() {
  const t = useT();
  return (
    <Card className="mt-4">
      <CardContent className="flex flex-col items-center gap-2 py-12 text-center text-muted-foreground">
        <ArrowUp className="h-5 w-5" />
        {t("supervisor.overview.selectEventPrompt")}
      </CardContent>
    </Card>
  );
}

function DeadlineNotPassedNotice({
  bookingDeadline,
}: {
  bookingDeadline: string | Date | undefined;
}) {
  const t = useT();
  const availableFrom = formatDateTime(bookingDeadline);
  return (
    <Card className="mt-4">
      <CardContent className="flex flex-col items-center gap-2 py-12 text-center text-muted-foreground">
        <Clock className="h-5 w-5" />
        <span>{t("supervisor.overview.deadlineNotPassed")}</span>
        {availableFrom && (
          <span className="text-sm">
            {t(
              "supervisor.overview.deadlineNotPassedAvailableFrom",
              availableFrom,
            )}
          </span>
        )}
      </CardContent>
    </Card>
  );
}

function DisconnectedNotice({ onReconnect }: { onReconnect: () => void }) {
  const t = useT();
  return (
    <Card className="mt-4">
      <CardContent className="flex flex-col items-center gap-2 py-12 text-center text-muted-foreground">
        <WifiOff className="h-5 w-5" />
        <span>{t("supervisor.overview.disconnected")}</span>
        <Button variant="outline" size="sm" onClick={onReconnect}>
          <RefreshCw />
          {t("liveview.status.reconnect")}
        </Button>
      </CardContent>
    </Card>
  );
}

interface StatSegment {
  label: string;
  value: number;
  colorClassName: string;
}

function BreakdownCard({
  icon: Icon,
  title,
  value,
  valueLabel,
  segments,
  footnote,
}: {
  icon: LucideIcon;
  title: string;
  value: number;
  valueLabel: string;
  segments: StatSegment[];
  footnote?: string;
}) {
  return (
    <Card>
      <CardContent className="flex flex-col gap-4 py-2">
        <div className="flex items-center justify-between gap-3">
          <h3 className="text-sm font-medium text-muted-foreground">{title}</h3>
          <Icon className="h-5 w-5 shrink-0 text-muted-foreground" />
        </div>
        <p className="flex flex-wrap items-baseline gap-x-2">
          <span className="text-4xl font-semibold tabular-nums">{value}</span>
          <span className="text-sm text-muted-foreground">{valueLabel}</span>
        </p>
        <div
          aria-hidden="true"
          className="flex h-2 gap-0.5 overflow-hidden rounded-full bg-muted"
        >
          {segments
            .filter((segment) => segment.value > 0)
            .map((segment) => (
              <div
                key={segment.label}
                className={cn("min-w-1", segment.colorClassName)}
                style={{ flexGrow: segment.value }}
              />
            ))}
        </div>
        <dl className="grid grid-cols-3 gap-2">
          {segments.map((segment) => (
            <div key={segment.label} className="min-w-0">
              <dt className="flex items-center gap-1.5 text-xs text-muted-foreground">
                <span
                  className={cn(
                    "h-2 w-2 shrink-0 rounded-full",
                    segment.colorClassName,
                  )}
                />
                <span className="truncate">{segment.label}</span>
              </dt>
              <dd className="text-lg font-semibold tabular-nums">
                {segment.value}
              </dd>
            </div>
          ))}
        </dl>
        {footnote && (
          <p className="text-xs text-muted-foreground">{footnote}</p>
        )}
      </CardContent>
    </Card>
  );
}

function EventStatsSkeleton() {
  return (
    <div className="mt-4 grid grid-cols-1 gap-4 lg:grid-cols-2">
      {Array.from({ length: 2 }, (_, i) => (
        <Skeleton key={i} className="h-52 rounded-lg" />
      ))}
    </div>
  );
}

function EventStatsCards({ stats }: { stats: EventStats }) {
  const t = useT();
  return (
    <div className="mt-4 grid grid-cols-1 gap-4 lg:grid-cols-2">
      <BreakdownCard
        icon={UserCheck}
        title={t("supervisor.overview.stats.admissionTitle")}
        value={stats.checkedInCount}
        valueLabel={t("supervisor.overview.stats.admissionValueLabel", {
          total: stats.bookedCount,
        })}
        segments={[
          {
            label: t("supervisor.overview.stats.checkedIn"),
            value: stats.checkedInCount,
            colorClassName: SEAT_STATUS_BG.CHECKED_IN,
          },
          {
            label: t("supervisor.overview.stats.awaiting"),
            value: stats.awaitingCount,
            colorClassName: SEAT_STATUS_BG.RESERVED,
          },
          {
            label: t("supervisor.overview.stats.cancelled"),
            value: stats.cancelledCount,
            colorClassName: SEAT_STATUS_BG.CANCELLED,
          },
        ]}
      />
      <BreakdownCard
        icon={Armchair}
        title={t("supervisor.overview.stats.seatsTitle")}
        value={stats.freeCount}
        valueLabel={t("supervisor.overview.stats.seatsValueLabel", {
          total: stats.totalSeats,
        })}
        segments={[
          {
            label: t("supervisor.overview.stats.free"),
            value: stats.freeCount,
            colorClassName: SEAT_STATUS_BG.AVAILABLE,
          },
          {
            label: t("supervisor.overview.stats.reserved"),
            value: stats.reservedCount,
            colorClassName: SEAT_STATUS_BG.RESERVED,
          },
          {
            label: t("supervisor.overview.stats.blocked"),
            value: stats.blockedCount,
            colorClassName: SEAT_STATUS_BG.BLOCKED,
          },
        ]}
        footnote={`${t("supervisor.overview.stats.cancelledNote", {
          count: stats.cancelledCount,
        })} ${t("supervisor.overview.stats.boxOfficeGuestsNote", {
          count: stats.boxOfficeGuestCount,
        })}`}
      />
    </div>
  );
}

function EventStatsSection({
  hasSelectedEvent,
  isDeadlineKnown,
  isDeadlinePassed,
  bookingDeadline,
  isLoading,
  hasData,
  onReconnect,
  stats,
}: {
  hasSelectedEvent: boolean;
  isDeadlineKnown: boolean;
  isDeadlinePassed: boolean;
  bookingDeadline: string | Date | undefined;
  isLoading: boolean;
  hasData: boolean;
  onReconnect: () => void;
  stats: EventStats;
}) {
  if (!hasSelectedEvent) return <SelectEventNotice />;
  if (!isDeadlineKnown) return <EventStatsSkeleton />;
  if (!isDeadlinePassed) {
    return <DeadlineNotPassedNotice bookingDeadline={bookingDeadline} />;
  }
  if (isLoading) return <EventStatsSkeleton />;
  if (!hasData) return <DisconnectedNotice onReconnect={onReconnect} />;
  return <EventStatsCards stats={stats} />;
}

export default function SupervisorOverviewPage() {
  const t = useT();
  const { selectedEventId, selectEvent } = useSupervisorEvent();
  const { isLoadingEvents } = useCheckin();

  const {
    isInitialLoading,
    isConnected,
    isConnecting,
    error,
    location,
    reservations,
    availableEvents,
    isDeadlinePassed,
    selectedEvent,
    isDeadlineKnown,
    event,
    reconnect,
  } = useSupervisorLiveView();

  const stats = useMemo(
    () => computeEventStats(reservations, location),
    [reservations, location],
  );

  return (
    <div className="container mx-auto p-4 sm:p-6">
      <PageHeader
        title={t("supervisor.overview.title")}
        description={t("supervisor.overview.description")}
        actions={
          selectedEventId && isDeadlinePassed ? (
            <LiveviewConnectionBadge
              isConnected={isConnected}
              isConnecting={isConnecting}
              error={error}
              event={event}
              onReconnect={reconnect}
            />
          ) : undefined
        }
        search={
          <EventSelector
            events={availableEvents}
            isLoadingEvents={isLoadingEvents}
            selectedEventId={selectedEventId}
            onEventSelect={selectEvent}
          />
        }
      />

      <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
        <ToolCard
          href="/supervisor/checkin"
          icon={CheckInIcon}
          title={t("checkin.title")}
          description={t("checkin.description")}
        />
        <ToolCard
          href="/supervisor/liveview"
          icon={Eye}
          title={t("liveview.title")}
          description={t("liveview.description")}
        />
        <ToolCard
          href="/supervisor/box-office"
          icon={Store}
          title={t("boxOffice.title")}
          description={t("boxOffice.pageDescription")}
        />
      </div>

      <EventStatsSection
        hasSelectedEvent={!!selectedEventId}
        isDeadlineKnown={isDeadlineKnown}
        isDeadlinePassed={isDeadlinePassed}
        bookingDeadline={selectedEvent?.bookingDeadline}
        isLoading={
          isInitialLoading ||
          (location === null && (isConnected || isConnecting))
        }
        hasData={location !== null}
        onReconnect={reconnect}
        stats={stats}
      />
    </div>
  );
}
