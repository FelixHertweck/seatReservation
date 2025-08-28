"use client";

import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { SeatMap } from "@/components/common/seat-map";
import { useState } from "react";
import type { ReservationResponseDto, SeatDto } from "@/api";
import { useT } from "@/lib/i18n/hooks";

interface SeatMapModalProps {
  seats: SeatDto[];
  reservation: ReservationResponseDto;
  eventReservations: ReservationResponseDto[];
  onClose: () => void;
  isLoading: boolean;
}

export function SeatMapModal({
  seats,
  reservation,
  eventReservations,
  onClose,
  isLoading,
}: SeatMapModalProps) {
  const t = useT();

  const [highlightedSeats, setHighlightedSeats] = useState<SeatDto[]>([]);

  const reservedSeats = eventReservations
    .map((res) => res.seat)
    .filter(
      (seat): seat is NonNullable<typeof seat> =>
        seat !== null && seat !== undefined,
    );

  const handleSeatClick = (seat: SeatDto) => {
    setHighlightedSeats((prev) => {
      const isAlreadyHighlighted = prev.some((s) => s.id === seat.id);
      if (isAlreadyHighlighted) {
        return [];
      } else {
        return [seat];
      }
    });
  };

  return (
    <Dialog open onOpenChange={onClose}>
      <DialogContent className="max-w-[95vw] w-[1200px] max-h-[90vh] h-[85vh] flex flex-col">
        <DialogHeader>
          <DialogTitle>
            {eventReservations.length > 1
              ? t("seatMapModal.yourReservedSeatsTitle")
              : t("seatMapModal.yourReservedSeatTitle")}
          </DialogTitle>
          <DialogDescription>
            {isLoading
              ? t("seatMapModal.loadingSeatMap")
              : eventReservations.length > 1
                ? t("seatMapModal.multipleSeatsReserved", {
                    count: eventReservations.length,
                  })
                : t("seatMapModal.singleSeatReserved", {
                    seatNumber: reservation.seat?.seatNumber,
                    x: reservation.seat?.xCoordinate,
                    y: reservation.seat?.yCoordinate,
                  })}
          </DialogDescription>
        </DialogHeader>

        {isLoading ? (
          <div className="flex justify-center items-center h-48">
            <p>{t("seatMapModal.loadingText")}</p>
          </div>
        ) : (
          <div className="flex-1 flex flex-col gap-4 min-h-0 overflow-hidden">
            <div className="flex gap-4 text-sm">
              <div className="flex items-center gap-2 animate-in slide-in-from-left duration-300">
                <div className="w-4 h-4 bg-green-500 rounded transition-all duration-300 hover:scale-110"></div>
                <span>{t("eventReservationModal.available")}</span>
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
            <div className="flex-1 min-h-0 min-w-0">
              <SeatMap
                seats={seats}
                selectedSeats={highlightedSeats}
                onSeatSelect={() => {}} // Read-only
                readonly
              />
            </div>

            <div className="shrink-0 bg-gray-50 p-3 md:p-4 rounded-lg border-t max-h-32 md:max-h-40 overflow-y-auto">
              <div className="flex flex-col gap-2 md:gap-3">
                <h3 className="font-semibold text-base md:text-lg">
                  {t("seatMapModal.yourReservedSeatsSectionTitle")}
                </h3>
                {reservedSeats.length > 0 ? (
                  <div className="flex flex-wrap gap-1.5 md:gap-2">
                    {reservedSeats.map((seat, index) => (
                      <button
                        key={seat.id || index}
                        className={`px-2 py-1.5 md:px-3 md:py-2 text-sm rounded-md border transition-all hover:shadow-md ${
                          highlightedSeats.some((s) => s.id === seat.id)
                            ? "bg-blue-500 text-white border-blue-600 shadow-md"
                            : "bg-white text-gray-700 border-gray-300 hover:bg-gray-50"
                        }`}
                        onClick={() => handleSeatClick(seat)}
                      >
                        {t("seatMapModal.seatNumberButton", {
                          seatNumber: seat.seatNumber,
                        })}
                      </button>
                    ))}
                  </div>
                ) : (
                  <p className="text-gray-500 text-sm">
                    {t("seatMapModal.noSeatsReserved")}
                  </p>
                )}
              </div>
            </div>
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}
