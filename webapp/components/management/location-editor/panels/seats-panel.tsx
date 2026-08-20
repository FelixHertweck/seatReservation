"use client";

import { useState } from "react";
import { Plus, Trash2 } from "lucide-react";

import { useT } from "@/lib/i18n/hooks";
import { cn } from "@/lib/utils";
import { Button } from "@/components/custom-ui/button";
import { SeatAddDialog } from "@/components/management/location-editor/seat-add-dialog";
import type { useLocationEditorSave } from "@/components/management/location-editor/use-location-editor-save";
import type {
  LocalId,
  LocationEditorState,
} from "@/components/management/location-editor/types";

interface SeatsPanelProps {
  state: LocationEditorState;
  selection: Set<LocalId>;
  onSelectionChange: (next: Set<LocalId>) => void;
  autosave: ReturnType<typeof useLocationEditorSave>;
}

export function SeatsPanel({
  state,
  selection,
  onSelectionChange,
  autosave,
}: SeatsPanelProps) {
  const t = useT();
  const [dialogOpen, setDialogOpen] = useState(false);

  const [anchorId, setAnchorId] = useState<LocalId | null>(null);

  const handleSeatClick = (seatLocalId: LocalId, e: React.MouseEvent) => {
    if (e.shiftKey && anchorId) {
      const fromIdx = state.seats.findIndex((s) => s.localId === anchorId);
      const toIdx = state.seats.findIndex((s) => s.localId === seatLocalId);
      if (fromIdx !== -1 && toIdx !== -1) {
        const start = Math.min(fromIdx, toIdx);
        const end = Math.max(fromIdx, toIdx);
        const next =
          e.ctrlKey || e.metaKey ? new Set(selection) : new Set<LocalId>();
        for (let i = start; i <= end; i++) {
          next.add(state.seats[i].localId);
        }
        onSelectionChange(next);
        return;
      }
    }

    if (e.ctrlKey || e.metaKey) {
      const next = new Set(selection);
      if (next.has(seatLocalId)) {
        next.delete(seatLocalId);
      } else {
        next.add(seatLocalId);
        setAnchorId(seatLocalId);
      }
      onSelectionChange(next);
      return;
    }

    setAnchorId(seatLocalId);
    onSelectionChange(new Set([seatLocalId]));
  };

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
                onClick={(e) => handleSeatClick(seat.localId, e)}
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
