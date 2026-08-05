"use client";

import { DeleteIcon } from "@/components/ui/delete";
import { useState } from "react";
import { PlusIcon } from "@/components/ui/plus";

import { useT } from "@/lib/i18n/hooks";
import { cn } from "@/lib/utils";
import { Button } from "@/components/custom-ui/button";
import { AreaAddDialog } from "@/components/management/location-editor/area-add-dialog";
import type { useLocationEditorSave } from "@/components/management/location-editor/use-location-editor-save";
import type {
  LocalId,
  LocationEditorState,
} from "@/components/management/location-editor/types";

interface AreasPanelProps {
  state: LocationEditorState;
  selectedAreaId: LocalId | null;
  onSelectedAreaChange: (id: LocalId | null) => void;
  autosave: ReturnType<typeof useLocationEditorSave>;
  onDrawWithSeats: (name: string, seatIds: Set<LocalId>) => void;
}

export function AreasPanel({
  state,
  selectedAreaId,
  onSelectedAreaChange,
  autosave,
  onDrawWithSeats,
}: AreasPanelProps) {
  const t = useT();
  const [dialogOpen, setDialogOpen] = useState(false);

  const seatCountFor = (areaLocalId: LocalId) =>
    state.seats.filter((s) => s.areaRef === areaLocalId).length;

  const handleDelete = (areaLocalId: LocalId) => {
    autosave.deleteAreas(new Set([areaLocalId]));
    if (selectedAreaId === areaLocalId) onSelectedAreaChange(null);
  };

  const handleUnassignAndDelete = (areaLocalId: LocalId) => {
    const seatIds = state.seats
      .filter((s) => s.areaRef === areaLocalId)
      .map((s) => s.localId);
    autosave.assignAreaToSeats(new Set(seatIds), undefined);
    autosave.deleteAreas(new Set([areaLocalId]));
    if (selectedAreaId === areaLocalId) onSelectedAreaChange(null);
  };

  return (
    <div className="space-y-3">
      <Button
        size="sm"
        variant="outline"
        className="w-full"
        onClick={() => setDialogOpen(true)}
      >
        <PlusIcon size={16} />
        {t("management.locationEditor.areas.addButton")}
      </Button>

      <div className="max-h-56 space-y-1 overflow-y-auto">
        {state.areas.length === 0 ? (
          <p className="text-xs text-muted-foreground">
            {t("management.locationEditor.areas.emptyList")}
          </p>
        ) : (
          state.areas.map((area) => {
            const referencedCount = seatCountFor(area.localId);
            return (
              <div
                key={area.localId}
                className={cn(
                  "flex items-center justify-between rounded-md px-2 py-1.5 text-xs transition-colors hover:bg-accent",
                  selectedAreaId === area.localId && "bg-accent",
                )}
              >
                <button
                  type="button"
                  className="flex-1 truncate text-left"
                  onClick={() => onSelectedAreaChange(area.localId)}
                >
                  {area.name}
                </button>
                <Button
                  variant="ghost"
                  size="sm"
                  className="h-6 w-6 p-0"
                  title={
                    referencedCount > 0
                      ? t(
                          "management.locationEditor.areas.cannotDeleteReferenced",
                          { count: referencedCount },
                        )
                      : undefined
                  }
                  onClick={() =>
                    referencedCount > 0
                      ? handleUnassignAndDelete(area.localId)
                      : handleDelete(area.localId)
                  }
                >
                  <DeleteIcon size={14} />
                </Button>
              </div>
            );
          })
        )}
      </div>

      <AreaAddDialog
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        state={state}
        autosave={autosave}
        onAreaCreated={onSelectedAreaChange}
        onDrawWithSeats={onDrawWithSeats}
      />
    </div>
  );
}
