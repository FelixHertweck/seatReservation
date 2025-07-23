"use client";

import { useState } from "react";
import { Plus, Edit, Trash2 } from "lucide-react";
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
import { SeatFormModal } from "@/components/manager/seat-form-modal";
import type {
  SeatResponseDto,
  SeatRequestDto,
  EventLocationResponseDto,
} from "@/api";

export interface SeatManagementProps {
  seats: SeatResponseDto[];
  locations: EventLocationResponseDto[];
  createSeat: (seat: SeatRequestDto) => Promise<SeatResponseDto>;
  updateSeat: (id: bigint, seat: SeatRequestDto) => Promise<SeatResponseDto>;
  deleteSeat: (id: bigint) => Promise<unknown>;
}

export function SeatManagement({
  seats,
  locations,
  createSeat,
  updateSeat,
  deleteSeat,
}: SeatManagementProps) {
  const [filteredSeats, setFilteredSeats] = useState(seats);
  const [selectedSeat, setSelectedSeat] = useState<SeatResponseDto | null>(
    null,
  );
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isCreating, setIsCreating] = useState(false);

  const handleSearch = (query: string) => {
    const filtered = seats.filter(
      (seat) =>
        seat.seatNumber?.toLowerCase().includes(query.toLowerCase()) ||
        seat.location?.name?.toLowerCase().includes(query.toLowerCase()),
    );
    setFilteredSeats(filtered);
  };

  const handleCreateSeat = () => {
    setSelectedSeat(null);
    setIsCreating(true);
    setIsModalOpen(true);
  };

  const handleEditSeat = (seat: SeatResponseDto) => {
    setSelectedSeat(seat);
    setIsCreating(false);
    setIsModalOpen(true);
  };

  const handleDeleteSeat = async (seat: SeatResponseDto) => {
    if (
      seat.id &&
      confirm(`Are you sure you want to delete seat ${seat.seatNumber}?`)
    ) {
      await deleteSeat(seat.id);
    }
  };

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between">
          <div>
            <CardTitle>Seat Management</CardTitle>
            <CardDescription>
              Create and manage seats for locations
            </CardDescription>
          </div>
          <Button onClick={handleCreateSeat}>
            <Plus className="mr-2 h-4 w-4" />
            Add Seat
          </Button>
        </div>
      </CardHeader>

      <CardContent>
        <SearchAndFilter
          onSearch={handleSearch}
          onFilter={() => {}}
          filterOptions={[]}
        />

        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Seat Number</TableHead>
              <TableHead>Location</TableHead>
              <TableHead>Position</TableHead>
              <TableHead>Status</TableHead>
              <TableHead>Actions</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {filteredSeats.map((seat) => (
              <TableRow key={seat.id?.toString()}>
                <TableCell className="font-medium">{seat.seatNumber}</TableCell>
                <TableCell>{seat.location?.name}</TableCell>
                <TableCell>
                  ({seat.xCoordinate}, {seat.yCoordinate})
                </TableCell>
                <TableCell>
                  <Badge variant={seat.status ? "destructive" : "default"}>
                    {seat.status || "Available"}
                  </Badge>
                </TableCell>
                <TableCell>
                  <div className="flex gap-2">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => handleEditSeat(seat)}
                    >
                      <Edit className="h-4 w-4" />
                    </Button>
                    <Button
                      variant="destructive"
                      size="sm"
                      onClick={() => handleDeleteSeat(seat)}
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
        <SeatFormModal
          locations={locations}
          seat={selectedSeat}
          isCreating={isCreating}
          onSubmit={async (seatData) => {
            if (isCreating) {
              await createSeat(seatData);
            } else if (selectedSeat?.id) {
              await updateSeat(selectedSeat.id, seatData);
            }
            setIsModalOpen(false);
          }}
          onClose={() => setIsModalOpen(false)}
        />
      )}
    </Card>
  );
}
