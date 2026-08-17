"use client";

import type React from "react";

import { useState } from "react";
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
import { Altcha } from "@/components/common/altcha";

export default function ForgotUsernamePage() {
  const t = useT();
  const { requestUsernameRecovery } = useAuth();
  const [email, setEmail] = useState("");
  const [altchaPayload, setAltchaPayload] = useState("");
  const [altchaResetKey, setAltchaResetKey] = useState(0);
  const [isLoading, setIsLoading] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    try {
      await requestUsernameRecovery({ email: email.trim(), altchaPayload });
      setIsSuccess(true);
    } catch {
      setAltchaResetKey((key) => key + 1);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="flex min-h-screen w-full items-center justify-center bg-background px-4">
      <Card className="w-full max-w-md rounded-none border-0 bg-transparent shadow-none md:rounded-lg md:border md:bg-card md:shadow-sm">
        <CardHeader className="space-y-1 p-0 pb-4 md:p-6">
          <CardTitle className="text-2xl font-bold">
            {t("forgotUsername.title")}
          </CardTitle>
          <CardDescription>{t("forgotUsername.description")}</CardDescription>
        </CardHeader>
        <CardContent className="p-0 md:p-6 md:pt-0">
          <Altcha onVerified={setAltchaPayload} resetKey={altchaResetKey} />
          {isSuccess ? (
            <Alert className="mb-4 text-green-600 dark:text-green-400 bg-green-50 dark:bg-green-900/20 border-green-200 dark:border-green-800">
              <AlertDescription>
                {t("forgotUsername.successMessage")}
              </AlertDescription>
            </Alert>
          ) : (
            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="email">{t("register.email")}</Label>
                <Input
                  id="email"
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder={t("register.emailPlaceholder")}
                  required
                />
              </div>
              <Button
                type="submit"
                className="w-full"
                isLoading={isLoading}
                disabled={isLoading || !altchaPayload}
              >
                {t("forgotUsername.sendLink")}
              </Button>
            </form>
          )}
          <Button asChild variant="outline" className="mt-4 w-full">
            <Link href="/login">{t("forgotUsername.backToLogin")}</Link>
          </Button>
        </CardContent>
      </Card>
    </div>
  );
}
