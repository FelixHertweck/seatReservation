import type React from "react";
import "./globals.css";
import { Toaster } from "@/components/ui/toaster";
import InitQueryClient from "@/app/initQueryClient";
import { ThemeProvider } from "@/components/theme-provider";
import { EmailVerificationPrompt } from "@/components/common/EmailVerificationPrompt";

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body>
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
      </body>
    </html>
  );
}

export const metadata = {
  generator: "v0.dev",
};
