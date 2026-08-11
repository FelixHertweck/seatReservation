import Link from "next/link";
import { AlarmClock } from "lucide-react";

import { useT } from "@/lib/i18n/hooks";
import { formatDateTime } from "@/lib/utils";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/custom-ui/skeleton";
import type { UpcomingEvent } from "@/hooks/use-management-overview";

const SKELETON_ROWS = 3;

export function DeadlinesPanel({
  events,
  isLoading,
}: {
  events: UpcomingEvent[];
  isLoading?: boolean;
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
          <Skeleton className="h-3.5 w-32" />
        </div>
        <Skeleton className="h-3 w-24 shrink-0" />
      </div>
    ));
  } else if (events.length === 0) {
    content = (
      <p className="text-sm text-muted-foreground">
        {t("management.overview.panels.deadlines.empty")}
      </p>
    );
  } else {
    content = events.map((item) => {
      const deadline = formatDateTime(item.bookingDeadline);
      return (
        <Link
          key={item.id}
          href={`/management/events`}
          className="flex items-center justify-between gap-3 rounded-md px-2 py-2 -mx-2 transition-colors hover:bg-accent/40"
        >
          <div className="flex min-w-0 items-center gap-3">
            <AlarmClock className="h-4 w-4 shrink-0 text-amber-500" />
            <p className="truncate text-sm font-medium">{item.name}</p>
          </div>
          {deadline && (
            <span className="shrink-0 text-xs text-muted-foreground tabular-nums">
              {t("management.overview.panels.deadlines.deadlineOn", {
                date: `${deadline.date} ${deadline.time}`,
              })}
            </span>
          )}
        </Link>
      );
    });
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base">
          {t("management.overview.panels.deadlines.title")}
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-1">{content}</CardContent>
    </Card>
  );
}
