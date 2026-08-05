"use client";

import { Suspense, useState } from "react";
import { useRouter, usePathname, useSearchParams } from "next/navigation";
import { useLiveView } from "@/hooks/use-liveview";
import { useT } from "@/lib/i18n/hooks";
import { ArrowUpIcon } from "@/components/ui/arrow-up";
import { Loader2 } from "lucide-react";
import { SeatMap } from "@/components/common/seat-map";
import { ReservationList } from "@/components/liveview/reservation-list";
import EventSelector from "@/components/common/supervisor/event-selector";
import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionTrigger,
} from "@/components/ui/accordion";
import {
  SeatDto,
  SupervisorReservationResponseDto,
  SupervisorSeatStatusDto,
} from "@/api";
import { Card, CardContent } from "@/components/ui/card";
import { PageHeader } from "@/components/page-header";
import SeatmapLegend from "@/components/common/seatmap-legend";
import { LiveviewStatus } from "@/components/liveview/liveview-status";

function LiveViewPageContent() {
  const t = useT();
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const [selectedEventId, setSelectedEventId] = useState<string | null>(() =>
    searchParams.get("eventId"),
  );
  const [selectedSeats, setSelectedSeats] = useState<SeatDto[]>([]);
  const [expandedAccordion, setExpandedAccordion] = useState<string[]>([]);

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
    setSelectedSeats([]);

    const params = new URLSearchParams(searchParams.toString());
    params.set("eventId", eventId);
    router.replace(`${pathname}?${params.toString()}`, { scroll: false });
  };

  // TODO: Implement
  const handleSeatSelect = (seat: SeatDto) => {
    console.log("Selected seat:", seat);
  };

  return (
    <div className="container mx-auto p-4 sm:p-6">
      <PageHeader
        title={t("liveview.title")}
        description={t("liveview.description")}
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

      {/* Show content only if event is selected */}
      {!selectedEventId ? (
        <Card>
          <CardContent className="flex flex-col items-center gap-2 py-12 text-center text-muted-foreground">
            <ArrowUpIcon size={20} />
            {t("liveview.eventSelector.selectFirst")}
          </CardContent>
        </Card>
      ) : isInitialLoading ? (
        <div className="p-4 border rounded-lg bg-card flex items-center justify-center max-h-[70vh]">
          <div className="flex flex-col items-center gap-2 text-muted-foreground">
            <Loader2 className="h-8 w-8 animate-spin" />
            <span>{t("liveview.status.loading")}</span>
          </div>
        </div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 lg:h-[calc(100vh-220px)] min-h-auto">
          {/* Left Sidebar - Desktop: Normal Layout, Mobile: Accordion */}
          <div className="lg:col-span-1 space-y-6 overflow-y-auto pr-2 lg:min-h-0">
            <div className="hidden lg:block space-y-6">
              {/* Connection Status & Event Information */}
              <LiveviewStatus
                isConnected={isConnected}
                isConnecting={isConnecting}
                isInitialLoading={isInitialLoading}
                error={error}
                event={event}
              />

              {/* Legend */}
              {location && !isInitialLoading && (
                <SeatmapLegend areas={location.areas ?? []} />
              )}

              {/* Reservations List */}
              {reservations && !isInitialLoading && (
                <div className="p-4 border rounded-lg bg-card">
                  <h3 className="text-lg font-bold mb-4">
                    {t("liveview.reservations.title")} ({reservations.length})
                  </h3>
                  <div className="max-h-96 overflow-y-auto pr-2">
                    <ReservationList
                      reservations={reservations}
                      isLoading={isInitialLoading}
                    />
                  </div>
                </div>
              )}
            </div>

            {/* Mobile: Accordion */}
            <div className="lg:hidden">
              <Accordion
                type="multiple"
                value={expandedAccordion}
                onValueChange={setExpandedAccordion}
              >
                {/* Connection Status & Event Information */}
                <AccordionItem value="status">
                  <AccordionTrigger className="text-base font-semibold">
                    {t("liveview.title")}
                  </AccordionTrigger>
                  <AccordionContent>
                    <div className="pt-2">
                      <LiveviewStatus
                        isConnected={isConnected}
                        isConnecting={isConnecting}
                        isInitialLoading={isInitialLoading}
                        error={error}
                        event={event}
                      />
                    </div>
                  </AccordionContent>
                </AccordionItem>

                {/* Legend */}
                {location && !isInitialLoading && (
                  <AccordionItem value="legend">
                    <AccordionTrigger className="text-base font-semibold">
                      {t("liveview.legend.title") || "Legende"}
                    </AccordionTrigger>
                    <AccordionContent>
                      <div className="pt-2">
                        <SeatmapLegend areas={location.areas ?? []} />
                      </div>
                    </AccordionContent>
                  </AccordionItem>
                )}

                {/* SeatMap */}
                {location && reservations && !isInitialLoading && (
                  <AccordionItem value="seatmap">
                    <AccordionTrigger className="text-base font-semibold">
                      {location.name}
                    </AccordionTrigger>
                    <AccordionContent>
                      <div className="pt-2 h-[50vh]">
                        <div className="w-full h-full border rounded-lg bg-card flex flex-col overflow-hidden">
                          <div className="flex-1 w-full h-full overflow-hidden">
                            <SeatMap
                              seats={location.seats || []}
                              seatStatuses={convertReservationsToStatuses(
                                reservations,
                              )}
                              markers={location.markers || []}
                              areas={location.areas ?? []}
                              selectedSeats={selectedSeats}
                              onSeatSelect={handleSeatSelect}
                              readonly={true}
                            />
                          </div>
                        </div>
                      </div>
                    </AccordionContent>
                  </AccordionItem>
                )}

                {/* Reservations List */}
                {reservations && !isInitialLoading && (
                  <AccordionItem value="reservations">
                    <AccordionTrigger className="text-base font-semibold">
                      {t("liveview.reservations.title")} ({reservations.length})
                    </AccordionTrigger>
                    <AccordionContent>
                      <div className="max-h-96 overflow-y-auto pr-2 pt-2">
                        <ReservationList
                          reservations={reservations}
                          isLoading={isInitialLoading}
                        />
                      </div>
                    </AccordionContent>
                  </AccordionItem>
                )}
              </Accordion>
            </div>
          </div>

          {/* Right Main Content - Desktop Only */}
          <div className="hidden lg:flex lg:col-span-2 flex-col min-h-0 max-h-[76vh]">
            {/* SeatMap */}
            {location && reservations && !isInitialLoading && (
              <div className="p-4 border rounded-lg bg-card flex flex-col overflow-hidden max-h-[76vh]">
                <h3 className="text-lg font-bold mb-4">{location.name}</h3>
                <div className="flex-1 overflow-hidden">
                  <SeatMap
                    seats={location.seats || []}
                    seatStatuses={convertReservationsToStatuses(reservations)}
                    markers={location.markers || []}
                    areas={location.areas ?? []}
                    selectedSeats={selectedSeats}
                    onSeatSelect={handleSeatSelect}
                    readonly={true}
                  />
                </div>
              </div>
            )}

            {isInitialLoading && (
              <div className="p-4 border rounded-lg bg-card flex items-center justify-center max-h-[70vh]">
                <div className="flex flex-col items-center gap-2 text-muted-foreground">
                  <Loader2 className="h-8 w-8 animate-spin" />
                  <span>{t("liveview.status.loading")}</span>
                </div>
              </div>
            )}
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
