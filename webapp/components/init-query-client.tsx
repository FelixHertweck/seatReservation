"use client";

import { useLayoutEffect, useState, type ReactNode } from "react";
import {
  QueryCache,
  QueryClient,
  QueryClientProvider,
} from "@tanstack/react-query";
import { toast } from "sonner";
import i18next from "i18next";
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

let onAuthRequiredCallback: (() => void) | null = null;
let onAuthSuccessCallback: (() => void) | null = null;

const notifyAuthRequired = () => {
  if (onAuthRequiredCallback) {
    onAuthRequiredCallback();
  }
};

const notifyAuthSuccess = () => {
  if (onAuthSuccessCallback) {
    onAuthSuccessCallback();
  }
};

let refreshPromise: Promise<Response> | null = null;

const refreshToken = async (): Promise<Response> => {
  if (refreshPromise) {
    return refreshPromise;
  }

  refreshPromise = fetch("/api/auth/refresh", {
    method: "POST",
    credentials: "include",
  }).finally(() => {
    refreshPromise = null;
  });

  return refreshPromise;
};

// Configure client once at module level so baseUrl is always '/' everywhere
client.setConfig({
  baseUrl: `/`,
  throwOnError: true,
  fetch: async (input: RequestInfo | URL, init?: RequestInit) => {
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
      refreshTokenExpiration.getTime() > Date.now()
    ) {
      const refreshResponse = await refreshToken();
      if (refreshResponse.ok) {
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
        notifyAuthRequired();
      }
    }
    if (!response.ok) {
      const body = await response.text();
      const error = new Error(
        `Request failed with status ${response.status}`,
      ) as ErrorWithResponse;
      error.response = {
        status: response.status,
        rawData: body,
        description: errorDescriptionConverter(body) ?? "",
      };
      throw error;
    }
    notifyAuthSuccess();
    return response;
  },
});

function makeQueryClient() {
  return new QueryClient({
    queryCache: new QueryCache({
      onError: (error) => {
        const status = (error as ErrorWithResponse)?.response?.status;
        if (status === 401) return;
        const description = (error as ErrorWithResponse)?.response?.description;
        const message =
          description ||
          (status === 403
            ? i18next.t("common.error.forbidden")
            : i18next.t("common.error.default"));
        toast.error(message, { id: `query-error-${status}-${message}` });
      },
    }),
    defaultOptions: {
      queries: {
        staleTime: 60000,
        refetchOnMount: true,
        refetchOnWindowFocus: true,
        retryDelay: 1000,
        throwOnError: false,
        retry: (failureCount, error) => {
          if ((error as ErrorWithResponse)?.response?.status === 401) {
            notifyAuthRequired();
            return false;
          }
          return failureCount < 2;
        },
      },
      mutations: {
        retryDelay: 1000,
        retry: (_failureCount, error) => {
          if ((error as ErrorWithResponse)?.response?.status === 401) {
            notifyAuthRequired();
          }
          return false;
        },
      },
    },
  });
}

export default function InitQueryClient({
  children,
}: Readonly<{
  children: ReactNode;
}>) {
  const { triggerLoginRequired, setIsOpen } = useLoginRequiredPopup();

  // useLayoutEffect (not useEffect) so this registers before any child's
  // passive effect can fire a request and hit a 401 - all layout effects in
  // the tree run before any passive effect, regardless of child/parent order.
  useLayoutEffect(() => {
    onAuthRequiredCallback = () => {
      queueMicrotask(() => triggerLoginRequired());
    };
    onAuthSuccessCallback = () => {
      queueMicrotask(() => setIsOpen(false));
    };
    return () => {
      onAuthRequiredCallback = null;
      onAuthSuccessCallback = null;
    };
  }, [triggerLoginRequired, setIsOpen]);

  const [queryClient] = useState(makeQueryClient);

  return (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
}

const errorDescriptionConverter = (response: string) => {
  // response is the raw response.text()
  if (!response) return undefined;

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

    return undefined;
  } catch {
    return undefined;
  }
};
