"use client";

import { useState, useMemo, useRef, useCallback, useEffect } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { SeatMap } from "@/components/common/seat-map";
import SeatmapLegend from "@/components/common/seatmap-legend";
import type {
  UserEventResponseDto,
  UserReservationResponseDto,
  SeatDto,
  UserEventLocationResponseDto,
} from "@/api";
import { getApiUserEventsQueryKey } from "@/api/@tanstack/react-query.gen";
import { useT } from "@/lib/i18n/hooks";
import { findSeatStatus } from "@/lib/reservationSeat";
import { useSeatCart } from "@/hooks/use-seat-cart";

interface EventReservationModalProps {
  event: UserEventResponseDto;
  location: UserEventLocationResponseDto | null;
  userReservations: UserReservationResponseDto[];
  onClose: () => void;
  onReserve: (
    eventId: string,
    seatIds: string[],
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
  const queryClient = useQueryClient();
  const { addSeatToCart, removeSeatFromCart } = useSeatCart();

  const [selectedSeats, setSelectedSeats] = useState<SeatDto[]>([]);
  const [isLoading, setIsLoading] = useState(false);

  // Mirrors selectedSeats so the unmount cleanup effect below can release the
  // latest held seats without depending on (and re-running for) every selection change.
  const selectedSeatsRef = useRef<SeatDto[]>(selectedSeats);
  const expiryTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    selectedSeatsRef.current = selectedSeats;
  }, [selectedSeats]);

  // Releases every currently held seat's cart entry and cancels the TTL timer.
  // Called on unmount (modal closed, with or without a completed reservation).
  useEffect(() => {
    return () => {
      if (expiryTimerRef.current) {
        clearTimeout(expiryTimerRef.current);
      }
      if (!event.id) return;
      selectedSeatsRef.current.forEach((seat) => {
        if (seat.id) {
          removeSeatFromCart(event.id!, seat.id).catch(() => {
            // Best-effort release; the Redis TTL will clean this up regardless.
          });
        }
      });
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [event.id]);

  const scheduleExpiry = useCallback(
    (expiresAt: Date | null | undefined) => {
      if (!expiresAt) return;
      if (expiryTimerRef.current) {
        clearTimeout(expiryTimerRef.current);
      }
      const delayMs = Math.max(0, expiresAt.getTime() - Date.now());
      expiryTimerRef.current = setTimeout(() => {
        setSelectedSeats([]);
        toast.info(t("eventReservationModal.cart.expired.title"), {
          description: t("eventReservationModal.cart.expired.description"),
        });
        queryClient.invalidateQueries({
          queryKey: getApiUserEventsQueryKey(),
        });
      }, delayMs);
    },
    [t, queryClient],
  );

  const seats: SeatDto[] = useMemo(
    () => location?.seats ?? [],
    [location?.seats],
  );

  const userReservedSeats = userReservations
    .filter((reservation) => reservation.eventId === event.id)
    .map((reservation) => reservation.seat)
    .filter((seat): seat is SeatDto => seat !== null && seat !== undefined);

  const handleSeatSelect = async (seat: SeatDto) => {
    if (!event.id || !seat.id) return;

    const isSelected = selectedSeats.some((s) => s.id === seat.id);

    if (isSelected) {
      // Always allow deselecting our own selection, even if a refetch made the
      // seat map report it as PENDING - that PENDING is this cart's own hold.
      setSelectedSeats((prev) => prev.filter((s) => s.id !== seat.id));
      removeSeatFromCart(event.id, seat.id).catch(() => {
        // Best-effort release; the Redis TTL will clean this up regardless.
      });
      return;
    }

    const seatStatus = findSeatStatus(seat.id, event.seatStatuses);
    if (seatStatus) return; // Can't select reserved, blocked, or pending seats

    const availableSeats = event.reservationsAllowed ?? 0;
    if (selectedSeats.length >= availableSeats) return;

    setSelectedSeats((prev) => [...prev, seat]);
    try {
      const entry = await addSeatToCart(event.id, seat.id);
      scheduleExpiry(entry.expiresAt);
    } catch {
      // Seat is unavailable (already reserved/blocked, or held by another user's
      // cart) - roll back the optimistic selection. useSeatCart already toasted.
      setSelectedSeats((prev) => prev.filter((s) => s.id !== seat.id));
    }
  };

  const handleReserve = async () => {
    if (!event.id || selectedSeats.length === 0) return;

    setIsLoading(true);
    try {
      const seatIds = selectedSeats
        .map((seat) => seat.id!)
        .filter((id) => id !== undefined);
      await onReserve(event.id, seatIds);
      selectedSeatsRef.current = [];
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
          <SeatmapLegend
            variant="selection"
            layout="bar"
            areas={location?.areas ?? []}
          />

          <div className="flex-1 min-h-0">
            <SeatMap
              seats={seats}
              seatStatuses={event.seatStatuses ?? []}
              markers={location?.markers ?? []}
              areas={location?.areas ?? []}
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
