import type React from "react";
import { languages } from "@/lib/i18n/config";
import { ClientProviders } from "@/components/client-providers";

export async function generateStaticParams() {
  return languages.map((locale) => ({ locale }));
}

export default async function LocaleLayout({
  children,
}: {
  children: React.ReactNode;
  params: Promise<{ locale: string }>;
}) {
  return <ClientProviders>{children}</ClientProviders>;
}
