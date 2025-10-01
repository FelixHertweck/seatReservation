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
  deleteReservationAllowance: (id: bigint) => Promise<void>;
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
  };

  const handleDeleteAllowance = async (allowance: EventUserAllowancesDto) => {
    if (
      allowance.id &&
      confirm(t("reservationAllowanceManagement.confirmDelete"))
    ) {
      await deleteReservationAllowance(allowance.id);
    }
  };

  const handleEventClick = (eventId: bigint) => {
    if (onNavigateToEvent) {
      onNavigateToEvent(eventId);
    }
  };

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between">
          <div>
            <CardTitle>{t("reservationAllowanceManagement.title")}</CardTitle>
            <CardDescription>
              {t("reservationAllowanceManagement.description")}
            </CardDescription>
          </div>
          <Button onClick={openCreateModal}>
            <Plus className="mr-2 h-4 w-4" />
            {t("reservationAllowanceManagement.addAllowanceButton")}
          </Button>
        </div>
      </CardHeader>

      <CardContent>
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

        <PaginationWrapper
          data={filteredAllowances}
          itemsPerPage={100}
          paginationLabel={t("reservationAllowanceManagement.paginationLabel")}
        >
          {(paginatedData) => (
            <Table>
              <TableHeader>
                <TableRow>
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
                      const user = users.find((u) => u.id === allowance.userId);

                      return (
                        <TableRow
                          key={`${allowance.id?.toString()}${allowance.eventId?.toString()}`}
                        >
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
                              t("reservationAllowanceManagement.unknownEvent")
                            )}
                          </TableCell>
                          <TableCell>
                            {user?.username ||
                              t("reservationAllowanceManagement.unknownUser")}
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
                                onClick={() => handleDeleteAllowance(allowance)}
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
