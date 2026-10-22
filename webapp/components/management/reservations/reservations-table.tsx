"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import {
  ArrowDown,
  ArrowUp,
  ArrowUpDown,
  Ban,
  ChevronDown,
  ChevronRight,
  Mail,
  Trash2,
  User,
} from "lucide-react";
import { useT } from "@/lib/i18n/hooks";
import { cn, formatDateTime } from "@/lib/utils";
import { Badge } from "@/components/ui/badge";
import { Checkbox } from "@/components/ui/checkbox";
import { Button } from "@/components/custom-ui/button";
import { ReservationsTableSkeleton } from "@/components/management/reservations/reservations-table-skeleton";
import {
  ReservationStatus,
  type ReservationResponseDto,
  type SeatDto,
} from "@/api";
import {
  SEAT_STATUS_BG,
  SEAT_STATUS_LABEL_KEY,
  SEAT_STATUS_TEXT,
  getSeatVisualStatus,
} from "@/lib/seatStatusStyles";

interface ReservationsTableProps {
  reservations: ReservationResponseDto[];
  seats?: SeatDto[];
  isLoading: boolean;
  selectedIds: Set<string>;
  onSelectedIdsChange: (next: Set<string>) => void;
  onToggleAll: () => void;
  onDeleteOne: (id: string) => void;
  onDeleteGroup: (ids: string[]) => void;
  deletingIds: Set<string>;
  highlightedSeatId?: string | null;
  onSeatClick?: (seatId: string) => void;
  onViewConfirmation?: (userId: string, userName: string) => void;
}

const UNKNOWN_USER_KEY = "—";

// Shared column layout so user headers and reservation rows line up.
const ROW_GRID =
  "grid grid-cols-[2.5rem_minmax(0,1fr)_auto] @xl:grid-cols-[2.5rem_minmax(0,1fr)_7rem_11rem_6rem] items-center gap-x-2";

type SortKey = "user" | "status" | "date";
type SortDirection = "asc" | "desc";

const timeOf = (r: ReservationResponseDto) =>
  r.reservationDateTime ? new Date(r.reservationDateTime).getTime() : 0;

function ReservationStatusBadge({
  reservation,
}: Readonly<{ reservation: ReservationResponseDto }>) {
  const t = useT();
  const visualStatus = getSeatVisualStatus(
    reservation.status,
    reservation.liveStatus,
  );
  return (
    <Badge
      className={cn(
        "whitespace-nowrap px-2 py-0.5 text-xs font-medium",
        SEAT_STATUS_BG[visualStatus],
        SEAT_STATUS_TEXT[visualStatus],
      )}
    >
      {t(SEAT_STATUS_LABEL_KEY[visualStatus])}
    </Badge>
  );
}

function SortableHead({
  label,
  sortKey,
  active,
  direction,
  onSort,
  className,
}: Readonly<{
  label: string;
  sortKey: SortKey;
  active: SortKey;
  direction: SortDirection;
  onSort: (key: SortKey) => void;
  className?: string;
}>) {
  const isActive = active === sortKey;
  let Icon = ArrowUpDown;
  if (isActive) Icon = direction === "asc" ? ArrowUp : ArrowDown;
  return (
    <div className={className}>
      <button
        type="button"
        onClick={() => onSort(sortKey)}
        className="inline-flex items-center gap-1 hover:text-foreground transition-colors"
      >
        {label}
        <Icon
          className={cn("h-3.5 w-3.5", !isActive && "text-muted-foreground/50")}
        />
      </button>
    </div>
  );
}

