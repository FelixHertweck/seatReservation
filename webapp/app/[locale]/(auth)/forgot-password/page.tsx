"use client";

import { useState } from "react";
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
import { postApiAuthPasswordReset } from "@/api/index";

export default function ForgotPasswordPage() {
  const t = useT();
  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setError(null);

    try {
      await postApiAuthPasswordReset({
        body: {
          username: username.trim(),
          email: email.trim(),
        },
      });
      setIsSuccess(true);
    } catch {
      setError(t("forgotPassword.error"));
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="flex h-screen w-full items-center justify-center px-4">
      <Card className="w-full max-w-sm">
        <CardHeader>
          <CardTitle className="text-2xl">
            {t("forgotPassword.title")}
          </CardTitle>
          <CardDescription>{t("forgotPassword.description")}</CardDescription>
        </CardHeader>
        <CardContent>
          {isSuccess ? (
            <Alert className="mb-4">
              <AlertDescription>
                {t("forgotPassword.successMessage")}
              </AlertDescription>
            </Alert>
          ) : (
            <form onSubmit={handleSubmit} className="space-y-4">
              {error && (
                <Alert variant="destructive">
                  <AlertDescription>{error}</AlertDescription>
                </Alert>
              )}
              <div className="space-y-2">
                <Label htmlFor="username">{t("login.username")}</Label>
                <Input
                  id="username"
                  type="text"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  placeholder={t("login.enterUsername")}
                  required
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="email">{t("register.email")}</Label>
                <Input
                  id="email"
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder={t("register.enterEmail")}
                  required
                />
              </div>
              <Button type="submit" className="w-full" disabled={isLoading}>
                {isLoading
                  ? t("forgotPassword.sending")
                  : t("forgotPassword.sendLink")}
              </Button>
            </form>
          )}
          <div className="mt-4 text-center text-sm">
            <Link href="/login" className="text-primary hover:underline">
              {t("forgotPassword.backToLogin")}
            </Link>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
