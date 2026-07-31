import Link from "next/link";
import { MapPinned } from "lucide-react";

import { useT } from "@/lib/i18n/hooks";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import type { LocationCapacity } from "@/hooks/use-management-overview";

export function CapacityPanel({
  locations,
}: {
  locations: LocationCapacity[];
}) {
  const t = useT();

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base">
          {t("management.overview.panels.capacity.title")}
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-1">
        {locations.length === 0 ? (
          <p className="text-sm text-muted-foreground">
            {t("management.overview.panels.capacity.empty")}
          </p>
        ) : (
          locations.map(({ location, seatCount, eventCount }) => (
            <Link
              key={location.id}
              href={`/management/locations/${location.id}`}
              className="flex items-center justify-between gap-3 rounded-md px-2 py-2 -mx-2 transition-colors hover:bg-accent/40"
            >
              <div className="flex min-w-0 items-center gap-3">
                <MapPinned className="h-4 w-4 shrink-0 text-muted-foreground" />
                <p className="truncate text-sm font-medium">{location.name}</p>
              </div>
              <span className="shrink-0 text-xs text-muted-foreground tabular-nums">
                {t("management.overview.panels.capacity.seats", {
                  count: seatCount,
                })}{" "}
                ·{" "}
                {t("management.overview.panels.capacity.events", {
                  count: eventCount,
                })}
              </span>
            </Link>
          ))
        )}
      </CardContent>
    </Card>
  );
}
