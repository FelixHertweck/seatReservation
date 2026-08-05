"use client";

import type React from "react";

import { useState } from "react";
import { UploadIcon } from "@/components/ui/upload";
import { FileTextIcon } from "@/components/ui/file-text";
import { Button } from "@/components/custom-ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/custom-ui/dialog";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/custom-ui/label";
import type { EventLocationRequestDto, EventLocationResponseDto } from "@/api";
import { useT } from "@/lib/i18n/hooks";

interface LocationImportModalProps {
  isOpen: boolean;
  onClose: () => void;
  onImportLocation: (
    data: EventLocationRequestDto,
  ) => Promise<EventLocationResponseDto>;
}

export function LocationImportModal({
  isOpen,
  onClose,
  onImportLocation,
}: LocationImportModalProps) {
  const t = useT();

  const [jsonData, setJsonData] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState("");

  const handleSubmit = async (e?: React.FormEvent | React.KeyboardEvent) => {
    if (e) {
      e.preventDefault();
    }
    setIsLoading(true);
    setError("");

    try {
      const parsedData = JSON.parse(jsonData);

      if (!parsedData.name || !parsedData.address) {
        throw new Error(t("locationImportModal.locationDataValidationError"));
      }
      await onImportLocation(parsedData as EventLocationRequestDto);

      // Reset form and close modal
      setJsonData("");
      onClose();
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : t("locationImportModal.invalidJsonOrDataStructureError"),
      );
    } finally {
      setIsLoading(false);
    }
  };

  const handleClose = () => {
    setJsonData("");
    setError("");
    onClose();
  };

  return (
    <Dialog open={isOpen} onOpenChange={handleClose}>
      <DialogContent
        className="sm:max-w-2xl sm:max-h-[80vh] sm:overflow-y-auto"
        onInteractOutside={(e) => e.preventDefault()}
      >
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <FileTextIcon size={20} />
            {t("locationImportModal.importLocationDataTitle")}
          </DialogTitle>
          <DialogDescription>
            {t("locationImportModal.importLocationDataDescription")}
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-6">
          {/* JSON Input */}
          <div className="space-y-2">
            <Label htmlFor="json-input">
              {t("locationImportModal.jsonDataLabel")}
              {t("locationImportModal.locationWithSeatsJsonHint")}
            </Label>
            <Textarea
              id="json-input"
              placeholder={t(
                "locationImportModal.pasteLocationJsonPlaceholder",
              )}
              value={jsonData}
              onChange={(e) => setJsonData(e.target.value)}
              rows={12}
              className="font-mono text-sm"
            />
          </div>

          {/* Error Display */}
          {error && (
            <div className="text-sm text-red-600 bg-red-50 p-3 rounded-md border border-red-200">
              {error}
            </div>
          )}
          {/* Action Buttons */}
          <div className="flex justify-end gap-3 pt-4">
            <Button type="button" variant="outline" onClick={handleClose}>
              {t("locationImportModal.cancelButton")}
            </Button>
            <Button
              type="submit"
              isLoading={isLoading}
              disabled={isLoading || !jsonData.trim()}
            >
              <UploadIcon size={16} className="mr-2" />
              {t("locationImportModal.importDataButton")}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}
