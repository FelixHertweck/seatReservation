"use client";

import { Fragment, useMemo, useState } from "react";
import { ChevronDown, ChevronRight, Mail, Trash2 } from "lucide-react";
import { useT } from "@/lib/i18n/hooks";
import { cn, formatDateTime } from "@/lib/utils";
import { Badge } from "@/components/ui/badge";
import { Checkbox } from "@/components/ui/checkbox";
import { Button } from "@/components/custom-ui/button";
import { ReservationsTableSkeleton } from "@/components/management/reservations/reservations-table-skeleton";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
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
}: ReservationsTableProps) {
  const t = useT();
  const [collapsedGroups, setCollapsedGroups] = useState<Set<string>>(
    new Set(),
  );

  const seatById = useMemo(() => new Map(seats.map((s) => [s.id, s])), [seats]);

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

  const toggleOne = (id: string) => {
    const next = new Set(selectedIds);
    if (next.has(id)) next.delete(id);
    else next.add(id);
    onSelectedIdsChange(next);
  };

  const toggleGroupCollapsed = (groupKey: string) => {
    const next = new Set(collapsedGroups);
    if (next.has(groupKey)) next.delete(groupKey);
    else next.add(groupKey);
    setCollapsedGroups(next);
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
  const sortedGroupKeys = [...groups.keys()].sort((a, b) =>
    a.localeCompare(b, undefined, { sensitivity: "base" }),
  );

  return (
    <Table className="table-fixed">
      <TableHeader>
        <TableRow>
          <TableHead className="w-10">
            <Checkbox
              checked={
                reservations.length > 0 &&
                selectedIds.size === reservations.length
              }
              onCheckedChange={onToggleAll}
            />
          </TableHead>
          <TableHead className="w-[28%]">
            {t("management.reservations.tableUser")}
          </TableHead>
          <TableHead className="w-[20%]">
            {t("management.reservations.tableSeat")}
          </TableHead>
          <TableHead className="w-[18%]">
            {t("management.reservations.tableStatus")}
          </TableHead>
          <TableHead className="w-[24%]">
            {t("management.reservations.tableDate")}
          </TableHead>
          <TableHead className="w-20 py-2 pl-2 pr-4 text-right" />
        </TableRow>
      </TableHeader>
      <TableBody>
        {sortedGroupKeys.map((groupKey) => {
          const groupReservations = groups.get(groupKey)!;
          const groupIds = groupReservations.map((r) => r.id ?? "");
          const allSelected = groupIds.every((id) => selectedIds.has(id));
          const someSelected =
            !allSelected && groupIds.some((id) => selectedIds.has(id));
          const isCollapsed = collapsedGroups.has(groupKey);
          const isBlockedGroup =
            groupReservations[0]?.status === ReservationStatus.BLOCKED;
          const groupUser = groupReservations.find((r) => r.user?.id)?.user;

          return (
            <Fragment key={groupKey}>
              <TableRow className="bg-muted/30 hover:bg-muted/30">
                <TableCell colSpan={5} className="py-2">
                  <div className="flex items-center gap-2">
                    <button
                      type="button"
                      onClick={() => toggleGroupCollapsed(groupKey)}
                      aria-label={groupKey}
                      className="shrink-0 rounded p-0.5 hover:bg-muted"
                    >
                      {isCollapsed ? (
                        <ChevronRight className="h-4 w-4" />
                      ) : (
                        <ChevronDown className="h-4 w-4" />
                      )}
                    </button>
                    <Checkbox
                      checked={
                        allSelected
                          ? true
                          : someSelected
                            ? "indeterminate"
                            : false
                      }
                      onCheckedChange={() =>
                        toggleGroupSelected(groupIds, allSelected)
                      }
                    />
                    <span className="text-sm font-medium">{groupKey}</span>
                    <Badge variant="secondary">
                      {groupReservations.length}
                    </Badge>
                  </div>
                </TableCell>
                <TableCell className="py-2 pl-2 pr-4 text-right">
                  <div className="flex items-center justify-end gap-2">
                    {!isBlockedGroup && groupUser?.id && onViewConfirmation && (
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
                </TableCell>
              </TableRow>
              {!isCollapsed &&
                groupReservations.map((reservation) => {
                  const date = formatDateTime(reservation.reservationDateTime);
                  const id = reservation.id ?? "";
                  const seatId = reservation.seatId;
                  const seat = seatId ? seatById.get(seatId) : undefined;
                  const isHighlighted =
                    !!seatId && seatId === highlightedSeatId;
                  return (
                    <TableRow
                      key={id}
                      onClick={() => seatId && onSeatClick?.(seatId)}
                      className={cn(
                        seatId && "cursor-pointer",
                        isHighlighted && "bg-primary/10 hover:bg-primary/15",
                      )}
                    >
                      <TableCell>
                        <Checkbox
                          checked={selectedIds.has(id)}
                          onCheckedChange={() => toggleOne(id)}
                        />
                      </TableCell>
                      <TableCell className="truncate">
                        {reservation.user?.username ?? "—"}
                      </TableCell>
                      <TableCell>
                        {seat ? `${seat.seatNumber} (${seat.seatRow})` : "—"}
                      </TableCell>
                      <TableCell>
                        {(() => {
                          const visualStatus = getSeatVisualStatus(
                            reservation.status,
                            reservation.liveStatus,
                          );
                          return (
                            <Badge
                              className={cn(
                                SEAT_STATUS_BG[visualStatus],
                                SEAT_STATUS_TEXT[visualStatus],
                              )}
                            >
                              {t(SEAT_STATUS_LABEL_KEY[visualStatus])}
                            </Badge>
                          );
                        })()}
                      </TableCell>
                      <TableCell className="text-xs text-muted-foreground">
                        {date ? `${date.date} ${date.time}` : "—"}
                      </TableCell>
                      <TableCell className="py-2 pl-2 pr-4 text-right">
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
                      </TableCell>
                    </TableRow>
                  );
                })}
            </Fragment>
          );
        })}
      </TableBody>
    </Table>
  );
}
