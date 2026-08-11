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
import type { UserDto } from "@/api";

interface DetailsDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  meta: LocationMeta;
  autosave: ReturnType<typeof useLocationEditorSave>;
  users: UserDto[];
}

export function DetailsDialog({
  open,
  onOpenChange,
  meta,
  autosave,
  users,
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
          users={users}
          onSaved={() => onOpenChange(false)}
        />
      </DialogContent>
    </Dialog>
  );
}
