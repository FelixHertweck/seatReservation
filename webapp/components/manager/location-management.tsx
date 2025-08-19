"use client";

import { useState, useEffect } from "react";
import {
  Plus,
  Edit,
  Trash2,
  ExternalLink,
  FileText,
  Download,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { LocationFormModal } from "@/components/manager/location-form-modal";
import { LocationImportModal } from "@/components/manager/location-import-modal";
import { SearchAndFilter } from "@/components/common/search-and-filter";
import type {
  EventLocationResponseDto,
  EventLocationRequestDto,
  ImportEventLocationDto,
  ImportSeatDto,
} from "@/api";

export interface LocationManagementProps {
  locations: EventLocationResponseDto[];
  createLocation: (
    location: EventLocationRequestDto,
  ) => Promise<EventLocationResponseDto>;
  updateLocation: (
    id: bigint,
    location: EventLocationRequestDto,
  ) => Promise<EventLocationResponseDto>;
  deleteLocation: (id: bigint) => Promise<unknown>;
  importLocationWithSeats: (
    data: ImportEventLocationDto,
  ) => Promise<EventLocationResponseDto>;
  importSeats: (
    seats: ImportSeatDto[],
    locationId: string,
  ) => Promise<EventLocationResponseDto>;
  onNavigateToSeats?: (locationId: bigint) => void;
  initialFilter?: Record<string, string>;
}

export function LocationManagement({
  locations,
  createLocation,
  updateLocation,
  deleteLocation,
  importLocationWithSeats,
  importSeats,
  onNavigateToSeats,
  initialFilter = {},
}: LocationManagementProps) {
  const [filteredLocations, setFilteredLocations] = useState(locations);
  const [selectedLocation, setSelectedLocation] =
    useState<EventLocationResponseDto | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isImportModalOpen, setIsImportModalOpen] = useState(false);
  const [isCreating, setIsCreating] = useState(false);
  const [currentFilters, setCurrentFilters] =
    useState<Record<string, string>>(initialFilter);

  useEffect(() => {
    setCurrentFilters(initialFilter);
  }, [initialFilter]);

  useEffect(() => {
    applyFilters("", currentFilters);
  }, [locations, currentFilters]);

  const applyFilters = (
    searchQuery: string,
    filters: Record<string, string>,
  ) => {
    let filtered = locations;

    // Apply search
    if (searchQuery) {
      filtered = filtered.filter(
        (location) =>
          location.name?.toLowerCase().includes(searchQuery.toLowerCase()) ||
          location.address?.toLowerCase().includes(searchQuery.toLowerCase()),
      );
    }

    // Apply filters
    if (filters.locationId) {
      filtered = filtered.filter(
        (location) => location.id?.toString() === filters.locationId,
      );
    }

    setFilteredLocations(filtered);
  };

  const handleSearch = (query: string) => {
    applyFilters(query, currentFilters);
  };

  const handleFilter = (filters: Record<string, unknown>) => {
    const stringFilters = Object.fromEntries(
      Object.entries(filters).map(([key, value]) => [key, String(value)]),
    );
    setCurrentFilters(stringFilters);
    applyFilters("", stringFilters);
  };

  const handleCreateLocation = () => {
    setSelectedLocation(null);
    setIsCreating(true);
    setIsModalOpen(true);
  };

  const handleImportLocation = () => {
    setIsImportModalOpen(true);
  };

  const handleEditLocation = (location: EventLocationResponseDto) => {
    setSelectedLocation(location);
    setIsCreating(false);
    setIsModalOpen(true);
  };

  const handleDeleteLocation = async (location: EventLocationResponseDto) => {
    if (
      location.id !== undefined &&
      confirm(`Are you sure you want to delete ${location.name}?`)
    ) {
      await deleteLocation(BigInt(location.id));
    }
  };

  const handleSeatsClick = (locationId: bigint) => {
    if (onNavigateToSeats) {
      onNavigateToSeats(locationId);
    }
  };

  const handleImportSeats = async (
    seats: ImportSeatDto[],
    locationId: string,
  ) => {
    if (importSeats) {
      await importSeats(seats, locationId);
    }
  };

  const handleExportLocation = (location: EventLocationResponseDto) => {
    const dataStr = JSON.stringify(
      location,
      (_, value) => (typeof value === "bigint" ? Number(value) : value),
      2,
    );
    const dataBlob = new Blob([dataStr], { type: "application/json" });
    const url = URL.createObjectURL(dataBlob);

    const link = document.createElement("a");
    link.href = url;
    link.download = `${location.name?.replace(/[^a-z0-9]/gi, "_").toLowerCase()}_export.json`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  };

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between">
          <div>
            <CardTitle>Location Management</CardTitle>
            <CardDescription>Create and manage event locations</CardDescription>
          </div>
          <div className="flex gap-2">
            <Button variant="outline" onClick={handleImportLocation}>
              <FileText className="mr-2 h-4 w-4" />
              Import JSON
            </Button>
            <Button onClick={handleCreateLocation}>
              <Plus className="mr-2 h-4 w-4" />
              Add Location
            </Button>
          </div>
        </div>
      </CardHeader>

      <CardContent>
        <div className="space-y-4">
          <SearchAndFilter
            onSearch={handleSearch}
            onFilter={handleFilter}
            filterOptions={[
              {
                key: "locationId",
                label: "Location",
                type: "select",
                options: locations.map((loc) => ({
                  value: loc.id?.toString() || "",
                  label: loc.name || "",
                })),
              },
            ]}
            initialFilters={currentFilters}
          />

          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Name</TableHead>
                <TableHead>Address</TableHead>
                <TableHead>Capacity</TableHead>
                <TableHead>Manager</TableHead>
                <TableHead>Seats</TableHead>
                <TableHead>Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {filteredLocations.map((location) => {
                const seatCount = location.seats?.length || 0;

                return (
                  <TableRow key={location.id?.toString()}>
                    <TableCell className="font-medium">
                      {location.name}
                    </TableCell>
                    <TableCell>{location.address}</TableCell>
                    <TableCell>{location.capacity}</TableCell>
                    <TableCell>{location.manager?.username}</TableCell>
                    <TableCell>
                      {seatCount > 0 ? (
                        <Button
                          variant="link"
                          className="p-0 h-auto font-normal text-blue-600 hover:text-blue-800"
                          onClick={() =>
                            location.id && handleSeatsClick(location.id)
                          }
                        >
                          {seatCount} seats
                          <ExternalLink className="ml-1 h-3 w-3" />
                        </Button>
                      ) : (
                        "0 seats"
                      )}
                    </TableCell>
                    <TableCell>
                      <div className="flex gap-2">
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => handleExportLocation(location)}
                          title="Export as JSON"
                        >
                          <Download className="h-4 w-4" />
                        </Button>
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => handleEditLocation(location)}
                        >
                          <Edit className="h-4 w-4" />
                        </Button>
                        <Button
                          variant="destructive"
                          size="sm"
                          onClick={() => handleDeleteLocation(location)}
                        >
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        </div>
      </CardContent>

      {isModalOpen && (
        <LocationFormModal
          location={selectedLocation}
          isCreating={isCreating}
          onSubmit={async (locationData) => {
            if (isCreating) {
              await createLocation(locationData);
            } else if (selectedLocation?.id !== undefined) {
              await updateLocation(BigInt(selectedLocation.id), locationData);
            }
            setIsModalOpen(false);
          }}
          onClose={() => setIsModalOpen(false)}
        />
      )}

      {isImportModalOpen && (
        <LocationImportModal
          isOpen={isImportModalOpen}
          onClose={() => setIsImportModalOpen(false)}
          locations={locations}
          onImportLocation={importLocationWithSeats}
          onImportSeats={handleImportSeats}
        />
      )}
    </Card>
  );
}
