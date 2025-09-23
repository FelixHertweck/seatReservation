"use client";

import { useState } from "react";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { useReservations } from "@/hooks/use-reservations";
import { useT } from "@/lib/i18n/hooks";
import EventsSubPage from "@/components/events/events-page";
import ReservationsSubPage from "@/components/reservations/reservation-page";

export default function EventsPage() {
  const t = useT();
  const { reservations, isLoading: reservationsLoading } = useReservations();
  const [activeTab, setActiveTab] = useState("available");

  const isLoading = reservationsLoading;

  return (
    <div className="container mx-auto px-2 py-3 md:p-6">
      <div className="mb-3 md:mb-6">
        <h1 className="text-2xl md:text-3xl font-bold mb-1 md:mb-2">
          {t("eventsPage.title")}
        </h1>
        <p className="text-muted-foreground text-sm md:text-base">
          {t("eventsPage.description")}
        </p>
      </div>

      <Tabs value={activeTab} onValueChange={setActiveTab} className="w-full">
        <TabsList className="grid w-full grid-cols-2">
          <TabsTrigger value="available">
            {t("eventsPage.availableEventsTab")}
          </TabsTrigger>

          <TabsTrigger value="reservations" className="flex items-center gap-2">
            {t("eventsPage.myReservationsTab")}
            {isLoading ? (
              <Skeleton className="h-5 w-6 ml-1" />
            ) : reservations.length > 0 ? (
              <Badge variant="secondary" className="ml-1">
                {reservations.length}
              </Badge>
            ) : null}
          </TabsTrigger>
        </TabsList>

        <TabsContent value="reservations" className="space-y-4 md:space-y-6">
          <ReservationsSubPage />
        </TabsContent>

        <TabsContent value="available" className="space-y-4 md:space-y-6">
          <EventsSubPage />
        </TabsContent>
      </Tabs>
    </div>
  );
}
