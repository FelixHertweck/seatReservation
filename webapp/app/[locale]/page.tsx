"use client";

import { useEffect } from "react";
import { useParams, useRouter } from "next/navigation";
import { BouncingDotsLoader } from "@/components/custom-ui/bouncing-dots-loader";
import { useAuth } from "@/hooks/use-auth";
import { redirectUser } from "@/lib/redirect-User";

export default function RootRedirectPage() {
  const params = useParams();
  const locale = params.locale as string;

  const { isLoggedIn, isLoading, user } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (!isLoading) {
      if (isLoggedIn) {
        redirectUser(router, locale, user);
      } else {
        router.replace(`/${locale}/start`);
      }
    }
  }, [isLoggedIn, isLoading, router, locale, user]);

  return (
    <div className="flex min-h-screen items-center justify-center bg-background">
      <BouncingDotsLoader />
    </div>
  );
}
