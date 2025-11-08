"use client";

import { useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { EventManagement } from "@/components/management/event-management";
import { LocationManagement } from "@/components/management/location-management";
import { SeatManagement } from "@/components/management/seat-management";
import { ReservationManagement } from "@/components/management/reservation-management";
import { ReservationAllowanceManagement } from "@/components/management/reservation-allowance-management";
import { useManager } from "@/hooks/use-manager";
import { useT } from "@/lib/i18n/hooks";
import { useIsMobile } from "@/hooks/use-mobile";

export default function ManagerPage() {
  const t = useT();
  const isMobile = useIsMobile();
  const router = useRouter();
  const searchParams = useSearchParams();

  const managerData = useManager();
  const [activeTab, setActiveTab] = useState(
    searchParams.get("tab") || "events",
  );
  const [filterValues, setFilterValues] = useState<Record<string, string>>({});

  const navigateToTab = (
    tab: string,
    filterId?: bigint,
    filterType?: string,
  ) => {
    setActiveTab(tab);
    const params = new URLSearchParams(searchParams);
    params.set("tab", tab);
    router.push(`?${params.toString()}`);

    if (filterId && filterType) {
      setFilterValues({ [filterType]: filterId.toString() });
    } else {
      setFilterValues({});
    }
  };

  const clearFilters = () => {
    setFilterValues({});
  };

  const handleTabChange = (tab: string) => {
    setActiveTab(tab);
    const params = new URLSearchParams(searchParams);
    params.set("tab", tab);
    router.push(`?${params.toString()}`);
    clearFilters();
  };

  return (
    <div className="container mx-auto p-4 sm:p-6">
      <div className="mb-4 sm:mb-6">
        <h1 className="text-2xl sm:text-3xl font-bold mb-2">
          {t("managerPage.dashboardTitle")}
        </h1>
        <p className="text-sm sm:text-base text-muted-foreground">
          {t("managerPage.dashboardDescription")}
        </p>
      </div>

      <Tabs
        value={activeTab}
        onValueChange={handleTabChange}
        className="space-y-4"
      >
        {isMobile ? (
          <Select value={activeTab} onValueChange={handleTabChange}>
            <SelectTrigger className="w-full">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="events">
                {t("managerPage.eventsTab")}
              </SelectItem>
              <SelectItem value="locations">
                {t("managerPage.locationsTab")}
              </SelectItem>
              <SelectItem value="seats">{t("managerPage.seatsTab")}</SelectItem>
              <SelectItem value="reservations">
                {t("managerPage.reservationsTab")}
              </SelectItem>
              <SelectItem value="allowances">
                {t("managerPage.allowancesTab")}
              </SelectItem>
            </SelectContent>
          </Select>
        ) : (
          <TabsList className="grid w-full grid-cols-5">
            <TabsTrigger value="events">
              {t("managerPage.eventsTab")}
            </TabsTrigger>
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
        )}

        <TabsContent value="events">
          <EventManagement
            {...managerData.events}
            isLoading={managerData.isLoading}
            onNavigateToLocation={(locationId) =>
              navigateToTab("locations", locationId, "locationId")
            }
            initialFilter={activeTab === "events" ? filterValues : {}}
          />
        </TabsContent>

        <TabsContent value="locations">
          <LocationManagement
            {...managerData.locations}
            isLoading={managerData.isLoading}
            onNavigateToSeats={(locationId) =>
              navigateToTab("seats", locationId, "locationId")
            }
            initialFilter={activeTab === "locations" ? filterValues : {}}
          />
        </TabsContent>

        <TabsContent value="seats">
          <SeatManagement
            {...managerData.seats}
            isLoading={managerData.isLoading}
            onNavigateToLocation={(locationId) =>
              navigateToTab("locations", locationId, "locationId")
            }
            initialFilter={activeTab === "seats" ? filterValues : {}}
          />
        </TabsContent>

        <TabsContent value="reservations">
          <ReservationManagement
            {...managerData.reservations}
            isLoading={managerData.isLoading}
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
            isLoading={managerData.isLoading}
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
