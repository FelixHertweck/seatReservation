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

  const availableSeatsCount = seats.filter(
    (seat) => seat.status !== "RESERVED",
  ).length;
  const totalSeatsCount = seats.length;
  const reservedSeatsCount = seats.filter(
    (seat) => seat.status === "RESERVED",
  ).length;

  return (
    <Dialog open onOpenChange={onClose}>
      <DialogContent className="max-w-7xl max-h-[90vh] h-[85vh] flex flex-col">
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
            <p>Loading...</p>
          </div>
        ) : (
          <div className="flex-1 flex gap-6 min-h-0">
            {/* Left side - Seat Map */}
            <div className="flex-1 min-h-0">
              <SeatMap
                seats={seats}
                selectedSeats={reservedSeat}
                onSeatSelect={() => {}} // Read-only
                readonly
              />
            </div>

            {/* Right side - Selected seats, legend, and statistics */}
            <div className="w-80 flex flex-col space-y-6 bg-gray-50 p-4 rounded-lg">
              {/* Selected Seats Section */}
              <div>
                <h3 className="font-semibold text-lg mb-3">
                  Your Selected Seat
                </h3>
                {reservation.seat ? (
                  <div className="bg-white p-3 rounded border">
                    <div className="flex items-center gap-2 mb-2">
                      <div className="w-4 h-4 bg-blue-500 rounded"></div>
                      <span className="font-medium">
                        Seat {reservation.seat.seatNumber}
                      </span>
                    </div>
                    <div className="text-sm text-gray-600">
                      <p>
                        Position: ({reservation.seat.xCoordinate},{" "}
                        {reservation.seat.yCoordinate})
                      </p>
                    </div>
                  </div>
                ) : (
                  <p className="text-gray-500">No seat selected</p>
                )}
              </div>

              {/* Legend Section */}
              <div>
                <h3 className="font-semibold text-lg mb-3">Legend</h3>
                <div className="space-y-2">
                  <div className="flex items-center gap-3">
                    <div className="w-4 h-4 bg-green-500 rounded"></div>
                    <span>Available Seats</span>
                  </div>
                  <div className="flex items-center gap-3">
                    <div className="w-4 h-4 bg-blue-500 rounded"></div>
                    <span>Your Seat</span>
                  </div>
                  <div className="flex items-center gap-3">
                    <div className="w-4 h-4 bg-red-500 rounded"></div>
                    <span>Reserved Seats</span>
                  </div>
                </div>
              </div>

              {/* Seat Statistics Section */}
              <div>
                <h3 className="font-semibold text-lg mb-3">
                  Seat Availability
                </h3>
                <div className="space-y-3">
                  <div className="bg-white p-3 rounded border">
                    <div className="flex justify-between items-center">
                      <span className="text-green-600 font-medium">
                        Available
                      </span>
                      <span className="font-bold text-green-600">
                        {availableSeatsCount}
                      </span>
                    </div>
                  </div>
                  <div className="bg-white p-3 rounded border">
                    <div className="flex justify-between items-center">
                      <span className="text-red-600 font-medium">Reserved</span>
                      <span className="font-bold text-red-600">
                        {reservedSeatsCount}
                      </span>
                    </div>
                  </div>
                  <div className="bg-white p-3 rounded border">
                    <div className="flex justify-between items-center">
                      <span className="text-gray-600 font-medium">Total</span>
                      <span className="font-bold text-gray-600">
                        {totalSeatsCount}
                      </span>
                    </div>
                  </div>

                  {/* Availability percentage */}
                  <div className="bg-white p-3 rounded border">
                    <div className="text-sm text-gray-600 mb-1">
                      Availability
                    </div>
                    <div className="flex items-center gap-2">
                      <div className="flex-1 bg-gray-200 rounded-full h-2">
                        <div
                          className="bg-green-500 h-2 rounded-full transition-all duration-300"
                          style={{
                            width: `${(availableSeatsCount / totalSeatsCount) * 100}%`,
                          }}
                        ></div>
                      </div>
                      <span className="text-sm font-medium">
                        {Math.round(
                          (availableSeatsCount / totalSeatsCount) * 100,
                        )}
                        %
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}
