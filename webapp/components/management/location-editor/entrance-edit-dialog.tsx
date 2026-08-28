"use client";

import { useState } from "react";
import { Check } from "lucide-react";

import { useT } from "@/lib/i18n/hooks";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/custom-ui/dialog";
import { Label } from "@/components/custom-ui/label";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/custom-ui/button";
import type { useLocationEditorSave } from "@/components/management/location-editor/use-location-editor-save";
import type { EditorEntrance } from "@/components/management/location-editor/types";

interface EntranceEditDialogProps {
  entrance: EditorEntrance | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  autosave: ReturnType<typeof useLocationEditorSave>;
}

function EntranceEditForm({
  entrance,
  onClose,
  autosave,
}: Readonly<{
  entrance: EditorEntrance;
  onClose: () => void;
  autosave: ReturnType<typeof useLocationEditorSave>;
}>) {
  const t = useT();
  const [name, setName] = useState(entrance.name);

  const handleSave = () => {
    if (!name.trim()) return;
    autosave.renameEntrance(entrance.localId, name.trim());
    onClose();
  };

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        handleSave();
      }}
      className="space-y-4"
    >
      <div className="space-y-2">
        <Label>{t("management.locationEditor.entrances.nameLabel")}</Label>
        <Input
          value={name}
          onChange={(e) => setName(e.target.value)}
          autoFocus
        />
      </div>
      <Button
        type="submit"
        size="sm"
        className="w-full"
        disabled={!name.trim() || name.trim() === entrance.name}
      >
        <Check className="h-4 w-4" />
        {t("common.save")}
      </Button>
    </form>
  );
}

export function EntranceEditDialog({
  entrance,
  open,
  onOpenChange,
  autosave,
}: Readonly<EntranceEditDialogProps>) {
  const t = useT();

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>
            {t("management.locationEditor.entrances.editButton")}
          </DialogTitle>
          <DialogDescription>
            {t("management.locationEditor.entrances.editDialogDescription")}
          </DialogDescription>
        </DialogHeader>
        {entrance && (
          <EntranceEditForm
            key={entrance.localId}
            entrance={entrance}
            onClose={() => onOpenChange(false)}
            autosave={autosave}
          />
        )}
      </DialogContent>
    </Dialog>
  );
}
