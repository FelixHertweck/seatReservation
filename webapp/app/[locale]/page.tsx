"use client";

import { useEffect } from "react";
import { useParams, useRouter } from "next/navigation";
import { BouncingDotsLoader } from "@/components/ui/bouncing-dots-loader";
import { useAuth } from "@/hooks/use-auth";

export default function RootRedirectPage() {
  const params = useParams();
  const locale = params.locale as string;

  const { isLoggedIn, isLoading } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (!isLoading) {
      if (isLoggedIn) {
        router.replace(`/${locale}/events`);
      } else {
        router.replace(`/${locale}/start`);
      }
    }
  }, [isLoggedIn, isLoading, router, locale]);

  return (
    <div className="flex min-h-screen items-center justify-center bg-background">
      <BouncingDotsLoader />
    </div>
  );
}
