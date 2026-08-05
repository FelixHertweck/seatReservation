"use client";

import { useState } from "react";
import { PenLine, Plus } from "@/components/icons";

import { useT } from "@/lib/i18n/hooks";
import { cn } from "@/lib/utils";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/custom-ui/dialog";
import { Label } from "@/components/custom-ui/label";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/custom-ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import type { useLocationEditorSave } from "@/components/management/location-editor/use-location-editor-save";
import type {
  LocalId,
  LocationEditorState,
} from "@/components/management/location-editor/types";

interface AreaAddDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  state: LocationEditorState;
  autosave: ReturnType<typeof useLocationEditorSave>;
  onAreaCreated: (localId: LocalId) => void;
  onDrawWithSeats: (name: string, seatIds: Set<LocalId>) => void;
}

export function AreaAddDialog({
  open,
  onOpenChange,
  state,
  autosave,
  onAreaCreated,
  onDrawWithSeats,
}: AreaAddDialogProps) {
  const t = useT();
  const [name, setName] = useState("");
  const [seatIds, setSeatIds] = useState<Set<LocalId>>(new Set());

  const reset = () => {
    setName("");
    setSeatIds(new Set());
  };

  const close = () => {
    reset();
    onOpenChange(false);
  };

  const toggleSeat = (localId: LocalId) => {
    const next = new Set(seatIds);
    if (next.has(localId)) next.delete(localId);
    else next.add(localId);
    setSeatIds(next);
  };

  const allSelected =
    state.seats.length > 0 && seatIds.size === state.seats.length;

  const handleApply = () => {
    const trimmed = name.trim();
    if (!trimmed) return;
    const localId = autosave.addArea({ name: trimmed, boundary: [] });
    if (seatIds.size > 0) {
      autosave.assignAreaToSeats(seatIds, localId);
    }
    onAreaCreated(localId);
    close();
  };

  const handleDraw = () => {
    const trimmed = name.trim();
    if (!trimmed) return;
    onDrawWithSeats(trimmed, seatIds);
    close();
  };

  return (
    <Dialog
      open={open}
      onOpenChange={(next) => (next ? onOpenChange(next) : close())}
    >
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>
            {t("management.locationEditor.areas.dialogTitle")}
          </DialogTitle>
          <DialogDescription>
            {t("management.locationEditor.areas.dialogDescription")}
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-3">
          <div className="space-y-1.5">
            <Label>{t("management.locationEditor.areas.nameLabel")}</Label>
            <Input
              value={name}
              onChange={(e) => setName(e.target.value)}
              autoFocus
            />
          </div>

          <div className="space-y-1.5">
            <div className="flex items-center justify-between">
              <Label>
                {t("management.locationEditor.areas.selectSeatsLabel")}
              </Label>
              {state.seats.length > 0 && (
                <button
                  type="button"
                  className="text-xs text-muted-foreground hover:text-foreground"
                  onClick={() =>
                    setSeatIds(
                      allSelected
                        ? new Set()
                        : new Set(state.seats.map((s) => s.localId)),
                    )
                  }
                >
                  {allSelected
                    ? t("management.locationEditor.areas.deselectAll")
                    : t("management.locationEditor.areas.selectAll")}
                </button>
              )}
            </div>
            <div className="max-h-56 space-y-1 overflow-y-auto rounded-md border p-1.5">
              {state.seats.length === 0 ? (
                <p className="p-1.5 text-xs text-muted-foreground">
                  {t("management.locationEditor.seats.emptyList")}
                </p>
              ) : (
                state.seats.map((seat) => (
                  <label
                    key={seat.localId}
                    className={cn(
                      "flex cursor-pointer items-center gap-2 rounded-md px-2 py-1.5 text-xs hover:bg-accent",
                      seatIds.has(seat.localId) && "bg-accent",
                    )}
                  >
                    <Checkbox
                      checked={seatIds.has(seat.localId)}
                      onCheckedChange={() => toggleSeat(seat.localId)}
                    />
                    <span className="flex-1 truncate">
                      <span className="font-medium">{seat.seatNumber}</span>
                      {seat.seatRow && (
                        <span className="text-muted-foreground">
                          {" "}
                          ({seat.seatRow})
                        </span>
                      )}
                    </span>
                  </label>
                ))
              )}
            </div>
          </div>
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={close}>
            {t("management.locationEditor.areas.cancelButton")}
          </Button>
          <Button
            variant="outline"
            onClick={handleDraw}
            disabled={!name.trim()}
          >
            <PenLine className="h-4 w-4" />
            {t("management.locationEditor.areas.drawButton")}
          </Button>
          <Button onClick={handleApply} disabled={!name.trim()}>
            <Plus className="h-4 w-4" />
            {t("management.locationEditor.areas.applyButton")}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
