"use client";

import { useState } from "react";
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

interface EventReservationModalProps {
  event: EventResponseDto;
  onClose: () => void;
  onReserve: (
    eventId: bigint,
    seatIds: bigint[],
  ) => Promise<ReservationResponseDto[]>;
}

export function EventReservationModal({
  event,
  onClose,
  onReserve,
}: EventReservationModalProps) {
  const [selectedSeats, setSelectedSeats] = useState<SeatDto[]>([]);
  const [isLoading, setIsLoading] = useState(false);

  const seats: SeatDto[] = event.location?.seats ?? [];

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
      <DialogContent className="max-w-6xl max-h-[90vh] h-[85vh] flex flex-col animate-in fade-in zoom-in duration-300">
        <DialogHeader className="animate-in slide-in-from-top duration-300">
          <DialogTitle className="text-xl font-bold">
            Reserve Seats - {event.name}
          </DialogTitle>
          <DialogDescription>
            Select your seats for this event. Available:{" "}
            {event.reservationsAllowed}
          </DialogDescription>
        </DialogHeader>

        <div className="flex-1 flex flex-col space-y-4 animate-in slide-in-from-bottom duration-500 min-h-0">
          <div className="flex gap-4 text-sm">
            <div className="flex items-center gap-2 animate-in slide-in-from-left duration-300">
              <div className="w-4 h-4 bg-green-500 rounded transition-all duration-300 hover:scale-110"></div>
              <span>Available</span>
            </div>
            <div
              className="flex items-center gap-2 animate-in slide-in-from-left duration-300"
              style={{ animationDelay: "100ms" }}
            >
              <div className="w-4 h-4 bg-blue-500 rounded transition-all duration-300 hover:scale-110"></div>
              <span>Selected</span>
            </div>
            <div
              className="flex items-center gap-2 animate-in slide-in-from-left duration-300"
              style={{ animationDelay: "200ms" }}
            >
              <div className="w-4 h-4 bg-red-500 rounded transition-all duration-300 hover:scale-110"></div>
              <span>Reserved</span>
            </div>
            <div
              className="flex items-center gap-2 animate-in slide-in-from-left duration-300"
              style={{ animationDelay: "300ms" }}
            >
              <div className="w-4 h-4 bg-gray-500 rounded transition-all duration-300 hover:scale-110"></div>
              <span>Blocked</span>
            </div>
          </div>

          <div className="flex-1 min-h-0">
            <SeatMap
              seats={seats}
              selectedSeats={selectedSeats}
              onSeatSelect={handleSeatSelect}
            />
          </div>

          {selectedSeats.length > 0 && (
            <div className="space-y-2 animate-in slide-in-from-bottom duration-300">
              <h4 className="font-medium">Selected Seats:</h4>
              <div className="flex flex-wrap gap-2">
                {selectedSeats.map((seat, index) => (
                  <Badge
                    key={seat.id?.toString()}
                    variant="outline"
                    className="animate-in zoom-in duration-300 hover:scale-105 transition-transform"
                    style={{ animationDelay: `${index * 50}ms` }}
                  >
                    {seat.seatNumber}
                  </Badge>
                ))}
              </div>
            </div>
          )}

          <div className="flex justify-end gap-2 animate-in slide-in-from-bottom duration-300">
            <Button
              variant="outline"
              onClick={onClose}
              className="hover:scale-[1.02] transition-all duration-300 active:scale-[0.98] bg-transparent"
            >
              Cancel
            </Button>
            <Button
              onClick={handleReserve}
              disabled={selectedSeats.length === 0 || isLoading}
              className="hover:scale-[1.02] transition-all duration-300 active:scale-[0.98]"
            >
              {isLoading
                ? "Reserving..."
                : `Reserve ${selectedSeats.length} Seat(s)`}
            </Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}
