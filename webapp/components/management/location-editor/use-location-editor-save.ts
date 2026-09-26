"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";

import { useT } from "@/lib/i18n/hooks";
import type { ErrorWithResponse } from "@/components/init-query-client";
import type {
  EventLocationResponseDto,
  EventLocationUpdateDto,
  LayoutOperationDto,
} from "@/api";
import {
  postApiManagerEventlocationsByIdLayoutOperationsMutation,
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

  const [isSaving, setIsSaving] = useState(false);

  const layoutOperations = useMutation({
    ...postApiManagerEventlocationsByIdLayoutOperationsMutation(),
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

  // ---- saveAll: the only place that talks to the network ----

  const buildOperations = useCallback(
    (
      s: LocationEditorState,
    ): {
      operations: LayoutOperationDto[];
      // Per operation: which local entity it belongs to (absent for
      // location/delete operations), used to reconcile the response.
      targets: ({ kind: EntityKind; localId: LocalId } | undefined)[];
    } => {
      const eventLocationId = s.meta.serverId;
      const operations: LayoutOperationDto[] = [];
      const targets: ({ kind: EntityKind; localId: LocalId } | undefined)[] =
        [];
      const push = (
        op: LayoutOperationDto,
        target?: { kind: EntityKind; localId: LocalId },
      ) => {
        operations.push(op);
        targets.push(target);
      };

      if (s.metaDirty) {
        push({
          entity: "LOCATION",
          action: "UPDATE",
          id: eventLocationId,
          data: {
            name: s.meta.name,
            address: s.meta.address,
            managerIds: s.meta.managerIds,
          },
        });
      }

      // The server applies operations strictly in this order: whatever a
      // later operation refers to (new entrance/area) is created before it,
      // and deletions come last so nothing still references what is removed.
      for (const e of s.entrances.filter((x) => x.syncState !== "synced")) {
        push(
          {
            entity: "ENTRANCE",
            action: e.serverId ? "UPDATE" : "CREATE",
            id: e.serverId,
            ref: e.localId,
            data: { name: e.name, eventLocationId },
          },
          { kind: "entrance", localId: e.localId },
        );
      }
      for (const a of s.areas.filter((x) => x.syncState !== "synced")) {
        push(
          {
            entity: "AREA",
            action: a.serverId ? "UPDATE" : "CREATE",
            id: a.serverId,
            ref: a.localId,
            data: {
              name: a.name,
              boundary: a.boundary.map((p) => ({
                xCoordinate: p.x,
                yCoordinate: p.y,
              })),
              eventLocationId,
            },
          },
          { kind: "area", localId: a.localId },
        );
      }
      for (const seat of s.seats.filter((x) => x.syncState !== "synced")) {
        const area = s.areas.find((a) => a.localId === seat.areaRef);
        const entrance = s.entrances.find(
          (e) => e.localId === seat.entranceRef,
        );
        push(
          {
            entity: "SEAT",
            action: seat.serverId ? "UPDATE" : "CREATE",
            id: seat.serverId,
            ref: seat.localId,
            areaRef: area && !area.serverId ? area.localId : undefined,
            entranceRef:
              entrance && !entrance.serverId ? entrance.localId : undefined,
            data: {
              seatNumber: seat.seatNumber,
              seatRow: seat.seatRow,
              eventLocationId,
              coordinate: { xCoordinate: seat.x, yCoordinate: seat.y },
              areaId: area?.serverId,
              entranceId: entrance?.serverId,
            },
          },
          { kind: "seat", localId: seat.localId },
        );
      }
      for (const m of s.markers.filter((x) => x.syncState !== "synced")) {
        push(
          {
            entity: "MARKER",
            action: m.serverId ? "UPDATE" : "CREATE",
            id: m.serverId,
            ref: m.localId,
            data: {
              label: m.label,
              coordinate: { xCoordinate: m.x, yCoordinate: m.y },
              eventLocationId,
            },
          },
          { kind: "marker", localId: m.localId },
        );
      }

      const deletions = s.pendingDeletions;
      for (const id of deletions.seat)
        push({ entity: "SEAT", action: "DELETE", id });
      for (const id of deletions.marker)
        push({ entity: "MARKER", action: "DELETE", id });
      for (const id of deletions.area)
        push({ entity: "AREA", action: "DELETE", id });
      for (const id of deletions.entrance)
        push({ entity: "ENTRANCE", action: "DELETE", id });

      return { operations, targets };
    },
    [],
  );

  const saveAll = useCallback(async () => {
    setIsSaving(true);
    try {
      const current = stateRef.current;
      const { operations, targets } = buildOperations(current);

      if (operations.length > 0) {
        // One request, one transaction: on failure nothing was changed, so
        // every entity stays dirty and the save can simply be retried.
        let response;
        try {
          response = await layoutOperations.mutateAsync({
            path: { id: current.meta.serverId },
            body: { operations },
          });
        } catch (err) {
          const error = err as ErrorWithResponse;
          toast.error(t("management.locationEditor.saveFailed"), {
            description:
              error?.response?.description || t("common.error.default"),
          });
          return;
        }

        targets.forEach((target, i) => {
          if (!target) return;
          const created = operations[i].action === "CREATE";
          const serverId = response.results?.[i]?.id;
          if (created && serverId) {
            dispatch({
              type: "RECONCILE",
              kind: target.kind,
              localId: target.localId,
              serverId,
            });
          } else {
            dispatch({
              type: "SET_SYNC_STATE",
              kind: target.kind,
              localId: target.localId,
              syncState: "synced",
            });
          }
        });
        invalidateAll();
      }

      dispatch({ type: "SAVE_SUCCESS" });
      toast.success(t("management.locationEditor.saveSuccess"));
    } finally {
      setIsSaving(false);
    }
  }, [buildOperations, layoutOperations, invalidateAll, dispatch, t]);

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
