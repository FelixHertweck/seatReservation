"use client";

import { useEffect, useState } from "react";

import { useT } from "@/lib/i18n/hooks";
import { useLocationEditorData } from "@/components/management/location-editor/use-location-editor-data";
import { useLocationEditorState } from "@/components/management/location-editor/use-location-editor-state";
import { useLocationAutosave } from "@/components/management/location-editor/use-location-autosave";
import { useFillHeight } from "@/hooks/use-fill-height";
import { EditorToolbar } from "@/components/management/location-editor/editor-toolbar";
import { SeatMapEditor } from "@/components/management/location-editor/seat-map-editor";
import { EditorSidePanel } from "@/components/management/location-editor/editor-side-panel";
import { JsonView } from "@/components/management/location-editor/json-view";
import { PreviewView } from "@/components/management/location-editor/preview-view";
import { emptyLocationEditorState } from "@/components/management/location-editor/types";
import type {
  EditorTab,
  LocalId,
} from "@/components/management/location-editor/types";
import { PageHeader } from "@/components/page-header";
import { Button } from "@/components/custom-ui/button";
import Link from "next/link";
import { ArrowLeft, AlertCircle, Loader2 } from "lucide-react";

export function LocationEditor({ locationId }: { locationId: string }) {
  const t = useT();
  const { isReady, isError, isNotFound, buildInitialState } =
    useLocationEditorData(locationId);

  const editor = useLocationEditorState(
    emptyLocationEditorState({
      serverId: locationId,
      name: "",
      address: "",
      capacity: 0,
    }),
  );

  useEffect(() => {
    if (isReady) {
      const initial = buildInitialState();
      if (initial) editor.hydrate(initial);
    }
    // Only re-hydrate when readiness flips to true, not on every render.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isReady]);

  const autosave = useLocationAutosave({
    state: editor.state,
    dispatch: editor.dispatch,
  });

  const locationName = editor.state.meta.name || undefined;

  const [tab, setTab] = useState<EditorTab>("map");
  const [selection, setSelection] = useState<Set<LocalId>>(new Set());
  const [selectedAreaId, setSelectedAreaId] = useState<LocalId | null>(null);
  const [tool, setTool] = useState<"select" | "draw-area">("select");
  const [pendingAreaDraw, setPendingAreaDraw] = useState<{
    name: string;
    seatIds: Set<LocalId>;
  } | null>(null);
  const [drawTargetAreaId, setDrawTargetAreaId] = useState<LocalId | null>(
    null,
  );

  const { ref: fillRef, height } = useFillHeight<HTMLDivElement>();

  if (isError || isNotFound) {
    return (
      <>
        <PageHeader title={t("management.locationEditor.title")} />
        <div
          ref={fillRef}
          className="flex flex-col items-center justify-center gap-3 text-muted-foreground"
          style={{ height }}
        >
          <AlertCircle className="h-6 w-6" />
          <span>{t("management.locationEditor.notFound")}</span>
          <Button variant="outline" size="sm" asChild>
            <Link href="/management/locations">
              <ArrowLeft className="h-4 w-4" />
              {t("management.locationEditor.backToLocations")}
            </Link>
          </Button>
        </div>
      </>
    );
  }

  if (!isReady) {
    return (
      <>
        <PageHeader title={t("management.locationEditor.title")} />
        <div
          ref={fillRef}
          className="flex items-center justify-center text-muted-foreground"
          style={{ height }}
        >
          <Loader2 className="mr-2 h-5 w-5 animate-spin" />
          {t("common.loading")}
        </div>
      </>
    );
  }

  return (
    <>
      <PageHeader
        title={t("management.locationEditor.title")}
        description={locationName}
      />
      <div
        ref={fillRef}
        className="flex flex-col overflow-hidden rounded-lg border"
        style={{ height }}
      >
        <EditorToolbar
          state={editor.state}
          autosave={autosave}
          tab={tab}
          onTabChange={setTab}
          canUndo={editor.canUndo}
          canRedo={editor.canRedo}
          onUndo={editor.undo}
          onRedo={editor.redo}
        />
        <div className="grid min-h-0 flex-1 grid-cols-1 gap-3 overflow-hidden p-3 lg:grid-cols-[minmax(0,1fr)_22rem]">
          <div className="min-h-0 min-w-0">
            {tab === "map" && (
              <SeatMapEditor
                state={editor.state}
                autosave={autosave}
                selection={selection}
                onSelectionChange={setSelection}
                selectedAreaId={selectedAreaId}
                onSelectedAreaChange={setSelectedAreaId}
                tool={tool}
                onAreaDrawn={(boundary) => {
                  if (drawTargetAreaId) {
                    autosave.updateAreaBoundary(drawTargetAreaId, boundary);
                    setSelectedAreaId(drawTargetAreaId);
                    setDrawTargetAreaId(null);
                    setTool("select");
                    return;
                  }
                  const count = editor.state.areas.length + 1;
                  const name = pendingAreaDraw?.name ?? `Area ${count}`;
                  const localId = autosave.addArea({ name, boundary });
                  if (pendingAreaDraw && pendingAreaDraw.seatIds.size > 0) {
                    autosave.assignAreaToSeats(
                      pendingAreaDraw.seatIds,
                      localId,
                    );
                  }
                  setSelectedAreaId(localId);
                  setPendingAreaDraw(null);
                  setTool("select");
                }}
                onCancelDrawArea={() => {
                  setPendingAreaDraw(null);
                  setDrawTargetAreaId(null);
                  setTool("select");
                }}
              />
            )}
            {tab === "preview" && <PreviewView state={editor.state} />}
            {tab === "json" && (
              <JsonView state={editor.state} autosave={autosave} />
            )}
          </div>
          <div className="min-h-0 min-w-0">
            <EditorSidePanel
              state={editor.state}
              autosave={autosave}
              selection={selection}
              onSelectionChange={setSelection}
              selectedAreaId={selectedAreaId}
              onSelectedAreaChange={setSelectedAreaId}
              onDrawAreaWithSeats={(name, seatIds) => {
                setPendingAreaDraw({ name, seatIds });
                setTab("map");
                setTool("draw-area");
              }}
              onDrawAreaBoundary={(areaLocalId) => {
                setDrawTargetAreaId(areaLocalId);
                setTab("map");
                setTool("draw-area");
              }}
            />
          </div>
        </div>
      </div>
    </>
  );
}
