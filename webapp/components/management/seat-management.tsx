"use client";

import { useState, useEffect, useCallback } from "react";
import { Plus, Edit, Trash2, ExternalLink } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
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
import { SearchAndFilter } from "@/components/common/search-and-filter";
import { ColumnFilter } from "@/components/common/column-filter";
import { SeatFormModal } from "@/components/management/seat-form-modal";
import { PaginationWrapper } from "@/components/common/pagination-wrapper";
import type { SeatDto, SeatRequestDto, EventLocationResponseDto } from "@/api";
import { useT } from "@/lib/i18n/hooks";
import { useColumnVisibility } from "@/hooks/use-column-visibility";

export interface SeatManagementProps {
  seats: SeatDto[];
  locations: EventLocationResponseDto[];
  createSeat: (seat: SeatRequestDto) => Promise<SeatDto>;
  updateSeat: (id: bigint, seat: SeatRequestDto) => Promise<SeatDto>;
  deleteSeat: (ids: bigint[]) => Promise<unknown>;
  onNavigateToLocation?: (locationId: bigint) => void;
  initialFilter?: Record<string, string>;
  isLoading?: boolean;
}

export function SeatManagement({
  seats,
  locations,
  createSeat,
  updateSeat,
  deleteSeat,
  onNavigateToLocation,
  initialFilter = {},
  isLoading = false,
}: SeatManagementProps) {
  const t = useT();

  const [filteredSeats, setFilteredSeats] = useState(seats);
  const [selectedSeat, setSelectedSeat] = useState<SeatDto | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isCreating, setIsCreating] = useState(false);
  const [currentFilters, setCurrentFilters] =
    useState<Record<string, string>>(initialFilter);
  const [selectedIds, setSelectedIds] = useState<Set<bigint>>(new Set());

  // Define column configuration
  const columnConfig = [
    { key: "select", label: t("seatManagement.table.selectHeader") },
    { key: "seatNumber", label: t("seatManagement.table.seatNumberHeader") },
    { key: "location", label: t("seatManagement.table.locationHeader") },
    { key: "seatRow", label: t("seatManagement.table.seatRowHeader") },
    { key: "position", label: t("seatManagement.table.positionHeader") },
    { key: "actions", label: t("seatManagement.table.actionsHeader") },
  ];

  const { visibleColumns, toggleColumn, resetColumns, isColumnVisible } =
    useColumnVisibility(columnConfig, "seat-management-columns");

  useEffect(() => {
    setCurrentFilters(initialFilter);
  }, [initialFilter]);

  const applyFilters = useCallback(
    (searchQuery: string, filters: Record<string, string>) => {
      let filtered = seats;

      // Apply search
      if (searchQuery) {
        filtered = filtered.filter(
          (seat) =>
            seat.seatNumber
              ?.toLowerCase()
              .includes(searchQuery.toLowerCase()) ||
            locations
              .find((loc) => loc.id === seat.locationId)
              ?.name?.toLowerCase()
              .includes(searchQuery.toLowerCase()) ||
            seat.seatRow?.toLowerCase().includes(searchQuery.toLowerCase()),
        );
      }

      // Apply filters
      if (filters.locationId) {
        filtered = filtered.filter(
          (seat) => seat.locationId?.toString() === filters.locationId,
        );
      }
      if (filters.seatId) {
        filtered = filtered.filter(
          (seat) => seat.id?.toString() === filters.seatId,
        );
      }

      setFilteredSeats(filtered);
    },
    [seats, locations],
  );

  useEffect(() => {
    applyFilters("", currentFilters);
  }, [seats, currentFilters, applyFilters]);

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

  const handleCreateSeat = () => {
    setSelectedSeat(null);
    setIsCreating(true);
    setIsModalOpen(true);
  };

  const handleEditSeat = (seat: SeatDto) => {
    setSelectedSeat(seat);
    setIsCreating(false);
    setIsModalOpen(true);
    setSelectedIds(new Set());
  };

  const handleDeleteSeat = async (seat: SeatDto) => {
    if (
      seat.id &&
      confirm(
        t("seatManagement.confirmDelete", { seatNumber: seat.seatNumber }),
      )
    ) {
      await deleteSeat([seat.id]);
      setSelectedIds(new Set());
    }
  };

  const handleLocationClick = (locationId: bigint) => {
    if (onNavigateToLocation) {
      onNavigateToLocation(locationId);
    }
  };

  const handleSelectAll = (paginatedData: SeatDto[]) => {
    const newSelectedIds = new Set(selectedIds);
    const allCurrentSelected = paginatedData.every((seat) =>
      seat.id ? selectedIds.has(seat.id) : false,
    );

    if (allCurrentSelected) {
      setSelectedIds(new Set());
    } else {
      paginatedData.forEach((seat) => {
        if (seat.id) newSelectedIds.add(seat.id);
      });
      setSelectedIds(newSelectedIds);
    }
  };

  const handleToggleSelect = (id: bigint) => {
    const newSelectedIds = new Set(selectedIds);
    if (newSelectedIds.has(id)) {
      newSelectedIds.delete(id);
    } else {
      newSelectedIds.add(id);
    }
    setSelectedIds(newSelectedIds);
  };

  const handleDeleteSelected = async () => {
    if (
      selectedIds.size > 0 &&
      confirm(
        t("seatManagement.confirmDeleteMultiple", {
          count: selectedIds.size,
        }),
      )
    ) {
      await deleteSeat(Array.from(selectedIds));
      setSelectedIds(new Set());
    }
  };

  return (
    <Card className="w-full">
      <CardHeader>
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="min-w-0 flex-1">
            <CardTitle className="text-xl sm:text-2xl">
              {t("seatManagement.title")}
            </CardTitle>
            <CardDescription className="text-sm">
              {t("seatManagement.description")}
            </CardDescription>
          </div>
          <div className="flex flex-col sm:flex-row gap-2 w-full sm:w-auto shrink-0">
            {selectedIds.size > 0 && (
              <Button
                variant="destructive"
                onClick={handleDeleteSelected}
                className="w-full sm:w-auto"
              >
                <Trash2 className="mr-2 h-4 w-4" />
                {selectedIds.size}
              </Button>
            )}
            <Button
              onClick={handleCreateSeat}
              className="w-full sm:w-auto shrink-0"
            >
              <Plus className="mr-2 h-4 w-4" />
              {t("seatManagement.addSeatButton")}
            </Button>
          </div>
        </div>
      </CardHeader>

      <CardContent>
        <div className="flex gap-2 mb-4">
          <div className="flex-1">
            <SearchAndFilter
              onSearch={handleSearch}
              onFilter={handleFilter}
              filterOptions={[
                {
                  key: "locationId",
                  label: t("seatManagement.filter.locationLabel"),
                  type: "select",
                  options: locations.map((loc) => ({
                    value: loc.id?.toString() || "",
                    label: loc.name || "",
                  })),
                },
              ]}
              initialFilters={currentFilters}
            />
          </div>
          <ColumnFilter
            columns={columnConfig}
            visibleColumns={visibleColumns}
            onVisibilityChange={toggleColumn}
            onResetColumns={resetColumns}
          />
        </div>

        <PaginationWrapper
          data={filteredSeats}
          itemsPerPage={100}
          paginationLabel={t("seatManagement.paginationLabel")}
        >
          {(paginatedData) => (
            <>
              <div className="hidden md:block overflow-x-auto">
                <div className="mb-2">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => handleSelectAll(paginatedData)}
                  >
                    {paginatedData.every((seat) =>
                      seat.id ? selectedIds.has(seat.id) : false,
                    )
                      ? t("seatManagement.deselectAll")
                      : t("seatManagement.selectAll")}
                  </Button>
                </div>
                <Table>
                  <TableHeader>
                    <TableRow>
                      {isColumnVisible("select") && (
                        <TableHead className="w-12">
                          {t("seatManagement.table.selectHeader")}
                        </TableHead>
                      )}
                      {isColumnVisible("seatNumber") && (
                        <TableHead>
                          {t("seatManagement.table.seatNumberHeader")}
                        </TableHead>
                      )}
                      {isColumnVisible("location") && (
                        <TableHead>
                          {t("seatManagement.table.locationHeader")}
                        </TableHead>
                      )}
                      {isColumnVisible("seatRow") && (
                        <TableHead>
                          {t("seatManagement.table.seatRowHeader")}
                        </TableHead>
                      )}
                      {isColumnVisible("position") && (
                        <TableHead>
                          {t("seatManagement.table.positionHeader")}
                        </TableHead>
                      )}
                      {isColumnVisible("actions") && (
                        <TableHead>
                          {t("seatManagement.table.actionsHeader")}
                        </TableHead>
                      )}
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {isLoading
                      ? Array.from({ length: 8 }).map((_, index) => (
                          <TableRow key={index}>
                            <TableCell>
                              <Skeleton className="h-4 w-4" />
                            </TableCell>
                            <TableCell>
                              <Skeleton className="h-4 w-16" />
                            </TableCell>
                            <TableCell>
                              <Skeleton className="h-4 w-32" />
                            </TableCell>
                            <TableCell>
                              <Skeleton className="h-4 w-12" />
                            </TableCell>
                            <TableCell>
                              <Skeleton className="h-4 w-20" />
                            </TableCell>
                            <TableCell>
                              <div className="flex gap-2">
                                <Skeleton className="h-8 w-8" />
                                <Skeleton className="h-8 w-8" />
                              </div>
                            </TableCell>
                          </TableRow>
                        ))
                      : paginatedData.map((seat) => {
                          const location = locations.find(
                            (loc) => loc.id === seat.locationId,
                          );

                          return (
                            <TableRow key={seat.id?.toString()}>
                              {isColumnVisible("select") && (
                                <TableCell>
                                  <Checkbox
                                    checked={
                                      seat.id ? selectedIds.has(seat.id) : false
                                    }
                                    onCheckedChange={() =>
                                      seat.id && handleToggleSelect(seat.id)
                                    }
                                  />
                                </TableCell>
                              )}
                              {isColumnVisible("seatNumber") && (
                                <TableCell className="font-medium">
                                  {seat.seatNumber}
                                </TableCell>
                              )}
                              {isColumnVisible("location") && (
                                <TableCell>
                                  {location ? (
                                    <Button
                                      variant="link"
                                      className="p-0 h-auto font-normal text-blue-600 hover:text-blue-800"
                                      onClick={() =>
                                        location.id &&
                                        handleLocationClick(location.id)
                                      }
                                    >
                                      {location.name}
                                      <ExternalLink className="ml-1 h-3 w-3" />
                                    </Button>
                                  ) : (
                                    t("seatManagement.unknownLocation")
                                  )}
                                </TableCell>
                              )}
                              {isColumnVisible("seatRow") && (
                                <TableCell>{seat.seatRow}</TableCell>
                              )}
                              {isColumnVisible("position") && (
                                <TableCell>
                                  ({seat.xCoordinate}, {seat.yCoordinate})
                                </TableCell>
                              )}
                              {isColumnVisible("actions") && (
                                <TableCell>
                                  <div className="flex gap-2">
                                    <Button
                                      variant="outline"
                                      size="sm"
                                      onClick={() => handleEditSeat(seat)}
                                    >
                                      <Edit className="h-4 w-4" />
                                    </Button>
                                    <Button
                                      variant="destructive"
                                      size="sm"
                                      onClick={() => handleDeleteSeat(seat)}
                                    >
                                      <Trash2 className="h-4 w-4" />
                                    </Button>
                                  </div>
                                </TableCell>
                              )}
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
                          <Skeleton className="h-5 w-1/2" />
                          <Skeleton className="h-4 w-3/4 mt-2" />
                        </CardHeader>
                        <CardContent>
                          <Skeleton className="h-4 w-full" />
                        </CardContent>
                      </Card>
                    ))
                  : paginatedData.map((seat) => {
                      const location = locations.find(
                        (loc) => loc.id === seat.locationId,
                      );

                      return (
                        <Card key={seat.id?.toString()} className="w-full">
                          <CardHeader className="pb-3 flex flex-row items-start space-x-3 space-y-0">
                            <Checkbox
                              checked={
                                seat.id ? selectedIds.has(seat.id) : false
                              }
                              onCheckedChange={() =>
                                seat.id && handleToggleSelect(seat.id)
                              }
                              className="mt-1"
                            />
                            <div className="flex-1 min-w-0">
                              <CardTitle className="text-base break-words">
                                {seat.seatNumber}
                              </CardTitle>
                              {seat.seatRow && (
                                <CardDescription className="text-sm mt-1 break-words">
                                  {t("seatManagement.table.seatRowHeader")}:{" "}
                                  {seat.seatRow}
                                </CardDescription>
                              )}
                            </div>
                          </CardHeader>
                          <CardContent className="space-y-3">
                            {location && (
                              <div className="min-w-0">
                                <p className="text-xs text-muted-foreground mb-1">
                                  {t("seatManagement.table.locationHeader")}
                                </p>
                                <Button
                                  variant="link"
                                  className="p-0 h-auto font-normal text-blue-600 hover:text-blue-800 text-sm break-words text-left max-w-full"
                                  onClick={() =>
                                    location.id &&
                                    handleLocationClick(location.id)
                                  }
                                >
                                  <span className="break-words max-w-full">
                                    {location.name}
                                  </span>
                                  <ExternalLink className="ml-1 h-3 w-3 shrink-0" />
                                </Button>
                              </div>
                            )}

                            <div className="min-w-0">
                              <p className="text-xs text-muted-foreground mb-1">
                                {t("seatManagement.table.positionHeader")}
                              </p>
                              <p className="text-sm break-words">
                                ({seat.xCoordinate}, {seat.yCoordinate})
                              </p>
                            </div>

                            <div className="flex gap-2 pt-2">
                              <Button
                                variant="outline"
                                size="sm"
                                className="flex-1 bg-transparent min-w-0"
                                onClick={() => handleEditSeat(seat)}
                              >
                                <Edit className="mr-2 h-4 w-4 shrink-0" />
                                <span className="truncate">
                                  {t("seatManagement.editButtonLabel")}
                                </span>
                              </Button>
                              <Button
                                variant="destructive"
                                size="sm"
                                className="flex-1 min-w-0"
                                onClick={() => handleDeleteSeat(seat)}
                              >
                                <Trash2 className="mr-2 h-4 w-4 shrink-0" />
                                <span className="truncate">
                                  {t("seatManagement.deleteButtonLabel")}
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
      </CardContent>

      {isModalOpen && (
        <SeatFormModal
          locations={locations}
          seat={selectedSeat}
          isCreating={isCreating}
          onSubmit={async (seatData) => {
            if (isCreating) {
              await createSeat(seatData);
            } else if (selectedSeat?.id) {
              await updateSeat(selectedSeat.id, seatData);
            }
            setIsModalOpen(false);
          }}
          onClose={() => setIsModalOpen(false)}
        />
      )}
    </Card>
  );
}
