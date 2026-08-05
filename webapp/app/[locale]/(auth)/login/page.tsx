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
import { useAuth } from "@/hooks/use-auth";
import { useWebAuthn } from "@/hooks/use-webauthn";
import { useParams, useRouter, useSearchParams } from "next/navigation";
import { useT } from "@/lib/i18n/hooks";
import { ErrorWithResponse } from "@/components/init-query-client";
import { redirectUser } from "@/lib/redirect-User";
import { KeyRound } from "@/components/icons";

export default function LoginPage() {
  const params = useParams();
  const locale = params.locale as string;
  const t = useT();
  const searchParams = useSearchParams();

  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [isLoadingForm, setIsLoadingForm] = useState(false);
  const [loginError, setLoginError] = useState<string | null>(null);
  const { user, isLoggedIn, login, logout, retryAfter } = useAuth();
  const { isSupported: isPasskeySupported, loginWithPasskey } = useWebAuthn();
  const [isPasskeyLoading, setIsPasskeyLoading] = useState(false);
  const router = useRouter();
  const [currentlyLoggingIn, setCurrentlyLoggingIn] = useState(false);
  const [remainingTime, setRemainingTime] = useState<number>(0);
  const [isRetryAfterActive, setIsRetryAfterActive] = useState(false);

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

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoadingForm(true);
    setLoginError(null);
    try {
      setCurrentlyLoggingIn(true);
      const returnToUrl = searchParams.get("returnTo");

      await login(username.trim(), password, returnToUrl);
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
    setIsPasskeyLoading(true);
    setLoginError(null);
    try {
      setCurrentlyLoggingIn(true);
      await loginWithPasskey(username.trim(), searchParams.get("returnTo"));
      setCurrentlyLoggingIn(false);
    } catch {
      // Errors are surfaced via toast inside the hook; keep the form usable.
      setCurrentlyLoggingIn(false);
    } finally {
      setIsPasskeyLoading(false);
    }
  };

  const handleContinue = () => {
    redirectUser(router, locale, user, searchParams.get("returnTo"));
  };

  const handleLogout = async () => {
    await logout();
  };

  if (isLoggedIn && !currentlyLoggingIn) {
    return (
      <div className="flex min-h-screen w-full items-center justify-center bg-background">
        <Card className="w-full max-w-md mx-4">
          <CardHeader className="space-y-1">
            <CardTitle className="text-2xl font-bold">
              {t("login.welcomeBack")}
            </CardTitle>
            <CardDescription>{t("login.alreadyLoggedIn")}</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
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

  return (
    <div className="flex min-h-screen w-full items-center justify-center bg-background">
      <Card className="w-full max-w-md mx-4">
        <CardHeader className="space-y-1">
          <CardTitle className="text-2xl font-bold">
            {t("login.signIn")}
          </CardTitle>
          <CardDescription>{t("login.enterCredentials")}</CardDescription>
        </CardHeader>
        <CardContent>
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
                autoCapitalize="none"
                autoComplete="username"
                required
              />
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
              disabled={isLoadingForm || isRetryAfterActive || !!loginError}
            >
              {loginError ||
                (isRetryAfterActive
                  ? formatRetryTime()
                  : t("login.signInButton"))}
            </Button>
          </form>
          {isPasskeySupported && (
            <>
              <div className="relative my-4">
                <div className="absolute inset-0 flex items-center">
                  <span className="w-full border-t" />
                </div>
                <div className="relative flex justify-center text-xs uppercase">
                  <span className="bg-background px-2 text-muted-foreground">
                    {t("login.or")}
                  </span>
                </div>
              </div>
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
