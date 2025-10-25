import { useState, useEffect } from "react";
import type { ColumnConfig } from "@/components/common/column-filter";

export function useColumnVisibility(
  columns: ColumnConfig[],
  storageKey?: string,
) {
  // Initialize visible columns with default values
  const getInitialVisibility = (): Set<string> => {
    if (storageKey && typeof window !== "undefined") {
      const stored = localStorage.getItem(storageKey);
      if (stored) {
        try {
          return new Set(JSON.parse(stored));
        } catch {
          // Fall back to defaults if parsing fails
        }
      }
    }

    return new Set(
      columns
        .filter((col) => col.defaultVisible !== false)
        .map((col) => col.key),
    );
  };

  const [visibleColumns, setVisibleColumns] = useState<Set<string>>(
    getInitialVisibility,
  );

  // Save to localStorage whenever visibility changes
  useEffect(() => {
    if (storageKey && typeof window !== "undefined") {
      localStorage.setItem(
        storageKey,
        JSON.stringify(Array.from(visibleColumns)),
      );
    }
  }, [visibleColumns, storageKey]);

  const toggleColumn = (columnKey: string, visible: boolean) => {
    setVisibleColumns((prev) => {
      const newSet = new Set(prev);
      if (visible) {
        newSet.add(columnKey);
      } else {
        newSet.delete(columnKey);
      }
      return newSet;
    });
  };

  const resetColumns = () => {
    const defaultColumns = new Set(
      columns
        .filter((col) => col.defaultVisible !== false)
        .map((col) => col.key),
    );
    setVisibleColumns(defaultColumns);
  };

  const isColumnVisible = (columnKey: string): boolean => {
    return visibleColumns.has(columnKey);
  };

  return {
    visibleColumns,
    toggleColumn,
    resetColumns,
    isColumnVisible,
  };
}
