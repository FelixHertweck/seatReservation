"use client";

import { useState } from "react";
import { Pencil, Plus, Trash2 } from "lucide-react";

import { useT } from "@/lib/i18n/hooks";
import { Button } from "@/components/custom-ui/button";
import { EntranceAddDialog } from "@/components/management/location-editor/entrance-add-dialog";
import { EntranceEditDialog } from "@/components/management/location-editor/entrance-edit-dialog";
import type { useLocationEditorSave } from "@/components/management/location-editor/use-location-editor-save";
import type {
  EditorEntrance,
  LocalId,
  LocationEditorState,
} from "@/components/management/location-editor/types";

interface EntrancesPanelProps {
  state: LocationEditorState;
  autosave: ReturnType<typeof useLocationEditorSave>;
}

export function EntrancesPanel({
  state,
  autosave,
}: Readonly<EntrancesPanelProps>) {
  const t = useT();
  const [addDialogOpen, setAddDialogOpen] = useState(false);
  const [editingEntrance, setEditingEntrance] = useState<EditorEntrance | null>(
    null,
  );

  const referencedCount = (entranceLocalId: LocalId) =>
    state.seats.filter((s) => s.entranceRef === entranceLocalId).length;

  return (
    <div className="space-y-3">
      <Button
        size="sm"
        variant="outline"
        className="w-full"
        onClick={() => setAddDialogOpen(true)}
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
                <span className="flex-1 truncate" title={entrance.name}>
                  {entrance.name}
                  {count > 0 && (
                    <span className="ml-1.5 text-[10px] text-muted-foreground">
                      ({count})
                    </span>
                  )}
                </span>
                <div className="flex items-center gap-1">
                  <Button
                    variant="ghost"
                    size="sm"
                    className="h-6 w-6 p-0"
                    title={t("management.locationEditor.entrances.editButton")}
                    onClick={() => setEditingEntrance(entrance)}
                  >
                    <Pencil className="h-3.5 w-3.5" />
                  </Button>
                  <Button
                    variant="ghost"
                    size="sm"
                    className="h-6 w-6 p-0 text-muted-foreground hover:text-destructive"
                    title={t(
                      "management.locationEditor.entrances.deleteConfirm",
                      { name: entrance.name },
                    )}
                    onClick={() =>
                      autosave.deleteEntrances(new Set([entrance.localId]))
                    }
                  >
                    <Trash2 className="h-3.5 w-3.5" />
                  </Button>
                </div>
              </div>
            );
          })
        )}
      </div>

      <EntranceAddDialog
        open={addDialogOpen}
        onOpenChange={setAddDialogOpen}
        autosave={autosave}
      />

      <EntranceEditDialog
        entrance={editingEntrance}
        open={!!editingEntrance}
        onOpenChange={(open) => {
          if (!open) setEditingEntrance(null);
        }}
        autosave={autosave}
      />
    </div>
  );
}
