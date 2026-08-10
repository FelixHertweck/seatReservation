"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import {
  Plus,
  Edit,
  Trash2,
  CalendarDays,
  Users,
  BookmarkCheck,
  Ticket,
} from "lucide-react";

import { useT } from "@/lib/i18n/hooks";
import { formatDateTime } from "@/lib/utils";
import { PageHeader } from "@/components/page-header";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/custom-ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/custom-ui/skeleton";
import { SearchAndFilter } from "@/components/common/search-and-filter";
import { EventFormModal } from "@/components/management/event-form-modal";
import { useManagementEvents } from "@/hooks/use-management-events";
import type { EventResponseDto } from "@/api";

export default function ManagementEventsPage() {
  const t = useT();
  const searchParams = useSearchParams();
  const lockedLocationId = searchParams.get("locationId") ?? undefined;

  const {
    events,
    locations,
    users,
    isLoading,
    createEvent,
    updateEvent,
    deleteEvent,
  } = useManagementEvents();

  const [searchQuery, setSearchQuery] = useState("");
  const [filters, setFilters] = useState<Record<string, unknown>>({});
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isCreating, setIsCreating] = useState(false);
  const [selectedEvent, setSelectedEvent] = useState<EventResponseDto | null>(
    null,
  );
  const [deletingEventId, setDeletingEventId] = useState<string | null>(null);

  const locationById = useMemo(
    () => new Map(locations.map((l) => [l.id, l])),
    [locations],
  );

  const locationOptions = useMemo(
    () =>
      locations
        .filter((l) => l.id && l.name)
        .map((l) => ({ value: l.id!, label: l.name! })),
    [locations],
  );

  const filteredEvents = useMemo(() => {
    const query = searchQuery.toLowerCase();
    const locationId = filters.locationId as string | undefined;
    const base = events.filter((e) => {
      const matchesQuery = !query || e.name?.toLowerCase().includes(query);
      const matchesLocation = !locationId || e.eventLocationId === locationId;
      return matchesQuery && matchesLocation;
    });
    return [...base].sort(
      (a, b) => (a.startTime?.getTime() ?? 0) - (b.startTime?.getTime() ?? 0),
    );
  }, [events, searchQuery, filters]);

  const groups = useMemo(() => {
    const map = new Map<string, EventResponseDto[]>();
    for (const event of filteredEvents) {
      const key = event.eventLocationId ?? "";
      map.set(key, [...(map.get(key) ?? []), event]);
    }
    return [...map.entries()];
  }, [filteredEvents]);

  const handleCreate = () => {
    setSelectedEvent(null);
    setIsCreating(true);
    setIsModalOpen(true);
  };

  const handleEdit = (event: EventResponseDto) => {
    setSelectedEvent(event);
    setIsCreating(false);
    setIsModalOpen(true);
  };

  const handleDelete = async (event: EventResponseDto) => {
    if (!event.id) return;
    if (confirm(t("management.events.deleteConfirm", { name: event.name }))) {
      setDeletingEventId(event.id);
      try {
        await deleteEvent([event.id]);
      } finally {
        setDeletingEventId(null);
      }
    }
  };

  const formModalEvent = selectedEvent
    ? selectedEvent
    : lockedLocationId
      ? ({ eventLocationId: lockedLocationId } as EventResponseDto)
      : null;

  return (
    <div className="container mx-auto p-4 sm:p-6">
      <PageHeader
        title={t("management.events.title")}
        description={t("management.events.description")}
        actions={
          <Button
            onClick={handleCreate}
            aria-label={t("management.events.newEvent")}
          >
            <Plus className="h-4 w-4" />
            <span className="hidden sm:inline">
              {t("management.events.newEvent")}
            </span>
          </Button>
        }
        search={
          <SearchAndFilter
            onSearch={setSearchQuery}
            onFilter={setFilters}
            filterOptions={
              locationOptions.length > 0
                ? [
                    {
                      key: "locationId",
                      label: t("management.events.locationFilterLabel"),
                      type: "select",
                      options: locationOptions,
                    },
                  ]
                : []
            }
            initialQuery={searchQuery}
            className="w-full"
          />
        }
      />

      {isLoading && (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {Array.from({ length: 6 }, (_, i) => (
            <Skeleton key={i} className="h-40 rounded-lg" />
          ))}
        </div>
      )}

      {!isLoading && filteredEvents.length === 0 && (
        <Card>
          <CardContent className="py-12 text-center text-muted-foreground">
            {events.length === 0
              ? t("management.events.empty")
              : t("management.events.noResults")}
          </CardContent>
        </Card>
      )}

      {!isLoading && filteredEvents.length > 0 && (
        <div className="space-y-6">
          {groups.map(([locationId, locationEvents]) => {
            const location = locationById.get(locationId);
            return (
              <div key={locationId || "none"}>
                <h2 className="mb-2 text-sm font-semibold text-muted-foreground">
                  {location?.name ?? t("management.events.noLocation")}
                </h2>
                <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
                  {locationEvents.map((event) => {
                    const capacity = location?.seatIds?.length ?? 0;
                    const reserved =
                      event.seatStatuses?.filter((s) => s.status === "RESERVED")
                        .length ?? 0;
                    const start = formatDateTime(event.startTime);
                    const bookingStart = formatDateTime(event.bookingStartTime);
                    const bookingDeadline = formatDateTime(
                      event.bookingDeadline,
                    );

                    return (
                      <Card key={event.id}>
                        <CardHeader>
                          <CardTitle className="flex items-center gap-2 truncate">
                            <CalendarDays className="h-4 w-4 shrink-0 text-muted-foreground" />
                            <span className="truncate">{event.name}</span>
                          </CardTitle>
                          {start && (
                            <p className="text-xs text-muted-foreground">
                              {start.date} {start.time}
                            </p>
                          )}
                        </CardHeader>
                        <CardContent className="space-y-2">
                          <div className="flex flex-wrap gap-2">
                            {capacity > 0 && (
                              <Badge variant="secondary">
                                {t("management.events.occupancy", {
                                  reserved,
                                  capacity,
                                })}
                              </Badge>
                            )}
                            {event.supervisorIds &&
                              event.supervisorIds.length > 0 && (
                                <Badge
                                  variant="secondary"
                                  className="flex items-center gap-1"
                                >
                                  <Users className="h-3 w-3" />
                                  {t("management.events.supervisorsCount", {
                                    count: event.supervisorIds.length,
                                  })}
                                </Badge>
                              )}
                            {event.managerIds &&
                              event.managerIds.length > 0 && (
                                <Badge
                                  variant="secondary"
                                  className="flex items-center gap-1"
                                >
                                  <Users className="h-3 w-3" />
                                  {t("management.events.managersCount", {
                                    count: event.managerIds.length,
                                  })}
                                </Badge>
                              )}
                          </div>
                          {bookingStart && bookingDeadline && (
                            <p className="text-xs text-muted-foreground">
                              {t("management.events.bookingWindow", {
                                start: `${bookingStart.date} ${bookingStart.time}`,
                                deadline: `${bookingDeadline.date} ${bookingDeadline.time}`,
                              })}
                            </p>
                          )}
                          <div className="flex flex-wrap gap-2 pt-1">
                            <Button variant="outline" size="sm" asChild>
                              <Link
                                href={`/management/reservations?eventId=${event.id}`}
                              >
                                <BookmarkCheck className="h-3.5 w-3.5" />
                                {t("management.events.viewReservations")}
                              </Link>
                            </Button>
                            <Button variant="outline" size="sm" asChild>
                              <Link
                                href={`/management/allowances?eventId=${event.id}`}
                              >
                                <Ticket className="h-3.5 w-3.5" />
                                {t("management.events.viewAllowances")}
                              </Link>
                            </Button>
                            <Button
                              variant="outline"
                              size="sm"
                              onClick={() => handleEdit(event)}
                            >
                              <Edit className="h-3.5 w-3.5" />
                            </Button>
                            <Button
                              variant="destructive"
                              size="sm"
                              onClick={() => handleDelete(event)}
                              isLoading={deletingEventId === event.id}
                            >
                              <Trash2 className="h-3.5 w-3.5" />
                            </Button>
                          </div>
                        </CardContent>
                      </Card>
                    );
                  })}
                </div>
              </div>
            );
          })}
        </div>
      )}

      {isModalOpen && (
        <EventFormModal
          allLocations={locations}
          event={formModalEvent}
          isCreating={isCreating}
          users={users}
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
    </div>
  );
}
