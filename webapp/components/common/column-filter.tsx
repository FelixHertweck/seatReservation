"use client";

import { useState } from "react";
import { Settings2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { useT } from "@/lib/i18n/hooks";

export interface ColumnConfig {
  key: string;
  label: string;
  defaultVisible?: boolean;
}

interface ColumnFilterProps {
  columns: ColumnConfig[];
  visibleColumns: Set<string>;
  onVisibilityChange: (columnKey: string, visible: boolean) => void;
  onResetColumns: () => void;
}

export function ColumnFilter({
  columns,
  visibleColumns,
  onVisibilityChange,
  onResetColumns,
}: ColumnFilterProps) {
  const t = useT();
  const [isOpen, setIsOpen] = useState(false);

  const visibleCount = columns.filter((col) =>
    visibleColumns.has(col.key),
  ).length;

  return (
    <Popover open={isOpen} onOpenChange={setIsOpen}>
      <PopoverTrigger asChild>
        <Button variant="outline" size="sm" className="h-10">
          <Settings2 className="mr-2 h-4 w-4" />
          {t("columnFilter.columnsButton")}
          {visibleCount < columns.length && (
            <span className="ml-1 text-muted-foreground">
              ({visibleCount}/{columns.length})
            </span>
          )}
        </Button>
      </PopoverTrigger>
      <PopoverContent className="w-56" align="end">
        <div className="space-y-4">
          <div className="space-y-2">
            <h4 className="font-medium text-sm">
              {t("columnFilter.toggleColumns")}
            </h4>
            <div className="space-y-2">
              {columns.map((column) => (
                <div key={column.key} className="flex items-center space-x-2">
                  <Checkbox
                    id={`column-${column.key}`}
                    checked={visibleColumns.has(column.key)}
                    onCheckedChange={(checked) =>
                      onVisibilityChange(column.key, checked as boolean)
                    }
                  />
                  <label
                    htmlFor={`column-${column.key}`}
                    className="text-sm cursor-pointer flex-1"
                  >
                    {column.label}
                  </label>
                </div>
              ))}
            </div>
          </div>
          <div className="pt-2 border-t">
            <Button
              variant="outline"
              size="sm"
              onClick={onResetColumns}
              className="w-full"
            >
              {t("columnFilter.resetButton")}
            </Button>
          </div>
        </div>
      </PopoverContent>
    </Popover>
  );
}
