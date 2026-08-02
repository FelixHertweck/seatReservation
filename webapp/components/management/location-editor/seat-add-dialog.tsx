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
import {
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
} from "@/components/custom-ui/tabs";
import { Label } from "@/components/custom-ui/label";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/custom-ui/button";
import { BulkSeatPanel } from "@/components/management/location-editor/panels/bulk-seat-panel";
import type { useLocationAutosave } from "@/components/management/location-editor/use-location-autosave";
import {
  isCellOccupied,
  type LocationEditorState,
} from "@/components/management/location-editor/types";

interface SeatAddDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  state: LocationEditorState;
  autosave: ReturnType<typeof useLocationAutosave>;
}

function SingleSeatForm({
  state,
  autosave,
}: {
  state: LocationEditorState;
  autosave: ReturnType<typeof useLocationAutosave>;
}) {
  const t = useT();
  const [seatNumber, setSeatNumber] = useState("");
  const [seatRow, setSeatRow] = useState("");
  const [x, setX] = useState("1");
  const [y, setY] = useState("1");

  const nx = Math.max(1, Number.parseInt(x, 10) || 1);
  const ny = Math.max(1, Number.parseInt(y, 10) || 1);
  const occupied = isCellOccupied(state, nx, ny);

  const handleAdd = () => {
    if (!seatNumber.trim() || occupied) return;
    autosave.addSeat({
      seatNumber: seatNumber.trim(),
      seatRow: seatRow.trim(),
      x: nx,
      y: ny,
    });
    setSeatNumber("");
  };

  return (
    <div className="space-y-3">
      <div className="grid grid-cols-2 gap-2">
        <div className="space-y-1.5">
          <Label>{t("management.locationEditor.seats.seatNumberLabel")}</Label>
          <Input
            value={seatNumber}
            onChange={(e) => setSeatNumber(e.target.value)}
          />
        </div>
        <div className="space-y-1.5">
          <Label>{t("management.locationEditor.seats.seatRowLabel")}</Label>
          <Input value={seatRow} onChange={(e) => setSeatRow(e.target.value)} />
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
        disabled={!seatNumber.trim() || occupied}
      >
        <Plus className="h-4 w-4" />
        {t("management.locationEditor.seats.addButton")}
      </Button>
    </div>
  );
}

export function SeatAddDialog({
  open,
  onOpenChange,
  state,
  autosave,
}: SeatAddDialogProps) {
  const t = useT();

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>
            {t("management.locationEditor.seats.addButton")}
          </DialogTitle>
          <DialogDescription>
            {t("management.locationEditor.seats.dialogDescription")}
          </DialogDescription>
        </DialogHeader>
        <Tabs defaultValue="single">
          <TabsList className="grid w-full grid-cols-2">
            <TabsTrigger value="single">
              {t("management.locationEditor.seats.singleTab")}
            </TabsTrigger>
            <TabsTrigger value="series">
              {t("management.locationEditor.seats.seriesTab")}
            </TabsTrigger>
          </TabsList>
          <TabsContent value="single">
            <SingleSeatForm state={state} autosave={autosave} />
          </TabsContent>
          <TabsContent value="series">
            <BulkSeatPanel state={state} autosave={autosave} />
          </TabsContent>
        </Tabs>
      </DialogContent>
    </Dialog>
  );
}