export function ReservationsTable({
  reservations,
  seats = [],
  isLoading,
  selectedIds,
  onSelectedIdsChange,
  onToggleAll,
  onDeleteOne,
  onDeleteGroup,
  deletingIds,
  highlightedSeatId = null,
  onSeatClick,
  onViewConfirmation,
}: Readonly<ReservationsTableProps>) {
  const t = useT();
  const [expandedGroups, setExpandedGroups] = useState<Set<string>>(new Set());
  const [sortKey, setSortKey] = useState<SortKey>("user");
  const [sortDirection, setSortDirection] = useState<SortDirection>("asc");

  const seatById = useMemo(() => new Map(seats.map((s) => [s.id, s])), [seats]);
  const rootRef = useRef<HTMLDivElement>(null);

  // Picking a seat on the map opens the holder's block (adjusted during render, not in an effect).
  const [lastHighlightedSeatId, setLastHighlightedSeatId] = useState<
    string | null
  >(null);
  if (highlightedSeatId !== lastHighlightedSeatId) {
    setLastHighlightedSeatId(highlightedSeatId);
    const match = highlightedSeatId
      ? reservations.find((r) => r.seatId === highlightedSeatId)
      : undefined;
    if (match) {
      const groupKey =
        match.status === "BLOCKED"
          ? t("seatStatus.blocked")
          : (match.user?.username ?? UNKNOWN_USER_KEY);
      setExpandedGroups((prev) => new Set(prev).add(groupKey));
    }
  }

  useEffect(() => {
    if (!highlightedSeatId) return;
    const frame = requestAnimationFrame(() =>
      rootRef.current
        ?.querySelector(`[data-seat-id="${CSS.escape(highlightedSeatId)}"]`)
        ?.scrollIntoView({ behavior: "smooth", block: "nearest" }),
    );
    return () => cancelAnimationFrame(frame);
  }, [highlightedSeatId]);

  if (isLoading) {
    return <ReservationsTableSkeleton rowCount={6} />;
  }

  if (reservations.length === 0) {
    return (
      <p className="p-6 text-center text-sm text-muted-foreground">
        {t("management.reservations.empty")}
      </p>
    );
  }

  const handleSort = (key: SortKey) => {
    if (key === sortKey) {
      setSortDirection((d) => (d === "asc" ? "desc" : "asc"));
    } else {
      setSortKey(key);
      setSortDirection("asc");
    }
  };

  const statusLabelOf = (r: ReservationResponseDto) =>
    t(SEAT_STATUS_LABEL_KEY[getSeatVisualStatus(r.status, r.liveStatus)]);

  const seatLabelOf = (r: ReservationResponseDto) => {
    const seat = r.seatId ? seatById.get(r.seatId) : undefined;
    return `${seat?.seatRow ?? ""} ${seat?.seatNumber ?? ""}`.trim();
  };

  const compareText = (a: string, b: string) =>
    a.localeCompare(b, undefined, { sensitivity: "base", numeric: true });

  const direction = sortDirection === "asc" ? 1 : -1;

  const toggleOne = (id: string) => {
    const next = new Set(selectedIds);
    if (next.has(id)) next.delete(id);
    else next.add(id);
    onSelectedIdsChange(next);
  };

  const toggleGroupCollapsed = (groupKey: string) => {
    const next = new Set(expandedGroups);
    if (next.has(groupKey)) next.delete(groupKey);
    else next.add(groupKey);
    setExpandedGroups(next);
  };

  const toggleGroupSelected = (ids: string[], allSelected: boolean) => {
    const next = new Set(selectedIds);
    if (allSelected) {
      ids.forEach((id) => next.delete(id));
    } else {
      ids.forEach((id) => next.add(id));
    }
    onSelectedIdsChange(next);
  };

  const groups = new Map<string, ReservationResponseDto[]>();
  for (const reservation of reservations) {
    const key =
      reservation.status === "BLOCKED"
        ? t("seatStatus.blocked")
        : (reservation.user?.username ?? UNKNOWN_USER_KEY);
    const existing = groups.get(key);
    if (existing) {
      existing.push(reservation);
    } else {
      groups.set(key, [reservation]);
    }
  }

  // A group is represented by its most recent reservation (status + date columns).
  const latestOf = new Map<string, ReservationResponseDto>();
  for (const [key, list] of groups) {
    latestOf.set(
      key,
      list.reduce((latest, r) => (timeOf(r) > timeOf(latest) ? r : latest)),
    );
  }

  const compareSeats = (a: ReservationResponseDto, b: ReservationResponseDto) =>
    compareText(seatLabelOf(a), seatLabelOf(b));

  const sortedGroupKeys = [...groups.keys()].sort((a, b) => {
    let result = 0;
    if (sortKey === "status") {
      result = compareText(
        statusLabelOf(latestOf.get(a)!),
        statusLabelOf(latestOf.get(b)!),
      );
    } else if (sortKey === "date") {
      result = timeOf(latestOf.get(a)!) - timeOf(latestOf.get(b)!);
    }
    if (result === 0) result = compareText(a, b);
    return result * direction;
  });

  const sortRows = (list: ReservationResponseDto[]) =>
    [...list].sort((a, b) => {
      let result = 0;
      if (sortKey === "status") {
        result = compareText(statusLabelOf(a), statusLabelOf(b));
      } else if (sortKey === "date") {
        result = timeOf(a) - timeOf(b);
      }
      if (result === 0) result = compareSeats(a, b);
      return result * direction;
    });

  return (
    <div ref={rootRef} className="@container">
      <div
        className={cn(
          ROW_GRID,
          "sticky top-0 z-10 border-b bg-card py-2 text-xs font-medium text-muted-foreground",
        )}
      >
        <Checkbox
          className="ml-3"
          checked={
            reservations.length > 0 && selectedIds.size === reservations.length
          }
          onCheckedChange={onToggleAll}
        />
        <div className="flex flex-wrap items-center gap-x-4 gap-y-1">
          <SortableHead
            label={t("management.reservations.tableUser")}
            sortKey="user"
            active={sortKey}
            direction={sortDirection}
            onSort={handleSort}
          />
          <SortableHead
            label={t("management.reservations.tableStatus")}
            sortKey="status"
            active={sortKey}
            direction={sortDirection}
            onSort={handleSort}
            className="@xl:hidden"
          />
          <SortableHead
            label={t("management.reservations.tableDate")}
            sortKey="date"
            active={sortKey}
            direction={sortDirection}
            onSort={handleSort}
            className="@xl:hidden"
          />
        </div>
        <SortableHead
          label={t("management.reservations.tableStatus")}
          sortKey="status"
          active={sortKey}
          direction={sortDirection}
          onSort={handleSort}
          className="hidden @xl:block"
        />
        <SortableHead
          label={t("management.reservations.tableDate")}
          sortKey="date"
          active={sortKey}
          direction={sortDirection}
          onSort={handleSort}
          className="hidden @xl:block"
        />
        <span />
      </div>
      <div className="space-y-3 p-3">
        {sortedGroupKeys.map((groupKey) => {
          const groupReservations = groups.get(groupKey)!;
          const groupIds = groupReservations.map((r) => r.id ?? "");
          const allSelected = groupIds.every((id) => selectedIds.has(id));
          const someSelected =
            !allSelected && groupIds.some((id) => selectedIds.has(id));
          const isCollapsed = !expandedGroups.has(groupKey);
          const isBlockedGroup =
            groupReservations[0]?.status === ReservationStatus.BLOCKED;
          const latest = latestOf.get(groupKey)!;
          const latestDate = formatDateTime(latest.reservationDateTime);
          const latestText = latestDate
            ? `${t("management.reservations.latest")} ${latestDate.date} ${latestDate.time}`
            : "—";
          const groupUser = groupReservations.find((r) => r.user?.id)?.user;
          let groupChecked: boolean | "indeterminate" = false;
          if (allSelected) {
            groupChecked = true;
          } else if (someSelected) {
            groupChecked = "indeterminate";
          }

          return (
            <div
              key={groupKey}
              className="overflow-hidden rounded-lg border bg-card shadow-xs"
            >
              <div className={cn(ROW_GRID, "bg-muted/60 py-2.5")}>
                <Checkbox
                  className="ml-3"
                  checked={groupChecked}
                  onCheckedChange={() =>
                    toggleGroupSelected(groupIds, allSelected)
                  }
                />
                <div className="min-w-0">
                  <div className="flex min-w-0 items-center gap-2">
                    <button
                      type="button"
                      onClick={() => toggleGroupCollapsed(groupKey)}
                      aria-label={groupKey}
                      className="shrink-0 rounded p-1 text-muted-foreground hover:bg-muted hover:text-foreground transition-colors"
                    >
                      {isCollapsed ? (
                        <ChevronRight className="h-4 w-4" />
                      ) : (
                        <ChevronDown className="h-4 w-4" />
                      )}
                    </button>
                    {isBlockedGroup ? (
                      <Ban className="h-4 w-4 shrink-0 text-muted-foreground" />
                    ) : (
                      <User className="h-4 w-4 shrink-0 text-primary" />
                    )}
                    <span
                      className="truncate text-sm font-semibold"
                      title={groupKey}
                    >
                      {groupKey}
                    </span>
                    <Badge
                      variant="secondary"
                      className="px-1.5 py-0 text-xs font-normal"
                    >
                      {groupReservations.length}
                    </Badge>
                  </div>
                  <div className="mt-1 flex flex-wrap items-center gap-x-2 gap-y-1 pl-8 @xl:hidden">
                    <ReservationStatusBadge reservation={latest} />
                    <span className="text-xs text-muted-foreground tabular-nums">
                      {latestText}
                    </span>
                  </div>
                </div>
                <div className="hidden @xl:block">
                  <ReservationStatusBadge reservation={latest} />
                </div>
                <div className="hidden text-xs text-muted-foreground whitespace-nowrap tabular-nums @xl:block">
                  {latestText}
                </div>
                <div className="flex items-center justify-end gap-1.5 pr-2 @xl:pr-4">
                  {!isBlockedGroup &&
                    !groupReservations.some((r) => r.boxOffice) &&
                    groupUser?.id &&
                    onViewConfirmation && (
                      <Button
                        variant="outline"
                        size="sm"
                        title={t("management.reservations.viewConfirmation")}
                        aria-label={t(
                          "management.reservations.viewConfirmation",
                        )}
                        onClick={(e) => {
                          e.stopPropagation();
                          onViewConfirmation(
                            groupUser.id!.toString(),
                            groupKey,
                          );
                        }}
                      >
                        <Mail className="h-4 w-4" />
                      </Button>
                    )}
                  <Button
                    variant="destructive"
                    size="sm"
                    aria-label={t("management.reservations.deleteUserGroup")}
                    isLoading={groupIds.some((id) => deletingIds.has(id))}
                    onClick={() => onDeleteGroup(groupIds)}
                  >
                    <Trash2 className="h-4 w-4" />
                  </Button>
                </div>
              </div>
              {!isCollapsed &&
                sortRows(groupReservations).map((reservation) => {
                  const date = formatDateTime(reservation.reservationDateTime);
                  const dateText = date ? `${date.date} ${date.time}` : "—";
                  const id = reservation.id ?? "";
                  const seatId = reservation.seatId;
                  const seat = seatId ? seatById.get(seatId) : undefined;
                  const isHighlighted =
                    !!seatId && seatId === highlightedSeatId;
                  return (
                    <div
                      key={id}
                      data-seat-id={seatId}
                      onClick={() => seatId && onSeatClick?.(seatId)}
                      className={cn(
                        ROW_GRID,
                        "border-t py-2 hover:bg-muted/30 transition-colors",
                        seatId && "cursor-pointer",
                        isHighlighted && "bg-primary/10 hover:bg-primary/15",
                      )}
                    >
                      <Checkbox
                        className="ml-3"
                        checked={selectedIds.has(id)}
                        onClick={(e) => e.stopPropagation()}
                        onCheckedChange={() => toggleOne(id)}
                      />
                      <div className="min-w-0 @xl:pl-9">
                        <div className="flex items-center gap-1.5">
                          <span className="text-sm font-medium">
                            {seat?.seatNumber ?? "—"}
                          </span>
                          {seat?.seatRow && (
                            <span
                              className="whitespace-nowrap text-xs text-muted-foreground"
                              title={seat.seatRow}
                            >
                              ({seat.seatRow})
                            </span>
                          )}
                        </div>
                        <div className="mt-1 flex flex-wrap items-center gap-x-2 gap-y-1 @xl:hidden">
                          <ReservationStatusBadge reservation={reservation} />
                          <span className="text-xs text-muted-foreground tabular-nums">
                            {dateText}
                          </span>
                        </div>
                      </div>
                      <div className="hidden @xl:block">
                        <ReservationStatusBadge reservation={reservation} />
                      </div>
                      <div className="hidden text-xs text-muted-foreground whitespace-nowrap tabular-nums @xl:block">
                        {dateText}
                      </div>
                      <div className="flex justify-end pr-2 @xl:pr-4">
                        <Button
                          variant="destructive"
                          size="sm"
                          aria-label={t("management.reservations.deleteOne")}
                          isLoading={deletingIds.has(id)}
                          onClick={(e) => {
                            e.stopPropagation();
                            onDeleteOne(id);
                          }}
                        >
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      </div>
                    </div>
                  );
                })}
            </div>
          );
        })}
      </div>
    </div>
  );
}
