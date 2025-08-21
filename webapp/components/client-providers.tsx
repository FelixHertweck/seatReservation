"use client";

import type React from "react";
import { CookiesProvider } from "react-cookie";
import InitQueryClient from "./initQueryClient";
import { ThemeProvider } from "@/components/theme-provider";
import { Toaster } from "@/components/ui/toaster";
import { EmailVerificationPrompt } from "@/components/common/EmailVerificationPrompt";
import { usePathname } from "next/navigation";
import { useEffect } from "react";
import { clearAllToasts } from "@/hooks/use-toast";
import {
  ToasterProvider,
  useToasterControl,
} from "@/hooks/use-toaster-control";

export function ClientProviders({ children }: { children: React.ReactNode }) {
  return (
    <ToasterProvider>
      <CookiesProvider>
        <InitQueryClient>
          <ThemeProvider
            attribute="class"
            defaultTheme="system"
            enableSystem
            disableTransitionOnChange
          >
            {children}
            <ToasterControlWrapper />
            <EmailVerificationPrompt />
          </ThemeProvider>
        </InitQueryClient>
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
