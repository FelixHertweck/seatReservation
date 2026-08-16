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
  Ban,
  AlertTriangle,
} from "lucide-react";

import { useT } from "@/lib/i18n/hooks";
import { formatDateTime } from "@/lib/utils";
import { PageHeader } from "@/components/page-header";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/custom-ui/button";
import { Badge } from "@/components/ui/badge";
import { Textarea } from "@/components/ui/textarea";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import { Skeleton } from "@/components/custom-ui/skeleton";
import { SearchAndFilter } from "@/components/common/search-and-filter";
import { EventFormModal } from "@/components/management/event-form-modal";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/custom-ui/alert-dialog";
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
    cancelEvent,
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
  const [deleteEventTarget, setDeleteEventTarget] =
    useState<EventResponseDto | null>(null);
  const [cancellingEvent, setCancellingEvent] =
    useState<EventResponseDto | null>(null);
  const [cancelReason, setCancelReason] = useState("");
  const [isCancelling, setIsCancelling] = useState(false);

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
    let result = [...events];
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase();
      result = result.filter(
        (e) =>
          e.name?.toLowerCase().includes(q) ||
          e.description?.toLowerCase().includes(q) ||
          locationById.get(e.eventLocationId)?.name?.toLowerCase().includes(q),
      );
    }
    if (filters.locationId) {
      result = result.filter((e) => e.eventLocationId === filters.locationId);
    }
    return result.sort(
      (a, b) => (a.startTime?.getTime() ?? 0) - (b.startTime?.getTime() ?? 0),
    );
  }, [events, searchQuery, filters, locationById]);

  // Group events by location
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

  const handleDelete = (event: EventResponseDto) => {
    if (!event.id) return;
    setDeleteEventTarget(event);
  };

  const confirmDelete = async () => {
    if (!deleteEventTarget?.id) return;
    setDeletingEventId(deleteEventTarget.id);
    try {
      await deleteEvent([deleteEventTarget.id]);
      setDeleteEventTarget(null);
    } finally {
      setDeletingEventId(null);
    }
  };

  const handleCancelClick = (event: EventResponseDto) => {
    setCancellingEvent(event);
    setCancelReason("");
  };

  const confirmCancel = async () => {
    if (!cancellingEvent?.id || !cancelReason.trim()) return;
    setIsCancelling(true);
    try {
      await cancelEvent(cancellingEvent.id, cancelReason.trim());
      setCancellingEvent(null);
      setCancelReason("");
    } finally {
      setIsCancelling(false);
    }
  };

  let formModalEvent: EventResponseDto | null = null;
  if (selectedEvent) {
    formModalEvent = selectedEvent;
  } else if (lockedLocationId) {
    formModalEvent = { eventLocationId: lockedLocationId } as EventResponseDto;
  }

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
                    const capacity = location?.seatCount ?? 0;
                    const reserved =
                      event.seatStatuses?.filter((s) => s.status === "RESERVED")
                        .length ?? 0;
                    const start = formatDateTime(event.startTime);
                    const bookingStart = formatDateTime(event.bookingStartTime);
                    const bookingDeadline = formatDateTime(
                      event.bookingDeadline,
                    );
                    const isCancelled = event.status === "CANCELLED";

                    return (
                      <Card
                        key={event.id}
                        className="flex h-full min-h-[26rem] flex-col"
                      >
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
                        <CardContent className="flex flex-1 flex-col space-y-2">
                          <div className="flex flex-wrap gap-2">
                            {isCancelled && (
                              <Badge
                                variant="destructive"
                                className="flex items-center gap-1"
                              >
                                <Ban className="h-3 w-3" />
                                {t("management.events.statusCancelled")}
                              </Badge>
                            )}
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
                          <div className="mt-auto flex flex-wrap gap-2 pt-1">
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
                            {isCancelled ? (
                              <TooltipProvider>
                                <Tooltip>
                                  <TooltipTrigger asChild>
                                    <span className="inline-block">
                                      <Button
                                        variant="outline"
                                        size="sm"
                                        disabled
                                        aria-label={t(
                                          "management.events.editDisabledCancelled",
                                        )}
                                      >
                                        <Edit className="h-3.5 w-3.5" />
                                      </Button>
                                    </span>
                                  </TooltipTrigger>
                                  <TooltipContent>
                                    <p>
                                      {t(
                                        "management.events.editDisabledCancelled",
                                      )}
                                    </p>
                                  </TooltipContent>
                                </Tooltip>
                              </TooltipProvider>
                            ) : (
                              <Button
                                variant="outline"
                                size="sm"
                                onClick={() => handleEdit(event)}
                                aria-label={t("management.events.editButton")}
                              >
                                <Edit className="h-3.5 w-3.5" />
                              </Button>
                            )}
                            {!isCancelled && (
                              <Button
                                variant="outline"
                                size="sm"
                                className="text-destructive hover:bg-destructive/10 hover:text-destructive"
                                onClick={() => handleCancelClick(event)}
                                aria-label={t("management.events.cancelEvent")}
                              >
                                <Ban className="h-3.5 w-3.5" />
                              </Button>
                            )}
                            <Button
                              variant="destructive"
                              size="sm"
                              onClick={() => handleDelete(event)}
                              isLoading={deletingEventId === event.id}
                              aria-label={t("common.delete")}
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

      <AlertDialog
        open={!!cancellingEvent}
        onOpenChange={(open) => {
          if (!open) {
            setCancellingEvent(null);
            setCancelReason("");
          }
        }}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle className="flex items-center gap-2">
              <Ban className="h-5 w-5 text-destructive" />
              {t("management.events.cancelConfirmTitle", {
                name: cancellingEvent?.name ?? "",
              })}
            </AlertDialogTitle>
            <AlertDialogDescription>
              {t("management.events.cancelConfirmDescription")}
            </AlertDialogDescription>
          </AlertDialogHeader>

          {(cancellingEvent?.reservedCount ?? 0) > 0 && (
            <div className="mt-4 flex items-start gap-2 rounded-md border border-amber-500/20 bg-amber-500/10 p-3 text-sm text-amber-600 dark:text-amber-400">
              <AlertTriangle className="h-4 w-4 shrink-0 mt-0.5" />
              <span>
                {t("management.events.cancelAffectedReservationsWarning", {
                  count: cancellingEvent?.reservedCount ?? 0,
                })}
              </span>
            </div>
          )}

          <div className="mt-4 pb-6">
            <label
              htmlFor="cancel-reason"
              className="mb-3 block text-sm font-medium"
            >
              {t("management.events.cancelReasonLabel")}{" "}
              <span className="text-destructive">*</span>
            </label>
            <Textarea
              id="cancel-reason"
              placeholder={t("management.events.cancelReasonPlaceholder")}
              value={cancelReason}
              onChange={(e) => setCancelReason(e.target.value)}
              maxLength={1000}
              rows={4}
              className="resize-none"
            />
            <div className="mt-1.5 flex justify-end text-xs text-muted-foreground">
              {cancelReason.length}/1000
            </div>
          </div>

          <AlertDialogFooter>
            <AlertDialogCancel disabled={isCancelling}>
              {t("common.cancel")}
            </AlertDialogCancel>
            <Button
              variant="destructive"
              disabled={isCancelling || !cancelReason.trim()}
              onClick={confirmCancel}
              isLoading={isCancelling}
            >
              {t("management.events.confirmCancel")}
            </Button>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      <AlertDialog
        open={!!deleteEventTarget}
        onOpenChange={(open) => {
          if (!open) setDeleteEventTarget(null);
        }}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>
              {(deleteEventTarget?.reservedCount ?? 0) > 0
                ? t("management.events.deleteBlockedTitle")
                : t("management.events.deleteConfirmTitle")}
            </AlertDialogTitle>
            <AlertDialogDescription>
              {(deleteEventTarget?.reservedCount ?? 0) > 0
                ? t("management.events.deleteBlockedDescription", {
                    count: deleteEventTarget?.reservedCount ?? 0,
                  })
                : t("management.events.deleteConfirmDescription", {
                    name: deleteEventTarget?.name ?? "",
                  })}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            {(deleteEventTarget?.reservedCount ?? 0) > 0 ? (
              <AlertDialogAction onClick={() => setDeleteEventTarget(null)}>
                {t("common.close")}
              </AlertDialogAction>
            ) : (
              <>
                <AlertDialogCancel>{t("common.cancel")}</AlertDialogCancel>
                <AlertDialogAction
                  onClick={confirmDelete}
                  className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
                >
                  {t("common.delete")}
                </AlertDialogAction>
              </>
            )}
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
