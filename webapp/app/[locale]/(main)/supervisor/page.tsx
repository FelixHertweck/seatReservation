"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import {
  ArrowRight,
  BookmarkCheck,
  Eye,
  LogIn as CheckInIcon,
  Store,
  UserCheck,
} from "lucide-react";
import type { LucideIcon } from "lucide-react";

import { useT } from "@/lib/i18n/hooks";
import { PageHeader } from "@/components/page-header";
import { Card, CardContent } from "@/components/ui/card";
import { StatCard } from "@/components/management/dashboard/stat-card";
import { Skeleton } from "@/components/custom-ui/skeleton";
import EventSelector from "@/components/common/supervisor/event-selector";
import { useLiveView } from "@/hooks/use-liveview";
import { useCheckin } from "@/hooks/use-checkin";
import { useSupervisorEvent } from "@/hooks/use-supervisor-event";
import { ReservationLiveStatus } from "@/api";

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

export default function SupervisorOverviewPage() {
  const t = useT();
  const { selectedEventId, selectEvent } = useSupervisorEvent();
  const { events, isLoadingEvents } = useCheckin();

  // Read via an effect rather than at render time (Date.now() is impure) -- refreshed
  // periodically so an event crossing its booking deadline while this page stays open
  // becomes available without a manual reload.
  const [now, setNow] = useState<number | null>(null);
  useEffect(() => {
    // Ticking clock synced from an external source (the system clock), so the
    // initial read + periodic refresh both belong here rather than in render.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setNow(Date.now());
    const interval = setInterval(() => setNow(Date.now()), 60_000);
    return () => clearInterval(interval);
  }, []);

  const availableEvents = useMemo(() => {
    if (now === null) return [];
    return (events ?? []).filter(
      (e) => !!e.bookingDeadline && new Date(e.bookingDeadline).getTime() < now,
    );
  }, [events, now]);
  const selectedEvent = useMemo(
    () => (events ?? []).find((e) => e.id === selectedEventId),
    [events, selectedEventId],
  );
  const isDeadlinePassed = useMemo(() => {
    if (now === null || !selectedEvent?.bookingDeadline) return false;
    return new Date(selectedEvent.bookingDeadline).getTime() < now;
  }, [selectedEvent, now]);

  const { isInitialLoading, reservations } = useLiveView(
    selectedEventId,
    !!selectedEventId && isDeadlinePassed,
  );

  const totalReservations = reservations.length;
  const checkedInCount = reservations.filter(
    (r) => r.liveStatus === ReservationLiveStatus.CHECKED_IN,
  ).length;
  const boxOfficeGuestCount = reservations.filter((r) => !!r.guestName).length;

  return (
    <div className="container mx-auto p-4 sm:p-6">
      <PageHeader
        title={t("supervisor.overview.title")}
        description={t("supervisor.overview.description")}
        search={
          <EventSelector
            events={availableEvents}
            isLoadingEvents={isLoadingEvents}
            selectedEventId={selectedEventId}
            onEventSelect={selectEvent}
          />
        }
      />

      {selectedEventId && (
        <div className="mb-4 grid grid-cols-1 gap-4 sm:grid-cols-3">
          {isInitialLoading ? (
            Array.from({ length: 3 }, (_, i) => (
              <Skeleton key={i} className="h-[4.5rem] rounded-lg" />
            ))
          ) : (
            <>
              <StatCard
                label={t("supervisor.overview.stats.reservations")}
                value={totalReservations}
                icon={BookmarkCheck}
                href="/supervisor/liveview"
              />
              <StatCard
                label={t("supervisor.overview.stats.checkedIn")}
                value={checkedInCount}
                subLabel={t("supervisor.overview.stats.checkedInSub", {
                  total: totalReservations,
                })}
                icon={UserCheck}
                href="/supervisor/checkin"
              />
              <StatCard
                label={t("supervisor.overview.stats.boxOfficeGuests")}
                value={boxOfficeGuestCount}
                subLabel={t("supervisor.overview.stats.boxOfficeGuestsSub")}
                icon={Store}
                href="/supervisor/box-office"
              />
            </>
          )}
        </div>
      )}

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
    </div>
  );
}
