"use client";

import type React from "react";

import { useState, useEffect, useCallback } from "react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
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
import { useParams, useRouter, useSearchParams } from "next/navigation";
import { useT } from "@/lib/i18n/hooks";
import { toast } from "@/hooks/use-toast";
import { ErrorWithResponse } from "@/components/init-query-client";
import { redirectUser } from "@/lib/redirect-User";

export default function VerifyEmailPage() {
  const params = useParams();
  const locale = params.locale as string;
  const t = useT();
  const searchParams = useSearchParams();

  const [verificationCode, setVerificationCode] = useState("");
  const [isLoadingForm, setIsLoadingForm] = useState(false);
  const [verificationError, setVerificationError] = useState<string | null>(
    null,
  );
  const { user, isLoggedIn, verifyEmail } = useAuth();
  const { resendConfirmation } = useProfile();
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
        await verifyEmail(code);
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
    [t, verifyEmail],
  );

  useEffect(() => {
    const codeFromUrl = searchParams.get("code");
    if (
      codeFromUrl &&
      codeFromUrl.length === 6 &&
      /^\d{6}$/.test(codeFromUrl)
    ) {
      setVerificationCode(codeFromUrl);
    }
  }, [setVerificationCode, searchParams]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    await handleVerification(verificationCode);
  };

  const handleCodeChange = (value: string) => {
    setVerificationCode(value);
    if (value.length === 6) {
      handleVerification(value);
    }
  };

  const handleContinue = () => {
    redirectUser(router, locale, user, searchParams.get("returnTo"));
  };

  const handleResendCode = async () => {
    try {
      await resendConfirmation();
      toast({
        title: t("emailVerification.confirmationEmailResentTitle"),
        description: t("emailVerification.confirmationEmailResentDescription"),
      });
    } catch (error) {
      console.error("Failed to resend confirmation:", error);
    }
  };

  if (isLoggedIn && !user?.email) {
    return (
      <div className="flex min-h-screen w-full items-center justify-center bg-background">
        <Card className="w-full max-w-md mx-4">
          <CardHeader className="space-y-1">
            <CardTitle className="text-2xl font-bold">
              {t("emailVerification.noEmailTitle")}
            </CardTitle>
            <CardDescription>
              {t("emailVerification.noEmailDescription")}
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
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
      <Card className="w-full max-w-md mx-4">
        <CardHeader className="space-y-1">
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
        <CardContent>
          {isLoggedIn && user?.emailVerified && (
            <div className="mb-4 text-sm text-green-600 dark:text-green-400 bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800 rounded-md p-3">
              {t("emailVerification.alreadyVerified")}
            </div>
          )}
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="space-y-4">
              <div className="flex justify-center">
                <InputOTP
                  maxLength={6}
                  value={verificationCode}
                  onChange={handleCodeChange}
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
            {verificationError && (
              <div className="text-sm text-destructive bg-destructive/10 border border-destructive/20 rounded-md p-3">
                {verificationError}
              </div>
            )}
            <Button
              type="submit"
              className="w-full"
              variant={alreadyVerified ? "outline" : "default"}
              disabled={isLoadingForm || verificationCode.length !== 6}
            >
              {isLoadingForm
                ? t("emailVerification.verifying")
                : t("emailVerification.verifyButton")}
            </Button>
            {alreadyVerified && (
              <div className="space-y-2">
                <Button onClick={handleContinue} className="w-full">
                  {t("emailVerification.continueButton")}
                </Button>
              </div>
            )}
          </form>
          {!alreadyVerified && (
            <>
              <div className="mt-4 text-center text-sm space-y-2">
                <button
                  onClick={handleResendCode}
                  className="text-primary hover:underline bg-transparent border-none cursor-pointer text-sm"
                >
                  {t("emailVerification.resendCode")}
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
