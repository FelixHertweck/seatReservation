"use client";

import { useT } from "@/lib/i18n/hooks";
import { formatDateTime } from "@/lib/utils";
import { Badge } from "@/components/ui/badge";
import { Checkbox } from "@/components/ui/checkbox";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import type { ReservationResponseDto } from "@/api";

interface ReservationsTableProps {
  reservations: ReservationResponseDto[];
  isLoading: boolean;
  selectedIds: Set<string>;
  onSelectedIdsChange: (next: Set<string>) => void;
  onToggleAll: () => void;
}

export function ReservationsTable({
  reservations,
  isLoading,
  selectedIds,
  onSelectedIdsChange,
  onToggleAll,
}: ReservationsTableProps) {
  const t = useT();

  if (isLoading) {
    return (
      <div className="space-y-2 p-4">
        {Array.from({ length: 5 }, (_, i) => (
          <Skeleton key={i} className="h-8 w-full" />
        ))}
      </div>
    );
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

  return (
    <Table>
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
          <TableHead>{t("management.reservations.tableUser")}</TableHead>
          <TableHead>{t("management.reservations.tableSeat")}</TableHead>
          <TableHead>{t("management.reservations.tableStatus")}</TableHead>
          <TableHead>{t("management.reservations.tableDate")}</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {reservations.map((reservation) => {
          const date = formatDateTime(reservation.reservationDateTime);
          return (
            <TableRow key={reservation.id}>
              <TableCell>
                <Checkbox
                  checked={selectedIds.has(reservation.id ?? "")}
                  onCheckedChange={() => toggleOne(reservation.id ?? "")}
                />
              </TableCell>
              <TableCell>{reservation.user?.username ?? "—"}</TableCell>
              <TableCell>
                {reservation.seat?.seatRow}
                {reservation.seat?.seatNumber}
              </TableCell>
              <TableCell>
                <Badge
                  variant={
                    reservation.status === "BLOCKED" ? "secondary" : "default"
                  }
                >
                  {reservation.status}
                </Badge>
              </TableCell>
              <TableCell className="text-xs text-muted-foreground">
                {date ? `${date.date} ${date.time}` : "—"}
              </TableCell>
            </TableRow>
          );
        })}
      </TableBody>
    </Table>
  );
}
