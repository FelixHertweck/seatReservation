"use client";

import type React from "react";
import { CookiesProvider } from "react-cookie";
import InitQueryClient from "./init-query-client";
import { ThemeProvider } from "@/components/theme-provider";
import { LoginRequiredPopupProvider } from "@/hooks/use-login-popup";
import { UnsavedChangesProvider } from "@/hooks/use-unsaved-changes";
import { Toaster } from "./ui/sonner";

export function ClientProviders({ children }: { children: React.ReactNode }) {
  return (
    <CookiesProvider>
      <LoginRequiredPopupProvider>
        <UnsavedChangesProvider>
          <InitQueryClient>
            <ThemeProvider
              attribute="class"
              defaultTheme="system"
              enableSystem
              disableTransitionOnChange
            >
              {children}
              <Toaster />
            </ThemeProvider>
          </InitQueryClient>
        </UnsavedChangesProvider>
      </LoginRequiredPopupProvider>
    </CookiesProvider>
  );
}
