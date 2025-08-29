"use client";

import { createContext, useContext, useState, ReactNode } from "react";

interface LoginRequiredPopupContextType {
  isOpen: boolean;
  setIsOpen: (isOpen: boolean) => void;
  triggerLoginRequired: () => void;
}

const LoginRequiredPopupContext = createContext<
  LoginRequiredPopupContextType | undefined
>(undefined);

export function LoginRequiredPopupProvider({
  children,
}: {
  children: ReactNode;
}) {
  const [isOpen, setIsOpen] = useState(false);

  const triggerLoginRequired = () => {
    setIsOpen(true);
  };

  return (
    <LoginRequiredPopupContext.Provider
      value={{ isOpen, setIsOpen, triggerLoginRequired }}
    >
      {children}
    </LoginRequiredPopupContext.Provider>
  );
}

export function useLoginRequiredPopup() {
  const context = useContext(LoginRequiredPopupContext);
  if (context === undefined) {
    throw new Error(
      "useLoginRequiredPopup must be used within a LoginRequiredPopupProvider",
    );
  }
  return context;
}
