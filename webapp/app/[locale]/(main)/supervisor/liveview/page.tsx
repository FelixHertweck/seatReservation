"use client";

import { Suspense, useEffect, useMemo, useState } from "react";
import { ArrowUp, ChevronUp, Clock, Loader2 } from "lucide-react";
import { useLiveView } from "@/hooks/use-liveview";
import { useCheckin } from "@/hooks/use-checkin";
import { useSupervisorEvent } from "@/hooks/use-supervisor-event";
import { useIsMobile } from "@/hooks/use-mobile";
import { useT } from "@/lib/i18n/hooks";
import { formatDateTime } from "@/lib/utils";
import { SeatMap } from "@/components/common/seat-map";
import { ReservationList } from "@/components/liveview/reservation-list";
import EventSelector from "@/components/common/supervisor/event-selector";
import {
  SupervisorReservationResponseDto,
  SupervisorSeatStatusDto,
} from "@/api";
import { Card, CardContent } from "@/components/ui/card";
import {
  Drawer,
  DrawerContent,
  DrawerHeader,
  DrawerTitle,
} from "@/components/ui/drawer";
import { PageHeader } from "@/components/page-header";
import { Skeleton } from "@/components/custom-ui/skeleton";
import SeatmapLegend from "@/components/common/seatmap-legend";
import { useFillHeight } from "@/hooks/use-fill-height";
import { LiveviewConnectionBadge } from "@/components/liveview/liveview-status";

