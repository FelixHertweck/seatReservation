"use client";

import { Suspense, useMemo, useState } from "react";
import { ArrowUp, ChevronUp, Clock, Loader2 } from "lucide-react";

import { useT } from "@/lib/i18n/hooks";
import { formatDateTime } from "@/lib/utils";
import { useSupervisorEvent } from "@/hooks/use-supervisor-event";
import { useIsMobile } from "@/hooks/use-mobile";
import { PageHeader } from "@/components/page-header";
import { Card, CardContent } from "@/components/ui/card";
import {
  Drawer,
  DrawerContent,
  DrawerHeader,
  DrawerTitle,
} from "@/components/ui/drawer";
import { Skeleton } from "@/components/custom-ui/skeleton";
import EventSelector from "@/components/common/supervisor/event-selector";
import { SeatMap } from "@/components/common/seat-map";
import SeatmapLegend from "@/components/common/seatmap-legend";
import {
  BoxOfficeActionPanel,
  type BoxOfficeReserveMode,
} from "@/components/box-office/box-office-action-panel";
import { BoxOfficeConfirmation } from "@/components/box-office/box-office-confirmation";
import { CancelUncheckedButton } from "@/components/box-office/cancel-unchecked-button";
import { LiveviewConnectionBadge } from "@/components/liveview/liveview-status";
import { useSupervisorLiveView } from "@/hooks/use-supervisor-liveview";
import { useCheckin } from "@/hooks/use-checkin";
import { useBoxOffice } from "@/hooks/use-box-office";
import { useFillHeight } from "@/hooks/use-fill-height";
import type {
  BoxOfficeReservationResponseDto,
  SeatDto,
  SupervisorReservationResponseDto,
  SupervisorSeatStatusDto,
} from "@/api";

const convertReservationsToStatuses = (
  reservations: SupervisorReservationResponseDto[],
): SupervisorSeatStatusDto[] =>
  reservations.map((reservation) => ({
    seatId: reservation.seat?.id,
    status: reservation.status,
    reservationId: reservation.id,
    liveStatus: reservation.liveStatus,
  }));

