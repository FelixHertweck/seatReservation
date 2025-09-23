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
import type { UserEventLocationResponseDto, UserEventResponseDto } from "@/api";
import { useT } from "@/lib/i18n/hooks";

interface EventCardProps {
  event: UserEventResponseDto;
  location: UserEventLocationResponseDto | null;
  onReserve: () => void;
}

export function EventCard({ event, location, onReserve }: EventCardProps) {
  const t = useT();

  const hasAvailableSeats = (event.reservationsAllowed ?? 0) > 0;
  const isBookingOpen = event.bookingDeadline
    ? new Date(event.bookingDeadline) > new Date()
    : true;

  return (
    <Card className="h-full flex flex-col hover:shadow-lg transition-all duration-300 hover:scale-[1.02] group animate-in fade-in slide-in-from-bottom duration-500">
      <CardHeader className="group-hover:bg-accent/5 transition-colors duration-300">
        <div className="flex items-start justify-between">
          <CardTitle className="line-clamp-2 group-hover:text-primary transition-colors duration-300">
            {event.name}
          </CardTitle>
          <Badge
            variant={
              hasAvailableSeats && isBookingOpen ? "default" : "secondary"
            }
            className="animate-in zoom-in duration-300 group-hover:scale-105 transition-transform"
          >
            {hasAvailableSeats && isBookingOpen
              ? t("eventCard.statusAvailable")
              : t("eventCard.statusFull")}
          </Badge>
        </div>
        <CardDescription className="line-clamp-3">
          {event.description}
        </CardDescription>
      </CardHeader>

      <CardContent className="flex-1 space-y-3">
        <div className="flex items-center text-sm text-muted-foreground group-hover:text-foreground transition-colors duration-300">
          <Calendar className="mr-2 h-4 w-4 group-hover:scale-110 transition-transform duration-300" />
          {event.startTime
            ? new Date(event.startTime).toLocaleDateString()
            : t("eventCard.tbd")}
        </div>

        <div className="flex items-center text-sm text-muted-foreground group-hover:text-foreground transition-colors duration-300">
          <Clock className="mr-2 h-4 w-4 group-hover:scale-110 transition-transform duration-300" />
          {event.startTime && event.endTime
            ? `${new Date(event.startTime).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })} - ${new Date(event.endTime).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}`
            : t("eventCard.timeTbd")}
        </div>

        <div className="flex items-center text-sm text-muted-foreground group-hover:text-foreground transition-colors duration-300">
          <MapPin className="mr-2 h-4 w-4 group-hover:scale-110 transition-transform duration-300" />
          {location?.name || t("eventCard.locationTbd")}
        </div>

        <div className="flex items-center text-sm text-muted-foreground group-hover:text-foreground transition-colors duration-300">
          <Users className="mr-2 h-4 w-4 group-hover:scale-110 transition-transform duration-300" />
          {t("eventCard.seatsAvailable", { count: event.reservationsAllowed })}
        </div>

        {event.bookingDeadline && (
          <div className="text-sm text-muted-foreground">
            {t("eventCard.bookingUntil")}:{" "}
            {new Date(event.bookingDeadline).toLocaleDateString()},{" "}
            {new Date(event.bookingDeadline).toLocaleTimeString([], {
              hour: "2-digit",
              minute: "2-digit",
            })}
          </div>
        )}
      </CardContent>

      <CardFooter>
        <Button
          onClick={onReserve}
          className="w-full hover:scale-[1.02] transition-all duration-300 active:scale-[0.98]"
          disabled={!hasAvailableSeats || !isBookingOpen}
        >
          {hasAvailableSeats && isBookingOpen
            ? t("eventCard.reserveSeatsButton")
            : t("eventCard.notAvailableButton")}
        </Button>
      </CardFooter>
    </Card>
  );
}
