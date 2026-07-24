"use client";

import { useState } from "react";
import { useSearchParams } from "next/navigation";
import Link from "next/link";
import { useT } from "@/lib/i18n/hooks";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { postApiAuthPasswordResetConfirm } from "@/api/index";

export default function ResetPasswordPage() {
  const t = useT();
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

    if (password !== confirmPassword) {
      setError(t("resetPassword.error.mismatch"));
      return;
    }

    setIsLoading(true);

    try {
      await postApiAuthPasswordResetConfirm({
        body: {
          token: token,
          newPassword: password,
        },
      });
      setIsSuccess(true);
    } catch (err: unknown) {
      if ((err as { status?: number }).status === 400) {
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
      <div className="flex h-screen w-full items-center justify-center px-4">
        <Card className="w-full max-w-sm">
          <CardHeader>
            <CardTitle className="text-2xl">
              {t("resetPassword.title")}
            </CardTitle>
          </CardHeader>
          <CardContent>
            <Alert variant="destructive" className="mb-4">
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
    <div className="flex h-screen w-full items-center justify-center px-4">
      <Card className="w-full max-w-sm">
        <CardHeader>
          <CardTitle className="text-2xl">{t("resetPassword.title")}</CardTitle>
          <CardDescription>{t("resetPassword.description")}</CardDescription>
        </CardHeader>
        <CardContent>
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
                <Alert variant="destructive">
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
                  required
                />
              </div>
              <Button type="submit" className="w-full" disabled={isLoading}>
                {isLoading
                  ? t("resetPassword.submitting")
                  : t("resetPassword.submit")}
              </Button>
            </form>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
