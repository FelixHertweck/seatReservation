"use client";

import type React from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { client } from "@/api/client.gen";
import { toast } from "@/hooks/use-toast";
import { usePathname } from "next/navigation";

export default function InitQueryClient({
  children,
}: {
  children: React.ReactNode;
}) {
  client.setConfig({
    baseUrl: `/`,
  });
  const currentpath = usePathname();

  const showToast = !(
    currentpath.includes("login") || currentpath.includes("register")
  );

  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 60000,
        throwOnError: () => {
          if (showToast) {
            setTimeout(() => {
              toast({
                title: "An error occurred",
                description: "Failed to retrieve data",
                variant: "destructive",
              });
            }, 0);
          }
          return false;
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
