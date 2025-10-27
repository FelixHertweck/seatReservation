"use client";

import type React from "react";

import { useState } from "react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useAuth } from "@/hooks/use-auth";
import { useParams, useRouter, useSearchParams } from "next/navigation";
import { useT } from "@/lib/i18n/hooks";
import { isValidRedirectUrlEncoded } from "@/lib/utils";

export default function LoginPage() {
  const params = useParams();
  const locale = params.locale as string;
  const t = useT();
  const searchParams = useSearchParams();

  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [isLoadingForm, setIsLoadingForm] = useState(false);
  const [loginError, setLoginError] = useState<string | null>(null);
  const { user, isLoggedIn, login, logout } = useAuth();
  const router = useRouter();
  const [currentlyLoggingIn, setCurrentlyLoggingIn] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoadingForm(true);
    setLoginError(null);
    try {
      setCurrentlyLoggingIn(true);
      const returnToUrl = searchParams.get("returnTo");

      await login(username.trim(), password, returnToUrl);
      setCurrentlyLoggingIn(false);
    } catch (error: any) {
      if (error?.response?.status === 401) {
        setLoginError(t("login.error.invalidCredentials"));
      }
      setCurrentlyLoggingIn(false);
    } finally {
      setIsLoadingForm(false);
    }
  };

  const handleContinue = () => {
    const returnToUrl = searchParams.get("returnTo");
    router.push(
      returnToUrl && isValidRedirectUrlEncoded(returnToUrl)
        ? decodeURIComponent(returnToUrl)
        : `/${locale}/events`,
    );
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
              <Label htmlFor="username">{t("login.username")}</Label>
              <Input
                id="username"
                type="text"
                placeholder={t("login.enterUsername")}
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                autoCapitalize="none"
                autoComplete="username"
                required
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="password">{t("login.password")}</Label>
              <Input
                id="password"
                type="password"
                autoCapitalize="none"
                autoComplete="current-password"
                placeholder={t("login.enterPassword")}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
            </div>
            {loginError && (
              <div className="text-sm text-destructive bg-destructive/10 border border-destructive/20 rounded-md p-3">
                {loginError}
              </div>
            )}
            <Button type="submit" className="w-full" disabled={isLoadingForm}>
              {isLoadingForm ? t("login.signingIn") : t("login.signInButton")}
            </Button>
          </form>
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
