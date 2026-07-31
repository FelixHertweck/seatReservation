"use client";

import { useCallback, useEffect, useMemo, useRef } from "react";
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

interface UseLocationAutosaveArgs {
  state: LocationEditorState;
  dispatch: (action: EditorAction) => void;
}

export function useLocationAutosave({
  state,
  dispatch,
}: UseLocationAutosaveArgs) {
  const t = useT();
  const queryClient = useQueryClient();

  // Always reflects the latest state - read from inside the deferred queued
  // tasks below (which run after the triggering dispatch has re-rendered).
  const stateRef = useRef(state);
  useEffect(() => {
    stateRef.current = state;
  }, [state]);

  const eventLocationId = state.meta.serverId;

  // Per-entity FIFO queue: rapid successive edits to the SAME entity (e.g.
  // two quick drags before the first PUT resolves) can't race and land out
  // of order. Edits to different entities run concurrently.
  const queues = useRef(new Map<LocalId, Promise<unknown>>());
  const enqueue = useCallback((localId: LocalId, task: () => Promise<void>) => {
    const previous = queues.current.get(localId) ?? Promise.resolve();
    const next = previous.then(task, task);
    queues.current.set(localId, next);
    return next;
  }, []);

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

  const invalidate = useCallback(
    (kind: EntityKind) => {
      const key = {
        seat: getApiManagerSeatsQueryKey(),
        marker: getApiManagerMarkersQueryKey(),
        area: getApiManagerAreasQueryKey(),
        entrance: getApiManagerEntrancesQueryKey(),
      }[kind];
      queryClient.invalidateQueries({ queryKey: key });
    },
    [queryClient],
  );

  const fail = useCallback(
    (kind: EntityKind, localId: LocalId, wasNew: boolean, err: unknown) => {
      const error = err as ErrorWithResponse;
      if (wasNew) {
        dispatch({ type: "REMOVE_LOCAL", kind, localId });
      } else {
        dispatch({
          type: "SET_SYNC_STATE",
          kind,
          localId,
          syncState: "error",
        });
      }
      toast.error(t("management.locationEditor.autosaveFailed"), {
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

  // ---- seats ----

  const saveSeat = useCallback(
    (localId: LocalId, wasNew: boolean) =>
      enqueue(localId, async () => {
        const seat = stateRef.current.seats.find((s) => s.localId === localId);
        if (!seat) return;
        try {
          if (wasNew || !seat.serverId) {
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
          invalidate("seat");
        } catch (err) {
          fail("seat", localId, wasNew || !seat.serverId, err);
        }
      }),
    [enqueue, seatCreate, seatUpdate, seatBody, dispatch, fail, invalidate],
  );

  const addSeat = useCallback(
    (seat: Omit<EditorSeat, "localId" | "syncState">) => {
      const localId = nextTmpId();
      dispatch({ type: "ADD_SEAT", seat: { ...seat, localId } });
      void saveSeat(localId, true);
      return localId;
    },
    [dispatch, saveSeat],
  );

  const addSeatsBulk = useCallback(
    (seats: Omit<EditorSeat, "localId" | "syncState">[]) => {
      const withIds = seats.map((seat) => ({ ...seat, localId: nextTmpId() }));
      dispatch({ type: "ADD_SEATS_BULK", seats: withIds });
      withIds.forEach((seat) => void saveSeat(seat.localId, true));
      return withIds.map((s) => s.localId);
    },
    [dispatch, saveSeat],
  );

  const updateSeat = useCallback(
    (
      localId: LocalId,
      changes: Partial<Omit<EditorSeat, "localId" | "syncState">>,
    ) => {
      dispatch({ type: "UPDATE_SEAT", localId, changes });
      void saveSeat(localId, false);
    },
    [dispatch, saveSeat],
  );

  // ---- markers ----

  const markerBody = useCallback(
    (marker: Pick<EditorMarker, "label" | "x" | "y">) => ({
      label: marker.label,
      coordinate: { xCoordinate: marker.x, yCoordinate: marker.y },
      eventLocationId,
    }),
    [eventLocationId],
  );

  const saveMarker = useCallback(
    (localId: LocalId, wasNew: boolean) =>
      enqueue(localId, async () => {
        const marker = stateRef.current.markers.find(
          (m) => m.localId === localId,
        );
        if (!marker) return;
        try {
          if (wasNew || !marker.serverId) {
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
          invalidate("marker");
        } catch (err) {
          fail("marker", localId, wasNew || !marker.serverId, err);
        }
      }),
    [
      enqueue,
      markerCreate,
      markerUpdate,
      markerBody,
      dispatch,
      fail,
      invalidate,
    ],
  );

  const addMarker = useCallback(
    (marker: Omit<EditorMarker, "localId" | "syncState">) => {
      const localId = nextTmpId();
      dispatch({ type: "ADD_MARKER", marker: { ...marker, localId } });
      void saveMarker(localId, true);
      return localId;
    },
    [dispatch, saveMarker],
  );

  const updateMarker = useCallback(
    (
      localId: LocalId,
      changes: Partial<Omit<EditorMarker, "localId" | "syncState">>,
    ) => {
      dispatch({ type: "UPDATE_MARKER", localId, changes });
      void saveMarker(localId, false);
    },
    [dispatch, saveMarker],
  );

  // ---- move (seats + markers together) ----

  const moveEntities = useCallback(
    (ids: Set<LocalId>, dx: number, dy: number) => {
      dispatch({ type: "MOVE_ENTITIES", ids, dx, dy });
      ids.forEach((id) => {
        if (stateRef.current.seats.some((s) => s.localId === id)) {
          void saveSeat(id, false);
        } else if (stateRef.current.markers.some((m) => m.localId === id)) {
          void saveMarker(id, false);
        }
      });
    },
    [dispatch, saveSeat, saveMarker],
  );

  // ---- delete (seats + markers together) ----

  const deleteEntities = useCallback(
    (ids: Set<LocalId>) => {
      const seatIds = stateRef.current.seats
        .filter((s) => ids.has(s.localId) && s.serverId)
        .map((s) => s.serverId!);
      const markerIds = stateRef.current.markers
        .filter((m) => ids.has(m.localId) && m.serverId)
        .map((m) => m.serverId!);

      dispatch({ type: "DELETE_ENTITIES", ids });

      if (seatIds.length > 0) {
        seatDelete.mutateAsync({ query: { ids: seatIds } }).then(
          () => invalidate("seat"),
          (err) =>
            toast.error(t("management.locationEditor.autosaveFailed"), {
              description: (err as ErrorWithResponse)?.response?.description,
            }),
        );
      }
      if (markerIds.length > 0) {
        markerDelete.mutateAsync({ query: { ids: markerIds } }).then(
          () => invalidate("marker"),
          (err) =>
            toast.error(t("management.locationEditor.autosaveFailed"), {
              description: (err as ErrorWithResponse)?.response?.description,
            }),
        );
      }
    },
    [dispatch, seatDelete, markerDelete, invalidate, t],
  );

  // ---- areas ----

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

  const saveArea = useCallback(
    (localId: LocalId, wasNew: boolean) =>
      enqueue(localId, async () => {
        const area = stateRef.current.areas.find((a) => a.localId === localId);
        if (!area) return;
        try {
          if (wasNew || !area.serverId) {
            const created = await areaCreate.mutateAsync({
              body: areaBody(area),
            });
            dispatch({
              type: "RECONCILE",
              kind: "area",
              localId,
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
              localId,
              syncState: "synced",
            });
          }
          invalidate("area");
        } catch (err) {
          fail("area", localId, wasNew || !area.serverId, err);
        }
      }),
    [enqueue, areaCreate, areaUpdate, areaBody, dispatch, fail, invalidate],
  );

  const addArea = useCallback(
    (area: Omit<EditorArea, "localId" | "syncState">) => {
      const localId = nextTmpId();
      dispatch({ type: "ADD_AREA", area: { ...area, localId } });
      void saveArea(localId, true);
      return localId;
    },
    [dispatch, saveArea],
  );

  const renameArea = useCallback(
    (localId: LocalId, name: string) => {
      dispatch({ type: "UPDATE_AREA", localId, changes: { name } });
      void saveArea(localId, false);
    },
    [dispatch, saveArea],
  );

  const updateAreaBoundary = useCallback(
    (localId: LocalId, boundary: { x: number; y: number }[]) => {
      dispatch({ type: "UPDATE_AREA", localId, changes: { boundary } });
      void saveArea(localId, false);
    },
    [dispatch, saveArea],
  );

  const moveAreaPoint = useCallback(
    (areaLocalId: LocalId, index: number, x: number, y: number) => {
      dispatch({ type: "MOVE_AREA_POINT", areaLocalId, index, x, y });
      void saveArea(areaLocalId, false);
    },
    [dispatch, saveArea],
  );

  const insertAreaPoint = useCallback(
    (areaLocalId: LocalId, index: number, x: number, y: number) => {
      dispatch({ type: "INSERT_AREA_POINT", areaLocalId, index, x, y });
      void saveArea(areaLocalId, false);
    },
    [dispatch, saveArea],
  );

  const deleteAreaPoint = useCallback(
    (areaLocalId: LocalId, index: number) => {
      dispatch({ type: "DELETE_AREA_POINT", areaLocalId, index });
      void saveArea(areaLocalId, false);
    },
    [dispatch, saveArea],
  );

  const deleteAreas = useCallback(
    (ids: Set<LocalId>) => {
      const serverIds = stateRef.current.areas
        .filter((a) => ids.has(a.localId) && a.serverId)
        .map((a) => a.serverId!);
      dispatch({ type: "DELETE_AREAS", ids });
      if (serverIds.length > 0) {
        areaDelete.mutateAsync({ query: { ids: serverIds } }).then(
          () => invalidate("area"),
          (err) =>
            toast.error(t("management.locationEditor.autosaveFailed"), {
              description: (err as ErrorWithResponse)?.response?.description,
            }),
        );
      }
    },
    [dispatch, areaDelete, invalidate, t],
  );

  const assignAreaToSeats = useCallback(
    (seatIds: Set<LocalId>, areaRef: LocalId | undefined) => {
      dispatch({ type: "ASSIGN_AREA_TO_SEATS", seatIds, areaRef });
      seatIds.forEach((id) => void saveSeat(id, false));
    },
    [dispatch, saveSeat],
  );

  // ---- entrances ----

  const entranceBody = useCallback(
    (entrance: Pick<EditorEntrance, "name">) => ({
      name: entrance.name,
      eventLocationId,
    }),
    [eventLocationId],
  );

  const saveEntrance = useCallback(
    (localId: LocalId, wasNew: boolean) =>
      enqueue(localId, async () => {
        const entrance = stateRef.current.entrances.find(
          (e) => e.localId === localId,
        );
        if (!entrance) return;
        try {
          if (wasNew || !entrance.serverId) {
            const created = await entranceCreate.mutateAsync({
              body: entranceBody(entrance),
            });
            dispatch({
              type: "RECONCILE",
              kind: "entrance",
              localId,
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
              localId,
              syncState: "synced",
            });
          }
          invalidate("entrance");
        } catch (err) {
          fail("entrance", localId, wasNew || !entrance.serverId, err);
        }
      }),
    [
      enqueue,
      entranceCreate,
      entranceUpdate,
      entranceBody,
      dispatch,
      fail,
      invalidate,
    ],
  );

  const addEntrance = useCallback(
    (entrance: Omit<EditorEntrance, "localId" | "syncState">) => {
      const localId = nextTmpId();
      dispatch({
        type: "ADD_ENTRANCE",
        entrance: { ...entrance, localId },
      });
      void saveEntrance(localId, true);
      return localId;
    },
    [dispatch, saveEntrance],
  );

  const renameEntrance = useCallback(
    (localId: LocalId, name: string) => {
      dispatch({ type: "UPDATE_ENTRANCE", localId, name });
      void saveEntrance(localId, false);
    },
    [dispatch, saveEntrance],
  );

  const deleteEntrances = useCallback(
    (ids: Set<LocalId>) => {
      const serverIds = stateRef.current.entrances
        .filter((e) => ids.has(e.localId) && e.serverId)
        .map((e) => e.serverId!);
      dispatch({ type: "DELETE_ENTRANCES", ids });
      if (serverIds.length > 0) {
        entranceDelete.mutateAsync({ query: { ids: serverIds } }).then(
          () => invalidate("entrance"),
          (err) =>
            toast.error(t("management.locationEditor.autosaveFailed"), {
              description: (err as ErrorWithResponse)?.response?.description,
            }),
        );
      }
    },
    [dispatch, entranceDelete, invalidate, t],
  );

  // ---- meta ----

  const updateMeta = useCallback(
    (changes: Partial<Omit<LocationMeta, "serverId">>) => {
      dispatch({ type: "SET_META", meta: changes });
      const next = { ...stateRef.current.meta, ...changes };
      const body: EventLocationUpdateDto = {
        name: next.name,
        address: next.address,
        capacity: next.capacity,
      };
      const request = metaUpdate.mutateAsync({
        path: { id: next.serverId },
        body,
      });
      request.then(
        (data) => {
          queryClient.setQueriesData(
            { queryKey: getApiManagerEventlocationsQueryKey() },
            (oldData: EventLocationResponseDto[] | undefined) =>
              oldData
                ? oldData.map((location) =>
                    location.id === data.id ? data : location,
                  )
                : oldData,
          );
        },
        (err) => {
          toast.error(t("management.locationEditor.autosaveFailed"), {
            description: (err as ErrorWithResponse)?.response?.description,
          });
        },
      );
      return request;
    },
    [dispatch, metaUpdate, queryClient, t],
  );

  const retryFailed = useCallback(() => {
    stateRef.current.seats
      .filter((s) => s.syncState === "error")
      .forEach((s) => void saveSeat(s.localId, !s.serverId));
    stateRef.current.markers
      .filter((m) => m.syncState === "error")
      .forEach((m) => void saveMarker(m.localId, !m.serverId));
    stateRef.current.areas
      .filter((a) => a.syncState === "error")
      .forEach((a) => void saveArea(a.localId, !a.serverId));
    stateRef.current.entrances
      .filter((e) => e.syncState === "error")
      .forEach((e) => void saveEntrance(e.localId, !e.serverId));
  }, [saveSeat, saveMarker, saveArea, saveEntrance]);

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
      isSavingMeta: metaUpdate.isPending,
      retryFailed,
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
      metaUpdate.isPending,
      retryFailed,
    ],
  );
}
