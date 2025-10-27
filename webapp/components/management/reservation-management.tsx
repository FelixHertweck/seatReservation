"use client";

import { useState, useEffect, useCallback } from "react";
import { Plus, Trash2, Ban, ExternalLink, Download } from "lucide-react"; // Added Download icon
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
import { SortableTableHead } from "@/components/common/sortable-table-head";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { SearchAndFilter } from "@/components/common/search-and-filter";
import { ReservationFormModal } from "@/components/management/reservation-form-modal";
import { BlockSeatsModal } from "@/components/management/block-seats-modal";
import { TruncatedCell } from "@/components/common/truncated-cell";
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
  EventResponseDto,
  ReservationResponseDto,
  ReservationRequestDto,
  BlockSeatsRequestDto,
  EventLocationResponseDto,
  SeatDto,
} from "@/api";
import { customSerializer } from "@/lib/jsonBodySerializer";
import { useT } from "@/lib/i18n/hooks";
import { useSortableData } from "@/lib/table-sorting";

export interface ReservationManagementProps {
  users: UserDto[];
  seats: SeatDto[];
  events: EventResponseDto[];
  locations: EventLocationResponseDto[];
  reservations: ReservationResponseDto[];
  createReservation: (
    reservation: ReservationRequestDto,
  ) => Promise<ReservationResponseDto[]>;
  deleteReservation: (ids: bigint[]) => Promise<unknown>;
  blockSeats: (
    request: BlockSeatsRequestDto,
  ) => Promise<ReservationResponseDto[]>;
  exportCSV: (eventId: bigint) => Promise<Blob>;
  exportPDF: (eventId: bigint) => Promise<Blob>;
  onNavigateToEvent?: (eventId: bigint) => void;
  onNavigateToSeat?: (seatId: bigint) => void;
  initialFilter?: Record<string, string>;
  isLoading?: boolean;
}

