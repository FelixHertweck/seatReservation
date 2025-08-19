"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { BouncingDotsLoader } from "@/components/ui/bouncing-dots-loader";
import { useAuthStatus } from "@/hooks/use-auth-status";

export default function RootRedirectPage() {
  const { isLoggedIn, isLoading } = useAuthStatus();
  const router = useRouter();

  useEffect(() => {
    if (!isLoading) {
      if (isLoggedIn) {
        router.replace("/events");
      } else {
        router.replace("/start");
      }
    }
  }, [isLoggedIn, isLoading, router]);

  return (
    <div className="flex min-h-screen items-center justify-center bg-background">
      <BouncingDotsLoader />
    </div>
  );
}
