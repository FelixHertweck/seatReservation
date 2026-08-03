"use client";

import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/custom-ui/dialog";
import { useT } from "@/lib/i18n/hooks";
import { DetailsPanel } from "@/components/management/location-editor/panels/details-panel";
import type { useLocationEditorSave } from "@/components/management/location-editor/use-location-editor-save";
import type { LocationMeta } from "@/components/management/location-editor/types";

interface DetailsDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  meta: LocationMeta;
  autosave: ReturnType<typeof useLocationEditorSave>;
}

export function DetailsDialog({
  open,
  onOpenChange,
  meta,
  autosave,
}: DetailsDialogProps) {
  const t = useT();

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>
            {t("management.locationEditor.details.title")}
          </DialogTitle>
          <DialogDescription>
            {t("management.locationEditor.details.dialogDescription")}
          </DialogDescription>
        </DialogHeader>
        <DetailsPanel
          meta={meta}
          autosave={autosave}
          onSaved={() => onOpenChange(false)}
        />
      </DialogContent>
    </Dialog>
  );
}
