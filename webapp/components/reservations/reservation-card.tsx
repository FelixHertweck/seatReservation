"use client";

import { Calendar, MapPin, Trash2, Eye } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import type { ReservationResponseDto } from "@/api";
import { t } from "i18next";

interface ReservationCardProps {
  reservation: ReservationResponseDto;
  onViewSeats: () => void;
  onDelete: () => void;
}

export function ReservationCard({
  reservation,
  onViewSeats,
  onDelete,
}: ReservationCardProps) {
  return (
    <Card>
      <CardHeader>
        <div className="flex items-start justify-between">
          <CardTitle className="line-clamp-1">
            {t("reservationCard.eventReservationTitle")}
          </CardTitle>
          <Badge variant="outline">
            {t("reservationCard.seatBadge", {
              seatNumber: reservation.seat?.seatNumber,
            })}
          </Badge>
        </div>
        <CardDescription>
          {t("reservationCard.reservedOn")}{" "}
          {reservation.reservationDateTime
            ? new Date(reservation.reservationDateTime).toLocaleDateString()
            : t("reservationCard.unknownDate")}
        </CardDescription>
      </CardHeader>

      <CardContent className="space-y-3">
        <div className="flex items-center text-sm text-muted-foreground">
          <MapPin className="mr-2 h-4 w-4" />
          {t("reservationCard.locationLabel")}:{" "}
          {reservation.seat?.locationId?.toString() ||
            t("reservationCard.unknownLocation")}
        </div>

        <div className="flex items-center text-sm text-muted-foreground">
          <Calendar className="mr-2 h-4 w-4" />
          {t("reservationCard.positionLabel")}: {t("reservationCard.rowLabel")}{" "}
          {reservation.seat?.yCoordinate}, {t("reservationCard.seatLabel")}{" "}
          {reservation.seat?.xCoordinate}
        </div>
      </CardContent>

      <CardFooter className="flex gap-2">
        <Button variant="outline" size="sm" onClick={onViewSeats}>
          <Eye className="mr-2 h-4 w-4" />
          {t("reservationCard.viewSeatButton")}
        </Button>
        <Button variant="destructive" size="sm" onClick={onDelete}>
          <Trash2 className="mr-2 h-4 w-4" />
          {t("reservationCard.cancelButton")}
        </Button>
      </CardFooter>
    </Card>
  );
}
