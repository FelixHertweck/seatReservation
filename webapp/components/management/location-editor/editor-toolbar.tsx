"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { ArrowLeftIcon } from "@/components/ui/arrow-left";
import { EyeIcon } from "@/components/ui/eye";
import { UndoIcon } from "@/components/ui/undo";
import { RedoIcon } from "@/components/ui/redo";
import { CircleCheckIcon } from "@/components/ui/circle-check";
import { CircleDashedIcon } from "@/components/ui/circle-dashed";
import { SettingsIcon } from "@/components/ui/settings";
import { Loader2, AlertCircle, Save, Map, FileJson } from "lucide-react";
import { useIconHover } from "@/hooks/use-icon-hover";

import { useT } from "@/lib/i18n/hooks";
import { cn } from "@/lib/utils";
import { Button } from "@/components/custom-ui/button";
import { Tabs, TabsList, TabsTrigger } from "@/components/custom-ui/tabs";
import { DetailsDialog } from "@/components/management/location-editor/details-dialog";
import { useUnsavedChanges } from "@/hooks/use-unsaved-changes";
import type { useLocationEditorSave } from "@/components/management/location-editor/use-location-editor-save";
import {
  hasUnsavedChanges,
  type EditorTab,
  type LocationEditorState,
} from "@/components/management/location-editor/types";

interface EditorToolbarProps {
  state: LocationEditorState;
  autosave: ReturnType<typeof useLocationEditorSave>;
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
  const { setPendingNavigation, setShowUnsavedDialog } = useUnsavedChanges();
  const {
    ref: previewIconRef,
    onMouseEnter: handlePreviewIconMouseEnter,
    onMouseLeave: handlePreviewIconMouseLeave,
  } = useIconHover();

  const unsaved = hasUnsavedChanges(state);

  const status = useMemo(() => {
    const all = [
      ...state.seats,
      ...state.markers,
      ...state.areas,
      ...state.entrances,
    ];
    const errorCount = all.filter((e) => e.syncState === "error").length;
    if (autosave.isSaving) return { kind: "saving" as const };
    if (errorCount > 0) return { kind: "error" as const, errorCount };
    if (unsaved) return { kind: "dirty" as const };
    return { kind: "saved" as const };
  }, [
    state.seats,
    state.markers,
    state.areas,
    state.entrances,
    autosave.isSaving,
    unsaved,
  ]);

  return (
    <div className="flex flex-wrap items-center justify-between gap-3 border-b bg-card px-3 py-2">
      <div className="flex items-center gap-3">
        <Button variant="ghost" size="sm" asChild>
          <Link
            href="/management/locations"
            onClick={(e) => {
              if (unsaved) {
                e.preventDefault();
                setPendingNavigation("/management/locations");
                setShowUnsavedDialog(true);
              }
            }}
          >
            <ArrowLeftIcon size={16} />
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
          <SettingsIcon size={14} className="shrink-0 text-muted-foreground" />
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
          <TabsTrigger
            value="preview"
            className="gap-1.5"
            onMouseEnter={handlePreviewIconMouseEnter}
            onMouseLeave={handlePreviewIconMouseLeave}
          >
            <EyeIcon ref={previewIconRef} size={14} />
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
          <UndoIcon size={16} />
        </Button>
        <Button
          variant="ghost"
          size="icon"
          disabled={!canRedo}
          onClick={onRedo}
          title={t("management.locationEditor.redo")}
        >
          <RedoIcon size={16} />
        </Button>

        <div
          className={cn(
            "flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs",
            status.kind === "saved" && "text-muted-foreground",
            status.kind === "dirty" && "text-amber-600",
            status.kind === "saving" && "text-amber-600",
            status.kind === "error" && "text-red-600",
          )}
        >
          {status.kind === "saved" && <CircleCheckIcon size={14} />}
          {(status.kind === "dirty" || status.kind === "saving") && (
            <CircleDashedIcon size={14} />
          )}
          {status.kind === "error" && <AlertCircle className="h-3.5 w-3.5" />}
          <span>
            {status.kind === "saved" &&
              t("management.locationEditor.status.saved")}
            {(status.kind === "dirty" || status.kind === "saving") &&
              t("management.locationEditor.status.unsaved")}
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
              onClick={() => void autosave.saveAll()}
            >
              {t("management.locationEditor.status.retry")}
            </Button>
          )}
        </div>

        <Button
          variant="default"
          size="sm"
          className="gap-1.5"
          disabled={!unsaved || autosave.isSaving}
          onClick={() => void autosave.saveAll()}
        >
          {autosave.isSaving ? (
            <Loader2 className="h-4 w-4 animate-spin" />
          ) : (
            <Save className="h-4 w-4" />
          )}
          {t("management.locationEditor.saveButton")}
        </Button>
      </div>
    </div>
  );
}