function LiveViewPageContent() {
  const t = useT();
  const { selectedEventId, selectEvent } = useSupervisorEvent();
  const isMobile = useIsMobile();
  const [isDrawerOpen, setIsDrawerOpen] = useState(false);
  const [highlightedSeatId, setHighlightedSeatId] = useState<string | null>(
    null,
  );

  const handleReservationClick = (seatId: string) => {
    setHighlightedSeatId((prev) => (prev === seatId ? null : seatId));
    // Close the drawer so the highlighted seat on the map is actually visible.
    if (isMobile) setIsDrawerOpen(false);
  };

  // The previously highlighted seat ID belongs to the event/location being
  // left behind, so it can't carry over to the newly selected one.
  const handleEventSelect = (eventId: string) => {
    selectEvent(eventId);
    setHighlightedSeatId(null);
  };

  const { ref: seatMapColumnRef, height: seatMapColumnHeight } =
    useFillHeight<HTMLDivElement>();

  const { events, isLoadingEvents } = useCheckin();

  // Read via an effect rather than at render time (Date.now() is impure) -- refreshed
  // periodically so an event crossing its booking deadline while this page stays open
  // becomes available without a manual reload.
  const [now, setNow] = useState<number | null>(null);
  useEffect(() => {
    // Ticking clock synced from an external source (the system clock), so the
    // initial read + periodic refresh both belong here rather than in render.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setNow(Date.now());
    const interval = setInterval(() => setNow(Date.now()), 60_000);
    return () => clearInterval(interval);
  }, []);

  const availableEvents = useMemo(() => {
    if (now === null) return [];
    return (events ?? []).filter(
      (e) => !!e.bookingDeadline && new Date(e.bookingDeadline).getTime() < now,
    );
  }, [events, now]);
  const selectedEvent = useMemo(
    () => (events ?? []).find((e) => e.id === selectedEventId),
    [events, selectedEventId],
  );
  // Derived from the plain REST event list rather than the live-view websocket's own
  // `event` field: that connection is itself gated behind the booking deadline on the
  // backend now, so relying on it here would be circular -- it would never populate
  // (and the reconnect budget would run out) before we already know the deadline passed.
  const isDeadlinePassed = useMemo(() => {
    if (now === null || !selectedEvent?.bookingDeadline) return false;
    return new Date(selectedEvent.bookingDeadline).getTime() < now;
  }, [selectedEvent, now]);

  // Only open the live-view websocket once the deadline has passed -- the backend
  // rejects the connection until then, so connecting earlier would just burn through
  // useWebSocket's limited reconnect attempts for nothing.
  const {
    isConnected,
    isConnecting,
    isInitialLoading,
    event,
    location,
    reservations,
    error,
  } = useLiveView(selectedEventId, !!selectedEventId && isDeadlinePassed);

  return (
    <div className="container mx-auto p-4 sm:p-6">
      <PageHeader
        title={t("liveview.title")}
        description={t("liveview.description")}
        actions={
          selectedEventId ? (
            <LiveviewConnectionBadge
              isConnected={isConnected}
              isConnecting={isConnecting}
              error={error}
              event={event}
            />
          ) : undefined
        }
        search={
          <EventSelector
            events={availableEvents}
            isLoadingEvents={isLoadingEvents}
            selectedEventId={selectedEventId}
            onEventSelect={handleEventSelect}
          />
        }
      />

      {!selectedEventId ? (
        <Card>
          <CardContent className="flex flex-col items-center gap-2 py-12 text-center text-muted-foreground">
            <ArrowUp className="h-5 w-5" />
            {t("liveview.eventSelector.selectFirst")}
          </CardContent>
        </Card>
      ) : isInitialLoading ? (
        <div className="flex max-h-[70vh] items-center justify-center rounded-lg border bg-card p-4">
          <div className="flex flex-col items-center gap-2 text-muted-foreground">
            <Loader2 className="h-8 w-8 animate-spin" />
            <span>{t("liveview.status.loading")}</span>
          </div>
        </div>
      ) : !isDeadlinePassed ? (
        <Card>
          <CardContent className="flex flex-col items-center gap-2 py-12 text-center text-muted-foreground">
            <Clock className="h-5 w-5" />
            <span>{t("liveview.deadlineNotPassed")}</span>
            {(() => {
              const formatted = formatDateTime(selectedEvent?.bookingDeadline);
              return formatted ? (
                <span className="text-sm">
                  {t("liveview.deadlineNotPassedAvailableFrom", formatted)}
                </span>
              ) : null;
            })()}
          </CardContent>
        </Card>
      ) : (
        <div
          className={`grid gap-4 ${isMobile ? "grid-cols-1" : "lg:grid-cols-2"}`}
        >
          <div
            ref={seatMapColumnRef}
            className="flex flex-col gap-2"
            style={{ height: seatMapColumnHeight }}
          >
            <SeatmapLegend
              layout="bar"
              areas={location?.areas ?? []}
              showLiveStatus
            />
            {!location ? (
              <Skeleton className="flex-1 rounded-lg" />
            ) : (
              <div className="min-h-0 flex-1">
                <SeatMap
                  readonly
                  seats={location.seats ?? []}
                  seatStatuses={convertReservationsToStatuses(reservations)}
                  markers={location.markers ?? []}
                  areas={location.areas ?? []}
                  selectedSeats={[]}
                  highlightedSeatId={highlightedSeatId}
                  onSeatSelect={() => {}}
                />
              </div>
            )}
          </div>

          {/* Reservations Panel - Desktop */}
          {!isMobile && (
            <div className="space-y-3">
              <Card className="lg:max-h-[calc(100vh-100px)] lg:overflow-y-auto">
                <CardContent className="p-4">
                  <h3 className="mb-4 text-lg font-bold">
                    {t("liveview.reservations.title")} ({reservations.length})
                  </h3>
                  <ReservationList
                    reservations={reservations}
                    isLoading={false}
                    highlightedSeatId={highlightedSeatId}
                    onReservationClick={handleReservationClick}
                  />
                </CardContent>
              </Card>
            </div>
          )}
        </div>
      )}

      {/* Reservations Drawer - Mobile */}
      {isMobile && (
        <Drawer open={isDrawerOpen} onOpenChange={setIsDrawerOpen}>
          <DrawerContent>
            <DrawerHeader>
              <DrawerTitle>
                {t("liveview.reservations.title")} ({reservations.length})
              </DrawerTitle>
            </DrawerHeader>
            <div className="px-4 pb-4 max-h-[80vh] overflow-y-auto">
              <ReservationList
                reservations={reservations}
                isLoading={false}
                highlightedSeatId={highlightedSeatId}
                onReservationClick={handleReservationClick}
              />
            </div>
          </DrawerContent>
        </Drawer>
      )}

      {/* Drawer Trigger - Mobile */}
      {isMobile &&
        selectedEventId &&
        isDeadlinePassed &&
        !isInitialLoading &&
        !isDrawerOpen && (
          <div
            className="fixed bottom-0 left-0 right-0 bg-background border-t p-2 flex items-center justify-center gap-2 cursor-pointer shadow-lg"
            onClick={() => setIsDrawerOpen(true)}
          >
            <ChevronUp className="h-6 w-6 text-muted-foreground" />
            <span className="text-sm text-muted-foreground">
              {t("liveview.reservations.title")} ({reservations.length})
            </span>
          </div>
        )}
    </div>
  );
}

export default function LiveViewPage() {
  return (
    <Suspense fallback={null}>
      <LiveViewPageContent />
    </Suspense>
  );
}

const convertReservationsToStatuses = (
  reservations: SupervisorReservationResponseDto[],
): SupervisorSeatStatusDto[] => {
  return reservations.map((reservation) => ({
    seatId: reservation.seat?.id,
    status: reservation.status,
    reservationId: reservation.id,
    liveStatus: reservation.liveStatus,
  }));
};
