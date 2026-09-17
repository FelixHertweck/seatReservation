"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";

import { useT } from "@/lib/i18n/hooks";
import type { ErrorWithResponse } from "@/components/init-query-client";
import type {
  EventLocationLayoutRequestDto,
  EventLocationResponseDto,
  EventLocationUpdateDto,
} from "@/api";
import {
  putApiManagerEventlocationsByIdLayoutMutation,
  putApiManagerEventlocationsByIdMutation,
  getApiManagerSeatsQueryKey,
  getApiManagerMarkersQueryKey,
  getApiManagerAreasQueryKey,
  getApiManagerEntrancesQueryKey,
  getApiManagerEventlocationsQueryKey,
} from "@/api/@tanstack/react-query.gen";
import type { EditorAction } from "@/components/management/location-editor/editor-reducer";
import {
  nextTmpId,
  type EditorArea,
  type EditorEntrance,
  type EditorMarker,
  type EditorSeat,
  type LocalId,
  type LocationEditorState,
  type LocationMeta,
} from "@/components/management/location-editor/types";

interface UseLocationEditorSaveArgs {
  state: LocationEditorState;
  dispatch: (action: EditorAction) => void;
}

/**
 * All mutators here only touch local (reducer) state - nothing is sent to
 * the server until saveAll() runs, so the caller controls exactly when
 * network requests fire (the toolbar's Save button).
 */
