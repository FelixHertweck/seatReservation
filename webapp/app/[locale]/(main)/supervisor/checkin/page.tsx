"use client";

import {
  Suspense,
  useEffect,
  useMemo,
  useState,
  useRef,
  useCallback,
} from "react";
import { useRouter, usePathname, useSearchParams } from "next/navigation";
import { useT } from "@/lib/i18n/hooks";
import { formatDateTime } from "@/lib/utils";
import { useCheckin } from "@/hooks/use-checkin";
import { useSupervisorEvent } from "@/hooks/use-supervisor-event";
import type { CheckInInfoRequestDto, CheckInInfoResponseDto } from "@/api";
import {
  QrCodeScanner,
  type ScannedData,
} from "@/components/checkin/qr-code-scanner";
import {
  ReservationSelector,
  type ReservationAction,
} from "@/components/checkin/reservation-selector";
import { useIsMobile } from "@/hooks/use-mobile";
import { ArrowUp, ChevronUp, Clock, Loader2 } from "lucide-react";
import {
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
} from "@/components/custom-ui/tabs";
import { UsernameSelector } from "@/components/checkin/username-selector";
import EventSelector from "@/components/common/supervisor/event-selector";
import { PageHeader } from "@/components/page-header";
import { Card, CardContent } from "@/components/ui/card";

function CheckInPageContent() {
  const t = useT();
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const { selectedEventId, selectEvent } = useSupervisorEvent();
  const [isScanning, setIsScanning] = useState(false);
  const [scannedData, setScannedData] = useState<ScannedData | null>(null);
  const [reservationActions, setReservationActions] = useState<
    Record<string, ReservationAction>
  >({});
  const [isDrawerOpen, setIsDrawerOpen] = useState(false);
  const [activeTab, setActiveTab] = useState<string>(
    () => searchParams.get("tab") || "qr-code-scanner",
  );
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

  // Read via an effect rather than at render time (Date.now() is impure) -- refreshed
  // periodically so an event crossing its booking deadline while this page stays open
  // becomes available without a manual reload.
  const [now, setNow] = useState<number | null>(null);
  useEffect(() => {
    // Ticking clock synced from an external source (the system clock), so the
    // initial read + periodic refresh both belong here rather than in render.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setNow(Date.now());
    const interval = setInterval(() => setNow(Date.now()), 60_000);
    return () => clearInterval(interval);
  }, []);

  const availableEvents = useMemo(() => {
    if (now === null) return [];
    return (events ?? []).filter(
      (e) => !!e.bookingDeadline && new Date(e.bookingDeadline).getTime() < now,
    );
  }, [events, now]);
  const selectedEvent = useMemo(
    () => (events ?? []).find((e) => e.id === selectedEventId),
    [events, selectedEventId],
  );
  const isDeadlinePassed = useMemo(() => {
    if (now === null || !selectedEvent?.bookingDeadline) return false;
    return new Date(selectedEvent.bookingDeadline).getTime() < now;
  }, [selectedEvent, now]);

  const handleScan = useCallback(
    (data: ScannedData) => {
      setIsScanning(false);
      setScannedData(data);

      if (data) {
        const scannedDataKey = `${data.userId}-${data.eventId}-${data.checkInToken}`;

        // Only fetch if this is a new scan (different from last scanned data)
        if (lastScannedDataRef.current !== scannedDataKey) {
          lastScannedDataRef.current = scannedDataKey;

          const checkInInfoRequest: CheckInInfoRequestDto = {
            userId: data.userId,
            eventId: data.eventId,
            checkInToken: data.checkInToken,
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
  const handleSubmit = async (userId: string, eventId: string) => {
    if (!checkInInfo?.reservations) return;

    const checkIn: string[] = [];
    const cancel: string[] = [];

    checkInInfo.reservations.forEach((reservation) => {
      if (reservation.id) {
        const action = reservationActions[reservation.id];
        if (action === "CHECK_IN") {
          checkIn.push(reservation.id);
        } else if (action === "CANCEL") {
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
    setReservationActions({});
    lastScannedDataRef.current = null;
    setIsScanning(true); // Restart scanning after submission
  };

  // Clear scanned data
  const handleClear = () => {
    setIsDrawerOpen(false);
    setScannedData(null);
    setCheckInInfo(null);
    setResetUsernameSelector((prev) => !prev);
    setReservationActions({});
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
    selectEvent(eventId);
    setCheckInInfo(null);
    setScannedData(null);
    setResetUsernameSelector((prev) => !prev);
    setReservationActions({});
    lastScannedDataRef.current = null;
    setIsScanning(true);
  };

  const handleTabChange = (value: string) => {
    setActiveTab(value);
    setIsScanning(value === "qr-code-scanner");

    const params = new URLSearchParams(searchParams.toString());
    params.set("tab", value);
    router.replace(`${pathname}?${params.toString()}`, { scroll: false });
  };

  return (
    <div className="container mx-auto p-4 sm:p-6">
      <PageHeader
        title={t("checkin.title")}
        description={t("checkin.description")}
        search={
          <EventSelector
            events={availableEvents}
            isLoadingEvents={isLoadingEvents}
            selectedEventId={selectedEventId}
            onEventSelect={handleEventSelect}
            placeholderKey="checkin.eventSelector.placeholder"
            noEventsKey="checkin.eventSelector.noEvents"
          />
        }
      />

      {/* Show content only if event is selected */}
      {!selectedEventId ? (
        <Card>
          <CardContent className="flex flex-col items-center gap-2 py-12 text-center text-muted-foreground">
            <ArrowUp className="h-5 w-5" />
            {t("checkin.eventSelector.selectFirst")}
          </CardContent>
        </Card>
      ) : isLoadingEvents || isLoadingInfo ? (
        <div className="p-4 border rounded-lg bg-card flex items-center justify-center max-h-[70vh]">
          <div className="flex flex-col items-center gap-2 text-muted-foreground">
            <Loader2 className="h-8 w-8 animate-spin" />
            <span>{t("checkin.reservations.loading")}</span>
          </div>
        </div>
      ) : !isDeadlinePassed ? (
        <Card>
          <CardContent className="flex flex-col items-center gap-2 py-12 text-center text-muted-foreground">
            <Clock className="h-5 w-5" />
            <span>{t("checkin.deadlineNotPassed")}</span>
            {(() => {
              const formatted = formatDateTime(selectedEvent?.bookingDeadline);
              return formatted ? (
                <span className="text-sm">
                  {t("checkin.deadlineNotPassedAvailableFrom", formatted)}
                </span>
              ) : null;
            })()}
          </CardContent>
        </Card>
      ) : (
        <div
          className={`grid gap-6 ${isMobile ? "grid-cols-1" : "md:grid-cols-2"}`}
        >
          <Tabs
            value={activeTab}
            className={isMobile ? "" : "md:sticky md:top-4 md:h-fit"}
            onValueChange={handleTabChange}
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
            reservationActions={reservationActions}
            setReservationActions={setReservationActions}
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

export default function CheckInPage() {
  return (
    <Suspense fallback={null}>
      <CheckInPageContent />
    </Suspense>
  );
}
