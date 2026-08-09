"use client";

import { Suspense, useState } from "react";
import { useRouter, usePathname, useSearchParams } from "next/navigation";
import { ArrowUp, Loader2 } from "lucide-react";
import { useLiveView } from "@/hooks/use-liveview";
import { useT } from "@/lib/i18n/hooks";
import { SeatMap } from "@/components/common/seat-map";
import { ReservationList } from "@/components/liveview/reservation-list";
import EventSelector from "@/components/common/supervisor/event-selector";
import {
  SupervisorReservationResponseDto,
  SupervisorSeatStatusDto,
} from "@/api";
import { Card, CardContent } from "@/components/ui/card";
import { PageHeader } from "@/components/page-header";
import { Skeleton } from "@/components/custom-ui/skeleton";
import SeatmapLegend from "@/components/common/seatmap-legend";
import { useFillHeight } from "@/hooks/use-fill-height";
import { LiveviewConnectionBadge } from "@/components/liveview/liveview-status";

function LiveViewPageContent() {
  const t = useT();
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const [selectedEventId, setSelectedEventId] = useState<string | null>(() =>
    searchParams.get("eventId"),
  );

  const { ref: seatMapColumnRef, height: seatMapColumnHeight } =
    useFillHeight<HTMLDivElement>();

  const {
    events,
    isLoadingEvents,
    isConnected,
    isConnecting,
    isInitialLoading,
    event,
    location,
    reservations,
    error,
  } = useLiveView(selectedEventId, !!selectedEventId);

  const handleEventSelect = (eventId: string) => {
    setSelectedEventId(eventId);

    const params = new URLSearchParams(searchParams.toString());
    params.set("eventId", eventId);
    router.replace(`${pathname}?${params.toString()}`, { scroll: false });
  };

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
            events={events}
            isLoadingEvents={isLoadingEvents}
            selectedEventId={selectedEventId}
            onEventSelect={handleEventSelect}
            placeholderKey="liveview.eventSelector.placeholder"
            noEventsKey="liveview.eventSelector.noEvents"
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
      ) : (
        <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
          <div
            ref={seatMapColumnRef}
            className="flex flex-col gap-2"
            style={{ height: seatMapColumnHeight }}
          >
            <SeatmapLegend layout="bar" areas={location?.areas ?? []} />
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
                  onSeatSelect={() => {}}
                />
              </div>
            )}
          </div>

          <div className="space-y-3">
            <div className="rounded-lg border bg-card p-4">
              <h3 className="mb-4 text-lg font-bold">
                {t("liveview.reservations.title")} ({reservations.length})
              </h3>
              <ReservationList reservations={reservations} isLoading={false} />
            </div>
          </div>
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
