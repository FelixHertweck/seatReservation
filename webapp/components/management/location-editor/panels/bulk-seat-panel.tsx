"use client";

import { useMemo, useState } from "react";
import { Layers } from "lucide-react";

import { useT } from "@/lib/i18n/hooks";
import { Label } from "@/components/custom-ui/label";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/custom-ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import type { useLocationEditorSave } from "@/components/management/location-editor/use-location-editor-save";
import type {
  EditorSeat,
  LocationEditorState,
} from "@/components/management/location-editor/types";

interface BulkSeatPanelProps {
  state: LocationEditorState;
  autosave: ReturnType<typeof useLocationEditorSave>;
}

type Layout = "row" | "column" | "block";

const NONE = "__none__";

function generateSeats(params: {
  layout: Layout;
  startX: number;
  startY: number;
  count: number;
  rows: number;
  cols: number;
  spacing: number;
  numberStart: number;
  prefix: string;
  seatRowLabel: string;
  entranceRef?: string;
  areaRef?: string;
  occupied: Set<string>;
}): Omit<EditorSeat, "localId" | "syncState">[] {
  const step = 1 + params.spacing;
  const seats: Omit<EditorSeat, "localId" | "syncState">[] = [];

  const push = (x: number, y: number, num: number, row: string) => {
    const key = `${x}-${y}`;
    if (params.occupied.has(key)) return;
    seats.push({
      seatNumber: `${params.prefix}${num}`,
      seatRow: row,
      x,
      y,
      entranceRef: params.entranceRef,
      areaRef: params.areaRef,
    });
  };

  if (params.layout === "row") {
    for (let i = 0; i < params.count; i++) {
      push(
        params.startX + i * step,
        params.startY,
        params.numberStart + i,
        params.seatRowLabel,
      );
    }
  } else if (params.layout === "column") {
    for (let i = 0; i < params.count; i++) {
      push(
        params.startX,
        params.startY + i * step,
        params.numberStart + i,
        params.seatRowLabel,
      );
    }
  } else {
    for (let r = 0; r < params.rows; r++) {
      const rowLabel = params.seatRowLabel
        ? `${params.seatRowLabel}${String.fromCharCode(65 + r)}`
        : String.fromCharCode(65 + r);
      for (let c = 0; c < params.cols; c++) {
        push(
          params.startX + c * step,
          params.startY + r * step,
          params.numberStart + c,
          rowLabel,
        );
      }
    }
  }

  return seats;
}

