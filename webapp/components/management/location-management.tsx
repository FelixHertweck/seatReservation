"use client";

import { useState, useEffect, useCallback } from "react";
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
import { Skeleton } from "@/components/ui/skeleton";
import { LocationFormModal } from "@/components/management/location-form-modal";
import { LocationImportModal } from "@/components/management/location-import-modal";
import { SearchAndFilter } from "@/components/common/search-and-filter";
import { PaginationWrapper } from "@/components/common/pagination-wrapper";
import type {
  EventLocationResponseDto,
  EventLocationRequestDto,
  ImportEventLocationDto,
  ImportSeatDto,
  SeatDto,
} from "@/api";
import { customSerializer } from "@/lib/jsonBodySerializer";
import { useT } from "@/lib/i18n/hooks";

export interface LocationManagementProps {
  locations: EventLocationResponseDto[];
  seats: SeatDto[];
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
  isLoading?: boolean;
}

export function LocationManagement({
  locations,
  createLocation,
  updateLocation,
  deleteLocation,
  importLocationWithSeats,
  importSeats,
  seats: seatDtos,
  onNavigateToSeats,
  initialFilter = {},
  isLoading = false,
}: LocationManagementProps) {
  const t = useT();

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

  const applyFilters = useCallback(
    (searchQuery: string, filters: Record<string, string>) => {
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
    },
    [locations],
  );

  useEffect(() => {
    applyFilters("", currentFilters);
  }, [locations, currentFilters, applyFilters]);

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
      confirm(
        t("locationManagement.confirmDelete", { locationName: location.name }),
      )
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

  const handleExportLocation = async (location: EventLocationResponseDto) => {
    const { seatIds, ...locationWithoutSeatIds } = location;

    let seats: SeatDto[] = [];
    if (seatIds && seatIds.length > 0) {
      seats = seatIds
        .map((seatId) => {
          return seatDtos.find((seat) => seat.id === BigInt(seatId));
        })
        .filter((seat): seat is SeatDto => seat !== undefined);
    }
    console.log("seats to export", seats);

    const exportData = {
      ...locationWithoutSeatIds,
      seats: seats.map(({ ...seat }) => seat),
    };

    const dataStr = customSerializer.json(exportData);
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
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="min-w-0 flex-1">
            <CardTitle className="text-xl sm:text-2xl">
              {t("locationManagement.title")}
            </CardTitle>
            <CardDescription className="text-sm">
              {t("locationManagement.description")}
            </CardDescription>
          </div>
          <div className="flex flex-col sm:flex-row gap-2 w-full sm:w-auto shrink-0">
            <Button
              variant="outline"
              onClick={handleImportLocation}
              className="w-full sm:w-auto bg-transparent"
            >
              <FileText className="mr-2 h-4 w-4" />
              <span className="hidden sm:inline">
                {t("locationManagement.importJsonButton")}
              </span>
            </Button>
            <Button onClick={handleCreateLocation} className="w-full sm:w-auto">
              <Plus className="mr-2 h-4 w-4" />
              {t("locationManagement.addLocationButton")}
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
                label: t("locationManagement.locationFilterLabel"),
                type: "select",
                options: locations.map((loc) => ({
                  value: loc.id?.toString() || "",
                  label: loc.name || "",
                })),
              },
            ]}
            initialFilters={currentFilters}
          />

          <PaginationWrapper
            data={filteredLocations}
            itemsPerPage={100}
            paginationLabel={t("locationManagement.paginationLabel")}
          >
            {(paginatedData) => (
              <>
                <div className="hidden md:block overflow-x-auto">
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead>
                          {t("locationManagement.tableHeaderName")}
                        </TableHead>
                        <TableHead>
                          {t("locationManagement.tableHeaderAddress")}
                        </TableHead>
                        <TableHead>
                          {t("locationManagement.tableHeaderCapacity")}
                        </TableHead>
                        <TableHead>
                          {t("locationManagement.tableHeaderManager")}
                        </TableHead>
                        <TableHead>
                          {t("locationManagement.tableHeaderMarker")}
                        </TableHead>
                        <TableHead>
                          {t("locationManagement.tableHeaderSeats")}
                        </TableHead>
                        <TableHead>
                          {t("locationManagement.tableHeaderActions")}
                        </TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {isLoading
                        ? Array.from({ length: 8 }).map((_, index) => (
                            <TableRow key={index}>
                              <TableCell>
                                <Skeleton className="h-4 w-32" />
                              </TableCell>
                              <TableCell>
                                <Skeleton className="h-4 w-48" />
                              </TableCell>
                              <TableCell>
                                <Skeleton className="h-4 w-16" />
                              </TableCell>
                              <TableCell>
                                <Skeleton className="h-4 w-24" />
                              </TableCell>
                              <TableCell>
                                <Skeleton className="h-4 w-40" />
                              </TableCell>
                              <TableCell>
                                <Skeleton className="h-4 w-20" />
                              </TableCell>
                              <TableCell>
                                <div className="flex gap-2">
                                  <Skeleton className="h-8 w-8" />
                                  <Skeleton className="h-8 w-8" />
                                  <Skeleton className="h-8 w-8" />
                                </div>
                              </TableCell>
                            </TableRow>
                          ))
                        : paginatedData.map((location) => {
                            const seatCount = location.seatIds?.length || 0;
                            const markersDisplay =
                              location.markers && location.markers.length > 0
                                ? location.markers
                                    .map(
                                      (marker) =>
                                        `${marker.label} (${marker.xCoordinate}, ${marker.yCoordinate})`,
                                    )
                                    .join(", ")
                                : "-";

                            return (
                              <TableRow key={location.id?.toString()}>
                                <TableCell className="font-medium">
                                  {location.name}
                                </TableCell>
                                <TableCell>{location.address}</TableCell>
                                <TableCell>{location.capacity}</TableCell>
                                <TableCell>
                                  {location.manager?.username}
                                </TableCell>
                                <TableCell
                                  className="text-sm max-w-48 truncate"
                                  title={markersDisplay}
                                >
                                  {markersDisplay}
                                </TableCell>
                                <TableCell>
                                  {seatCount > 0 ? (
                                    <Button
                                      variant="link"
                                      className="p-0 h-auto font-normal text-blue-600 hover:text-blue-800"
                                      onClick={() =>
                                        location.id &&
                                        handleSeatsClick(location.id)
                                      }
                                    >
                                      {t("locationManagement.seatsCount", {
                                        count: seatCount,
                                      })}
                                      <ExternalLink className="ml-1 h-3 w-3" />
                                    </Button>
                                  ) : (
                                    t("locationManagement.noSeats")
                                  )}
                                </TableCell>
                                <TableCell>
                                  <div className="flex gap-2">
                                    <Button
                                      variant="outline"
                                      size="sm"
                                      onClick={() =>
                                        handleExportLocation(location)
                                      }
                                      title={t(
                                        "locationManagement.exportAsJsonTitle",
                                      )}
                                    >
                                      <Download className="h-4 w-4" />
                                    </Button>
                                    <Button
                                      variant="outline"
                                      size="sm"
                                      onClick={() =>
                                        handleEditLocation(location)
                                      }
                                    >
                                      <Edit className="h-4 w-4" />
                                    </Button>
                                    <Button
                                      variant="destructive"
                                      size="sm"
                                      onClick={() =>
                                        handleDeleteLocation(location)
                                      }
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

                <div className="md:hidden space-y-4">
                  {isLoading
                    ? Array.from({ length: 3 }).map((_, index) => (
                        <Card key={index}>
                          <CardHeader className="pb-3">
                            <Skeleton className="h-5 w-3/4" />
                            <Skeleton className="h-4 w-full mt-2" />
                          </CardHeader>
                          <CardContent>
                            <Skeleton className="h-4 w-full" />
                          </CardContent>
                        </Card>
                      ))
                    : paginatedData.map((location) => {
                        const seatCount = location.seatIds?.length || 0;
                        const markersDisplay =
                          location.markers && location.markers.length > 0
                            ? location.markers
                                .map(
                                  (marker) =>
                                    `${marker.label} (${marker.xCoordinate}, ${marker.yCoordinate})`,
                                )
                                .join(", ")
                            : "-";

                        return (
                          <Card
                            key={location.id?.toString()}
                            className="overflow-hidden"
                          >
                            <CardHeader className="pb-3">
                              <div className="flex items-start justify-between gap-2 min-w-0">
                                <div className="flex-1 min-w-0">
                                  <CardTitle className="text-base break-words">
                                    {location.name}
                                  </CardTitle>
                                  {location.address && (
                                    <CardDescription className="text-sm mt-1 break-words">
                                      {location.address}
                                    </CardDescription>
                                  )}
                                </div>
                              </div>
                            </CardHeader>
                            <CardContent className="space-y-3">
                              <div className="grid grid-cols-2 gap-3 text-sm">
                                <div className="min-w-0">
                                  <p className="text-xs text-muted-foreground mb-1">
                                    {t(
                                      "locationManagement.tableHeaderCapacity",
                                    )}
                                  </p>
                                  <p className="text-sm truncate">
                                    {location.capacity}
                                  </p>
                                </div>
                                <div className="min-w-0">
                                  <p className="text-xs text-muted-foreground mb-1">
                                    {t("locationManagement.tableHeaderManager")}
                                  </p>
                                  <p className="text-sm truncate">
                                    {location.manager?.username || "-"}
                                  </p>
                                </div>
                              </div>

                              {location.markers &&
                                location.markers.length > 0 && (
                                  <div className="min-w-0">
                                    <p className="text-xs text-muted-foreground mb-1">
                                      {t(
                                        "locationManagement.tableHeaderMarker",
                                      )}
                                    </p>
                                    <p className="text-sm break-words line-clamp-3">
                                      {markersDisplay}
                                    </p>
                                  </div>
                                )}

                              {seatCount > 0 && (
                                <div className="min-w-0">
                                  <p className="text-xs text-muted-foreground mb-1">
                                    {t("locationManagement.tableHeaderSeats")}
                                  </p>
                                  <Button
                                    variant="link"
                                    className="p-0 h-auto font-normal text-blue-600 hover:text-blue-800 text-sm"
                                    onClick={() =>
                                      location.id &&
                                      handleSeatsClick(location.id)
                                    }
                                  >
                                    {t("locationManagement.seatsCount", {
                                      count: seatCount,
                                    })}
                                    <ExternalLink className="ml-1 h-3 w-3" />
                                  </Button>
                                </div>
                              )}

                              <div className="flex flex-col gap-2 pt-2">
                                <div className="flex gap-2">
                                  <Button
                                    variant="outline"
                                    size="sm"
                                    className="flex-1 bg-transparent"
                                    onClick={() => handleEditLocation(location)}
                                  >
                                    <Edit className="mr-2 h-4 w-4 shrink-0" />
                                    <span className="truncate">
                                      {t("locationManagement.editButtonLabel")}
                                    </span>
                                  </Button>
                                  <Button
                                    variant="outline"
                                    size="sm"
                                    onClick={() =>
                                      handleExportLocation(location)
                                    }
                                    title={t(
                                      "locationManagement.exportAsJsonTitle",
                                    )}
                                  >
                                    <Download className="h-4 w-4" />
                                  </Button>
                                </div>
                                <Button
                                  variant="destructive"
                                  size="sm"
                                  className="w-full"
                                  onClick={() => handleDeleteLocation(location)}
                                >
                                  <Trash2 className="mr-2 h-4 w-4 shrink-0" />
                                  <span className="truncate">
                                    {t("locationManagement.deleteButtonLabel")}
                                  </span>
                                </Button>
                              </div>
                            </CardContent>
                          </Card>
                        );
                      })}
                </div>
              </>
            )}
          </PaginationWrapper>
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
