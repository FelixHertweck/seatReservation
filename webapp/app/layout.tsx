import type React from "react";
import { SidebarProvider, SidebarInset } from "@/components/ui/sidebar";
import { AppSidebar } from "@/components/app-sidebar";
import { Toaster } from "@/components/ui/toaster";
import "./globals.css";
import InitQueryClient from "@/app/initQueryClient";

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body>
        <InitQueryClient>
          <SidebarProvider>
            <AppSidebar />
            <SidebarInset>
              <main className="flex-1">{children}</main>
            </SidebarInset>
            <Toaster />
          </SidebarProvider>
        </InitQueryClient>
      </body>
    </html>
  );
}

export const metadata = {
  generator: "v0.dev",
};
