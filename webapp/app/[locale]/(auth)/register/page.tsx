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
import { Label } from "@/components/ui/label";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import { Info } from "lucide-react";
import { useAuth } from "@/hooks/use-auth";
import type { RegisterRequestDto } from "@/api";
import { useT } from "@/lib/i18n/hooks";
import { InputWithLoading } from "@/components/common/input-with-loading";
import { TFunction } from "i18next";

export default function RegisterPage() {
  const t = useT();

  const [formData, setFormData] = useState({
    username: "",
    email: "",
    password: "",
    firstname: "",
    lastname: "",
  });
  const [isLoading, setIsLoading] = useState(false);
  const { register, registrationStatus } = useAuth();

  const isPasswordTooShort =
    formData.password.length > 0 && formData.password.length < 8;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    try {
      const userData: RegisterRequestDto = {
        username: formData.username,
        email: formData.email,
        password: formData.password,
        firstname: formData.firstname,
        lastname: formData.lastname,
      };
      await register(userData);
    } finally {
      setIsLoading(false);
    }
  };

  const handleInputChange = (field: string, value: string) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
  };

  const isDisabled = registrationStatus.data?.enabled === false;

  return (
    <div className="flex min-h-screen w-full items-center justify-center bg-background px-4">
      <Card className="w-full max-w-md">
        <CardHeader className="space-y-1">
          <CardTitle className="text-2xl font-bold">
            {t("register.createAccountTitle")}
          </CardTitle>
          <CardDescription>{t("register.enterInfo")}</CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="firstname">{t("register.firstName")}</Label>
                <InputWithLoading
                  id="firstname"
                  placeholder={t("register.firstNamePlaceholder")}
                  value={formData.firstname}
                  onChange={(e) =>
                    handleInputChange("firstname", e.target.value)
                  }
                  disabled={registrationStatus.isLoading || isDisabled}
                  loading={registrationStatus.isLoading}
                  required
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="lastname">{t("register.lastName")}</Label>
                <InputWithLoading
                  id="lastname"
                  placeholder={t("register.lastNamePlaceholder")}
                  value={formData.lastname}
                  onChange={(e) =>
                    handleInputChange("lastname", e.target.value)
                  }
                  disabled={registrationStatus.isLoading || isDisabled}
                  loading={registrationStatus.isLoading}
                  required
                />
              </div>
            </div>
            <div className="space-y-2">
              <div className="flex items-center gap-2">
                <Label htmlFor="username">{t("register.username")}</Label>
                <TooltipProvider>
                  <Tooltip>
                    <TooltipTrigger asChild>
                      <Info className="h-4 w-4 text-muted-foreground cursor-help" />
                    </TooltipTrigger>
                    <TooltipContent>
                      <p>{t("validation.usernameHint")}</p>
                    </TooltipContent>
                  </Tooltip>
                </TooltipProvider>
              </div>
              <InputWithLoading
                id="username"
                placeholder={t("register.usernamePlaceholder")}
                value={formData.username}
                onChange={(e) => handleInputChange("username", e.target.value)}
                autoCapitalize="none"
                autoComplete="username"
                disabled={registrationStatus.isLoading || isDisabled}
                loading={registrationStatus.isLoading}
                required
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="email">{t("register.email")}</Label>
              <InputWithLoading
                id="email"
                type="email"
                placeholder={t("register.emailPlaceholder")}
                value={formData.email}
                onChange={(e) => handleInputChange("email", e.target.value)}
                disabled={registrationStatus.isLoading || isDisabled}
                loading={registrationStatus.isLoading}
                required
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="password">{t("register.password")}</Label>
              <InputWithLoading
                id="password"
                type="password"
                value={formData.password}
                onChange={(e) => handleInputChange("password", e.target.value)}
                disabled={registrationStatus.isLoading || isDisabled}
                loading={registrationStatus.isLoading}
                required
              />
              {isPasswordTooShort && (
                <p className="text-sm text-destructive">
                  {t("register.passwordTooShort")}
                </p>
              )}
            </div>
            <Button
              type="submit"
              className="w-full"
              disabled={isLoading || isDisabled || isPasswordTooShort}
            >
              {ButtonLabel(t, isLoading, isDisabled)}
            </Button>
          </form>
          {isDisabled && (
            <div className="mt-4 text-center text-sm text-destructive">
              {t("register.registrationDisabled.description")}
            </div>
          )}
          <div className="mt-4 text-center text-sm">
            {t("register.alreadyHaveAccount")}
            <Link href="/login" className="text-primary hover:underline">
              {t("register.signIn")}
            </Link>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}

const ButtonLabel = (
  t: TFunction<string, string>,
  isLoading: boolean,
  isDisabled: boolean,
) => {
  if (isDisabled) {
    return t("register.registrationDisabled.title");
  }
  if (isLoading) return t("register.creatingAccount");
  return t("register.createAccountButton");
};
