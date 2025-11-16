"use client";

import { Calendar, MapPin, Trash2, Eye, QrCode } from "lucide-react";
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
import { Checkbox } from "@/components/ui/checkbox";
import type { UserReservationResponseDto } from "@/api";
import { useT } from "@/lib/i18n/hooks";
import { useState } from "react";
import { DeleteConfirmationModal } from "./delete-confirmation-modal";
import { QRCodeModal } from "./qr-code-modal";
import { useAuth } from "@/hooks/use-auth";

interface ReservationCardProps {
  reservations: UserReservationResponseDto[];
  eventName?: string;
  locationName?: string;
  bookingDeadline?: Date;
  onViewSeats: (reservation: UserReservationResponseDto) => void;
  onDelete: (reservationIds: bigint[]) => void;
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
  const { user } = useAuth();
  const [deleteModalOpen, setDeleteModalOpen] = useState(false);
  const [qrCodeModalOpen, setQrCodeModalOpen] = useState(false);
  const [selectedReservations, setSelectedReservations] = useState<Set<bigint>>(
    new Set(),
  );

  const firstReservation = reservations[0];
  if (!firstReservation) return null;

  const allSelected = reservations.every((r) =>
    selectedReservations.has(r.id!),
  );

  const toggleReservationSelection = (reservationId: bigint) => {
    setSelectedReservations((prev) => {
      const newSet = new Set(prev);
      if (newSet.has(reservationId)) {
        newSet.delete(reservationId);
      } else {
        newSet.add(reservationId);
      }
      return newSet;
    });
  };

  const toggleSelectAll = () => {
    if (allSelected) {
      setSelectedReservations(new Set());
    } else {
      setSelectedReservations(new Set(reservations.map((r) => r.id!)));
    }
  };

  const handleDeleteSelected = () => {
    if (selectedReservations.size > 0) {
      setDeleteModalOpen(true);
    }
  };

  const handleConfirmDelete = async () => {
    const toDelete = Array.from(selectedReservations);
    await onDelete(toDelete);
    setDeleteModalOpen(false);
    setSelectedReservations(new Set());
  };

  const handleCancelDelete = () => {
    setDeleteModalOpen(false);
  };

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

          <div className="space-y-2 mt-4">
            <div className="flex items-center justify-between mb-2 min-h-[32px]">
              <div className="flex items-center space-x-2">
                <Checkbox
                  id={`select-all-${firstReservation.eventId}`}
                  checked={allSelected}
                  onCheckedChange={toggleSelectAll}
                />
                <label
                  htmlFor={`select-all-${firstReservation.eventId}`}
                  className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70 cursor-pointer"
                >
                  {t("reservationCard.selectAll")}
                </label>
              </div>
              <Button
                variant="destructive"
                size="sm"
                onClick={handleDeleteSelected}
                disabled={selectedReservations.size === 0}
                className="h-8 text-xs px-2"
              >
                <Trash2 className="mr-1 h-3 w-3" />
                {selectedReservations.size > 0
                  ? selectedReservations.size
                  : "0"}
              </Button>
            </div>
            <div className="flex flex-wrap gap-2 h-16 overflow-y-auto content-start">
              {reservations.map((reservation) => (
                <div
                  key={reservation.id?.toString()}
                  className={`flex items-center gap-1 px-2 py-1 rounded-md text-sm h-fit transition-colors bg-secondary text-secondary-foreground `}
                >
                  <Checkbox
                    checked={selectedReservations.has(reservation.id!)}
                    onCheckedChange={() =>
                      toggleReservationSelection(reservation.id!)
                    }
                    className="mr-1"
                  />
                  <span>
                    {reservation.seat?.seatNumber +
                      (reservation.seat?.seatRow
                        ? " (" + reservation.seat.seatRow + ")"
                        : "")}
                  </span>
                </div>
              ))}
            </div>
          </div>
        </CardContent>

        <CardFooter className="flex gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={() => onViewSeats(firstReservation)}
            className="flex-1 hover:scale-[1.02] transition-all duration-300 active:scale-[0.98]"
          >
            <Eye className="mr-2 h-4 w-4" />
            {reservations.length > 1
              ? t("reservationCard.viewSeatMultipleButton")
              : t("reservationCard.viewSeatButton")}
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
        </CardFooter>
      </Card>

      <QRCodeModal
        isOpen={qrCodeModalOpen}
        onClose={() => setQrCodeModalOpen(false)}
        reservations={reservations}
        eventName={eventName}
        userId={user?.id}
      />

      <DeleteConfirmationModal
        isOpen={deleteModalOpen}
        onClose={handleCancelDelete}
        onConfirm={handleConfirmDelete}
        selectedCount={selectedReservations.size}
        seats={reservations
          .filter((r) => selectedReservations.has(r.id!))
          .map(
            (r) =>
              r.seat?.seatNumber +
              (r.seat?.seatRow ? " (" + r.seat.seatRow + ")" : ""),
          )}
      />
    </>
  );
}
