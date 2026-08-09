"use client";

import { Suspense, useEffect, useMemo, useState } from "react";
import { ArrowUp, Clock, Loader2 } from "lucide-react";

import { useT } from "@/lib/i18n/hooks";
import { formatDateTime } from "@/lib/utils";
import { useSupervisorEvent } from "@/hooks/use-supervisor-event";
import { PageHeader } from "@/components/page-header";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/custom-ui/skeleton";
import EventSelector from "@/components/common/supervisor/event-selector";
import { SeatMap } from "@/components/common/seat-map";
import SeatmapLegend from "@/components/common/seatmap-legend";
import {
  BoxOfficeActionPanel,
  type BoxOfficeReserveMode,
} from "@/components/box-office/box-office-action-panel";
import { BoxOfficeConfirmation } from "@/components/box-office/box-office-confirmation";
import { useLiveView } from "@/hooks/use-liveview";
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

  const { events, isLoadingEvents } = useCheckin();
  const {
    users,
    isLoadingUsers,
    createForKnownUser,
    createForGuest,
    isSubmitting,
  } = useBoxOffice();

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
  // Derived from the plain REST event list rather than the live-view websocket's own
  // `event` field: that connection is itself gated behind the booking deadline on the
  // backend now, so relying on it here would be circular -- it would never populate
  // (and the reconnect budget would run out) before we already know the deadline passed.
  const isDeadlinePassed = useMemo(() => {
    if (now === null || !selectedEvent?.bookingDeadline) return false;
    return new Date(selectedEvent.bookingDeadline).getTime() < now;
  }, [selectedEvent, now]);

  // Only open the live-view websocket once the deadline has passed -- the backend
  // rejects the connection until then, so connecting earlier would just burn through
  // useWebSocket's limited reconnect attempts for nothing.
  const { isInitialLoading, location, reservations } = useLiveView(
    selectedEventId,
    !!selectedEventId && isDeadlinePassed,
  );

  const seatStatuses = useMemo(
    () => convertReservationsToStatuses(reservations),
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
  };

  return (
    <div className="container mx-auto p-4 sm:p-6">
      <PageHeader
        title={t("boxOffice.title")}
        description={t("boxOffice.pageDescription")}
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
        <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
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
                />
              </div>
            )}
          </div>

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
