import Link from "next/link";
import { CalendarDays } from "lucide-react";

import { useT } from "@/lib/i18n/hooks";
import { formatDateTime } from "@/lib/utils";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import type { UpcomingEvent } from "@/hooks/use-management-overview";

export function UpcomingEventsPanel({ events }: { events: UpcomingEvent[] }) {
  const t = useT();

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base">
          {t("management.overview.panels.upcomingEvents.title")}
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-1">
        {events.length === 0 ? (
          <p className="text-sm text-muted-foreground">
            {t("management.overview.panels.upcomingEvents.empty")}
          </p>
        ) : (
          events.map(({ event, location, reservedCount, capacity }) => {
            const start = formatDateTime(event.startTime);
            return (
              <Link
                key={event.id}
                href={`/management/reservations?eventId=${event.id}`}
                className="flex items-center justify-between gap-3 rounded-md px-2 py-2 -mx-2 transition-colors hover:bg-accent/40"
              >
                <div className="flex min-w-0 items-center gap-3">
                  <CalendarDays className="h-4 w-4 shrink-0 text-muted-foreground" />
                  <div className="min-w-0">
                    <p className="truncate text-sm font-medium">{event.name}</p>
                    <p className="truncate text-xs text-muted-foreground">
                      {location?.name ?? "—"}
                      {start && ` · ${start.date} ${start.time}`}
                    </p>
                  </div>
                </div>
                {capacity > 0 && (
                  <span className="shrink-0 text-xs text-muted-foreground tabular-nums">
                    {t("management.overview.panels.upcomingEvents.occupancy", {
                      reserved: reservedCount,
                      capacity,
                    })}
                  </span>
                )}
              </Link>
            );
          })
        )}
      </CardContent>
    </Card>
  );
}
