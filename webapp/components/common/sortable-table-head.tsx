import * as React from "react";
import { ArrowUpDown, ArrowUp, ArrowDown } from "lucide-react";
import { cn } from "@/lib/utils";

export type SortDirection = "asc" | "desc" | null;

interface SortableTableHeadProps
  extends React.ThHTMLAttributes<HTMLTableCellElement> {
  sortKey?: string;
  currentSortKey?: string | null;
  currentSortDirection?: SortDirection;
  onSort?: (key: string) => void;
  children: React.ReactNode;
}

export function SortableTableHead({
  sortKey,
  currentSortKey,
  currentSortDirection,
  onSort,
  children,
  className,
  ...props
}: SortableTableHeadProps) {
  const isSorted = sortKey && currentSortKey === sortKey;
  const canSort = sortKey && onSort;

  const handleClick = () => {
    if (canSort) {
      onSort(sortKey);
    }
  };

  return (
    <th
      data-slot="table-head"
      className={cn(
        "h-12 px-4 text-left align-middle font-medium text-muted-foreground [&:has([role=checkbox])]:pr-0",
        canSort &&
          "cursor-pointer select-none hover:text-foreground transition-colors",
        className,
      )}
      onClick={handleClick}
      {...props}
    >
      <div className="flex items-center gap-2">
        <span>{children}</span>
        {canSort && (
          <div className="flex-shrink-0 w-4 h-4">
            {!isSorted && <ArrowUpDown className="h-4 w-4 opacity-40" />}
            {isSorted && currentSortDirection === "asc" && (
              <ArrowUp className="h-4 w-4" />
            )}
            {isSorted && currentSortDirection === "desc" && (
              <ArrowDown className="h-4 w-4" />
            )}
          </div>
        )}
      </div>
    </th>
  );
}
