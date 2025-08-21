"use client";

import type React from "react";
import { CookiesProvider } from "react-cookie";
import InitQueryClient from "./initQueryClient";
import { ThemeProvider } from "@/components/theme-provider";
import { Toaster } from "@/components/ui/toaster";
import { EmailVerificationPrompt } from "@/components/common/EmailVerificationPrompt";
import { useEffect } from "react";
import i18next from "i18next";

export function ClientProviders({
  children,
  locale,
}: {
  children: React.ReactNode;
  locale?: string;
}) {
  useEffect(() => {
    if (locale && i18next.language !== locale) {
      i18next.changeLanguage(locale);
    }
  }, [locale]);

  return (
    <CookiesProvider>
      <InitQueryClient>
        <ThemeProvider
          attribute="class"
          defaultTheme="system"
          enableSystem
          disableTransitionOnChange
        >
          {children}
          <Toaster />
          <EmailVerificationPrompt />
        </ThemeProvider>
      </InitQueryClient>
    </CookiesProvider>
  );
}
