"use client";

import { useMemo, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { ArrowUp, Plus, Ban, Download, Trash2 } from "lucide-react";

import { useT } from "@/lib/i18n/hooks";
import { sanitizeFileName } from "@/lib/utils/filename";
import { PageHeader } from "@/components/page-header";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import EventSelector from "@/components/common/supervisor/event-selector";
import { SeatMap } from "@/components/common/seat-map";
import SeatmapLegend from "@/components/common/seatmap-legend";
import { ReservationFormModal } from "@/components/management/reservation-form-modal";
import { BlockSeatsModal } from "@/components/management/block-seats-modal";
import { ReservationsTable } from "@/components/management/reservations/reservations-table";
import { useManagementReservations } from "@/hooks/use-management-reservations";

export default function ManagementReservationsPage() {
  const t = useT();
  const router = useRouter();
  const searchParams = useSearchParams();
  const eventId = searchParams.get("eventId");

  const {
    events,
    locations,
    users,
    seats,
    reservations,
    isLoading,
    isSeatsLoading,
    isReservationsLoading,
    createReservation,
    blockSeats,
    deleteReservations,
    exportCsv,
    exportPdf,
  } = useManagementReservations(eventId);

  const [isFormOpen, setIsFormOpen] = useState(false);
  const [isBlockOpen, setIsBlockOpen] = useState(false);
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());

  const event = events.find((e) => e.id === eventId);
  const location = locations.find((l) => l.id === event?.eventLocationId);
  const eventSeats = useMemo(
    () => seats.filter((s) => s.locationId === location?.id),
    [seats, location?.id],
  );

  const handleEventSelect = (id: string) => {
    router.push(`/management/reservations?eventId=${id}`);
    setSelectedIds(new Set());
  };

  const handleDeleteSelected = async () => {
    if (selectedIds.size === 0) return;
    if (
      confirm(
        t("management.reservations.deleteConfirm", {
          count: selectedIds.size,
        }),
      )
    ) {
      await deleteReservations([...selectedIds]);
      setSelectedIds(new Set());
    }
  };

  const handleExport = async (format: "csv" | "pdf") => {
    if (!eventId) return;
    const blob =
      format === "csv" ? await exportCsv(eventId) : await exportPdf(eventId);
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `reservations-${sanitizeFileName(event?.name)}.${format}`;
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(url);
  };

  const toggleAll = () => {
    if (selectedIds.size === reservations.length) {
      setSelectedIds(new Set());
    } else {
      setSelectedIds(new Set(reservations.map((r) => r.id ?? "")));
    }
  };

  return (
    <div className="container mx-auto p-4 sm:p-6">
      <PageHeader
        title={t("management.reservations.title")}
        description={t("management.reservations.description")}
        search={
          <EventSelector
            events={events}
            isLoadingEvents={isLoading}
            selectedEventId={eventId}
            onEventSelect={handleEventSelect}
          />
        }
      />

      {!eventId ? (
        <Card>
          <CardContent className="flex flex-col items-center gap-2 py-12 text-center text-muted-foreground">
            <ArrowUp className="h-5 w-5" />
            {t("management.reservations.selectEventPrompt")}
          </CardContent>
        </Card>
      ) : (
        <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
          <div className="flex min-h-[28rem] flex-col gap-2">
            <SeatmapLegend
              variant="supervisor"
              layout="bar"
              areas={location?.areas ?? []}
            />
            {isSeatsLoading ? (
              <Skeleton className="flex-1 rounded-lg" />
            ) : (
              <div className="flex-1">
                <SeatMap
                  readonly
                  seats={eventSeats}
                  seatStatuses={event?.seatStatuses ?? []}
                  markers={location?.markers ?? []}
                  areas={location?.areas ?? []}
                  selectedSeats={[]}
                  onSeatSelect={() => {}}
                />
              </div>
            )}
          </div>

          <div className="space-y-3">
            <div className="flex flex-wrap justify-end gap-2">
              {selectedIds.size > 0 && (
                <Button
                  variant="destructive"
                  size="sm"
                  onClick={handleDeleteSelected}
                >
                  <Trash2 className="h-4 w-4" />
                  {selectedIds.size}
                </Button>
              )}
              <Button
                variant="outline"
                size="sm"
                onClick={() => handleExport("csv")}
              >
                <Download className="h-4 w-4" />
                {t("management.reservations.exportCsv")}
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={() => handleExport("pdf")}
              >
                <Download className="h-4 w-4" />
                {t("management.reservations.exportPdf")}
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={() => setIsBlockOpen(true)}
              >
                <Ban className="h-4 w-4" />
                {t("management.reservations.blockSeats")}
              </Button>
              <Button size="sm" onClick={() => setIsFormOpen(true)}>
                <Plus className="h-4 w-4" />
                {t("management.reservations.newReservation")}
              </Button>
            </div>

            <Card>
              <CardContent className="p-0">
                <ReservationsTable
                  reservations={reservations}
                  isLoading={isReservationsLoading}
                  selectedIds={selectedIds}
                  onSelectedIdsChange={setSelectedIds}
                  onToggleAll={toggleAll}
                />
              </CardContent>
            </Card>
          </div>
        </div>
      )}

      {isFormOpen && (
        <ReservationFormModal
          users={users}
          seats={seats}
          locations={locations}
          events={events}
          reservations={reservations}
          eventId={eventId ?? undefined}
          onSubmit={async (data) => {
            await createReservation(data);
            setIsFormOpen(false);
          }}
          onClose={() => setIsFormOpen(false)}
        />
      )}

      {isBlockOpen && (
        <BlockSeatsModal
          events={events}
          seats={seats}
          locations={locations}
          onSubmit={async (data) => {
            await blockSeats(data);
            setIsBlockOpen(false);
          }}
          onClose={() => setIsBlockOpen(false)}
        />
      )}
    </div>
  );
}
