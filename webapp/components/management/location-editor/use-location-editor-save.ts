"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";

import { useT } from "@/lib/i18n/hooks";
import type { ErrorWithResponse } from "@/components/init-query-client";
import type { EventLocationResponseDto, EventLocationUpdateDto } from "@/api";
import {
  postApiManagerSeatsMutation,
  putApiManagerSeatsByIdMutation,
  deleteApiManagerSeatsMutation,
  postApiManagerMarkersMutation,
  putApiManagerMarkersByIdMutation,
  deleteApiManagerMarkersMutation,
  postApiManagerAreasMutation,
  putApiManagerAreasByIdMutation,
  deleteApiManagerAreasMutation,
  postApiManagerEntrancesMutation,
  putApiManagerEntrancesByIdMutation,
  deleteApiManagerEntrancesMutation,
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
  type EntityKind,
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

  // Always reflects the latest state - read from inside saveAll, which spans
  // multiple awaited phases and must see reconciled server ids from earlier
  // phases (e.g. a seat's new area needs the area's fresh serverId).
  const stateRef = useRef(state);
  useEffect(() => {
    stateRef.current = state;
  }, [state]);

  const eventLocationId = state.meta.serverId;
  const [isSaving, setIsSaving] = useState(false);

  const seatCreate = useMutation({ ...postApiManagerSeatsMutation() });
  const seatUpdate = useMutation({ ...putApiManagerSeatsByIdMutation() });
  const seatDelete = useMutation({ ...deleteApiManagerSeatsMutation() });
  const markerCreate = useMutation({ ...postApiManagerMarkersMutation() });
  const markerUpdate = useMutation({ ...putApiManagerMarkersByIdMutation() });
  const markerDelete = useMutation({ ...deleteApiManagerMarkersMutation() });
  const areaCreate = useMutation({ ...postApiManagerAreasMutation() });
  const areaUpdate = useMutation({ ...putApiManagerAreasByIdMutation() });
  const areaDelete = useMutation({ ...deleteApiManagerAreasMutation() });
  const entranceCreate = useMutation({ ...postApiManagerEntrancesMutation() });
  const entranceUpdate = useMutation({
    ...putApiManagerEntrancesByIdMutation(),
  });
  const entranceDelete = useMutation({
    ...deleteApiManagerEntrancesMutation(),
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
  }, [queryClient]);

  const markError = useCallback(
    (kind: EntityKind, localId: LocalId, err: unknown) => {
      const error = err as ErrorWithResponse;
      dispatch({ type: "SET_SYNC_STATE", kind, localId, syncState: "error" });
      toast.error(t("management.locationEditor.saveFailed"), {
        description: error?.response?.description,
      });
    },
    [dispatch, t],
  );

  const resolveEntranceId = useCallback(
    (ref: LocalId | undefined): string | undefined =>
      ref
        ? stateRef.current.entrances.find((e) => e.localId === ref)?.serverId
        : undefined,
    [],
  );
  const resolveAreaId = useCallback(
    (ref: LocalId | undefined): string | undefined =>
      ref
        ? stateRef.current.areas.find((a) => a.localId === ref)?.serverId
        : undefined,
    [],
  );

  const seatBody = useCallback(
    (
      seat: Pick<
        EditorSeat,
        "seatNumber" | "seatRow" | "x" | "y" | "entranceRef" | "areaRef"
      >,
    ) => ({
      seatNumber: seat.seatNumber,
      eventLocationId,
      coordinate: { xCoordinate: seat.x, yCoordinate: seat.y },
      seatRow: seat.seatRow,
      entranceId: resolveEntranceId(seat.entranceRef),
      areaId: resolveAreaId(seat.areaRef),
    }),
    [eventLocationId, resolveEntranceId, resolveAreaId],
  );

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
    (changes: Partial<Omit<LocationMeta, "serverId">>) => {
      dispatch({ type: "SET_META", meta: changes });
    },
    [dispatch],
  );

  // ---- saveAll: the only place that talks to the network ----

  const areaBody = useCallback(
    (area: Pick<EditorArea, "name" | "boundary">) => ({
      name: area.name,
      boundary: area.boundary.map((p) => ({
        xCoordinate: p.x,
        yCoordinate: p.y,
      })),
      eventLocationId,
    }),
    [eventLocationId],
  );

  const markerBody = useCallback(
    (marker: Pick<EditorMarker, "label" | "x" | "y">) => ({
      label: marker.label,
      coordinate: { xCoordinate: marker.x, yCoordinate: marker.y },
      eventLocationId,
    }),
    [eventLocationId],
  );

  const entranceBody = useCallback(
    (entrance: Pick<EditorEntrance, "name">) => ({
      name: entrance.name,
      eventLocationId,
    }),
    [eventLocationId],
  );

  const saveEntranceOnce = useCallback(
    async (entrance: EditorEntrance) => {
      try {
        if (!entrance.serverId) {
          const created = await entranceCreate.mutateAsync({
            body: entranceBody(entrance),
          });
          dispatch({
            type: "RECONCILE",
            kind: "entrance",
            localId: entrance.localId,
            serverId: created.id!,
          });
        } else {
          await entranceUpdate.mutateAsync({
            path: { id: entrance.serverId },
            body: entranceBody(entrance),
          });
          dispatch({
            type: "SET_SYNC_STATE",
            kind: "entrance",
            localId: entrance.localId,
            syncState: "synced",
          });
        }
        return true;
      } catch (err) {
        markError("entrance", entrance.localId, err);
        return false;
      }
    },
    [entranceCreate, entranceUpdate, entranceBody, dispatch, markError],
  );

  const saveAreaOnce = useCallback(
    async (area: EditorArea) => {
      try {
        if (!area.serverId) {
          const created = await areaCreate.mutateAsync({
            body: areaBody(area),
          });
          dispatch({
            type: "RECONCILE",
            kind: "area",
            localId: area.localId,
            serverId: created.id!,
          });
        } else {
          await areaUpdate.mutateAsync({
            path: { id: area.serverId },
            body: areaBody(area),
          });
          dispatch({
            type: "SET_SYNC_STATE",
            kind: "area",
            localId: area.localId,
            syncState: "synced",
          });
        }
        return true;
      } catch (err) {
        markError("area", area.localId, err);
        return false;
      }
    },
    [areaCreate, areaUpdate, areaBody, dispatch, markError],
  );

  const saveSeatOnce = useCallback(
    async (localId: LocalId) => {
      const seat = stateRef.current.seats.find((s) => s.localId === localId);
      if (!seat) return true;
      try {
        if (!seat.serverId) {
          const created = await seatCreate.mutateAsync({
            body: seatBody(seat),
          });
          dispatch({
            type: "RECONCILE",
            kind: "seat",
            localId,
            serverId: created.id!,
          });
        } else {
          await seatUpdate.mutateAsync({
            path: { id: seat.serverId },
            body: seatBody(seat),
          });
          dispatch({
            type: "SET_SYNC_STATE",
            kind: "seat",
            localId,
            syncState: "synced",
          });
        }
        return true;
      } catch (err) {
        markError("seat", localId, err);
        return false;
      }
    },
    [seatCreate, seatUpdate, seatBody, dispatch, markError],
  );

  const saveMarkerOnce = useCallback(
    async (localId: LocalId) => {
      const marker = stateRef.current.markers.find(
        (m) => m.localId === localId,
      );
      if (!marker) return true;
      try {
        if (!marker.serverId) {
          const created = await markerCreate.mutateAsync({
            body: markerBody(marker),
          });
          dispatch({
            type: "RECONCILE",
            kind: "marker",
            localId,
            serverId: created.id!,
          });
        } else {
          await markerUpdate.mutateAsync({
            path: { id: marker.serverId },
            body: markerBody(marker),
          });
          dispatch({
            type: "SET_SYNC_STATE",
            kind: "marker",
            localId,
            syncState: "synced",
          });
        }
        return true;
      } catch (err) {
        markError("marker", localId, err);
        return false;
      }
    },
    [markerCreate, markerUpdate, markerBody, dispatch, markError],
  );

  const deletePending = useCallback(
    async (
      mutation: {
        mutateAsync: (args: { query: { ids: string[] } }) => Promise<unknown>;
      },
      ids: string[],
    ): Promise<boolean> => {
      if (ids.length === 0) return true;
      try {
        await mutation.mutateAsync({ query: { ids } });
        return true;
      } catch (err) {
        const error = err as ErrorWithResponse;
        toast.error(t("management.locationEditor.saveFailed"), {
          description: error?.response?.description,
        });
        return false;
      }
    },
    [t],
  );

  const saveMetaOnce = useCallback(async () => {
    const meta = stateRef.current.meta;
    const body: EventLocationUpdateDto = {
      name: meta.name,
      address: meta.address,
      capacity: meta.capacity,
    };
    try {
      const data = await metaUpdate.mutateAsync({
        path: { id: meta.serverId },
        body,
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
      return true;
    } catch (err) {
      const error = err as ErrorWithResponse;
      toast.error(t("management.locationEditor.saveFailed"), {
        description: error?.response?.description,
      });
      return false;
    }
  }, [metaUpdate, queryClient, t]);

  const saveAll = useCallback(async () => {
    setIsSaving(true);
    try {
      const initial = stateRef.current;

      // Entrances and areas have no dependencies on other entity kinds and
      // must be reconciled before seats, which reference them by server id.
      const entranceResults = await Promise.all(
        initial.entrances
          .filter((e) => e.syncState !== "synced")
          .map(saveEntranceOnce),
      );
      const areaResults = await Promise.all(
        initial.areas.filter((a) => a.syncState !== "synced").map(saveAreaOnce),
      );

      const afterDeps = stateRef.current;
      const seatResults = await Promise.all(
        afterDeps.seats
          .filter((s) => s.syncState !== "synced")
          .map((s) => saveSeatOnce(s.localId)),
      );
      const markerResults = await Promise.all(
        afterDeps.markers
          .filter((m) => m.syncState !== "synced")
          .map((m) => saveMarkerOnce(m.localId)),
      );

      const deletionResults = await Promise.all([
        deletePending(seatDelete, initial.pendingDeletions.seat),
        deletePending(markerDelete, initial.pendingDeletions.marker),
        deletePending(areaDelete, initial.pendingDeletions.area),
        deletePending(entranceDelete, initial.pendingDeletions.entrance),
      ]);

      const metaOk = initial.metaDirty ? await saveMetaOnce() : true;

      invalidateAll();

      const allOk =
        metaOk &&
        [
          ...entranceResults,
          ...areaResults,
          ...seatResults,
          ...markerResults,
          ...deletionResults,
        ].every(Boolean);

      if (allOk) {
        dispatch({ type: "SAVE_SUCCESS" });
        toast.success(t("management.locationEditor.saveSuccess"));
      }
    } finally {
      setIsSaving(false);
    }
  }, [
    saveEntranceOnce,
    saveAreaOnce,
    saveSeatOnce,
    saveMarkerOnce,
    saveMetaOnce,
    deletePending,
    seatDelete,
    markerDelete,
    areaDelete,
    entranceDelete,
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
