import type React from "react";
import { languages } from "@/lib/i18n/config";
import { ClientProviders } from "@/components/client-providers";

export async function generateStaticParams() {
  return languages.map((locale) => ({ locale }));
}

export default async function LocaleLayout({
  children,
  params,
}: {
  children: React.ReactNode;
  params: Promise<{ locale: string }>;
}) {
  const { locale } = await params;

  return <ClientProviders locale={locale}>{children}</ClientProviders>;
}
