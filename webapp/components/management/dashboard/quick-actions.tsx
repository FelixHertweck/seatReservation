import Link from "next/link";
import { Plus, CalendarPlus, BookmarkPlus, Ticket } from "lucide-react";

import { useT } from "@/lib/i18n/hooks";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";

export function QuickActions() {
  const t = useT();

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base">
          {t("management.overview.panels.quickActions.title")}
        </CardTitle>
      </CardHeader>
      <CardContent className="flex flex-col gap-2">
        <Button variant="outline" className="justify-start" asChild>
          <Link href="/management/locations/new">
            <Plus className="h-4 w-4" />
            {t("management.overview.panels.quickActions.newLocation")}
          </Link>
        </Button>
        <Button variant="outline" className="justify-start" asChild>
          <Link href="/management/events">
            <CalendarPlus className="h-4 w-4" />
            {t("management.overview.panels.quickActions.newEvent")}
          </Link>
        </Button>
        <Button variant="outline" className="justify-start" asChild>
          <Link href="/management/reservations">
            <BookmarkPlus className="h-4 w-4" />
            {t("management.overview.panels.quickActions.newReservation")}
          </Link>
        </Button>
        <Button variant="outline" className="justify-start" asChild>
          <Link href="/management/allowances">
            <Ticket className="h-4 w-4" />
            {t("management.overview.panels.quickActions.grantAllowances")}
          </Link>
        </Button>
      </CardContent>
    </Card>
  );
}
