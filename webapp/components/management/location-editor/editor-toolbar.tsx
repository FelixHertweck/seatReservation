"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import {
  ArrowLeft,
  Undo2,
  Redo2,
  Loader2,
  CheckCircle2,
  AlertCircle,
  Settings,
  Map,
  Eye,
  FileJson,
} from "lucide-react";

import { useT } from "@/lib/i18n/hooks";
import { cn } from "@/lib/utils";
import { Button } from "@/components/custom-ui/button";
import { Tabs, TabsList, TabsTrigger } from "@/components/custom-ui/tabs";
import { DetailsDialog } from "@/components/management/location-editor/details-dialog";
import type { useLocationAutosave } from "@/components/management/location-editor/use-location-autosave";
import type {
  EditorTab,
  LocationEditorState,
} from "@/components/management/location-editor/types";

interface EditorToolbarProps {
  state: LocationEditorState;
  autosave: ReturnType<typeof useLocationAutosave>;
  tab: EditorTab;
  onTabChange: (tab: EditorTab) => void;
  canUndo: boolean;
  canRedo: boolean;
  onUndo: () => void;
  onRedo: () => void;
}

export function EditorToolbar({
  state,
  autosave,
  tab,
  onTabChange,
  canUndo,
  canRedo,
  onUndo,
  onRedo,
}: EditorToolbarProps) {
  const t = useT();
  const [isDetailsOpen, setIsDetailsOpen] = useState(false);

  const status = useMemo(() => {
    const all = [
      ...state.seats,
      ...state.markers,
      ...state.areas,
      ...state.entrances,
    ];
    const saving = all.some((e) => e.syncState === "saving");
    const errorCount = all.filter((e) => e.syncState === "error").length;
    if (errorCount > 0) return { kind: "error" as const, errorCount };
    if (saving) return { kind: "saving" as const };
    return { kind: "saved" as const };
  }, [state.seats, state.markers, state.areas, state.entrances]);

  return (
    <div className="flex flex-wrap items-center justify-between gap-3 border-b bg-card px-3 py-2">
      <div className="flex items-center gap-3">
        <Button variant="ghost" size="sm" asChild>
          <Link href="/management/locations">
            <ArrowLeft className="h-4 w-4" />
            {t("management.locationEditor.backToLocations")}
          </Link>
        </Button>
        <Button
          variant="ghost"
          size="sm"
          className="max-w-[16rem] gap-1.5"
          onClick={() => setIsDetailsOpen(true)}
        >
          <span className="truncate font-medium">{state.meta.name}</span>
          <Settings className="h-3.5 w-3.5 shrink-0 text-muted-foreground" />
        </Button>
      </div>

      <DetailsDialog
        open={isDetailsOpen}
        onOpenChange={setIsDetailsOpen}
        meta={state.meta}
        autosave={autosave}
      />

      <Tabs value={tab} onValueChange={(v) => onTabChange(v as EditorTab)}>
        <TabsList>
          <TabsTrigger value="map" className="gap-1.5">
            <Map className="h-3.5 w-3.5" />
            {t("management.locationEditor.tabs.map")}
          </TabsTrigger>
          <TabsTrigger value="preview" className="gap-1.5">
            <Eye className="h-3.5 w-3.5" />
            {t("management.locationEditor.tabs.preview")}
          </TabsTrigger>
          <TabsTrigger value="json" className="gap-1.5">
            <FileJson className="h-3.5 w-3.5" />
            {t("management.locationEditor.tabs.json")}
          </TabsTrigger>
        </TabsList>
      </Tabs>

      <div className="flex items-center gap-2">
        <Button
          variant="ghost"
          size="icon"
          disabled={!canUndo}
          onClick={onUndo}
          title={t("management.locationEditor.undo")}
        >
          <Undo2 className="h-4 w-4" />
        </Button>
        <Button
          variant="ghost"
          size="icon"
          disabled={!canRedo}
          onClick={onRedo}
          title={t("management.locationEditor.redo")}
        >
          <Redo2 className="h-4 w-4" />
        </Button>

        <div
          className={cn(
            "flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs",
            status.kind === "saved" && "text-muted-foreground",
            status.kind === "saving" && "text-amber-600",
            status.kind === "error" && "text-red-600",
          )}
        >
          {status.kind === "saved" && <CheckCircle2 className="h-3.5 w-3.5" />}
          {status.kind === "saving" && (
            <Loader2 className="h-3.5 w-3.5 animate-spin" />
          )}
          {status.kind === "error" && <AlertCircle className="h-3.5 w-3.5" />}
          <span>
            {status.kind === "saved" &&
              t("management.locationEditor.status.saved")}
            {status.kind === "saving" &&
              t("management.locationEditor.status.saving")}
            {status.kind === "error" &&
              t("management.locationEditor.status.error", {
                count: status.errorCount,
              })}
          </span>
          {status.kind === "error" && (
            <Button
              variant="link"
              size="sm"
              className="h-auto p-0 text-xs"
              onClick={autosave.retryFailed}
            >
              {t("management.locationEditor.status.retry")}
            </Button>
          )}
        </div>
      </div>
    </div>
  );
}
