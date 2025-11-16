"use client";

import { useState, useRef, useCallback } from "react";
import { useT } from "@/lib/i18n/hooks";
import { useCheckin } from "@/hooks/use-checkin";
import type { CheckInInfoRequestDto, CheckInInfoResponseDto } from "@/api";
import {
  QrCodeScanner,
  type ScannedData,
} from "@/components/checkin/qr-code-scanner";
import { ReservationSelector } from "@/components/checkin/reservation-selector";
import { useIsMobile } from "@/hooks/use-mobile";
import { ChevronUp, Loader2 } from "lucide-react";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { UsernameSelector } from "@/components/checkin/username-selector";
// Select component is now extracted into EventSelector for event selection
import EventSelector from "@/components/common/supervisor/event-selector";

export default function CheckInPage() {
  const t = useT();
  const [isScanning, setIsScanning] = useState(false);
  const [scannedData, setScannedData] = useState<ScannedData | null>(null);
  const [selectedReservations, setSelectedReservations] = useState<Set<bigint>>(
    new Set(),
  );
  const [isDrawerOpen, setIsDrawerOpen] = useState(false);
  const [selectedEventId, setSelectedEventId] = useState<bigint | null>(null);
  const [resetUsernameSelector, setResetUsernameSelector] =
    useState<boolean>(false);
  const isMobile = useIsMobile();
  const lastScannedDataRef = useRef<string | null>(null);
  const [checkInInfo, setCheckInInfo] = useState<
    CheckInInfoResponseDto | undefined | null
  >(null);

  const {
    isLoadingInfo,
    fetchCheckInInfo,

    isLoadingPerformCheckIn,
    performCheckIn,

    events,
    isLoadingEvents,

    getUsernamesByEventId,

    fetchCheckInInfoByUsername,
  } = useCheckin();

  const handleScan = useCallback(
    (data: ScannedData) => {
      setIsScanning(false);
      setScannedData(data);

      if (data) {
        const scannedDataKey = `${data.userId}-${data.eventId}-${data.checkInTokens.join(",")}`;

        // Only fetch if this is a new scan (different from last scanned data)
        if (lastScannedDataRef.current !== scannedDataKey) {
          lastScannedDataRef.current = scannedDataKey;

          const checkInInfoRequest: CheckInInfoRequestDto = {
            userId: BigInt(data.userId),
            eventId: BigInt(data.eventId),
            checkInTokens: data.checkInTokens,
          };
          fetchCheckInInfo(checkInInfoRequest).then((info) => {
            setCheckInInfo(info);
          });
        }
      }
    },
    [fetchCheckInInfo],
  );

  // Handle check-in submission
  const handleSubmit = async (userId: bigint, eventId: bigint) => {
    if (!checkInInfo?.reservations) return;

    const checkIn: bigint[] = [];
    const cancel: bigint[] = [];

    checkInInfo.reservations.forEach((reservation) => {
      if (reservation.id) {
        if (selectedReservations.has(reservation.id)) {
          checkIn.push(reservation.id);
        } else {
          cancel.push(reservation.id);
        }
      }
    });

    await performCheckIn({ userId, eventId, checkIn, cancel });

    // Close drawer and reset
    setIsDrawerOpen(false);
    setCheckInInfo(null);
    setScannedData(null);
    setResetUsernameSelector((prev) => !prev);
    setSelectedReservations(new Set());
    lastScannedDataRef.current = null;
    setIsScanning(true); // Restart scanning after submission
  };

  // Clear scanned data
  const handleClear = () => {
    setIsDrawerOpen(false);
    setScannedData(null);
    setCheckInInfo(null);
    setResetUsernameSelector((prev) => !prev);
    setSelectedReservations(new Set());
    lastScannedDataRef.current = null;
    setIsScanning(true); // Restart scanning after clearing data
  };

  const onSelectUsername = (username: string) => {
    if (selectedEventId) {
      fetchCheckInInfoByUsername(username).then((info) => {
        setCheckInInfo(info);
      });
    }
  };

  const handleEventSelect = (eventId: string) => {
    setSelectedEventId(BigInt(eventId));
    setCheckInInfo(null);
    setScannedData(null);
    setResetUsernameSelector((prev) => !prev);
    setSelectedReservations(new Set());
    lastScannedDataRef.current = null;
    setIsScanning(true);
  };

  return (
    <div className="container mx-auto px-2 py-3 md:p-6">
      <div className="mb-3 md:mb-6">
        <h1 className="text-2xl md:text-3xl font-bold mb-1 md:mb-2">
          {t("checkin.title")}
        </h1>
        <p className="text-muted-foreground text-sm md:text-base">
          {t("checkin.description")}
        </p>
      </div>

      {/* Event Selector */}

      <EventSelector
        events={events}
        isLoadingEvents={isLoadingEvents}
        selectedEventId={selectedEventId}
        onEventSelect={(id) => handleEventSelect(id)}
        labelKey="checkin.eventSelector.label"
        placeholderKey="checkin.eventSelector.placeholder"
        noEventsKey="checkin.eventSelector.noEvents"
      />

      {/* Show content only if event is selected */}
      {!selectedEventId ? (
        <div className="text-center py-12 text-muted-foreground">
          <p className="text-lg">{t("checkin.eventSelector.selectFirst")}</p>
        </div>
      ) : isLoadingEvents || isLoadingInfo ? (
        <div className="p-4 border rounded-lg bg-card flex items-center justify-center max-h-[70vh]">
          <div className="flex flex-col items-center gap-2 text-muted-foreground">
            <Loader2 className="h-8 w-8 animate-spin" />
            <span>{t("checkin.reservations.loading")}</span>
          </div>
        </div>
      ) : (
        <div
          className={`grid gap-6 ${isMobile ? "grid-cols-1" : "md:grid-cols-2"}`}
        >
          <Tabs
            defaultValue="qr-code-scanner"
            className={isMobile ? "" : "md:sticky md:top-4 md:h-fit"}
            onValueChange={(value) => {
              if (value === "qr-code-scanner") {
                setIsScanning(true);
              } else {
                setIsScanning(false);
              }
            }}
          >
            <TabsList className="grid w-full grid-cols-2">
              <TabsTrigger value="qr-code-scanner">
                {t("checkin.tabs.qrScanner")}
              </TabsTrigger>
              <TabsTrigger value="username-selection">
                {t("checkin.tabs.userSelection")}
              </TabsTrigger>
            </TabsList>
            <TabsContent value="qr-code-scanner">
              {/* QR Scanner Section */}
              <div>
                <QrCodeScanner
                  onScan={handleScan}
                  isScanning={isScanning}
                  setIsScanning={setIsScanning}
                  scannedData={scannedData}
                  setScannedData={setScannedData}
                />
              </div>
            </TabsContent>
            <TabsContent value="username-selection">
              {/* Username Input Section */}
              <UsernameSelector
                eventId={selectedEventId}
                onSelectUsername={onSelectUsername}
                getUsernamesByEventId={getUsernamesByEventId}
                resetTrigger={resetUsernameSelector}
              />
            </TabsContent>
          </Tabs>

          {/* Reservations Section - Desktop */}
          <ReservationSelector
            checkInInfo={checkInInfo}
            eventId={selectedEventId}
            isLoadingInfo={isLoadingInfo}
            isLoading={isLoadingPerformCheckIn}
            isMobile={isMobile}
            isDrawerOpen={isDrawerOpen}
            setIsDrawerOpen={setIsDrawerOpen}
            selectedReservations={selectedReservations}
            setSelectedReservations={setSelectedReservations}
            onSubmit={handleSubmit}
            onClear={handleClear}
          />
        </div>
      )}

      {/* Drawer Trigger - Mobile */}
      {isMobile && scannedData && checkInInfo && (
        <div
          className={`fixed bottom-0 left-0 right-0 bg-background border-t p-2 flex justify-center cursor-pointer shadow-lg ${
            isDrawerOpen ? "hidden" : ""
          }`}
          onClick={() => setIsDrawerOpen(true)}
        >
          <ChevronUp className="h-6 w-6 text-muted-foreground" />
          <span className="sr-only">
            {t("checkin.reservations.openDrawer")}
          </span>
        </div>
      )}
    </div>
  );
}
