"use client";

import Link from "next/link";
import { Calendar, Clock, MapPin, Users, BookmarkCheck } from "lucide-react";
import { Button } from "@/components/custom-ui/button";
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
import { LocationCardMapBackground } from "@/components/management/location-card-map-background";

interface EventCardProps {
  event: UserEventResponseDto;
  location: UserEventLocationResponseDto | null;
  reservationCount?: number;
  onReserve: () => void;
}

export function EventCard({
  event,
  location,
  reservationCount = 0,
  onReserve,
}: EventCardProps) {
  const t = useT();

  const bookingAlreadyStarted = event.bookingStartTime
    ? new Date(event.bookingStartTime) < new Date()
    : false;
  const hasAvailableSeats = (event.reservationsAllowed ?? 0) > 0;
  const isBookingOpen = event.bookingDeadline
    ? new Date(event.bookingDeadline) > new Date()
    : true;

  const buttonLabel = () => {
    if (!bookingAlreadyStarted) {
      // Booking start date and replace with formatted date
      if (event.bookingStartTime) {
        return t("eventCard.bookingOpenOnButtonWithDate", {
          date: new Date(event.bookingStartTime).toLocaleDateString(),
          time: new Date(event.bookingStartTime).toLocaleTimeString([], {
            hour: "2-digit",
            minute: "2-digit",
          }),
        });
      }
    }

    if (hasAvailableSeats && isBookingOpen) {
      return t("eventCard.reserveSeatsButton");
    } else {
      return t("eventCard.notAvailableButton");
    }
  };

  return (
    <Card className="relative overflow-hidden h-full min-h-[26rem] flex flex-col hover:shadow-lg transition-all duration-300 hover:scale-[1.02] group animate-in fade-in slide-in-from-bottom duration-500">
      {location?.address && (
        <LocationCardMapBackground address={location.address} />
      )}
      <CardHeader className="relative z-10">
        <div className="flex items-start justify-between gap-2 mb-3">
          <Badge
            variant={
              hasAvailableSeats && isBookingOpen && bookingAlreadyStarted
                ? "default"
                : "secondary"
            }
            className="animate-in zoom-in duration-300 group-hover:scale-105 transition-transform shrink-0 whitespace-nowrap"
          >
            {hasAvailableSeats && isBookingOpen && bookingAlreadyStarted
              ? t("eventCard.statusAvailable")
              : t("eventCard.statusNotAvailable")}
          </Badge>
          {reservationCount > 0 && (
            <Link
              href={`/events/reservations?eventId=${event.id}`}
              onClick={(e) => e.stopPropagation()}
              className="shrink-0"
            >
              <Badge
                variant="outline"
                className="flex items-center gap-1 whitespace-nowrap hover:bg-accent transition-colors"
              >
                <BookmarkCheck className="h-3 w-3" />
                {t("eventCard.myReservationsButton", {
                  count: reservationCount,
                })}
              </Badge>
            </Link>
          )}
        </div>
        <div className="h-[6rem] flex flex-col">
          <CardTitle className="line-clamp-2 leading-tight mb-2">
            {event.name}
          </CardTitle>
          <CardDescription className="line-clamp-2 text-sm leading-relaxed flex-1">
            {event.description}
          </CardDescription>
        </div>
      </CardHeader>

      <CardContent className="relative z-10 flex-1 space-y-3">
        <div className="flex items-center text-sm text-muted-foreground">
          <Calendar className="mr-2 h-4 w-4" />
          {event.startTime
            ? new Date(event.startTime).toLocaleDateString()
            : t("eventCard.tbd")}
        </div>

        <div className="flex items-center text-sm text-muted-foreground">
          <Clock className="mr-2 h-4 w-4" />
          {event.startTime && event.endTime
            ? `${new Date(event.startTime).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })} - ${new Date(event.endTime).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}`
            : t("eventCard.timeTbd")}
        </div>

        <div className="flex items-center text-sm text-muted-foreground">
          <MapPin className="mr-2 h-4 w-4" />
          {location?.name || t("eventCard.locationTbd")}
        </div>

        <div className="flex items-center text-sm text-muted-foreground">
          <Users className="mr-2 h-4 w-4" />
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

      <CardFooter className="relative z-10">
        <Button
          onClick={onReserve}
          className="w-full hover:scale-[1.02] transition-all duration-300 active:scale-[0.98]"
          disabled={
            !hasAvailableSeats || !isBookingOpen || !bookingAlreadyStarted
          }
        >
          {buttonLabel()}
        </Button>
      </CardFooter>
    </Card>
  );
}
