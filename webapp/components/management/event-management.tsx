"use client";

import { useState, useEffect, useCallback } from "react";
import { Plus, Edit, Trash2, ExternalLink, Mail, Clock, X } from "lucide-react";
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
import { Skeleton } from "@/components/ui/skeleton";
import { SearchAndFilter } from "@/components/common/search-and-filter";
import { EventFormModal } from "@/components/management/event-form-modal";
import { TruncatedCell } from "@/components/common/truncated-cell";
import type {
  EventResponseDto,
  EventLocationResponseDto,
  EventRequestDto,
} from "@/api";
import { useT } from "@/lib/i18n/hooks";
import { PaginationWrapper } from "@/components/common/pagination-wrapper";
import { useSortableData } from "@/lib/table-sorting";
import { formatDateTime } from "@/lib/utils";

export interface EventManagementProps {
  events: EventResponseDto[];
  allLocations: EventLocationResponseDto[];
  createEvent: (event: EventRequestDto) => Promise<EventResponseDto>;
  updateEvent: (
    id: bigint,
    event: EventRequestDto,
  ) => Promise<EventResponseDto>;
  deleteEvent: (ids: bigint[]) => Promise<void>;
  onNavigateToLocation?: (locationId: bigint) => void;
  initialFilter?: Record<string, string>;
  isLoading?: boolean;
}

