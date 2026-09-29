"use client";

import { useState, useMemo } from "react";
import { useParams, useRouter } from "next/navigation";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/custom-ui/dialog";
import { Button } from "@/components/custom-ui/button";
import { Input } from "@/components/ui/input";
import { useAuth } from "@/hooks/use-auth";
import { useProfile } from "@/hooks/use-profile";
import { useTwoFactor } from "@/hooks/use-2fa";
import {
  useCooldown,
  EMAIL_RESEND_COOLDOWN_SECONDS,
} from "@/hooks/use-cooldown";
import { ErrorWithResponse } from "@/components/init-query-client";
import Link from "next/link";
import { useT } from "@/lib/i18n/hooks";
import {
  BadgeCheck,
  Mail,
  MailCheck,
  MailWarning,
  RefreshCw,
  User,
} from "lucide-react";
import { EMAIL_PATTERN } from "@/lib/validation";

// The single source of truth for which of the dialog's variants to render
type PromptMode =
  | "loading"
  | "needsProfileForEmail"
  | "needsEmailInline"
  | "verified"
  | "verificationSent"
  | "verificationNotSent";

/**
 * The "add/verify your email" dialog itself, controlled by the caller. Used both as the
 * auto-triggered nag (see EmailVerificationPrompt) and on-demand wherever else an action
 * requires a verified email (e.g. creating a reservation).
 */
