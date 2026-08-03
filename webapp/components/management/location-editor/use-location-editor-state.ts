"use client";

import { useCallback, useReducer, useState } from "react";

import {
  editorReducer,
  type EditorAction,
} from "@/components/management/location-editor/editor-reducer";
import type { LocationEditorState } from "@/components/management/location-editor/types";

const HISTORY_LIMIT = 50;

// System-driven bookkeeping actions (autosave lifecycle) are not undo steps -
// only actions a user actually chose to do are.
const NON_UNDOABLE = new Set<EditorAction["type"]>([
  "HYDRATE",
  "REPLACE_STATE",
  "SET_SYNC_STATE",
  "RECONCILE",
  "REMOVE_LOCAL",
  "SAVE_SUCCESS",
]);

export function useLocationEditorState(initial: LocationEditorState) {
  const [state, rawDispatch] = useReducer(editorReducer, initial);
  const [past, setPast] = useState<LocationEditorState[]>([]);
  const [future, setFuture] = useState<LocationEditorState[]>([]);

  const dispatch = useCallback(
    (action: EditorAction) => {
      if (!NON_UNDOABLE.has(action.type)) {
        setPast((p) => [...p.slice(-HISTORY_LIMIT + 1), state]);
        setFuture([]);
      }
      rawDispatch(action);
    },
    [state],
  );

  const hydrate = useCallback((next: LocationEditorState) => {
    setPast([]);
    setFuture([]);
    rawDispatch({ type: "HYDRATE", state: next });
  }, []);

  const undo = useCallback(() => {
    if (past.length === 0) return;
    const previous = past.at(-1)!;
    setPast(past.slice(0, -1));
    setFuture((f) => [...f, state]);
    rawDispatch({ type: "REPLACE_STATE", state: previous });
  }, [past, state]);

  const redo = useCallback(() => {
    if (future.length === 0) return;
    const next = future.at(-1)!;
    setFuture(future.slice(0, -1));
    setPast((p) => [...p, state]);
    rawDispatch({ type: "REPLACE_STATE", state: next });
  }, [future, state]);

  return {
    state,
    dispatch,
    hydrate,
    undo,
    redo,
    canUndo: past.length > 0,
    canRedo: future.length > 0,
  };
}
