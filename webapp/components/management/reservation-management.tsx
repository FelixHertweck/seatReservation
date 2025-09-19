"use client";

import { useState, useEffect, useCallback } from "react";
import { Plus, Trash2, Ban, ExternalLink, Download } from "lucide-react"; // Added Download icon
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
import { ReservationFormModal } from "@/components/management/reservation-form-modal";
import { BlockSeatsModal } from "@/components/management/block-seats-modal";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { PaginationWrapper } from "@/components/common/pagination-wrapper";
import type {
  UserDto,
  DetailedEventResponseDto,
  DetailedReservationResponseDto,
  ReservationRequestDto,
  BlockSeatsRequestDto,
} from "@/api";
import { customSerializer } from "@/lib/jsonBodySerializer";
import { useT } from "@/lib/i18n/hooks";

export interface ReservationManagementProps {
  users: UserDto[];
  events: DetailedEventResponseDto[];
  reservations: DetailedReservationResponseDto[];
  createReservation: (
    reservation: ReservationRequestDto,
  ) => Promise<DetailedReservationResponseDto[]>;
  deleteReservation: (id: bigint) => Promise<unknown>;
  blockSeats: (
    request: BlockSeatsRequestDto,
  ) => Promise<DetailedReservationResponseDto[]>;
  onNavigateToEvent?: (eventId: bigint) => void;
  onNavigateToSeat?: (seatId: bigint) => void;
  initialFilter?: Record<string, string>;
}

