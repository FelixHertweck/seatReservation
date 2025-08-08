"use client";

import { useState } from "react";
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
  const [activeTab, setActiveTab] = useState("events");
  const [filterValues, setFilterValues] = useState<Record<string, string>>({});

  if (managerData.isLoading) {
    return <Loading />;
  }

  const navigateToTab = (
    tab: string,
    filterId?: bigint,
    filterType?: string,
  ) => {
    setActiveTab(tab);
    if (filterId && filterType) {
      setFilterValues({ [filterType]: filterId.toString() });
    } else {
      setFilterValues({});
    }
  };

  const clearFilters = () => {
    setFilterValues({});
  };

  return (
    <div className="container mx-auto p-6">
      <div className="mb-6">
        <h1 className="text-3xl font-bold mb-2">Manager Dashboard</h1>
        <p className="text-muted-foreground">
          Manage events, locations, seats, and reservations
        </p>
      </div>

      <Tabs
        value={activeTab}
        onValueChange={(value) => {
          setActiveTab(value);
          clearFilters();
        }}
        className="space-y-4"
      >
        <TabsList className="grid w-full grid-cols-5">
          <TabsTrigger value="events">Events</TabsTrigger>
          <TabsTrigger value="locations">Locations</TabsTrigger>
          <TabsTrigger value="seats">Seats</TabsTrigger>
          <TabsTrigger value="reservations">Reservations</TabsTrigger>
          <TabsTrigger value="allowances">Allowances</TabsTrigger>
        </TabsList>

        <TabsContent value="events">
          <EventManagement
            {...managerData.events}
            onNavigateToLocation={(locationId) =>
              navigateToTab("locations", locationId, "locationId")
            }
            initialFilter={activeTab === "events" ? filterValues : {}}
          />
        </TabsContent>

        <TabsContent value="locations">
          <LocationManagement
            {...managerData.locations}
            onNavigateToSeats={(locationId) =>
              navigateToTab("seats", locationId, "locationId")
            }
            initialFilter={activeTab === "locations" ? filterValues : {}}
          />
        </TabsContent>

        <TabsContent value="seats">
          <SeatManagement
            {...managerData.seats}
            onNavigateToLocation={(locationId) =>
              navigateToTab("locations", locationId, "locationId")
            }
            initialFilter={activeTab === "seats" ? filterValues : {}}
          />
        </TabsContent>

        <TabsContent value="reservations">
          <ReservationManagement
            {...managerData.reservations}
            onNavigateToEvent={(eventId) =>
              navigateToTab("events", eventId, "eventId")
            }
            onNavigateToSeat={(seatId) =>
              navigateToTab("seats", seatId, "seatId")
            }
            initialFilter={activeTab === "reservations" ? filterValues : {}}
          />
        </TabsContent>

        <TabsContent value="allowances">
          <ReservationAllowanceManagement
            {...managerData.reservationAllowance}
            onNavigateToEvent={(eventId) =>
              navigateToTab("events", eventId, "eventId")
            }
            initialFilter={activeTab === "allowances" ? filterValues : {}}
          />
        </TabsContent>
      </Tabs>
    </div>
  );
}
