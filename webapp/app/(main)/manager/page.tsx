"use client";

import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { EventManagement } from "@/components/manager/event-management";
import { LocationManagement } from "@/components/manager/location-management";
import { SeatManagement } from "@/components/manager/seat-management";
import { ReservationManagement } from "@/components/manager/reservation-management";
import { ReservationAllowanceManagement } from "@/components/manager/reservation-allowance-management";
import { useManager } from "@/hooks/use-manager";
import Loading from "./loading";

export default function ManagerPage() {
  const managerData = useManager();

  if (managerData.isLoading) {
    return <Loading />;
  }

  return (
    <div className="container mx-auto p-6">
      <div className="mb-6">
        <h1 className="text-3xl font-bold mb-2">Manager Dashboard</h1>
        <p className="text-muted-foreground">
          Manage events, locations, seats, and reservations
        </p>
      </div>

      <Tabs defaultValue="events" className="space-y-4">
        <TabsList className="grid w-full grid-cols-5">
          <TabsTrigger value="events">Events</TabsTrigger>
          <TabsTrigger value="locations">Locations</TabsTrigger>
          <TabsTrigger value="seats">Seats</TabsTrigger>
          <TabsTrigger value="reservations">Reservations</TabsTrigger>
          <TabsTrigger value="allowances">Allowances</TabsTrigger>
        </TabsList>

        <TabsContent value="events">
          <EventManagement {...managerData.events} />
        </TabsContent>

        <TabsContent value="locations">
          <LocationManagement {...managerData.locations} />
        </TabsContent>

        <TabsContent value="seats">
          <SeatManagement {...managerData.seats} />
        </TabsContent>

        <TabsContent value="reservations">
          <ReservationManagement {...managerData.reservations} />
        </TabsContent>

        <TabsContent value="allowances">
          <ReservationAllowanceManagement
            {...managerData.reservationAllowance}
          />
        </TabsContent>
      </Tabs>
    </div>
  );
}
