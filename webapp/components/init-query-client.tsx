"use client";

import type React from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { client } from "@/api/client.gen";
import { toast } from "@/hooks/use-toast";
import { useLoginRequiredPopup } from "@/hooks/use-login-popup";
import { getRefreshTokenExpiration } from "@/lib/refreshTokenExpirationCookie";

export interface ErrorWithResponse extends Error {
  response?: {
    status: number;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    data?: any;
  };
}

// Promise cache for token refresh to prevent multiple simultaneous refresh calls
let refreshPromise: Promise<Response> | null = null;

const refreshToken = async (): Promise<Response> => {
  if (refreshPromise) {
    return refreshPromise;
  }

  refreshPromise = fetch("/api/auth/refresh", {
    method: "POST",
    credentials: "include",
  }).finally(() => {
    // Clear the promise after it resolves/rejects so next refresh can be triggered
    refreshPromise = null;
  });

  return refreshPromise;
};

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
      let response = await fetch(input, init);
      const refreshTokenExpiration = getRefreshTokenExpiration();
      if (
        !response.ok &&
        response.status === 401 &&
        refreshTokenExpiration !== null &&
        refreshTokenExpiration.getTime() > new Date().getTime()
      ) {
        // Refresh token (uses cached promise if multiple requests fail simultaneously)
        const refreshResponse = await refreshToken();
        if (refreshResponse.ok) {
          // Retry original request
          response = await fetch(input, init);
        } else {
          console.error(
            "Failed to refresh token:",
            refreshResponse.status,
            refreshResponse.statusText,
          );
          triggerLoginRequired();
        }
      }
      if (!response.ok) {
        const error = new Error(response.statusText) as ErrorWithResponse;
        error.response = { ...response, status: response.status };

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
        retryDelay: 1000,
        throwOnError(error) {
          const status = (error as ErrorWithResponse)?.response?.status;
          if (status !== 401) {
            toast({
              title: "An error occurred",
              description: error.message || "Please try again.",
              variant: "destructive",
            });
            return false;
          }
          return true;
        },
        retry: (failureCount, error) => {
          if ((error as ErrorWithResponse)?.response?.status === 401) {
            triggerLoginRequired();
            return false;
          }
          return failureCount < 2;
        },
      },

      mutations: {
        retryDelay: 1000,
        retry: (_failureCount, error) => {
          if ((error as ErrorWithResponse)?.response?.status === 401) {
            triggerLoginRequired();
          }
          return false;
        },
        onError: (error: Error) => {
          const errorResponse = (error as ErrorWithResponse)?.response;
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

interface ViolationError {
  field: string;
  message: string;
}
