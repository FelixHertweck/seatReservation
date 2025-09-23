"use client";

import {
  Calendar,
  MapPin,
  Trash2,
  Eye,
  ChevronDown,
  ChevronUp,
} from "lucide-react";
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
  const [isExpanded, setIsExpanded] = useState(false);
  const [deleteModalOpen, setDeleteModalOpen] = useState(false);
  const [reservationToDelete, setReservationToDelete] =
    useState<UserReservationResponseDto | null>(null);

  const firstReservation = reservations[0];
  if (!firstReservation) return null;

  const maxVisibleSeats = 3;
  const shouldShowExpandButton = reservations.length > maxVisibleSeats;
  const visibleReservations = isExpanded
    ? reservations
    : reservations.slice(0, maxVisibleSeats);

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
      <Card>
        <CardHeader>
          <div className="flex items-start justify-between">
            <CardTitle className="line-clamp-1">
              {eventName || t("reservationCard.unknownEvent")}
            </CardTitle>
            <Badge variant="outline">
              {reservations.length}{" "}
              {reservations.length === 1
                ? t("reservationCard.seatSingular")
                : t("reservationCard.seatPlural")}
            </Badge>
          </div>
          <CardDescription>
            {t("reservationCard.reservedOn")}{" "}
            {firstReservation.reservationDateTime
              ? new Date(
                  firstReservation.reservationDateTime,
                ).toLocaleDateString()
              : t("reservationCard.unknownDate")}
          </CardDescription>
        </CardHeader>

        <CardContent className="space-y-3">
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

          <div className="space-y-2 mt-4">
            <div className="flex flex-wrap gap-2">
              {visibleReservations.map((reservation) => (
                <div
                  key={reservation.id?.toString()}
                  className="flex items-center gap-1 bg-secondary text-secondary-foreground px-2 py-1 rounded-md text-sm"
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

            {shouldShowExpandButton && (
              <Button
                variant="ghost"
                size="sm"
                onClick={() => setIsExpanded(!isExpanded)}
                className="w-full mt-2"
              >
                {isExpanded ? (
                  <>
                    <ChevronUp className="mr-2 h-4 w-4" />
                    {t("reservationCard.showLessButton")}
                  </>
                ) : (
                  <>
                    <ChevronDown className="mr-2 h-4 w-4" />
                    {t("reservationCard.showMoreButton", {
                      count: reservations.length - maxVisibleSeats,
                    })}
                  </>
                )}
              </Button>
            )}
          </div>
        </CardContent>

        <CardFooter className="flex gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={() => onViewSeats(firstReservation)}
          >
            <Eye className="mr-2 h-4 w-4" />
            {t("reservationCard.viewSeatButton")}
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
