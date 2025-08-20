"use client";

import { useTranslation } from "./client";
import { useParams } from "next/navigation";

export function useT() {
  const params = useParams();
  const locale = params.locale as string;
  const { t } = useTranslation(locale);

  return t;
}