export function ReservationManagement({
  users,
  seats,
  events,
  locations,
  reservations,
  createReservation,
  deleteReservation,
  blockSeats,
  onNavigateToEvent,
  onNavigateToSeat,
  exportCSV,
  exportPDF,
  initialFilter = {},
  isLoading = false,
}: ReservationManagementProps) {
  const t = useT();

  const [filteredReservations, setFilteredReservations] =
    useState(reservations);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isBlockModalOpen, setIsBlockModalOpen] = useState(false);
  const [isExportModalOpen, setIsExportModalOpen] = useState(false);
  const [selectedEventForExport, setSelectedEventForExport] =
    useState<string>("");
  const [selectedFormat, setSelectedFormat] = useState<string>("");
  const [currentFilters, setCurrentFilters] =
    useState<Record<string, string>>(initialFilter);
  const [selectedIds, setSelectedIds] = useState<Set<bigint>>(new Set());

  const { sortedData, sortKey, sortDirection, handleSort } =
    useSortableData(filteredReservations);

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
      if (filters.status) {
        filtered = filtered.filter(
          (reservation) => reservation.status === filters.status,
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
    reservation: ReservationResponseDto,
  ) => {
    if (reservation.id && confirm(t("reservationManagement.confirmDelete"))) {
      await deleteReservation([reservation.id]);
      setSelectedIds(new Set());
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

  const handleExportReservations = async () => {
    if (!selectedEventForExport) return;

    const eventId = BigInt(selectedEventForExport);
    const event = events.find((e) => e.id === eventId);

    let blob: Blob;
    let fileName: string;

    if (selectedFormat === "csv") {
      blob = await exportCSV(eventId);
      fileName = `reservations-${event?.name?.replace(/[^a-z0-9]/gi, "_").toLowerCase() || "event"}-${new Date().toISOString().split("T")[0]}.csv`;
    } else if (selectedFormat === "pdf") {
      blob = await exportPDF(eventId);
      fileName = `reservations-${event?.name?.replace(/[^a-z0-9]/gi, "_").toLowerCase() || "event"}-${new Date().toISOString().split("T")[0]}.pdf`;
    } else {
      const eventReservations = reservations.filter(
        (r) => r.eventId === eventId,
      );
      const exportData = customSerializer.json(eventReservations);
      blob = new Blob([exportData], {
        type: "application/json",
      });
      fileName = `reservations-${event?.name?.replace(/[^a-z0-9]/gi, "_").toLowerCase() || "event"}-${new Date().toISOString().split("T")[0]}.json`;
    }

    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = fileName;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);

    setIsExportModalOpen(false);
    setSelectedEventForExport("");
    setSelectedFormat("");
  };

  const handleSelectAll = (paginatedData: ReservationResponseDto[]) => {
    const allCurrentSelected = paginatedData.every((reservation) =>
      reservation.id ? selectedIds.has(reservation.id) : false,
    );

    if (allCurrentSelected) {
      // Clear ALL selections when deselecting
      setSelectedIds(new Set());
    } else {
      // Add current page items to selection
      const newSelectedIds = new Set(selectedIds);
      paginatedData.forEach((reservation) => {
        if (reservation.id) newSelectedIds.add(reservation.id);
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
        t("reservationManagement.confirmDeleteMultiple", {
          count: selectedIds.size,
        }),
      )
    ) {
      await deleteReservation(Array.from(selectedIds));
      setSelectedIds(new Set());
    }
  };

  return (
    <Card>
      <CardHeader>
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <CardTitle className="text-xl sm:text-2xl">
              {t("reservationManagement.title")}
            </CardTitle>
            <CardDescription className="text-sm">
              {t("reservationManagement.description")}
            </CardDescription>
          </div>
          <div className="flex flex-col sm:flex-row gap-2 w-full sm:w-auto">
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
            <Dialog
              open={isExportModalOpen}
              onOpenChange={setIsExportModalOpen}
            >
              <DialogTrigger asChild>
                <Button
                  variant="outline"
                  className="w-full sm:w-auto bg-transparent"
                >
                  <Download className="mr-2 h-4 w-4" />
                  <span className="sm:inline">
                    {t("reservationManagement.exportReservationsButton")}
                  </span>
                </Button>
              </DialogTrigger>
              <DialogContent onInteractOutside={(e) => e.preventDefault()}>
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
                  <div>
                    <label className="text-sm font-medium">
                      {t("reservationManagement.selectFormatLabel")}
                    </label>
                    <Select
                      value={selectedFormat}
                      onValueChange={setSelectedFormat}
                    >
                      <SelectTrigger>
                        <SelectValue
                          placeholder={t(
                            "reservationManagement.chooseFormatToExportPlaceholder",
                          )}
                        />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="json">
                          {t("reservationManagement.jsonOption")}
                        </SelectItem>
                        <SelectItem value="csv">
                          {t("reservationManagement.csvOption")}
                        </SelectItem>
                        <SelectItem value="pdf">
                          {t("reservationManagement.pdfOption")}
                        </SelectItem>
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
                      disabled={
                        !selectedEventForExport || selectedFormat === ""
                      }
                    >
                      {selectedFormat === "csv"
                        ? t("reservationManagement.exportCsvButton")
                        : selectedFormat === "pdf"
                          ? t("reservationManagement.exportPdfButton")
                          : t("reservationManagement.exportJsonButton")}
                    </Button>
                  </div>
                </div>
              </DialogContent>
            </Dialog>
            <Button
              variant="outline"
              onClick={() => setIsBlockModalOpen(true)}
              className="w-full sm:w-auto"
            >
              <Ban className="mr-2 h-4 w-4" />
              <span className="sm:inline">
                {t("reservationManagement.blockSeatsButton")}
              </span>
            </Button>
            <Button
              onClick={handleCreateReservation}
              className="w-full sm:w-auto"
            >
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
            {
              key: "status",
              label: t("reservationManagement.statusFilterLabel"),
              type: "select",
              options: [
                {
                  value: "RESERVED",
                  label: t("reservationManagement.statusReserved"),
                },
                {
                  value: "BLOCKED",
                  label: t("reservationManagement.statusBlocked"),
                },
              ],
            },
          ]}
          initialFilters={currentFilters}
        />

        <PaginationWrapper
          data={sortedData}
          itemsPerPage={100}
          paginationLabel={t("reservationManagement.paginationLabel")}
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
                    {paginatedData.every((reservation) =>
                      reservation.id ? selectedIds.has(reservation.id) : false,
                    )
                      ? t("reservationManagement.deselectAll")
                      : t("reservationManagement.selectAll")}
                  </Button>
                </div>
                <Table className="table-fixed">
                  <TableHeader>
                    <TableRow>
                      <TableHead className="w-[5%]">
                        {t("reservationManagement.tableHeaderSelect")}
                      </TableHead>
                      <SortableTableHead
                        sortKey="user.username"
                        currentSortKey={sortKey}
                        currentSortDirection={sortDirection}
                        onSort={handleSort}
                        className="w-[12%]"
                      >
                        {t("reservationManagement.tableHeaderUser")}
                      </SortableTableHead>
                      <SortableTableHead
                        sortKey="eventId"
                        currentSortKey={sortKey}
                        currentSortDirection={sortDirection}
                        onSort={handleSort}
                        className="w-[20%]"
                      >
                        {t("reservationManagement.tableHeaderEvent")}
                      </SortableTableHead>
                      <SortableTableHead
                        sortKey="seat.seatNumber"
                        currentSortKey={sortKey}
                        currentSortDirection={sortDirection}
                        onSort={handleSort}
                        className="w-[12%]"
                      >
                        {t("reservationManagement.tableHeaderSeat")}
                      </SortableTableHead>
                      <SortableTableHead
                        sortKey="reservationStatus"
                        currentSortKey={sortKey}
                        currentSortDirection={sortDirection}
                        onSort={handleSort}
                        className="w-[10%]"
                      >
                        {t("reservationManagement.tableHeaderStatus")}
                      </SortableTableHead>
                      <SortableTableHead
                        sortKey="reservationDateTime"
                        currentSortKey={sortKey}
                        currentSortDirection={sortDirection}
                        onSort={handleSort}
                        className="w-[15%]"
                      >
                        {t("reservationManagement.tableHeaderReservedDate")}
                      </SortableTableHead>
                      <TableHead className="w-[8%]">
                        {t("reservationManagement.tableHeaderActions")}
                      </TableHead>
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
                              <Skeleton className="h-4 w-24" />
                            </TableCell>
                            <TableCell>
                              <Skeleton className="h-4 w-32" />
                            </TableCell>
                            <TableCell>
                              <Skeleton className="h-5 w-16" />
                            </TableCell>
                            <TableCell>
                              <Skeleton className="h-5 w-20" />
                            </TableCell>
                            <TableCell>
                              <Skeleton className="h-4 w-32" />
                            </TableCell>
                            <TableCell>
                              <div className="flex gap-2">
                                <Skeleton className="h-8 w-8" />
                              </div>
                            </TableCell>
                          </TableRow>
                        ))
                      : paginatedData.map((reservation) => {
                          const event = events.find(
                            (event) => event.id === reservation.eventId,
                          );

                          return (
                            <TableRow key={reservation.id?.toString()}>
                              <TableCell>
                                <Checkbox
                                  checked={
                                    reservation.id
                                      ? selectedIds.has(reservation.id)
                                      : false
                                  }
                                  onCheckedChange={() =>
                                    reservation.id &&
                                    handleToggleSelect(reservation.id)
                                  }
                                />
                              </TableCell>
                              <TruncatedCell
                                content={reservation.user?.username}
                                className="w-[12%]"
                              />
                              <TableCell className="w-[20%]">
                                {event ? (
                                  <Button
                                    variant="link"
                                    className="p-0 h-auto font-normal text-blue-600 hover:text-blue-800 truncate w-full justify-start"
                                    onClick={() =>
                                      event.id && handleEventClick(event.id)
                                    }
                                  >
                                    <span className="truncate">
                                      {event.name}
                                    </span>
                                    <ExternalLink className="ml-1 h-3 w-3 flex-shrink-0" />
                                  </Button>
                                ) : (
                                  t("reservationManagement.unknownEvent")
                                )}
                              </TableCell>
                              <TableCell className="w-[12%]">
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
                                <Badge
                                  variant={
                                    reservation.status === "BLOCKED"
                                      ? "secondary"
                                      : "default"
                                  }
                                >
                                  {reservation.status === "BLOCKED"
                                    ? t("reservationManagement.statusBlocked")
                                    : t("reservationManagement.statusReserved")}
                                </Badge>
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
                                    onClick={() =>
                                      handleDeleteReservation(reservation)
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
                          <Skeleton className="h-5 w-1/2" />
                          <Skeleton className="h-4 w-3/4 mt-2" />
                        </CardHeader>
                        <CardContent>
                          <Skeleton className="h-4 w-full" />
                        </CardContent>
                      </Card>
                    ))
                  : paginatedData.map((reservation) => {
                      const event = events.find(
                        (event) => event.id === reservation.eventId,
                      );

                      return (
                        <Card key={reservation.id?.toString()}>
                          <CardHeader className="pb-3 flex flex-row items-start space-x-3 space-y-0">
                            <Checkbox
                              checked={
                                reservation.id
                                  ? selectedIds.has(reservation.id)
                                  : false
                              }
                              onCheckedChange={() =>
                                reservation.id &&
                                handleToggleSelect(reservation.id)
                              }
                              className="mt-1"
                            />
                            <div className="flex-1 min-w-0">
                              <CardTitle className="text-base">
                                {reservation.user?.username}
                              </CardTitle>
                              <CardDescription className="text-sm mt-1">
                                <Badge
                                  variant={
                                    reservation.status === "BLOCKED"
                                      ? "secondary"
                                      : "default"
                                  }
                                  className="text-xs"
                                >
                                  {reservation.status === "BLOCKED"
                                    ? t("reservationManagement.statusBlocked")
                                    : t("reservationManagement.statusReserved")}
                                </Badge>
                              </CardDescription>
                            </div>
                          </CardHeader>
                          <CardContent className="space-y-3">
                            {event && (
                              <div>
                                <p className="text-xs text-muted-foreground mb-1">
                                  {t("reservationManagement.tableHeaderEvent")}
                                </p>
                                <Button
                                  variant="link"
                                  className="p-0 h-auto font-normal text-blue-600 hover:text-blue-800 text-sm"
                                  onClick={() =>
                                    event.id && handleEventClick(event.id)
                                  }
                                >
                                  {event.name}
                                  <ExternalLink className="ml-1 h-3 w-3" />
                                </Button>
                              </div>
                            )}

                            {reservation.seat && (
                              <div>
                                <p className="text-xs text-muted-foreground mb-1">
                                  {t("reservationManagement.tableHeaderSeat")}
                                </p>
                                <Button
                                  variant="link"
                                  className="p-0 h-auto font-normal text-blue-600 hover:text-blue-800 text-sm"
                                  onClick={() =>
                                    reservation.seat?.id &&
                                    handleSeatClick(reservation.seat.id)
                                  }
                                >
                                  <Badge variant="outline" className="text-xs">
                                    {reservation.seat.seatNumber}
                                  </Badge>
                                  <ExternalLink className="ml-1 h-3 w-3" />
                                </Button>
                              </div>
                            )}

                            <div>
                              <p className="text-xs text-muted-foreground mb-1">
                                {t(
                                  "reservationManagement.tableHeaderReservedDate",
                                )}
                              </p>
                              <p className="text-sm">
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
                              </p>
                            </div>

                            <div className="flex gap-2 pt-2">
                              <Button
                                variant="destructive"
                                size="sm"
                                className="flex-1"
                                onClick={() =>
                                  handleDeleteReservation(reservation)
                                }
                              >
                                <Trash2 className="mr-2 h-4 w-4" />
                                {t("reservationManagement.deleteButtonLabel")}
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
        <ReservationFormModal
          users={users}
          seats={seats}
          locations={locations}
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
          seats={seats}
          locations={locations}
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
