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
import { BadgeCheck, MailCheck, RefreshCw, User } from "lucide-react";

export function EmailVerificationPrompt() {
  const t = useT();
  const params = useParams();
  const locale = params.locale as string;

  const { user, isLoggedIn, isLoading, resendConfirmation } = useAuth();
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

  const handleGoToVerify = () => {
    setShowPopup(false);
    router.push(`/${locale}/verify`);
  };

  const handleSendVerification = async () => {
    await resendConfirmation();
    setTimeout(() => {
      handleGoToVerify();
    }, 700);
  };

  if (!showPopup) {
    return null;
  }

  const DynamicDialogContent = () => {
    if (user?.emailVerified) {
      return (
        <p className="text-sm text-gray-500 mt-4">
          {t("emailVerificationPrompt.emailAlreadyVerifiedInfo")}
        </p>
      );
    }

    if (user?.email) {
      if (user?.emailVerificationSent) {
        // This case happens when the user already got a verification email
        return (
          <>
            {t("emailVerificationPrompt.emailSentTo")}
            <span className="font-semibold">{user?.email}</span>
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
        );
      } else {
        // This case happens when the user has an email but did not get a verification email yet
        return (
          <>
            {t("emailVerificationPrompt.emailRegistered1")}
            <span className="font-semibold">{user?.email}</span>
            {t("emailVerificationPrompt.emailRegistered2")}
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
        );
      }
    } else {
      return <>{t("emailVerificationPrompt.noEmailRegistered")}</>;
    }
  };

  const DynamicDialogFooter = () => {
    if (user?.emailVerified) {
      return (
        <Button
          onClick={() => window.location.reload()}
          className="w-full sm:w-auto"
          variant="outline-solid"
        >
          <RefreshCw className="mr-2 h-4 w-4" />
          {t("emailVerificationPrompt.reloadPageButton")}
        </Button>
      );
    }

    if (user?.email) {
      if (user?.emailVerificationSent) {
        return (
          <>
            <Button
              onClick={handleGoToProfile}
              className="w-full sm:w-auto"
              variant="outline-solid"
            >
              <User className="mr-2 h-4 w-4" />
              {t("emailVerificationPrompt.goToProfileButton")}
            </Button>
            <Button onClick={handleGoToVerify} className="w-full sm:w-auto">
              <BadgeCheck className="mr-2 h-4 w-4" />
              {t("emailVerificationPrompt.goToVerifyButton")}
            </Button>
          </>
        );
      } else {
        return (
          <>
            <Button
              onClick={handleGoToProfile}
              className="w-full sm:w-auto"
              variant="outline-solid"
            >
              <User className="mr-2 h-4 w-4" />
              {t("emailVerificationPrompt.goToProfileButton")}
            </Button>
            <Button
              onClick={handleSendVerification}
              className="w-full sm:w-auto"
            >
              <MailCheck className="mr-2 h-4 w-4" />
              {t("emailVerificationPrompt.sendVerificationMail")}
            </Button>
          </>
        );
      }
    } else {
      return (
        <Button onClick={handleGoToProfile} className="w-full sm:w-auto">
          <User className="mr-2 h-4 w-4" />
          {t("emailVerificationPrompt.goToProfileButton")}
        </Button>
      );
    }
  };

  return (
    <Dialog open={showPopup} onOpenChange={setShowPopup}>
      <DialogContent noX={true} onInteractOutside={(e) => e.preventDefault()}>
        <DialogHeader>
          <DialogTitle>{t("emailVerificationPrompt.title")}</DialogTitle>
          <DialogDescription>
            <DynamicDialogContent />
          </DialogDescription>
        </DialogHeader>
        <DialogFooter className="flex flex-col sm:flex-row gap-2 sm:justify-end sm:space-x-2">
          <DynamicDialogFooter />
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
