"use client";

import { useEffect, useMemo, type SetStateAction, type Dispatch } from "react";
import { useT } from "@/lib/i18n/hooks";
import { Button } from "@/components/custom-ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Drawer,
  DrawerContent,
  DrawerHeader,
  DrawerTitle,
} from "@/components/ui/drawer";
import { Loader2, ChevronUp, X } from "lucide-react";
import { Separator } from "@/components/ui/separator";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";
import {
  SEAT_STATUS_BG,
  SEAT_STATUS_LABEL_KEY,
  SEAT_STATUS_TEXT,
  getSeatVisualStatus,
} from "@/lib/seatStatusStyles";
import type {
  CheckInInfoResponseDto,
  SupervisorReservationResponseDto,
} from "@/api";

export type ReservationAction = "CHECK_IN" | "CANCEL" | "NONE";

interface ReservationSelectorProps {
  checkInInfo: CheckInInfoResponseDto | null | undefined;
  eventId: string | undefined;
  isLoadingInfo: boolean;
  isLoading: boolean;
  isMobile: boolean;
  isDrawerOpen: boolean;
  setIsDrawerOpen: (isOpen: boolean) => void;
  reservationActions: Record<string, ReservationAction>;
  setReservationActions: Dispatch<
    SetStateAction<Record<string, ReservationAction>>
  >;
  onSubmit: (userId: string, eventId: string) => void;
  onClear: () => void;
}

/** Numeric sort priority for the checkin list: pending first, then cancelled, then checked-in. */
function getStatusSortPriority(
  r: SupervisorReservationResponseDto,
): number {
  const visual = getSeatVisualStatus(r.status, r.liveStatus);
  if (visual === "CHECKED_IN") return 2;
  if (visual === "CANCELLED") return 1;
  return 0; // RESERVED / PENDING / etc. – needs action → show first
}

