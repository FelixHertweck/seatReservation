import Link from "next/link";
import { CalendarDays } from "lucide-react";

import { useT } from "@/lib/i18n/hooks";
import { formatDateTime } from "@/lib/utils";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/custom-ui/skeleton";
import type { UpcomingEvent } from "@/hooks/use-management-overview";

const SKELETON_ROWS = 3;

export function UpcomingEventsPanel({
  events,
  isLoading,
  className,
}: {
  events: UpcomingEvent[];
  isLoading?: boolean;
  className?: string;
}) {
  const t = useT();

  let content: React.ReactNode;
  if (isLoading) {
    content = Array.from({ length: SKELETON_ROWS }, (_, i) => (
      <div
        key={i}
        className="flex items-center justify-between gap-3 rounded-md px-2 py-2 -mx-2"
      >
        <div className="flex min-w-0 items-center gap-3">
          <Skeleton className="h-4 w-4 shrink-0 rounded-sm" />
          <div className="min-w-0 space-y-1">
            <Skeleton className="h-3.5 w-32" />
            <Skeleton className="h-3 w-24" />
          </div>
        </div>
        <Skeleton className="h-3 w-14 shrink-0" />
      </div>
    ));
  } else if (events.length === 0) {
    content = (
      <p className="text-sm text-muted-foreground">
        {t("management.overview.panels.upcomingEvents.empty")}
      </p>
    );
  } else {
    content = events.map((item) => {
      const start = formatDateTime(item.startTime);
      return (
        <Link
          key={item.id}
          href={`/management/reservations?eventId=${item.id}`}
          className="flex items-center justify-between gap-3 rounded-md px-2 py-2 -mx-2 transition-colors hover:bg-accent/40"
        >
          <div className="flex min-w-0 items-center gap-3">
            <CalendarDays className="h-4 w-4 shrink-0 text-muted-foreground" />
            <div className="min-w-0">
              <p className="truncate text-sm font-medium">{item.name}</p>
              <p className="truncate text-xs text-muted-foreground">
                {item.locationName ?? "—"}
                {start && ` · ${start.date} ${start.time}`}
              </p>
            </div>
          </div>
          {(item.capacity ?? 0) > 0 && (
            <span className="shrink-0 text-xs text-muted-foreground tabular-nums">
              {t("management.overview.panels.upcomingEvents.occupancy", {
                reserved: item.reservedCount ?? 0,
                capacity: item.capacity ?? 0,
              })}
            </span>
          )}
        </Link>
      );
    });
  }

  return (
    <Card className={className}>
      <CardHeader>
        <CardTitle className="text-base">
          {t("management.overview.panels.upcomingEvents.title")}
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-1">{content}</CardContent>
    </Card>
  );
}
