"use client";

import { Fragment, useMemo, useState } from "react";
import {
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
}: Readonly<ReservationsTableProps>) {
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
    <Table className="min-w-[500px]">
      <TableHeader>
        <TableRow>
          <TableHead className="w-10 pl-3 pr-1">
            <Checkbox
              checked={
                reservations.length > 0 &&
                selectedIds.size === reservations.length
              }
              onCheckedChange={onToggleAll}
            />
          </TableHead>
          <TableHead>{t("management.reservations.tableSeat")}</TableHead>
          <TableHead className="w-28">
            {t("management.reservations.tableStatus")}
          </TableHead>
          <TableHead className="w-36">
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
          let groupChecked: boolean | "indeterminate" = false;
          if (allSelected) {
            groupChecked = true;
          } else if (someSelected) {
            groupChecked = "indeterminate";
          }

          return (
            <Fragment key={groupKey}>
              <TableRow className="bg-muted/40 hover:bg-muted/50 transition-colors border-b">
                <TableCell colSpan={4} className="py-2.5 pl-3 pr-2">
                  <div className="flex items-center gap-2">
                    <button
                      type="button"
                      onClick={() => toggleGroupCollapsed(groupKey)}
                      aria-label={groupKey}
                      className="shrink-0 rounded p-1 text-muted-foreground hover:text-foreground hover:bg-muted transition-colors"
                    >
                      {isCollapsed ? (
                        <ChevronRight className="h-4 w-4" />
                      ) : (
                        <ChevronDown className="h-4 w-4" />
                      )}
                    </button>
                    <Checkbox
                      checked={groupChecked}
                      onCheckedChange={() =>
                        toggleGroupSelected(groupIds, allSelected)
                      }
                    />
                    <div className="flex items-center gap-1.5 font-medium text-sm">
                      {isBlockedGroup ? (
                        <Ban className="h-3.5 w-3.5 text-muted-foreground" />
                      ) : (
                        <User className="h-3.5 w-3.5 text-muted-foreground" />
                      )}
                      <span>{groupKey}</span>
                    </div>
                    <Badge
                      variant="secondary"
                      className="px-1.5 py-0 text-xs font-normal"
                    >
                      {groupReservations.length}
                    </Badge>
                  </div>
                </TableCell>
                <TableCell className="py-2.5 pl-2 pr-4 text-right">
                  <div className="flex items-center justify-end gap-1.5">
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
                        "hover:bg-muted/30 transition-colors",
                      )}
                    >
                      <TableCell className="py-2 pl-9 pr-1">
                        <Checkbox
                          checked={selectedIds.has(id)}
                          onCheckedChange={() => toggleOne(id)}
                        />
                      </TableCell>
                      <TableCell className="py-2 px-3">
                        <div className="flex items-center gap-1.5">
                          <span className="font-medium text-sm">
                            {seat?.seatNumber ?? "—"}
                          </span>
                          {seat?.seatRow && (
                            <span className="text-xs text-muted-foreground whitespace-nowrap">
                              ({seat.seatRow})
                            </span>
                          )}
                        </div>
                      </TableCell>
                      <TableCell className="py-2 px-3">
                        {(() => {
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
                        })()}
                      </TableCell>
                      <TableCell className="py-2 px-3 text-xs text-muted-foreground whitespace-nowrap tabular-nums">
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
