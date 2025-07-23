import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { SeatMap } from "@/components/common/seat-map";
import type { ReservationResponseDto, SeatDto } from "@/api";

interface SeatMapModalProps {
  seats: SeatDto[];
  reservation: ReservationResponseDto;
  onClose: () => void;
  isLoading: boolean;
}

export function SeatMapModal({
  seats,
  reservation,
  onClose,
  isLoading,
}: SeatMapModalProps) {
  const reservedSeat = reservation.seat ? [reservation.seat] : [];

  return (
    <Dialog open onOpenChange={onClose}>
      <DialogContent className="max-w-2xl">
        <DialogHeader>
          <DialogTitle>Your Reserved Seat</DialogTitle>
          <DialogDescription>
            {isLoading
              ? "Loading seat map..."
              : `Seat ${reservation.seat?.seatNumber} - Position (${reservation.seat?.xCoordinate}, ${reservation.seat?.yCoordinate})`}
          </DialogDescription>
        </DialogHeader>

        {isLoading ? (
          <div className="flex justify-center items-center h-48">
            <p>Loading...</p>{" "}
            {/* You can replace this with a proper spinner/loader component */}
          </div>
        ) : (
          <div className="space-y-4">
            <div className="flex gap-4 text-sm">
              <div className="flex items-center gap-2">
                <div className="w-4 h-4 bg-green-500 rounded"></div>
                <span>Available</span>
              </div>
              <div className="flex items-center gap-2">
                <div className="w-4 h-4 bg-blue-500 rounded"></div>
                <span>Your Seat</span>
              </div>
              <div className="flex items-center gap-2">
                <div className="w-4 h-4 bg-red-500 rounded"></div>
                <span>Reserved</span>
              </div>
            </div>

            <SeatMap
              seats={seats}
              selectedSeats={reservedSeat}
              onSeatSelect={() => {}} // Read-only
              readonly
            />
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}
