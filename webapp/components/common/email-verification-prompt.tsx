"use client";

import { useState, useEffect, useMemo } from "react";
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

  const { user, isLoggedIn, isLoading } = useAuth();
  const currentpath = usePathname();

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
      (!user.emailVerified || !user.email) &&
      !currentpath.includes("profile")
    );
  }, [timerCompleted, isLoading, isLoggedIn, user, currentpath]);

  const [isOpen, setIsOpen] = useState(false);

  // Sync the dialog open state with the computed showPopup value
  useEffect(() => {
    setIsOpen(showPopup);
  }, [showPopup]);

  if (!showPopup) {
    return null;
  }

  return (
    <Dialog open={isOpen} onOpenChange={setIsOpen}>
      <DialogContent noX={true} onInteractOutside={(e) => e.preventDefault()}>
        <DialogHeader>
          <DialogTitle>{t("emailVerificationPrompt.title")}</DialogTitle>
          <DialogDescription>
            <DynamicDialogContent setShowPopup={setIsOpen} />
          </DialogDescription>
        </DialogHeader>
        <DialogFooter className="flex flex-col sm:flex-row gap-2 sm:justify-end sm:space-x-2">
          <DynamicDialogFooter setShowPopup={setIsOpen} />
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

function DynamicDialogContent({
  setShowPopup,
}: {
  setShowPopup: (show: boolean) => void;
}) {
  const { user } = useAuth();
  const t = useT();

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
}

function DynamicDialogFooter({
  setShowPopup,
}: {
  setShowPopup: (show: boolean) => void;
}) {
  const params = useParams();
  const locale = params.locale as string;

  const router = useRouter();
  const { user, resendConfirmation } = useAuth();

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

  if (user?.emailVerified) {
    return <EmailVerified />;
  }

  if (user?.email) {
    if (user?.emailVerificationSent) {
      return (
        <EmailVerificationAlreadySent
          handleGoToProfile={handleGoToProfile}
          handleGoToVerify={handleGoToVerify}
        />
      );
    } else {
      return (
        <EmailVerificationNotSent
          handleGoToProfile={handleGoToProfile}
          handleSendVerification={handleSendVerification}
        />
      );
    }
  } else {
    return <NoEmailProvided handleGoToProfile={handleGoToProfile} />;
  }
}

const EmailVerified = () => {
  const t = useT();
  return (
    <Button
      onClick={() => window.location.reload()}
      className="w-full sm:w-auto"
      variant="outline"
    >
      <RefreshCw className="mr-2 h-4 w-4" />
      {t("emailVerificationPrompt.reloadPageButton")}
    </Button>
  );
};

const NoEmailProvided = ({
  handleGoToProfile,
}: {
  handleGoToProfile: () => void;
}) => {
  const t = useT();

  return (
    <Button onClick={handleGoToProfile} className="w-full sm:w-auto">
      <User className="mr-2 h-4 w-4" />
      {t("emailVerificationPrompt.goToProfileButton")}
    </Button>
  );
};

const EmailVerificationAlreadySent = ({
  handleGoToProfile,
  handleGoToVerify,
}: {
  handleGoToProfile: () => void;
  handleGoToVerify: () => void;
}) => {
  const t = useT();

  return (
    <>
      <Button
        onClick={handleGoToProfile}
        className="w-full sm:w-auto"
        variant="outline"
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
};

const EmailVerificationNotSent = ({
  handleGoToProfile,
  handleSendVerification,
}: {
  handleGoToProfile: () => void;
  handleSendVerification: () => void;
}) => {
  const t = useT();

  return (
    <>
      <Button
        onClick={handleGoToProfile}
        className="w-full sm:w-auto"
        variant="outline"
      >
        <User className="mr-2 h-4 w-4" />
        {t("emailVerificationPrompt.goToProfileButton")}
      </Button>
      <Button onClick={handleSendVerification} className="w-full sm:w-auto">
        <MailCheck className="mr-2 h-4 w-4" />
        {t("emailVerificationPrompt.sendVerificationMail")}
      </Button>
    </>
  );
};