export function ReservationManagement({
  users,
  events,
  reservations,
  createReservation,
  deleteReservation,
  blockSeats,
  onNavigateToEvent,
  onNavigateToSeat,
  initialFilter = {},
}: ReservationManagementProps) {
  const t = useT();

  const [filteredReservations, setFilteredReservations] =
    useState(reservations);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isBlockModalOpen, setIsBlockModalOpen] = useState(false);
  const [isExportModalOpen, setIsExportModalOpen] = useState(false); // Added state for export functionality
  const [selectedEventForExport, setSelectedEventForExport] =
    useState<string>("");
  const [currentFilters, setCurrentFilters] =
    useState<Record<string, string>>(initialFilter);

  useEffect(() => {
    setCurrentFilters(initialFilter);
  }, [initialFilter]);

  const applyFilters = useCallback(
    (searchQuery: string, filters: Record<string, string>) => {
      let filtered = reservations;

      // Apply search
      if (searchQuery) {
        filtered = filtered.filter(
          (reservation) =>
            reservation.user?.username
              ?.toLowerCase()
              .includes(searchQuery.toLowerCase()) ||
            events
              .find((event) => event.id === reservation.eventId)
              ?.name?.toLowerCase()
              .includes(searchQuery.toLowerCase()) ||
            reservation.seat?.seatNumber
              ?.toLowerCase()
              .includes(searchQuery.toLowerCase()) ||
            reservation.seat?.seatRow
              ?.toLowerCase()
              .includes(searchQuery.toLowerCase()),
        );
      }

      // Apply filters
      if (filters.eventId) {
        filtered = filtered.filter(
          (reservation) => reservation.eventId?.toString() === filters.eventId,
        );
      }
      if (filters.seatId) {
        filtered = filtered.filter(
          (reservation) => reservation.seat?.id?.toString() === filters.seatId,
        );
      }

      setFilteredReservations(filtered);
    },
    [reservations, events],
  );

  useEffect(() => {
    applyFilters("", currentFilters);
  }, [reservations, currentFilters, applyFilters]);

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

  const handleCreateReservation = () => {
    setIsModalOpen(true);
  };

  const handleDeleteReservation = async (
    reservation: DetailedReservationResponseDto,
  ) => {
    if (reservation.id && confirm(t("reservationManagement.confirmDelete"))) {
      await deleteReservation(reservation.id);
    }
  };

  const handleEventClick = (eventId: bigint) => {
    if (onNavigateToEvent) {
      onNavigateToEvent(eventId);
    }
  };

  const handleSeatClick = (seatId: bigint) => {
    if (onNavigateToSeat) {
      onNavigateToSeat(seatId);
    }
  };

  const handleExportReservations = () => {
    if (!selectedEventForExport) return;

    const eventId = BigInt(selectedEventForExport);
    const event = events.find((e) => e.id === eventId);
    const eventReservations = reservations.filter((r) => r.eventId === eventId);

    const exportData = customSerializer.json(eventReservations);

    const blob = new Blob([exportData], {
      type: "application/json",
    });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `reservations-${event?.name?.replace(/[^a-z0-9]/gi, "_").toLowerCase() || "event"}-${new Date().toISOString().split("T")[0]}.json`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);

    setIsExportModalOpen(false);
    setSelectedEventForExport("");
  };

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between">
          <div>
            <CardTitle>{t("reservationManagement.title")}</CardTitle>
            <CardDescription>
              {t("reservationManagement.description")}
            </CardDescription>
          </div>
          <div className="flex gap-2">
            <Dialog
              open={isExportModalOpen}
              onOpenChange={setIsExportModalOpen}
            >
              <DialogTrigger asChild>
                <Button variant="outline">
                  <Download className="mr-2 h-4 w-4" />
                  {t("reservationManagement.exportReservationsButton")}
                </Button>
              </DialogTrigger>
              <DialogContent>
                <DialogHeader>
                  <DialogTitle>
                    {t("reservationManagement.exportEventReservationsTitle")}
                  </DialogTitle>
                  <DialogDescription>
                    {t(
                      "reservationManagement.exportEventReservationsDescription",
                    )}
                  </DialogDescription>
                </DialogHeader>
                <div className="space-y-4">
                  <div>
                    <label className="text-sm font-medium">
                      {t("reservationManagement.selectEventLabel")}
                    </label>
                    <Select
                      value={selectedEventForExport}
                      onValueChange={setSelectedEventForExport}
                    >
                      <SelectTrigger>
                        <SelectValue
                          placeholder={t(
                            "reservationManagement.chooseEventToExportPlaceholder",
                          )}
                        />
                      </SelectTrigger>
                      <SelectContent>
                        {events.map((event) => (
                          <SelectItem
                            key={event.id?.toString()}
                            value={event.id?.toString() || ""}
                          >
                            {event.name}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>
                  <div className="flex justify-end gap-2">
                    <Button
                      variant="outline"
                      onClick={() => setIsExportModalOpen(false)}
                    >
                      {t("reservationManagement.cancelButton")}
                    </Button>
                    <Button
                      onClick={handleExportReservations}
                      disabled={!selectedEventForExport}
                    >
                      {t("reservationManagement.exportJsonButton")}
                    </Button>
                  </div>
                </div>
              </DialogContent>
            </Dialog>
            <Button variant="outline" onClick={() => setIsBlockModalOpen(true)}>
              <Ban className="mr-2 h-4 w-4" />
              {t("reservationManagement.blockSeatsButton")}
            </Button>
            <Button onClick={handleCreateReservation}>
              <Plus className="mr-2 h-4 w-4" />
              {t("reservationManagement.addReservationButton")}
            </Button>
          </div>
        </div>
      </CardHeader>

      <CardContent>
        <SearchAndFilter
          onSearch={handleSearch}
          onFilter={handleFilter}
          filterOptions={[
            {
              key: "eventId",
              label: t("reservationManagement.eventFilterLabel"),
              type: "select",
              options: events.map((event) => ({
                value: event.id?.toString() || "",
                label: event.name || "",
              })),
            },
          ]}
          initialFilters={currentFilters}
        />

        <PaginationWrapper
          data={filteredReservations}
          itemsPerPage={100}
          paginationLabel={t("reservationManagement.paginationLabel")}
        >
          {(paginatedData) => (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>
                    {t("reservationManagement.tableHeaderUser")}
                  </TableHead>
                  <TableHead>
                    {t("reservationManagement.tableHeaderEvent")}
                  </TableHead>
                  <TableHead>
                    {t("reservationManagement.tableHeaderSeat")}
                  </TableHead>
                  <TableHead>
                    {t("reservationManagement.tableHeaderReservedDate")}
                  </TableHead>
                  <TableHead>
                    {t("reservationManagement.tableHeaderActions")}
                  </TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {paginatedData.map((reservation) => {
                  const event = events.find(
                    (event) => event.id === reservation.eventId,
                  );

                  return (
                    <TableRow key={reservation.id?.toString()}>
                      <TableCell>{reservation.user?.username}</TableCell>
                      <TableCell>
                        {event ? (
                          <Button
                            variant="link"
                            className="p-0 h-auto font-normal text-blue-600 hover:text-blue-800"
                            onClick={() =>
                              event.id && handleEventClick(event.id)
                            }
                          >
                            {event.name}
                            <ExternalLink className="ml-1 h-3 w-3" />
                          </Button>
                        ) : (
                          t("reservationManagement.unknownEvent")
                        )}
                      </TableCell>
                      <TableCell>
                        {reservation.seat ? (
                          <Button
                            variant="link"
                            className="p-0 h-auto font-normal text-blue-600 hover:text-blue-800"
                            onClick={() =>
                              reservation.seat?.id &&
                              handleSeatClick(reservation.seat.id)
                            }
                          >
                            <Badge variant="outline">
                              {reservation.seat.seatNumber}
                            </Badge>
                            <ExternalLink className="ml-1 h-3 w-3" />
                          </Button>
                        ) : (
                          <Badge variant="outline">
                            {t("reservationManagement.unknownSeat")}
                          </Badge>
                        )}
                      </TableCell>
                      <TableCell>
                        {reservation.reservationDateTime
                          ? new Date(
                              reservation.reservationDateTime,
                            ).toLocaleString([], {
                              year: "numeric",
                              month: "2-digit",
                              day: "2-digit",
                              hour: "2-digit",
                              minute: "2-digit",
                            })
                          : t("reservationManagement.unknownDate")}
                      </TableCell>
                      <TableCell>
                        <div className="flex gap-2">
                          <Button
                            variant="destructive"
                            size="sm"
                            onClick={() => handleDeleteReservation(reservation)}
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
          )}
        </PaginationWrapper>
      </CardContent>

      {isModalOpen && (
        <ReservationFormModal
          users={users}
          events={events}
          reservations={reservations}
          onSubmit={async (reservationData) => {
            await createReservation(reservationData);
            setIsModalOpen(false);
          }}
          onClose={() => setIsModalOpen(false)}
        />
      )}

      {isBlockModalOpen && (
        <BlockSeatsModal
          events={events}
          onSubmit={async (blockData) => {
            await blockSeats(blockData);
            setIsBlockModalOpen(false);
          }}
          onClose={() => setIsBlockModalOpen(false)}
        />
      )}
    </Card>
  );
}
