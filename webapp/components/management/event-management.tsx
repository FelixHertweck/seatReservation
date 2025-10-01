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
import { Skeleton } from "@/components/ui/skeleton";
import { SearchAndFilter } from "@/components/common/search-and-filter";
import { EventFormModal } from "@/components/management/event-form-modal";
import type {
  EventResponseDto,
  EventLocationResponseDto,
  EventRequestDto,
} from "@/api";
import { useT } from "@/lib/i18n/hooks";
import { PaginationWrapper } from "@/components/common/pagination-wrapper";

export interface EventManagementProps {
  events: EventResponseDto[];
  allLocations: EventLocationResponseDto[];
  createEvent: (event: EventRequestDto) => Promise<EventResponseDto>;
  updateEvent: (
    id: bigint,
    event: EventRequestDto,
  ) => Promise<EventResponseDto>;
  deleteEvent: (id: bigint) => Promise<void>;
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
  };

  const handleDeleteEvent = async (event: EventResponseDto) => {
    if (
      event.id &&
      confirm(t("eventManagement.confirmDelete", { eventName: event.name }))
    ) {
      await deleteEvent(event.id);
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
            <CardTitle>{t("eventManagement.title")}</CardTitle>
            <CardDescription>
              {t("eventManagement.description")}
            </CardDescription>
          </div>
          <Button onClick={handleCreateEvent}>
            <Plus className="mr-2 h-4 w-4" />
            {t("eventManagement.addEventButton")}
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
              label: t("eventManagement.locationFilterLabel"),
              type: "select",
              options: allLocations.map((loc) => ({
                value: loc.id?.toString() || "",
                label: loc.name || "",
              })),
            },
          ]}
          initialFilters={currentFilters}
        />

        <PaginationWrapper
          data={filteredEvents}
          itemsPerPage={100}
          paginationLabel={t("eventManagement.paginationLabel")}
        >
          {(paginatedData) => (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>{t("eventManagement.tableHeaderName")}</TableHead>
                  <TableHead>
                    {t("eventManagement.tableHeaderDescription")}
                  </TableHead>
                  <TableHead>
                    {t("eventManagement.tableHeaderStartTime")}
                  </TableHead>
                  <TableHead>
                    {t("eventManagement.tableHeaderEndTime")}
                  </TableHead>
                  <TableHead>
                    {t("eventManagement.tableHeaderBookingStartTime")}
                  </TableHead>
                  <TableHead>
                    {t("eventManagement.tableHeaderBookingDeadline")}
                  </TableHead>
                  <TableHead>
                    {t("eventManagement.tableHeaderLocation")}
                  </TableHead>
                  <TableHead>
                    {t("eventManagement.tableHeaderActions")}
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
                          <TableCell className="font-medium">
                            {event.name}
                          </TableCell>
                          <TableCell className="max-w-xs truncate">
                            {event.description}
                          </TableCell>
                          <TableCell>
                            {event.startTime
                              ? new Date(event.startTime).toLocaleString()
                              : t("eventManagement.tbd")}
                          </TableCell>
                          <TableCell>
                            {event.endTime
                              ? new Date(event.endTime).toLocaleString()
                              : t("eventManagement.tbd")}
                          </TableCell>
                          <TableCell>
                            {event.bookingStartTime
                              ? new Date(
                                  event.bookingStartTime,
                                ).toLocaleString()
                              : t("eventManagement.tbd")}
                          </TableCell>
                          <TableCell>
                            {event.bookingDeadline
                              ? new Date(event.bookingDeadline).toLocaleString()
                              : t("eventManagement.tbd")}
                          </TableCell>
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
                              t("eventManagement.noLocation")
                            )}
                          </TableCell>
                          <TableCell>
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
