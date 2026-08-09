"use client";

import { Pencil, Trash2 } from "lucide-react";
import { useMemo, useState } from "react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/custom-ui/dialog";
import { Button } from "@/components/custom-ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Drawer,
  DrawerContent,
  DrawerFooter,
  DrawerHeader,
  DrawerTitle,
  DrawerTrigger,
} from "@/components/ui/drawer";
import { SeatMap } from "@/components/common/seat-map";
import SeatmapLegend from "@/components/common/seatmap-legend";
import { DeleteConfirmationModal } from "./delete-confirmation-modal";
import type {
  AreaDto,
  EventLocationMakerDto,
  UserReservationResponseDto,
  SeatDto,
  SeatStatusDto,
} from "@/api";
import { useT } from "@/lib/i18n/hooks";
import { cn } from "@/lib/utils";
import { useIsMobile } from "@/hooks/use-mobile";

interface SeatMapModalProps {
  seats: SeatDto[];
  seatStatuses: SeatStatusDto[];
  markers: EventLocationMakerDto[];
  areas?: AreaDto[];
  reservation: UserReservationResponseDto;
  eventReservations: UserReservationResponseDto[];
  onClose: () => void;
  onDelete: (reservationIds: string[]) => void | Promise<void>;
  isLoading: boolean;
}

