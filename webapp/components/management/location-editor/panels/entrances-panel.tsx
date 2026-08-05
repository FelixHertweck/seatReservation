"use client";

import { useState } from "react";
import { Plus, Trash2 } from "@/components/icons";

import { useT } from "@/lib/i18n/hooks";
import { Button } from "@/components/custom-ui/button";
import { EntranceAddDialog } from "@/components/management/location-editor/entrance-add-dialog";
import type { useLocationEditorSave } from "@/components/management/location-editor/use-location-editor-save";
import type {
  LocalId,
  LocationEditorState,
} from "@/components/management/location-editor/types";

interface EntrancesPanelProps {
  state: LocationEditorState;
  autosave: ReturnType<typeof useLocationEditorSave>;
}

export function EntrancesPanel({ state, autosave }: EntrancesPanelProps) {
  const t = useT();
  const [dialogOpen, setDialogOpen] = useState(false);

  const referencedCount = (entranceLocalId: LocalId) =>
    state.seats.filter((s) => s.entranceRef === entranceLocalId).length;

  return (
    <div className="space-y-3">
      <Button
        size="sm"
        variant="outline"
        className="w-full"
        onClick={() => setDialogOpen(true)}
      >
        <Plus className="h-4 w-4" />
        {t("management.locationEditor.entrances.addButton")}
      </Button>

      <div className="max-h-56 space-y-1 overflow-y-auto">
        {state.entrances.length === 0 ? (
          <p className="text-xs text-muted-foreground">
            {t("management.locationEditor.entrances.emptyList")}
          </p>
        ) : (
          state.entrances.map((entrance) => {
            const count = referencedCount(entrance.localId);
            return (
              <div
                key={entrance.localId}
                className="flex items-center justify-between rounded-md px-2 py-1.5 text-xs transition-colors hover:bg-accent"
              >
                <span className="truncate">{entrance.name}</span>
                <Button
                  variant="ghost"
                  size="sm"
                  className="h-6 w-6 p-0"
                  disabled={count > 0}
                  title={
                    count > 0
                      ? t(
                          "management.locationEditor.entrances.cannotDeleteReferenced",
                          { count },
                        )
                      : undefined
                  }
                  onClick={() =>
                    autosave.deleteEntrances(new Set([entrance.localId]))
                  }
                >
                  <Trash2 className="h-3.5 w-3.5" />
                </Button>
              </div>
            );
          })
        )}
      </div>

      <EntranceAddDialog
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        autosave={autosave}
      />
    </div>
  );
}