export function useLocationEditorSave({
  state,
  dispatch,
}: UseLocationEditorSaveArgs) {
  const t = useT();
  const queryClient = useQueryClient();

  const stateRef = useRef(state);
  useEffect(() => {
    stateRef.current = state;
  }, [state]);

  const eventLocationId = state.meta.serverId;
  const [isSaving, setIsSaving] = useState(false);

  const layoutMutation = useMutation({
    ...putApiManagerEventlocationsByIdLayoutMutation(),
  });
  const metaUpdate = useMutation({
    ...putApiManagerEventlocationsByIdMutation(),
  });

  const invalidateAll = useCallback(() => {
    queryClient.invalidateQueries({ queryKey: getApiManagerSeatsQueryKey() });
    queryClient.invalidateQueries({
      queryKey: getApiManagerMarkersQueryKey(),
    });
    queryClient.invalidateQueries({ queryKey: getApiManagerAreasQueryKey() });
    queryClient.invalidateQueries({
      queryKey: getApiManagerEntrancesQueryKey(),
    });
    queryClient.invalidateQueries({
      queryKey: getApiManagerEventlocationsQueryKey(),
    });
  }, [queryClient]);

  // ---- local-only mutators ----

  const addSeat = useCallback(
    (seat: Omit<EditorSeat, "localId" | "syncState">) => {
      const localId = nextTmpId();
      dispatch({ type: "ADD_SEAT", seat: { ...seat, localId } });
      return localId;
    },
    [dispatch],
  );

  const addSeatsBulk = useCallback(
    (seats: Omit<EditorSeat, "localId" | "syncState">[]) => {
      const withIds = seats.map((seat) => ({ ...seat, localId: nextTmpId() }));
      dispatch({ type: "ADD_SEATS_BULK", seats: withIds });
      return withIds.map((s) => s.localId);
    },
    [dispatch],
  );

  const updateSeat = useCallback(
    (
      localId: LocalId,
      changes: Partial<Omit<EditorSeat, "localId" | "syncState">>,
    ) => {
      dispatch({ type: "UPDATE_SEAT", localId, changes });
    },
    [dispatch],
  );

  const addMarker = useCallback(
    (marker: Omit<EditorMarker, "localId" | "syncState">) => {
      const localId = nextTmpId();
      dispatch({ type: "ADD_MARKER", marker: { ...marker, localId } });
      return localId;
    },
    [dispatch],
  );

  const updateMarker = useCallback(
    (
      localId: LocalId,
      changes: Partial<Omit<EditorMarker, "localId" | "syncState">>,
    ) => {
      dispatch({ type: "UPDATE_MARKER", localId, changes });
    },
    [dispatch],
  );

  const moveEntities = useCallback(
    (ids: Set<LocalId>, dx: number, dy: number) => {
      dispatch({ type: "MOVE_ENTITIES", ids, dx, dy });
    },
    [dispatch],
  );

  const deleteEntities = useCallback(
    (ids: Set<LocalId>) => {
      dispatch({ type: "DELETE_ENTITIES", ids });
    },
    [dispatch],
  );

  const addArea = useCallback(
    (area: Omit<EditorArea, "localId" | "syncState">) => {
      const localId = nextTmpId();
      dispatch({ type: "ADD_AREA", area: { ...area, localId } });
      return localId;
    },
    [dispatch],
  );

  const renameArea = useCallback(
    (localId: LocalId, name: string) => {
      dispatch({ type: "UPDATE_AREA", localId, changes: { name } });
    },
    [dispatch],
  );

  const updateAreaBoundary = useCallback(
    (localId: LocalId, boundary: { x: number; y: number }[]) => {
      dispatch({ type: "UPDATE_AREA", localId, changes: { boundary } });
    },
    [dispatch],
  );

  const moveAreaPoint = useCallback(
    (areaLocalId: LocalId, index: number, x: number, y: number) => {
      dispatch({ type: "MOVE_AREA_POINT", areaLocalId, index, x, y });
    },
    [dispatch],
  );

  const insertAreaPoint = useCallback(
    (areaLocalId: LocalId, index: number, x: number, y: number) => {
      dispatch({ type: "INSERT_AREA_POINT", areaLocalId, index, x, y });
    },
    [dispatch],
  );

  const deleteAreaPoint = useCallback(
    (areaLocalId: LocalId, index: number) => {
      dispatch({ type: "DELETE_AREA_POINT", areaLocalId, index });
    },
    [dispatch],
  );

  const deleteAreas = useCallback(
    (ids: Set<LocalId>) => {
      dispatch({ type: "DELETE_AREAS", ids });
    },
    [dispatch],
  );

  const assignAreaToSeats = useCallback(
    (seatIds: Set<LocalId>, areaRef: LocalId | undefined) => {
      dispatch({ type: "ASSIGN_AREA_TO_SEATS", seatIds, areaRef });
    },
    [dispatch],
  );

  const addEntrance = useCallback(
    (entrance: Omit<EditorEntrance, "localId" | "syncState">) => {
      const localId = nextTmpId();
      dispatch({ type: "ADD_ENTRANCE", entrance: { ...entrance, localId } });
      return localId;
    },
    [dispatch],
  );

  const renameEntrance = useCallback(
    (localId: LocalId, name: string) => {
      dispatch({ type: "UPDATE_ENTRANCE", localId, name });
    },
    [dispatch],
  );

  const deleteEntrances = useCallback(
    (ids: Set<LocalId>) => {
      dispatch({ type: "DELETE_ENTRANCES", ids });
    },
    [dispatch],
  );

  const updateMeta = useCallback(
    async (changes: Partial<Omit<LocationMeta, "serverId">>) => {
      const currentMeta = stateRef.current.meta;
      const updatedMeta = { ...currentMeta, ...changes };
      const body: EventLocationUpdateDto = {
        name: updatedMeta.name,
        address: updatedMeta.address,
      };
      try {
        const data = await metaUpdate.mutateAsync({
          path: { id: currentMeta.serverId },
          body,
        });
        dispatch({
          type: "SET_META",
          meta: {
            name: data.name ?? updatedMeta.name,
            address: data.address ?? updatedMeta.address,
          },
          dirty: false,
        });
        queryClient.setQueriesData(
          { queryKey: getApiManagerEventlocationsQueryKey() },
          (oldData: EventLocationResponseDto[] | undefined) =>
            oldData
              ? oldData.map((location) =>
                  location.id === data.id ? data : location,
                )
              : oldData,
        );
        toast.success(t("management.locations.updateSuccess"));
        return true;
      } catch (err) {
        const error = err as ErrorWithResponse;
        toast.error(t("management.locationEditor.saveFailed"), {
          description:
            error?.response?.description || t("common.error.default"),
        });
        return false;
      }
    },
    [metaUpdate, queryClient, dispatch, t],
  );

  // ---- saveAll: atomic layout synchronization via a single request ----

  const saveAll = useCallback(async () => {
    setIsSaving(true);
    try {
      const initial = stateRef.current;

      const layoutBody: EventLocationLayoutRequestDto = {
        name: initial.meta.name,
        address: initial.meta.address,
        managerIds: initial.meta.managerIds,
        entrances: initial.entrances
          .filter((e) => e.syncState !== "synced")
          .map((e) => ({
            id: e.serverId,
            tempId: !e.serverId ? e.localId : undefined,
            name: e.name,
          })),
        areas: initial.areas
          .filter((a) => a.syncState !== "synced")
          .map((a) => ({
            id: a.serverId,
            tempId: !a.serverId ? a.localId : undefined,
            name: a.name,
            boundary: a.boundary.map((p) => ({
              xCoordinate: p.x,
              yCoordinate: p.y,
            })),
          })),
        markers: initial.markers
          .filter((m) => m.syncState !== "synced")
          .map((m) => ({
            id: m.serverId,
            tempId: !m.serverId ? m.localId : undefined,
            label: m.label,
            coordinate: { xCoordinate: m.x, yCoordinate: m.y },
          })),
        seats: initial.seats
          .filter((s) => s.syncState !== "synced")
          .map((s) => {
            const entrance = s.entranceRef
              ? initial.entrances.find((e) => e.localId === s.entranceRef)
              : undefined;
            const area = s.areaRef
              ? initial.areas.find((a) => a.localId === s.areaRef)
              : undefined;
            return {
              id: s.serverId,
              tempId: !s.serverId ? s.localId : undefined,
              seatNumber: s.seatNumber,
              seatRow: s.seatRow,
              coordinate: { xCoordinate: s.x, yCoordinate: s.y },
              entranceId: entrance?.serverId,
              entranceTempId:
                entrance && !entrance.serverId ? entrance.localId : undefined,
              areaId: area?.serverId,
              areaTempId: area && !area.serverId ? area.localId : undefined,
            };
          }),
        deletedSeatIds: initial.pendingDeletions.seat,
        deletedMarkerIds: initial.pendingDeletions.marker,
        deletedAreaIds: initial.pendingDeletions.area,
        deletedEntranceIds: initial.pendingDeletions.entrance,
      };

      const response = await layoutMutation.mutateAsync({
        path: { id: eventLocationId },
        body: layoutBody,
      });

      if (response.createdEntranceIdMap) {
        for (const [tempId, serverId] of Object.entries(
          response.createdEntranceIdMap,
        )) {
          dispatch({
            type: "RECONCILE",
            kind: "entrance",
            localId: tempId,
            serverId,
          });
        }
      }
      if (response.createdAreaIdMap) {
        for (const [tempId, serverId] of Object.entries(
          response.createdAreaIdMap,
        )) {
          dispatch({
            type: "RECONCILE",
            kind: "area",
            localId: tempId,
            serverId,
          });
        }
      }
      if (response.createdMarkerIdMap) {
        for (const [tempId, serverId] of Object.entries(
          response.createdMarkerIdMap,
        )) {
          dispatch({
            type: "RECONCILE",
            kind: "marker",
            localId: tempId,
            serverId,
          });
        }
      }
      if (response.createdSeatIdMap) {
        for (const [tempId, serverId] of Object.entries(
          response.createdSeatIdMap,
        )) {
          dispatch({
            type: "RECONCILE",
            kind: "seat",
            localId: tempId,
            serverId,
          });
        }
      }

      if (response.id) {
        queryClient.setQueriesData(
          { queryKey: getApiManagerEventlocationsQueryKey() },
          (oldData: EventLocationResponseDto[] | undefined) =>
            oldData
              ? oldData.map((location) =>
                  location.id === response.id
                    ? {
                        ...location,
                        name: response.name ?? location.name,
                        address: response.address ?? location.address,
                      }
                    : location,
                )
              : oldData,
        );
      }

      dispatch({ type: "SAVE_SUCCESS" });
      invalidateAll();
      toast.success(t("management.locationEditor.saveSuccess"));
    } catch (err) {
      const error = err as ErrorWithResponse;
      toast.error(t("management.locationEditor.saveFailed"), {
        description: error?.response?.description || t("common.error.default"),
      });
    } finally {
      setIsSaving(false);
    }
  }, [
    eventLocationId,
    layoutMutation,
    queryClient,
    invalidateAll,
    dispatch,
    t,
  ]);

  return useMemo(
    () => ({
      addSeat,
      addSeatsBulk,
      updateSeat,
      addMarker,
      updateMarker,
      moveEntities,
      deleteEntities,
      addArea,
      renameArea,
      updateAreaBoundary,
      moveAreaPoint,
      insertAreaPoint,
      deleteAreaPoint,
      deleteAreas,
      assignAreaToSeats,
      addEntrance,
      renameEntrance,
      deleteEntrances,
      updateMeta,
      saveAll,
      isSaving,
    }),
    [
      addSeat,
      addSeatsBulk,
      updateSeat,
      addMarker,
      updateMarker,
      moveEntities,
      deleteEntities,
      addArea,
      renameArea,
      updateAreaBoundary,
      moveAreaPoint,
      insertAreaPoint,
      deleteAreaPoint,
      deleteAreas,
      assignAreaToSeats,
      addEntrance,
      renameEntrance,
      deleteEntrances,
      updateMeta,
      saveAll,
      isSaving,
    ],
  );
}