export function SeatMapModal({
  seats,
  seatStatuses,
  markers,
  areas = [],
  reservation,
  eventReservations,
  onClose,
  onDelete,
  isLoading,
}: SeatMapModalProps) {
  const t = useT();
  const isMobile = useIsMobile();

  const [isSeatsDrawerOpen, setIsSeatsDrawerOpen] = useState(true);
  const [deleteModalOpen, setDeleteModalOpen] = useState(false);
  const [selectedReservations, setSelectedReservations] = useState<Set<string>>(
    new Set(),
  );

  const sortedReservations = useMemo(
    () =>
      [...eventReservations].sort((a, b) => {
        const rowCompare = (a.seat?.seatRow ?? "").localeCompare(
          b.seat?.seatRow ?? "",
          undefined,
          { numeric: true, sensitivity: "base" },
        );
        if (rowCompare !== 0) return rowCompare;
        return (a.seat?.seatNumber ?? "").localeCompare(
          b.seat?.seatNumber ?? "",
          undefined,
          { numeric: true, sensitivity: "base" },
        );
      }),
    [eventReservations],
  );

  const reservedSeats = eventReservations
    .map((res) => res.seat)
    .filter(
      (seat): seat is NonNullable<typeof seat> =>
        seat !== null && seat !== undefined,
    );

  const selectedSeats = eventReservations
    .filter((res) => res.id && selectedReservations.has(res.id))
    .map((res) => res.seat)
    .filter(
      (seat): seat is NonNullable<typeof seat> =>
        seat !== null && seat !== undefined,
    );

  const allSelected =
    eventReservations.length > 0 &&
    eventReservations.every((r) => r.id && selectedReservations.has(r.id));

  const toggleReservationSelection = (reservationId: string) => {
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
      setSelectedReservations(
        new Set(
          eventReservations.map((r) => r.id).filter((id): id is string => !!id),
        ),
      );
    }
  };

  const handleDeleteSelected = () => {
    if (selectedReservations.size > 0) {
      setIsSeatsDrawerOpen(false);
      setDeleteModalOpen(true);
    }
  };

  const handleConfirmDelete = async () => {
    await onDelete(Array.from(selectedReservations));
    setDeleteModalOpen(false);
    setSelectedReservations(new Set());
    onClose();
  };

  const handleCancelDelete = () => {
    setDeleteModalOpen(false);
  };

  return (
    <>
      <Dialog open onOpenChange={onClose}>
        <DialogContent
          className="flex flex-col sm:max-w-[95vw] sm:w-[1200px] sm:max-h-[90vh] sm:h-[85vh]"
          onInteractOutside={(e) => e.preventDefault()}
        >
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
                      x: reservation.seat?.coordinate?.xCoordinate,
                      y: reservation.seat?.coordinate?.yCoordinate,
                    })}
            </DialogDescription>
          </DialogHeader>

          {isLoading ? (
            <div className="flex justify-center items-center h-48">
              <p>{t("seatMapModal.loadingText")}</p>
            </div>
          ) : (
            <div className="flex-1 flex flex-col min-h-0 overflow-hidden">
              <SeatmapLegend
                layout="bar"
                areas={areas}
                showSelected
                showUserReserved
                showPending
              />
              <div className="flex-1 min-h-0 min-w-0">
                <SeatMap
                  seats={seats}
                  seatStatuses={seatStatuses}
                  markers={markers}
                  areas={areas}
                  selectedSeats={selectedSeats}
                  onSeatSelect={() => {}} // Read-only
                  userReservedSeats={reservedSeats}
                  readonly
                />
              </div>

              <div className="shrink-0 border-t pt-2">
                {reservedSeats.length > 0 ? (
                  <Drawer
                    open={isSeatsDrawerOpen}
                    onOpenChange={setIsSeatsDrawerOpen}
                    direction={isMobile ? "bottom" : "right"}
                  >
                    <DrawerTrigger asChild>
                      <button
                        type="button"
                        className="flex items-center gap-2 w-full rounded-md border bg-secondary/50 px-3 py-2 text-sm hover:bg-secondary transition-colors"
                      >
                        <Pencil className="h-4 w-4 shrink-0 text-muted-foreground" />
                        <span className="flex-1 text-left font-semibold">
                          {t("reservationCard.manageSeatsButton")}
                        </span>
                        <span className="text-muted-foreground">
                          {reservedSeats.length}
                        </span>
                      </button>
                    </DrawerTrigger>
                    <DrawerContent>
                      <DrawerHeader>
                        <DrawerTitle>
                          {t("seatMapModal.yourReservedSeatsSectionTitle")}
                        </DrawerTitle>
                      </DrawerHeader>
                      <div className="flex items-center space-x-2 px-4 pb-2">
                        <Checkbox
                          id={`select-all-${reservation.eventId}`}
                          checked={allSelected}
                          onCheckedChange={toggleSelectAll}
                        />
                        <label
                          htmlFor={`select-all-${reservation.eventId}`}
                          className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70 cursor-pointer"
                        >
                          {t("reservationCard.selectAll")}
                        </label>
                      </div>
                      <div className="flex flex-col gap-1.5 px-4 pb-6 max-h-[60vh] overflow-y-auto">
                        {sortedReservations.map((res) => {
                          const seat = res.seat;
                          const isSelected =
                            !!res.id && selectedReservations.has(res.id);
                          return (
                            <label
                              key={res.id?.toString()}
                              className={cn(
                                "flex items-center gap-1.5 pl-1.5 pr-2.5 py-1.5 rounded-md border text-sm cursor-pointer transition-colors",
                                isSelected
                                  ? "bg-primary/10 border-primary"
                                  : "bg-seatmap hover:bg-secondary",
                              )}
                            >
                              <Checkbox
                                checked={isSelected}
                                onCheckedChange={() =>
                                  res.id && toggleReservationSelection(res.id)
                                }
                              />
                              <span className="flex flex-col leading-tight py-0.5">
                                <span className="font-medium">
                                  {t("seatMapModal.seatNumberButton", {
                                    seatNumber:
                                      seat?.seatNumber +
                                      (seat?.seatRow
                                        ? " (" + seat.seatRow + ")"
                                        : ""),
                                  })}
                                </span>
                                {(seat?.area || seat?.entrance) && (
                                  <span className="text-[10px] text-muted-foreground">
                                    {[
                                      seat?.area &&
                                        `${t("seatMapModal.areaLabel")}: ${seat.area}`,
                                      seat?.entrance &&
                                        `${t("seatMapModal.entranceLabel")}: ${seat.entrance}`,
                                    ]
                                      .filter(Boolean)
                                      .join(" · ")}
                                  </span>
                                )}
                              </span>
                            </label>
                          );
                        })}
                      </div>
                      <DrawerFooter>
                        <Button
                          variant="destructive"
                          onClick={handleDeleteSelected}
                          disabled={selectedReservations.size === 0}
                        >
                          <Trash2 className="mr-2 h-4 w-4" />
                          {selectedReservations.size === 1
                            ? t("reservationCard.deleteSeatButton")
                            : t("reservationCard.deleteSeatsButton", {
                                count: selectedReservations.size,
                              })}
                        </Button>
                      </DrawerFooter>
                    </DrawerContent>
                  </Drawer>
                ) : (
                  <p className="text-gray-500 dark:text-gray-400 text-sm">
                    {t("seatMapModal.noSeatsReserved")}
                  </p>
                )}
              </div>
            </div>
          )}
        </DialogContent>
      </Dialog>

      <DeleteConfirmationModal
        isOpen={deleteModalOpen}
        onClose={handleCancelDelete}
        onConfirm={handleConfirmDelete}
        selectedCount={selectedReservations.size}
        seats={eventReservations
          .filter((r) => r.id && selectedReservations.has(r.id))
          .map(
            (r) =>
              r.seat?.seatNumber +
              (r.seat?.seatRow ? " (" + r.seat.seatRow + ")" : ""),
          )}
      />
    </>
  );
}
