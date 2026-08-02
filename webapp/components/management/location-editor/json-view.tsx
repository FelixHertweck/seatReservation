"use client";

import { useState } from "react";
import { Copy, Download, Upload, Check } from "lucide-react";

import { useT } from "@/lib/i18n/hooks";
import { Button } from "@/components/custom-ui/button";
import { Textarea } from "@/components/ui/textarea";
import { sanitizeFileName } from "@/lib/utils/filename";
import {
  jsonToDiff,
  stateToJson,
  isDiffEmpty,
  type JsonDiff,
} from "@/components/management/location-editor/json-codec";
import type { useLocationAutosave } from "@/components/management/location-editor/use-location-autosave";
import type { LocationEditorState } from "@/components/management/location-editor/types";

interface JsonViewProps {
  state: LocationEditorState;
  autosave: ReturnType<typeof useLocationAutosave>;
}

function applyDiff(
  diff: JsonDiff,
  autosave: ReturnType<typeof useLocationAutosave>,
) {
  if (Object.keys(diff.metaChanges).length > 0) {
    autosave.updateMeta(diff.metaChanges);
  }

  const entranceIdByName = new Map<string, string>();
  diff.entrancesToCreate.forEach((name) => {
    entranceIdByName.set(name, autosave.addEntrance({ name }));
  });
  const areaIdByName = new Map<string, string>();
  diff.areasToCreate.forEach((area) => {
    areaIdByName.set(
      area.name,
      autosave.addArea({ name: area.name, boundary: area.boundary }),
    );
  });
  diff.areasToUpdate.forEach((area) => {
    autosave.updateAreaBoundary(area.localId, area.boundary);
  });

  diff.markersToCreate.forEach((marker) => autosave.addMarker(marker));

  diff.seatsToCreate.forEach((seat) => {
    autosave.addSeat({
      seatNumber: seat.seatNumber,
      seatRow: seat.seatRow,
      x: seat.x,
      y: seat.y,
      entranceRef: seat.entranceName
        ? entranceIdByName.get(seat.entranceName)
        : undefined,
      areaRef: seat.areaName ? areaIdByName.get(seat.areaName) : undefined,
    });
  });
  diff.seatsToUpdate.forEach((seat) => {
    autosave.updateSeat(seat.localId, seat.changes);
  });

  if (diff.seatsToDelete.length > 0) {
    autosave.deleteEntities(new Set(diff.seatsToDelete));
  }
  if (diff.markersToDelete.length > 0) {
    autosave.deleteEntities(new Set(diff.markersToDelete));
  }
  if (diff.areasToDelete.length > 0) {
    autosave.deleteAreas(new Set(diff.areasToDelete));
  }
  if (diff.entrancesToDelete.length > 0) {
    autosave.deleteEntrances(new Set(diff.entrancesToDelete));
  }
}

export function JsonView({ state, autosave }: JsonViewProps) {
  const t = useT();
  const [jsonText, setJsonText] = useState(() =>
    JSON.stringify(stateToJson(state), null, 2),
  );
  const [baselineText, setBaselineText] = useState(jsonText);
  const [errors, setErrors] = useState<string[]>([]);
  const [warnings, setWarnings] = useState<string[]>([]);
  const [copied, setCopied] = useState(false);

  const isDirty = jsonText !== baselineText;

  const refreshFromState = () => {
    const text = JSON.stringify(stateToJson(state), null, 2);
    setJsonText(text);
    setBaselineText(text);
    setErrors([]);
    setWarnings([]);
  };

  const handleApply = () => {
    let parsed: unknown;
    try {
      parsed = JSON.parse(jsonText);
    } catch {
      setErrors(["Invalid JSON syntax."]);
      setWarnings([]);
      return;
    }
    const result = jsonToDiff(parsed, state);
    setErrors(result.errors.map((e) => e.message));
    setWarnings(result.warnings.map((w) => w.message));
    if (result.errors.length > 0 || !result.diff) return;

    if (!isDiffEmpty(result.diff)) {
      applyDiff(result.diff, autosave);
    }
    setBaselineText(jsonText);
  };

  const handleCopy = async () => {
    await navigator.clipboard.writeText(jsonText);
    setCopied(true);
    setTimeout(() => setCopied(false), 1500);
  };

  const handleDownload = () => {
    const blob = new Blob([jsonText], { type: "application/json" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = `${sanitizeFileName(state.meta.name || "location")}.json`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  };

  const handleUploadClick = () => {
    const input = document.createElement("input");
    input.type = "file";
    input.accept = "application/json";
    input.onchange = () => {
      const file = input.files?.[0];
      if (!file) return;
      file.text().then((text) => setJsonText(text));
    };
    input.click();
  };

  return (
    <div className="flex h-full flex-col gap-2">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div className="flex gap-2">
          <Button variant="outline" size="sm" onClick={handleCopy}>
            {copied ? (
              <Check className="h-4 w-4" />
            ) : (
              <Copy className="h-4 w-4" />
            )}
            {t("management.locationEditor.json.copyButton")}
          </Button>
          <Button variant="outline" size="sm" onClick={handleDownload}>
            <Download className="h-4 w-4" />
            {t("management.locationEditor.json.downloadButton")}
          </Button>
          <Button variant="outline" size="sm" onClick={handleUploadClick}>
            <Upload className="h-4 w-4" />
            {t("management.locationEditor.json.uploadButton")}
          </Button>
        </div>
        <Button size="sm" onClick={handleApply} disabled={!isDirty}>
          {t("management.locationEditor.json.applyButton")}
        </Button>
      </div>

      {isDirty && errors.length === 0 && (
        <div className="flex items-center justify-between rounded-md border border-amber-300 bg-amber-50 px-3 py-1.5 text-xs text-amber-800 dark:border-amber-800 dark:bg-amber-950 dark:text-amber-200">
          {t("management.locationEditor.json.notApplied")}
          <Button variant="ghost" size="sm" onClick={refreshFromState}>
            {t("management.locationEditor.json.discardButton")}
          </Button>
        </div>
      )}

      {errors.length > 0 && (
        <div className="rounded-md border border-red-300 bg-red-50 px-3 py-2 text-xs text-red-800 dark:border-red-900 dark:bg-red-950 dark:text-red-200">
          <p className="font-medium">
            {t("management.locationEditor.json.errorsTitle")}
          </p>
          <ul className="list-disc pl-4">
            {errors.map((e, i) => (
              <li key={i}>{e}</li>
            ))}
          </ul>
        </div>
      )}
      {warnings.length > 0 && (
        <div className="rounded-md border border-amber-300 bg-amber-50 px-3 py-2 text-xs text-amber-800 dark:border-amber-800 dark:bg-amber-950 dark:text-amber-200">
          <p className="font-medium">
            {t("management.locationEditor.json.warningsTitle")}
          </p>
          <ul className="list-disc pl-4">
            {warnings.map((w, i) => (
              <li key={i}>{w}</li>
            ))}
          </ul>
        </div>
      )}

      <Textarea
        value={jsonText}
        onChange={(e) => setJsonText(e.target.value)}
        className="flex-1 resize-none font-mono text-xs"
        spellCheck={false}
      />
    </div>
  );
}
