"use client";

import type React from "react";

import { useState } from "react";
import { useSearchParams } from "next/navigation";
import Link from "next/link";
import { useAuth } from "@/hooks/use-auth";
import { Button } from "@/components/custom-ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/custom-ui/label";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { useT } from "@/lib/i18n/hooks";
import { ErrorWithResponse } from "@/components/init-query-client";

export default function ResetPasswordPage() {
  const t = useT();
  const { confirmPasswordReset } = useAuth();
  const searchParams = useSearchParams();
  const token = searchParams.get("token");

  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!token) {
      setError(t("resetPassword.error.missingToken"));
      return;
    }

    if (password.length < 8) {
      setError(t("resetPassword.error.tooShort"));
      return;
    }

    if (password !== confirmPassword) {
      setError(t("resetPassword.error.mismatch"));
      return;
    }

    setIsLoading(true);
    try {
      await confirmPasswordReset({ token, newPassword: password });
      setIsSuccess(true);
    } catch (err) {
      const status = (err as ErrorWithResponse)?.response?.status;
      if (status === 410) {
        setError(t("resetPassword.error.tokenExpired"));
      } else if (status === 400) {
        setError(t("resetPassword.error.invalidToken"));
      } else {
        setError(t("resetPassword.error.general"));
      }
    } finally {
      setIsLoading(false);
    }
  };

  if (!token) {
    return (
      <div className="flex min-h-screen w-full items-center justify-center bg-background px-4">
        <Card className="w-full max-w-md rounded-none border-0 bg-transparent shadow-none md:rounded-lg md:border md:bg-card md:shadow-sm">
          <CardHeader className="space-y-1 p-0 pb-4 md:p-6">
            <CardTitle className="text-2xl font-bold">
              {t("resetPassword.title")}
            </CardTitle>
          </CardHeader>
          <CardContent className="p-0 md:p-6 md:pt-0">
            <Alert
              variant="destructive"
              className="mb-4 border-destructive bg-destructive text-destructive-foreground"
            >
              <AlertDescription>
                {t("resetPassword.error.missingToken")}
              </AlertDescription>
            </Alert>
            <div className="text-center text-sm">
              <Link href="/login" className="text-primary hover:underline">
                {t("forgotPassword.backToLogin")}
              </Link>
            </div>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="flex min-h-screen w-full items-center justify-center bg-background px-4">
      <Card className="w-full max-w-md rounded-none border-0 bg-transparent shadow-none md:rounded-lg md:border md:bg-card md:shadow-sm">
        <CardHeader className="space-y-1 p-0 pb-4 md:p-6">
          <CardTitle className="text-2xl font-bold">
            {t("resetPassword.title")}
          </CardTitle>
          <CardDescription>{t("resetPassword.description")}</CardDescription>
        </CardHeader>
        <CardContent className="p-0 md:p-6 md:pt-0">
          {isSuccess ? (
            <div className="space-y-4">
              <Alert>
                <AlertDescription>
                  {t("resetPassword.successMessage")}
                </AlertDescription>
              </Alert>
              <Button asChild className="w-full">
                <Link href="/login">{t("forgotPassword.backToLogin")}</Link>
              </Button>
            </div>
          ) : (
            <form onSubmit={handleSubmit} className="space-y-4">
              {error && (
                <Alert
                  variant="destructive"
                  className="border-destructive bg-destructive text-destructive-foreground"
                >
                  <AlertDescription>{error}</AlertDescription>
                </Alert>
              )}
              <div className="space-y-2">
                <Label htmlFor="password">
                  {t("resetPassword.newPassword")}
                </Label>
                <Input
                  id="password"
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder={t("resetPassword.enterNewPassword")}
                  autoComplete="new-password"
                  minLength={8}
                  required
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="confirmPassword">
                  {t("resetPassword.confirmPassword")}
                </Label>
                <Input
                  id="confirmPassword"
                  type="password"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  placeholder={t("resetPassword.enterConfirmPassword")}
                  autoComplete="new-password"
                  required
                />
              </div>
              <Button
                type="submit"
                className="w-full"
                isLoading={isLoading}
                disabled={isLoading}
              >
                {t("resetPassword.submit")}
              </Button>
            </form>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
