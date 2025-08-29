"use client";

import type React from "react";
import { CookiesProvider } from "react-cookie";
import InitQueryClient from "./init-query-client";
import { ThemeProvider } from "@/components/theme-provider";
import { Toaster } from "@/components/ui/toaster";
import { usePathname } from "next/navigation";
import { useEffect } from "react";
import { clearAllToasts } from "@/hooks/use-toast";
import {
  ToasterProvider,
  useToasterControl,
} from "@/hooks/use-toaster-control";
import { LoginRequiredPopupProvider } from "@/hooks/use-login-popup";

export function ClientProviders({ children }: { children: React.ReactNode }) {
  return (
    <ToasterProvider>
      <CookiesProvider>
        <LoginRequiredPopupProvider>
          <InitQueryClient>
            <ThemeProvider
              attribute="class"
              defaultTheme="system"
              enableSystem
              disableTransitionOnChange
            >
              {children}
              <ToasterControlWrapper />
            </ThemeProvider>
          </InitQueryClient>
        </LoginRequiredPopupProvider>
      </CookiesProvider>
    </ToasterProvider>
  );
}

function ToasterControlWrapper() {
  const { toasterDisabled, disableToaster, enableToaster } =
    useToasterControl();
  const pathname = usePathname();

  useEffect(() => {
    clearAllToasts();
    if (pathname.endsWith("start")) {
      disableToaster();
    } else {
      enableToaster();
    }
  }, [pathname, disableToaster, enableToaster]);

  return <Toaster disabled={toasterDisabled} />;
}
