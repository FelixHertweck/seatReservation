"use client";

import type React from "react";

import { useState } from "react";
import { Upload, FileText } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import type {
  ImportEventLocationDto,
  ImportSeatDto,
  EventLocationResponseDto,
} from "@/api";
import { t } from "i18next";

interface LocationImportModalProps {
  isOpen: boolean;
  onClose: () => void;
  locations: EventLocationResponseDto[];
  onImportLocation: (
    data: ImportEventLocationDto,
  ) => Promise<EventLocationResponseDto>;
  onImportSeats: (seats: ImportSeatDto[], locationId: string) => Promise<void>;
}

export function LocationImportModal({
  isOpen,
  onClose,
  locations,
  onImportLocation,
  onImportSeats,
}: LocationImportModalProps) {
  const [jsonData, setJsonData] = useState("");
  const [importType, setImportType] = useState<"location" | "seats">(
    "location",
  );
  const [selectedLocationId, setSelectedLocationId] = useState<string>("");
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState("");

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setError("");

    try {
      const parsedData = JSON.parse(jsonData);

      if (importType === "location") {
        // Validate ImportEventLocationDto structure
        if (!parsedData.name || !parsedData.address || !parsedData.capacity) {
          throw new Error(t("locationImportModal.locationDataValidationError"));
        }
        await onImportLocation(parsedData as ImportEventLocationDto);
      } else {
        // Import seats to existing location
        if (!selectedLocationId) {
          throw new Error(t("locationImportModal.selectLocationForSeatsError"));
        }

        // Validate seats array
        if (!Array.isArray(parsedData)) {
          throw new Error(t("locationImportModal.seatsDataArrayError"));
        }

        // Validate each seat has required fields
        for (const seat of parsedData) {
          if (
            !seat.seatNumber ||
            seat.xCoordinate === undefined ||
            seat.yCoordinate === undefined
          ) {
            throw new Error(t("locationImportModal.seatDataValidationError"));
          }
        }

        await onImportSeats(parsedData as ImportSeatDto[], selectedLocationId);
      }

      // Reset form and close modal
      setJsonData("");
      setImportType("location");
      setSelectedLocationId("");
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
    setImportType("location");
    setSelectedLocationId("");
    setError("");
    onClose();
  };

  return (
    <Dialog open={isOpen} onOpenChange={handleClose}>
      <DialogContent className="max-w-2xl max-h-[80vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <FileText className="h-5 w-5" />
            {t("locationImportModal.importLocationDataTitle")}
          </DialogTitle>
          <DialogDescription>
            {t("locationImportModal.importLocationDataDescription")}
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-6">
          {/* Import Type Selection */}
          <div className="space-y-3">
            <Label className="text-sm font-medium">
              {t("locationImportModal.importTypeLabel")}
            </Label>
            <RadioGroup
              value={importType}
              onValueChange={(value) =>
                setImportType(value as "location" | "seats")
              }
              className="flex gap-6"
            >
              <div className="flex items-center space-x-2">
                <RadioGroupItem value="location" id="location" />
                <Label htmlFor="location" className="cursor-pointer">
                  {t("locationImportModal.newLocationWithSeatsOption")}
                </Label>
              </div>
              <div className="flex items-center space-x-2">
                <RadioGroupItem value="seats" id="seats" />
                <Label htmlFor="seats" className="cursor-pointer">
                  {t("locationImportModal.addSeatsToExistingLocationOption")}
                </Label>
              </div>
            </RadioGroup>
          </div>

          {/* Location Selection for Seats Import */}
          {importType === "seats" && (
            <div className="space-y-2">
              <Label htmlFor="location-select">
                {t("locationImportModal.selectLocationLabel")}
              </Label>
              <Select
                value={selectedLocationId}
                onValueChange={setSelectedLocationId}
              >
                <SelectTrigger>
                  <SelectValue
                    placeholder={t(
                      "locationImportModal.chooseLocationPlaceholder",
                    )}
                  />
                </SelectTrigger>
                <SelectContent>
                  {locations.map((location) => (
                    <SelectItem
                      key={location.id?.toString()}
                      value={location.id?.toString() || ""}
                    >
                      {location.name} - {location.address}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          )}

          {/* JSON Input */}
          <div className="space-y-2">
            <Label htmlFor="json-input">
              {t("locationImportModal.jsonDataLabel")}
              {importType === "location"
                ? t("locationImportModal.locationWithSeatsJsonHint")
                : t("locationImportModal.seatsArrayJsonHint")}
            </Label>
            <Textarea
              id="json-input"
              placeholder={
                importType === "location"
                  ? t("locationImportModal.pasteLocationJsonPlaceholder")
                  : t("locationImportModal.pasteSeatsJsonPlaceholder")
              }
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
              disabled={
                isLoading ||
                !jsonData.trim() ||
                (importType === "seats" && !selectedLocationId)
              }
            >
              <Upload className="mr-2 h-4 w-4" />
              {isLoading
                ? t("locationImportModal.importingButton")
                : t("locationImportModal.importDataButton")}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}
