"use client";

import type React from "react";

import { useState, useEffect } from "react";
import Link from "next/link";
import { Button } from "@/components/custom-ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/custom-ui/label";
import { TwoFactorCodeInput } from "@/components/common/two-factor-code-input";
import { useAuth } from "@/hooks/use-auth";
import { useWebAuthn } from "@/hooks/use-webauthn";
import { useTwoFactor } from "@/hooks/use-2fa";
import { useParams, useRouter, useSearchParams } from "next/navigation";
import { useT } from "@/lib/i18n/hooks";
import { ErrorWithResponse } from "@/components/init-query-client";
import { redirectUser } from "@/lib/redirect-User";
import { KeyRound, ShieldCheck, ArrowLeft } from "lucide-react";
import { useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";

export default function LoginPage() {
  const params = useParams();
  const locale = params.locale as string;
  const t = useT();
  const searchParams = useSearchParams();
  const router = useRouter();
  const queryClient = useQueryClient();

  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [isLoadingForm, setIsLoadingForm] = useState(false);
  const [loginError, setLoginError] = useState<string | null>(null);

  const { user, isLoggedIn, login, logout, retryAfter, refetchUser } =
    useAuth();
  const { isSupported: isPasskeySupported, loginWithPasskey } = useWebAuthn();
  const { verifyChallenge, resendChallengeEmail } = useTwoFactor();

  const [isPasskeyLoading, setIsPasskeyLoading] = useState(false);
  const [currentlyLoggingIn, setCurrentlyLoggingIn] = useState(false);
  const [remainingTime, setRemainingTime] = useState<number>(0);
  const [isRetryAfterActive, setIsRetryAfterActive] = useState(false);

  // 2FA Challenge states
  const [twoFactorChallenge, setTwoFactorChallenge] = useState<{
    challengeToken: string;
    totpAvailable: boolean;
    emailAvailable: boolean;
  } | null>(null);
  const [twoFactorCode, setTwoFactorCode] = useState("");
  const [isVerifying2Fa, setIsVerifying2Fa] = useState(false);
  const [isResendingEmailCode, setIsResendingEmailCode] = useState(false);

  useEffect(() => {
    if (!retryAfter) {
      setIsRetryAfterActive(false);
      setRemainingTime(0);
      return;
    }

    const retryAfterDate = new Date(retryAfter).getTime();
    const now = new Date().getTime();

    if (retryAfterDate > now) {
      setIsRetryAfterActive(true);
      const calculateRemaining = () => {
        const current = new Date().getTime();
        const remaining = Math.max(
          0,
          Math.ceil((retryAfterDate - current) / 1000),
        );
        setRemainingTime(remaining);
        return remaining > 0;
      };

      calculateRemaining();
      const interval = setInterval(() => {
        if (!calculateRemaining()) {
          clearInterval(interval);
          setIsRetryAfterActive(false);
        }
      }, 1000);

      return () => clearInterval(interval);
    } else {
      setIsRetryAfterActive(false);
      setRemainingTime(0);
    }
  }, [retryAfter]);

  const formatRetryTime = (): string => {
    const hours = Math.floor(remainingTime / 3600);
    const minutes = Math.floor((remainingTime % 3600) / 60);
    const seconds = remainingTime % 60;

    let duration = "";
    if (hours > 0) duration += `${hours}h `;
    if (minutes > 0) duration += `${minutes}m `;
    duration += `${seconds}s`;

    if (retryAfter) {
      const retryDate = new Date(retryAfter);
      const timeString = retryDate.toLocaleTimeString(locale);
      return t("login.error.accountLocked", {
        time: timeString,
        duration: duration.trim(),
      });
    }
    return "";
  };

  const isEmailUsername = username.includes("@");

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (isEmailUsername) {
      setLoginError(t("login.error.emailNotAllowed"));
      return;
    }
    setIsLoadingForm(true);
    setLoginError(null);
    try {
      setCurrentlyLoggingIn(true);
      const returnToUrl = searchParams.get("returnTo");

      const res = await login(username.trim(), password, returnToUrl);
      if (res?.twoFactorRequired && res?.challengeToken) {
        setTwoFactorChallenge({
          challengeToken: res.challengeToken,
          totpAvailable: res.totpAvailable ?? false,
          emailAvailable: res.emailAvailable ?? false,
        });
        return;
      }
      setCurrentlyLoggingIn(false);
    } catch (error) {
      if ((error as ErrorWithResponse).response?.status === 401) {
        setLoginError(t("login.error.invalidCredentials"));
      }
      setCurrentlyLoggingIn(false);
    } finally {
      setIsLoadingForm(false);
    }
  };

  const handlePasskeyLogin = async () => {
    if (username.trim() && isEmailUsername) {
      setLoginError(t("login.error.emailNotAllowed"));
      return;
    }
    setIsPasskeyLoading(true);
    setLoginError(null);
    try {
      setCurrentlyLoggingIn(true);
      const res = await loginWithPasskey(
        username.trim(),
        searchParams.get("returnTo"),
      );
      if (res?.twoFactorRequired && res?.challengeToken) {
        setTwoFactorChallenge({
          challengeToken: res.challengeToken,
          totpAvailable: res.totpAvailable ?? false,
          emailAvailable: res.emailAvailable ?? false,
        });
        return;
      }
      setCurrentlyLoggingIn(false);
    } catch {
      setCurrentlyLoggingIn(false);
    } finally {
      setIsPasskeyLoading(false);
    }
  };

  const handleVerify2Fa = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!twoFactorChallenge || !twoFactorCode.trim()) return;

    setIsVerifying2Fa(true);
    try {
      await verifyChallenge(
        twoFactorChallenge.challengeToken,
        twoFactorCode.trim(),
      );
      await queryClient.invalidateQueries();
      const { data: freshUser } = await refetchUser();
      toast.success(t("login.success.title"));
      redirectUser(
        router,
        locale,
        freshUser ?? user,
        searchParams.get("returnTo"),
      );
    } catch {
      // Error handled by verifyChallenge toast
    } finally {
      setIsVerifying2Fa(false);
    }
  };

  const handleResendEmail = async () => {
    if (!twoFactorChallenge) return;
    setIsResendingEmailCode(true);
    try {
      await resendChallengeEmail(twoFactorChallenge.challengeToken);
    } catch {
      // Toast handles error
    } finally {
      setIsResendingEmailCode(false);
    }
  };

  const handleContinue = () => {
    redirectUser(router, locale, user, searchParams.get("returnTo"));
  };

  const handleLogout = async () => {
    await logout();
  };

  if (isLoggedIn && !currentlyLoggingIn && !twoFactorChallenge) {
    return (
      <div className="flex min-h-screen w-full items-center justify-center bg-background">
        <Card className="w-full max-w-md mx-4 rounded-none border-0 bg-transparent shadow-none md:rounded-lg md:border md:bg-card md:shadow-sm">
          <CardHeader className="space-y-1 p-0 pb-4 md:p-6">
            <CardTitle className="text-2xl font-bold">
              {t("login.welcomeBack")}
            </CardTitle>
            <CardDescription>{t("login.alreadyLoggedIn")}</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4 p-0 md:p-6 md:pt-0">
            <Button onClick={handleContinue} className="w-full">
              {t("login.continueWithUser", { username: user?.username })}
            </Button>
            <Button
              onClick={handleLogout}
              variant="outline"
              className="w-full bg-transparent"
            >
              {t("login.logoutButton")}
            </Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  // 2FA Challenge Form UI
  if (twoFactorChallenge) {
    return (
      <div className="flex min-h-screen w-full items-center justify-center bg-background">
        <Card className="w-full max-w-md mx-4 rounded-none border-0 bg-transparent shadow-none md:rounded-lg md:border md:bg-card md:shadow-sm">
          <CardHeader className="space-y-1 p-0 pb-4 md:p-6">
            <div className="flex items-center gap-2">
              <ShieldCheck className="h-6 w-6 text-primary" />
              <CardTitle className="text-xl font-bold">
                {t("twoFactor.challengeTitle")}
              </CardTitle>
            </div>
          </CardHeader>
          <CardContent className="p-0 md:p-6 md:pt-0">
            <form onSubmit={handleVerify2Fa} className="space-y-4">
              <TwoFactorCodeInput
                id="2fa-code"
                totpAvailable={twoFactorChallenge.totpAvailable}
                emailAvailable={twoFactorChallenge.emailAvailable}
                code={twoFactorCode}
                onCodeChange={setTwoFactorCode}
                onRequestEmailCode={handleResendEmail}
                isRequestingEmailCode={isResendingEmailCode}
                emailButtonLabel={t("twoFactor.resendEmailCode")}
                autoSendEmailCodeOnSwitch
                autoFocus
              />

              <Button
                type="submit"
                className="w-full"
                isLoading={isVerifying2Fa}
                disabled={!twoFactorCode.trim() || isVerifying2Fa}
              >
                {t("login.signInButton")}
              </Button>

              <div className="flex justify-center pt-2 border-t text-xs">
                <button
                  type="button"
                  onClick={() => {
                    setTwoFactorChallenge(null);
                    setCurrentlyLoggingIn(false);
                  }}
                  className="text-muted-foreground hover:text-foreground flex items-center gap-1"
                >
                  <ArrowLeft className="h-3.5 w-3.5" />
                  {t("common.cancel")}
                </button>
              </div>
            </form>
          </CardContent>
        </Card>
      </div>
    );
  }

  // Standard Login Form UI
  return (
    <div className="flex min-h-screen w-full items-center justify-center bg-background">
      <Card className="w-full max-w-md mx-4 rounded-none border-0 bg-transparent shadow-none md:rounded-lg md:border md:bg-card md:shadow-sm">
        <CardHeader className="space-y-1 p-0 pb-4 md:p-6">
          <CardTitle className="text-2xl font-bold">
            {t("login.signIn")}
          </CardTitle>
          <CardDescription>{t("login.enterCredentials")}</CardDescription>
        </CardHeader>
        <CardContent className="p-0 md:p-6 md:pt-0">
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <Label htmlFor="username">{t("login.username")}</Label>
                <Link
                  href="/forgot-username"
                  className="text-sm text-primary hover:underline"
                >
                  {t("login.forgotUsername")}
                </Link>
              </div>
              <Input
                id="username"
                type="text"
                placeholder={t("login.enterUsername")}
                value={username}
                onChange={(e) => {
                  setUsername(e.target.value);
                  setLoginError(null);
                }}
                className={
                  isEmailUsername
                    ? "border-destructive focus-visible:ring-destructive"
                    : ""
                }
                autoCapitalize="none"
                autoComplete="username"
                required
              />
              {isEmailUsername && (
                <p className="text-xs text-destructive mt-1">
                  {t("login.error.emailNotAllowed")}
                </p>
              )}
            </div>
            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <Label htmlFor="password">{t("login.password")}</Label>
                <Link
                  href="/forgot-password"
                  className="text-sm text-primary hover:underline"
                >
                  {t("login.forgotPassword")}
                </Link>
              </div>
              <Input
                id="password"
                type="password"
                autoCapitalize="none"
                autoComplete="current-password"
                placeholder={t("login.enterPassword")}
                value={password}
                onChange={(e) => {
                  setPassword(e.target.value);
                  setLoginError(null);
                }}
                required
              />
            </div>
            <Button
              type="submit"
              className="w-full"
              variant={loginError ? "destructive" : "default"}
              isLoading={isLoadingForm}
              disabled={
                isLoadingForm ||
                isRetryAfterActive ||
                !!loginError ||
                isEmailUsername
              }
            >
              {loginError ||
                (isRetryAfterActive
                  ? formatRetryTime()
                  : t("login.signInButton"))}
            </Button>
          </form>
          {isPasskeySupported && (
            <>
              <div className="my-4 border-t" />
              <Button
                type="button"
                variant="outline"
                className="w-full"
                onClick={handlePasskeyLogin}
                isLoading={isPasskeyLoading}
                disabled={isPasskeyLoading || isRetryAfterActive}
              >
                <KeyRound className="mr-2 h-4 w-4" />
                {t("webauthn.login.button")}
              </Button>
            </>
          )}
          <div className="mt-4 text-center text-sm">
            {t("login.noAccount")}
            <Link href="/register" className="text-primary hover:underline">
              {t("login.register")}
            </Link>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
