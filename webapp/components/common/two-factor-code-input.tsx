"use client";

import { useEffect, useRef, useState } from "react";
import { Loader2 } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/custom-ui/label";
import {
  InputOTP,
  InputOTPGroup,
  InputOTPSlot,
} from "@/components/ui/input-otp";
import { useT } from "@/lib/i18n/hooks";
import type { TwoFactorMethod } from "@/api";

interface TwoFactorCodeInputProps {
  id: string;
  totpAvailable: boolean;
  emailAvailable: boolean;
  code: string;
  onCodeChange: (code: string) => void;
  onRequestEmailCode?: () => void;
  isRequestingEmailCode?: boolean;
  emailButtonLabel?: string;
  autoSendEmailCode?: boolean;
  autoSendEmailCodeOnSwitch?: boolean;
  allowBackupCode?: boolean;
  autoFocus?: boolean;
}

/**
 * Shared 2FA code entry: TOTP/email method switching (when both are available), backup-code
 * fallback, and the boxed 6-digit OTP input -- the same building block used for the login
 * challenge, disabling a factor, and confirming a sensitive account change.
 */
export function TwoFactorCodeInput({
  id,
  totpAvailable,
  emailAvailable,
  code,
  onCodeChange,
  onRequestEmailCode,
  isRequestingEmailCode,
  emailButtonLabel,
  autoSendEmailCode = false,
  autoSendEmailCodeOnSwitch = false,
  allowBackupCode = true,
  autoFocus,
}: TwoFactorCodeInputProps) {
  const t = useT();
  const [activeMethod, setActiveMethod] = useState<TwoFactorMethod>(
    totpAvailable ? "TOTP" : "EMAIL",
  );
  const [isBackupCodeMode, setIsBackupCodeMode] = useState(false);
  const bothAvailable = totpAvailable && emailAvailable;

  const onRequestEmailCodeRef = useRef(onRequestEmailCode);
  useEffect(() => {
    onRequestEmailCodeRef.current = onRequestEmailCode;
  }, [onRequestEmailCode]);

  useEffect(() => {
    if (autoSendEmailCode && activeMethod === "EMAIL" && !isBackupCodeMode) {
      onRequestEmailCodeRef.current?.();
    }
  }, [autoSendEmailCode, activeMethod, isBackupCodeMode]);

  const switchMethod = (method: TwoFactorMethod) => {
    setActiveMethod(method);
    onCodeChange("");
    if (method === "EMAIL" && autoSendEmailCodeOnSwitch) {
      onRequestEmailCode?.();
    }
  };

  const toggleBackupCodeMode = () => {
    setIsBackupCodeMode((prev) => !prev);
    onCodeChange("");
  };

  return (
    <div className="space-y-4">
      <div className="space-y-4">
        <div className="flex justify-center">
          <Label htmlFor={id} className="sr-only">
            {isBackupCodeMode
              ? t("twoFactor.backupCodesTitle")
              : activeMethod === "EMAIL"
                ? t("twoFactor.methodEmail")
                : t("twoFactor.methodTotp")}
          </Label>

          {isBackupCodeMode ? (
            <Input
              id={id}
              type="text"
              placeholder={t("twoFactor.backupCodePlaceholder")}
              value={code}
              onChange={(e) => onCodeChange(e.target.value)}
              maxLength={16}
              className="max-w-[220px] font-mono text-lg text-center tracking-widest"
              autoFocus={autoFocus}
            />
          ) : (
            <InputOTP
              id={id}
              maxLength={6}
              value={code}
              onChange={onCodeChange}
              autoFocus={autoFocus}
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
          )}
        </div>
        <p className="text-sm text-muted-foreground text-center">
          {isBackupCodeMode
            ? t("twoFactor.saveBackupCodesWarning")
            : activeMethod === "EMAIL"
              ? t("twoFactor.challengeDescriptionEmail")
              : t("twoFactor.challengeDescriptionTotp")}
        </p>
      </div>

      <div className="text-center text-sm space-y-2">
        {!isBackupCodeMode &&
          activeMethod === "EMAIL" &&
          onRequestEmailCode && (
            <div>
              <button
                type="button"
                onClick={onRequestEmailCode}
                disabled={isRequestingEmailCode}
                className="inline-flex items-center gap-1.5 text-primary hover:underline bg-transparent border-none cursor-pointer text-sm disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {isRequestingEmailCode && (
                  <Loader2 className="h-3.5 w-3.5 animate-spin" />
                )}
                {emailButtonLabel ?? t("twoFactor.resendEmailCode")}
              </button>
            </div>
          )}
        {bothAvailable && !isBackupCodeMode && (
          <div>
            <button
              type="button"
              onClick={() =>
                switchMethod(activeMethod === "TOTP" ? "EMAIL" : "TOTP")
              }
              className="text-primary hover:underline bg-transparent border-none cursor-pointer text-sm"
            >
              {activeMethod === "TOTP"
                ? t("twoFactor.switchToEmailCode")
                : t("twoFactor.switchToTotpCode")}
            </button>
          </div>
        )}
        {allowBackupCode && (
          <div>
            <button
              type="button"
              onClick={toggleBackupCodeMode}
              className="text-primary hover:underline bg-transparent border-none cursor-pointer text-sm"
            >
              {isBackupCodeMode
                ? t("twoFactor.useTotpOrEmailCode")
                : t("twoFactor.useBackupCode")}
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
