"use client";

import { useState } from "react";
import { Plus, Trash2 } from "lucide-react";

import { useT } from "@/lib/i18n/hooks";
import { cn } from "@/lib/utils";
import { Button } from "@/components/custom-ui/button";
import { SeatAddDialog } from "@/components/management/location-editor/seat-add-dialog";
import type { useLocationAutosave } from "@/components/management/location-editor/use-location-autosave";
import type {
  LocalId,
  LocationEditorState,
} from "@/components/management/location-editor/types";

interface SeatsPanelProps {
  state: LocationEditorState;
  selection: Set<LocalId>;
  onSelectionChange: (next: Set<LocalId>) => void;
  autosave: ReturnType<typeof useLocationAutosave>;
}

export function SeatsPanel({
  state,
  selection,
  onSelectionChange,
  autosave,
}: SeatsPanelProps) {
  const t = useT();
  const [dialogOpen, setDialogOpen] = useState(false);

  return (
    <div className="space-y-3">
      <Button
        size="sm"
        variant="outline"
        className="w-full"
        onClick={() => setDialogOpen(true)}
      >
        <Plus className="h-4 w-4" />
        {t("management.locationEditor.seats.addButton")}
      </Button>

      <div className="max-h-56 space-y-1 overflow-y-auto">
        {state.seats.length === 0 ? (
          <p className="text-xs text-muted-foreground">
            {t("management.locationEditor.seats.emptyList")}
          </p>
        ) : (
          state.seats.map((seat) => (
            <div
              key={seat.localId}
              className={cn(
                "flex items-center justify-between rounded-md px-2 py-1.5 text-xs transition-colors hover:bg-accent",
                selection.has(seat.localId) && "bg-accent",
              )}
            >
              <button
                type="button"
                onClick={() => onSelectionChange(new Set([seat.localId]))}
                className="flex flex-1 items-center justify-between text-left"
              >
                <span className="font-medium">{seat.seatNumber}</span>
                {seat.seatRow && (
                  <span className="text-muted-foreground">
                    ({seat.seatRow})
                  </span>
                )}
              </button>
              <Button
                variant="ghost"
                size="sm"
                className="ml-1 h-6 w-6 shrink-0 p-0"
                onClick={() => {
                  autosave.deleteEntities(new Set([seat.localId]));
                  if (selection.has(seat.localId)) onSelectionChange(new Set());
                }}
              >
                <Trash2 className="h-3.5 w-3.5" />
              </Button>
            </div>
          ))
        )}
      </div>

      <SeatAddDialog
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        state={state}
        autosave={autosave}
      />
    </div>
  );
}
