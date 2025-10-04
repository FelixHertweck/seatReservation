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
import type { UserReservationResponseDto } from "@/api";
import { useT } from "@/lib/i18n/hooks";
import { useState } from "react";
import { DeleteConfirmationModal } from "./delete-confirmation-modal";

interface ReservationCardProps {
  reservations: UserReservationResponseDto[];
  eventName?: string;
  locationName?: string;
  bookingDeadline?: Date;
  onViewSeats: (reservation: UserReservationResponseDto) => void;
  onDelete: (reservationId: bigint) => void;
}

export function ReservationCard({
  reservations,
  eventName,
  locationName,
  bookingDeadline,
  onViewSeats,
  onDelete,
}: ReservationCardProps) {
  const t = useT();
  const [deleteModalOpen, setDeleteModalOpen] = useState(false);
  const [reservationToDelete, setReservationToDelete] =
    useState<UserReservationResponseDto | null>(null);

  const firstReservation = reservations[0];
  if (!firstReservation) return null;

  const handleDeleteClick = (reservation: UserReservationResponseDto) => {
    setReservationToDelete(reservation);
    setDeleteModalOpen(true);
  };

  const handleConfirmDelete = () => {
    if (reservationToDelete?.id) {
      onDelete(reservationToDelete.id);
    }
    setDeleteModalOpen(false);
    setReservationToDelete(null);
  };

  const handleCancelDelete = () => {
    setDeleteModalOpen(false);
    setReservationToDelete(null);
  };

  return (
    <>
      <Card className="h-full flex flex-col hover:shadow-lg transition-all duration-300 hover:scale-[1.02] group animate-in fade-in slide-in-from-bottom duration-500">
        <CardHeader className="group-hover:bg-accent/5 transition-colors duration-300">
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
            <CardTitle className="line-clamp-2 group-hover:text-primary transition-colors duration-300 leading-tight mb-2">
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
          <div className="flex items-center text-sm text-muted-foreground group-hover:text-foreground transition-colors duration-300">
            <MapPin className="mr-2 h-4 w-4 group-hover:scale-110 transition-transform duration-300" />
            {t("reservationCard.locationLabel")}:{" "}
            {locationName || t("reservationCard.unknownLocation")}
          </div>

          <div className="flex items-center text-sm text-muted-foreground group-hover:text-foreground transition-colors duration-300">
            <Calendar className="mr-2 h-4 w-4 group-hover:scale-110 transition-transform duration-300" />
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

          <div className="space-y-2 mt-4">
            <div className="flex flex-wrap gap-2 h-16 overflow-y-auto content-start">
              {reservations.map((reservation) => (
                <div
                  key={reservation.id?.toString()}
                  className="flex items-center gap-1 bg-secondary text-secondary-foreground px-2 py-1 rounded-md text-sm h-fit"
                >
                  <span>
                    {reservation.seat?.seatNumber +
                      (reservation.seat?.seatRow
                        ? " (" + reservation.seat.seatRow + ")"
                        : "")}
                  </span>
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => handleDeleteClick(reservation)}
                    className="h-4 w-4 p-0 text-destructive hover:text-destructive hover:bg-destructive/10 ml-1"
                  >
                    <Trash2 className="h-3 w-3" />
                  </Button>
                </div>
              ))}
            </div>
          </div>
        </CardContent>

        <CardFooter>
          <Button
            variant="outline"
            size="sm"
            onClick={() => onViewSeats(firstReservation)}
            className="w-full hover:scale-[1.02] transition-all duration-300 active:scale-[0.98]"
          >
            <Eye className="mr-2 h-4 w-4" />
            {reservations.length > 1
              ? t("reservationCard.viewSeatMultipleButton")
              : t("reservationCard.viewSeatButton")}
          </Button>
        </CardFooter>
      </Card>

      <DeleteConfirmationModal
        isOpen={deleteModalOpen}
        onClose={handleCancelDelete}
        onConfirm={handleConfirmDelete}
        seatNumber={
          reservationToDelete?.seat?.seatNumber +
          (reservationToDelete?.seat?.seatRow
            ? " (" + reservationToDelete.seat.seatRow + ")"
            : "")
        }
      />
    </>
  );
}
