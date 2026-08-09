"use client";

import { useT } from "@/lib/i18n/hooks";
import type {
  ReservationLiveStatus,
  SupervisorReservationResponseDto,
} from "@/api";
import { Badge } from "@/components/ui/badge";
import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionTrigger,
} from "@/components/ui/accordion";

interface ReservationListProps {
  reservations: SupervisorReservationResponseDto[];
  isLoading?: boolean;
}

const displayName = (reservation: SupervisorReservationResponseDto): string =>
  reservation.guestName || reservation.username || `User ${reservation.userId}`;

export function ReservationList({
  reservations,
  isLoading = false,
}: ReservationListProps) {
  const t = useT();

  const getLiveStatusBadgeVariant = (
    liveStatus: ReservationLiveStatus | undefined,
  ): "default" | "secondary" | "destructive" | "outline" => {
    switch (liveStatus) {
      case "CHECKED_IN":
        return "default";
      case "CANCELLED":
        return "secondary";
      case "NO_SHOW":
        return "destructive";
      default:
        return "outline";
    }
  };

  const getLiveStatusLabel = (
    liveStatus: ReservationLiveStatus | undefined,
  ): string => {
    switch (liveStatus) {
      case "CHECKED_IN":
        return t("seatStatus.checkedIn");
      case "CANCELLED":
        return t("seatStatus.cancelled");
      case "NO_SHOW":
        return t("seatStatus.noShow");
      default:
        return liveStatus || "unknown";
    }
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-8 text-muted-foreground">
        <p className="text-sm">{t("common.loading")}</p>
      </div>
    );
  }

  if (reservations.length === 0) {
    return (
      <div className="flex items-center justify-center py-8 text-muted-foreground">
        <p className="text-sm">{t("liveview.reservations.empty")}</p>
      </div>
    );
  }

  const filteredReservations = reservations.filter(
    (reservation) => reservation.status === "RESERVED",
  );

  const groups = new Map<string, SupervisorReservationResponseDto[]>();
  for (const reservation of filteredReservations) {
    const key = displayName(reservation);
    const existing = groups.get(key);
    if (existing) {
      existing.push(reservation);
    } else {
      groups.set(key, [reservation]);
    }
  }
  const sortedGroupKeys = [...groups.keys()].sort((a, b) =>
    a.localeCompare(b, undefined, { sensitivity: "base" }),
  );

  return (
    <Accordion type="multiple" defaultValue={sortedGroupKeys}>
      {sortedGroupKeys.map((groupKey) => {
        const groupReservations = groups.get(groupKey)!;
        return (
          <AccordionItem key={groupKey} value={groupKey}>
            <AccordionTrigger className="text-sm">
              <span className="flex items-center gap-2">
                {groupKey}
                <Badge variant="secondary">{groupReservations.length}</Badge>
              </span>
            </AccordionTrigger>
            <AccordionContent>
              <div className="space-y-2">
                {groupReservations.map((reservation, index) => (
                  <div
                    key={reservation.id?.toString() || `reservation-${index}`}
                    className="p-3 border rounded-lg bg-card hover:bg-muted/50 transition-colors"
                  >
                    <div className="flex items-center justify-between gap-2">
                      <p className="font-medium text-sm">
                        {reservation.seat?.seatNumber} (
                        {reservation.seat?.seatRow})
                      </p>
                      {reservation.liveStatus && (
                        <Badge
                          variant={getLiveStatusBadgeVariant(
                            reservation.liveStatus,
                          )}
                        >
                          {getLiveStatusLabel(reservation.liveStatus)}
                        </Badge>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </AccordionContent>
          </AccordionItem>
        );
      })}
    </Accordion>
  );
}
