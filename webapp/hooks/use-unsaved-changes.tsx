import { useRouter } from "next/navigation";
import React, {
  createContext,
  useContext,
  useState,
  useCallback,
  ReactNode,
  useMemo,
} from "react";

type SaveHandler = () => Promise<void> | void;

interface UnsavedChangesContextType {
  hasUnsavedChanges: boolean;
  showUnsavedDialog: boolean;
  setHasUnsavedChanges: (hasChanges: boolean) => void;
  setShowUnsavedDialog: (show: boolean) => void;
  setPendingNavigation: (url: string | null) => void;
  registerSaveHandler: (handler: SaveHandler | null) => void;
  handleDiscardChanges: () => void;
  handleSaveAndNavigate: () => void;
}

const UnsavedChangesContext = createContext<
  UnsavedChangesContextType | undefined
>(undefined);

export const UnsavedChangesProvider = ({
  children,
}: {
  children: ReactNode;
}) => {
  const [hasUnsavedChanges, setHasUnsavedChanges] = useState(false);
  const [showUnsavedDialog, setShowUnsavedDialog] = useState(false);
  const [pendingNavigation, setPendingNavigation] = useState<string | null>(
    null,
  );
  const [saveHandler, setSaveHandler] = useState<SaveHandler | null>(null);
  const router = useRouter();

  const registerSaveHandler = useCallback((handler: SaveHandler | null) => {
    // Wrap in a thunk - useState's setter calls function values as updaters
    // otherwise, instead of storing `handler` itself.
    setSaveHandler(handler === null ? null : () => handler);
  }, []);

  const handleDiscardChanges = useCallback(() => {
    if (pendingNavigation) {
      router.push(pendingNavigation);
    }
    setHasUnsavedChanges(false);
    setShowUnsavedDialog(false);
    setPendingNavigation(null);
  }, [
    pendingNavigation,
    router,
    setHasUnsavedChanges,
    setShowUnsavedDialog,
    setPendingNavigation,
  ]);

  const handleSaveAndNavigate = useCallback(async () => {
    try {
      if (saveHandler) {
        await saveHandler();
      } else {
        // Fallback for pages (e.g. Profile) that haven't registered a
        // handler - submit their form directly.
        const form = document.querySelector("form") as HTMLFormElement;
        form?.requestSubmit();
        await new Promise((resolve) => setTimeout(resolve, 500));
      }
    } catch {
      // Save failed - keep the dialog open and the changes marked unsaved
      // instead of navigating away and silently losing them.
      return;
    }
    if (pendingNavigation) {
      router.push(pendingNavigation);
    }
    setHasUnsavedChanges(false);
    setShowUnsavedDialog(false);
    setPendingNavigation(null);
  }, [
    saveHandler,
    pendingNavigation,
    router,
    setHasUnsavedChanges,
    setShowUnsavedDialog,
    setPendingNavigation,
  ]);

  const value = useMemo(
    () => ({
      hasUnsavedChanges,
      showUnsavedDialog,
      setHasUnsavedChanges,
      setShowUnsavedDialog,
      setPendingNavigation,
      registerSaveHandler,
      handleDiscardChanges,
      handleSaveAndNavigate,
    }),
    [
      hasUnsavedChanges,
      showUnsavedDialog,
      setHasUnsavedChanges,
      setShowUnsavedDialog,
      setPendingNavigation,
      registerSaveHandler,
      handleDiscardChanges,
      handleSaveAndNavigate,
    ],
  );

  return (
    <UnsavedChangesContext.Provider value={value}>
      {children}
    </UnsavedChangesContext.Provider>
  );
};

export function useUnsavedChanges() {
  const context = useContext(UnsavedChangesContext);
  if (!context) {
    throw new Error(
      "useUnsavedChanges must be used within a UnsavedChangesProvider",
    );
  }
  return context;
}
