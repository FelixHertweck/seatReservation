"use client";

import { useState, useEffect, useMemo } from "react";
import { usePathname } from "next/navigation";
import { useAuth } from "@/hooks/use-auth";
import { EmailRequiredDialog } from "@/components/common/email-required-dialog";

/** Auto-shows EmailRequiredDialog shortly after login for accounts with an unverified email. */
export function EmailVerificationPrompt() {
  const { user, isLoggedIn, isLoading } = useAuth();
  const currentpath = usePathname();

  const [isOpen, setIsOpen] = useState(false);
  const [timerCompleted, setTimerCompleted] = useState(false);

  useEffect(() => {
    const timer = setTimeout(() => {
      setTimerCompleted(true);
    }, 500);

    return () => clearTimeout(timer);
  }, []);

  // Derive showPopup directly from dependencies instead of using setState in useEffect
  const showPopup = useMemo(() => {
    return (
      timerCompleted &&
      !isLoading &&
      isLoggedIn &&
      user !== null &&
      user !== undefined &&
      // A verified account with no email was deliberately set up that way by an
      // admin (e.g. a shared box-office/supervisor login) and shouldn't be nagged.
      !user.emailVerified &&
      !currentpath.includes("profile")
    );
  }, [timerCompleted, isLoading, isLoggedIn, user, currentpath]);

  // Sync the dialog open state with the computed showPopup value
  useEffect(() => {
    setIsOpen(showPopup);
  }, [showPopup]);

  if (!showPopup) {
    return null;
  }

  return <EmailRequiredDialog open={isOpen} onOpenChange={setIsOpen} />;
}
