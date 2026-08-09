"use client";

import { useState } from "react";
import { Filter, Search } from "lucide-react";
import { useT } from "@/lib/i18n/hooks";
import type { SupervisorReservationResponseDto } from "@/api";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/custom-ui/button";
import {
  DropdownMenu,
  DropdownMenuCheckboxItem,
  DropdownMenuContent,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { cn } from "@/lib/utils";
import {
  SEAT_STATUS_BG,
  SEAT_STATUS_LABEL_KEY,
  SEAT_STATUS_TEXT,
  getSeatVisualStatus,
  type SeatVisualStatus,
} from "@/lib/seatStatusStyles";
import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionTrigger,
} from "@/components/ui/accordion";

interface ReservationListProps {
  reservations: SupervisorReservationResponseDto[];
  isLoading?: boolean;
  highlightedSeatId?: string | null;
  onReservationClick?: (seatId: string) => void;
}

// The only visual statuses a `status === "RESERVED"` reservation can carry
// (see getSeatVisualStatus) - the set this filter offers.
const STATUS_FILTER_OPTIONS: { status: SeatVisualStatus; labelKey: string }[] =
  [
    { status: "RESERVED", labelKey: "liveview.reservations.pending" },
    { status: "CHECKED_IN", labelKey: "liveview.reservations.checkedIn" },
    { status: "NO_SHOW", labelKey: "liveview.reservations.noShow" },
    { status: "CANCELLED", labelKey: "liveview.reservations.cancelled" },
  ];

const displayName = (reservation: SupervisorReservationResponseDto): string =>
  reservation.guestName || reservation.username || `User ${reservation.userId}`;

export function ReservationList({
  reservations,
  isLoading = false,
  highlightedSeatId = null,
  onReservationClick,
}: ReservationListProps) {
  const t = useT();
  const [searchQuery, setSearchQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState<Set<SeatVisualStatus>>(
    new Set(),
  );

  const toggleStatusFilter = (status: SeatVisualStatus) => {
    setStatusFilter((prev) => {
      const next = new Set(prev);
      if (next.has(status)) {
        next.delete(status);
      } else {
        next.add(status);
      }
      return next;
    });
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-8 text-muted-foreground">
        <p className="text-sm">{t("common.loading")}</p>
      </div>
    );
  }

  if (reservations.length === 0) {
    return (
      <div className="flex items-center justify-center py-8 text-muted-foreground">
        <p className="text-sm">{t("liveview.reservations.empty")}</p>
      </div>
    );
  }

  const filteredReservations = reservations.filter(
    (reservation) => reservation.status === "RESERVED",
  );

  // Matches either the reservation holder's name or their seat (number or
  // row), so the same search box works for "who sits where" and "who is
  // this seat" lookups without a separate mode toggle.
  const query = searchQuery.trim().toLowerCase();
  const searchedReservations = query
    ? filteredReservations.filter((reservation) => {
        const name = displayName(reservation).toLowerCase();
        const seatNumber = (reservation.seat?.seatNumber ?? "").toLowerCase();
        const seatRow = (reservation.seat?.seatRow ?? "").toLowerCase();
        return (
          name.includes(query) ||
          seatNumber.includes(query) ||
          seatRow.includes(query)
        );
      })
    : filteredReservations;

  const filteredByStatus =
    statusFilter.size > 0
      ? searchedReservations.filter((reservation) =>
          statusFilter.has(
            getSeatVisualStatus(reservation.status, reservation.liveStatus),
          ),
        )
      : searchedReservations;

  const groups = new Map<string, SupervisorReservationResponseDto[]>();
  for (const reservation of filteredByStatus) {
    const key = displayName(reservation);
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
    <div className="space-y-3">
      <div className="flex items-center gap-2">
        <div className="relative flex-1">
          <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder={t("liveview.reservations.searchPlaceholder")}
            className="pl-8"
          />
        </div>
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="outline" size="icon" className="relative shrink-0">
              <Filter className="h-4 w-4" />
              {statusFilter.size > 0 && (
                <span className="absolute top-0.5 right-0.5 flex h-4 min-w-4 items-center justify-center rounded-full bg-primary px-1 text-[10px] font-medium leading-none text-primary-foreground ring-2 ring-background">
                  {statusFilter.size}
                </span>
              )}
              <span className="sr-only">
                {t("liveview.reservations.filterByStatus")}
              </span>
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="w-56">
            {STATUS_FILTER_OPTIONS.map((option) => (
              <DropdownMenuCheckboxItem
                key={option.status}
                checked={statusFilter.has(option.status)}
                onCheckedChange={() => toggleStatusFilter(option.status)}
                onSelect={(e) => e.preventDefault()}
              >
                {t(option.labelKey)}
              </DropdownMenuCheckboxItem>
            ))}
          </DropdownMenuContent>
        </DropdownMenu>
      </div>

      {sortedGroupKeys.length === 0 ? (
        <div className="flex items-center justify-center py-8 text-muted-foreground">
          <p className="text-sm">{t("liveview.reservations.noResults")}</p>
        </div>
      ) : (
        <Accordion
          type="multiple"
          defaultValue={sortedGroupKeys}
          key={`${query}-${[...statusFilter].sort().join(",")}`}
        >
          {sortedGroupKeys.map((groupKey) => {
            const groupReservations = groups.get(groupKey)!;
            return (
              <AccordionItem key={groupKey} value={groupKey}>
                <AccordionTrigger className="text-sm">
                  <span className="flex items-center gap-2">
                    {groupKey}
                    <Badge variant="secondary">
                      {groupReservations.length}
                    </Badge>
                  </span>
                </AccordionTrigger>
                <AccordionContent>
                  <div className="space-y-2">
                    {groupReservations.map((reservation, index) => {
                      const visualStatus = getSeatVisualStatus(
                        reservation.status,
                        reservation.liveStatus,
                      );
                      const seatId = reservation.seat?.id;
                      const isHighlighted =
                        !!seatId && seatId === highlightedSeatId;
                      const rowClassName = cn(
                        "w-full p-3 border rounded-lg bg-card text-left transition-colors",
                        seatId && "cursor-pointer hover:bg-muted/50",
                        isHighlighted &&
                          "border-primary ring-2 ring-primary bg-primary/10",
                      );
                      const rowContent = (
                        <div className="flex items-center justify-between gap-2">
                          <p className="font-medium text-sm">
                            {reservation.seat?.seatNumber} (
                            {reservation.seat?.seatRow})
                          </p>
                          <Badge
                            className={cn(
                              SEAT_STATUS_BG[visualStatus],
                              SEAT_STATUS_TEXT[visualStatus],
                            )}
                          >
                            {t(SEAT_STATUS_LABEL_KEY[visualStatus])}
                          </Badge>
                        </div>
                      );
                      const key =
                        reservation.id?.toString() || `reservation-${index}`;

                      return seatId ? (
                        <button
                          key={key}
                          type="button"
                          onClick={() => onReservationClick?.(seatId)}
                          className={rowClassName}
                        >
                          {rowContent}
                        </button>
                      ) : (
                        <div key={key} className={rowClassName}>
                          {rowContent}
                        </div>
                      );
                    })}
                  </div>
                </AccordionContent>
              </AccordionItem>
            );
          })}
        </Accordion>
      )}
    </div>
  );
}
