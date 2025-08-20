"use client";

import { useState, useEffect, useCallback } from "react";
import { Plus, Edit, Trash2, ExternalLink } from "lucide-react";
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
import { Badge } from "@/components/ui/badge";
import { SearchAndFilter } from "@/components/common/search-and-filter";
import { SeatFormModal } from "@/components/manager/seat-form-modal";
import type {
  SeatResponseDto,
  SeatRequestDto,
  EventLocationResponseDto,
} from "@/api";
import { useT } from "@/lib/i18n/hooks";

export interface SeatManagementProps {
  seats: SeatResponseDto[];
  locations: EventLocationResponseDto[];
  createSeat: (seat: SeatRequestDto) => Promise<SeatResponseDto>;
  updateSeat: (id: bigint, seat: SeatRequestDto) => Promise<SeatResponseDto>;
  deleteSeat: (id: bigint) => Promise<unknown>;
  onNavigateToLocation?: (locationId: bigint) => void;
  initialFilter?: Record<string, string>;
}

export function SeatManagement({
  seats,
  locations,
  createSeat,
  updateSeat,
  deleteSeat,
  onNavigateToLocation,
  initialFilter = {},
}: SeatManagementProps) {
  const t = useT();

  const [filteredSeats, setFilteredSeats] = useState(seats);
  const [selectedSeat, setSelectedSeat] = useState<SeatResponseDto | null>(
    null,
  );
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isCreating, setIsCreating] = useState(false);
  const [currentFilters, setCurrentFilters] =
    useState<Record<string, string>>(initialFilter);

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
              .find((loc) => loc.id === seat.eventLocationId)
              ?.name?.toLowerCase()
              .includes(searchQuery.toLowerCase()),
        );
      }

      // Apply filters
      if (filters.locationId) {
        filtered = filtered.filter(
          (seat) => seat.eventLocationId?.toString() === filters.locationId,
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

  const handleEditSeat = (seat: SeatResponseDto) => {
    setSelectedSeat(seat);
    setIsCreating(false);
    setIsModalOpen(true);
  };

  const handleDeleteSeat = async (seat: SeatResponseDto) => {
    if (
      seat.id &&
      confirm(
        t("seatManagement.confirmDelete", { seatNumber: seat.seatNumber }),
      )
    ) {
      await deleteSeat(seat.id);
    }
  };

  const handleLocationClick = (locationId: bigint) => {
    if (onNavigateToLocation) {
      onNavigateToLocation(locationId);
    }
  };

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between">
          <div>
            <CardTitle>{t("seatManagement.title")}</CardTitle>
            <CardDescription>{t("seatManagement.description")}</CardDescription>
          </div>
          <Button onClick={handleCreateSeat}>
            <Plus className="mr-2 h-4 w-4" />
            {t("seatManagement.addSeatButton")}
          </Button>
        </div>
      </CardHeader>

      <CardContent>
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

        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>
                {t("seatManagement.table.seatNumberHeader")}
              </TableHead>
              <TableHead>{t("seatManagement.table.locationHeader")}</TableHead>
              <TableHead>{t("seatManagement.table.positionHeader")}</TableHead>
              <TableHead>{t("seatManagement.table.statusHeader")}</TableHead>
              <TableHead>{t("seatManagement.table.actionsHeader")}</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {filteredSeats.map((seat) => {
              const location = locations.find(
                (loc) => loc.id === seat.eventLocationId,
              );

              return (
                <TableRow key={seat.id?.toString()}>
                  <TableCell className="font-medium">
                    {seat.seatNumber}
                  </TableCell>
                  <TableCell>
                    {location ? (
                      <Button
                        variant="link"
                        className="p-0 h-auto font-normal text-blue-600 hover:text-blue-800"
                        onClick={() =>
                          location.id && handleLocationClick(location.id)
                        }
                      >
                        {location.name}
                        <ExternalLink className="ml-1 h-3 w-3" />
                      </Button>
                    ) : (
                      t("seatManagement.unknownLocation")
                    )}
                  </TableCell>
                  <TableCell>
                    ({seat.xCoordinate}, {seat.yCoordinate})
                  </TableCell>
                  <TableCell>
                    <Badge variant={seat.status ? "destructive" : "default"}>
                      {seat.status || t("seatManagement.statusAvailable")}
                    </Badge>
                  </TableCell>
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
                </TableRow>
              );
            })}
          </TableBody>
        </Table>
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
