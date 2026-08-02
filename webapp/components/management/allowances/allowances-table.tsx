"use client";

import { Minus, Plus, Trash2 } from "lucide-react";

import { useT } from "@/lib/i18n/hooks";
import { Button } from "@/components/custom-ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Skeleton } from "@/components/custom-ui/skeleton";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import type { EventUserAllowancesDto, UserDto } from "@/api";

interface AllowancesTableProps {
  allowances: EventUserAllowancesDto[];
  users: UserDto[];
  isLoading: boolean;
  selectedIds: Set<string>;
  onSelectedIdsChange: (next: Set<string>) => void;
  onToggleAll: () => void;
  onChangeCount: (allowance: EventUserAllowancesDto, delta: number) => void;
  onDeleteAllowance: (allowance: EventUserAllowancesDto) => void;
  deletingId?: string | null;
}

export function AllowancesTable({
  allowances,
  users,
  isLoading,
  selectedIds,
  onSelectedIdsChange,
  onToggleAll,
  onChangeCount,
  onDeleteAllowance,
  deletingId = null,
}: AllowancesTableProps) {
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

  if (allowances.length === 0) {
    return (
      <p className="p-6 text-center text-sm text-muted-foreground">
        {t("management.allowances.empty")}
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
                allowances.length > 0 && selectedIds.size === allowances.length
              }
              onCheckedChange={onToggleAll}
            />
          </TableHead>
          <TableHead>{t("management.allowances.tableUser")}</TableHead>
          <TableHead>{t("management.allowances.tableCount")}</TableHead>
          <TableHead className="w-10" />
        </TableRow>
      </TableHeader>
      <TableBody>
        {allowances.map((allowance) => {
          const user = users.find((u) => u.id === allowance.userId);
          return (
            <TableRow key={allowance.id}>
              <TableCell>
                <Checkbox
                  checked={selectedIds.has(allowance.id ?? "")}
                  onCheckedChange={() => toggleOne(allowance.id ?? "")}
                />
              </TableCell>
              <TableCell>{user?.username ?? "—"}</TableCell>
              <TableCell>
                <div className="flex items-center gap-2">
                  <Button
                    variant="outline"
                    size="icon"
                    className="h-7 w-7"
                    disabled={(allowance.reservationsAllowedCount ?? 0) <= 0}
                    onClick={() => onChangeCount(allowance, -1)}
                  >
                    <Minus className="h-3.5 w-3.5" />
                  </Button>
                  <span className="w-6 text-center tabular-nums">
                    {allowance.reservationsAllowedCount ?? 0}
                  </span>
                  <Button
                    variant="outline"
                    size="icon"
                    className="h-7 w-7"
                    onClick={() => onChangeCount(allowance, 1)}
                  >
                    <Plus className="h-3.5 w-3.5" />
                  </Button>
                </div>
              </TableCell>
              <TableCell>
                <Button
                  variant="destructive"
                  size="icon"
                  className="h-7 w-7"
                  onClick={() => onDeleteAllowance(allowance)}
                  isLoading={deletingId === allowance.id}
                >
                  <Trash2 className="h-3.5 w-3.5" />
                </Button>
              </TableCell>
            </TableRow>
          );
        })}
      </TableBody>
    </Table>
  );
}
