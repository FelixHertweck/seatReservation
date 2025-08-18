"use client";

import { useState, useEffect } from "react";
import { Plus, Trash2, Ban, ExternalLink } from "lucide-react";
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
  ) => Promise<DetailedReservationResponseDto[]>;
  deleteReservation: (id: bigint) => Promise<unknown>;
  blockSeats: (
    request: BlockSeatsRequestDto,
  ) => Promise<DetailedReservationResponseDto[]>;
  onNavigateToEvent?: (eventId: bigint) => void;
  onNavigateToSeat?: (seatId: bigint) => void;
  initialFilter?: Record<string, string>;
}

export function ReservationManagement({
  users,
  events,
  seats,
  reservations,
  createReservation,
  deleteReservation,
  blockSeats,
  onNavigateToEvent,
  onNavigateToSeat,
  initialFilter = {},
}: ReservationManagementProps) {
  const [filteredReservations, setFilteredReservations] =
    useState(reservations);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isBlockModalOpen, setIsBlockModalOpen] = useState(false);
  const [currentFilters, setCurrentFilters] =
    useState<Record<string, string>>(initialFilter);

  useEffect(() => {
    setCurrentFilters(initialFilter);
  }, [initialFilter]);

  useEffect(() => {
    applyFilters("", currentFilters);
  }, [reservations, currentFilters]);

  const applyFilters = (
    searchQuery: string,
    filters: Record<string, string>,
  ) => {
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

    setFilteredReservations(filtered);
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

  const handleCreateReservation = () => {
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
            {
              key: "eventId",
              label: "Event",
              type: "select",
              options: events.map((event) => ({
                value: event.id?.toString() || "",
                label: event.name || "",
              })),
            },
          ]}
          initialFilters={currentFilters}
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
            {filteredReservations.map((reservation) => {
              const event = events.find(
                (event) => event.id === reservation.eventId,
              );

              return (
                <TableRow key={reservation.id?.toString()}>
                  <TableCell>{reservation.user?.username}</TableCell>
                  <TableCell>
                    {event ? (
                      <Button
                        variant="link"
                        className="p-0 h-auto font-normal text-blue-600 hover:text-blue-800"
                        onClick={() => event.id && handleEventClick(event.id)}
                      >
                        {event.name}
                        <ExternalLink className="ml-1 h-3 w-3" />
                      </Button>
                    ) : (
                      "Unknown event"
                    )}
                  </TableCell>
                  <TableCell>
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
                      <Badge variant="outline">Unknown seat</Badge>
                    )}
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
                        variant="destructive"
                        size="sm"
                        onClick={() => handleDeleteReservation(reservation)}
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
        <ReservationFormModal
          users={users}
          events={events}
          seats={seats}
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
