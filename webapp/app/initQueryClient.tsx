"use client";

import type React from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { client } from "@/api/client.gen";
import { toast } from "@/hooks/use-toast";
import { usePathname } from "next/navigation";
import { t } from "i18next";

export default function InitQueryClient({
  children,
}: {
  children: React.ReactNode;
}) {
  client.setConfig({
    baseUrl: `/`,
  });
  const currentpath = usePathname();
  const showToast = currentpath === "login" || currentpath === "register";

  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 60000,
        throwOnError: () => {
          if (showToast) {
            setTimeout(() => {
              toast({
                title: t("error.title"),
                description: t("error.dataLoadFailed"),
                variant: "destructive",
              });
            }, 0);
          }
          return false;
        },
      },
      mutations: {
        onError: (error: Error) => {
          if (showToast) {
            toast({
              title: t("error.title"),
              description: t("error.anErrorOccurred") + error.message,
              variant: "destructive",
            });
          }
        },
      },
    },
  });

  return (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
}
