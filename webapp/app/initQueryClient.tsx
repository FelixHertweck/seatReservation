"use client";

import type React from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { client } from "@/api/client.gen";
import { toast } from "@/hooks/use-toast";

export default function InitQueryClient({
  children,
}: {
  children: React.ReactNode;
}) {
  client.setConfig({
    baseUrl: `/`,
  });

  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 60000,
        throwOnError: () => {
          toast({
            title: "Error",
            description: "Failed to load data. Please try again.",
            variant: "destructive",
          });
          return false;
        },
      },
      mutations: {
        onError: (error: Error) => {
          toast({
            title: "Error",
            description: "An error occurred: " + error.message,
            variant: "destructive",
          });
        },
      },
    },
  });

  return (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
}
