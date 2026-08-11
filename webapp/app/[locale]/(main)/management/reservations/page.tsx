"use client";

import { useMemo, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { ArrowUp, Plus, Ban, Download, Trash2 } from "lucide-react";

import { useT } from "@/lib/i18n/hooks";
import { sanitizeFileName } from "@/lib/utils/filename";
import { PageHeader } from "@/components/page-header";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/custom-ui/button";
import { Skeleton } from "@/components/custom-ui/skeleton";
import EventSelector from "@/components/common/supervisor/event-selector";
import { OverflowActionBar } from "@/components/common/overflow-action-bar";
import { SearchAndFilter } from "@/components/common/search-and-filter";
import { SeatMap } from "@/components/common/seat-map";
import SeatmapLegend from "@/components/common/seatmap-legend";
import { ReservationActionPanel } from "@/components/management/reservations/reservation-action-panel";
import { ReservationConfirmationModal } from "@/components/management/reservations/reservation-confirmation-modal";
import { ReservationsTable } from "@/components/management/reservations/reservations-table";
import { useManagementReservations } from "@/hooks/use-management-reservations";
import { useFillHeight } from "@/hooks/use-fill-height";
import type { ReservationResponseDto, SeatDto } from "@/api";
import { useQuery } from "@tanstack/react-query";
import { getApiManagerReservationsConfirmationEmailByEventIdByUserIdOptions } from "@/api/@tanstack/react-query.gen";

type ActionMode = "view" | "reserve" | "block";

interface ReservationsViewPanelProps {
  reservations: ReservationResponseDto[];
  seats?: SeatDto[];
  isReservationsLoading: boolean;
  selectedIds: Set<string>;
  onSelectedIdsChange: (ids: Set<string>) => void;
  onToggleAll: () => void;
  onDeleteOne: (id: string) => void;
  onDeleteGroup: (ids: string[]) => void;
  deletingIds: Set<string>;
  onSearch: (query: string) => void;
  highlightedSeatId: string | null;
  onSeatClick: (seatId: string) => void;
  onViewConfirmation: (userId: string, userName: string) => void;
}

function ReservationsViewPanel({
  reservations,
  seats,
  isReservationsLoading,
  selectedIds,
  onSelectedIdsChange,
  onToggleAll,
  onDeleteOne,
  onDeleteGroup,
  deletingIds,
  onSearch,
  highlightedSeatId,
  onSeatClick,
  onViewConfirmation,
}: ReservationsViewPanelProps) {
  return (
    <div className="space-y-3">
      <SearchAndFilter
        onSearch={onSearch}
        onFilter={() => {}}
        filterOptions={[]}
      />
      <Card>
        <CardContent className="p-0">
          <ReservationsTable
            reservations={reservations}
            seats={seats}
            isLoading={isReservationsLoading}
            selectedIds={selectedIds}
            onSelectedIdsChange={onSelectedIdsChange}
            onToggleAll={onToggleAll}
            onDeleteOne={onDeleteOne}
            onDeleteGroup={onDeleteGroup}
            deletingIds={deletingIds}
            highlightedSeatId={highlightedSeatId}
            onSeatClick={onSeatClick}
            onViewConfirmation={onViewConfirmation}
          />
        </CardContent>
      </Card>
    </div>
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
    areas,
    markers,
    reservations,
    isLoading,
    isSeatsLoading,
    isReservationsLoading,
    createReservation,
    blockSeats,
    deleteReservations,
    exportCsv,
    exportPdf,
    resendConfirmationEmail,
  } = useManagementReservations(eventId);

  const [mode, setMode] = useState<ActionMode>("view");
  const [selectedSeats, setSelectedSeats] = useState<SeatDto[]>([]);
  const [reserveUserId, setReserveUserId] = useState("");
  const [deductAllowance, setDeductAllowance] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
  const [isDeletingSelected, setIsDeletingSelected] = useState(false);
  const [deletingIds, setDeletingIds] = useState<Set<string>>(new Set());
  const [searchQuery, setSearchQuery] = useState("");
  const [highlightedSeatId, setHighlightedSeatId] = useState<string | null>(
    null,
  );
  const [exportingFormat, setExportingFormat] = useState<"csv" | "pdf" | null>(
    null,
  );
  const [confirmationUser, setConfirmationUser] = useState<{
    userId: string;
    userName: string;
  } | null>(null);

  const {
    data: confirmationEmailData,
    isLoading: isConfirmationLoading,
    isError: isConfirmationError,
  } = useQuery({
    ...getApiManagerReservationsConfirmationEmailByEventIdByUserIdOptions({
      path: {
        eventId: eventId ?? "",
        userId: confirmationUser?.userId ?? "",
      },
    }),
    enabled: !!eventId && !!confirmationUser?.userId,
  });

  const handleResendConfirmation = async () => {
    if (!eventId || !confirmationUser?.userId) return;
    await resendConfirmationEmail(eventId, confirmationUser.userId);
  };

  const { ref: seatMapColumnRef, height: seatMapColumnHeight } =
    useFillHeight<HTMLDivElement>();

  const event = events.find((e) => e.id === eventId);
  const location = locations.find((l) => l.id === event?.eventLocationId);
  const eventSeats = useMemo(
    () => seats.filter((s) => s.locationId === location?.id),
    [seats, location?.id],
  );
  const seatById = useMemo(() => new Map(seats.map((s) => [s.id, s])), [seats]);
  const seatStatuses = useMemo(
    () =>
      reservations.map((r) => ({
        seatId: r.seatId,
        status: r.status,
      })),
    [reservations],
  );

  const filteredReservations = useMemo(() => {
    const query = searchQuery.trim().toLowerCase();
    if (!query) return reservations;
    return reservations.filter((r) => {
      const username = r.user?.username?.toLowerCase() ?? "";
      const s = r.seatId ? seatById.get(r.seatId) : undefined;
      const seat = `${s?.seatNumber ?? ""} ${s?.seatRow ?? ""}`.toLowerCase();
      return username.includes(query) || seat.includes(query);
    });
  }, [reservations, searchQuery, seatById]);

  const userReservedSeats = useMemo(() => {
    if (mode !== "reserve" || !reserveUserId || !eventId) return [];
    return reservations
      .filter(
        (reservation) =>
          reservation.user?.id?.toString() === reserveUserId &&
          reservation.eventId?.toString() === eventId &&
          reservation.status === "RESERVED",
      )
      .map((reservation) =>
        reservation.seatId ? seatById.get(reservation.seatId) : undefined,
      )
      .filter((seat): seat is SeatDto => seat !== undefined);
  }, [mode, reserveUserId, eventId, reservations, seatById]);

  const resetAction = () => {
    setMode("view");
    setSelectedSeats([]);
    setReserveUserId("");
    setDeductAllowance(true);
    setHighlightedSeatId(null);
  };

  const handleSeatClick = (seatId: string) => {
    setHighlightedSeatId((prev) => (prev === seatId ? null : seatId));
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

  const handleDeleteIds = async (ids: string[]) => {
    if (ids.length === 0) return;
    if (
      !confirm(
        t("management.reservations.deleteConfirm", { count: ids.length }),
      )
    ) {
      return;
    }
    setDeletingIds((prev) => new Set([...prev, ...ids]));
    try {
      await deleteReservations(ids);
      setSelectedIds((prev) => {
        const next = new Set(prev);
        ids.forEach((id) => next.delete(id));
        return next;
      });
    } finally {
      setDeletingIds((prev) => {
        const next = new Set(prev);
        ids.forEach((id) => next.delete(id));
        return next;
      });
    }
  };

  const handleDeleteOne = (id: string) => handleDeleteIds([id]);
  const handleDeleteGroup = (ids: string[]) => handleDeleteIds(ids);

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
    if (selectedIds.size === filteredReservations.length) {
      setSelectedIds(new Set());
    } else {
      setSelectedIds(new Set(filteredReservations.map((r) => r.id ?? "")));
    }
  };

  const isInteractive = mode !== "view";

  let actionColumn;
  if (mode === "view") {
    actionColumn = (
      <ReservationsViewPanel
        reservations={filteredReservations}
        seats={seats}
        isReservationsLoading={isReservationsLoading}
        selectedIds={selectedIds}
        onSelectedIdsChange={setSelectedIds}
        onToggleAll={toggleAll}
        onDeleteOne={handleDeleteOne}
        onDeleteGroup={handleDeleteGroup}
        deletingIds={deletingIds}
        onSearch={setSearchQuery}
        highlightedSeatId={highlightedSeatId}
        onSeatClick={handleSeatClick}
        onViewConfirmation={(userId, userName) =>
          setConfirmationUser({ userId, userName })
        }
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
                          icon: <Trash2 className="h-4 w-4" />,
                          onClick: handleDeleteSelected,
                          variant: "destructive" as const,
                          isLoading: isDeletingSelected,
                        },
                      ]
                    : []),
                  {
                    key: "csv",
                    label: t("management.reservations.exportCsv"),
                    icon: <Download className="h-4 w-4" />,
                    onClick: () => handleExport("csv"),
                    isLoading: exportingFormat === "csv",
                  },
                  {
                    key: "pdf",
                    label: t("management.reservations.exportPdf"),
                    icon: <Download className="h-4 w-4" />,
                    onClick: () => handleExport("pdf"),
                    isLoading: exportingFormat === "pdf",
                  },
                  {
                    key: "block",
                    label: t("management.reservations.blockSeats"),
                    icon: <Ban className="h-4 w-4" />,
                    onClick: handleStartBlock,
                  },
                ]}
              />
              <Button
                onClick={handleStartReserve}
                aria-label={t("management.reservations.newReservation")}
              >
                <Plus className="h-4 w-4" />
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
            <ArrowUp className="h-5 w-5" />
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
              layout="bar"
              areas={areas}
              showSelected={isInteractive}
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
                  seatStatuses={seatStatuses}
                  markers={markers}
                  areas={areas}
                  selectedSeats={isInteractive ? selectedSeats : []}
                  userReservedSeats={userReservedSeats}
                  highlightedSeatId={!isInteractive ? highlightedSeatId : null}
                  onSeatSelect={isInteractive ? handleSeatToggle : () => {}}
                />
              </div>
            )}
          </div>

          <div className="space-y-3">{actionColumn}</div>
        </div>
      )}

      <ReservationConfirmationModal
        open={!!confirmationUser}
        onOpenChange={(open) => !open && setConfirmationUser(null)}
        userName={confirmationUser?.userName ?? ""}
        emailData={confirmationEmailData}
        isLoading={isConfirmationLoading}
        isError={isConfirmationError}
        onResend={handleResendConfirmation}
      />
    </div>
  );
}
