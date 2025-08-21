"use client";

import { useState, useEffect, useMemo } from "react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { SeatMap } from "@/components/common/seat-map";
import { Badge } from "@/components/ui/badge";
import type { EventResponseDto, ReservationResponseDto, SeatDto } from "@/api";
import { useT } from "@/lib/i18n/hooks";

interface EventReservationModalProps {
  event: EventResponseDto;
  userReservations: ReservationResponseDto[];
  initialSeatId: bigint;
  onClose: () => void;
  onReserve: (
    eventId: bigint,
    seatIds: bigint[],
  ) => Promise<ReservationResponseDto[]>;
}

export function EventReservationModal({
  event,
  userReservations,
  initialSeatId,
  onClose,
  onReserve,
}: EventReservationModalProps) {
  const t = useT();

  const [selectedSeats, setSelectedSeats] = useState<SeatDto[]>([]);
  const [isLoading, setIsLoading] = useState(false);

  const seats: SeatDto[] = useMemo(
    () => event.location?.seats ?? [],
    [event.location?.seats],
  );

  useEffect(() => {
    if (initialSeatId) {
      const initialSeat = seats.find((seat) => seat.id === initialSeatId);
      if (initialSeat && !initialSeat.status) {
        setSelectedSeats([initialSeat]);
      }
    }
  }, [initialSeatId, seats]);

  const userReservedSeats = userReservations
    .filter((reservation) => reservation.eventId === event.id)
    .map((reservation) => reservation.seat)
    .filter((seat): seat is SeatDto => seat !== null && seat !== undefined);

  const handleSeatSelect = (seat: SeatDto) => {
    if (seat.status) return; // Can't select reserved or blocked seats

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
      <DialogContent className="w-[95vw] max-w-none max-h-[90vh] h-[85vh] flex flex-col animate-in fade-in zoom-in duration-300">
        <DialogHeader className="animate-in slide-in-from-top duration-300">
          <DialogTitle className="text-xl font-bold">
            {t("eventReservationModal.title", { eventName: event.name })}
          </DialogTitle>
          <DialogDescription>
            {t("eventReservationModal.description", {
              availableSeats: event.reservationsAllowed,
            })}
          </DialogDescription>
        </DialogHeader>

        <div className="flex-1 flex flex-col animate-in slide-in-from-bottom duration-500 min-h-0">
          <div className="flex gap-4 text-sm">
            <div className="flex items-center gap-2 animate-in slide-in-from-left duration-300">
              <div className="w-4 h-4 bg-green-500 rounded transition-all duration-300 hover:scale-110"></div>
              <span>{t("eventReservationModal.available")}</span>
            </div>
            <div
              className="flex items-center gap-2 animate-in slide-in-from-left duration-300"
              style={{ animationDelay: "100ms" }}
            >
              <div className="w-4 h-4 bg-blue-500 rounded transition-all duration-300 hover:scale-110"></div>
              <span>{t("eventReservationModal.selected")}</span>
            </div>
            <div
              className="flex items-center gap-2 animate-in slide-in-from-left duration-300"
              style={{ animationDelay: "150ms" }}
            >
              <div className="w-4 h-4 bg-yellow-500 rounded transition-all duration-300 hover:scale-110"></div>
              <span>{t("eventReservationModal.myReserved")}</span>
            </div>
            <div
              className="flex items-center gap-2 animate-in slide-in-from-left duration-300"
              style={{ animationDelay: "200ms" }}
            >
              <div className="w-4 h-4 bg-red-500 rounded transition-all duration-300 hover:scale-110"></div>
              <span>{t("eventReservationModal.reserved")}</span>
            </div>
            <div
              className="flex items-center gap-2 animate-in slide-in-from-left duration-300"
              style={{ animationDelay: "300ms" }}
            >
              <div className="w-4 h-4 bg-gray-500 rounded transition-all duration-300 hover:scale-110"></div>
              <span>{t("eventReservationModal.blocked")}</span>
            </div>
          </div>

          <div className="flex-1 min-h-0">
            <SeatMap
              seats={seats}
              selectedSeats={selectedSeats}
              userReservedSeats={userReservedSeats}
              onSeatSelect={handleSeatSelect}
            />
          </div>

          {selectedSeats.length > 0 && (
            <div className="m-0 animate-in slide-in-from-bottom duration-300 max-h-15 md:max-h-24 overflow-y-auto">
              <h4 className="font-medium text-sm md:text-base">
                {t("eventReservationModal.selectedSeatsTitle")}
              </h4>
              <div className="flex flex-wrap gap-1.5 md:gap-2">
                {selectedSeats.map((seat, index) => (
                  <Badge
                    key={seat.id?.toString()}
                    variant="outline"
                    className="animate-in zoom-in duration-300 hover:scale-105 transition-transform text-xs md:text-sm px-2 py-1"
                    style={{ animationDelay: `${index * 50}ms` }}
                  >
                    {seat.seatNumber}
                  </Badge>
                ))}
              </div>
            </div>
          )}

          <div className="flex justify-end gap-2 animate-in slide-in-from-bottom duration-300 pt-2">
            <Button
              variant="outline"
              onClick={onClose}
              className="hover:scale-[1.02] transition-all duration-300 active:scale-[0.98] bg-transparent text-sm md:text-base px-3 py-2"
            >
              {t("eventReservationModal.cancelButton")}
            </Button>
            <Button
              onClick={handleReserve}
              disabled={selectedSeats.length === 0 || isLoading}
              className="hover:scale-[1.02] transition-all duration-300 active:scale-[0.98] text-sm md:text-base px-3 py-2"
            >
              {isLoading
                ? t("eventReservationModal.reservingButton")
                : t("eventReservationModal.reserveSeatsButton", {
                    count: selectedSeats.length,
                  })}
            </Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}
