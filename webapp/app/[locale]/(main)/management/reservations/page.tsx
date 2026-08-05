"use client";

import { DeleteIcon } from "@/components/ui/delete";
import { useMemo, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { ArrowUpIcon } from "@/components/ui/arrow-up";
import { PlusIcon } from "@/components/ui/plus";
import { DownloadIcon } from "@/components/ui/download";
import { BanIcon } from "@/components/ui/ban";

import { useT } from "@/lib/i18n/hooks";
import { sanitizeFileName } from "@/lib/utils/filename";
import { PageHeader } from "@/components/page-header";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/custom-ui/button";
import { Skeleton } from "@/components/custom-ui/skeleton";
import EventSelector from "@/components/common/supervisor/event-selector";
import { OverflowActionBar } from "@/components/common/overflow-action-bar";
import { SeatMap } from "@/components/common/seat-map";
import SeatmapLegend from "@/components/common/seatmap-legend";
import { ReservationActionPanel } from "@/components/management/reservations/reservation-action-panel";
import { ReservationsTable } from "@/components/management/reservations/reservations-table";
import { useManagementReservations } from "@/hooks/use-management-reservations";
import { useFillHeight } from "@/hooks/use-fill-height";
import type { ReservationResponseDto, SeatDto } from "@/api";

type ActionMode = "view" | "reserve" | "block";

interface ReservationsViewPanelProps {
  reservations: ReservationResponseDto[];
  isReservationsLoading: boolean;
  selectedIds: Set<string>;
  onSelectedIdsChange: (ids: Set<string>) => void;
  onToggleAll: () => void;
}

function ReservationsViewPanel({
  reservations,
  isReservationsLoading,
  selectedIds,
  onSelectedIdsChange,
  onToggleAll,
}: ReservationsViewPanelProps) {
  return (
    <Card>
      <CardContent className="p-0">
        <ReservationsTable
          reservations={reservations}
          isLoading={isReservationsLoading}
          selectedIds={selectedIds}
          onSelectedIdsChange={onSelectedIdsChange}
          onToggleAll={onToggleAll}
        />
      </CardContent>
    </Card>
  );
}

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

  const [mode, setMode] = useState<ActionMode>("view");
  const [selectedSeats, setSelectedSeats] = useState<SeatDto[]>([]);
  const [reserveUserId, setReserveUserId] = useState("");
  const [deductAllowance, setDeductAllowance] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
  const [isDeletingSelected, setIsDeletingSelected] = useState(false);
  const [exportingFormat, setExportingFormat] = useState<"csv" | "pdf" | null>(
    null,
  );

  const { ref: seatMapColumnRef, height: seatMapColumnHeight } =
    useFillHeight<HTMLDivElement>();

  const event = events.find((e) => e.id === eventId);
  const location = locations.find((l) => l.id === event?.eventLocationId);
  const eventSeats = useMemo(
    () => seats.filter((s) => s.locationId === location?.id),
    [seats, location?.id],
  );

  const userReservedSeats = useMemo(() => {
    if (mode !== "reserve" || !reserveUserId || !eventId) return [];
    return reservations
      .filter(
        (reservation) =>
          reservation.user?.id?.toString() === reserveUserId &&
          reservation.eventId?.toString() === eventId &&
          reservation.status === "RESERVED",
      )
      .map((reservation) => reservation.seat)
      .filter((seat): seat is SeatDto => seat !== undefined);
  }, [mode, reserveUserId, eventId, reservations]);

  const resetAction = () => {
    setMode("view");
    setSelectedSeats([]);
    setReserveUserId("");
    setDeductAllowance(true);
  };

  const handleEventSelect = (id: string) => {
    router.push(`/management/reservations?eventId=${id}`);
    setSelectedIds(new Set());
    resetAction();
  };

  const handleStartReserve = () => {
    setMode("reserve");
    setSelectedSeats([]);
    setReserveUserId("");
    setDeductAllowance(true);
  };

  const handleStartBlock = () => {
    setMode("block");
    setSelectedSeats([]);
  };

  const handleSeatToggle = (seat: SeatDto) => {
    setSelectedSeats((prev) => {
      const isSelected = prev.some((s) => s.id === seat.id);
      if (isSelected) {
        return prev.filter((s) => s.id !== seat.id);
      }
      return [...prev, seat];
    });
  };

  const handleSubmitAction = async () => {
    if (!eventId) return;
    setIsSubmitting(true);
    try {
      if (mode === "reserve") {
        await createReservation({
          eventId,
          userId: reserveUserId,
          seatIds: selectedSeats.map((seat) => seat.id!),
          deductAllowance,
        });
      } else if (mode === "block") {
        await blockSeats({
          eventId,
          seatIds: selectedSeats.map((seat) => seat.id!),
        });
      }
      resetAction();
    } finally {
      setIsSubmitting(false);
    }
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
      setIsDeletingSelected(true);
      try {
        await deleteReservations([...selectedIds]);
        setSelectedIds(new Set());
      } finally {
        setIsDeletingSelected(false);
      }
    }
  };

  const handleExport = async (format: "csv" | "pdf") => {
    if (!eventId) return;
    setExportingFormat(format);
    try {
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
    } finally {
      setExportingFormat(null);
    }
  };

  const toggleAll = () => {
    if (selectedIds.size === reservations.length) {
      setSelectedIds(new Set());
    } else {
      setSelectedIds(new Set(reservations.map((r) => r.id ?? "")));
    }
  };

  const isInteractive = mode !== "view";

  let actionColumn;
  if (mode === "view") {
    actionColumn = (
      <ReservationsViewPanel
        reservations={reservations}
        isReservationsLoading={isReservationsLoading}
        selectedIds={selectedIds}
        onSelectedIdsChange={setSelectedIds}
        onToggleAll={toggleAll}
      />
    );
  } else if (mode === "reserve") {
    actionColumn = (
      <ReservationActionPanel
        mode="reserve"
        users={users}
        selectedSeats={selectedSeats}
        userId={reserveUserId}
        onUserIdChange={setReserveUserId}
        deductAllowance={deductAllowance}
        onDeductAllowanceChange={setDeductAllowance}
        isSubmitting={isSubmitting}
        onSubmit={handleSubmitAction}
        onCancel={resetAction}
      />
    );
  } else {
    actionColumn = (
      <ReservationActionPanel
        mode="block"
        selectedSeats={selectedSeats}
        isSubmitting={isSubmitting}
        onSubmit={handleSubmitAction}
        onCancel={resetAction}
      />
    );
  }

  return (
    <div className="container mx-auto p-4 sm:p-6">
      <PageHeader
        title={t("management.reservations.title")}
        description={t("management.reservations.description")}
        actions={
          eventId && mode === "view" ? (
            <>
              <OverflowActionBar
                actions={[
                  ...(selectedIds.size > 0
                    ? [
                        {
                          key: "delete",
                          label: `${selectedIds.size}`,
                          icon: <DeleteIcon size={16} />,
                          onClick: handleDeleteSelected,
                          variant: "destructive" as const,
                          isLoading: isDeletingSelected,
                        },
                      ]
                    : []),
                  {
                    key: "csv",
                    label: t("management.reservations.exportCsv"),
                    icon: <DownloadIcon size={16} />,
                    onClick: () => handleExport("csv"),
                    isLoading: exportingFormat === "csv",
                  },
                  {
                    key: "pdf",
                    label: t("management.reservations.exportPdf"),
                    icon: <DownloadIcon size={16} />,
                    onClick: () => handleExport("pdf"),
                    isLoading: exportingFormat === "pdf",
                  },
                  {
                    key: "block",
                    label: t("management.reservations.blockSeats"),
                    icon: <BanIcon size={16} />,
                    onClick: handleStartBlock,
                  },
                ]}
              />
              <Button
                onClick={handleStartReserve}
                aria-label={t("management.reservations.newReservation")}
              >
                <PlusIcon size={16} />
                <span className="hidden sm:inline">
                  {t("management.reservations.newReservation")}
                </span>
              </Button>
            </>
          ) : undefined
        }
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
            <ArrowUpIcon size={20} />
            {t("management.reservations.selectEventPrompt")}
          </CardContent>
        </Card>
      ) : (
        <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
          <div
            ref={seatMapColumnRef}
            className="flex flex-col gap-2"
            style={{ height: seatMapColumnHeight }}
          >
            <SeatmapLegend
              variant={isInteractive ? "selection" : "supervisor"}
              layout="bar"
              areas={location?.areas ?? []}
              showUserReserved={mode === "reserve"}
              userReservedLabel={
                mode === "reserve"
                  ? t("management.reservations.userReservedStatus")
                  : undefined
              }
            />
            {isSeatsLoading ? (
              <Skeleton className="flex-1 rounded-lg" />
            ) : (
              <div className="min-h-0 flex-1">
                <SeatMap
                  readonly={!isInteractive}
                  seats={eventSeats}
                  seatStatuses={event?.seatStatuses ?? []}
                  markers={location?.markers ?? []}
                  areas={location?.areas ?? []}
                  selectedSeats={isInteractive ? selectedSeats : []}
                  userReservedSeats={userReservedSeats}
                  onSeatSelect={isInteractive ? handleSeatToggle : () => {}}
                />
              </div>
            )}
          </div>

          <div className="space-y-3">{actionColumn}</div>
        </div>
      )}
    </div>
  );
}
