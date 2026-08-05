"use client";

import { useState } from "react";
import { useIconHover } from "@/hooks/use-icon-hover";
import { ShieldCheckIcon } from "@/components/ui/shield-check";
import { CopyIcon } from "@/components/ui/copy";
import { CheckIcon } from "@/components/ui/check";
import { RefreshCWIcon } from "@/components/ui/refresh-cw";
import { KeyIcon } from "@/components/ui/key";
import { EyeIcon } from "@/components/ui/eye";

import { ShieldAlert, Loader2, Smartphone, Mail, EyeOff } from "lucide-react";
import { Button } from "@/components/custom-ui/button";
import { Input } from "@/components/ui/input";
import {
  InputOTP,
  InputOTPGroup,
  InputOTPSlot,
} from "@/components/ui/input-otp";
import { Label } from "@/components/custom-ui/label";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/custom-ui/skeleton";
import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionTrigger,
} from "@/components/ui/accordion";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/custom-ui/alert-dialog";
import { toast } from "sonner";
import { useParams, useRouter } from "next/navigation";
import { useT } from "@/lib/i18n/hooks";
import { useTwoFactor } from "@/hooks/use-2fa";
import { useProfile } from "@/hooks/use-profile";
import type { TwoFactorMethod, TwoFactorSetupDto } from "@/api";
import { BackupCodesDisplay } from "@/components/common/backup-codes-display";
import { TwoFactorCodeInput } from "@/components/common/two-factor-code-input";

type TotpSetupStep = 1 | 2 | 3;

