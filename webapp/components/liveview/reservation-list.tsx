"use client";

import { useT } from "@/lib/i18n/hooks";
import type {
  ReservationLiveStatus,
  SupervisorReservationResponseDto,
} from "@/api";
import { Badge } from "@/components/ui/badge";

interface ReservationListProps {
  reservations: SupervisorReservationResponseDto[];
  isLoading?: boolean;
}

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

  return (
    <div className="space-y-2">
      {filteredReservations.map((reservation, index) => (
        <div
          key={reservation.id?.toString() || `reservation-${index}`}
          className="p-3 border rounded-lg bg-card hover:bg-muted/50 transition-colors"
        >
          <div className="flex items-center justify-between gap-2">
            <div className="flex-1">
              <p className="font-medium text-sm">
                {reservation.seat?.seatNumber} ({reservation.seat?.seatRow})
              </p>
              <p className="text-xs text-muted-foreground">
                {reservation.username || `User ${reservation.userId}`}
              </p>
            </div>
            {reservation.liveStatus && (
              <Badge
                variant={getLiveStatusBadgeVariant(reservation.liveStatus)}
              >
                {getLiveStatusLabel(reservation.liveStatus)}
              </Badge>
            )}
          </div>
        </div>
      ))}
    </div>
  );
}
