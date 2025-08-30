import { useRouter } from "next/navigation";
import React, {
  createContext,
  useContext,
  useState,
  useCallback,
  ReactNode,
  useMemo,
} from "react";

interface ProfileUnsavedChangesContextType {
  hasUnsavedChanges: boolean;
  showUnsavedDialog: boolean;
  setHasUnsavedChanges: (hasChanges: boolean) => void;
  setShowUnsavedDialog: (show: boolean) => void;
  setPendingNavigation: (url: string | null) => void;
  handleDiscardChanges: () => void;
  handleSaveAndNavigate: () => void;
}

const ProfileUnsavedChangesContext = createContext<
  ProfileUnsavedChangesContextType | undefined
>(undefined);

export const ProfileUnsavedChangesProvider = ({
  children,
}: {
  children: ReactNode;
}) => {
  const [hasUnsavedChanges, setHasUnsavedChanges] = useState(false);
  const [showUnsavedDialog, setShowUnsavedDialog] = useState(false);
  const [pendingNavigation, setPendingNavigation] = useState<string | null>(
    null,
  );
  const router = useRouter();

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

  const handleSaveAndNavigate = useCallback(() => {
    // Trigger form submission on profile page
    const form = document.querySelector("form") as HTMLFormElement;
    if (form) {
      form.requestSubmit();
      // Wait a bit for the form to submit, then navigate
      setTimeout(() => {
        if (pendingNavigation) {
          router.push(pendingNavigation);
        }
        setHasUnsavedChanges(false); // Reset unsaved changes after saving
        setShowUnsavedDialog(false);
        setPendingNavigation(null);
      }, 500);
    }
  }, [
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
      handleDiscardChanges,
      handleSaveAndNavigate,
    }),
    [
      hasUnsavedChanges,
      showUnsavedDialog,
      setHasUnsavedChanges,
      setShowUnsavedDialog,
      setPendingNavigation,
      handleDiscardChanges,
      handleSaveAndNavigate,
    ],
  );

  return (
    <ProfileUnsavedChangesContext.Provider value={value}>
      {children}
    </ProfileUnsavedChangesContext.Provider>
  );
};

export function useProfileUnsavedChanges() {
  const context = useContext(ProfileUnsavedChangesContext);
  if (!context) {
    throw new Error(
      "useProfileUnsavedChanges must be used within a ProfileUnsavedChangesProvider",
    );
  }
  return context;
}