export function TwoFactorSection() {
  const t = useT();
  const params = useParams();
  const locale = params.locale as string;
  const router = useRouter();
  const {
    ref: shieldIconRef,
    onMouseEnter: handleShieldIconMouseEnter,
    onMouseLeave: handleShieldIconMouseLeave,
  } = useIconHover();
  const {
    status,
    isStatusLoading,
    setupTotp,
    sendSetupEmail,
    enableTwoFactor,
    disableTwoFactor,
    updateSettings,
    regenerateBackupCodes,
    isSetupLoading,
    isEnableLoading,
    isDisableLoading,
    isRegenerateLoading,
  } = useTwoFactor();
  const { user, resendConfirmation, isResendingConfirmation } = useProfile();

  const isEmailVerified = !!user?.emailVerified;

  // Setup Modal -- TOTP only. EMAIL needs no setup step: once the account email is verified,
  // enabling it is a single direct call (see handleEnableEmail).
  const [isSetupOpen, setIsSetupOpen] = useState(false);
  const [totpSetupData, setTotpSetupData] = useState<TwoFactorSetupDto | null>(
    null,
  );
  const [verificationCode, setVerificationCode] = useState("");
  const [isCopiedSecret, setIsCopiedSecret] = useState(false);
  const [isSecretVisible, setIsSecretVisible] = useState(false);
  const [totpSetupStep, setTotpSetupStep] = useState<TotpSetupStep>(1);

  // Backup codes modal after regeneration
  const [newBackupCodes, setNewBackupCodes] = useState<string[] | null>(null);

  // Regenerate confirmation -- requires proving current possession of 2FA, same as disabling a
  // factor, since minting a fresh set of backup codes is just as much a takeover as that is.
  const [isRegenerateConfirmOpen, setIsRegenerateConfirmOpen] = useState(false);
  const [regenerateCode, setRegenerateCode] = useState("");

  // Disable confirmation -- scoped to a single factor at a time (TOTP and EMAIL are
  // independent, so each has its own Disable action).
  const [isDisableConfirmOpen, setIsDisableConfirmOpen] = useState(false);
  const [disableMethod, setDisableMethod] = useState<TwoFactorMethod>("TOTP");
  const [disableCode, setDisableCode] = useState("");

  const is2FaEnabled = status?.twoFactorEnabled ?? false;
  const isTotpEnabled = status?.totpEnabled ?? false;
  const isEmailEnabled = status?.emailEnabled ?? false;

  const handleStartTotpSetup = async () => {
    setVerificationCode("");
    setIsCopiedSecret(false);
    setIsSecretVisible(false);
    setTotpSetupStep(1);

    try {
      const data = await setupTotp();
      if (data) {
        setTotpSetupData(data);
        setIsSetupOpen(true);
      }
    } catch {
      // Handled by toast
    }
  };

  const handleEnableSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!verificationCode.trim()) return;

    try {
      const result = await enableTwoFactor("TOTP", verificationCode.trim());
      setIsSetupOpen(false);
      setTotpSetupData(null);
      setVerificationCode("");
      if (result?.backupCodes && result.backupCodes.length > 0) {
        setNewBackupCodes(result.backupCodes);
      }
    } catch {
      // Handled by toast
    }
  };

  // Email possession was already proven via account email verification, so this is a single
  // direct call -- no separate 2FA setup code needed.
  const handleEnableEmail = async () => {
    try {
      const result = await enableTwoFactor("EMAIL");
      if (result?.backupCodes && result.backupCodes.length > 0) {
        setNewBackupCodes(result.backupCodes);
      }
    } catch {
      // Handled by toast
    }
  };

  const handleVerifyEmailFirst = async () => {
    try {
      await resendConfirmation();
      setTimeout(() => {
        router.push(`/${locale}/verify`);
      }, 700);
    } catch {
      // Handled by toast
    }
  };

  const openDisableConfirm = (method: TwoFactorMethod) => {
    setDisableMethod(method);
    setDisableCode("");
    setIsDisableConfirmOpen(true);
  };

  const handleDisableConfirm = async (e: React.MouseEvent) => {
    // AlertDialogAction closes the dialog on click by default; keep it open until we know the
    // code was actually accepted, so the user can see the error and retry.
    e.preventDefault();
    if (!disableCode.trim()) return;

    try {
      await disableTwoFactor(disableMethod, disableCode.trim());
      setIsDisableConfirmOpen(false);
      setDisableCode("");
    } catch {
      // Handled by toast
    }
  };

  const openRegenerateConfirm = () => {
    setRegenerateCode("");
    setIsRegenerateConfirmOpen(true);
  };

  const handleRegenerateConfirm = async (e: React.MouseEvent) => {
    // AlertDialogAction closes the dialog on click by default; keep it open until we know the
    // code was actually accepted, so the user can see the error and retry.
    e.preventDefault();
    if (!regenerateCode.trim()) return;

    try {
      const data = await regenerateBackupCodes(regenerateCode.trim());
      setIsRegenerateConfirmOpen(false);
      setRegenerateCode("");
      if (data?.backupCodes) {
        setNewBackupCodes(data.backupCodes);
      }
    } catch {
      // Handled by toast
    }
  };

  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text);
    setIsCopiedSecret(true);
    toast.success(t("twoFactor.secretCopied"));
    setTimeout(() => setIsCopiedSecret(false), 2000);
  };

  const handleTogglePasskey2Fa = async (
    e: React.ChangeEvent<HTMLInputElement>,
  ) => {
    const checked = e.target.checked;
    try {
      await updateSettings(checked);
    } catch {
      // Handled by toast
    }
  };

  // TOTP and email are independent, equally valid factors: each gets its own row with its own
  // Enable/Disable action, so either or both can be active at once.
  const renderMethodRow = (method: TwoFactorMethod, enabled: boolean) => {
    const Icon = method === "TOTP" ? Smartphone : Mail;
    const label =
      method === "TOTP"
        ? t("twoFactor.methodTotp")
        : t("twoFactor.methodEmail");
    const emailNeedsVerification =
      method === "EMAIL" && !enabled && !isEmailVerified;

    return (
      <div
        key={method}
        className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b pb-3 last:border-b-0 last:pb-0"
      >
        <div className="flex items-center gap-2">
          <Icon className="h-4 w-4 text-muted-foreground" />
          <div>
            <p className="text-sm font-medium">{label}</p>
            {enabled ? (
              <Badge
                variant="default"
                className="bg-emerald-600 hover:bg-emerald-700"
              >
                {t("twoFactor.enabled")}
              </Badge>
            ) : (
              <Badge variant="secondary">{t("twoFactor.disabled")}</Badge>
            )}
            {emailNeedsVerification && (
              <p className="mt-1 text-xs text-muted-foreground">
                {t("twoFactor.emailNotVerifiedHint")}
              </p>
            )}
          </div>
        </div>
        {enabled ? (
          <Button
            type="button"
            variant="destructive"
            size="sm"
            onClick={() => openDisableConfirm(method)}
            isLoading={isDisableLoading && disableMethod === method}
          >
            {t("twoFactor.disableButton")}
          </Button>
        ) : emailNeedsVerification ? (
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={handleVerifyEmailFirst}
            isLoading={isResendingConfirmation}
          >
            {t("twoFactor.verifyEmailButton")}
          </Button>
        ) : (
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={
              method === "TOTP" ? handleStartTotpSetup : handleEnableEmail
            }
            isLoading={method === "TOTP" ? isSetupLoading : isEnableLoading}
            disabled={method === "TOTP" ? isSetupLoading : isEnableLoading}
          >
            {t("twoFactor.enableButton")}
          </Button>
        )}
      </div>
    );
  };

  return (
    <div className="border-t pt-4">
      <Accordion type="single" collapsible>
        <AccordionItem value="two-factor" className="border-none">
          <AccordionTrigger
            className="py-0 hover:no-underline disabled:cursor-not-allowed disabled:opacity-50"
            disabled={isStatusLoading}
            onMouseEnter={handleShieldIconMouseEnter}
            onMouseLeave={handleShieldIconMouseLeave}
          >
            <div className="flex flex-col items-start gap-0.5 text-left">
              <span className="flex items-center gap-2 text-base font-medium">
                <ShieldCheckIcon
                  ref={shieldIconRef}
                  size={20}
                  className="text-primary"
                />
                {t("twoFactor.title")}
                {isStatusLoading ? (
                  <Skeleton className="h-5 w-16 rounded-full" />
                ) : is2FaEnabled ? (
                  <Badge
                    variant="default"
                    className="bg-emerald-600 hover:bg-emerald-700"
                  >
                    {t("twoFactor.enabled")}
                  </Badge>
                ) : (
                  <Badge variant="secondary">{t("twoFactor.disabled")}</Badge>
                )}
              </span>
              <span className="text-sm font-normal text-muted-foreground">
                {t("twoFactor.description")}
              </span>
            </div>
          </AccordionTrigger>

          <AccordionContent>
            <div className="mt-3 space-y-4 rounded-lg border bg-muted/30 p-4">
              <div className="space-y-3">
                {renderMethodRow("TOTP", isTotpEnabled)}
                {renderMethodRow("EMAIL", isEmailEnabled)}
              </div>

              {is2FaEnabled && (
                <>
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-t pt-3">
                    <div>
                      <p className="text-sm font-medium">
                        {t("twoFactor.requireForPasskey")}
                      </p>
                      <p className="text-xs text-muted-foreground">
                        {t("twoFactor.requireForPasskeyHint")}
                      </p>
                    </div>
                    <input
                      type="checkbox"
                      checked={status?.twoFactorPasskeyEnabled ?? false}
                      onChange={handleTogglePasskey2Fa}
                      className="h-4 w-4 rounded border-gray-300 text-primary focus:ring-primary cursor-pointer"
                    />
                  </div>

                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-t pt-3">
                    <div>
                      <p className="text-sm font-medium">
                        {t("twoFactor.backupCodesTitle")}
                      </p>
                      <p className="text-xs text-muted-foreground">
                        {t("twoFactor.backupCodesCount", {
                          count: Number(status?.remainingBackupCodes ?? 0),
                          total: 8,
                        })}
                      </p>
                    </div>
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      onClick={openRegenerateConfirm}
                    >
                      <RefreshCWIcon size={14} className="mr-1.5" />
                      {t("twoFactor.regenerateBackupCodes")}
                    </Button>
                  </div>
                </>
              )}
            </div>
          </AccordionContent>
        </AccordionItem>
      </Accordion>

      {/* Setup Modal -- TOTP only (EMAIL enables directly, see handleEnableEmail) */}
      <Dialog open={isSetupOpen} onOpenChange={setIsSetupOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Smartphone className="h-5 w-5 text-primary" />
              {t("twoFactor.setupTitle")}
            </DialogTitle>
            <DialogDescription>
              {t("twoFactor.stepIndicator", {
                current: totpSetupStep,
                total: 3,
              })}
            </DialogDescription>
          </DialogHeader>

          {totpSetupData && totpSetupStep === 1 && (
            <div className="space-y-4 py-2">
              <p className="text-sm text-muted-foreground">
                {t("twoFactor.totpInstructions")}
              </p>

              {totpSetupData.qrCodeDataUrl && (
                <div className="flex justify-center p-2 bg-white rounded-lg border w-max mx-auto">
                  {/* eslint-disable-next-line @next/next/no-img-element */}
                  <img
                    src={totpSetupData.qrCodeDataUrl}
                    alt="2FA QR Code"
                    className="h-44 w-44"
                  />
                </div>
              )}

              {totpSetupData.secret && (
                <div className="space-y-1.5">
                  <Label className="text-xs">{t("twoFactor.secretKey")}</Label>
                  <div className="flex items-center gap-2">
                    <Input
                      readOnly
                      type={isSecretVisible ? "text" : "password"}
                      value={totpSetupData.secret}
                      className="font-mono text-xs"
                    />
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      onClick={() => setIsSecretVisible(!isSecretVisible)}
                      aria-label={
                        isSecretVisible
                          ? t("twoFactor.hideSecret")
                          : t("twoFactor.showSecret")
                      }
                    >
                      {isSecretVisible ? (
                        <EyeOff className="h-4 w-4" />
                      ) : (
                        <EyeIcon size={16} />
                      )}
                    </Button>
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      onClick={() => copyToClipboard(totpSetupData.secret!)}
                    >
                      {isCopiedSecret ? (
                        <CheckIcon size={16} className="text-green-500" />
                      ) : (
                        <CopyIcon size={16} />
                      )}
                    </Button>
                  </div>
                </div>
              )}
            </div>
          )}

          {totpSetupData?.backupCodes &&
            totpSetupData.backupCodes.length > 0 &&
            totpSetupStep === 2 && (
              <div className="space-y-2 py-2">
                <div className="flex items-center gap-1.5 text-sm font-medium text-amber-700 dark:text-amber-400">
                  <KeyIcon size={16} />
                  {t("twoFactor.backupCodesTitle")}
                </div>
                <p className="text-xs text-muted-foreground">
                  {t("twoFactor.saveBackupCodesWarning")}
                </p>
                <BackupCodesDisplay codes={totpSetupData.backupCodes} />
              </div>
            )}

          {totpSetupStep < 3 && (
            <DialogFooter>
              <Button
                type="button"
                variant="outline"
                onClick={() =>
                  totpSetupStep === 1
                    ? setIsSetupOpen(false)
                    : setTotpSetupStep(1)
                }
              >
                {totpSetupStep === 1 ? t("common.cancel") : t("common.back")}
              </Button>
              <Button
                type="button"
                onClick={() =>
                  setTotpSetupStep((totpSetupStep + 1) as TotpSetupStep)
                }
              >
                {t("common.next")}
              </Button>
            </DialogFooter>
          )}

          {totpSetupStep === 3 && (
            <form onSubmit={handleEnableSubmit} className="space-y-4">
              <div className="space-y-2 py-2">
                <Label htmlFor="verificationCode">
                  {t("twoFactor.enterCodeToEnable")}
                </Label>
                <div className="flex justify-center">
                  <InputOTP
                    id="verificationCode"
                    maxLength={6}
                    value={verificationCode}
                    onChange={setVerificationCode}
                    autoFocus
                  >
                    <InputOTPGroup>
                      <InputOTPSlot index={0} />
                      <InputOTPSlot index={1} />
                      <InputOTPSlot index={2} />
                      <InputOTPSlot index={3} />
                      <InputOTPSlot index={4} />
                      <InputOTPSlot index={5} />
                    </InputOTPGroup>
                  </InputOTP>
                </div>
              </div>

              <DialogFooter>
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => setTotpSetupStep(2)}
                >
                  {t("common.back")}
                </Button>
                <Button
                  type="submit"
                  isLoading={isEnableLoading}
                  disabled={!verificationCode.trim() || isEnableLoading}
                >
                  {t("twoFactor.verifyAndEnable")}
                </Button>
              </DialogFooter>
            </form>
          )}
        </DialogContent>
      </Dialog>

      {/* Backup Codes Display Modal */}
      <Dialog
        open={!!newBackupCodes}
        onOpenChange={() => setNewBackupCodes(null)}
      >
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <KeyIcon size={20} className="text-amber-500" />
              {t("twoFactor.backupCodesTitle")}
            </DialogTitle>
            <DialogDescription>
              {t("twoFactor.saveBackupCodesWarning")}
            </DialogDescription>
          </DialogHeader>

          {newBackupCodes && (
            <div className="py-2">
              <BackupCodesDisplay codes={newBackupCodes} />
            </div>
          )}

          <DialogFooter>
            <Button type="button" onClick={() => setNewBackupCodes(null)}>
              OK
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Disable Factor Alert Modal */}
      <AlertDialog
        open={isDisableConfirmOpen}
        onOpenChange={setIsDisableConfirmOpen}
      >
        <AlertDialogContent>
          <AlertDialogHeader className="text-left">
            <AlertDialogTitle className="flex items-center gap-2">
              <ShieldAlert className="h-5 w-5 text-destructive" />
              {disableMethod === "TOTP"
                ? t("twoFactor.disableConfirmTitleTotp")
                : t("twoFactor.disableConfirmTitleEmail")}
            </AlertDialogTitle>
            <AlertDialogDescription>
              {t("twoFactor.disableConfirmDescription")}
            </AlertDialogDescription>
          </AlertDialogHeader>

          <TwoFactorCodeInput
            id="disableCode"
            totpAvailable={isTotpEnabled}
            emailAvailable={isEmailEnabled}
            code={disableCode}
            onCodeChange={setDisableCode}
            onRequestEmailCode={() => sendSetupEmail()}
            isRequestingEmailCode={isSetupLoading}
            autoSendEmailCode
            autoFocus
          />

          <AlertDialogFooter>
            <AlertDialogCancel onClick={() => setIsDisableConfirmOpen(false)}>
              {t("common.cancel")}
            </AlertDialogCancel>
            <AlertDialogAction
              onClick={handleDisableConfirm}
              disabled={!disableCode.trim() || isDisableLoading}
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
            >
              {isDisableLoading && (
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
              )}
              {t("twoFactor.disableButton")}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      {/* Regenerate Backup Codes Alert Modal */}
      <AlertDialog
        open={isRegenerateConfirmOpen}
        onOpenChange={setIsRegenerateConfirmOpen}
      >
        <AlertDialogContent>
          <AlertDialogHeader className="text-left">
            <AlertDialogTitle className="flex items-center gap-2">
              <RefreshCWIcon size={20} className="text-primary" />
              {t("twoFactor.regenerateConfirmTitle")}
            </AlertDialogTitle>
            <AlertDialogDescription>
              {t("twoFactor.regenerateConfirmDescription")}
            </AlertDialogDescription>
          </AlertDialogHeader>

          <TwoFactorCodeInput
            id="regenerateCode"
            totpAvailable={isTotpEnabled}
            emailAvailable={isEmailEnabled}
            code={regenerateCode}
            onCodeChange={setRegenerateCode}
            onRequestEmailCode={() => sendSetupEmail()}
            isRequestingEmailCode={isSetupLoading}
            autoSendEmailCode
            autoFocus
          />

          <AlertDialogFooter>
            <AlertDialogCancel
              onClick={() => setIsRegenerateConfirmOpen(false)}
            >
              {t("common.cancel")}
            </AlertDialogCancel>
            <AlertDialogAction
              onClick={handleRegenerateConfirm}
              disabled={!regenerateCode.trim() || isRegenerateLoading}
            >
              {isRegenerateLoading && (
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
              )}
              {t("twoFactor.regenerateBackupCodes")}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
