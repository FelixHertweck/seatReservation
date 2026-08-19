"use client";

import type React from "react";

import { useState, useEffect, useCallback } from "react";
import Link from "next/link";
import { Button } from "@/components/custom-ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  InputOTP,
  InputOTPGroup,
  InputOTPSlot,
} from "@/components/ui/input-otp";
import { useAuth } from "@/hooks/use-auth";
import { useProfile } from "@/hooks/use-profile";
import {
  useCooldown,
  EMAIL_RESEND_COOLDOWN_SECONDS,
} from "@/hooks/use-cooldown";
import { useParams, useRouter, useSearchParams } from "next/navigation";
import { useT } from "@/lib/i18n/hooks";
import { ErrorWithResponse } from "@/components/init-query-client";
import { redirectUser } from "@/lib/redirect-User";

export default function VerifyEmailPage() {
  const params = useParams();
  const locale = params.locale as string;
  const t = useT();
  const searchParams = useSearchParams();

  const [verificationCode, setVerificationCode] = useState("");
  const [isLoadingForm, setIsLoadingForm] = useState(false);
  const [isContinuing, setIsContinuing] = useState(false);
  const [verificationError, setVerificationError] = useState<string | null>(
    null,
  );
  const { user, isLoggedIn, verifyEmail, isRedirecting } = useAuth();
  const { resendConfirmation } = useProfile();
  const cooldown = useCooldown();
  const router = useRouter();

  const handleVerification = useCallback(
    async (code: string) => {
      if (code.length !== 6) {
        setVerificationError(t("emailVerification.invalidCodeLength"));
        return;
      }

      setIsLoadingForm(true);
      setVerificationError(null);

      try {
        await verifyEmail(code, searchParams.get("returnTo"));
      } catch (error) {
        if ((error as ErrorWithResponse)?.response?.status === 400) {
          setVerificationError(t("emailVerification.invalidCode"));
        } else {
          setVerificationError(t("emailVerification.verificationFailed"));
        }
      } finally {
        setIsLoadingForm(false);
      }
    },
    [t, verifyEmail, searchParams],
  );

  useEffect(() => {
    const codeFromUrl = searchParams.get("code");
    if (codeFromUrl?.length === 6 && /^\d{6}$/.test(codeFromUrl)) {
      setVerificationCode(codeFromUrl);
    }
  }, [setVerificationCode, searchParams]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    await handleVerification(verificationCode);
  };

  const handleCodeChange = (value: string) => {
    setVerificationCode(value);
  };

  const handleContinue = () => {
    setIsContinuing(true);
    redirectUser(router, locale, user, searchParams.get("returnTo"));
  };

  const handleResendCode = async () => {
    if (cooldown.isActive) return;
    try {
      await resendConfirmation();
      cooldown.startForSeconds(EMAIL_RESEND_COOLDOWN_SECONDS);
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
    }
  };

  if (isLoggedIn && !user?.email) {
    return (
      <div className="flex min-h-screen w-full items-center justify-center bg-background">
        <Card className="w-full max-w-md mx-4 rounded-none border-0 bg-transparent shadow-none md:rounded-lg md:border md:bg-card md:shadow-sm">
          <CardHeader className="space-y-1 p-0 pb-4 md:p-6">
            <CardTitle className="text-2xl font-bold">
              {t("emailVerification.noEmailTitle")}
            </CardTitle>
            <CardDescription>
              {t("emailVerification.noEmailDescription")}
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4 p-0 md:p-6 md:pt-0">
            <Button
              onClick={() => router.push(`/${locale}/profile`)}
              className="w-full"
            >
              {t("emailVerification.goToProfile")}
            </Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  const alreadyVerified = isLoggedIn && user?.emailVerified;

  return (
    <div className="flex min-h-screen w-full items-center justify-center bg-background">
      <Card className="w-full max-w-md mx-4 rounded-none border-0 bg-transparent shadow-none md:rounded-lg md:border md:bg-card md:shadow-sm">
        <CardHeader className="space-y-1 p-0 pb-4 md:p-6">
          <CardTitle className="text-2xl font-bold">
            {t("emailVerification.title")}
          </CardTitle>
          <CardDescription>
            {t("emailVerification.enterCode")}
            {user?.email && (
              <span className="font-medium text-foreground"> {user.email}</span>
            )}
          </CardDescription>
        </CardHeader>
        <CardContent className="p-0 md:p-6 md:pt-0">
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="space-y-4">
              <div className="flex justify-center">
                <InputOTP
                  maxLength={6}
                  value={verificationCode}
                  onChange={handleCodeChange}
                  disabled={alreadyVerified || isLoadingForm || isRedirecting}
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
              <p className="text-sm text-muted-foreground text-center">
                {t("emailVerification.codeHint")}
              </p>
            </div>

            {alreadyVerified && (
              <div className="text-sm text-green-600 dark:text-green-400 bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800 rounded-md p-3">
                {t("emailVerification.success.description")}
              </div>
            )}

            {verificationError && !alreadyVerified && (
              <div className="text-sm text-destructive bg-destructive/10 border border-destructive/20 rounded-md p-3">
                {verificationError}
              </div>
            )}

            {alreadyVerified ? (
              <Button
                type="button"
                onClick={handleContinue}
                className="w-full"
                isLoading={isContinuing || isRedirecting}
                disabled={isContinuing || isRedirecting}
              >
                {t("emailVerification.continueButton")}
              </Button>
            ) : (
              <Button
                type="submit"
                className="w-full"
                isLoading={isLoadingForm || isRedirecting}
                disabled={
                  isLoadingForm ||
                  isRedirecting ||
                  verificationCode.length !== 6
                }
              >
                {t("emailVerification.verifyButton")}
              </Button>
            )}
          </form>

          {!alreadyVerified && (
            <>
              <div className="mt-4 text-center text-sm space-y-2">
                <button
                  type="button"
                  onClick={handleResendCode}
                  disabled={cooldown.isActive}
                  className="text-primary hover:underline bg-transparent border-none cursor-pointer text-sm disabled:opacity-50 disabled:cursor-not-allowed disabled:no-underline"
                >
                  {cooldown.isActive
                    ? `${t("emailVerification.resendCode")} (${cooldown.remainingSeconds}s)`
                    : t("emailVerification.resendCode")}
                </button>
              </div>
              <div className="mt-4 text-center text-sm">
                <Link
                  href={`/${locale}/profile`}
                  className="text-muted-foreground hover:text-foreground"
                >
                  {t("emailVerification.backToProfile")}
                </Link>
              </div>
            </>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
