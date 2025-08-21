"use client";

import React, {
  createContext,
  useContext,
  useState,
  type ReactNode,
  useEffect,
} from "react";
import { setToastsDisabled } from "@/hooks/use-toast";

interface ToasterContextType {
  toasterDisabled: boolean;
  disableToaster: () => void;
  enableToaster: () => void;
}

const ToasterContext = createContext<ToasterContextType | undefined>(undefined);

export function ToasterProvider({ children }: { children: ReactNode }) {
  const [toasterDisabled, setToasterDisabled] = useState(false);

  useEffect(() => {
    setToastsDisabled(toasterDisabled);
  }, [toasterDisabled]);

  const disableToaster = () => setToasterDisabled(true);
  const enableToaster = () => setToasterDisabled(false);

  return (
    <ToasterContext.Provider
      value={{ toasterDisabled, disableToaster, enableToaster }}
    >
      {children}
    </ToasterContext.Provider>
  );
}

export function useToasterControl() {
  const context = useContext(ToasterContext);
  if (context === undefined) {
    throw new Error("useToasterControl must be used within a ToasterProvider");
  }
  return context;
}
