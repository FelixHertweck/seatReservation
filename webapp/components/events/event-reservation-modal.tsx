"use client";

import { useState, useMemo } from "react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { SeatMap } from "@/components/common/seat-map";
import type {
  UserEventResponseDto,
  UserReservationResponseDto,
  SeatDto,
  UserEventLocationResponseDto,
} from "@/api";
import { useT } from "@/lib/i18n/hooks";
import { findSeatStatus } from "@/lib/reservationSeat";

interface EventReservationModalProps {
  event: UserEventResponseDto;
  location: UserEventLocationResponseDto | null;
  userReservations: UserReservationResponseDto[];
  onClose: () => void;
  onReserve: (
    eventId: bigint,
    seatIds: bigint[],
  ) => Promise<UserReservationResponseDto[]>;
}

export function EventReservationModal({
  event,
  location,
  userReservations,
  onClose,
  onReserve,
}: EventReservationModalProps) {
  const t = useT();

  const [selectedSeats, setSelectedSeats] = useState<SeatDto[]>([]);
  const [isLoading, setIsLoading] = useState(false);

  const seats: SeatDto[] = useMemo(
    () => location?.seats ?? [],
    [location?.seats],
  );

  const userReservedSeats = userReservations
    .filter((reservation) => reservation.eventId === event.id)
    .map((reservation) => reservation.seat)
    .filter((seat): seat is SeatDto => seat !== null && seat !== undefined);

  const handleSeatSelect = (seat: SeatDto) => {
    const seatStatus = findSeatStatus(seat.id!, event.seatStatuses);

    if (seatStatus) return; // Can't select reserved or blocked seats

    setSelectedSeats((prev) => {
      const isSelected = prev.some((s) => s.id === seat.id);
      if (isSelected) {
        return prev.filter((s) => s.id !== seat.id);
      } else {
        const availableSeats = event.reservationsAllowed ?? 0;
        if (prev.length >= availableSeats) return prev;
        return [...prev, seat];
      }
    });
  };

  const handleReserve = async () => {
    if (!event.id || selectedSeats.length === 0) return;

    setIsLoading(true);
    try {
      const seatIds = selectedSeats
        .map((seat) => seat.id!)
        .filter((id) => id !== undefined);
      await onReserve(event.id, seatIds);
      onClose();
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Dialog open onOpenChange={onClose}>
      <DialogContent
        className="w-[95vw] max-w-none max-h-[90vh] h-[85vh] flex flex-col"
        onInteractOutside={(e) => e.preventDefault()}
      >
        <DialogHeader>
          <DialogTitle className="text-xl font-bold">
            {t("eventReservationModal.title", { eventName: event.name })}
          </DialogTitle>
          <DialogDescription>
            {t("eventReservationModal.description", {
              availableSeats: event.reservationsAllowed,
            })}
          </DialogDescription>
        </DialogHeader>

        <div className="flex-1 flex flex-col min-h-0">
          <div className="flex flex-wrap gap-2 md:gap-4 text-sm border-b pb-1">
            <div className="flex items-center gap-2">
              <div className="w-4 h-4 bg-green-500 rounded"></div>
              <span>{t("eventReservationModal.available")}</span>
            </div>
            <div className="flex items-center gap-2">
              <div className="w-4 h-4 bg-blue-500 rounded"></div>
              <span>{t("eventReservationModal.selected")}</span>
            </div>
            <div className="flex items-center gap-2">
              <div className="w-4 h-4 bg-yellow-500 rounded"></div>
              <span>{t("eventReservationModal.myReserved")}</span>
            </div>
            <div className="flex items-center gap-2">
              <div className="w-4 h-4 bg-red-500 rounded"></div>
              <span>{t("eventReservationModal.reserved")}</span>
            </div>
            <div className="flex items-center gap-2">
              <div className="w-4 h-4 bg-gray-500 rounded"></div>
              <span>{t("eventReservationModal.blocked")}</span>
            </div>
          </div>

          <div className="flex-1 min-h-0">
            <SeatMap
              seats={seats}
              seatStatuses={event.seatStatuses ?? []}
              markers={location?.markers ?? []}
              selectedSeats={selectedSeats}
              userReservedSeats={userReservedSeats}
              onSeatSelect={handleSeatSelect}
            />
          </div>

          <div className="flex justify-between items-center gap-2 pt-2 border-t">
            <div className="flex-1 min-w-0">
              {selectedSeats.length > 0 && (
                <div className="overflow-x-auto scrollbar-thin">
                  <div className="flex gap-1.5 md:gap-2 pb-0">
                    {selectedSeats.map((seat) => (
                      <button
                        key={seat.id?.toString()}
                        className="flex-shrink-0 px-2 py-1.5 md:px-3 md:py-2 text-sm rounded-md border bg-seatmap border rounded shadow-xs hover:bg-secondary"
                      >
                        {seat.seatNumber +
                          (seat.seatRow ? " (" + seat.seatRow + ")" : "")}
                      </button>
                    ))}
                  </div>
                </div>
              )}
            </div>
            <div className="flex gap-2 flex-shrink-0">
              <Button
                variant="outline"
                onClick={onClose}
                className="bg-transparent text-sm md:text-base px-3 py-2"
              >
                {t("eventReservationModal.cancelButton")}
              </Button>
              <Button
                onClick={handleReserve}
                disabled={selectedSeats.length === 0 || isLoading}
                className="text-sm md:text-base px-3 py-2"
              >
                {isLoading
                  ? t("eventReservationModal.reservingButton")
                  : selectedSeats.length === 1
                    ? t("eventReservationModal.reserveSeatButton")
                    : t("eventReservationModal.reserveSeatsButton", {
                        count: selectedSeats.length,
                      })}
              </Button>
            </div>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}