export function BulkSeatPanel({ state, autosave }: BulkSeatPanelProps) {
  const t = useT();
  const [layout, setLayout] = useState<Layout>("row");
  const [startX, setStartX] = useState("1");
  const [startY, setStartY] = useState("1");
  const [count, setCount] = useState("10");
  const [rows, setRows] = useState("3");
  const [cols, setCols] = useState("5");
  const [spacing, setSpacing] = useState("0");
  const [numberStart, setNumberStart] = useState("1");
  const [prefix, setPrefix] = useState("");
  const [seatRowLabel, setSeatRowLabel] = useState("");
  const [entranceRef, setEntranceRef] = useState(NONE);
  const [areaRef, setAreaRef] = useState(NONE);

  const occupied = useMemo(() => {
    const s = new Set<string>();
    state.seats.forEach((seat) => s.add(`${seat.x}-${seat.y}`));
    state.markers.forEach((m) => s.add(`${m.x}-${m.y}`));
    return s;
  }, [state.seats, state.markers]);

  const preview = useMemo(
    () =>
      generateSeats({
        layout,
        startX: Math.max(1, Number.parseInt(startX, 10) || 1),
        startY: Math.max(1, Number.parseInt(startY, 10) || 1),
        count: Math.max(0, Number.parseInt(count, 10) || 0),
        rows: Math.max(0, Number.parseInt(rows, 10) || 0),
        cols: Math.max(0, Number.parseInt(cols, 10) || 0),
        spacing: Math.max(0, Number.parseInt(spacing, 10) || 0),
        numberStart: Number.parseInt(numberStart, 10) || 1,
        prefix,
        seatRowLabel,
        entranceRef: entranceRef === NONE ? undefined : entranceRef,
        areaRef: areaRef === NONE ? undefined : areaRef,
        occupied,
      }),
    [
      layout,
      startX,
      startY,
      count,
      rows,
      cols,
      spacing,
      numberStart,
      prefix,
      seatRowLabel,
      entranceRef,
      areaRef,
      occupied,
    ],
  );

  const handleInsert = () => {
    if (preview.length === 0) return;
    autosave.addSeatsBulk(preview);
  };

  return (
    <div className="space-y-3">
      <div className="space-y-1.5">
        <Label>{t("management.locationEditor.bulk.layoutLabel")}</Label>
        <Select value={layout} onValueChange={(v) => setLayout(v as Layout)}>
          <SelectTrigger>
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="row">
              {t("management.locationEditor.bulk.layoutRow")}
            </SelectItem>
            <SelectItem value="column">
              {t("management.locationEditor.bulk.layoutColumn")}
            </SelectItem>
            <SelectItem value="block">
              {t("management.locationEditor.bulk.layoutBlock")}
            </SelectItem>
          </SelectContent>
        </Select>
      </div>

      <div className="grid grid-cols-2 gap-2">
        <div className="space-y-1.5">
          <Label>{t("management.locationEditor.bulk.startX")}</Label>
          <Input
            type="number"
            min={1}
            value={startX}
            onChange={(e) => setStartX(e.target.value)}
          />
        </div>
        <div className="space-y-1.5">
          <Label>{t("management.locationEditor.bulk.startY")}</Label>
          <Input
            type="number"
            min={1}
            value={startY}
            onChange={(e) => setStartY(e.target.value)}
          />
        </div>
      </div>

      {layout !== "block" ? (
        <div className="space-y-1.5">
          <Label>{t("management.locationEditor.bulk.countLabel")}</Label>
          <Input
            type="number"
            min={0}
            value={count}
            onChange={(e) => setCount(e.target.value)}
          />
        </div>
      ) : (
        <div className="grid grid-cols-2 gap-2">
          <div className="space-y-1.5">
            <Label>{t("management.locationEditor.bulk.rowsLabel")}</Label>
            <Input
              type="number"
              min={0}
              value={rows}
              onChange={(e) => setRows(e.target.value)}
            />
          </div>
          <div className="space-y-1.5">
            <Label>{t("management.locationEditor.bulk.colsLabel")}</Label>
            <Input
              type="number"
              min={0}
              value={cols}
              onChange={(e) => setCols(e.target.value)}
            />
          </div>
        </div>
      )}

      <div className="grid grid-cols-2 gap-2">
        <div className="space-y-1.5">
          <Label>{t("management.locationEditor.bulk.spacingLabel")}</Label>
          <Input
            type="number"
            min={0}
            value={spacing}
            onChange={(e) => setSpacing(e.target.value)}
          />
        </div>
        <div className="space-y-1.5">
          <Label>{t("management.locationEditor.bulk.numberStartLabel")}</Label>
          <Input
            type="number"
            value={numberStart}
            onChange={(e) => setNumberStart(e.target.value)}
          />
        </div>
      </div>

      <div className="grid grid-cols-2 gap-2">
        <div className="space-y-1.5">
          <Label>{t("management.locationEditor.bulk.prefixLabel")}</Label>
          <Input value={prefix} onChange={(e) => setPrefix(e.target.value)} />
        </div>
        <div className="space-y-1.5">
          <Label>{t("management.locationEditor.bulk.seatRowLabel")}</Label>
          <Input
            value={seatRowLabel}
            onChange={(e) => setSeatRowLabel(e.target.value)}
          />
        </div>
      </div>

      <div className="space-y-1.5">
        <Label>{t("management.locationEditor.bulk.entranceLabel")}</Label>
        <Select value={entranceRef} onValueChange={setEntranceRef}>
          <SelectTrigger>
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value={NONE}>
              {t("management.locationEditor.bulk.noneOption")}
            </SelectItem>
            {state.entrances
              .filter((e) => e.serverId)
              .map((e) => (
                <SelectItem key={e.localId} value={e.localId}>
                  {e.name}
                </SelectItem>
              ))}
          </SelectContent>
        </Select>
      </div>

      <div className="space-y-1.5">
        <Label>{t("management.locationEditor.bulk.areaLabel")}</Label>
        <Select value={areaRef} onValueChange={setAreaRef}>
          <SelectTrigger>
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value={NONE}>
              {t("management.locationEditor.bulk.noneOption")}
            </SelectItem>
            {state.areas
              .filter((a) => a.serverId)
              .map((a) => (
                <SelectItem key={a.localId} value={a.localId}>
                  {a.name}
                </SelectItem>
              ))}
          </SelectContent>
        </Select>
      </div>

      <Button
        size="sm"
        className="w-full"
        disabled={preview.length === 0}
        onClick={handleInsert}
      >
        <Layers className="h-4 w-4" />
        {t("management.locationEditor.bulk.insertButton", {
          count: preview.length,
        })}
      </Button>
    </div>
  );
}
