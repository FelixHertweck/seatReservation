import type React from "react";
import "./globals.css";
import { Toaster } from "@/components/ui/toaster";
import InitQueryClient from "@/app/initQueryClient";
import { ThemeProvider } from "@/components/theme-provider";
import { EmailVerificationPrompt } from "@/components/common/EmailVerificationPrompt";
import { initI18N } from "@/locals/Languages";

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  initI18N();

  return (
    <html lang="en">
      <head>
        <link
          rel="apple-touch-icon"
          sizes="180x180"
          href="/apple-touch-icon.png"
        />
        <link
          rel="icon"
          type="image/png"
          sizes="32x32"
          href="/favicon-32x32.png"
        />
        <link
          rel="icon"
          type="image/png"
          sizes="16x16"
          href="/favicon-16x16.png"
        />
        <link rel="manifest" href="/site.webmanifest" />
      </head>
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

export const metadata = {};
