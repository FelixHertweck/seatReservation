import Link from "next/link";
import { Ticket, ArrowRight } from "lucide-react";

import { useT } from "@/lib/i18n/hooks";
import { formatDateTime } from "@/lib/utils";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/custom-ui/skeleton";
import type { EventContingentUsage } from "@/hooks/use-management-overview";

const SKELETON_ROWS = 3;

function getProgressColor(percent: number): string {
  if (percent >= 100) return "bg-emerald-500";
  if (percent >= 80) return "bg-amber-500";
  return "bg-primary";
}

export function ContingentUsagePanel({
  events,
  stats,
  isLoading,
  className,
}: Readonly<{
  events: EventContingentUsage[];
  stats?: {
    contingentUsed: number;
    contingentGranted: number;
    contingentUsagePercent: number;
  };
  isLoading?: boolean;
  className?: string;
}>) {
  const t = useT();

  let content: React.ReactNode;
  if (isLoading) {
    content = Array.from({ length: SKELETON_ROWS }, (_, i) => (
      <div key={i} className="space-y-2 rounded-md px-2 py-2.5 -mx-2">
        <div className="flex items-center justify-between gap-3">
          <div className="space-y-1">
            <Skeleton className="h-3.5 w-36" />
            <Skeleton className="h-3 w-24" />
          </div>
          <Skeleton className="h-3.5 w-16" />
        </div>
        <Skeleton className="h-2 w-full rounded-full" />
      </div>
    ));
  } else if (events.length === 0) {
    content = (
      <div className="flex flex-col items-center justify-center py-6 text-center">
        <Ticket className="h-8 w-8 text-muted-foreground/50 mb-2" />
        <p className="text-sm text-muted-foreground">
          {t("management.overview.panels.contingentUsage.empty")}
        </p>
        <Link
          href="/management/allowances"
          className="mt-2 text-xs font-medium text-primary hover:underline flex items-center gap-1"
        >
          {t("management.overview.panels.contingentUsage.manageAllowances")}
          <ArrowRight className="h-3 w-3" />
        </Link>
      </div>
    );
  } else {
    content = events.map((item) => {
      const start = formatDateTime(item.startTime);
      const percent = Math.min(100, Math.max(0, item.percent));
      const progressColor = getProgressColor(percent);

      return (
        <Link
          key={item.id}
          href={`/management/allowances?eventId=${item.id}`}
          className="group block rounded-lg px-3 py-2.5 -mx-2 transition-all hover:bg-accent/40"
        >
          <div className="flex items-center justify-between gap-3 mb-1.5">
            <div className="min-w-0 flex-1">
              <p className="truncate text-sm font-medium group-hover:text-primary transition-colors">
                {item.name}
              </p>
              <p className="truncate text-xs text-muted-foreground">
                {item.locationName ?? "—"}
                {start && ` · ${start.date} ${start.time}`}
              </p>
            </div>
            <div className="text-right shrink-0">
              <span className="text-xs font-semibold tabular-nums">
                {item.used} / {item.total}
              </span>
              <span className="text-xs text-muted-foreground ml-1">
                ({percent}%)
              </span>
            </div>
          </div>

          {/* Horizontal progress bar / loader */}
          <div
            className="h-2 w-full rounded-full bg-secondary/80 overflow-hidden"
            role="progressbar"
            aria-valuenow={percent}
            aria-valuemin={0}
            aria-valuemax={100}
            aria-label={`${item.name} contingent usage`}
          >
            <div
              className={`h-full rounded-full ${progressColor} transition-all duration-500 ease-out`}
              style={{ width: `${percent}%` }}
            />
          </div>
        </Link>
      );
    });
  }

  return (
    <Card className={className}>
      <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-3">
        <div className="flex items-center gap-2">
          <Ticket className="h-4 w-4 text-muted-foreground" />
          <CardTitle className="text-base">
            {t("management.overview.panels.contingentUsage.title")}
          </CardTitle>
        </div>
        {stats && stats.contingentGranted > 0 && !isLoading && (
          <Badge variant="secondary" className="font-normal text-xs">
            {t("management.overview.panels.contingentUsage.totalSummary", {
              used: stats.contingentUsed,
              total: stats.contingentGranted,
              percent: stats.contingentUsagePercent,
            })}
          </Badge>
        )}
      </CardHeader>
      <CardContent className="space-y-2">{content}</CardContent>
    </Card>
  );
}
