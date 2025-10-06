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
import { AllowanceFormModal } from "@/components/management/allowance-form-modal";
import { PaginationWrapper } from "@/components/common/pagination-wrapper";
import type {
  EventUserAllowancesDto,
  EventUserAllowancesCreateDto,
  EventUserAllowanceUpdateDto,
  EventResponseDto,
  LimitedUserInfoDto,
} from "@/api";
import { useT } from "@/lib/i18n/hooks";

export interface ReservationAllowanceManagementProps {
  allowances: EventUserAllowancesDto[];
  events: EventResponseDto[];
  users: LimitedUserInfoDto[];
  createReservationAllowance: (
    allowance: EventUserAllowancesCreateDto,
  ) => Promise<EventUserAllowancesDto[]>;
  updateReservationAllowance: (
    allowance: EventUserAllowanceUpdateDto,
  ) => Promise<EventUserAllowancesDto>;
  deleteReservationAllowance: (ids: bigint[]) => Promise<void>;
  onNavigateToEvent?: (eventId: bigint) => void;
  initialFilter?: Record<string, string>;
  isLoading?: boolean;
}

export function ReservationAllowanceManagement({
  allowances,
  events,
  users,
  createReservationAllowance,
  updateReservationAllowance,
  deleteReservationAllowance,
  onNavigateToEvent,
  initialFilter = {},
  isLoading = false,
}: ReservationAllowanceManagementProps) {
  const t = useT();

  const [filteredAllowances, setFilteredAllowances] = useState(allowances);
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [selectedAllowance, setSelectedAllowance] =
    useState<EventUserAllowancesDto | null>(null);
  const [currentFilters, setCurrentFilters] =
    useState<Record<string, string>>(initialFilter);
  const [selectedIds, setSelectedIds] = useState<Set<bigint>>(new Set());

  useEffect(() => {
    setCurrentFilters(initialFilter);
  }, [initialFilter]);

  const applyFilters = useCallback(
    (searchQuery: string, filters: Record<string, string>) => {
      let filtered = allowances;

      // Apply search
      if (searchQuery) {
        filtered = filtered.filter((allowance) => {
          const event = events.find((e) => e.id === allowance.eventId);
          const user = users.find((u) => u.id === allowance.userId);
          return (
            event?.name?.toLowerCase().includes(searchQuery.toLowerCase()) ||
            user?.username?.toLowerCase().includes(searchQuery.toLowerCase())
          );
        });
      }

      // Apply filters
      if (filters.eventId) {
        filtered = filtered.filter(
          (allowance) => allowance.eventId?.toString() === filters.eventId,
        );
      }

      setFilteredAllowances(filtered);
    },
    [allowances, events, users],
  );

  useEffect(() => {
    applyFilters("", currentFilters);
  }, [allowances, currentFilters, applyFilters]);

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

  const handleCreateAllowance = async (
    allowanceData: EventUserAllowancesCreateDto | EventUserAllowanceUpdateDto,
  ) => {
    if ("userIds" in allowanceData) {
      await createReservationAllowance(allowanceData);
    }
    setIsCreateModalOpen(false);
  };

  const handleUpdateAllowance = async (
    allowanceData: EventUserAllowancesCreateDto | EventUserAllowanceUpdateDto,
  ) => {
    if ("id" in allowanceData) {
      await updateReservationAllowance(allowanceData);
    }
    setIsEditModalOpen(false);
    setSelectedAllowance(null);
  };

  const openCreateModal = () => {
    setSelectedAllowance(null);
    setIsCreateModalOpen(true);
  };

  const openEditModal = (allowance: EventUserAllowancesDto) => {
    setSelectedAllowance(allowance);
    setIsEditModalOpen(true);
    setSelectedIds(new Set());
  };

  const handleDeleteAllowance = async (allowance: EventUserAllowancesDto) => {
    if (
      allowance.id &&
      confirm(t("reservationAllowanceManagement.confirmDelete"))
    ) {
      await deleteReservationAllowance([allowance.id]);
      setSelectedIds(new Set());
    }
  };

  const handleEventClick = (eventId: bigint) => {
    if (onNavigateToEvent) {
      onNavigateToEvent(eventId);
    }
  };

  const handleSelectAll = (paginatedData: EventUserAllowancesDto[]) => {
    const newSelectedIds = new Set(selectedIds);
    const allCurrentSelected = paginatedData.every((allowance) =>
      allowance.id ? selectedIds.has(allowance.id) : false,
    );

    if (allCurrentSelected) {
      setSelectedIds(new Set());
    } else {
      paginatedData.forEach((allowance) => {
        if (allowance.id) newSelectedIds.add(allowance.id);
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
        t("reservationAllowanceManagement.confirmDeleteMultiple", {
          count: selectedIds.size,
        }),
      )
    ) {
      await deleteReservationAllowance(Array.from(selectedIds));
      setSelectedIds(new Set());
    }
  };

  return (
    <Card className="w-full">
      <CardHeader>
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="min-w-0 flex-1">
            <CardTitle className="text-xl sm:text-2xl">
              {t("reservationAllowanceManagement.title")}
            </CardTitle>
            <CardDescription className="text-sm">
              {t("reservationAllowanceManagement.description")}
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
              onClick={openCreateModal}
              className="w-full sm:w-auto shrink-0"
            >
              <Plus className="mr-2 h-4 w-4" />
              {t("reservationAllowanceManagement.addAllowanceButton")}
            </Button>
          </div>
        </div>
      </CardHeader>

      <CardContent>
        <div>
          <SearchAndFilter
            onSearch={handleSearch}
            onFilter={handleFilter}
            filterOptions={[
              {
                key: "eventId",
                label: t("reservationAllowanceManagement.eventFilterLabel"),
                type: "select",
                options: events.map((event) => ({
                  value: event.id?.toString() || "",
                  label: event.name || "",
                })),
              },
            ]}
            initialFilters={currentFilters}
          />
        </div>

        <PaginationWrapper
          data={filteredAllowances}
          itemsPerPage={100}
          paginationLabel={t("reservationAllowanceManagement.paginationLabel")}
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
                    {paginatedData.every((allowance) =>
                      allowance.id ? selectedIds.has(allowance.id) : false,
                    )
                      ? t("reservationAllowanceManagement.deselectAll")
                      : t("reservationAllowanceManagement.selectAll")}
                  </Button>
                </div>
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead className="w-12">
                        {t("reservationAllowanceManagement.tableHeaderSelect")}
                      </TableHead>
                      <TableHead>
                        {t("reservationAllowanceManagement.tableHeaderEvent")}
                      </TableHead>
                      <TableHead>
                        {t("reservationAllowanceManagement.tableHeaderUser")}
                      </TableHead>
                      <TableHead>
                        {t(
                          "reservationAllowanceManagement.tableHeaderAllowedReservations",
                        )}
                      </TableHead>
                      <TableHead>
                        {t("reservationAllowanceManagement.tableHeaderActions")}
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
                              <Skeleton className="h-4 w-24" />
                            </TableCell>
                            <TableCell>
                              <Skeleton className="h-4 w-12" />
                            </TableCell>
                            <TableCell>
                              <div className="flex gap-2">
                                <Skeleton className="h-8 w-8" />
                                <Skeleton className="h-8 w-8" />
                              </div>
                            </TableCell>
                          </TableRow>
                        ))
                      : paginatedData.map((allowance) => {
                          const event = events.find(
                            (e) => e.id === allowance.eventId,
                          );
                          const user = users.find(
                            (u) => u.id === allowance.userId,
                          );

                          return (
                            <TableRow
                              key={`${allowance.id?.toString()}${allowance.eventId?.toString()}`}
                            >
                              <TableCell>
                                <Checkbox
                                  checked={
                                    allowance.id
                                      ? selectedIds.has(allowance.id)
                                      : false
                                  }
                                  onCheckedChange={() =>
                                    allowance.id &&
                                    handleToggleSelect(allowance.id)
                                  }
                                />
                              </TableCell>
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
                                  t(
                                    "reservationAllowanceManagement.unknownEvent",
                                  )
                                )}
                              </TableCell>
                              <TableCell>
                                {user?.username ||
                                  t(
                                    "reservationAllowanceManagement.unknownUser",
                                  )}
                              </TableCell>
                              <TableCell>
                                {allowance.reservationsAllowedCount}
                              </TableCell>
                              <TableCell>
                                <div className="flex gap-2">
                                  <Button
                                    variant="outline"
                                    size="sm"
                                    onClick={() => openEditModal(allowance)}
                                  >
                                    <Edit className="h-4 w-4" />
                                  </Button>
                                  <Button
                                    variant="destructive"
                                    size="sm"
                                    onClick={() =>
                                      handleDeleteAllowance(allowance)
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
                          <Skeleton className="h-4 w-1/2 mt-2" />
                        </CardHeader>
                        <CardContent>
                          <Skeleton className="h-4 w-full" />
                        </CardContent>
                      </Card>
                    ))
                  : paginatedData.map((allowance) => {
                      const event = events.find(
                        (e) => e.id === allowance.eventId,
                      );
                      const user = users.find((u) => u.id === allowance.userId);

                      return (
                        <Card
                          key={`${allowance.id?.toString()}${allowance.eventId?.toString()}`}
                          className="w-full"
                        >
                          <CardHeader className="pb-3 flex flex-row items-start space-x-3 space-y-0">
                            <Checkbox
                              checked={
                                allowance.id
                                  ? selectedIds.has(allowance.id)
                                  : false
                              }
                              onCheckedChange={() =>
                                allowance.id && handleToggleSelect(allowance.id)
                              }
                              className="mt-1"
                            />
                            <div className="flex-1 min-w-0">
                              <CardTitle className="text-base break-words">
                                {user?.username ||
                                  t(
                                    "reservationAllowanceManagement.unknownUser",
                                  )}
                              </CardTitle>
                              <CardDescription className="text-sm mt-1 break-words">
                                {allowance.reservationsAllowedCount}{" "}
                                {t(
                                  "reservationAllowanceManagement.tableHeaderAllowedReservations",
                                )}
                              </CardDescription>
                            </div>
                          </CardHeader>
                          <CardContent className="space-y-3">
                            {event && (
                              <div className="min-w-0">
                                <p className="text-xs text-muted-foreground mb-1">
                                  {t(
                                    "reservationAllowanceManagement.tableHeaderEvent",
                                  )}
                                </p>
                                <Button
                                  variant="link"
                                  className="p-0 h-auto font-normal text-blue-600 hover:text-blue-800 text-sm break-words text-left max-w-full"
                                  onClick={() =>
                                    event.id && handleEventClick(event.id)
                                  }
                                >
                                  <span className="break-words max-w-full">
                                    {event.name}
                                  </span>
                                  <ExternalLink className="ml-1 h-3 w-3 shrink-0" />
                                </Button>
                              </div>
                            )}

                            <div className="flex gap-2 pt-2">
                              <Button
                                variant="outline"
                                size="sm"
                                className="flex-1 bg-transparent min-w-0"
                                onClick={() => openEditModal(allowance)}
                              >
                                <Edit className="mr-2 h-4 w-4 shrink-0" />
                                <span className="truncate">
                                  {t(
                                    "reservationAllowanceManagement.editButtonLabel",
                                  )}
                                </span>
                              </Button>
                              <Button
                                variant="destructive"
                                size="sm"
                                className="flex-1 min-w-0"
                                onClick={() => handleDeleteAllowance(allowance)}
                              >
                                <Trash2 className="mr-2 h-4 w-4 shrink-0" />
                                <span className="truncate">
                                  {t(
                                    "reservationAllowanceManagement.deleteButtonLabel",
                                  )}
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

      {isCreateModalOpen && (
        <AllowanceFormModal
          events={events}
          users={users}
          allowance={null}
          isCreating={true}
          onSubmit={handleCreateAllowance}
          onClose={() => setIsCreateModalOpen(false)}
        />
      )}

      {isEditModalOpen && selectedAllowance && (
        <AllowanceFormModal
          events={events}
          users={users}
          allowance={selectedAllowance}
          isCreating={false}
          onSubmit={handleUpdateAllowance}
          onClose={() => setIsEditModalOpen(false)}
        />
      )}
    </Card>
  );
}
