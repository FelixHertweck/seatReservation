"use client";

import { useState } from "react";
import { Plus, Trash2 } from "lucide-react";
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
import { AllowanceFormModal } from "@/components/manager/allowance-form-modal";
import type {
  DetailedEventResponseDto,
  EventUserAllowancesDto,
  UserDto,
} from "@/api";

export interface ReservationAllowanceManagementProps {
  events: DetailedEventResponseDto[];
  users: UserDto[];
  reservationAllowance: EventUserAllowancesDto[];
  createReservationAllowance: (
    allowance: EventUserAllowancesDto,
  ) => Promise<EventUserAllowancesDto>;
  deleteReservationAllowance: (id: bigint) => Promise<unknown>;
}

export function ReservationAllowanceManagement({
  events,
  users,
  reservationAllowance,
  createReservationAllowance,
  deleteReservationAllowance,
}: ReservationAllowanceManagementProps) {
  const [filteredAllowances, setFilteredAllowances] =
    useState(reservationAllowance);
  const [isModalOpen, setIsModalOpen] = useState(false);

  const handleSearch = (query: string) => {
    const filtered = reservationAllowance.filter(
      (allowance) =>
        allowance.eventId?.toString().includes(query) ||
        allowance.userId?.toString().includes(query),
    );
    setFilteredAllowances(filtered);
  };

  const handleFilter = (filters: Record<string, unknown>) => {
    let filtered = reservationAllowance;

    if (filters.eventId) {
      filtered = filtered.filter(
        (a) => a.eventId?.toString() === filters.eventId,
      );
    }

    setFilteredAllowances(filtered);
  };

  const handleDeleteAllowance = async (allowance: EventUserAllowancesDto) => {
    if (
      allowance.eventId &&
      confirm(`Are you sure you want to delete this allowance?`)
    ) {
      await deleteReservationAllowance(allowance.eventId);
    }
  };

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between">
          <div>
            <CardTitle>Reservation Allowance Management</CardTitle>
            <CardDescription>
              Manage user reservation limits for events
            </CardDescription>
          </div>
          <Button onClick={() => setIsModalOpen(true)}>
            <Plus className="mr-2 h-4 w-4" />
            Add Allowance
          </Button>
        </div>
      </CardHeader>

      <CardContent>
        <SearchAndFilter
          onSearch={handleSearch}
          onFilter={handleFilter}
          filterOptions={[
            { key: "eventId", label: "Filter by Event", type: "string" },
          ]}
        />

        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Event ID</TableHead>
              <TableHead>User ID</TableHead>
              <TableHead>Allowed Reservations</TableHead>
              <TableHead>Actions</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {filteredAllowances.map((allowance, index) => (
              <TableRow
                key={`${allowance.eventId}-${allowance.userId}-${index}`}
              >
                <TableCell>{allowance.eventId?.toString()}</TableCell>
                <TableCell>{allowance.userId?.toString()}</TableCell>
                <TableCell>{allowance.reservationsAllowedCount}</TableCell>
                <TableCell>
                  <Button
                    variant="destructive"
                    size="sm"
                    onClick={() => handleDeleteAllowance(allowance)}
                  >
                    <Trash2 className="h-4 w-4" />
                  </Button>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </CardContent>

      {isModalOpen && (
        <AllowanceFormModal
          events={events}
          users={users}
          onSubmit={async (allowanceData) => {
            await createReservationAllowance(allowanceData);
            setIsModalOpen(false);
          }}
          onClose={() => setIsModalOpen(false)}
        />
      )}
    </Card>
  );
}
