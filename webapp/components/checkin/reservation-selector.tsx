"use client";

import { useEffect, type SetStateAction, type Dispatch } from "react";
import { useT } from "@/lib/i18n/hooks";
import { Button } from "@/components/ui/button";
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
import type { CheckInInfoResponseDto } from "@/api";

interface ReservationSelectorProps {
  checkInInfo: CheckInInfoResponseDto | null | undefined;
  eventId: bigint | undefined;
  isLoadingInfo: boolean;
  isLoading: boolean;
  isMobile: boolean;
  isDrawerOpen: boolean;
  setIsDrawerOpen: (isOpen: boolean) => void;
  selectedReservations: Set<bigint>;
  setSelectedReservations: Dispatch<SetStateAction<Set<bigint>>>;
  onSubmit: (userId: bigint, eventId: bigint) => void;
  onClear: () => void;
}

export function ReservationSelector({
  checkInInfo,
  eventId,
  isLoadingInfo,
  isLoading,
  isMobile,
  isDrawerOpen,
  setIsDrawerOpen,
  selectedReservations,
  setSelectedReservations,
  onSubmit,
  onClear,
}: ReservationSelectorProps) {
  const t = useT();

  // Initialize selected reservations when check-in info loads
  useEffect(() => {
    if (checkInInfo?.reservations && checkInInfo.reservations.length > 0) {
      const initialSelected = new Set<bigint>();
      checkInInfo.reservations.forEach((reservation) => {
        if (reservation.id) {
          initialSelected.add(reservation.id);
        }
      });
      setSelectedReservations(initialSelected);

      // Open drawer on mobile, show on desktop
      if (isMobile) {
        setIsDrawerOpen(true);
      }
    }
  }, [checkInInfo, isMobile, setIsDrawerOpen, setSelectedReservations]);

  // Toggle reservation selection
  const toggleReservation = (reservationId: bigint) => {
    setSelectedReservations((prev: Set<bigint>) => {
      const newSet = new Set(prev);
      if (newSet.has(reservationId)) {
        newSet.delete(reservationId);
      } else {
        newSet.add(reservationId);
      }
      return newSet;
    });
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

    if (!checkInInfo?.reservations || checkInInfo.reservations.length === 0) {
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
          {checkInInfo.user && (
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
            {checkInInfo.reservations.map((reservation, index) => (
              <Card
                key={reservation.id?.toString() || `reservation-${index}`}
                className={`p-4 cursor-pointer transition-colors ${
                  selectedReservations.has(reservation.id!)
                    ? "bg-primary/10 border-primary"
                    : "hover:bg-muted"
                }`}
                onClick={() => toggleReservation(reservation.id!)}
              >
                <div className="flex items-start gap-3 justify-between">
                  <div className="flex-1">
                    <div className="text-sm font-medium">
                      {t("checkin.reservations.seat")}:{" "}
                      {reservation.seat?.seatNumber || "N/A"}
                      {reservation.seat?.seatRow &&
                        ` (${reservation.seat.seatRow})`}
                    </div>
                    <div className="mt-2 flex gap-2">
                      <Badge variant="outline">
                        {(() => {
                          const status = reservation.liveStatus || "NO_SHOW";
                          const statusMap: Record<string, string> = {
                            NO_SHOW: "noShow",
                            CHECKED_IN: "checkedIn",
                            CANCELLED: "cancelled",
                          };
                          return t(
                            `seatStatus.${statusMap[status] || "noShow"}`,
                          );
                        })()}
                      </Badge>
                      <span className="mx-2">{"-->"}</span>
                      <Badge
                        variant={
                          selectedReservations.has(reservation.id!)
                            ? "default"
                            : "secondary"
                        }
                      >
                        {selectedReservations.has(reservation.id!)
                          ? t("seatStatus.checkedIn")
                          : t("seatStatus.cancelled")}
                      </Badge>
                    </div>
                  </div>
                </div>
              </Card>
            ))}
          </div>
        </div>{" "}
        <div className="sticky bottom-0 bg-background p-4 border-t">
          <Separator className="mb-4" />

          <div className="flex justify-between text-sm mb-4">
            <span>
              {t("checkin.reservations.checkInCount", {
                count: selectedReservations.size,
              })}
            </span>
            <span>
              {t("checkin.reservations.cancelledCount", {
                count:
                  checkInInfo.reservations.length - selectedReservations.size,
              })}
            </span>
          </div>

          <div className="flex gap-2">
            <Button
              onClick={onProcessingSubmit}
              disabled={isLoading}
              className="flex-1"
            >
              {isLoading ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  {t("checkin.actions.processing")}
                </>
              ) : (
                t("checkin.actions.submit")
              )}
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