export function EventManagement({
  events,
  allLocations,
  createEvent,
  updateEvent,
  deleteEvent,
  onNavigateToLocation,
  isLoading = false,
  initialFilter = {},
}: EventManagementProps) {
  const t = useT();

  const [filteredEvents, setFilteredEvents] = useState(events);
  const [selectedEvent, setSelectedEvent] = useState<EventResponseDto | null>(
    null,
  );
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isCreating, setIsCreating] = useState(false);
  const [currentFilters, setCurrentFilters] =
    useState<Record<string, string>>(initialFilter);
  const [selectedIds, setSelectedIds] = useState<Set<bigint>>(new Set());

  const { sortedData, sortKey, sortDirection, handleSort } =
    useSortableData(filteredEvents);

  useEffect(() => {
    setCurrentFilters(initialFilter);
  }, [initialFilter]);

  const applyFilters = useCallback(
    (searchQuery: string, filters: Record<string, string>) => {
      let filtered = events;

      // Apply search
      if (searchQuery) {
        filtered = filtered.filter(
          (event) =>
            event.name?.toLowerCase().includes(searchQuery.toLowerCase()) ||
            event.description
              ?.toLowerCase()
              .includes(searchQuery.toLowerCase()),
        );
      }

      // Apply filters
      if (filters.eventId) {
        filtered = filtered.filter(
          (event) => event.id?.toString() === filters.eventId,
        );
      }
      if (filters.locationId) {
        filtered = filtered.filter(
          (event) => event.eventLocationId?.toString() === filters.locationId,
        );
      }
      if (filters.reminderStatus && filters.reminderStatus !== "all") {
        filtered = filtered.filter((event) => {
          if (filters.reminderStatus === "sent") {
            return event.isReminderSent === true;
          } else if (filters.reminderStatus === "scheduled") {
            return (
              event.reminderSendDate != null && event.isReminderSent !== true
            );
          } else if (filters.reminderStatus === "notScheduled") {
            return event.reminderSendDate == null;
          }
          return true;
        });
      }

      setFilteredEvents(filtered);
    },
    [events],
  );

  useEffect(() => {
    applyFilters("", currentFilters);
  }, [events, currentFilters, applyFilters]);

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

  const handleCreateEvent = () => {
    setSelectedEvent(null);
    setIsCreating(true);
    setIsModalOpen(true);
  };

  const handleEditEvent = (event: EventResponseDto) => {
    setSelectedEvent(event);
    setIsCreating(false);
    setIsModalOpen(true);
    setSelectedIds(new Set());
  };

  const handleDeleteEvent = async (event: EventResponseDto) => {
    if (
      event.id &&
      confirm(t("eventManagement.confirmDelete", { eventName: event.name }))
    ) {
      await deleteEvent([event.id]);
      setSelectedIds(new Set());
    }
  };

  const handleLocationClick = (locationId: bigint) => {
    if (onNavigateToLocation) {
      onNavigateToLocation(locationId);
    }
  };

  const handleSelectAll = (paginatedData: EventResponseDto[]) => {
    const newSelectedIds = new Set(selectedIds);
    const allCurrentSelected = paginatedData.every((event) =>
      event.id ? selectedIds.has(event.id) : false,
    );

    if (allCurrentSelected) {
      setSelectedIds(new Set());
    } else {
      paginatedData.forEach((event) => {
        if (event.id) newSelectedIds.add(event.id);
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
        t("eventManagement.confirmDeleteMultiple", {
          count: selectedIds.size,
        }),
      )
    ) {
      await deleteEvent(Array.from(selectedIds));
      setSelectedIds(new Set());
    }
  };

  return (
    <Card>
      <CardHeader>
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <CardTitle className="text-xl sm:text-2xl">
              {t("eventManagement.title")}
            </CardTitle>
            <CardDescription className="text-sm">
              {t("eventManagement.description")}
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
            <Button onClick={handleCreateEvent} className="w-full sm:w-auto">
              <Plus className="mr-2 h-4 w-4" />
              {t("eventManagement.addEventButton")}
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
              key: "locationId",
              label: t("eventManagement.locationFilterLabel"),
              type: "select",
              options: allLocations.map((loc) => ({
                value: loc.id?.toString() || "",
                label: loc.name || "",
              })),
            },
            {
              key: "reminderStatus",
              label: t("eventManagement.reminderStatusFilterLabel"),
              type: "select",
              options: [
                {
                  value: "all",
                  label: t("eventManagement.reminderStatusAll"),
                },
                {
                  value: "scheduled",
                  label: t("eventManagement.reminderStatusScheduled"),
                },
                {
                  value: "sent",
                  label: t("eventManagement.reminderStatusSent"),
                },
                {
                  value: "notScheduled",
                  label: t("eventManagement.reminderStatusNotScheduled"),
                },
              ],
            },
          ]}
          initialFilters={currentFilters}
        />

        <PaginationWrapper
          data={sortedData}
          itemsPerPage={100}
          paginationLabel={t("eventManagement.paginationLabel")}
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
                    {paginatedData.every((event) =>
                      event.id ? selectedIds.has(event.id) : false,
                    )
                      ? t("eventManagement.deselectAll")
                      : t("eventManagement.selectAll")}
                  </Button>
                </div>
                <Table className="table-fixed">
                  <TableHeader>
                    <TableRow>
                      <TableHead className="w-[5%]">
                        {t("eventManagement.tableHeaderSelect")}
                      </TableHead>
                      <SortableTableHead
                        sortKey="name"
                        currentSortKey={sortKey}
                        currentSortDirection={sortDirection}
                        onSort={handleSort}
                        className="w-[15%]"
                      >
                        {t("eventManagement.tableHeaderName")}
                      </SortableTableHead>
                      <SortableTableHead
                        sortKey="description"
                        currentSortKey={sortKey}
                        currentSortDirection={sortDirection}
                        onSort={handleSort}
                        className="w-[15%]"
                      >
                        {t("eventManagement.tableHeaderDescription")}
                      </SortableTableHead>
                      <SortableTableHead
                        sortKey="startTime"
                        currentSortKey={sortKey}
                        currentSortDirection={sortDirection}
                        onSort={handleSort}
                        className="w-[8%]"
                      >
                        {t("eventManagement.tableHeaderStartTime")}
                      </SortableTableHead>
                      <SortableTableHead
                        sortKey="endTime"
                        currentSortKey={sortKey}
                        currentSortDirection={sortDirection}
                        onSort={handleSort}
                        className="w-[8%]"
                      >
                        {t("eventManagement.tableHeaderEndTime")}
                      </SortableTableHead>
                      <SortableTableHead
                        sortKey="bookingStartTime"
                        currentSortKey={sortKey}
                        currentSortDirection={sortDirection}
                        onSort={handleSort}
                        className="w-[8%]"
                      >
                        {t("eventManagement.tableHeaderBookingStartTime")}
                      </SortableTableHead>
                      <SortableTableHead
                        sortKey="bookingDeadline"
                        currentSortKey={sortKey}
                        currentSortDirection={sortDirection}
                        onSort={handleSort}
                        className="w-[8%]"
                      >
                        {t("eventManagement.tableHeaderBookingDeadline")}
                      </SortableTableHead>
                      <SortableTableHead
                        sortKey="reminderSendDate"
                        currentSortKey={sortKey}
                        currentSortDirection={sortDirection}
                        onSort={handleSort}
                        className="w-[10%]"
                      >
                        {t("eventManagement.tableHeaderReminderSendDate")}
                      </SortableTableHead>
                      <TableHead className="w-[5%]">
                        <div className="flex items-center justify-center">
                          <Mail className="h-4 w-4" />
                        </div>
                      </TableHead>
                      <SortableTableHead
                        sortKey="location.name"
                        currentSortKey={sortKey}
                        currentSortDirection={sortDirection}
                        onSort={handleSort}
                        className="w-[10%]"
                      >
                        {t("eventManagement.tableHeaderLocation")}
                      </SortableTableHead>
                      <TableHead className="w-[8%]">
                        {t("eventManagement.tableHeaderActions")}
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
                              <Skeleton className="h-4 w-32" />
                            </TableCell>
                            <TableCell>
                              <Skeleton className="h-4 w-48" />
                            </TableCell>
                            <TableCell>
                              <Skeleton className="h-4 w-36" />
                            </TableCell>
                            <TableCell>
                              <Skeleton className="h-4 w-36" />
                            </TableCell>
                            <TableCell>
                              <Skeleton className="h-4 w-36" />
                            </TableCell>
                            <TableCell>
                              <Skeleton className="h-4 w-36" />
                            </TableCell>
                            <TableCell>
                              <Skeleton className="h-4 w-24" />
                            </TableCell>
                            <TableCell>
                              <div className="flex gap-2">
                                <Skeleton className="h-8 w-8" />
                                <Skeleton className="h-8 w-8" />
                              </div>
                            </TableCell>
                          </TableRow>
                        ))
                      : paginatedData.map((event) => {
                          const location = allLocations.find(
                            (loc) => loc.id === event.eventLocationId,
                          );
                          return (
                            <TableRow key={event.id?.toString()}>
                              <TableCell className="w-[5%]">
                                <Checkbox
                                  checked={
                                    event.id ? selectedIds.has(event.id) : false
                                  }
                                  onCheckedChange={() =>
                                    event.id && handleToggleSelect(event.id)
                                  }
                                />
                              </TableCell>
                              <TruncatedCell
                                content={event.name}
                                className="font-medium w-[10%]"
                              />
                              <TruncatedCell
                                content={event.description}
                                className="w-[15%]"
                              />
                              <TableCell className="w-[10%]">
                                {(() => {
                                  const formatted = formatDateTime(
                                    event.startTime,
                                  );
                                  return formatted ? (
                                    <div className="flex flex-col text-sm">
                                      <span>{formatted.date}</span>
                                      <span>{formatted.time}</span>
                                    </div>
                                  ) : (
                                    t("eventManagement.tbd")
                                  );
                                })()}
                              </TableCell>
                              <TableCell className="w-[10%]">
                                {(() => {
                                  const formatted = formatDateTime(
                                    event.endTime,
                                  );
                                  return formatted ? (
                                    <div className="flex flex-col text-sm">
                                      <span>{formatted.date}</span>
                                      <span>{formatted.time}</span>
                                    </div>
                                  ) : (
                                    t("eventManagement.tbd")
                                  );
                                })()}
                              </TableCell>
                              <TableCell className="w-[10%]">
                                {(() => {
                                  const formatted = formatDateTime(
                                    event.bookingStartTime,
                                  );
                                  return formatted ? (
                                    <div className="flex flex-col text-sm">
                                      <span>{formatted.date}</span>
                                      <span>{formatted.time}</span>
                                    </div>
                                  ) : (
                                    t("eventManagement.tbd")
                                  );
                                })()}
                              </TableCell>
                              <TableCell className="w-[10%]">
                                {(() => {
                                  const formatted = formatDateTime(
                                    event.bookingDeadline,
                                  );
                                  return formatted ? (
                                    <div className="flex flex-col text-sm">
                                      <span>{formatted.date}</span>
                                      <span>{formatted.time}</span>
                                    </div>
                                  ) : (
                                    t("eventManagement.tbd")
                                  );
                                })()}
                              </TableCell>
                              <TableCell className="w-[10%]">
                                {(() => {
                                  const formatted = formatDateTime(
                                    event.reminderSendDate,
                                  );
                                  return formatted ? (
                                    <div className="flex flex-col text-sm">
                                      <span>{formatted.date}</span>
                                      <span>{formatted.time}</span>
                                    </div>
                                  ) : (
                                    "-"
                                  );
                                })()}
                              </TableCell>
                              <TableCell className="w-[5%]">
                                <div className="flex items-center justify-center">
                                  {event.isReminderSent ? (
                                    <Mail className="h-4 w-4 text-green-600" />
                                  ) : event.reminderSendDate ? (
                                    <Clock className="h-4 w-4 text-orange-500" />
                                  ) : (
                                    <X className="h-4 w-4 text-red-500" />
                                  )}
                                </div>
                              </TableCell>
                              <TableCell className="w-[10%]">
                                {location ? (
                                  <Button
                                    variant="link"
                                    className="p-0 h-auto font-normal text-blue-600 hover:text-blue-800 truncate justify-start"
                                    onClick={() =>
                                      location.id &&
                                      handleLocationClick(location.id)
                                    }
                                  >
                                    <span className="truncate">
                                      {location.name}
                                    </span>
                                    <ExternalLink className="ml-1 h-3 w-3 flex-shrink-0" />
                                  </Button>
                                ) : (
                                  t("eventManagement.noLocation")
                                )}
                              </TableCell>
                              <TableCell className="w-[8%]">
                                <div className="flex gap-2">
                                  <Button
                                    variant="outline"
                                    size="sm"
                                    onClick={() => handleEditEvent(event)}
                                  >
                                    <Edit className="h-4 w-4" />
                                  </Button>
                                  <Button
                                    variant="destructive"
                                    size="sm"
                                    onClick={() => handleDeleteEvent(event)}
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
                <div className="mb-2">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => handleSelectAll(paginatedData)}
                  >
                    {paginatedData.every((event) =>
                      event.id ? selectedIds.has(event.id) : false,
                    )
                      ? t("eventManagement.deselectAll")
                      : t("eventManagement.selectAll")}
                  </Button>
                </div>
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
                  : paginatedData.map((event) => {
                      const location = allLocations.find(
                        (loc) => loc.id === event.eventLocationId,
                      );
                      return (
                        <Card key={event.id?.toString()}>
                          <CardHeader className="pb-3 flex flex-row items-start space-x-3 space-y-0">
                            <Checkbox
                              checked={
                                event.id ? selectedIds.has(event.id) : false
                              }
                              onCheckedChange={() =>
                                event.id && handleToggleSelect(event.id)
                              }
                              className="mt-1"
                            />
                            <div className="flex-1 min-w-0">
                              <div className="flex items-center gap-2">
                                <CardTitle className="text-base">
                                  {event.name}
                                </CardTitle>
                                {event.isReminderSent && (
                                  <Mail className="h-4 w-4 text-green-600 flex-shrink-0" />
                                )}
                              </div>
                              {event.description && (
                                <CardDescription className="text-sm mt-1 line-clamp-2">
                                  {event.description}
                                </CardDescription>
                              )}
                            </div>
                          </CardHeader>
                          <CardContent className="space-y-3">
                            <div className="grid grid-cols-1 gap-2 text-sm">
                              <div>
                                <p className="text-xs text-muted-foreground mb-1">
                                  {t("eventManagement.tableHeaderStartTime")}
                                </p>
                                <p className="text-sm">
                                  {event.startTime
                                    ? new Date(event.startTime).toLocaleString()
                                    : t("eventManagement.tbd")}
                                </p>
                              </div>
                              <div>
                                <p className="text-xs text-muted-foreground mb-1">
                                  {t("eventManagement.tableHeaderEndTime")}
                                </p>
                                <p className="text-sm">
                                  {event.endTime
                                    ? new Date(event.endTime).toLocaleString()
                                    : t("eventManagement.tbd")}
                                </p>
                              </div>
                              <div>
                                <p className="text-xs text-muted-foreground mb-1">
                                  {t(
                                    "eventManagement.tableHeaderBookingStartTime",
                                  )}
                                </p>
                                <p className="text-sm">
                                  {event.bookingStartTime
                                    ? new Date(
                                        event.bookingStartTime,
                                      ).toLocaleString()
                                    : t("eventManagement.tbd")}
                                </p>
                              </div>
                              <div>
                                <p className="text-xs text-muted-foreground mb-1">
                                  {t(
                                    "eventManagement.tableHeaderBookingDeadline",
                                  )}
                                </p>
                                <p className="text-sm">
                                  {event.bookingDeadline
                                    ? new Date(
                                        event.bookingDeadline,
                                      ).toLocaleString()
                                    : t("eventManagement.tbd")}
                                </p>
                              </div>
                              <div>
                                <p className="text-xs text-muted-foreground mb-1">
                                  {t(
                                    "eventManagement.tableHeaderReminderSendDate",
                                  )}
                                </p>
                                <p className="text-sm">
                                  {event.reminderSendDate
                                    ? new Date(
                                        event.reminderSendDate,
                                      ).toLocaleString()
                                    : t("eventManagement.tbd")}
                                </p>
                              </div>
                            </div>

                            {location && (
                              <div>
                                <p className="text-xs text-muted-foreground mb-1">
                                  {t("eventManagement.tableHeaderLocation")}
                                </p>
                                <Button
                                  variant="link"
                                  className="p-0 h-auto font-normal text-blue-600 hover:text-blue-800 text-sm"
                                  onClick={() =>
                                    location.id &&
                                    handleLocationClick(location.id)
                                  }
                                >
                                  {location.name}
                                  <ExternalLink className="ml-1 h-3 w-3" />
                                </Button>
                              </div>
                            )}

                            <div className="flex gap-2 pt-2">
                              <Button
                                variant="outline"
                                size="sm"
                                className="flex-1 bg-transparent"
                                onClick={() => handleEditEvent(event)}
                              >
                                <Edit className="mr-2 h-4 w-4" />
                                {t("eventManagement.editButtonLabel")}
                              </Button>
                              <Button
                                variant="destructive"
                                size="sm"
                                className="flex-1"
                                onClick={() => handleDeleteEvent(event)}
                              >
                                <Trash2 className="mr-2 h-4 w-4" />
                                {t("eventManagement.deleteButtonLabel")}
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
        <EventFormModal
          allLocations={allLocations}
          event={selectedEvent}
          isCreating={isCreating}
          onSubmit={async (eventData) => {
            if (isCreating) {
              await createEvent(eventData);
            } else if (selectedEvent?.id) {
              await updateEvent(selectedEvent.id, eventData);
            }
            setIsModalOpen(false);
          }}
          onClose={() => setIsModalOpen(false)}
        />
      )}
    </Card>
  );
}
