"use client";

import type React from "react";

import { useState } from "react";
import { Upload } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import type {
  EventLocationRegistrationDto,
  EventLocationResponseDto,
} from "@/api";

interface LocationJsonUploadProps {
  onUpload: (
    data: EventLocationRegistrationDto,
  ) => Promise<EventLocationResponseDto>;
}

export function LocationJsonUpload({ onUpload }: LocationJsonUploadProps) {
  const [jsonData, setJsonData] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState("");

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setError("");

    try {
      const data = JSON.parse(jsonData) as EventLocationRegistrationDto;
      await onUpload(data);
      setJsonData("");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Invalid JSON format");
    } finally {
      setIsLoading(false);
    }
  };

  const exampleJson = {
    eventLocation: {
      name: "Main Auditorium",
      address: "123 Main St, City, State",
      capacity: 100,
    },
    seats: [
      { seatNumber: "A1", xCoordinate: 1, yCoordinate: 1 },
      { seatNumber: "A2", xCoordinate: 2, yCoordinate: 1 },
      { seatNumber: "B1", xCoordinate: 1, yCoordinate: 2 },
      { seatNumber: "B2", xCoordinate: 2, yCoordinate: 2 },
    ],
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>Upload Location with Seats</CardTitle>
        <CardDescription>
          Upload a JSON file to create a location with its seats in one
          operation
        </CardDescription>
      </CardHeader>

      <CardContent className="space-y-4">
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="json">JSON Data</Label>
            <Textarea
              id="json"
              placeholder="Paste your JSON data here..."
              value={jsonData}
              onChange={(e) => setJsonData(e.target.value)}
              rows={10}
              className="font-mono text-sm"
            />
          </div>

          {error && (
            <div className="text-sm text-red-600 bg-red-50 p-2 rounded">
              {error}
            </div>
          )}

          <Button type="submit" disabled={isLoading || !jsonData.trim()}>
            <Upload className="mr-2 h-4 w-4" />
            {isLoading ? "Uploading..." : "Upload Location"}
          </Button>
        </form>

        <div className="space-y-2">
          <Label>Example JSON Format:</Label>
          <pre className="text-xs bg-gray-100 p-3 rounded overflow-x-auto">
            {JSON.stringify(exampleJson, null, 2)}
          </pre>
        </div>
      </CardContent>
    </Card>
  );
}
