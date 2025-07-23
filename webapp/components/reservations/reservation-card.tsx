"use client";

import { Calendar, MapPin, Trash2, Eye } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import type { ReservationResponseDto } from "@/api";

interface ReservationCardProps {
  reservation: ReservationResponseDto;
  onViewSeats: () => void;
  onDelete: () => void;
}

export function ReservationCard({
  reservation,
  onViewSeats,
  onDelete,
}: ReservationCardProps) {
  return (
    <Card>
      <CardHeader>
        <div className="flex items-start justify-between">
          <CardTitle className="line-clamp-1">Event Reservation</CardTitle>
          <Badge variant="outline">Seat {reservation.seat?.seatNumber}</Badge>
        </div>
        <CardDescription>
          Reserved on{" "}
          {reservation.reservationDateTime
            ? new Date(reservation.reservationDateTime).toLocaleDateString()
            : "Unknown date"}
        </CardDescription>
      </CardHeader>

      <CardContent className="space-y-3">
        <div className="flex items-center text-sm text-muted-foreground">
          <MapPin className="mr-2 h-4 w-4" />
          Location: {reservation.seat?.locationId?.toString() || "Unknown"}
        </div>

        <div className="flex items-center text-sm text-muted-foreground">
          <Calendar className="mr-2 h-4 w-4" />
          Position: Row {reservation.seat?.yCoordinate}, Seat{" "}
          {reservation.seat?.xCoordinate}
        </div>
      </CardContent>

      <CardFooter className="flex gap-2">
        <Button variant="outline" size="sm" onClick={onViewSeats}>
          <Eye className="mr-2 h-4 w-4" />
          View Seat
        </Button>
        <Button variant="destructive" size="sm" onClick={onDelete}>
          <Trash2 className="mr-2 h-4 w-4" />
          Cancel
        </Button>
      </CardFooter>
    </Card>
  );
}