export function ReservationSelector({
  checkInInfo,
  eventId,
  isLoadingInfo,
  isLoading,
  isMobile,
  isDrawerOpen,
  setIsDrawerOpen,
  reservationActions,
  setReservationActions,
  onSubmit,
  onClear,
}: ReservationSelectorProps) {
  const t = useT();

  // Sort reservations: pending → cancelled → checked-in
  const sortedReservations = useMemo(() => {
    if (!checkInInfo?.reservations) return [];
    return [...checkInInfo.reservations].sort(
      (a, b) => getStatusSortPriority(a) - getStatusSortPriority(b),
    );
  }, [checkInInfo?.reservations]);

  // Initialize reservation actions when check-in info loads.
  // Already checked-in / cancelled reservations get NONE by default (no double processing).
  useEffect(() => {
    if (sortedReservations.length > 0) {
      const initialActions: Record<string, ReservationAction> = {};
      sortedReservations.forEach((reservation) => {
        if (reservation.id) {
          const visual = getSeatVisualStatus(
            reservation.status,
            reservation.liveStatus,
          );
          // Already processed → no default action; still pending → default to CHECK_IN
          initialActions[reservation.id] =
            visual === "CHECKED_IN" || visual === "CANCELLED"
              ? "NONE"
              : "CHECK_IN";
        }
      });
      setReservationActions(initialActions);

      // Open drawer on mobile, show on desktop
      if (isMobile) {
        setIsDrawerOpen(true);
      }
    }
  }, [checkInInfo, isMobile, setIsDrawerOpen, setReservationActions, sortedReservations]);

  /**
   * Clicking the active action toggles it off (resets to NONE).
   * Clicking an inactive action sets it.
   * This replaces the separate "Unverändert" button.
   */
  const toggleReservationAction = (
    reservationId: string,
    action: ReservationAction,
  ) => {
    setReservationActions((prev) => ({
      ...prev,
      [reservationId]: prev[reservationId] === action ? "NONE" : action,
    }));
  };

  const onProcessingSubmit = () => {
    if (checkInInfo?.user && eventId) {
      onSubmit(checkInInfo.user.id!, eventId);
    }
  };

  // Render reservation list
  const renderReservationList = () => {
    if (isLoadingInfo) {
      return (
        <div className="flex items-center justify-center py-8">
          <Loader2 className="h-8 w-8 animate-spin" />
          <span className="ml-2">{t("checkin.reservations.loading")}</span>
        </div>
      );
    }

    if (sortedReservations.length === 0) {
      return (
        <div className="text-center py-8 text-muted-foreground">
          {t("checkin.reservations.noReservations")}
        </div>
      );
    }

    return (
      <div className="flex flex-col h-full">
        <div className="flex-1 overflow-y-auto space-y-4 p-4">
          {/* User Info Card - shown once at the top */}
          {checkInInfo?.user && (
            <div className="space-y-2">
              <div className="text-sm">
                <span className="font-semibold">
                  {t("checkin.reservations.user")}:
                </span>{" "}
                <span>{checkInInfo.user.username || "N/A"}</span>
              </div>
            </div>
          )}

          <div className="space-y-2">
            {sortedReservations.map((reservation, index) => {
              const reservationId = reservation.id!;
              const action = reservationActions[reservationId] ?? "NONE";
              const currentStatus = getSeatVisualStatus(
                reservation.status,
                reservation.liveStatus,
              );
              const targetStatus =
                action === "CHECK_IN"
                  ? "CHECKED_IN"
                  : action === "CANCEL"
                    ? "CANCELLED"
                    : currentStatus;

              // Disable CHECK_IN if already checked in; disable CANCEL if already cancelled
              const isAlreadyCheckedIn = currentStatus === "CHECKED_IN";
              const isAlreadyCancelled = currentStatus === "CANCELLED";

              return (
                <Card
                  key={reservationId.toString() || `reservation-${index}`}
                  className={cn(
                    "p-4",
                    (isAlreadyCheckedIn || isAlreadyCancelled) && "opacity-60",
                  )}
                >
                  <div className="text-sm font-medium">
                    {t("checkin.reservations.seat")}:{" "}
                    {reservation.seat?.seatNumber || "N/A"}
                    {reservation.seat?.seatRow &&
                      ` (${reservation.seat.seatRow})`}
                  </div>
                  {reservation.guestName && (
                    <div className="text-sm text-muted-foreground">
                      {reservation.guestName}
                    </div>
                  )}
                  {(reservation.seat?.area || reservation.seat?.entrance) && (
                    <div className="text-[10px] text-muted-foreground">
                      {[
                        reservation.seat?.area &&
                          `${t("seatMapModal.areaLabel")}: ${reservation.seat.area}`,
                        reservation.seat?.entrance &&
                          `${t("seatMapModal.entranceLabel")}: ${reservation.seat.entrance}`,
                      ]
                        .filter(Boolean)
                        .join(" · ")}
                    </div>
                  )}
                  <div className="mt-2 flex items-center gap-2">
                    <Badge
                      className={cn(
                        SEAT_STATUS_BG[currentStatus],
                        SEAT_STATUS_TEXT[currentStatus],
                      )}
                    >
                      {t(SEAT_STATUS_LABEL_KEY[currentStatus])}
                    </Badge>
                    {action !== "NONE" && (
                      <>
                        <span>{"-->"}</span>
                        <Badge
                          className={cn(
                            SEAT_STATUS_BG[targetStatus],
                            SEAT_STATUS_TEXT[targetStatus],
                          )}
                        >
                          {t(SEAT_STATUS_LABEL_KEY[targetStatus])}
                        </Badge>
                      </>
                    )}
                  </div>
                  {/* Action toggle buttons – clicking the active button deselects it (= NONE) */}
                  <div className="mt-3 flex gap-1 rounded-md border p-1">
                    <button
                      type="button"
                      disabled={isAlreadyCheckedIn}
                      onClick={() =>
                        toggleReservationAction(reservationId, "CHECK_IN")
                      }
                      className={cn(
                        "flex-1 rounded-sm px-2 py-1 text-xs font-medium transition-colors",
                        isAlreadyCheckedIn
                          ? "cursor-not-allowed text-muted-foreground/40"
                          : action === "CHECK_IN"
                            ? "bg-primary text-primary-foreground"
                            : "text-muted-foreground hover:bg-muted",
                      )}
                    >
                      {t("checkin.reservations.actionCheckIn")}
                    </button>
                    <button
                      type="button"
                      disabled={isAlreadyCancelled}
                      onClick={() =>
                        toggleReservationAction(reservationId, "CANCEL")
                      }
                      className={cn(
                        "flex-1 rounded-sm px-2 py-1 text-xs font-medium transition-colors",
                        isAlreadyCancelled
                          ? "cursor-not-allowed text-muted-foreground/40"
                          : action === "CANCEL"
                            ? "bg-primary text-primary-foreground"
                            : "text-muted-foreground hover:bg-muted",
                      )}
                    >
                      {t("checkin.reservations.actionCancel")}
                    </button>
                  </div>
                </Card>
              );
            })}
          </div>
        </div>{" "}
        <div className="sticky bottom-0 bg-background p-4 border-t">
          <Separator className="mb-4" />

          {(() => {
            const actions = Object.values(reservationActions);
            const tiles: {
              key: ReservationAction;
              label: string;
              dot: string;
            }[] = [
              {
                key: "CHECK_IN",
                label: t("checkin.reservations.checkInLabel"),
                dot: SEAT_STATUS_BG.CHECKED_IN,
              },
              {
                key: "NONE",
                label: t("checkin.reservations.noActionLabel"),
                dot: "bg-muted-foreground/40",
              },
              {
                key: "CANCEL",
                label: t("checkin.reservations.cancelLabel"),
                dot: SEAT_STATUS_BG.CANCELLED,
              },
            ];
            return (
              <div className="grid grid-cols-3 gap-2 mb-4">
                {tiles.map((tile) => (
                  <div
                    key={tile.key}
                    className="flex flex-col items-center gap-1 rounded-md border bg-muted/30 py-2"
                  >
                    <div className="flex items-center gap-1.5">
                      <span className={cn("h-2 w-2 rounded-full", tile.dot)} />
                      <span className="text-lg font-semibold tabular-nums">
                        {actions.filter((a) => a === tile.key).length}
                      </span>
                    </div>
                    <span className="text-xs text-muted-foreground">
                      {tile.label}
                    </span>
                  </div>
                ))}
              </div>
            );
          })()}

          <div className="flex gap-2">
            <Button
              onClick={onProcessingSubmit}
              isLoading={isLoading}
              disabled={isLoading}
              className="flex-1"
            >
              {t("checkin.actions.submit")}
            </Button>
            <Button variant="outline" onClick={onClear}>
              <X className="mr-2 h-4 w-4" />
              {t("checkin.actions.close")}
            </Button>
          </div>
        </div>
      </div>
    );
  };

  return (
    <>
      {/* Reservations Section - Desktop */}
      {!isMobile && checkInInfo && (
        <Card className="md:max-h-[calc(100vh-100px)] md:overflow-y-auto">
          <CardHeader>
            <CardTitle>{t("checkin.reservations.title")}</CardTitle>
          </CardHeader>
          <CardContent>{renderReservationList()}</CardContent>
        </Card>
      )}

      {/* Reservations Drawer - Mobile */}
      {isMobile && (
        <Drawer open={isDrawerOpen} onOpenChange={setIsDrawerOpen}>
          <DrawerContent>
            <DrawerHeader>
              <DrawerTitle>{t("checkin.reservations.title")}</DrawerTitle>
            </DrawerHeader>
            <div className="px-4 pb-4 max-h-[80vh] overflow-y-auto">
              {renderReservationList()}
            </div>
          </DrawerContent>
        </Drawer>
      )}

      {/* Drawer Trigger - Mobile */}
      {isMobile && !isDrawerOpen && checkInInfo && (
        <div
          className="fixed bottom-0 left-0 right-0 bg-background border-t p-2 flex justify-center cursor-pointer shadow-lg"
          onClick={() => setIsDrawerOpen(true)}
        >
          <ChevronUp className="h-6 w-6 text-muted-foreground" />
          <span className="sr-only">
            {t("checkin.reservations.openDrawer")}
          </span>
        </div>
      )}
    </>
  );
}
