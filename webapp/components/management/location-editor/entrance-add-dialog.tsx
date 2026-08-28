"use client";

import { useState } from "react";
import { Plus } from "lucide-react";

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

interface EntranceAddDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  autosave: ReturnType<typeof useLocationEditorSave>;
}

export function EntranceAddDialog({
  open,
  onOpenChange,
  autosave,
}: EntranceAddDialogProps) {
  const t = useT();
  const [name, setName] = useState("");

  const handleAdd = () => {
    if (!name.trim()) return;
    autosave.addEntrance({ name: name.trim() });
    setName("");
    onOpenChange(false);
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>
            {t("management.locationEditor.entrances.addButton")}
          </DialogTitle>
          <DialogDescription>
            {t("management.locationEditor.entrances.dialogDescription")}
          </DialogDescription>
        </DialogHeader>
        <form
          onSubmit={(e) => {
            e.preventDefault();
            handleAdd();
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
            disabled={!name.trim()}
          >
            <Plus className="h-4 w-4" />
            {t("management.locationEditor.entrances.addButton")}
          </Button>
        </form>
      </DialogContent>
    </Dialog>
  );
}
