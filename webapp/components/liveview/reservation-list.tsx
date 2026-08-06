"use client";

import { useT } from "@/lib/i18n/hooks";
import type {
  ReservationLiveStatus,
  SupervisorReservationResponseDto,
} from "@/api";
import type { GuestSeatAssignmentDto } from "@/lib/websocket-types";
import { Badge } from "@/components/ui/badge";
import { Trash2 } from "lucide-react";
import { Button } from "@/components/custom-ui/button";

interface ReservationListProps {
  reservations: SupervisorReservationResponseDto[];
  guestAssignments?: GuestSeatAssignmentDto[];
  onRemoveGuestAssignment?: (id: string) => void;
  isLoading?: boolean;
}

export function ReservationList({
  reservations,
  guestAssignments = [],
  onRemoveGuestAssignment,
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

  const filteredReservations = reservations.filter(
    (reservation) => reservation.status === "RESERVED",
  );

  if (filteredReservations.length === 0 && guestAssignments.length === 0) {
    return (
      <div className="flex items-center justify-center py-8 text-muted-foreground">
        <p className="text-sm">{t("liveview.reservations.empty")}</p>
      </div>
    );
  }

  return (
    <div className="space-y-2">
      {guestAssignments.map((guest, index) => (
        <div
          key={guest.id || `guest-${index}`}
          className="p-3 border rounded-lg bg-card hover:bg-muted/50 transition-colors"
        >
          <div className="flex items-center justify-between gap-2">
            <div className="flex-1">
              <p className="font-medium text-sm">
                {guest.seat?.seatNumber} ({guest.seat?.seatRow})
              </p>
              <p className="text-xs text-muted-foreground font-medium">
                {guest.guestName}
              </p>
            </div>
            <div className="flex items-center gap-1.5">
              <Badge
                variant="outline"
                className="border-amber-500 text-amber-600 dark:text-amber-400"
              >
                {t("liveview.guestBadge") || "Gast"}
              </Badge>
              <Badge variant={getLiveStatusBadgeVariant("CHECKED_IN")}>
                {getLiveStatusLabel("CHECKED_IN")}
              </Badge>
              {onRemoveGuestAssignment && guest.id && (
                <Button
                  variant="ghost"
                  size="icon"
                  className="h-7 w-7 text-muted-foreground hover:text-destructive"
                  onClick={() => onRemoveGuestAssignment(guest.id!)}
                  title={t("common.delete")}
                >
                  <Trash2 className="h-3.5 w-3.5" />
                </Button>
              )}
            </div>
          </div>
        </div>
      ))}
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
