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
    fetch: async (input: RequestInfo | URL, init?: RequestInit) => {
      const response = await fetch(input, init);
      if (!response.ok) {
        const error = new Error(response.statusText) as any;
        error.response = { status: response.status };

        // Try to parse error response body as JSON
        try {
          const errorBody = await response.text();
          if (errorBody) {
            try {
              error.response.data = JSON.parse(errorBody);
            } catch {
              error.response.data = { message: errorBody };
            }
          }
        } catch {
          console.error("Failed to parse error response body as JSON");
        }

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
        onError: (error: Error, variables, context) => {
          // Try to extract message from error response body
          const errorResponse = (error as any)?.response;
          const responseData = errorResponse?.data;

          // Handle different error formats
          let errorTitle = "An error occurred";
          let errorDescription = "Please try again.";

          if (responseData) {
            // Handle Constraint Violations format
            if (responseData.violations && responseData.violations.length > 0) {
              errorTitle = responseData.title || "Constraint Violation";
              errorDescription = responseData.violations
                .map((violation: ViolationError) => violation.message)
                .join(", ");
            }
            // Handle simple error format
            else if (responseData.error) {
              errorTitle = "Error";
              errorDescription = responseData.error;
            }
            // Handle message format
            else if (responseData.message) {
              errorTitle = "Error";
              errorDescription = responseData.message;
            }
          }

          // Fallback to error message
          if (errorDescription === "Please try again." && error.message) {
            errorDescription = error.message;
          }

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
