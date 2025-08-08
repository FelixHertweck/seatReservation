"use client";

import { useState, useEffect } from "react";
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
import { SearchAndFilter } from "@/components/common/search-and-filter";
import { EventFormModal } from "@/components/manager/event-form-modal";
import type {
  DetailedEventResponseDto,
  EventLocationResponseDto,
  EventRequestDto,
} from "@/api";

export interface EventManagementProps {
  events: DetailedEventResponseDto[];
  allLocations: EventLocationResponseDto[];
  createEvent: (event: EventRequestDto) => Promise<DetailedEventResponseDto>;
  updateEvent: (
    id: bigint,
    event: EventRequestDto,
  ) => Promise<DetailedEventResponseDto>;
  deleteEvent: (id: bigint) => Promise<void>;
  onNavigateToLocation?: (locationId: bigint) => void;
  initialFilter?: Record<string, string>;
}

export function EventManagement({
  events,
  allLocations,
  createEvent,
  updateEvent,
  deleteEvent,
  onNavigateToLocation,
  initialFilter = {},
}: EventManagementProps) {
  const [filteredEvents, setFilteredEvents] = useState(events);
  const [selectedEvent, setSelectedEvent] =
    useState<DetailedEventResponseDto | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isCreating, setIsCreating] = useState(false);
  const [currentFilters, setCurrentFilters] =
    useState<Record<string, string>>(initialFilter);

  useEffect(() => {
    setCurrentFilters(initialFilter);
  }, [initialFilter]);

  useEffect(() => {
    applyFilters("", currentFilters);
  }, [events, currentFilters]);

  const applyFilters = (
    searchQuery: string,
    filters: Record<string, string>,
  ) => {
    let filtered = events;

    // Apply search
    if (searchQuery) {
      filtered = filtered.filter(
        (event) =>
          event.name?.toLowerCase().includes(searchQuery.toLowerCase()) ||
          event.description?.toLowerCase().includes(searchQuery.toLowerCase()),
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

  const handleCreateEvent = () => {
    setSelectedEvent(null);
    setIsCreating(true);
    setIsModalOpen(true);
  };

  const handleEditEvent = (event: DetailedEventResponseDto) => {
    setSelectedEvent(event);
    setIsCreating(false);
    setIsModalOpen(true);
  };

  const handleDeleteEvent = async (event: DetailedEventResponseDto) => {
    if (event.id && confirm(`Are you sure you want to delete ${event.name}?`)) {
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
            <CardTitle>Event Management</CardTitle>
            <CardDescription>Create and manage events</CardDescription>
          </div>
          <Button onClick={handleCreateEvent}>
            <Plus className="mr-2 h-4 w-4" />
            Add Event
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
              label: "Location",
              type: "select",
              options: allLocations.map((loc) => ({
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
              <TableHead>Description</TableHead>
              <TableHead>Start Time</TableHead>
              <TableHead>End Time</TableHead>
              <TableHead>Location</TableHead>
              <TableHead>Actions</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {filteredEvents.map((event) => {
              const location = allLocations.find(
                (loc) => loc.id === event.eventLocationId,
              );

              return (
                <TableRow key={event.id?.toString()}>
                  <TableCell className="font-medium">{event.name}</TableCell>
                  <TableCell className="max-w-xs truncate">
                    {event.description}
                  </TableCell>
                  <TableCell>
                    {event.startTime
                      ? new Date(event.startTime).toLocaleString()
                      : "TBD"}
                  </TableCell>
                  <TableCell>
                    {event.endTime
                      ? new Date(event.endTime).toLocaleString()
                      : "TBD"}
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
                      "No location"
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
