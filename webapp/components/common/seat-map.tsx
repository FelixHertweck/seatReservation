"use client";

import { cn } from "@/lib/utils";
import type { SeatDto } from "@/api";

interface SeatMapProps {
  seats: SeatDto[];
  selectedSeats: SeatDto[];
  onSeatSelect: (seat: SeatDto) => void;
  readonly?: boolean;
}

export function SeatMap({
  seats,
  selectedSeats,
  onSeatSelect,
  readonly = false,
}: SeatMapProps) {
  // Calculate grid dimensions
  const maxX = Math.max(...seats.map((s) => s.xCoordinate || 0));
  const maxY = Math.max(...seats.map((s) => s.yCoordinate || 0));

  const getSeatAtPosition = (x: number, y: number) => {
    return seats.find(
      (seat) => seat.xCoordinate === x && seat.yCoordinate === y,
    );
  };

  const getSeatColor = (seat: SeatDto | undefined) => {
    if (!seat) return "transparent";

    const isSelected = selectedSeats.some((s) => s.id === seat.id);
    if (isSelected) return "bg-blue-500 hover:bg-blue-600";

    switch (seat.status) {
      case "RESERVED":
        return "bg-red-500";
      case "BLOCKED":
        return "bg-gray-500";
      default:
        return "bg-green-500 hover:bg-green-600";
    }
  };

  const canSelectSeat = (seat: SeatDto | undefined) => {
    if (!seat || readonly) return false;
    return !seat.status; // Can only select seats without status (available)
  };

  return (
    <div className="p-4 bg-gray-50 rounded-lg">
      <div className="text-center mb-4 text-sm font-medium text-gray-600">
        STAGE
      </div>
      <div
        className="grid gap-2 mx-auto"
        style={{
          gridTemplateColumns: `repeat(${maxX}, 1fr)`,
          maxWidth: `${maxX * 40}px`,
        }}
      >
        {Array.from({ length: maxY }, (_, y) =>
          Array.from({ length: maxX }, (_, x) => {
            const seat = getSeatAtPosition(x + 1, y + 1);
            const seatColor = getSeatColor(seat);
            const clickable = canSelectSeat(seat);

            return (
              <div
                key={`${x}-${y}`}
                className={cn(
                  "w-8 h-8 rounded border-2 border-gray-300 flex items-center justify-center text-xs font-medium transition-colors",
                  seatColor,
                  clickable && "cursor-pointer",
                  !seat && "border-transparent",
                )}
                onClick={() => seat && clickable && onSeatSelect(seat)}
                title={seat ? `Seat ${seat.seatNumber}` : undefined}
              >
                {seat && (
                  <span className="text-white text-xs">
                    {seat.seatNumber?.slice(-1)}
                  </span>
                )}
              </div>
            );
          }),
        )}
      </div>
    </div>
  );
}
