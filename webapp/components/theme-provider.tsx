"use client";

import * as React from "react";
import {
  ThemeProvider as NextThemesProvider,
  type ThemeProviderProps,
} from "next-themes";
import { useEffect, useState } from "react";

export function ThemeProvider({ children, ...props }: ThemeProviderProps) {
  const [mounted, setMounted] = useState(false);

  // useEffect only runs on the client, so we can safely show the UI now
  useEffect(() => {
    setMounted(true);
  }, []);

  if (!mounted) {
    // Renders nothing or a fallback UI on the server and during the initial client render.
    // This prevents next-themes from trying to read localStorage or the system theme on the server
    return <>{children}</>;
  }

  return <NextThemesProvider {...props}>{children}</NextThemesProvider>;
}
