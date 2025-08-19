"use client";

import React, { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
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
import { t } from "i18next";

export function EmailVerificationPrompt() {
  const { user, isLoggedIn, isLoading } = useAuth();
  const router = useRouter();
  const [showPopup, setShowPopup] = useState(false);
  const currentpath = usePathname();

  useEffect(() => {
    if (
      !isLoading &&
      isLoggedIn &&
      user &&
      (!user.emailVerified || !user.email) &&
      currentpath !== "/profile"
    ) {
      setShowPopup(true);
    } else {
      setShowPopup(false);
    }
  }, [user, isLoggedIn, isLoading, currentpath]);

  const handleGoToProfile = () => {
    setShowPopup(false);
    router.push("/profile");
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
          </DialogDescription>
        </DialogHeader>
        <DialogFooter>
          <Button onClick={handleGoToProfile}>
            {t("emailVerificationPrompt.goToProfileButton")}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
