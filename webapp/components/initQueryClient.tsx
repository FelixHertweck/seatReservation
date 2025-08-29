"use client";

import type React from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { client } from "@/api/client.gen";
import { toast } from "@/hooks/use-toast";
import { useLoginRequiredPopup } from "@/hooks/use-login-popup";

export default function InitQueryClient({
  children,
}: {
  children: React.ReactNode;
}) {
  const { triggerLoginRequired, setIsOpen, isOpen } = useLoginRequiredPopup();

  client.setConfig({
    baseUrl: `/`,
    throwOnError: true,
    fetch: async (input: RequestInfo, init?: RequestInit) => {
      const response = await fetch(input, init);
      if (!response.ok) {
        const error = new Error(response.statusText) as any;
        error.response = { status: response.status };
        throw error;
      }
      if (isOpen) {
        setIsOpen(false);
      }
      return response;
    },
  });

  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 60000,
        refetchOnMount: true,
        refetchOnWindowFocus: true,
        throwOnError(error) {
          const status = (error as any)?.response?.status;
          if (status !== 401) {
            toast({
              title: "An error occurred",
              description: error.message || "Please try again.",
              variant: "destructive",
            });
          }
          return false;
        },
        retry: (failureCount, error) => {
          const status = (error as any)?.response?.status;
          if (status === 401 && failureCount > 0) {
            triggerLoginRequired();
            return false;
          }
          return failureCount < 2;
        },
      },

      mutations: {
        onError: (error: Error) => {
          const errorTitle =
            error.message ||
            (error as unknown as ValidationError).title ||
            "An unexpected error occurred" ||
            "An error occurred";
          const errorDescription =
            (error as unknown as ValidationError).violations
              ?.map((violation) => violation.message)
              .join(", ") || "Please try again.";

          toast({
            title: errorTitle,
            description: errorDescription,
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

interface ValidationError {
  title: string;
  status: number;
  violations: ViolationError[];
}

interface ViolationError {
  field: string;
  message: string;
}
