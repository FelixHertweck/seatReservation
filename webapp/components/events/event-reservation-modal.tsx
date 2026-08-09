"use client";

import { useState, useMemo, useRef, useCallback, useEffect } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Trash2 } from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/custom-ui/dialog";
import {
  Drawer,
  DrawerContent,
  DrawerHeader,
  DrawerTitle,
  DrawerTrigger,
} from "@/components/ui/drawer";
import { Button } from "@/components/custom-ui/button";
import { cn } from "@/lib/utils";
import { useIsMobile } from "@/hooks/use-mobile";
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
  const isMobile = useIsMobile();

  const [selectedSeats, setSelectedSeats] = useState<SeatDto[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [highlightedSeatId, setHighlightedSeatId] = useState<string | null>(
    null,
  );
  const [isSeatDrawerOpen, setIsSeatDrawerOpen] = useState(false);

  // Mirrors selectedSeats so the unmount cleanup effect below can release the
  // latest held seats without depending on (and re-running for) every selection change.
  const selectedSeatsRef = useRef<SeatDto[]>(selectedSeats);
  // One timer per held seat (keyed by seatId), not a single shared timer - each seat's
  // Redis hold expires independently, so a later selection must not clobber an earlier
  // seat's own expiry tracking, and deselecting a seat must cancel only its own timer.
  const expiryTimersRef = useRef<Map<string, ReturnType<typeof setTimeout>>>(
    new Map(),
  );

  useEffect(() => {
    selectedSeatsRef.current = selectedSeats;
    if (selectedSeats.length === 0) setIsSeatDrawerOpen(false);
  }, [selectedSeats]);

  const clearExpiryTimer = useCallback((seatId: string) => {
    const timer = expiryTimersRef.current.get(seatId);
    if (timer) {
      clearTimeout(timer);
      expiryTimersRef.current.delete(seatId);
    }
  }, []);

  // Releases every currently held seat's cart entry and cancels all TTL timers.
  // Called on unmount (modal closed, with or without a completed reservation).
  useEffect(() => {
    return () => {
      // Intentionally read live at cleanup time (not a DOM ref) - eslint's suggested fix of
      // capturing the ref at effect-setup time would defeat the purpose of tracking timers
      // added after this effect first ran.
      // eslint-disable-next-line react-hooks/exhaustive-deps
      const expiryTimers = expiryTimersRef.current;
      expiryTimers.forEach((timer) => clearTimeout(timer));
      expiryTimers.clear();
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
    (seatId: string, expiresAt: Date | null | undefined) => {
      clearExpiryTimer(seatId);
      if (!expiresAt) return;
      const delayMs = Math.max(0, expiresAt.getTime() - Date.now());
      const timer = setTimeout(() => {
        expiryTimersRef.current.delete(seatId);
        setSelectedSeats((prev) => prev.filter((s) => s.id !== seatId));
        toast.info(t("eventReservationModal.cart.expired.title"), {
          description: t("eventReservationModal.cart.expired.description"),
        });
        queryClient.invalidateQueries({
          queryKey: getApiUserEventsQueryKey(),
        });
      }, delayMs);
      expiryTimersRef.current.set(seatId, timer);
    },
    [t, queryClient, clearExpiryTimer],
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
      clearExpiryTimer(seat.id);
      setSelectedSeats((prev) => prev.filter((s) => s.id !== seat.id));
      setHighlightedSeatId((prev) => (prev === seat.id ? null : prev));
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
      scheduleExpiry(seat.id, entry.expiresAt);
    } catch {
      // Seat is unavailable (already reserved/blocked, or held by another user's
      // cart) - roll back the optimistic selection. useSeatCart already toasted.
      setSelectedSeats((prev) => prev.filter((s) => s.id !== seat.id));
    }
  };

  const handleSeatChipClick = (seatId: string) => {
    setHighlightedSeatId((prev) => (prev === seatId ? null : seatId));
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
      expiryTimersRef.current.forEach((timer) => clearTimeout(timer));
      expiryTimersRef.current.clear();
      onClose();
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Dialog open onOpenChange={onClose}>
      <DialogContent
        className="flex flex-col sm:w-[95vw] sm:max-w-none sm:max-h-[90vh] sm:h-[85vh]"
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
            layout="bar"
            areas={location?.areas ?? []}
            showSelected
            showUserReserved
            showPending
          />

          <div className="flex-1 min-h-0">
            <SeatMap
              seats={seats}
              seatStatuses={event.seatStatuses ?? []}
              markers={location?.markers ?? []}
              areas={location?.areas ?? []}
              selectedSeats={selectedSeats}
              userReservedSeats={userReservedSeats}
              highlightedSeatId={highlightedSeatId}
              onSeatSelect={handleSeatSelect}
            />
          </div>

          <div className="flex justify-between items-center gap-2 pt-2 border-t">
            <div className="flex-1 min-w-0">
              {selectedSeats.length > 0 && (
                <Drawer
                  open={isSeatDrawerOpen}
                  onOpenChange={setIsSeatDrawerOpen}
                  direction={isMobile ? "bottom" : "right"}
                >
                  <DrawerTrigger asChild>
                    <button
                      type="button"
                      className="rounded-md border bg-seatmap px-3 py-1.5 text-sm hover:bg-secondary transition-colors"
                    >
                      {selectedSeats.length === 1
                        ? t("eventReservationModal.selectedSeatButton")
                        : t("eventReservationModal.selectedSeatsButton", {
                            count: selectedSeats.length,
                          })}
                    </button>
                  </DrawerTrigger>
                  <DrawerContent>
                    <DrawerHeader>
                      <DrawerTitle>
                        {t("eventReservationModal.selectedSeatsTitle")}
                      </DrawerTitle>
                    </DrawerHeader>
                    <div className="flex flex-col gap-1.5 px-4 pb-6 max-h-[50vh] overflow-y-auto">
                      {selectedSeats.map((seat) => {
                        const isHighlighted = highlightedSeatId === seat.id;
                        return (
                          <div
                            key={seat.id?.toString()}
                            className={cn(
                              "flex items-center justify-between gap-2 rounded-md border px-2 py-2 text-sm transition-colors",
                              isHighlighted
                                ? "bg-primary/10 border-primary"
                                : "bg-seatmap hover:bg-secondary",
                            )}
                          >
                            <button
                              type="button"
                              onClick={() => {
                                if (seat.id) handleSeatChipClick(seat.id);
                                setIsSeatDrawerOpen(false);
                              }}
                              className="flex-1 text-left px-1"
                            >
                              {seat.seatNumber +
                                (seat.seatRow ? " (" + seat.seatRow + ")" : "")}
                            </button>
                            <button
                              type="button"
                              aria-label={t(
                                "eventReservationModal.removeSeatAriaLabel",
                              )}
                              onClick={() => handleSeatSelect(seat)}
                              className="rounded-full p-1 hover:bg-destructive/20 hover:text-destructive transition-colors"
                            >
                              <Trash2 className="h-4 w-4" />
                            </button>
                          </div>
                        );
                      })}
                    </div>
                  </DrawerContent>
                </Drawer>
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
                isLoading={isLoading}
                disabled={selectedSeats.length === 0 || isLoading}
                className="text-sm md:text-base px-3 py-2"
              >
                {selectedSeats.length === 1
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
