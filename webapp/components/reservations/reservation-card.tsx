"use client";

import Link from "next/link";
import { Calendar, MapPin, Pencil, QrCode, CalendarDays } from "lucide-react";
import { Button } from "@/components/custom-ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import type { UserReservationResponseDto } from "@/api";
import { useT } from "@/lib/i18n/hooks";
import { useState } from "react";
import { QRCodeModal } from "./qr-code-modal";
import { useAuth } from "@/hooks/use-auth";

interface ReservationCardProps {
  reservations: UserReservationResponseDto[];
  eventName?: string;
  locationName?: string;
  bookingDeadline?: Date;
  onViewSeats: (reservation: UserReservationResponseDto) => void;
  viewEventHref?: string;
}

export function ReservationCard({
  reservations,
  eventName,
  locationName,
  bookingDeadline,
  onViewSeats,
  viewEventHref,
}: ReservationCardProps) {
  const t = useT();
  const { user } = useAuth();
  const [qrCodeModalOpen, setQrCodeModalOpen] = useState(false);

  const firstReservation = reservations[0];

  if (!firstReservation) return null;

  return (
    <>
      <Card className="h-full flex flex-col hover:shadow-lg transition-all duration-300 hover:scale-[1.02] group animate-in fade-in slide-in-from-bottom duration-500">
        <CardHeader>
          <div className="flex items-start justify-between gap-2 mb-3">
            <Badge
              variant="outline"
              className="animate-in zoom-in duration-300 group-hover:scale-105 transition-transform shrink-0 whitespace-nowrap"
            >
              {reservations.length}{" "}
              {reservations.length === 1
                ? t("reservationCard.seatSingular")
                : t("reservationCard.seatPlural")}
            </Badge>
          </div>
          <div className="flex flex-col">
            <CardTitle className="line-clamp-2 leading-tight mb-2">
              {eventName || t("reservationCard.unknownEvent")}
            </CardTitle>
            <CardDescription className="line-clamp-2 text-sm leading-relaxed flex-1">
              {t("reservationCard.reservedOn")}{" "}
              {firstReservation.reservationDateTime
                ? new Date(
                    firstReservation.reservationDateTime,
                  ).toLocaleDateString()
                : t("reservationCard.unknownDate")}
            </CardDescription>
          </div>
        </CardHeader>

        <CardContent className="flex-1 space-y-2">
          <div className="flex items-center text-sm text-muted-foreground">
            <MapPin className="mr-2 h-4 w-4" />
            {t("reservationCard.locationLabel")}:{" "}
            {locationName || t("reservationCard.unknownLocation")}
          </div>

          <div className="flex items-center text-sm text-muted-foreground">
            <Calendar className="mr-2 h-4 w-4" />
            {t("reservationCard.bookingUntil")}:{" "}
            {bookingDeadline
              ? bookingDeadline.toLocaleDateString() +
                " " +
                bookingDeadline.toLocaleTimeString([], {
                  hour: "2-digit",
                  minute: "2-digit",
                })
              : t("reservationCard.unknownDate")}
          </div>
        </CardContent>

        <CardFooter className="flex flex-wrap gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={() => onViewSeats(firstReservation)}
            className="flex-1 hover:scale-[1.02] transition-all duration-300 active:scale-[0.98]"
          >
            <Pencil className="mr-2 h-4 w-4" />
            {t("reservationCard.manageSeatsButton")}
          </Button>
          <Button
            variant="outline"
            size="sm"
            onClick={() => setQrCodeModalOpen(true)}
            className="flex-1 hover:scale-[1.02] transition-all duration-300 active:scale-[0.98]"
          >
            <QrCode className="mr-2 h-4 w-4" />
            {t("reservationCard.showQRCodeButton")}
          </Button>
          {viewEventHref && (
            <Button
              variant="outline"
              size="sm"
              asChild
              className="flex-1 hover:scale-[1.02] transition-all duration-300 active:scale-[0.98]"
            >
              <Link href={viewEventHref}>
                <CalendarDays className="mr-2 h-4 w-4" />
                {t("reservationsPage.viewEventButton")}
              </Link>
            </Button>
          )}
        </CardFooter>
      </Card>

      <QRCodeModal
        isOpen={qrCodeModalOpen}
        onClose={() => setQrCodeModalOpen(false)}
        reservations={reservations}
        eventName={eventName}
        userId={user?.id}
      />
    </>
  );
}
