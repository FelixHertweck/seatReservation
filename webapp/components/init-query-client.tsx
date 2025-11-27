"use client";

import type React from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { client } from "@/api/client.gen";
import { useLoginRequiredPopup } from "@/hooks/use-login-popup";
import { getRefreshTokenExpiration } from "@/lib/refreshTokenExpirationCookie";

export interface ErrorWithResponse extends Error {
  response?: {
    status: number;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    rawData: any;
    description: string;
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

  const scheduleTriggerLoginRequired = () => {
    queueMicrotask(() => triggerLoginRequired());
  };

  client.setConfig({
    baseUrl: `/`,
    throwOnError: true,
    fetch: async (input: RequestInfo | URL, init?: RequestInit) => {
      // Clone the request before the first attempt if it's a Request object
      // This is necessary because Request bodies can only be read once
      let clonedRequest: Request | undefined;
      if (input instanceof Request) {
        clonedRequest = input.clone();
      }

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
          // Retry original request using the cloned request or original parameters
          if (clonedRequest) {
            response = await fetch(clonedRequest);
          } else {
            response = await fetch(input, init);
          }
        } else {
          console.warn(
            "Failed to refresh token:",
            refreshResponse.status,
            refreshResponse.statusText,
          );
          scheduleTriggerLoginRequired();
        }
      }
      if (!response.ok) {
        const error = new Error() as ErrorWithResponse;
        const body = await response.text();
        error.response = {
          status: response.status,
          rawData: body,
          description: errorDescriptionConverter(body),
        };
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
            return false;
          }
          return true;
        },
        retry: (failureCount, error) => {
          if ((error as ErrorWithResponse)?.response?.status === 401) {
            scheduleTriggerLoginRequired();
            return false;
          }
          return failureCount < 2;
        },
      },

      mutations: {
        retryDelay: 1000,
        retry: (_failureCount, error) => {
          if ((error as ErrorWithResponse)?.response?.status === 401) {
            scheduleTriggerLoginRequired();
          }
          return false;
        },
      },
    },
  });

  return (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
}

const errorDescriptionConverter = (response: string) => {
  // response is the raw response.text()
  if (!response) return response;

  try {
    const parsed = JSON.parse(response);

    // If it's a string
    if (typeof parsed === "string") return parsed;

    // Pattern 1: { message: "..." }
    if (typeof parsed?.message === "string") {
      return parsed.message;
    }

    // Pattern 2: Constraint Violation
    // {"title":"Constraint Violation","status":400,"violations":[{...}]}
    if (Array.isArray(parsed?.violations)) {
      const messages = parsed.violations
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        .map((v: any) =>
          typeof v?.message === "string" ? v.message : undefined,
        )
        .filter(Boolean) as string[];
      if (messages.length > 0) return messages.join(", ");
    }

    // Fallback
    return "Unknown error. Please try again.";
  } catch {
    // Not JSON, just return raw body
    return "Unknown error. Please try again.";
  }
};
