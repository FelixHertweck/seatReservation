"use client";

import { useState } from "react";
import { Plus, Edit, Trash2, Ban } from "lucide-react";
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
import { ReservationFormModal } from "@/components/manager/reservation-form-modal";
import { BlockSeatsModal } from "@/components/manager/block-seats-modal";
import type {
  DetailedReservationResponseDto,
  ReservationRequestDto,
  BlockSeatsRequestDto,
  UserDto,
  DetailedEventResponseDto,
  SeatDto,
} from "@/api";

export interface ReservationManagementProps {
  users: UserDto[];
  events: DetailedEventResponseDto[];
  seats: SeatDto[];
  reservations: DetailedReservationResponseDto[];
  createReservation: (
    reservation: ReservationRequestDto,
  ) => Promise<DetailedReservationResponseDto>;
  updateReservation: (
    id: bigint,
    reservation: ReservationRequestDto,
  ) => Promise<DetailedReservationResponseDto>;
  deleteReservation: (id: bigint) => Promise<unknown>;
  blockSeats: (request: BlockSeatsRequestDto) => Promise<void>;
}

export function ReservationManagement({
  users,
  events,
  seats,
  reservations,
  createReservation,
  updateReservation,
  deleteReservation,
  blockSeats,
}: ReservationManagementProps) {
  const [filteredReservations, setFilteredReservations] =
    useState(reservations);
  const [selectedReservation, setSelectedReservation] =
    useState<DetailedReservationResponseDto | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isBlockModalOpen, setIsBlockModalOpen] = useState(false);
  const [isCreating, setIsCreating] = useState(false);

  const handleSearch = (query: string) => {
    const filtered = reservations.filter(
      (reservation) =>
        reservation.user?.username
          ?.toLowerCase()
          .includes(query.toLowerCase()) ||
        reservation.event?.name?.toLowerCase().includes(query.toLowerCase()) ||
        reservation.seat?.seatNumber
          ?.toLowerCase()
          .includes(query.toLowerCase()),
    );
    setFilteredReservations(filtered);
  };

  const handleFilter = (filters: Record<string, unknown>) => {
    let filtered = reservations;

    if (filters.eventId) {
      filtered = filtered.filter(
        (r) => r.event?.id?.toString() === filters.eventId,
      );
    }

    setFilteredReservations(filtered);
  };

  const handleCreateReservation = () => {
    setSelectedReservation(null);
    setIsCreating(true);
    setIsModalOpen(true);
  };

  const handleEditReservation = (
    reservation: DetailedReservationResponseDto,
  ) => {
    setSelectedReservation(reservation);
    setIsCreating(false);
    setIsModalOpen(true);
  };

  const handleDeleteReservation = async (
    reservation: DetailedReservationResponseDto,
  ) => {
    if (
      reservation.id &&
      confirm(`Are you sure you want to delete this reservation?`)
    ) {
      await deleteReservation(reservation.id);
    }
  };

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between">
          <div>
            <CardTitle>Reservation Management</CardTitle>
            <CardDescription>
              Manage event reservations and block seats
            </CardDescription>
          </div>
          <div className="flex gap-2">
            <Button variant="outline" onClick={() => setIsBlockModalOpen(true)}>
              <Ban className="mr-2 h-4 w-4" />
              Block Seats
            </Button>
            <Button onClick={handleCreateReservation}>
              <Plus className="mr-2 h-4 w-4" />
              Add Reservation
            </Button>
          </div>
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
              <TableHead>User</TableHead>
              <TableHead>Event</TableHead>
              <TableHead>Seat</TableHead>
              <TableHead>Reserved Date</TableHead>
              <TableHead>Actions</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {filteredReservations.map((reservation) => (
              <TableRow key={reservation.id?.toString()}>
                <TableCell>{reservation.user?.username}</TableCell>
                <TableCell>{reservation.event?.name}</TableCell>
                <TableCell>
                  <Badge variant="outline">
                    {reservation.seat?.seatNumber}
                  </Badge>
                </TableCell>
                <TableCell>
                  {reservation.reservationDateTime
                    ? new Date(
                        reservation.reservationDateTime,
                      ).toLocaleDateString()
                    : "Unknown"}
                </TableCell>
                <TableCell>
                  <div className="flex gap-2">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => handleEditReservation(reservation)}
                    >
                      <Edit className="h-4 w-4" />
                    </Button>
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
            ))}
          </TableBody>
        </Table>
      </CardContent>

      {isModalOpen && (
        <ReservationFormModal
          users={users}
          events={events}
          seats={seats}
          reservation={selectedReservation}
          isCreating={isCreating}
          onSubmit={async (reservationData) => {
            if (isCreating) {
              await createReservation(reservationData);
            } else if (selectedReservation?.id) {
              await updateReservation(selectedReservation.id, reservationData);
            }
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
