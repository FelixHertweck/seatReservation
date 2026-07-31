"use client";

import { useMemo } from "react";

import { useT } from "@/lib/i18n/hooks";
import { SeatMap } from "@/components/common/seat-map";
import type { AreaDto, EventLocationMakerDto, SeatDto } from "@/api";
import type { LocationEditorState } from "@/components/management/location-editor/types";

interface PreviewViewProps {
  state: LocationEditorState;
}

// Renders the current editor state through the same read-only SeatMap used
// on the booking/liveview pages, so managers can see how the location will
// actually look before it goes live.
export function PreviewView({ state }: PreviewViewProps) {
  const t = useT();

  const { seats, markers, areas } = useMemo(() => {
    const seats: SeatDto[] = state.seats.map((seat) => ({
      id: seat.localId,
      seatNumber: seat.seatNumber,
      seatRow: seat.seatRow,
      coordinate: { xCoordinate: seat.x, yCoordinate: seat.y },
    }));

    const markers: EventLocationMakerDto[] = state.markers.map((marker) => ({
      id: marker.localId,
      label: marker.label,
      coordinate: { xCoordinate: marker.x, yCoordinate: marker.y },
    }));

    const areas: AreaDto[] = state.areas.map((area) => ({
      id: area.localId,
      name: area.name,
      seatIds: state.seats
        .filter((seat) => seat.areaRef === area.localId)
        .map((seat) => seat.localId),
      boundary: area.boundary.map((p) => ({
        xCoordinate: p.x,
        yCoordinate: p.y,
      })),
    }));

    return { seats, markers, areas };
  }, [state.seats, state.markers, state.areas]);

  if (seats.length === 0 && markers.length === 0) {
    return (
      <div className="flex h-full w-full items-center justify-center rounded-lg border bg-seatmap text-sm text-muted-foreground">
        {t("management.locationEditor.preview.empty")}
      </div>
    );
  }

  return (
    <div className="h-full w-full rounded-lg border bg-seatmap">
      <SeatMap
        seats={seats}
        seatStatuses={[]}
        markers={markers}
        areas={areas}
        selectedSeats={[]}
        onSeatSelect={() => {}}
        readonly
      />
    </div>
  );
}
