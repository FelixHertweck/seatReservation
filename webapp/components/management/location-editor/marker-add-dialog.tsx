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
import type { useLocationAutosave } from "@/components/management/location-editor/use-location-autosave";
import {
  isCellOccupied,
  type LocationEditorState,
} from "@/components/management/location-editor/types";

interface MarkerAddDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  state: LocationEditorState;
  autosave: ReturnType<typeof useLocationAutosave>;
}

export function MarkerAddDialog({
  open,
  onOpenChange,
  state,
  autosave,
}: MarkerAddDialogProps) {
  const t = useT();
  const [label, setLabel] = useState("");
  const [x, setX] = useState("1");
  const [y, setY] = useState("1");

  const nx = Math.max(1, Number.parseInt(x, 10) || 1);
  const ny = Math.max(1, Number.parseInt(y, 10) || 1);
  const occupied = isCellOccupied(state, nx, ny);

  const handleAdd = () => {
    if (!label.trim() || occupied) return;
    autosave.addMarker({
      label: label.trim(),
      x: nx,
      y: ny,
    });
    setLabel("");
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>
            {t("management.locationEditor.markers.addButton")}
          </DialogTitle>
          <DialogDescription>
            {t("management.locationEditor.markers.dialogDescription")}
          </DialogDescription>
        </DialogHeader>
        <div className="space-y-3">
          <div className="grid grid-cols-3 gap-2">
            <div className="col-span-1 space-y-1.5">
              <Label>{t("management.locationEditor.markers.labelLabel")}</Label>
              <Input value={label} onChange={(e) => setLabel(e.target.value)} />
            </div>
            <div className="space-y-1.5">
              <Label>{t("management.locationEditor.seats.xLabel")}</Label>
              <Input
                type="number"
                min={1}
                value={x}
                onChange={(e) => setX(e.target.value)}
              />
            </div>
            <div className="space-y-1.5">
              <Label>{t("management.locationEditor.seats.yLabel")}</Label>
              <Input
                type="number"
                min={1}
                value={y}
                onChange={(e) => setY(e.target.value)}
              />
            </div>
          </div>
          {occupied && (
            <p className="text-xs text-destructive">
              {t("management.locationEditor.positionOccupied")}
            </p>
          )}
          <Button
            size="sm"
            className="w-full"
            onClick={handleAdd}
            disabled={!label.trim() || occupied}
          >
            <Plus className="h-4 w-4" />
            {t("management.locationEditor.markers.addButton")}
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