function BoxOfficePageContent() {
  const t = useT();
  const { selectedEventId, selectEvent } = useSupervisorEvent();
  const isMobile = useIsMobile();
  const [isDrawerOpen, setIsDrawerOpen] = useState(false);

  const [selectedSeats, setSelectedSeats] = useState<SeatDto[]>([]);
  const [highlightedSeatId, setHighlightedSeatId] = useState<string | null>(
    null,
  );
  const [reserveMode, setReserveMode] = useState<BoxOfficeReserveMode>("user");
  const [userId, setUserId] = useState("");
  const [deductAllowance, setDeductAllowance] = useState(true);
  const [guestName, setGuestName] = useState("");
  const [guestEmail, setGuestEmail] = useState("");
  const [checkedIn, setCheckedIn] = useState(false);
  const [confirmation, setConfirmation] = useState<{
    result: BoxOfficeReservationResponseDto;
    guestEmail?: string;
    notifiedUsername?: string;
  } | null>(null);

  const { ref: seatMapColumnRef, height: seatMapColumnHeight } =
    useFillHeight<HTMLDivElement>();

  const { isLoadingEvents } = useCheckin();
  const {
    users,
    isLoadingUsers,
    createForKnownUser,
    createForGuest,
    isSubmitting,
    cancelUnchecked,
    isCancellingUnchecked,
  } = useBoxOffice();

  const {
    isInitialLoading,
    isConnected,
    isConnecting,
    error,
    location,
    reservations,
    availableEvents,
    isDeadlinePassed,
    selectedEvent,
    event,
    reconnect,
  } = useSupervisorLiveView();

  const seatStatuses = useMemo(
    () => convertReservationsToStatuses(reservations),
    [reservations],
  );

  const awaitingCheckInCount = useMemo(
    () =>
      reservations.filter(
        (r) =>
          r.status === "RESERVED" &&
          (!r.liveStatus || r.liveStatus === "NO_SHOW"),
      ).length,
    [reservations],
  );

  const resetForm = () => {
    setSelectedSeats([]);
    setHighlightedSeatId(null);
    setUserId("");
    setDeductAllowance(true);
    setGuestName("");
    setGuestEmail("");
    setCheckedIn(false);
  };

  const handleEventSelect = (eventId: string) => {
    selectEvent(eventId);
    resetForm();
    setConfirmation(null);
  };

  const handleSeatToggle = (seat: SeatDto) => {
    setSelectedSeats((prev) => {
      const isSelected = prev.some((s) => s.id === seat.id);
      if (isSelected) {
        return prev.filter((s) => s.id !== seat.id);
      }
      return [...prev, seat];
    });
    setHighlightedSeatId((prev) => (prev === seat.id ? null : prev));
  };

  const handleSeatChipClick = (seatId: string) => {
    setHighlightedSeatId((prev) => (prev === seatId ? null : seatId));
    if (isMobile) setIsDrawerOpen(false);
  };

  const handleSubmit = async () => {
    if (!selectedEventId) return;

    if (reserveMode === "user") {
      const result = await createForKnownUser({
        eventId: selectedEventId,
        userId,
        seatIds: selectedSeats.map((seat) => seat.id!),
        deductAllowance,
        checkedIn,
      });
      setConfirmation({
        result,
        notifiedUsername: users.find((u) => u.id === userId)?.username,
      });
    } else {
      const result = await createForGuest({
        eventId: selectedEventId,
        seatIds: selectedSeats.map((seat) => seat.id!),
        guestName,
        guestEmail: guestEmail || undefined,
        checkedIn,
      });
      setConfirmation({ result, guestEmail: guestEmail || undefined });
    }
    resetForm();
  };

  const handleCreateAnother = () => {
    setConfirmation(null);
    if (isMobile) setIsDrawerOpen(false);
  };

  const drawerTitle = useMemo(() => {
    if (confirmation) {
      return t("boxOffice.confirmation.title");
    }
    if (selectedSeats.length > 0) {
      return `${t("boxOffice.title")} (${selectedSeats.length})`;
    }
    return t("boxOffice.title");
  }, [confirmation, selectedSeats.length, t]);

  return (
    <div className="container mx-auto p-4 sm:p-6">
      <PageHeader
        title={t("boxOffice.title")}
        description={t("boxOffice.pageDescription")}
        actions={
          selectedEventId && isDeadlinePassed ? (
            <>
              <CancelUncheckedButton
                seatCount={awaitingCheckInCount}
                isCancelling={isCancellingUnchecked}
                onCancel={() => cancelUnchecked(selectedEventId)}
              />
              <LiveviewConnectionBadge
                isConnected={isConnected}
                isConnecting={isConnecting}
                error={error}
                event={event}
                onReconnect={reconnect}
              />
            </>
          ) : undefined
        }
        search={
          <EventSelector
            events={availableEvents}
            isLoadingEvents={isLoadingEvents}
            selectedEventId={selectedEventId}
            onEventSelect={handleEventSelect}
          />
        }
      />

      {!selectedEventId ? (
        <Card>
          <CardContent className="flex flex-col items-center gap-2 py-12 text-center text-muted-foreground">
            <ArrowUp className="h-5 w-5" />
            {t("boxOffice.selectEventPrompt")}
          </CardContent>
        </Card>
      ) : isInitialLoading ? (
        <div className="flex max-h-[70vh] items-center justify-center rounded-lg border bg-card p-4">
          <div className="flex flex-col items-center gap-2 text-muted-foreground">
            <Loader2 className="h-8 w-8 animate-spin" />
            <span>{t("liveview.status.loading")}</span>
          </div>
        </div>
      ) : !isDeadlinePassed ? (
        <Card>
          <CardContent className="flex flex-col items-center gap-2 py-12 text-center text-muted-foreground">
            <Clock className="h-5 w-5" />
            <span>{t("boxOffice.deadlineNotPassed")}</span>
            {(() => {
              const formatted = formatDateTime(selectedEvent?.bookingDeadline);
              return formatted ? (
                <span className="text-sm">
                  {t("boxOffice.deadlineNotPassedAvailableFrom", formatted)}
                </span>
              ) : null;
            })()}
          </CardContent>
        </Card>
      ) : (
        <div
          className={`grid gap-4 ${isMobile ? "grid-cols-1" : "lg:grid-cols-2"}`}
        >
          <div
            ref={seatMapColumnRef}
            className="flex flex-col gap-2"
            style={{ height: seatMapColumnHeight }}
          >
            <SeatmapLegend
              layout="bar"
              areas={location?.areas ?? []}
              showSelected
              showLiveStatus
            />
            {!location ? (
              <Skeleton className="flex-1 rounded-lg" />
            ) : (
              <div className="min-h-0 flex-1">
                <SeatMap
                  readonly={!!confirmation}
                  seats={location.seats ?? []}
                  seatStatuses={seatStatuses}
                  markers={location.markers ?? []}
                  areas={location.areas ?? []}
                  selectedSeats={confirmation ? [] : selectedSeats}
                  highlightedSeatId={confirmation ? null : highlightedSeatId}
                  onSeatSelect={confirmation ? () => {} : handleSeatToggle}
                  allowCancelledSelection={true}
                />
              </div>
            )}
          </div>

          {/* Action Panel / Confirmation - Desktop */}
          {!isMobile && (
            <div className="space-y-3">
              {confirmation ? (
                <BoxOfficeConfirmation
                  result={confirmation.result}
                  guestEmail={confirmation.guestEmail}
                  notifiedUsername={confirmation.notifiedUsername}
                  onCreateAnother={handleCreateAnother}
                />
              ) : (
                <BoxOfficeActionPanel
                  reserveMode={reserveMode}
                  onReserveModeChange={setReserveMode}
                  users={isLoadingUsers ? [] : users}
                  userId={userId}
                  onUserIdChange={setUserId}
                  deductAllowance={deductAllowance}
                  onDeductAllowanceChange={setDeductAllowance}
                  guestName={guestName}
                  onGuestNameChange={setGuestName}
                  guestEmail={guestEmail}
                  onGuestEmailChange={setGuestEmail}
                  checkedIn={checkedIn}
                  onCheckedInChange={setCheckedIn}
                  selectedSeats={selectedSeats}
                  highlightedSeatId={highlightedSeatId}
                  onSeatChipClick={handleSeatChipClick}
                  onSeatRemove={handleSeatToggle}
                  isSubmitting={isSubmitting}
                  onSubmit={handleSubmit}
                />
              )}
            </div>
          )}
        </div>
      )}

      {/* Box Office Drawer - Mobile */}
      {isMobile && (
        <Drawer open={isDrawerOpen} onOpenChange={setIsDrawerOpen}>
          <DrawerContent>
            <DrawerHeader>
              <DrawerTitle>{drawerTitle}</DrawerTitle>
            </DrawerHeader>
            <div className="px-4 pb-4 max-h-[80vh] overflow-y-auto">
              {confirmation ? (
                <BoxOfficeConfirmation
                  className="border-0 p-0 sm:p-0"
                  result={confirmation.result}
                  guestEmail={confirmation.guestEmail}
                  notifiedUsername={confirmation.notifiedUsername}
                  onCreateAnother={handleCreateAnother}
                />
              ) : (
                <BoxOfficeActionPanel
                  className="border-0 p-0 sm:p-0"
                  reserveMode={reserveMode}
                  onReserveModeChange={setReserveMode}
                  users={isLoadingUsers ? [] : users}
                  userId={userId}
                  onUserIdChange={setUserId}
                  deductAllowance={deductAllowance}
                  onDeductAllowanceChange={setDeductAllowance}
                  guestName={guestName}
                  onGuestNameChange={setGuestName}
                  guestEmail={guestEmail}
                  onGuestEmailChange={setGuestEmail}
                  checkedIn={checkedIn}
                  onCheckedInChange={setCheckedIn}
                  selectedSeats={selectedSeats}
                  highlightedSeatId={highlightedSeatId}
                  onSeatChipClick={handleSeatChipClick}
                  onSeatRemove={handleSeatToggle}
                  isSubmitting={isSubmitting}
                  onSubmit={handleSubmit}
                />
              )}
            </div>
          </DrawerContent>
        </Drawer>
      )}

      {/* Drawer Trigger - Mobile */}
      {isMobile &&
        selectedEventId &&
        isDeadlinePassed &&
        !isInitialLoading &&
        !isDrawerOpen && (
          <div
            className="fixed bottom-0 left-0 right-0 bg-background border-t p-2 flex items-center justify-center gap-2 cursor-pointer shadow-lg z-10"
            onClick={() => setIsDrawerOpen(true)}
          >
            <ChevronUp className="h-6 w-6 text-muted-foreground" />
            <span className="text-sm text-muted-foreground">{drawerTitle}</span>
          </div>
        )}
    </div>
  );
}

export default function BoxOfficePage() {
  return (
    <Suspense fallback={null}>
      <BoxOfficePageContent />
    </Suspense>
  );
}
