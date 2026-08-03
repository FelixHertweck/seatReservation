"use client";

import type { ReactNode } from "react";
import { Armchair, DoorOpen, MapPin, Shapes } from "lucide-react";

import { useT } from "@/lib/i18n/hooks";
import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionTrigger,
} from "@/components/ui/accordion";
import { Badge } from "@/components/ui/badge";
import { SelectionPanel } from "@/components/management/location-editor/panels/selection-panel";
import { SeatsPanel } from "@/components/management/location-editor/panels/seats-panel";
import { MarkersPanel } from "@/components/management/location-editor/panels/markers-panel";
import { AreasPanel } from "@/components/management/location-editor/panels/areas-panel";
import { EntrancesPanel } from "@/components/management/location-editor/panels/entrances-panel";
import type { useLocationEditorSave } from "@/components/management/location-editor/use-location-editor-save";
import type {
  LocalId,
  LocationEditorState,
} from "@/components/management/location-editor/types";

interface EditorSidePanelProps {
  state: LocationEditorState;
  autosave: ReturnType<typeof useLocationEditorSave>;
  selection: Set<LocalId>;
  onSelectionChange: (next: Set<LocalId>) => void;
  selectedAreaId: LocalId | null;
  onSelectedAreaChange: (id: LocalId | null) => void;
  onDrawAreaWithSeats: (name: string, seatIds: Set<LocalId>) => void;
  onDrawAreaBoundary: (areaLocalId: LocalId) => void;
}

function SectionTrigger({
  icon,
  label,
  count,
}: {
  icon: ReactNode;
  label: string;
  count: number;
}) {
  return (
    <div className="flex flex-1 items-center gap-2">
      <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-md bg-muted text-muted-foreground">
        {icon}
      </span>
      <span>{label}</span>
      <Badge variant="secondary" className="ml-auto mr-2 tabular-nums">
        {count}
      </Badge>
    </div>
  );
}

export function EditorSidePanel({
  state,
  autosave,
  selection,
  onSelectionChange,
  selectedAreaId,
  onSelectedAreaChange,
  onDrawAreaWithSeats,
  onDrawAreaBoundary,
}: EditorSidePanelProps) {
  const t = useT();

  return (
    <div className="flex h-full min-h-0 flex-col gap-3">
      <div className="h-1/3 shrink-0 space-y-2 overflow-y-auto rounded-lg border p-3">
        <p className="text-sm font-medium">
          {t("management.locationEditor.selection.title")}
        </p>
        <SelectionPanel
          state={state}
          selection={selection}
          onSelectionChange={onSelectionChange}
          selectedAreaId={selectedAreaId}
          onSelectedAreaChange={onSelectedAreaChange}
          autosave={autosave}
          onDrawAreaBoundary={onDrawAreaBoundary}
        />
      </div>

      <Accordion
        type="multiple"
        defaultValue={["seats"]}
        className="min-h-0 flex-1 space-y-2 overflow-y-auto"
      >
        <AccordionItem value="seats" className="rounded-lg border bg-card px-3">
          <AccordionTrigger className="hover:no-underline">
            <SectionTrigger
              icon={<Armchair className="h-4 w-4" />}
              label={t("management.locationEditor.seats.title")}
              count={state.seats.length}
            />
          </AccordionTrigger>
          <AccordionContent>
            <SeatsPanel
              state={state}
              selection={selection}
              onSelectionChange={onSelectionChange}
              autosave={autosave}
            />
          </AccordionContent>
        </AccordionItem>

        <AccordionItem
          value="markers"
          className="rounded-lg border bg-card px-3"
        >
          <AccordionTrigger className="hover:no-underline">
            <SectionTrigger
              icon={<MapPin className="h-4 w-4" />}
              label={t("management.locationEditor.markers.title")}
              count={state.markers.length}
            />
          </AccordionTrigger>
          <AccordionContent>
            <MarkersPanel
              state={state}
              selection={selection}
              onSelectionChange={onSelectionChange}
              autosave={autosave}
            />
          </AccordionContent>
        </AccordionItem>

        <AccordionItem value="areas" className="rounded-lg border bg-card px-3">
          <AccordionTrigger className="hover:no-underline">
            <SectionTrigger
              icon={<Shapes className="h-4 w-4" />}
              label={t("management.locationEditor.areas.title")}
              count={state.areas.length}
            />
          </AccordionTrigger>
          <AccordionContent>
            <AreasPanel
              state={state}
              selectedAreaId={selectedAreaId}
              onSelectedAreaChange={onSelectedAreaChange}
              autosave={autosave}
              onDrawWithSeats={onDrawAreaWithSeats}
            />
          </AccordionContent>
        </AccordionItem>

        <AccordionItem
          value="entrances"
          className="rounded-lg border bg-card px-3"
        >
          <AccordionTrigger className="hover:no-underline">
            <SectionTrigger
              icon={<DoorOpen className="h-4 w-4" />}
              label={t("management.locationEditor.entrances.title")}
              count={state.entrances.length}
            />
          </AccordionTrigger>
          <AccordionContent>
            <EntrancesPanel state={state} autosave={autosave} />
          </AccordionContent>
        </AccordionItem>
      </Accordion>
    </div>
  );
}
