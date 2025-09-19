"use client";

import { useState } from "react";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { EventManagement } from "@/components/management/event-management";
import { LocationManagement } from "@/components/management/location-management";
import { SeatManagement } from "@/components/management/seat-management";
import { ReservationManagement } from "@/components/management/reservation-management";
import { ReservationAllowanceManagement } from "@/components/management/reservation-allowance-management";
import { useManager } from "@/hooks/use-manager";
import Loading from "./loading";
import { useT } from "@/lib/i18n/hooks";

export default function ManagerPage() {
  const t = useT();

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
        <h1 className="text-3xl font-bold mb-2">
          {t("managerPage.dashboardTitle")}
        </h1>
        <p className="text-muted-foreground">
          {t("managerPage.dashboardDescription")}
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
          <TabsTrigger value="events">{t("managerPage.eventsTab")}</TabsTrigger>
          <TabsTrigger value="locations">
            {t("managerPage.locationsTab")}
          </TabsTrigger>
          <TabsTrigger value="seats">{t("managerPage.seatsTab")}</TabsTrigger>
          <TabsTrigger value="reservations">
            {t("managerPage.reservationsTab")}
          </TabsTrigger>
          <TabsTrigger value="allowances">
            {t("managerPage.allowancesTab")}
          </TabsTrigger>
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
