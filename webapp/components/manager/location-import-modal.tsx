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
          throw new Error(
            "Location data must include name, address, and capacity",
          );
        }
        await onImportLocation(parsedData as ImportEventLocationDto);
      } else {
        // Import seats to existing location
        if (!selectedLocationId) {
          throw new Error("Please select a location for the seats");
        }

        // Validate seats array
        if (!Array.isArray(parsedData)) {
          throw new Error("Seats data must be an array");
        }

        // Validate each seat has required fields
        for (const seat of parsedData) {
          if (
            !seat.seatNumber ||
            seat.xCoordinate === undefined ||
            seat.yCoordinate === undefined
          ) {
            throw new Error(
              "Each seat must have seatNumber, xCoordinate, and yCoordinate",
            );
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
          : "Invalid JSON format or data structure",
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
            Import Location Data
          </DialogTitle>
          <DialogDescription>
            Import location or seat data using JSON format
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-6">
          {/* Import Type Selection */}
          <div className="space-y-3">
            <Label className="text-sm font-medium">Import Type</Label>
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
                  New Location with Seats
                </Label>
              </div>
              <div className="flex items-center space-x-2">
                <RadioGroupItem value="seats" id="seats" />
                <Label htmlFor="seats" className="cursor-pointer">
                  Add Seats to Existing Location
                </Label>
              </div>
            </RadioGroup>
          </div>

          {/* Location Selection for Seats Import */}
          {importType === "seats" && (
            <div className="space-y-2">
              <Label htmlFor="location-select">Select Location</Label>
              <Select
                value={selectedLocationId}
                onValueChange={setSelectedLocationId}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Choose a location..." />
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
              JSON Data
              {importType === "location"
                ? " (Location with Seats)"
                : " (Seats Array)"}
            </Label>
            <Textarea
              id="json-input"
              placeholder={
                importType === "location"
                  ? "Paste location JSON data here..."
                  : "Paste seats array JSON data here..."
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
              Cancel
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
              {isLoading ? "Importing..." : "Import Data"}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}
