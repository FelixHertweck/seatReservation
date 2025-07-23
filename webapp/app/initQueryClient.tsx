"use client";

import React from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { client } from "@/api/client.gen";

export default function InitQueryClient({
  children,
}: {
  children: React.ReactNode;
}) {
  client.setConfig({
    baseUrl: `/`,
  });

  const queryClient = new QueryClient();

  return (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
}
