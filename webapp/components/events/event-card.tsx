"use client";

import { Calendar, Clock, MapPin, Users } from "lucide-react";
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
import type { EventResponseDto } from "@/api";

interface EventCardProps {
  event: EventResponseDto;
  onReserve: () => void;
}

export function EventCard({ event, onReserve }: EventCardProps) {
  const hasAvailableSeats = (event.reservationsAllowed ?? 0) > 0;
  const isBookingOpen = event.bookingDeadline
    ? new Date(event.bookingDeadline) > new Date()
    : true;

  return (
    <Card className="h-full flex flex-col">
      <CardHeader>
        <div className="flex items-start justify-between">
          <CardTitle className="line-clamp-2">{event.name}</CardTitle>
          <Badge
            variant={
              hasAvailableSeats && isBookingOpen ? "default" : "secondary"
            }
          >
            {hasAvailableSeats && isBookingOpen ? "Available" : "Full"}
          </Badge>
        </div>
        <CardDescription className="line-clamp-3">
          {event.description}
        </CardDescription>
      </CardHeader>

      <CardContent className="flex-1 space-y-3">
        <div className="flex items-center text-sm text-muted-foreground">
          <Calendar className="mr-2 h-4 w-4" />
          {event.startTime
            ? new Date(event.startTime).toLocaleDateString()
            : "TBD"}
        </div>

        <div className="flex items-center text-sm text-muted-foreground">
          <Clock className="mr-2 h-4 w-4" />
          {event.startTime && event.endTime
            ? `${new Date(event.startTime).toLocaleTimeString()} - ${new Date(event.endTime).toLocaleTimeString()}`
            : "Time TBD"}
        </div>

        <div className="flex items-center text-sm text-muted-foreground">
          <MapPin className="mr-2 h-4 w-4" />
          {event.location?.name || "Location TBD"}
        </div>

        <div className="flex items-center text-sm text-muted-foreground">
          <Users className="mr-2 h-4 w-4" />
          {event.reservationsAllowed} seats available
        </div>

        {event.bookingDeadline && (
          <div className="text-sm text-muted-foreground">
            Booking until: {new Date(event.bookingDeadline).toLocaleString()}
          </div>
        )}
      </CardContent>

      <CardFooter>
        <Button
          onClick={onReserve}
          className="w-full"
          disabled={!hasAvailableSeats || !isBookingOpen}
        >
          {hasAvailableSeats && isBookingOpen
            ? "Reserve Seats"
            : "Not Available"}
        </Button>
      </CardFooter>
    </Card>
  );
}
