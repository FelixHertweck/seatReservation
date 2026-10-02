"use client";

import { useMemo, useRef, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import {
  ArrowUp,
  Plus,
  Ban,
  ChevronDown,
  ChevronUp,
  Download,
  Map as MapIcon,
  Trash2,
  X,
} from "lucide-react";

import { useT } from "@/lib/i18n/hooks";
import { cn } from "@/lib/utils";
import { sanitizeFileName } from "@/lib/utils/filename";
import { PageHeader } from "@/components/page-header";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/custom-ui/button";
import {
  Drawer,
  DrawerContent,
  DrawerHeader,
  DrawerTitle,
} from "@/components/ui/drawer";
import EventSelector from "@/components/common/supervisor/event-selector";
import { OverflowActionBar } from "@/components/common/overflow-action-bar";
import { SearchAndFilter } from "@/components/common/search-and-filter";
import { SeatMap } from "@/components/common/seat-map";
import { SeatMapSkeleton } from "@/components/common/seat-map-skeleton";
import { Skeleton } from "@/components/custom-ui/skeleton";
import SeatmapLegend from "@/components/common/seatmap-legend";
import { ReservationActionPanel } from "@/components/management/reservations/reservation-action-panel";
import { ReservationConfirmationModal } from "@/components/management/reservations/reservation-confirmation-modal";
import { ReservationsTable } from "@/components/management/reservations/reservations-table";
import { ReservationsTableSkeleton } from "@/components/management/reservations/reservations-table-skeleton";
import { useManagementReservations } from "@/hooks/use-management-reservations";
import { useFillHeight } from "@/hooks/use-fill-height";
import { useIsBelowBreakpoint } from "@/hooks/use-mobile";
import { findSeatStatus } from "@/lib/reservationSeat";
import type { ReservationResponseDto, SeatDto } from "@/api";
import { useQuery } from "@tanstack/react-query";
import { getApiManagerReservationsConfirmationEmailByEventIdByUserIdOptions } from "@/api/@tanstack/react-query.gen";

type ActionMode = "view" | "reserve" | "block";

// Matches the `lg:grid-cols-2` breakpoint below: under it the map and the
// list stack, so the page switches to its compact (mobile) layout.
const COMPACT_BREAKPOINT = 1024;
// Fixed drawer trigger bar at the bottom of the compact layout.
const DRAWER_TRIGGER_HEIGHT = 72;

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
  /** Fixed height so the list scrolls inside its card; omit to let the page scroll. */
  height?: number;
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
  height,
}: Readonly<ReservationsViewPanelProps>) {
  return (
    <div className="flex flex-col gap-3" style={{ height }}>
      <SearchAndFilter
        onSearch={onSearch}
        onFilter={() => {}}
        filterOptions={[]}
      />
      <Card
        className={cn(height !== undefined && "min-h-0 flex-1 overflow-y-auto")}
      >
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
    allowances,
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

  // `undefined` until measured on the client; see the placeholder below.
  const compactLayout = useIsBelowBreakpoint(COMPACT_BREAKPOINT);
  const isCompact = !!compactLayout;
  const [isMapExpanded, setIsMapExpanded] = useState(false);
  const [isDrawerOpen, setIsDrawerOpen] = useState(false);
  const mapSectionRef = useRef<HTMLDivElement>(null);

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
    setIsDrawerOpen(false);
  };

  const handleSeatClick = (seatId: string) => {
    const willHighlight = highlightedSeatId !== seatId;
    setHighlightedSeatId(willHighlight ? seatId : null);
    // On the compact layout the map is collapsed below the fold by default,
    // so picking a seat from the list brings the map to it.
    if (isCompact && willHighlight) {
      setIsMapExpanded(true);
      requestAnimationFrame(() =>
        mapSectionRef.current?.scrollIntoView({
          behavior: "smooth",
          block: "start",
        }),
      );
    }
  };

  const handleSeatChipClick = (seatId: string) => {
    setHighlightedSeatId((prev) => (prev === seatId ? null : seatId));
    if (isCompact) setIsDrawerOpen(false);
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
    setHighlightedSeatId(null);
    // Picking the user comes first, so open the form straight away.
    if (isCompact) setIsDrawerOpen(true);
  };

  const handleStartBlock = () => {
    setMode("block");
    setSelectedSeats([]);
    setHighlightedSeatId(null);
  };

  const handleSeatToggle = (seat: SeatDto) => {
    setSelectedSeats((prev) => {
      const isSelected = prev.some((s) => s.id === seat.id);
      if (isSelected) {
        return prev.filter((s) => s.id !== seat.id);
      }
      const seatStatus = findSeatStatus(seat.id, seatStatuses);
      if (seatStatus) {
        return prev;
      }
      return [...prev, seat];
    });
    setHighlightedSeatId((prev) => (prev === seat.id ? null : prev));
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

  const renderActionPanel = (className?: string) => {
    const shared = {
      className,
      selectedSeats,
      highlightedSeatId,
      onSeatChipClick: handleSeatChipClick,
      onSeatRemove: handleSeatToggle,
      isSubmitting,
      onSubmit: handleSubmitAction,
      onCancel: resetAction,
    };
    if (mode === "reserve") {
      return (
        <ReservationActionPanel
          {...shared}
          mode="reserve"
          users={users}
          allowances={allowances}
          userId={reserveUserId}
          onUserIdChange={setReserveUserId}
          userReservedCount={userReservedSeats.length}
          deductAllowance={deductAllowance}
          onDeductAllowanceChange={setDeductAllowance}
        />
      );
    }
    return <ReservationActionPanel {...shared} mode="block" />;
  };

  const viewPanel = (
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
      height={isCompact ? undefined : seatMapColumnHeight}
    />
  );

  const seatMapContent = (
    <>
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
        <SeatMapSkeleton showLegend={false} />
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
            highlightedSeatId={highlightedSeatId}
            onSeatSelect={isInteractive ? handleSeatToggle : () => {}}
            isLoading={isSeatsLoading}
          />
        </div>
      )}
    </>
  );

  const modeTitle =
    mode === "block"
      ? t("management.reservations.blockSeats")
      : t("management.reservations.newReservation");
  const drawerTitle =
    selectedSeats.length > 0
      ? `${modeTitle} (${selectedSeats.length})`
      : modeTitle;

  // Second line of the drawer trigger: what the form still needs / holds.
  const seatCountText =
    selectedSeats.length > 1
      ? t("management.reservations.multipleSeatsSelected", {
          count: selectedSeats.length,
        })
      : t("management.reservations.seatSelected");
  let drawerSubtitle =
    selectedSeats.length > 0
      ? seatCountText
      : t("management.reservations.selectSeatsHint");
  if (mode === "reserve") {
    const reserveUser = users.find((u) => u.id?.toString() === reserveUserId);
    const userText =
      reserveUser?.username ??
      t("management.reservations.selectUserPlaceholder");
    drawerSubtitle =
      selectedSeats.length > 0 ? `${userText} · ${seatCountText}` : userText;
  }

  let content;
  if (!eventId) {
    content = (
      <Card>
        <CardContent className="flex flex-col items-center gap-2 py-12 text-center text-muted-foreground">
          <ArrowUp className="h-5 w-5" />
          {t("management.reservations.selectEventPrompt")}
        </CardContent>
      </Card>
    );
  } else if (compactLayout === undefined) {
    // Layout not measured yet (SSR / first paint): a CSS-only placeholder
    // that already has the shape of either layout, so nothing jumps once the
    // real one is picked.
    content = (
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <div
          className="hidden flex-col lg:flex"
          style={{ height: seatMapColumnHeight }}
        >
          <SeatMapSkeleton showLegend={false} />
        </div>
        <div className="flex flex-col gap-4 lg:gap-3">
          <Skeleton className="h-12 w-full rounded-lg lg:hidden" />
          <Skeleton className="h-10 w-full rounded-md" />
          <Card>
            <ReservationsTableSkeleton />
          </Card>
        </div>
      </div>
    );
  } else if (!isCompact) {
    content = (
      <div className="grid grid-cols-2 gap-4">
        <div
          ref={seatMapColumnRef}
          className="flex flex-col gap-2"
          style={{ height: seatMapColumnHeight }}
        >
          {seatMapContent}
        </div>
        <div className="space-y-3">
          {isInteractive ? renderActionPanel() : viewPanel}
        </div>
      </div>
    );
  } else if (isInteractive) {
    // Selecting seats needs the whole screen for the map; the form lives in
    // a bottom drawer (same pattern as the box office and live view).
    content = (
      <div
        ref={seatMapColumnRef}
        className="flex flex-col gap-2"
        style={{ height: seatMapColumnHeight - DRAWER_TRIGGER_HEIGHT }}
      >
        {seatMapContent}
      </div>
    );
  } else {
    // The map captures touch gestures for panning, so it starts collapsed to
    // keep the list scrollable.
    content = (
      <div className="flex flex-col gap-4">
        <Card ref={mapSectionRef} className="scroll-mt-4 overflow-hidden">
          <button
            type="button"
            onClick={() => setIsMapExpanded((prev) => !prev)}
            aria-expanded={isMapExpanded}
            aria-controls="management-reservations-seat-map"
            className="flex w-full items-center gap-3 px-4 py-3 text-left transition-colors hover:bg-muted/40"
          >
            <MapIcon className="h-4 w-4 shrink-0 text-muted-foreground" />
            <span className="flex-1 text-sm font-medium">
              {t("management.reservations.seatMapTitle")}
            </span>
            <span className="text-xs text-muted-foreground">
              {isMapExpanded
                ? t("management.reservations.hideSeatMap")
                : t("management.reservations.showSeatMap")}
            </span>
            {isMapExpanded ? (
              <ChevronUp className="h-4 w-4 shrink-0 text-muted-foreground" />
            ) : (
              <ChevronDown className="h-4 w-4 shrink-0 text-muted-foreground" />
            )}
          </button>
          {isMapExpanded && (
            <div
              id="management-reservations-seat-map"
              className="flex h-[60vh] min-h-80 flex-col gap-2 border-t p-2"
            >
              {seatMapContent}
            </div>
          )}
        </Card>
        {viewPanel}
      </div>
    );
  }

  return (
    <div className="container mx-auto p-4 sm:p-6">
      <PageHeader
        title={t("management.reservations.title")}
        description={t("management.reservations.description")}
        actions={
          eventId ? (
            <>
              <OverflowActionBar
                actions={[
                  ...(selectedIds.size > 0 && mode === "view"
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
                    disabled: mode !== "view",
                  },
                  {
                    key: "pdf",
                    label: t("management.reservations.exportPdf"),
                    icon: <Download className="h-4 w-4" />,
                    onClick: () => handleExport("pdf"),
                    isLoading: exportingFormat === "pdf",
                    disabled: mode !== "view",
                  },
                  {
                    key: "block",
                    label: (
                      <span className="grid place-items-center">
                        <span
                          className={cn(
                            "col-start-1 row-start-1 inline-flex items-center",
                            mode === "block" ? "visible" : "invisible",
                          )}
                          aria-hidden={mode !== "block"}
                        >
                          {t("common.cancel")}
                        </span>
                        <span
                          className={cn(
                            "col-start-1 row-start-1 inline-flex items-center",
                            mode === "block" ? "invisible" : "visible",
                          )}
                          aria-hidden={mode === "block"}
                        >
                          {t("management.reservations.blockSeats")}
                        </span>
                      </span>
                    ),
                    icon: (
                      <span className="grid place-items-center">
                        <span
                          className={cn(
                            "col-start-1 row-start-1 inline-flex items-center",
                            mode === "block" ? "visible" : "invisible",
                          )}
                          aria-hidden={mode !== "block"}
                        >
                          <X className="h-4 w-4" />
                        </span>
                        <span
                          className={cn(
                            "col-start-1 row-start-1 inline-flex items-center",
                            mode === "block" ? "invisible" : "visible",
                          )}
                          aria-hidden={mode === "block"}
                        >
                          <Ban className="h-4 w-4" />
                        </span>
                      </span>
                    ),
                    onClick: mode === "block" ? resetAction : handleStartBlock,
                    disabled: mode === "reserve",
                  },
                ]}
              />
              <Button
                variant={mode === "reserve" ? "outline" : "default"}
                onClick={mode === "reserve" ? resetAction : handleStartReserve}
                disabled={mode === "block"}
                aria-label={
                  mode === "reserve"
                    ? t("common.cancel")
                    : t("management.reservations.newReservation")
                }
              >
                <span className="grid place-items-center">
                  <span
                    className={cn(
                      "col-start-1 row-start-1 inline-flex items-center gap-2",
                      mode === "reserve" ? "visible" : "invisible",
                    )}
                    aria-hidden={mode !== "reserve"}
                  >
                    <X className="h-4 w-4" />
                    <span className="hidden sm:inline">
                      {t("common.cancel")}
                    </span>
                  </span>
                  <span
                    className={cn(
                      "col-start-1 row-start-1 inline-flex items-center gap-2",
                      mode === "reserve" ? "invisible" : "visible",
                    )}
                    aria-hidden={mode === "reserve"}
                  >
                    <Plus className="h-4 w-4" />
                    <span className="hidden sm:inline">
                      {t("management.reservations.newReservation")}
                    </span>
                  </span>
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

      {content}

      {isCompact && eventId && isInteractive && (
        <>
          <Drawer open={isDrawerOpen} onOpenChange={setIsDrawerOpen}>
            <DrawerContent>
              <DrawerHeader>
                <DrawerTitle>{drawerTitle}</DrawerTitle>
              </DrawerHeader>
              <div className="max-h-[80vh] overflow-y-auto px-4 pb-4">
                {renderActionPanel("border-0 p-0 sm:p-0")}
              </div>
            </DrawerContent>
          </Drawer>

          {!isDrawerOpen && (
            <button
              type="button"
              onClick={() => setIsDrawerOpen(true)}
              className="fixed inset-x-0 bottom-0 z-40 flex flex-col rounded-t-2xl border-t bg-background px-4 pb-[env(safe-area-inset-bottom)] shadow-[0_-4px_16px_rgba(0,0,0,0.12)] transition-colors active:bg-muted"
              style={{
                minHeight: DRAWER_TRIGGER_HEIGHT,
              }}
            >
              <span className="mx-auto mt-2 h-1.5 w-12 shrink-0 rounded-full bg-muted-foreground/30" />
              <span className="flex w-full flex-1 items-center gap-3 py-2 text-left">
                <span className="flex min-w-0 flex-1 flex-col">
                  <span className="truncate text-sm font-semibold">
                    {drawerTitle}
                  </span>
                  <span className="truncate text-xs text-muted-foreground">
                    {drawerSubtitle}
                  </span>
                </span>
                <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-primary text-primary-foreground">
                  <ChevronUp className="h-5 w-5" />
                </span>
              </span>
            </button>
          )}
        </>
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