export function EmailRequiredDialog({
  open,
  onOpenChange,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
  const t = useT();

  const { user } = useAuth();
  const { updateProfile, isUpdating } = useProfile();
  const { status: twoFactorStatus, isStatusLoading: isTwoFactorStatusLoading } =
    useTwoFactor({ enabled: !user?.email });
  const router = useRouter();
  const params = useParams();
  const locale = params.locale as string;

  const [newEmail, setNewEmail] = useState("");

  const isEmailFlowLoading = !user?.email && isTwoFactorStatusLoading;
  const requiresProfileForEmail = !!twoFactorStatus?.twoFactorEnabled;

  const mode: PromptMode = useMemo(() => {
    if (!user?.email) {
      if (isEmailFlowLoading) return "loading";
      if (requiresProfileForEmail) return "needsProfileForEmail";
      return "needsEmailInline";
    }
    if (user.emailVerified) return "verified";
    if (user.emailVerificationSent) return "verificationSent";
    return "verificationNotSent";
  }, [user, isEmailFlowLoading, requiresProfileForEmail]);

  const handleUpdateEmail = async () => {
    if (!user || !EMAIL_PATTERN.test(newEmail.trim())) return;
    try {
      await updateProfile({
        email: newEmail.trim(),
        firstname: user.firstname || "",
        lastname: user.lastname || "",
      });
    } catch {
      // Handled by toast
      return;
    }
    setNewEmail("");
    // The backend already sent a verification email for the new address
    onOpenChange(false);
    setTimeout(() => {
      router.push(`/${locale}/verify`);
    }, 700);
  };

  const needsEmailInput = mode === "needsEmailInline";

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent noX={true} onInteractOutside={(e) => e.preventDefault()}>
        <DialogHeader className="space-y-3">
          <DialogTitle className="flex items-center gap-2">
            <MailWarning className="h-5 w-5 text-primary" />
            {t("emailVerificationPrompt.title")}
          </DialogTitle>
          <DialogDescription>
            <DynamicDialogContent setShowPopup={onOpenChange} mode={mode} />
          </DialogDescription>
        </DialogHeader>
        {needsEmailInput && (
          <div className="relative mt-3">
            <Mail className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              type="email"
              value={newEmail}
              onChange={(e) => setNewEmail(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === "Enter" && EMAIL_PATTERN.test(newEmail.trim())) {
                  handleUpdateEmail();
                }
              }}
              placeholder={t("emailVerificationPrompt.emailPlaceholder")}
              className="h-12 pl-9 text-base"
              autoFocus
            />
          </div>
        )}
        <DialogFooter className="flex flex-col sm:flex-row gap-2 sm:justify-end sm:space-x-2">
          <DynamicDialogFooter
            setShowPopup={onOpenChange}
            mode={mode}
            newEmail={newEmail}
            handleUpdateEmail={handleUpdateEmail}
            isUpdatingEmail={isUpdating}
          />
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

function DynamicDialogContent({
  setShowPopup,
  mode,
}: {
  setShowPopup: (show: boolean) => void;
  mode: PromptMode;
}) {
  const { user } = useAuth();
  const t = useT();

  switch (mode) {
    case "loading":
      return (
        <span className="flex items-center gap-2">
          <RefreshCw className="h-4 w-4 animate-spin" />
          {t("emailVerificationPrompt.checkingAccountStatus")}
        </span>
      );
    case "needsProfileForEmail":
      return <>{t("emailVerificationPrompt.noEmailRegistered")}</>;
    case "needsEmailInline":
      return <>{t("emailVerificationPrompt.noEmailRegisteredInline")}</>;
    case "verified":
      return (
        <p className="text-sm text-gray-500 mt-4">
          {t("emailVerificationPrompt.emailAlreadyVerifiedInfo")}
        </p>
      );
    case "verificationSent":
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
    case "verificationNotSent":
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
}

function DynamicDialogFooter({
  setShowPopup,
  mode,
  newEmail,
  handleUpdateEmail,
  isUpdatingEmail,
}: {
  setShowPopup: (show: boolean) => void;
  mode: PromptMode;
  newEmail: string;
  handleUpdateEmail: () => Promise<void>;
  isUpdatingEmail: boolean;
}) {
  const params = useParams();
  const locale = params.locale as string;

  const t = useT();
  const router = useRouter();
  const { resendConfirmation } = useAuth();
  const [isSending, setIsSending] = useState(false);
  const cooldown = useCooldown();

  const handleGoToProfile = () => {
    setShowPopup(false);
    router.push(`/${locale}/profile`);
  };

  const handleGoToVerify = () => {
    setShowPopup(false);
    router.push(`/${locale}/verify`);
  };

  const handleSendVerification = async () => {
    if (cooldown.isActive) return;
    setIsSending(true);
    try {
      await resendConfirmation();
      cooldown.startForSeconds(EMAIL_RESEND_COOLDOWN_SECONDS);
      setTimeout(() => {
        handleGoToVerify();
      }, 700);
    } catch (error) {
      const err = error as ErrorWithResponse;
      if (err?.response?.status === 429) {
        const retryAfter = (err?.response?.rawData as { retryAfter?: string })
          ?.retryAfter;
        if (retryAfter) {
          cooldown.startUntil(retryAfter);
        } else {
          cooldown.startForSeconds(EMAIL_RESEND_COOLDOWN_SECONDS);
        }
      }
    } finally {
      setIsSending(false);
    }
  };

  switch (mode) {
    case "loading":
      return (
        <Button className="w-full sm:w-auto" disabled isLoading>
          {t("emailVerificationPrompt.checkingAccountStatus")}
        </Button>
      );
    case "needsProfileForEmail":
      return <NoEmailProvided handleGoToProfile={handleGoToProfile} />;
    case "needsEmailInline":
      return (
        <UpdateEmailButton
          onClick={handleUpdateEmail}
          isUpdating={isUpdatingEmail}
          disabled={!EMAIL_PATTERN.test(newEmail.trim())}
        />
      );
    case "verified":
      return <EmailVerified />;
    case "verificationSent":
      return (
        <EmailVerificationAlreadySent
          handleGoToProfile={handleGoToProfile}
          handleGoToVerify={handleGoToVerify}
        />
      );
    case "verificationNotSent":
      return (
        <EmailVerificationNotSent
          handleGoToProfile={handleGoToProfile}
          handleSendVerification={handleSendVerification}
          isSending={isSending}
          cooldownActive={cooldown.isActive}
          remainingSeconds={cooldown.remainingSeconds}
        />
      );
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

const UpdateEmailButton = ({
  onClick,
  isUpdating,
  disabled,
}: {
  onClick: () => void;
  isUpdating: boolean;
  disabled: boolean;
}) => {
  const t = useT();

  return (
    <Button
      onClick={onClick}
      className="w-full sm:w-auto"
      isLoading={isUpdating}
      disabled={disabled || isUpdating}
    >
      <Mail className="mr-2 h-4 w-4" />
      {t("emailVerificationPrompt.updateEmailButton")}
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
  isSending,
  cooldownActive,
  remainingSeconds,
}: {
  handleGoToProfile: () => void;
  handleSendVerification: () => void;
  isSending: boolean;
  cooldownActive: boolean;
  remainingSeconds: number;
}) => {
  const t = useT();

  return (
    <>
      <Button
        onClick={handleGoToProfile}
        className="w-full sm:w-auto"
        variant="outline"
        disabled={isSending}
      >
        <User className="mr-2 h-4 w-4" />
        {t("emailVerificationPrompt.goToProfileButton")}
      </Button>
      <Button
        onClick={handleSendVerification}
        className="w-full sm:w-auto"
        isLoading={isSending}
        disabled={isSending || cooldownActive}
      >
        <MailCheck className="mr-2 h-4 w-4" />
        {cooldownActive
          ? `${t("emailVerificationPrompt.sendVerificationMail")} (${remainingSeconds}s)`
          : t("emailVerificationPrompt.sendVerificationMail")}
      </Button>
    </>
  );
};
