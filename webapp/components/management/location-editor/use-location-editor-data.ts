"use client";

import { useQueries, useQuery } from "@tanstack/react-query";

import {
  getApiManagerEventlocationsOptions,
  getApiManagerSeatsOptions,
  getApiManagerMarkersOptions,
  getApiManagerAreasOptions,
  getApiManagerEntrancesOptions,
} from "@/api/@tanstack/react-query.gen";
import {
  emptyPendingDeletions,
  type LocationEditorState,
} from "@/components/management/location-editor/types";

/**
 * Loads everything needed to hydrate the editor for an existing location.
 * No `GET /eventlocations/{id}` exists, so meta comes from the list query;
 * seats/markers/areas/entrances use their own endpoints since mutations
 * need an authoritative server id to PUT/DELETE against.
 */
export function useLocationEditorData(locationId: string) {
  const locationsQuery = useQuery({
    ...getApiManagerEventlocationsOptions(),
  });
  const location = locationsQuery.data?.find((l) => l.id === locationId);

  const [seatsQuery, markersQuery, areasQuery, entrancesQuery] = useQueries({
    queries: [
      {
        ...getApiManagerSeatsOptions({
          query: { eventLocationId: locationId },
        }),
        staleTime: Infinity,
      },
      {
        ...getApiManagerMarkersOptions({
          query: { eventLocationId: locationId },
        }),
        staleTime: Infinity,
      },
      {
        ...getApiManagerAreasOptions({
          query: { eventLocationId: locationId },
        }),
        staleTime: Infinity,
      },
      {
        ...getApiManagerEntrancesOptions({
          query: { eventLocationId: locationId },
        }),
        staleTime: Infinity,
      },
    ],
  });

  const isLoading =
    locationsQuery.isLoading ||
    seatsQuery.isLoading ||
    markersQuery.isLoading ||
    areasQuery.isLoading ||
    entrancesQuery.isLoading;

  const isError =
    locationsQuery.isError ||
    seatsQuery.isError ||
    markersQuery.isError ||
    areasQuery.isError ||
    entrancesQuery.isError;

  const isReady = !isLoading && !isError && !!location;
  const isNotFound = !isLoading && !isError && !location;

  const buildInitialState = (): LocationEditorState | null => {
    if (!location?.id) return null;

    return {
      meta: {
        serverId: location.id,
        name: location.name ?? "",
        address: location.address ?? "",
        managerIds: location.managerIds ?? [],
      },
      metaDirty: false,
      pendingDeletions: emptyPendingDeletions(),
      seats: (seatsQuery.data ?? []).map((seat) => ({
        localId: `seat-${seat.id ?? ""}`,
        serverId: seat.id,
        seatNumber: seat.seatNumber ?? "",
        seatRow: seat.seatRow ?? "",
        x: seat.coordinate?.xCoordinate ?? 1,
        y: seat.coordinate?.yCoordinate ?? 1,
        entranceRef: seat.entranceId
          ? `entrance-${seat.entranceId}`
          : undefined,
        areaRef: seat.areaId ? `area-${seat.areaId}` : undefined,
        syncState: "synced" as const,
      })),
      markers: (markersQuery.data ?? []).map((marker) => ({
        localId: `marker-${marker.id ?? ""}`,
        serverId: marker.id,
        label: marker.label ?? "",
        x: marker.coordinate?.xCoordinate ?? 1,
        y: marker.coordinate?.yCoordinate ?? 1,
        syncState: "synced" as const,
      })),
      areas: (areasQuery.data ?? []).map((area) => ({
        localId: `area-${area.id ?? ""}`,
        serverId: area.id,
        name: area.name ?? "",
        boundary: (area.boundary ?? [])
          .filter((p) => p.xCoordinate != null && p.yCoordinate != null)
          .map((p) => ({ x: p.xCoordinate!, y: p.yCoordinate! })),
        syncState: "synced" as const,
      })),
      entrances: (entrancesQuery.data ?? []).map((entrance) => ({
        localId: `entrance-${entrance.id ?? ""}`,
        serverId: entrance.id,
        name: entrance.name ?? "",
        syncState: "synced" as const,
      })),
    };
  };

  return {
    isLoading,
    isReady,
    isError,
    isNotFound,
    location,
    buildInitialState,
  };
}
