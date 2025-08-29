"use client";

import React, { useState, useEffect } from "react";
import { useParams, useRouter } from "next/navigation";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/hooks/use-auth";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useT } from "@/lib/i18n/hooks";
import { RefreshCw, User } from "lucide-react";

export function EmailVerificationPrompt() {
  const t = useT();
  const params = useParams();
  const locale = params.locale as string;

  const { user, isLoggedIn, isLoading } = useAuth();
  const router = useRouter();
  const [showPopup, setShowPopup] = useState(false);
  const currentpath = usePathname();

  const [timerCompleted, setTimerCompleted] = useState(false);

  useEffect(() => {
    const timer = setTimeout(() => {
      setTimerCompleted(true);
    }, 500);

    return () => clearTimeout(timer);
  }, []);

  useEffect(() => {
    if (
      timerCompleted &&
      !isLoading &&
      isLoggedIn &&
      user &&
      (!user.emailVerified || !user.email) &&
      currentpath.includes("profile") === false
    ) {
      setShowPopup(true);
    } else {
      setShowPopup(false);
    }
  }, [user, isLoggedIn, isLoading, currentpath, timerCompleted]);

  const handleGoToProfile = () => {
    setShowPopup(false);
    router.push(`/${locale}/profile`);
  };

  if (!showPopup) {
    return null;
  }

  return (
    <Dialog open={showPopup} onOpenChange={setShowPopup}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{t("emailVerificationPrompt.title")}</DialogTitle>
          <DialogDescription>
            {user?.email ? (
              <>
                {t("emailVerificationPrompt.emailSentTo")}
                <span className="font-semibold">{user.email}</span>
                {t("emailVerificationPrompt.clickLinkToConfirm")}
                <br />
                {t("emailVerificationPrompt.reloadInfo")}
                <br />
                <br />
                {t("emailVerificationPrompt.ifEmailIncorrect")}
                <Link
                  href="/profile"
                  className="text-primary hover:underline"
                  onClick={() => setShowPopup(false)}
                >
                  {t("emailVerificationPrompt.profilePageLink")}
                </Link>{" "}
                {t("emailVerificationPrompt.changeIt")}
              </>
            ) : (
              <>{t("emailVerificationPrompt.noEmailRegistered")}</>
            )}
            {user?.emailVerified && (
              <p className="text-sm text-gray-500 mt-4">
                {t("emailVerificationPrompt.emailAlreadyVerifiedInfo")}
              </p>
            )}
          </DialogDescription>
        </DialogHeader>
        <DialogFooter className="flex flex-row sm:justify-end sm:space-x-2 sm:space-y-0">
          <Button
            onClick={() => window.location.reload()}
            className="w-full sm:w-auto"
            variant="outline-solid"
          >
            <RefreshCw className="mr-2 h-4 w-4" />
            {t("emailVerificationPrompt.reloadPageButton")}
          </Button>
          <Button onClick={handleGoToProfile} className="w-full sm:w-auto">
            <User className="mr-2 h-4 w-4" />
            {t("emailVerificationPrompt.goToProfileButton")}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
