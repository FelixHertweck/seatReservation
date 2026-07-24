"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useAuth } from "@/hooks/use-auth";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { AlertCircle, User as UserIcon } from "lucide-react";
import { useSearchParams } from "next/navigation";
import { useT } from "@/lib/i18n/hooks";
import { useWebAuthn } from "@/hooks/use-webauthn";

export default function LoginPage() {
  const t = useT();
  const searchParams = useSearchParams();

  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [isLoadingForm, setIsLoadingForm] = useState(false);
  const [loginError, setLoginError] = useState<string | null>(null);
  const { user, isLoggedIn, login, logout, retryAfter } = useAuth();
  const { isSupported: isPasskeySupported, loginWithPasskey } = useWebAuthn();
  const [isPasskeyLoading, setIsPasskeyLoading] = useState(false);
  const [currentlyLoggingIn, setCurrentlyLoggingIn] = useState(false);

  const isRetryAfterActive =
    retryAfter !== null && new Date(retryAfter) > new Date();

  const [timeRemaining, setTimeRemaining] = useState<number>(0);

  useEffect(() => {
    if (retryAfter) {
      const updateTimer = () => {
        const remaining = Math.max(
          0,
          new Date(retryAfter).getTime() - new Date().getTime(),
        );
        setTimeRemaining(remaining);
      };

      updateTimer();
      const interval = setInterval(updateTimer, 1000);
      return () => clearInterval(interval);
    }
  }, [retryAfter]);

  const formatTimeRemaining = (ms: number) => {
    const totalSeconds = Math.ceil(ms / 1000);
    const minutes = Math.floor(totalSeconds / 60);
    const seconds = totalSeconds % 60;
    return `${minutes}:${seconds.toString().padStart(2, "0")}`;
  };

  const handleFormLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    if (isRetryAfterActive) return;
    setIsLoadingForm(true);
    setLoginError(null);
    try {
      setCurrentlyLoggingIn(true);
      const returnToUrl = searchParams.get("returnTo");

      await login(username.trim(), password, returnToUrl);
      setCurrentlyLoggingIn(false);
    } catch (error: unknown) {
      if ((error as { status?: number }).status === 401) {
        setLoginError(t("login.error.invalidCredentials"));
      }
      setIsLoadingForm(false);
      setCurrentlyLoggingIn(false);
    }
  };

  const handlePasskeyLogin = async () => {
    if (isRetryAfterActive) return;
    setIsPasskeyLoading(true);
    setLoginError(null);
    try {
      setCurrentlyLoggingIn(true);
      await loginWithPasskey(username.trim(), searchParams.get("returnTo"));
      setCurrentlyLoggingIn(false);
    } catch (error) {
      console.error("Passkey login failed:", error);
      setLoginError(t("login.error.passkeyFailed"));
      setIsPasskeyLoading(false);
      setCurrentlyLoggingIn(false);
    }
  };

  if (isLoggedIn && !currentlyLoggingIn) {
    return (
      <div className="flex h-screen w-full items-center justify-center px-4">
        <Card className="w-full max-w-sm">
          <CardHeader>
            <CardTitle className="text-2xl">{t("login.welcomeBack")}</CardTitle>
            <CardDescription>{t("login.alreadyLoggedIn")}</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <Button asChild className="w-full" variant="default">
              <Link href="/">
                {t("login.continueWithUser", { username: user?.username })}
              </Link>
            </Button>
            <Button
              onClick={() => logout()}
              variant="outline"
              className="w-full"
            >
              {t("login.logoutButton")}
            </Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="flex h-screen w-full items-center justify-center px-4">
      <Card className="w-full max-w-sm">
        <CardHeader>
          <CardTitle className="text-2xl">{t("login.signIn")}</CardTitle>
          <CardDescription>{t("login.enterCredentials")}</CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleFormLogin} className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="username">{t("login.username")}</Label>
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

            {loginError && (
              <Alert
                variant={loginError ? "destructive" : "default"}
                className={
                  loginError
                    ? ""
                    : "border-yellow-500 bg-yellow-50 text-yellow-900"
                }
              >
                <AlertCircle className="h-4 w-4" />
                <AlertTitle>{t("login.error.title")}</AlertTitle>
                <AlertDescription>{loginError}</AlertDescription>
              </Alert>
            )}

            {isRetryAfterActive && (
              <Alert variant="destructive">
                <AlertCircle className="h-4 w-4" />
                <AlertTitle>{t("login.error.title")}</AlertTitle>
                <AlertDescription>
                  {t("login.error.accountLocked", {
                    timeRemaining: formatTimeRemaining(timeRemaining),
                  })}
                </AlertDescription>
              </Alert>
            )}

            <Button
              type="submit"
              className="w-full"
              disabled={isLoadingForm || isRetryAfterActive || !!loginError}
            >
              {isLoadingForm ? t("login.signingIn") : t("login.signInButton")}
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

              <div className="space-y-2">
                <Label htmlFor="passkey-username" className="sr-only">
                  {t("login.username")}
                </Label>
                <Input
                  id="passkey-username"
                  type="text"
                  placeholder={t("login.enterUsernameForPasskey")}
                  value={username}
                  onChange={(e) => {
                    setUsername(e.target.value);
                    setLoginError(null);
                  }}
                  autoCapitalize="none"
                  autoComplete="username webauthn"
                />
                <Button
                  type="button"
                  variant="outline"
                  className="w-full"
                  onClick={handlePasskeyLogin}
                  disabled={
                    isPasskeyLoading ||
                    isRetryAfterActive ||
                    !username.trim() ||
                    !!loginError
                  }
                >
                  <UserIcon className="mr-2 h-4 w-4" />
                  {isPasskeyLoading
                    ? t("login.signingIn")
                    : t("login.signInWithPasskey")}
                </Button>
              </div>
            </>
          )}
        </CardContent>
        <CardFooter className="flex flex-col space-y-2">
          <div className="text-center text-sm">
            {t("login.noAccount")}{" "}
            <Link href="/register" className="text-primary hover:underline">
              {t("login.registerHere")}
            </Link>
          </div>
        </CardFooter>
      </Card>
    </div>
  );
}
