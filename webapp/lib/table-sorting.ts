import * as React from "react";
import type { SortDirection } from "@/components/common/sortable-table-head";

export function useSortableData<T>(data: T[]) {
  const [sortKey, setSortKey] = React.useState<string | null>(null);
  const [sortDirection, setSortDirection] = React.useState<SortDirection>(null);

  const handleSort = React.useCallback(
    (key: string) => {
      if (sortKey === key) {
        // Same column clicked - cycle through states
        if (sortDirection === null) {
          setSortDirection("asc");
        } else if (sortDirection === "asc") {
          setSortDirection("desc");
        } else {
          // Back to unsorted
          setSortDirection(null);
          setSortKey(null);
        }
      } else {
        // Different column clicked - start with ascending
        setSortKey(key);
        setSortDirection("asc");
      }
    },
    [sortKey, sortDirection],
  );

  const sortedData = React.useMemo(() => {
    if (!sortKey || !sortDirection) return data;

    return [...data].sort((a, b) => {
      const aValue = getNestedValue(a, sortKey);
      const bValue = getNestedValue(b, sortKey);

      // Handle null/undefined values
      if (aValue == null && bValue == null) return 0;
      if (aValue == null) return 1;
      if (bValue == null) return -1;

      // Compare values
      let comparison = 0;

      if (typeof aValue === "number" && typeof bValue === "number") {
        comparison = aValue - bValue;
      } else if (aValue instanceof Date && bValue instanceof Date) {
        comparison = aValue.getTime() - bValue.getTime();
      } else if (typeof aValue === "bigint" && typeof bValue === "bigint") {
        comparison = aValue < bValue ? -1 : aValue > bValue ? 1 : 0;
      } else if (typeof aValue === "boolean" && typeof bValue === "boolean") {
        comparison = aValue === bValue ? 0 : aValue ? 1 : -1;
      } else if (typeof aValue === "string" && typeof bValue === "string") {
        // Try to parse as numbers first (for strings like "1", "10", "100")
        const aNum = parseFloat(aValue);
        const bNum = parseFloat(bValue);

        if (!isNaN(aNum) && !isNaN(bNum)) {
          // Both are numeric strings - compare as numbers
          comparison = aNum - bNum;
        } else {
          // At least one is not numeric - use natural sort with locale
          comparison = aValue
            .toLowerCase()
            .localeCompare(bValue.toLowerCase(), undefined, {
              numeric: true,
              sensitivity: "base",
            });
        }
      } else {
        // Fallback to string comparison
        comparison = String(aValue).localeCompare(String(bValue), undefined, {
          numeric: true,
          sensitivity: "base",
        });
      }

      return sortDirection === "asc" ? comparison : -comparison;
    });
  }, [data, sortKey, sortDirection]);

  return {
    sortedData,
    sortKey,
    sortDirection,
    handleSort,
  };
}

// Helper function to get nested object values
function getNestedValue(obj: any, path: string): any {
  return path.split(".").reduce((current, key) => current?.[key], obj);
}
