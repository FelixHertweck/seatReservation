"use client";

import { useState } from "react";
import { getApiAuthUsernameSuggestion } from "@/api";

/**
 * Suggests a free username derived from a first/last name pair. The
 * candidate search (base name, then numeric suffixes) runs entirely
 * server-side in a single request, so clicking "suggest" doesn't hammer
 * the availability-check endpoint with dozens of lookups.
 */
export function useGenerateUsername() {
  const [isGenerating, setIsGenerating] = useState(false);

  const generate = async (
    firstname: string,
    lastname: string,
  ): Promise<string | null> => {
    setIsGenerating(true);
    try {
      const { data } = await getApiAuthUsernameSuggestion({
        query: { firstname, lastname },
      });
      return data?.username ?? null;
    } finally {
      setIsGenerating(false);
    }
  };

  return { generate, isGenerating };
}
